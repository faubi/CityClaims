package faubiguy.cityclaims;

public class PlotType {

	public String name;

	public AdvancedPricing advancedPricing;
	public double price = 0;
	public int limit = -1;

	public double getPrice(int owned) {
		if (advancedPricing != null) {
			Double price = advancedPricing.getPrice(owned);
			if (price != null) {
				return price;
			}
			return this.price;
		}
		return price;
	}

	@Override
	public boolean equals(Object other) {
		return (other instanceof PlotType) && price == ((PlotType) other).price
				&& limit == ((PlotType) other).limit
				&& advancedPricing == ((PlotType) other).advancedPricing;
	}

	@Override
	public int hashCode() {
		return ((advancedPricing == null ? 0 : advancedPricing.hashCode())
				+ (int) price - limit);
	}

}
