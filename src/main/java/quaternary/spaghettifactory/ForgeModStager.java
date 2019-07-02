package quaternary.spaghettifactory;

import com.google.common.collect.ImmutableMap;
import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Feature;
import com.google.common.jimfs.Jimfs;
import com.google.common.jimfs.PathType;
import com.sun.javafx.scene.shape.PathUtils;
import com.sun.nio.zipfs.ZipFileSystem;
import com.sun.nio.zipfs.ZipFileSystemProvider;
import it.unimi.dsi.fastutil.io.FastBufferedInputStream;
import net.fabricmc.loader.util.FileSystemUtil;
import net.fabricmc.loader.util.UrlConversionException;
import net.fabricmc.loader.util.UrlUtil;
import org.apache.commons.io.FileUtils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URL;
import java.nio.file.CopyOption;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipFile;

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
	
	public static Path stageMod(FileSystem diskFs, Path srcJarPath) throws IOException {
		Path destJarPath = FORGE_FS.getPath(srcJarPath.getFileName().toString().concat("-spaghettifactory.jar"));
		
		//adding a jar: scheme because the original destjarpath has a jimfs: scheme
		FileSystem destZipFs = FileSystems.newFileSystem(URI.create("jar:" + destJarPath.toUri()), ImmutableMap.of("create", "true"), null);
		
		Files.walkFileTree(diskFs.getRootDirectories().iterator().next(), new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult visitFile(Path srcPath, BasicFileAttributes attrs) throws IOException {
				Path destPath = destZipFs.getPath(srcPath.toString());
				
				//TODO stuff:
				//* more sophisticated class file processing (duh)
				//* copying assets/ and data/ directories in one unit (preVisitDirectory)
				//* more pandoras box opening
				
				Files.createDirectories(destPath);
				Files.copy(srcPath, destPath, StandardCopyOption.REPLACE_EXISTING);
				
				return FileVisitResult.CONTINUE;
			}
		});
		
		destZipFs.close();
		
		return destJarPath;
	}
}
