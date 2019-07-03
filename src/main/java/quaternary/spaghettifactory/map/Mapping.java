package quaternary.spaghettifactory.map;

import com.google.common.base.Preconditions;
import net.fabricmc.mappings.EntryTriple;
import org.objectweb.asm.commons.Remapper;

public class Mapping<SRC extends Namespace, DST extends Namespace> {
	public Mapping(Bijection<String, String> classMapper, Bijection<EntryTriple, EntryTriple> methodMapper, Bijection<EntryTriple, EntryTriple> fieldMapper) {
		this.classMapper = classMapper;
		this.methodMapper = methodMapper;
		this.fieldMapper = fieldMapper;
	}
	
	public static <SRC extends Namespace, DST extends Namespace> Mapping<SRC, DST> identity() {
		return new Mapping<>(Bijection.identity(), Bijection.identity(), Bijection.identity());
	}
	
	private final Bijection<String, String> classMapper;
	private final Bijection<EntryTriple, EntryTriple> methodMapper;
	private final Bijection<EntryTriple, EntryTriple> fieldMapper;
	
	public Mapping<DST, SRC> reversed() {
		return new Mapping<>(classMapper.reverse(), methodMapper.reverse(), fieldMapper.reverse());
	}
	
	public <NEWDST extends Namespace> Mapping<SRC, NEWDST> compose(Mapping<DST, NEWDST> other) {
		return new Mapping<>(classMapper.compose(other.classMapper), methodMapper.compose(other.methodMapper), fieldMapper.compose(other.fieldMapper));
	}
	
	public Mapping<SRC, DST> memoized() {
		return new Mapping<>(classMapper.memoized(), methodMapper.memoized(), fieldMapper.memoized());
	}
	
	public static final Bijection<String, String> FROM_INTERNAL_NAME = Bijection.of(s -> s.replace('/', '.'), s -> s.replace('.', '/'));
	
	public Remapper toRemapper() {
		return new Remapper() {
			@Override
			public String map(String internalName) {
				return FROM_INTERNAL_NAME.rmap(internalName, classMapper::to);
			}
			
			@Override
			public String mapFieldName(String owner, String name, String descriptor) {
				return fieldMapper.to(new EntryTriple(FROM_INTERNAL_NAME.to(owner), name, descriptor)).getName();
			}
			
			@Override
			public String mapMethodName(String owner, String name, String descriptor) {
				return methodMapper.to(new EntryTriple(FROM_INTERNAL_NAME.to(owner), name, descriptor)).getName();
			}
		};
	}
	
	public static class Builder<SRC extends Namespace, DST extends Namespace> {
		public Builder() {}
		
		private Bijection<String, String> classMapper;
		private Bijection<EntryTriple, EntryTriple> methodMapper;
		private Bijection<EntryTriple, EntryTriple> fieldMapper;
		
		public Builder<SRC, DST> classes(Bijection<String, String> classMapper) {
			this.classMapper = classMapper;
			return this;
		}
		
		public Builder<SRC, DST> methods(Bijection<EntryTriple, EntryTriple> methodMapper) {
			this.methodMapper = methodMapper;
			return this;
		}
		
		public Builder<SRC, DST> fields(Bijection<EntryTriple, EntryTriple> fieldMapper) {
			this.fieldMapper = fieldMapper;
			return this;
		}
		
		public Mapping<SRC, DST> build() {
			Preconditions.checkNotNull(classMapper, "no class mappings");
			Preconditions.checkNotNull(methodMapper, "no method mappings");
			Preconditions.checkNotNull(fieldMapper, "no field mappings");
			return new Mapping<>(classMapper, methodMapper, fieldMapper);
		}
	}
}
