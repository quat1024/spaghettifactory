package quaternary.spaghettifactory;

import com.google.common.collect.BiMap;
import quaternary.spaghettifactory.map.Bijection;

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
	
	public interface ExceptionalFunction<A, B> {
		B apply(A a) throws Exception;
	}
	
	public static <X> Bijection<X, X> defaultedBijectionFrom(BiMap<X, X> bimap) {
		return Bijection.of(
			x -> bimap.getOrDefault(x, x),
			x -> bimap.inverse().getOrDefault(x, x)
		);
	}
}
