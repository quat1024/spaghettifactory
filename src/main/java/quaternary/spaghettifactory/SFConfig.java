package quaternary.spaghettifactory;

import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Path;
import java.nio.file.Paths;

public class SFConfig {
	public static Path RELATIVE_PATH_TO_FORGE_MODS = Paths.get("mods-sf-forge");
	public static Path RELATIVE_PATH_TO_JOINED_TSRG = RELATIVE_PATH_TO_FORGE_MODS.resolve(Paths.get("joined.tsrg"));
	
	public static Path getPathToForgeMods(FabricLoader loader) {
		return loader.getGameDirectory().toPath().resolve(RELATIVE_PATH_TO_FORGE_MODS);
	}
	
	public static Path getPathToJoinedTsrg(FabricLoader loader) {
		return loader.getGameDirectory().toPath().resolve(RELATIVE_PATH_TO_JOINED_TSRG);
	}
}
