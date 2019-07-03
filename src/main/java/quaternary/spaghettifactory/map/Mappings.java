package quaternary.spaghettifactory.map;

import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.Remapper;

import java.io.InputStream;

import static quaternary.spaghettifactory.map.Namespace.*;

public class Mappings {
	private static Remapper remapper;
	
	public static void buildRemapper(InputStream joinedTsrg) {
		Mapping<Srg, Intermediary> srg2Inter = srg(joinedTsrg).reversed().compose(intermediary());
		
		if(FabricLoader.getInstance().isDevelopmentEnvironment()) {
			remapper = srg2Inter.compose(yarn()).memoized().toRemapper();
		} else { 
			remapper = srg2Inter.memoized().toRemapper();
		}
	}
	
	public static ClassRemapper getRemapper(ClassVisitor parent) {
		if(remapper == null) throw new IllegalStateException("Didn't initialize the remapper!");
		
		return new ClassRemapper(parent, remapper);
	}
	
	public static Mapping<Original, Srg> srg(InputStream joinedTsrg) {
		return identity(); //TODO
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
