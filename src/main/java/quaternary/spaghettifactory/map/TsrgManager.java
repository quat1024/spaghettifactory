package quaternary.spaghettifactory.map;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableList;
import com.google.gson.internal.Streams;
import com.sun.xml.internal.ws.util.StreamUtils;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.mappings.EntryTriple;
import org.apache.commons.lang3.mutable.MutableInt;
import org.objectweb.asm.commons.Remapper;
import org.objectweb.asm.commons.SignatureRemapper;
import org.objectweb.asm.signature.SignatureReader;
import org.objectweb.asm.signature.SignatureWriter;
import quaternary.spaghettifactory.SFConfig;
import quaternary.spaghettifactory.Util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class TsrgManager {
	public static Path pathToTsrg(FabricLoader loader) {
		Path path = SFConfig.getPathToJoinedTsrg(loader);
		if(Files.exists(path)) {
			return path; //the file exists!
		} else {
			//TODO, download the file
			throw new RuntimeException("Can't find joined.tsrg file at '" + path.toAbsolutePath() + "', please download it!");
		}
	}
	
	public static Mapping<Namespace.Original, Namespace.Srg> readMappingFile(Path joinedTsrgPath) throws IOException {
		//A layer of indirection to work around Java's crappy variable capture support
		class Box<T> {
			public Box(T value) { this.value = value; }
			T value;
		}
		
		class ClassMapping {
			public ClassMapping(String fromTsrg) {
				String[] split = fromTsrg.split(" ");
				og = split[0];
				srg = split[1];
			}
			
			String og; //The real OG mapping, straight from Proguard
			String srg;//The srg name
		}
		
		BiMap<String, String> classMappings = HashBiMap.create();
		BiMap<EntryTriple, EntryTriple> methodMappings = HashBiMap.create();
		BiMap<EntryTriple, EntryTriple> fieldMappings = HashBiMap.create();
		
		Map<ClassMapping, List<String>> linesGroupedByClass = new HashMap<>();
		
		try(Stream<String> lines = Files.lines(joinedTsrgPath)) {
			//First pass: gather class mappings.
			//Also associate field/method mapping lines with the class they belong to
			Box<ClassMapping> lastClassMapping = new Box<>(null);
			lines.forEach(line -> {
				if(line.isEmpty()) return;
				if(line.charAt(0) != '\t') {
					ClassMapping cmap = new ClassMapping(line);
					lastClassMapping.value = cmap;
					classMappings.put(cmap.og, cmap.srg);
				} else {
					linesGroupedByClass.computeIfAbsent(lastClassMapping.value, (x) -> new ArrayList<>()).add(line);
				}
			});
		}
		
		//A method signature remapper based on the class data:
		Remapper classRemapper = new Remapper() {
			@Override
			public String map(String internalName) {
				return Mapping.FROM_INTERNAL_NAME.rmap(internalName, extName -> classMappings.getOrDefault(extName, extName));
			}
		};
		
		//Second pass: create field and method mappings for each class
		for(Map.Entry<ClassMapping, List<String>> entry : linesGroupedByClass.entrySet()) {
			String ownerOg = entry.getKey().og;
			String ownerSrg = entry.getKey().srg;
			
			for(String line : entry.getValue()) {
				String[] data = line.split(" ");
				//How many fields are in this record determines whether it's a method or field mapping
				if(data.length == 2) {
					String fieldOg = data[0];
					String fieldSrg = data[1];
					
					fieldMappings.put(
						new IncompleteEntryTriple(ownerOg, fieldOg),
						new IncompleteEntryTriple(ownerSrg, fieldSrg)
					);
				} else {
					String methodOg = data[0];
					String methodOgSig = data[1];
					String methodSrg = data[2];
					
					//Visitor pattern is always perpetually confusing. Ah well.
					SignatureWriter sigWriter = new SignatureWriter();
					SignatureRemapper sigRemapper = new SignatureRemapper(sigWriter, classRemapper);
					SignatureReader sigReader = new SignatureReader(methodOgSig);
					sigReader.accept(sigRemapper);
					
					methodMappings.put(
						new EntryTriple(ownerOg, methodOg, methodOgSig),
						new EntryTriple(ownerSrg, methodSrg, sigWriter.toString())
					);
				}
			}
		}
		
		return new Mapping.Builder<Namespace.Original, Namespace.Srg>()
			.classes(Util.defaultedBijectionFrom(classMappings))
			.methods(Util.defaultedBijectionFrom(methodMappings))
			.fields(Util.defaultedBijectionFrom(fieldMappings))
			.build();
	}
}
