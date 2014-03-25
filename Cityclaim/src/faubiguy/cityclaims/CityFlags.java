package faubiguy.cityclaims;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

public class CityFlags extends Flags {

	public static final CityFlags DEFAULTS;
	public static final Map<String,String> FLAG_TYPES;
	
	public final City city;
	
	static {
		Map<String,Object> defaultsMap = new HashMap<>();
		defaultsMap.put  ("plotlimit", -1);
		defaultsMap.put("requireempty", true);
		defaultsMap.put("allowsell", true);
		defaultsMap.put("sellmultiplier", 1D);
		defaultsMap.put("playersell", true);
		defaultsMap.put("havetreasury", true);
		defaultsMap.put("hidden", false);
		DEFAULTS = new CityFlags(null, defaultsMap);
		
		FLAG_TYPES = new HashMap<>();
		FLAG_TYPES.put("plotlimit", "integer");
		FLAG_TYPES.put("requireempty", "boolean");
		FLAG_TYPES.put("allowsell", "boolean");
		FLAG_TYPES.put("sellmultiplier", "double");
		FLAG_TYPES.put("playersell", "boolean");
		FLAG_TYPES.put("havetreasury", "boolean");
		FLAG_TYPES.put("hidden", "boolean");
		
	}
	
	public Map<String,String> getFlagTypes() {
		return FLAG_TYPES;
	}

	public CityFlags(City city, Map<String, Object> map) { // Construct from Map of flags
		super(FLAG_TYPES);
		this.city = city; 
		flags = map;
	}

	public CityFlags(City city) { // Construct without any flags
		super(FLAG_TYPES);
		this.city = city;
		flags = new HashMap<>();
	}
	
	public boolean hasCity() {
		return city != null;
	}
	
	public List<String> listFlags(boolean includeLocked) {
		List<String> flagList = new ArrayList<String>();
		for (String flag : getFlagTypes().keySet()) {
			if (includeLocked || !CityClaims.instance.lockedFlags.contains(flag)) {
				flagList.add(flag);
			}
		}
		return flagList;
	}
	
	public Object getFlag(String flag) {
		return getFlag(flag, true);
	}

	public Object getFlag(String flag, boolean inherit) {		
		//CityClaims.instance.getLogger().info("Getting flag: " + flag);
		Object value = flags.get(flag);
		if (!inherit) {
			//CityClaims.instance.getLogger().info("Inherit is false");
			//CityClaims.instance.getLogger().info("Returning " + (value != null ? value.toString() : "null"));
			return value;
		}
		if (value == null) {
			//CityClaims.instance.getLogger().info("Checking server default");
			value = CityClaims.instance.defaults.getFlag(flag, false);
			if (value == null) {
				//CityClaims.instance.getLogger().info("Checking plugin default");
				value = CityFlags.DEFAULTS.getFlag(flag, false);
			}
		}
		//CityClaims.instance.getLogger().info("Returning (inherited)" + value.toString());
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
	
	public static void loadGlobalFlags() {
		FileConfiguration flagFile = YamlConfiguration
				.loadConfiguration(new File(CityClaims.instance.dataPath,
						"flags.yml"));
		if(flagFile.isConfigurationSection("flags")) {
			CityClaims.instance.defaults = new CityFlags(null, flagFile
					.getConfigurationSection("flags").getValues(false));
		} else {
			CityClaims.instance.defaults = new CityFlags(null);
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

	@Override
	public void save() {
		city.saveFlags();
		
	}

}
