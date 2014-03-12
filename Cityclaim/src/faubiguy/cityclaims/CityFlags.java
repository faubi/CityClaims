package faubiguy.cityclaims;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

public class CityFlags {

	public static final CityFlags DEFAULTS = new CityFlags(-1, // plotlimit
			true, // requireempty
			true, // allowsell
			1, // sellmultiplier
			true, // playersell
			true, // allowwarp
			true // havetreasury
	);

	private Map<String, Object> flags;

	public CityFlags(Map<String, Object> map) { // Construct from Map of flags
		flags = map;
	}

	public CityFlags() { // Construct without any flags
		flags = new HashMap<>();
	}

	public CityFlags(int plotlimit, boolean requireempty, boolean allowsell,
			double sellmultiplier, boolean playersell, boolean allowwarp,
			boolean havetreasury) { // Construct with flags as parameters
		flags = new HashMap<>();
		flags.put("plotlimit", plotlimit);
		flags.put("requireempty", requireempty);
		flags.put("allowsell", allowsell);
		flags.put("sellmultiplier", sellmultiplier);
		flags.put("playersell", playersell);
		flags.put("allowwarp", allowwarp);
		flags.put("havetreasury", havetreasury);
	}

	public Map<String, Object> getMap() {
		return flags;
	}

	public Object getFlag(String flag) {
		return getFlag(flag, true);
	}

	public Object getFlag(String flag, boolean inherit) {
		Object value = flags.get(flag);
		if (!inherit) {
			return value;
		}
		if (value == null) {
			value = CityClaims.instance.defaults.getFlag(flag, false);
			if (value == null) {
				value = CityFlags.DEFAULTS.getFlag(flag, false);
			}
		}
		return value;
	}

	public boolean setFlag(String flag, Object value) {
		return setFlag(flag, value, false);
	}

	public boolean setFlag(String flag, Object value, boolean overrideLock) {
		if (CityClaims.instance.lockedFlags.contains(flag) && !overrideLock
				&& flags.get(flag) == null) {
			return false;
		}
		flags.put(flag, value);
		return true;
	}

	public void removeFlag(String flag) {
		flags.remove(flag);
	}

	public static void loadGlobalFlags() {
		FileConfiguration flagFile = YamlConfiguration
				.loadConfiguration(new File(CityClaims.instance.dataPath,
						"flags.yml"));
		if(flagFile.isConfigurationSection("flags")) {
			CityClaims.instance.defaults = new CityFlags(flagFile
					.getConfigurationSection("flags").getValues(false));
		} else {
			CityClaims.instance.defaults = new CityFlags();
		}
		if (flagFile.isConfigurationSection("locked")) {
			CityClaims.instance.lockedFlags = new HashSet<String>(flagFile
					.getConfigurationSection("locked").getStringList(""));
		} else {
			CityClaims.instance.lockedFlags = new HashSet<>();
		}
	}

	public static boolean saveGlobalFlags() {
		FileConfiguration flagFile = YamlConfiguration
				.loadConfiguration(new File(CityClaims.instance.dataPath,
						"flags.yml"));
		flagFile.createSection("flags", CityClaims.instance.defaults.getMap());
		flagFile.set("locked", new ArrayList<>(CityClaims.instance.lockedFlags));
		try {
			flagFile.save(new File(CityClaims.instance.dataPath, "flags.yml"));
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

}
