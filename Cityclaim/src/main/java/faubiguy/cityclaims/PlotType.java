package faubiguy.cityclaims;

import java.util.HashMap;
import java.util.Map;

public class PlotType {

	public City city;

	public String name;

	public AdvancedPricing advancedPricing = new AdvancedPricing();
	public Flags flags = new TypeFlags(this);
	
	public PlotType(City city, String name) {
		this.city = city;
		this.name = name;
	}
	
	public double getPrice() {
		return flags.getFlagDouble("price");
	}
	
	public PlotType(City city, String name, double price, int limit) {
		this(city, name);
		flags.setFlag("limit", limit);
		flags.setFlag("price", price);
	}

	public double getPrice(int owned) {
		if (hasAdvancedPricing()) {
			Double price = advancedPricing.getPrice(owned);
			if (price != null) {
				return price;
			}
			return getPrice();
		}
		return getPrice();
	}
	
	public void save() {
		city.saveType(this);
	}
	
	public boolean hasAdvancedPricing() {
		return !advancedPricing.getRanges().isEmpty();
	}

	@Override
	public boolean equals(Object other) {
		return (other instanceof PlotType) && getPrice() == ((PlotType) other).getPrice()
				&& flags.getFlagInt("limit") == ((PlotType) other).flags.getFlagInt("limit")
				&& (advancedPricing == null ? ((PlotType)other).advancedPricing == null : advancedPricing.equals(((PlotType) other).advancedPricing));
	}

	@Override
	public int hashCode() {
		return (advancedPricing.hashCode() + (int) getPrice() - flags.getFlagInt("limit"));
	}
	
	public static class TypeFlags extends Flags {
		
		public PlotType type;
		
		public static final Map<String, String> TYPE_FLAG_TYPES;
		public static final Flags DEFAULTS;
		static {
			TYPE_FLAG_TYPES = new HashMap<String, String>();
			TYPE_FLAG_TYPES.put("price", "double");
			TYPE_FLAG_TYPES.put("plotlimit", "integer");
			TYPE_FLAG_TYPES.put("requireempty", "boolean");
			TYPE_FLAG_TYPES.put("allowsell", "boolean");
			TYPE_FLAG_TYPES.put("sellmultiplier", "double");
			TYPE_FLAG_TYPES.put("playersell", "boolean");
			TYPE_FLAG_TYPES.put("usetreasury", "boolean");
			Map<String,Object> defaultsMap = new HashMap<String, Object>();
			defaultsMap.put("price", 0);
			defaultsMap.put("plotlimit", -1);
			defaultsMap.put("requireempty", true);
			defaultsMap.put("allowsell", true);
			defaultsMap.put("sellmultiplier", 1D);
			defaultsMap.put("playersell", true);
			defaultsMap.put("usetreasury", true);
			DEFAULTS = new TypeFlags(null, defaultsMap);
		}
		
		public TypeFlags(PlotType type) {
			this(type, new HashMap<String,Object>());
		}
		
		public TypeFlags(PlotType type, Map<String,Object> flagsMap) {
			super(TYPE_FLAG_TYPES);
			this.type = type;
			flags = flagsMap;
		}

		@Override
		public void save() {
			type.save();
		}

		@Override
		public Flags getDefaults() {
			return DEFAULTS;
		}		
		
	}

}
