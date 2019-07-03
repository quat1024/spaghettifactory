package quaternary.spaghettifactory.stage;

import net.fabricmc.loader.FabricLoader;
import net.fabricmc.loader.discovery.ModCandidate;
import net.fabricmc.loader.discovery.ModCandidateFinder;
import net.fabricmc.loader.discovery.ModCandidateSet;
import net.fabricmc.loader.discovery.ModResolutionException;
import net.fabricmc.loader.discovery.ModResolver;
import net.fabricmc.loader.metadata.LoaderModMetadata;
import net.fabricmc.loader.util.FileSystemUtil;
import net.fabricmc.loader.util.UrlConversionException;
import net.fabricmc.loader.util.UrlUtil;
import quaternary.spaghettifactory.ReflectionHax;
import quaternary.spaghettifactory.SpaghettiFactory;

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
			//TODO NORELEASE set this back at 30... it just keeps timing out when debugging loll
			pool.awaitTermination(300000, TimeUnit.SECONDS);
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
			throw new ModResolutionException("Forge mod resolution took too long!", e);
		}
		if (tookTooLong) {
			/// different error message
			throw new ModResolutionException("Forge mod resolution took too long!");
		}
		if (exception != null) {
			/// different error message
			throw new ModResolutionException("Forge mod resolution failed!", exception);
		}
		
		long time2 = System.currentTimeMillis();             ///different logger
		Map<String, ModCandidate> result = findCompatibleSet(SpaghettiFactory.LOGGER, candidatesById);
		
		long time3 = System.currentTimeMillis();
		///different logger
		SpaghettiFactory.LOGGER.debug("Forge mod resolution detection time: " + (time2 - time1) + "ms");
		SpaghettiFactory.LOGGER.debug("Forge mod resolution time: " + (time3 - time2) + "ms");
		
		return result;
	}
	
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
			Path jarPath;
			
			try {
				jarPath = UrlUtil.asPath(url).normalize();
			} catch(UrlConversionException e) {
				throw new RuntimeException("Failed to convert URL " + url + "!", e);
			}
			
			if(Files.isDirectory(jarPath)) {
				//TODO...?
				throw new IllegalArgumentException("Can't handle mods from directories right now: " + jarPath);
			} else {
				// JAR file
				try {
					///grab the forge mods.toml file
					jarFs = FileSystemUtil.getJarFileSystem(jarPath, false);
					//rootDir = jarFs.get().getRootDirectories().iterator().next();
				} catch(IOException e) {
					throw new RuntimeException("Failed to read Forge mods.toml for mod at " + jarPath + "!");
				}
			}
			
			///new: copy the mod into jimfs
			//this step takes care of transformation, too
			try {
				StagedForgeMod staged = ForgeModStager.stageMod(jarFs.get(), jarPath);
				ModCandidate candidate = new ModCandidate(staged.metadata, UrlUtil.asUrl(staged.pathToJar), depth);
				boolean added;
				
				if(candidate.getInfo().getId() == null || candidate.getInfo().getId().isEmpty()) {
					throw new RuntimeException(String.format("Mod file `%s` has no id", candidate.getOriginUrl().getFile()));
				}
				
				added = candidatesById.computeIfAbsent(candidate.getInfo().getId(), ModCandidateSet::new).add(candidate);
				
				if(!added) {
					SpaghettiFactory.LOGGER.debug(candidate.getOriginUrl() + " already present as " + candidate);
				} else {
					SpaghettiFactory.LOGGER.debug("Adding " + candidate.getOriginUrl() + " as " + candidate);
				}
			} catch(Exception e) {
				throw new RuntimeException("couldn't stage Forge mod at '" + jarPath + "'", e);
			}
		}
	}
}
