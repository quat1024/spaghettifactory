package quaternary.spaghettifactory.map;

import com.google.common.collect.HashBiMap;

import java.util.function.Function;
import java.util.function.UnaryOperator;

public interface Bijection<X, Y> {
	Y apply(X x);
	X unapply(Y y);
	
	static <T> Bijection<T, T> identity() {
		return of(Function.identity(), Function.identity());
	}
	
	static <X, Y> Bijection<X, Y> of(Function<X, Y> applier, Function<Y, X> unapplier) {
		return new Bijection<X, Y>() {
			@Override
			public Y apply(X x) {
				return applier.apply(x);
			}
			
			@Override
			public X unapply(Y y) {
				return unapplier.apply(y);
			}
		};
	}
	
	default Bijection<Y, X> reverse() {
		Bijection<X, Y> capturedThis = this;
		
		return new Bijection<Y, X>() {
			@Override
			public X apply(Y y) {
				return capturedThis.unapply(y);
			}
			
			@Override
			public Y unapply(X x) {
				return capturedThis.apply(x);
			}
			
			@Override
			public Bijection<X, Y> reverse() {
				return capturedThis;
			}
		};
	}
	
	//x   -apply--> y   -other.apply--> z
	//x <--unapp-   y <--other.unapp-   z
	default <Z> Bijection<X, Z> compose(Bijection<Y, Z> other) {
		return Bijection.of(x -> other.apply(apply(x)), z -> unapply(other.unapply(z)));
	}
	
	default X map(X x, UnaryOperator<Y> func) {
		return unapply(func.apply(apply(x)));
	}
	
	default Y unmap(Y y, UnaryOperator<X> func) {
		return apply(func.apply(unapply(y)));
	}
	
	default Bijection<X, Y> memoized() {
		HashBiMap<X, Y> cache = HashBiMap.create();
		return Bijection.of(x -> cache.computeIfAbsent(x, this::apply), y -> cache.inverse().computeIfAbsent(y, this::unapply));
	}
}
