package quaternary.spaghettifactory.stage;

import net.fabricmc.loader.metadata.LoaderModMetadata;

import java.nio.file.Path;

public class StagedForgeMod {
	public StagedForgeMod(Path pathToJar, LoaderModMetadata metadata) {
		this.pathToJar = pathToJar;
		this.metadata = metadata;
	}
	
	public final Path pathToJar;
	public final LoaderModMetadata metadata;
	
	public Path getPathToJar() {
		return pathToJar;
	}
	
	public LoaderModMetadata getMetadata() {
		return metadata;
	}
}
