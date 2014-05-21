package faubiguy.cityclaims;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.GriefPrevention;

import org.bukkit.Location;
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

	public Plot loadPlot(Claim plotClaim) {
		Plot plot = new Plot(plotClaim);
		plot.parent = city;
		Location loc = plotClaim.getLesserBoundaryCorner();
		plot.size = new PlotSize(plotClaim.getWidth(), plotClaim.getHeight());
		if (file.isConfigurationSection("plots." + Plot.getStringFromLocation(loc))) {
			ConfigurationSection section = file.getConfigurationSection("plots." + Plot.getStringFromLocation(loc));
			plot.ownerUUID = UUID.fromString(section.getString("owner_UUID"));
			plot.ownerName = section.getString("owner_name");
			plot.name = section.getString("name");
			plot.type = city.getType(section.getString("type"));
			plot.id = section.getLong("id");
			plot.surfaceLevel = section.getInt("surface_level", 64);
			if (section.isConfigurationSection("sale")) {
				try {
					plot.putForSale(section.getDouble("sale.price", 0), section.isSet("sale.expires") ? new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(section.getString("sale.expires", "")) : null);
				} catch (ParseException e) {
					return null;
				}
			}
		}
		return plot;
	}

	public Map<Long, Plot> loadPlots() throws CityLoadingException {
		Map<Long, Plot> plots = new HashMap<Long, Plot>();
		if(!file.isConfigurationSection("plots")) {
			file.createSection("plots");
		}
		for (Claim plotClaim : city.base.children) {
			Plot plot = loadPlot(plotClaim);
			if (plot == null) {
				throw new CityLoadingException("Unable to load plot: " + Plot.getStringFromLocation(plotClaim.getLesserBoundaryCorner()), city.name);
			}
			plots.put(plot.id, plot);
		}
		return plots;
	}

	public boolean savePlot(Plot plot, boolean saveFile) {
		ConfigurationSection section = file.createSection("plots."
				+ plot.getCornerString());
		section.set("owner_UUID", plot.ownerUUID.toString());
		section.set("owner_name", plot.ownerName);
		section.set("name", plot.name);
		section.set("id", plot.id);
		section.set("type", plot.type == null ? null : plot.type.name);
		section.set("size", plot.size.toString());
		section.set("surface_level", plot.surfaceLevel);
		Plot.Sale sale = plot.getSale();
		if (sale != null) {
			section = section.createSection("sale");
			section.set("price", sale.price);
			if (sale.expires != null) {
				section.set("expires", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(sale.expires));
			}
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
		PlotType type = new PlotType(city, name);
		type.flags.setFlag("price", typeSection.getDouble("price", 0));
		type.flags.setFlag("limit", typeSection.getInt("limit", -1));
		if (typeSection.isConfigurationSection("advanced_pricing")) {
			AdvancedPricing advancedPricing = new AdvancedPricing();
			ConfigurationSection advancedPricingSection = typeSection
					.getConfigurationSection("advanced_pricing");
			for (Map.Entry<String, Object> entry : advancedPricingSection
					.getValues(false).entrySet()) {
				try {
					advancedPricing.addRange(
							AdvancedPricing.Range.fromString(entry.getKey()),
							(Double) entry.getValue());
				} catch (ClassCastException e) {
					return null;
				}
			}
			type.advancedPricing = advancedPricing;
		}
		return type;
	}

	public Set<PlotType> loadTypes() throws CityLoadingException {
		Set<PlotType> types = new HashSet<PlotType>();
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
		typeSection.set("price", type.getPrice());
		typeSection.set("limit", type.flags.getFlagInt("limit"));
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
			return new CityFlags(city);
		}
		ConfigurationSection flagSection = file.getConfigurationSection("flags");
		Map<String,Object> flagMap = new HashMap<String, Object>();
		for (String flag : CityFlags.DEFAULTS.listFlags(true)) {
			if (flagSection.isSet(flag)) {
				String type = CityFlags.FLAG_TYPES.get(flag);
				flagMap.put(flag, type == "integer" ? flagSection.getInt(flag) : type == "double" ? flagSection.getDouble(flag) : type == "boolean" ? flagSection.getBoolean(flag) : null);
			}
		}
		return new CityFlags(city, flagMap);
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
		Map<PlotSize, String> sizeMap = new HashMap<PlotSize, String>();
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
		Map<String, String> sizeMap = new HashMap<String, String>();
		for (Map.Entry<PlotSize, String> entry : city.sizeTypes.entrySet()) {
			sizeMap.put(entry.getKey().toString(), entry.getValue());
		}
		file.createSection("sizes", sizeMap);
		if (saveFile) {
			return save();
		}
		return true;
	}

	public long loadNextPlotId() throws CityLoadingException {
		if (!file.isLong("next_plot_id")) {
			return file.isConfigurationSection("plots") ? file.getConfigurationSection("plots").getKeys(false).size() : 0;
		}
		return file.getLong("next_plot_id");
	}

	public boolean saveNextPlotId(boolean saveFile) {
		file.set("next_plot_id", city.nextPlotId);
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
		city.types = loadTypes();
		city.plots = loadPlots();
		city.flags = loadFlags();
		city.treasury = loadTreasury();
		city.sizeTypes = loadSizeTypes();
		city.nextPlotId = loadNextPlotId();
	}

	public boolean saveCity() {
		saveID(false);
		savePlots(false);
		saveTypes(false);
		saveFlags(false);
		saveTreasury(false);
		saveSizeTypes(false);
		saveNextPlotId(false);
		return save();
	}

	public void rename(String newName) {
		File newPath = new File(CityClaims.instance.dataPath, "cities"
				+ File.separator + newName + ".yml");
		path.renameTo(newPath);
		path = newPath;
	}

}
