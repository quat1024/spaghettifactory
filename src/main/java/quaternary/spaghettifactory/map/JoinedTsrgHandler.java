package quaternary.spaghettifactory.map;

import net.fabricmc.loader.api.FabricLoader;
import quaternary.spaghettifactory.SpaghettiFactory;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

public class JoinedTsrgHandler {
	public static InputStream getInputStream(FabricLoader loader) {
		try {
			if(Files.exists(SpaghettiFactory.JOINED_TSRG_FILE.toPath())) {
				return new BufferedInputStream(new FileInputStream(SpaghettiFactory.JOINED_TSRG_FILE));
			}
		} catch(Exception e) {
			throw new RuntimeException("Can't read joined.tsrg file!", e);
		}
		
		//TODO download it, if it's not there
		
		throw new RuntimeException("joined.tsrg file not found, please put it in " + SpaghettiFactory.JOINED_TSRG_FILE.toString());
	}
}
