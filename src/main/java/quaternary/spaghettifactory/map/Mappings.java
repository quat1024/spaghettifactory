package quaternary.spaghettifactory.map;

import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.Remapper;

import java.io.IOException;
import java.nio.file.Path;

import static quaternary.spaghettifactory.map.Namespace.*;

public class Mappings {
	private static Remapper remapper;
	
	public static void buildRemapper(Path joinedTsrgPath) throws IOException {
		Mapping<Srg, Intermediary> srgToIntermed = srg(joinedTsrgPath).reversed().compose(intermediary());
		Mapping<Srg, ?> mapping = FabricLoader.getInstance().isDevelopmentEnvironment() ? srgToIntermed.compose(yarn()) : srgToIntermed;
		
		remapper = mapping.memoized().toRemapper();
	}
	
	public static ClassRemapper getRemapper(ClassVisitor parent) {
		if(remapper == null) throw new IllegalStateException("Didn't initialize the remapper!");
		else return new ClassRemapper(parent, remapper);
	}
	
	public static Mapping<Original, Srg> srg(Path joinedTsrgPath) throws IOException {
		return TsrgManager.readMappingFile(joinedTsrgPath);
	}
	
	public static Mapping<Original, Intermediary> intermediary() {
		return identity(); //TODO
	}
	
	public static Mapping<Intermediary, Yarn> yarn() {
		return identity(); //TODO
	}
	
	public static <X extends Namespace, Y extends Namespace> Mapping<X, Y> identity() {
		return Mapping.identity();
	}
}
