package quaternary.spaghettifactory.metadata;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonElement;
import com.moandjiezana.toml.Toml;
import com.sun.istack.internal.Nullable;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.Version;
import net.fabricmc.loader.api.metadata.ContactInformation;
import net.fabricmc.loader.api.metadata.ModDependency;
import net.fabricmc.loader.api.metadata.Person;
import net.fabricmc.loader.metadata.EntrypointMetadata;
import net.fabricmc.loader.metadata.LoaderModMetadata;
import net.fabricmc.loader.metadata.NestedJarEntry;
import quaternary.spaghettifactory.Util;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Attempts to parse a Forge mods.toml file into something resembling a standard Fabric metadata object.
 */
public class ForgeModMetadataParser {
	public static LoaderModMetadata[] getForgeMods(FabricLoader loader, InputStream in) {
		Toml toml = new Toml().read(in);
		
		//global stuff
		@Nullable String issueUrl = loader.getModContainer("spaghettifactory").flatMap(c -> c.getMetadata().getContact().get("issues")).orElse(null);
		
		//mods table
		if(Optional.ofNullable(toml.getTables("mods")).orElse(Collections.emptyList()).size() != 1) {
			//TODO
			throw new IllegalArgumentException("Can't handle jars with more or less than one mod in them currently!");
		}
		
		Toml modsTable = toml.getTables("mods").get(0);
		String modId = modsTable.getString("modId");
		String modVersion = modsTable.getString("version");
		String name = modsTable.getString("displayName", "");
		String homepage = modsTable.getString("displayUrl");
		Optional<String> logoFile = Optional.ofNullable(modsTable.getString("logoFile"));
		String authors = modsTable.getString("authors", "");
		String description = modsTable.getString("description", "");
		
		//TODO do fabric and forge use the same version schema?
		//I think it's both semver
		Version version = Util.getIfThrows(() -> Version.parse(modVersion), () -> () -> "Unknown version");
		
		//Forge just uses a text field for this. Try to parse it into a list of people
		List<Person> people = 
			Arrays.stream(authors.split("[ ,]"))
				.filter(s -> !s.isEmpty())
				.map(s -> new Person() {
					@Override
					public String getName() {
						return s;
					}
					
					@Override
					public ContactInformation getContact() {
						return ContactInformation.EMPTY;
					}
				})
				.collect(Collectors.toList());
		
		Map<String, String> contactMap = ImmutableMap.of(
			"homepage", homepage,
			"issues", issueUrl
		);
		
		ContactInformation contactInfo = new ContactInformation() {
			@Override
			public Optional<String> get(String key) {
				return Optional.ofNullable(contactMap.get(key));
			}
			
			@Override
			public Map<String, String> asMap() {
				return contactMap;
			}
		};
		
		return new LoaderModMetadata[]{ new LoaderModMetadata() {
			@Override
			public int getSchemaVersion() {
				return 1;
			}
			
			@Override
			public Map<String, String> getLanguageAdapterDefinitions() {
				return Collections.emptyMap();
			}
			
			@Override
			public Collection<NestedJarEntry> getJars() {
				//TODO: forge jar in jar
				return Collections.emptyList();
			}
			
			@Override
			public Collection<String> getMixinConfigs(EnvType type) {
				return Collections.emptyList(); //No mixins in Forge mods
			}
			
			@Override
			public boolean loadsInEnvironment(EnvType type) {
				return true; //TODO
			}
			
			@Override
			public Collection<String> getOldInitializers() {
				return Collections.emptyList();
			}
			
			@Override
			public List<EntrypointMetadata> getEntrypoints(String type) {
				//TODO Very important
				return Collections.emptyList();
			}
			
			@Override
			public Collection<String> getEntrypointKeys() {
				//TODO Very important
				return Collections.emptyList();
			}
			
			@Override
			public String getType() {
				return "spaghettifactory-forge";
			}
			
			@Override
			public String getId() {
				return modId;
			}
			
			@Override
			public Version getVersion() {
				return version;
			}
			
			@Override
			public Collection<ModDependency> getDepends() {
				//TODO: parse dependencies
				return Collections.emptyList();
			}
			
			@Override
			public Collection<ModDependency> getRecommends() {
				return Collections.emptyList();
			}
			
			@Override
			public Collection<ModDependency> getSuggests() {
				return Collections.emptyList();
			}
			
			@Override
			public Collection<ModDependency> getConflicts() {
				return Collections.emptyList();
			}
			
			@Override
			public Collection<ModDependency> getBreaks() {
				return Collections.emptyList();
			}
			
			@Override
			public String getName() {
				return name;
			}
			
			@Override
			public String getDescription() {
				return description;
			}
			
			@Override
			public Collection<Person> getAuthors() {
				return people;
			}
			
			@Override
			public Collection<Person> getContributors() {
				return Collections.emptyList();
			}
			
			@Override
			public ContactInformation getContact() {
				return contactInfo;
			}
			
			@Override
			public Collection<String> getLicense() {
				//The Forge spec doesn't include this, to my knowledge...?
				return ImmutableList.of("(Unknown)");
			}
			
			@Override
			public Optional<String> getIconPath(int size) {
				return logoFile;
			}
			
			@Override
			public boolean containsCustomElement(String key) {
				return false;
			}
			
			@Override
			public JsonElement getCustomElement(String key) {
				return null;
			}
		}};
	}
}
