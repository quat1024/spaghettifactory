package quaternary.spaghettifactory.map;

import net.fabricmc.mappings.EntryTriple;

/**
 * SRG mappings do not include type information for fields.
 * Instead, I infer them from the Tiny-mapped fields with the same names (since they're all unique per class)
 * Incomplete SRG mapping triples are stored in this class.
 */
public class IncompleteEntryTriple extends EntryTriple {
	public IncompleteEntryTriple(String owner, String name) {
		super(owner, name, null);
	}
	
	public EntryTriple fillDesc(String descNew) {
		if(getDesc() == null)	return new EntryTriple(getOwner(), getName(), descNew);
		else return this;
	}
	
	public boolean isKindaEqual(EntryTriple other) {
		return other.getOwner().equals(getOwner()) && other.getName().equals(getName());
	}
	
	@Override
	public String toString() {
		return "Incomplete".concat(super.toString());
	}
	
	@Override
	public int hashCode() {
		return getOwner().hashCode() + 37 * getName().hashCode() + 696969;
	}
}
