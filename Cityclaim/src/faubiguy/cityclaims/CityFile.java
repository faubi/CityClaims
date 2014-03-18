package faubiguy.cityclaims;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import me.ryanhamshire.GriefPrevention.GriefPrevention;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

public class CityFile {

	private File path;
	private FileConfiguration file;
	private City city;
	private boolean loaded = false;

	public CityFile(City city, boolean load) {
		path = new File(CityClaims.instance.dataPath, "cities" + File.separator
				+ city.name + ".yml");
		this.city = city;
		if (load) {
			loadFile();
		}
	}

	public CityFile(City city) {
		this(city, true);
	}

	public void loadFile() {
		file = YamlConfiguration.loadConfiguration(path);
		loaded = true;
	}

	public boolean isLoaded() {
		return loaded;
	}

	public boolean save() {
		try {
			file.save(path);
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	public boolean generate(boolean force) {
		// TODO File generation
		if (!isEmpty() && !force) {
			return false;
		}
		return saveCity();
	}

	public boolean isEmpty() {
		return file.getKeys(false).isEmpty();
	}

	public boolean exists() {
		return path.exists();
	}

	public void delete() {
		path.delete();
	}

	public long loadID() throws CityLoadingException {
		if (!(file.isLong("id") || file.isInt("id"))) {
			throw new CityLoadingException("Id missing or unreadable",
					city.name);
		}
		return file.getLong("id", -1);
	}

	public boolean saveID(boolean saveFile) {
		file.set("id", city.base.getID());
		if (saveFile) {
			return save();
		}
		return true;
	}

	public Plot loadPlot(long id) {
		;
		if (!file.isConfigurationSection("plots." + Long.toString(id))) {
			return null;
		}
		ConfigurationSection section = file.getConfigurationSection("plots."
				+ Long.toString(id));
		Plot plot = new Plot();
		plot.parent = city;
		plot.base = city.base.children.get((int) id);
		plot.owner = section.getString("owner");
		plot.name = section.getString("name");
		plot.type = city.getType(section.getString("type"));
		plot.size = PlotSize.fromString(section.getString("size"));
		if (section.isConfigurationSection("sale")) {
			try {
				plot.sale = new Plot.Sale(section.getDouble("sale.price", 0),
						new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
								.parse(section.getString("sale.expires", "")));
			} catch (ParseException e) {
				return null;
			}
		}
		return plot;
	}

	public Map<Long, Plot> loadPlots() throws CityLoadingException {
		Map<Long, Plot> plots = new HashMap<>();
		if(file.isConfigurationSection("plots")) {		
			for (String key : file.getConfigurationSection("plots").getKeys(false)) {
				long id;
				try {
					id = Long.parseLong(key);
				} catch (NumberFormatException e) {
					throw new CityLoadingException("Unreadable plot id: " + key,
							city.name);
				}
				Plot plot = loadPlot(id);
				if (plot == null) {
					throw new CityLoadingException("Unable to load plot: " + id,
							city.name);
				}
				plots.put(id, loadPlot(id));
			}
		}
		return plots;
	}

	public boolean savePlot(Plot plot, boolean saveFile) {
		ConfigurationSection section = file.createSection("plots."
				+ plot.getID());
		section.set("owner", plot.owner);
		section.set("name", plot.name);
		section.set("type", plot.type == null ? null : plot.type.name);
		section.set("size", plot.size.toString());
		if (plot.sale != null) {
			section = section.createSection("sale");
			section.set("price", plot.sale.price);
			section.set("expires", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
					.format(plot.sale.expires));
		}
		if (saveFile) {
			return save();
		}
		return true;
	}

	public void savePlots(boolean saveFile) {
		for (Plot plot : city.plots.values()) {
			savePlot(plot, false);
		}
		if (saveFile) {
			save();
		}
	}

	// Saving Methods

	public PlotType loadType(String name) {
		if (!file.isConfigurationSection("types." + name)) {
			return null;
		}
		ConfigurationSection typeSection = file
				.getConfigurationSection("types." + name);
		PlotType type = new PlotType();
		type.price = typeSection.getDouble("price", 0);
		type.limit = typeSection.getInt("limit", -1);
		if (typeSection.isConfigurationSection("advanced_pricing")) {
			AdvancedPricing advancedPricing = new AdvancedPricing();
			ConfigurationSection advancedPricingSection = typeSection
					.getConfigurationSection("advanced_pricing");
			for (Map.Entry<String, Object> entry : advancedPricingSection
					.getValues(false).entrySet()) {
				try {
					advancedPricing.addRange(
							AdvancedPricing.Range.fromString(entry.getKey()),
							(double) entry.getValue());
				} catch (ClassCastException e) {
					return null;
				}
			}
			type.advancedPricing = advancedPricing;
		}
		return type;
	}

	public Set<PlotType> loadTypes() throws CityLoadingException {
		Set<PlotType> types = new HashSet<>();
		if (file.isConfigurationSection("types")) {
			for (String key : file.getConfigurationSection("types").getKeys(false)) {
				PlotType type = loadType(key);
				if (type == null) {
					throw new CityLoadingException("Unable to load type: " + key,
							city.name);
				}
				types.add(type);
			}
		}
		return types;
	}

	public void saveType(PlotType type, boolean saveFile) {
		ConfigurationSection typeSection = file.createSection("types."
				+ type.name);
		typeSection.set("price", type.price);
		typeSection.set("limit", type.limit);
		if (type.advancedPricing != null) {
			ConfigurationSection advancedPricingSection = typeSection
					.createSection("advanced_pricing");
			Map<AdvancedPricing.Range, Double> ranges = type.advancedPricing
					.getRanges();
			for (Map.Entry<AdvancedPricing.Range, Double> entry : ranges
					.entrySet()) {
				advancedPricingSection.set(entry.getKey().toString(),
						entry.getValue());
			}
		}
		if (saveFile) {
			save();
		}

	}

	public void saveTypes(boolean saveFile) {
		for (PlotType type : city.types) {
			saveType(type, false);
		}
		if (saveFile) {
			save();
		}
	}

	public CityFlags loadFlags() {
		if (!file.isConfigurationSection("flags")) {
			return new CityFlags();
		}
		ConfigurationSection flagSection = file.getConfigurationSection("flags");
		Map<String,Object> flagMap = new HashMap<>();
		for (String flag : CityFlags.listFlags(true)) {
			if (flagSection.isSet(flag)) {
				String type = CityFlags.FLAG_TYPES.get(flag);
				flagMap.put(flag, type == "integer" ? flagSection.getInt(flag) : type == "double" ? flagSection.getDouble(flag) : type == "boolean" ? flagSection.getBoolean(flag) : null);
			}
		}
		return new CityFlags(flagMap);
	}

	public void saveFlags(boolean saveFile) {
		file.createSection("flags", city.flags.getMap());
		if (saveFile) {
			save();
		}
	}

	public double loadTreasury() throws CityLoadingException {
		if (!file.isDouble("treasury")) {
			throw new CityLoadingException("Treasury balance missing or unreadable",
					city.name);
		}
		return file.getDouble("treasury");
	}

	public boolean saveTreasury(boolean saveFile) {
		file.set("treasury", city.treasury);
		if (saveFile) {
			return save();
		}
		return true;
	}

	public Map<PlotSize, String> loadSizeTypes() {
		Map<PlotSize, String> sizeMap = new HashMap<>();
		if (file.isConfigurationSection("sizes")) {
			for (Map.Entry<String, Object> entry : file
					.getConfigurationSection("sizes").getValues(false)
					.entrySet()) {
				sizeMap.put(PlotSize.fromString(entry.getKey()),
						(String) entry.getValue());
			}
		}
		return sizeMap;

	}

	public boolean saveSizeTypes(boolean saveFile) {
		Map<String, String> sizeMap = new HashMap<>();
		for (Map.Entry<PlotSize, String> entry : city.sizeTypes.entrySet()) {
			sizeMap.put(entry.getKey().toString(), entry.getValue());
		}
		file.createSection("sizes", sizeMap);
		if (saveFile) {
			return save();
		}
		return true;
	}

	public void loadCity() throws CityLoadingException {
		city.id = loadID();
		city.base = GriefPrevention.instance.dataStore.getClaim(city.id);
		if (city.base == null) {
			throw new CityLoadingException(
					"Claim associated with city does not exist", city.name);
		}
		city.plots = loadPlots();
		city.types = loadTypes();
		city.flags = loadFlags();
		city.treasury = loadTreasury();
		city.sizeTypes = loadSizeTypes();
	}

	public boolean saveCity() {
		saveID(false);
		savePlots(false);
		saveTypes(false);
		saveFlags(false);
		saveTreasury(false);
		saveSizeTypes(false);
		return save();
	}

	public void rename(String newName) {
		File newPath = new File(CityClaims.instance.dataPath, "cities"
				+ File.separator + newName + ".yml");
		path.renameTo(newPath);
		path = newPath;
	}

}
