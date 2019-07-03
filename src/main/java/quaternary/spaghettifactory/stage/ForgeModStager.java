package quaternary.spaghettifactory.stage;

import com.google.common.collect.ImmutableMap;
import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Feature;
import com.google.common.jimfs.Jimfs;
import com.google.common.jimfs.PathType;
import net.fabricmc.loader.discovery.ModResolutionException;
import net.fabricmc.loader.metadata.LoaderModMetadata;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;
import quaternary.spaghettifactory.map.Mappings;

import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.stream.Collectors;

/**
 * Moves a forge mod into a jimfs filesystem.
 */
public class ForgeModStager {
	private static final FileSystem FORGE_FS = Jimfs.newFileSystem("spaghettiForgeStore",
		Configuration.builder(PathType.unix())
			.setRoots("/")
			.setWorkingDirectory("/")
			.setAttributeViews("basic")
			.setSupportedFeatures(Feature.SECURE_DIRECTORY_STREAM, Feature.FILE_CHANNEL)
			.build()
	);
	
	public static StagedForgeMod stageMod(FileSystem jarFs, Path srcJarPath) throws IOException, ModResolutionException {
		//step one: copy the forge mod jar into jimfs
		Path destJarPath = FORGE_FS.getPath(srcJarPath.getFileName().toString().concat("-spaghettifactory.jar"));
		
		//adding a jar: scheme because the original destjarpath has a jimfs: scheme
		try(FileSystem destZipFs = FileSystems.newFileSystem(URI.create("jar:" + destJarPath.toUri()), ImmutableMap.of("create", "true"), null)) {
			
			Files.walkFileTree(jarFs.getRootDirectories().iterator().next(), new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult visitFile(Path srcPath, BasicFileAttributes attrs) throws IOException {
					Path destPath = destZipFs.getPath(srcPath.toString());
					Files.createDirectories(destPath.getParent());
					
					//TODO stuff:
					//* more sophisticated class file processing (duh)
					//* copying assets/ and data/ directories in one unit (preVisitDirectory)
					//* more pandoras box opening
					if(srcPath.getFileName().toString().endsWith(".class")) {
						ClassReader reader = new ClassReader(Files.readAllBytes(srcPath));
						ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
						
						ClassVisitor remapper = Mappings.getRemapper(writer);
						reader.accept(remapper, 0);
						
						byte[] classs = writer.toByteArray();
						
						ClassReader lol = new ClassReader(classs);
						ClassNode ha = new ClassNode();
						lol.accept(ha, 0);
						
						Files.write(destPath, classs, StandardOpenOption.WRITE, StandardOpenOption.CREATE);
					} else {
						Files.copy(srcPath, destPath);
					}
					
					return FileVisitResult.CONTINUE;
				}
			});
		}
		
		//step two: generate a loadermodmetadata for this file
		//TODO is it better to literally just add a json file to the jar and let fabric handle it?
		LoaderModMetadata meta;
		
		try {
			Path modsToml = jarFs.getPath("META-INF", "mods.toml");
			meta = ForgeModMetadataParser.parseForgeMetadata(new StringReader(Files.lines(modsToml).collect(Collectors.joining("\n"))));
		} catch(Exception e) {
			throw new ModResolutionException("Cannot parse Forge mods.toml!");
		}
		
		return new StagedForgeMod(destJarPath, meta);
	}
}
