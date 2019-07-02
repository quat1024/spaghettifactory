package quaternary.spaghettifactory.discovery;

import net.fabricmc.loader.FabricLoader;
import net.fabricmc.loader.discovery.ModCandidate;
import net.fabricmc.loader.discovery.ModCandidateFinder;
import net.fabricmc.loader.discovery.ModCandidateSet;
import net.fabricmc.loader.discovery.ModResolutionException;
import net.fabricmc.loader.discovery.ModResolver;
import net.fabricmc.loader.metadata.LoaderModMetadata;
import net.fabricmc.loader.metadata.ModMetadataParser;
import net.fabricmc.loader.util.FileSystemUtil;
import net.fabricmc.loader.util.UrlConversionException;
import net.fabricmc.loader.util.UrlUtil;
import quaternary.spaghettifactory.ReflectionHax;
import quaternary.spaghettifactory.SpaghettiFactory;
import quaternary.spaghettifactory.metadata.ForgeModMetadataParser;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

public class ForgeModResolver extends ModResolver {
	private static final Map<URL, List<Path>> inMemoryCache;
	private static final Pattern MOD_ID_PATTERN;
	private static final Object launcherSyncObject;
	
	private final List<ModCandidateFinder> forgeCandidateFinders = new ArrayList<>();
	
	static {
		inMemoryCache = ReflectionHax.get(ModResolver.class, null, "inMemoryCache");
		MOD_ID_PATTERN = ReflectionHax.get(ModResolver.class, null, "MOD_ID_PATTERN");
		launcherSyncObject = ReflectionHax.get(ModResolver.class, null, "launcherSyncObject");
	}
	
	@Override
	public void addCandidateFinder(ModCandidateFinder f) {
		forgeCandidateFinders.add(f);
	}
	
	@Override
	public Map<String, ModCandidate> resolve(FabricLoader loader) throws ModResolutionException {
		//Mainly a copy-paste of superclass.
		//Changes prefixed with ///
		
		Map<String, ModCandidateSet> candidatesById = new ConcurrentHashMap<>();
		
		long time1 = System.currentTimeMillis();
		
		///new action
		Queue<ForgeUrlProcessAction> allActions = new ConcurrentLinkedQueue<>();
		ForkJoinPool pool = new ForkJoinPool(Math.max(1, Runtime.getRuntime().availableProcessors() - 1));
		for (ModCandidateFinder f : forgeCandidateFinders) {
			f.findCandidates(loader, (u) -> {
				///Use a different URLProcessAction
				ForgeUrlProcessAction action = new ForgeUrlProcessAction(loader, candidatesById, u, 0);
				allActions.add(action);
				pool.execute(action);
			});
		}
		
		boolean tookTooLong = false;
		Throwable exception = null;
		try {
			pool.shutdown();
			pool.awaitTermination(30, TimeUnit.SECONDS);
			for (ForgeUrlProcessAction action : allActions) { ///new action
				if (!action.isDone()) {
					tookTooLong = true;
				} else {
					Throwable t = action.getException();
					if (t != null) {
						if (exception == null) {
							exception = t;
						} else {
							exception.addSuppressed(t);
						}
					}
				}
			}
		} catch (InterruptedException e) {
			/// different error message
			throw new RuntimeException("Forge mod resolution took too long!", e);
		}
		if (tookTooLong) {
			/// different error message
			throw new RuntimeException("Forge mod resolution took too long!");
		}
		if (exception != null) {
			/// different error message
			throw new RuntimeException("Forge mod resolution failed!", exception);
		}
		
		long time2 = System.currentTimeMillis();             ///different logger
		Map<String, ModCandidate> result = findCompatibleSet(SpaghettiFactory.LOGGER, candidatesById);
		
		long time3 = System.currentTimeMillis();
		///different logger
		SpaghettiFactory.LOGGER.debug("Forge mod resolution detection time: " + (time2 - time1) + "ms");
		SpaghettiFactory.LOGGER.debug("Forge mod resolution time: " + (time3 - time2) + "ms");
		
		return result;
	}
	
	///Also a copypaste of the superclass's inner class basically.
	static class ForgeUrlProcessAction extends RecursiveAction {
		private final FabricLoader loader;
		private final Map<String, ModCandidateSet> candidatesById;
		private final URL url;
		private final int depth;
		
		ForgeUrlProcessAction(FabricLoader loader, Map<String, ModCandidateSet> candidatesById, URL url, int depth) {
			this.loader = loader;
			this.candidatesById = candidatesById;
			this.url = url;
			this.depth = depth;
		}
		
		@Override
		protected void compute() {
			FileSystemUtil.FileSystemDelegate jarFs;
			Path path, metaInfDir, modToml, rootDir;
			URL normalizedUrl;
			
			SpaghettiFactory.LOGGER.debug("Testing " + url);
			
			try {
				path = UrlUtil.asPath(url).normalize();
				// normalize URL (used as key for nested JAR lookup)
				normalizedUrl = UrlUtil.asUrl(path);
			} catch (UrlConversionException e) {
				throw new RuntimeException("Failed to convert URL " + url + "!", e);
			}
			
			if (Files.isDirectory(path)) {
				throw new IllegalArgumentException("Can't handle mods from directories right now: " + path);
				/*
				// Directory
				modJson = path.resolve("fabric.mod.json");
				rootDir = path;
				
				if (loader.isDevelopmentEnvironment() && !Files.exists(modJson)) {
					SpaghettiFactory.LOGGER.warn("Adding directory " + path + " to mod classpath in development environment - workaround for Gradle splitting mods into two directories");
					synchronized (launcherSyncObject) {
						FabricLauncherBase.getLauncher().propose(url);
					}
				}
				*/
			} else {
				// JAR file
				try {
					///grab the forge mods.toml file
					jarFs = FileSystemUtil.getJarFileSystem(path, false);
					metaInfDir = jarFs.get().getPath("META-INF");
					modToml = metaInfDir.resolve("mods.toml");
					//rootDir = jarFs.get().getRootDirectories().iterator().next();
				} catch (IOException e) {
					throw new RuntimeException("Failed to open mod Forge JAR at " + path + "!");
				}
			}
			
			LoaderModMetadata[] info;
			
			try (InputStream stream = Files.newInputStream(modToml)) {
				info = ForgeModMetadataParser.getForgeMods(loader, stream);
			} catch (Exception e) {
				throw new RuntimeException("Can't recognize the Forge mod at '" + path + "'", e);
			}
			
			for (LoaderModMetadata i : info) {
				ModCandidate candidate = new ModCandidate(i, normalizedUrl, depth);
				boolean added;
				
				if (candidate.getInfo().getId() == null || candidate.getInfo().getId().isEmpty()) {
					throw new RuntimeException(String.format("Mod file `%s` has no id", candidate.getOriginUrl().getFile()));
				}
				
				if (!MOD_ID_PATTERN.matcher(candidate.getInfo().getId()).matches()) {
					throw new RuntimeException(String.format("Mod id `%s` does not match the requirements", candidate.getInfo().getId()));
				}
				
				if (candidate.getInfo().getSchemaVersion() < ModMetadataParser.LATEST_VERSION) {
					SpaghettiFactory.LOGGER.warn("Mod ID " + candidate.getInfo().getId() + " uses outdated schema version: " + candidate.getInfo().getSchemaVersion() + " < " + ModMetadataParser.LATEST_VERSION);
				}
				
				added = candidatesById.computeIfAbsent(candidate.getInfo().getId(), ModCandidateSet::new).add(candidate);
				
				if (!added) {
					SpaghettiFactory.LOGGER.debug(candidate.getOriginUrl() + " already present as " + candidate);
				} else {
					SpaghettiFactory.LOGGER.debug("Adding " + candidate.getOriginUrl() + " as " + candidate);
					
					//Skip jij for now
					/*
					List<Path> jarInJars = inMemoryCache.computeIfAbsent(candidate.getOriginUrl(), (u) -> {
						SpaghettiFactory.LOGGER.debug("Searching for nested JARs in " + candidate);
						Collection<NestedJarEntry> jars = candidate.getInfo().getJars();
						List<Path> list = new ArrayList<>(jars.size());
						
						jars.stream()
							.map((j) -> rootDir.resolve(j.getFile().replace("/", rootDir.getFileSystem().getSeparator())))
							.forEach((modPath) -> {
								if (!Files.isDirectory(modPath) && modPath.toString().endsWith(".jar")) {
									// TODO: pre-check the JAR before loading it, if possible
									SpaghettiFactory.LOGGER.debug("Found nested JAR: " + modPath);
									Path dest = inMemoryFs.getPath(UUID.randomUUID() + ".jar");
									
									try {
										Files.copy(modPath, dest);
									} catch (IOException e) {
										throw new RuntimeException("Failed to load nested JAR " + modPath + " into memory (" + dest + ")!", e);
									}
									
									list.add(dest);
								}
							});
						
						return list;
					});
					
					if (!jarInJars.isEmpty()) {
						invokeAll(
							jarInJars.stream()
								.map((p) -> {
									try {
										return new ForgeUrlProcessAction(loader, candidatesById, UrlUtil.asUrl(p.normalize()), depth + 1);
									} catch (UrlConversionException e) {
										throw new RuntimeException("Failed to turn path '" + p.normalize() + "' into URL!", e);
									}
								}).collect(Collectors.toList())
						);
					}
					*/
				}
			}

			/* if (jarFs != null) {
				jarFs.close();
			} */
		}
	}
}
