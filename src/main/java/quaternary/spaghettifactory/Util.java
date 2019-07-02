package quaternary.spaghettifactory;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
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
