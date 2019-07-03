package quaternary.spaghettifactory;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.FabricLoader;
import net.fabricmc.loader.ModContainer;
import net.fabricmc.loader.discovery.DirectoryModCandidateFinder;
import net.fabricmc.loader.discovery.ModCandidate;
import net.fabricmc.loader.discovery.ModResolutionException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import quaternary.spaghettifactory.map.JoinedTsrgHandler;
import quaternary.spaghettifactory.map.Mappings;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class SpaghettiFactory implements ModInitializer {
	//Need the internal one (not the one in the api package), to get at all the fun internal methods.
	@SuppressWarnings("deprecation")
	public static FabricLoader FABRIC_LOADER = FabricLoader.INSTANCE;
	
	public static final Logger LOGGER = LogManager.getFormatterLogger("Spaghetti Factory");
	
	public static final File JOINED_TSRG_FILE = new File(FABRIC_LOADER.getGameDirectory(), "joined.tsrg").getAbsoluteFile();
	
	@Override
	public void onInitialize() {
		LOGGER.info("Hello world!");
		LOGGER.info("Preparing mappings transformer...");
		
		try(InputStream joinedTsrgInput = JoinedTsrgHandler.getInputStream(FABRIC_LOADER)) {
			Mappings.buildRemapper(joinedTsrgInput);
		} catch(IOException e) {
			throw new RuntimeException("hmm", e);
		}
		
		LOGGER.info("Discovering Forge mods...");
		
		//Modeled after FabricLoader#load
		ForgeModResolver resolver = new ForgeModResolver();
		resolver.addCandidateFinder(new DirectoryModCandidateFinder(getForgeModsFile().toPath()));
		//TODO classpath
		Map<String, ModCandidate> candidateMap;
		try {
			candidateMap = resolver.resolve(FABRIC_LOADER);
		} catch(ModResolutionException e) {
			throw new RuntimeException("SpaghettiFactory failed to resolve Forge mods", e);
		}
		
		if(candidateMap.isEmpty()) {
			LOGGER.info("Did not find any Forge mods, exiting.");
			LOGGER.info("SpaghettiFactory mixins will still be present, so");
			LOGGER.info("remove this mod if you don't want to use Forge mods.");
			return;
		}
		
		
		String modText;
		switch (candidateMap.values().size()) {
			case 1:
				modText = "Loading %d Forge mod: %s";
				break;
			default:
				modText = "Loading %d Forge mods: %s";
				break;
		}
		
		//Straight cut and paste from FabricLoader#load, basically. I like this log message.
		LOGGER.info(modText, candidateMap.values().size(), candidateMap.values().stream()
			.map(info -> String.format("%s@%s", info.getInfo().getId(), info.getInfo().getVersion().getFriendlyString()))
			.collect(Collectors.joining(", ")));
		
		//Inject these mods into fabric loader.
		ReflectionHax.doUnfrozen(FABRIC_LOADER, loader -> {
			LOGGER.info("Telling Fabric Loader about the discovered mods...");
			for(ModCandidate cand : candidateMap.values()) {
				ReflectionHax.addMod(loader, cand);
			}
			
			LOGGER.info("Initializing discovered Forge mods... Hold on to your butts!");
			for(ModCandidate cand : candidateMap.values()) {
				loader.getModContainer(cand.getInfo().getId()).ifPresent(c -> {
					if(c instanceof ModContainer) {
						ReflectionHax.instantiateModContainer((ModContainer) c);
					} else {
						throw new IllegalStateException("Fabric Loader guts changed relating to ModContainers, can't continue!");
					}
				});
			}
			
			LOGGER.info("Done initializing.");
		});
	}
	
	public static File getForgeModsFile() {
		return new File(FABRIC_LOADER.getGameDirectory(), "mods-sf-forge");
	}
	
	public static void info(String msg, Object... args) {
		LOGGER.info(msg, args);
	}
	
	public static void debug(String msg, Object... args) {
		LOGGER.debug(msg, args);
	}
}
