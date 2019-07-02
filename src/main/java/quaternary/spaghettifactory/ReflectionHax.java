package quaternary.spaghettifactory;

import net.fabricmc.loader.FabricLoader;
import net.fabricmc.loader.ModContainer;
import net.fabricmc.loader.discovery.ModCandidate;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

public class ReflectionHax {
	private static final MethodHandles.Lookup lookie = MethodHandles.lookup();
	
	public static <T> T get(Class classs, Object inst, String fieldName) {
		try {
			Field field = classs.getDeclaredField(fieldName);
			if(!field.isAccessible()) field.setAccessible(true);
			return (T) field.get(inst);
		} catch(Exception e) {
			throw new RuntimeException("Reflection machine broke!", e);
		}
	}
	
	public static <T> void set(Class classs, Object inst, String fieldName, T thing) {
		try {
			Field field = classs.getDeclaredField(fieldName);
			if(!field.isAccessible()) field.setAccessible(true);
			field.set(inst, thing);
		} catch(Exception e) {
			throw new RuntimeException("Reflection machine broke!", e);
		}
	}
	
	public static <T> void map(Class classs, Object inst, String fieldName, UnaryOperator<T> mapper) {
		set(classs, inst, fieldName, mapper.apply(get(classs, inst, fieldName)));
	}
	
	public static void doUnfrozen(FabricLoader loader, Consumer<FabricLoader> action) {
		boolean needsHax = isFrozen(loader);
		
		if(needsHax) haxLoaderFrozenState(loader, false);
		action.accept(SpaghettiFactory.FABRIC_LOADER);
		if(needsHax) haxLoaderFrozenState(loader, true);
	}
	
	public static void haxLoaderFrozenState(FabricLoader loader, boolean frozen) {
		set(FabricLoader.class, loader, "frozen", frozen);
	}
	
	public static boolean isFrozen(FabricLoader loader) {
		return get(FabricLoader.class, loader, "frozen");
	}
	
	public static final MethodHandle addMod = getAHandleOnLife(FabricLoader.class, "addMod", ModCandidate.class);
	public static final MethodHandle instantiate = getAHandleOnLife(ModContainer.class, "instantiate");
	
	public static void addMod(FabricLoader loader, ModCandidate cand) {
		try {
			addMod.invokeExact(loader, cand);
		} catch(Throwable e) {
			throw new RuntimeException("Trouble adding a mod", e);
		}
	}
	
	public static void instantiateModContainer(ModContainer cont) {
		try {
			instantiate.invokeExact(cont);
		} catch(Throwable e) {
			throw new RuntimeException("Trouble instantiating a mod", e);
		}
	}
	
	public static MethodHandle getAHandleOnLife(Class<?> classs, String name, Class<?>... argTypes) {
		try {
			Method woop = classs.getDeclaredMethod(name, argTypes);
			woop.setAccessible(true);
			return lookie.unreflect(woop);
		} catch(Exception e) {
			throw new RuntimeException("Trouble unreflecting a MethodHandle", e);
		}
	}
}
