package quaternary.spaghettifactory;

import java.util.function.Supplier;

public class Util {
	public static <T> T getIfThrows(ExceptionalSupplier<T> supp, Supplier<T> def) {
		try {
			return supp.get();
		} catch(Exception e) {
			return def.get();
		}
	}
	
	public interface ExceptionalSupplier<T> {
		T get() throws Exception;
	}
}
