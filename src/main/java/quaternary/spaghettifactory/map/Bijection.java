package quaternary.spaghettifactory.map;

import com.google.common.collect.HashBiMap;

import java.util.function.Function;
import java.util.function.UnaryOperator;

/**
 * A function that "goes both ways".
 * Establishes a one-to-one relationship.
 * 
 * Laws:
 * For all X, from(to(x)).equals(x) holds.
 * For all Y, to(from(y)).equals(y) holds.
 * It follows that to(X) never returns the same Y for different X, and vice versa for from(Y).
 * 
 * @param <X> the source type
 * @param <Y> the target type
 */
public interface Bijection<X, Y> {
	/**
	 * Apply the function forwards.
	 */
	Y to(X x);
	
	/**
	 * Apply the function backwards.
	 */
	X from(Y y);
	
	/**
	 * A bijection that never changes its argument to either method.
	 */
	static <T> Bijection<T, T> identity() {
		return of(Function.identity(), Function.identity());
	}
	
	/**
	 * Factory method, syntax sugar, exactly the same as defining your own anonymous class, but better!
	 */
	static <X, Y> Bijection<X, Y> of(Function<X, Y> applier, Function<Y, X> unapplier) {
		return new Bijection<X, Y>() {
			@Override
			public Y to(X x) {
				return applier.apply(x);
			}
			
			@Override
			public X from(Y y) {
				return unapplier.apply(y);
			}
		};
	}
	
	/**
	 * Returns the inverse of this bijection. Note the type signature.
	 * Since the two functions of a bijection are (by definition) inverses of each other,
	 * all this does is switch "to" and "from".
	 */
	default Bijection<Y, X> reverse() {
		Bijection<X, Y> capturedThis = this;
		
		return new Bijection<Y, X>() {
			@Override
			public X to(Y y) {
				return capturedThis.from(y);
			}
			
			@Override
			public Y from(X x) {
				return capturedThis.to(x);
			}
			
			@Override
			public Bijection<X, Y> reverse() {
				return capturedThis;
			}
		};
	}
	
	/**
	 * Compose the provided bijection to the right of this one ("after" on to, "before" on from)
	 */
	default <B> Bijection<X, B> compose(Bijection<Y, B> other) {
		return of(x -> other.to(to(x)), b -> from(other.from(b)));
	}
	
	/**
	 * Compose the provided bijection to the left of this one ("before" on to, "after" on from)
	 */
	default <A> Bijection<A, Y> precompose(Bijection<A, X> other) {
		return of(a -> to(other.to(a)), y -> other.from(from(y)));
	}
	
	/**
	 * Why the fuck not?
	 */
	default <A, B> Bijection<A, B> dicompose(Bijection<A, X> left, Bijection<Y, B> right) {
		return of(a -> right.to(to(left.to(a))), b -> left.from(from(right.from(b))));
	}
	
	/**
	 * Apply a Y -> Y function to the image of X as seen through the bijection.
	 */
	default X rmap(X x, UnaryOperator<Y> func) {
		return from(func.apply(to(x)));
	}
	
	/**
	 * Apply an X -> X function to the image of Y as seen through the bijection.
	 */
	default Y lmap(Y y, UnaryOperator<X> func) {
		return to(func.apply(from(y)));
	}
	
	/**
	 * Convert this bijection into one that only computes each function once for a given input.
	 */
	default Bijection<X, Y> memoized() {
		HashBiMap<X, Y> cache = HashBiMap.create();
		return Bijection.of(x -> cache.computeIfAbsent(x, this::to), y -> cache.inverse().computeIfAbsent(y, this::from));
	}
}
