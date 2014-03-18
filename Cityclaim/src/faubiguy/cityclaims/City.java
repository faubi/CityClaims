package faubiguy.cityclaims;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.GriefPrevention;

public class City {

	public static Map<String, City> cities;

	public static Map<Long, String> names;

	public Claim base;
	public CityFile file;
	public CityFlags flags;
	public Set<PlotType> types;
	public Map<Long, Plot> plots;
	public Map<PlotSize, String> sizeTypes;

	public long id;

	public double treasury;

	public String name;

	protected City(String name) {
		this.name = name;
	}

	public void save() {
		getFile().saveCity();
	}

	public boolean updatePlot(Plot plot) {
		return getFile().savePlot(plot, true);
	}

	static City getCity(Location loc) {
		return getCity(GriefPrevention.instance.dataStore.getClaimAt(loc, true,
				null));
	}

	static City getCity(Claim claim) {
		if (claim == null) {
			return null;
		}
		if (claim.parent != null) {
			claim = claim.parent;
		}
		return getCity(claim.getID());
	}

	static City getCity(long id) {
		String name = names.get(id);
		if (name == null) {
			return null;
		}
		return getCity(name);
	}

	static City getCity(String name) {
		return cities.get(name);
	}

	static void loadCity(String name) throws CityLoadingException {
		City city = new City(name);
		CityFile file = new CityFile(city, false);
		if (!file.exists()) {
			throw new CityLoadingException("No such city: " + name, name);
		}
		file.loadFile();
		file.loadCity();
		cities.put(name, city);
	}

	public static String newCity(String name, Claim base) {
		City city = new City(name);
		if (city.exists()) { // City already exists
			return "There is already a city with that name.";
		}
		if (getCity(base) != null) {
			return "That claim is already being used for a city.";
		}
		if (base.parent != null) { // Claim is a subdivision
			return "That claim is a subdivision.";
		}
		city.name = name;
		city.base = base;
		city.flags = new CityFlags();
		city.types = new HashSet<>();
		city.plots = new HashMap<>();
		city.sizeTypes = new HashMap<>();
		city.id = base.getID();
		city.treasury = 0;
		city.save();
		cities.put(name, city);
		names.put(base.getID(), name);
		saveIDFile();
		return null;
	}

	public void delete() {
		cities.remove(name);
		names.remove(getID());
		saveIDFile();
		getFile(false).delete();
	}

	public Plot getPlot(long id) {
		return plots.get(id);
	}

	public Plot getPlot(String name) {
		for (Plot plot : plots.values()) {
			if (plot.name == name) {
				return plot;
			}
		}
		return null;
	}

	public PlotType getType(PlotSize size) {
		return getType(sizeTypes.get(size));
	}

	public PlotType getType(String name) {
		for (PlotType type : types) {
			if (type.name == name) {
				return type;
			}
		}
		return null;
	}

	private CityFile getFile() {
		return getFile(true);
	}

	private CityFile getFile(boolean load) {
		if (file != null) {
			return file;
		}
		return new CityFile(this, load);
	}

	public boolean exists() {
		return getFile(false).exists();
	}

	public long getID() {
		return id;
	}

	public void addType(PlotType type) {
		if (types.add(type)) {
			getFile().saveType(type, true);
		}
	}

	public void removeType(PlotType type) {
		if (types.remove(type)) {
			getFile().saveTypes(true);
		}
	}

	public void removeType(String name) {
		PlotType type = getType(name);
		if (type != null) {
			removeType(type);
		}
	}

	public void addSizeType(PlotSize size, String typeName) {
		sizeTypes.put(size, typeName);
		getFile().saveSizeTypes(true);
	}

	public void addSizeType(PlotSize size, PlotType type) {
		addSizeType(size, type.name);
	}

	public void removeSizeType(PlotSize size) {
		if (sizeTypes.remove(size) != null) {
			getFile().saveSizeTypes(true);
		}
	}

	public String setFlag(String flag, Object value, boolean overrideLock) {
		if (!flags.setFlag(flag, value, overrideLock)) {
			return "That flag has been globally locked";
		}
		getFile().saveFlags(true);
		return null;
	}

	public String setFlag(String flag, Object value) {
		return setFlag(flag, value, false);
	}

	public void removeFlag(String flag) {
		flags.removeFlag(flag);
		getFile().saveFlags(true);
	}

	public void setName(String newName) {
		name = newName;
		names.put(getID(), newName);
		saveIDFile();
		getFile().rename(newName);
	}

	public static String loadIDFile() {
		FileConfiguration idFile = YamlConfiguration
				.loadConfiguration(new File(CityClaims.instance.dataPath,
						"ids.yml"));
		Map<Long, String> idMap = new HashMap<>();
		Map<String, Object> loadedMap = idFile.getValues(false);
		try {
			for (Map.Entry<String, Object> entry : loadedMap.entrySet()) {
				idMap.put(Long.parseLong(entry.getKey()),
						(String) entry.getValue());
			}
		} catch (NumberFormatException e) {
			return "Improperly formatted city ID in ids.yml";
		} catch (ClassCastException e) {
			return "Improperly formatted city name in ids.yml";
		}
		City.names = idMap;
		return null;
	}

	public static boolean saveIDFile() {
		File idFilePath = new File(CityClaims.instance.dataPath, "ids.yml");
		FileConfiguration idFile = YamlConfiguration
				.loadConfiguration(idFilePath);
		for (Map.Entry<Long, String> entry : names.entrySet()) {
			idFile.set(Long.toString(entry.getKey()), entry.getValue());
		}
		try {
			idFile.save(idFilePath);
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

}
