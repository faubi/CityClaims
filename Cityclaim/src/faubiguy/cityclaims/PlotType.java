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
		static {
			TYPE_FLAG_TYPES = new HashMap<>();
			TYPE_FLAG_TYPES.put("limit", "integer");
			TYPE_FLAG_TYPES.put("price", "double");
		}
		
		public TypeFlags(PlotType type) {
			super(TYPE_FLAG_TYPES);
			this.type = type;
			flags = new HashMap<>();
		}

		@Override
		public void save() {
			type.save();
		}		
		
	}

}
