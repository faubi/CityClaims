package faubiguy.cityclaims;

import java.util.Date;

import org.bukkit.Location;

import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.GriefPrevention;

public class Plot {

	public City parent;

	public String name;
	public Claim base;
	public PlotSize size;
	public PlotType type;
	public String owner;
	public Sale sale;

	public static class Sale {

		public double price;
		public Date expires;

		public Sale(double price, Date expires) {
			this.price = price;
			this.expires = expires;
		}

	}

	public static Plot getPlot(String cityname, String name) {
		City city = City.getCity(cityname);
		if (city == null) {
			return null;
		}
		int id;
		try {
			id = Integer.parseInt(name);
		} catch (NumberFormatException e) {
			return city.getPlot(name);
		}
		return city.getPlot(id);
	}

	public static Plot getPlot(Location loc) {
		Claim claim = GriefPrevention.instance.dataStore.getClaimAt(loc, true,
				null);
		if (claim.parent == null) {
			return null;
		}
		return City.getCity(claim.parent.getID()).getPlot(claim.getID());
	}

	public void changeOwner(String username) {
		base.clearPermissions();
		base.removeManager(owner);
		owner = username;
		if (!(owner == "" || owner == null)) {
			base.addManager(owner);
		}
		parent.updatePlot(this);
	}

	public boolean setName(String name) {
		if (parent.getPlot(name) != null) {
			return false;
		}
		this.name = name;
		parent.updatePlot(this);
		return true;
	}

	public Double getPrice(int owned) {
		PlotType type = getType();
		if (type == null) {
			return null;
		}
		return type.getPrice(owned);
	}

	public PlotType getType() {
		if (type != null) {
			return type;
		}
		return parent.getType(size);
	}

	public void setType(PlotType type) {
		if (!this.type.equals(type)) {
			this.type = type;
			parent.updatePlot(this);
		}
	}

	public void putForSale(double price, Date expires) {
		sale = new Sale(price, expires);
		parent.updatePlot(this);
	}

	public void removeSale() {
		sale = null;
		parent.updatePlot(this);
	}

	public long getID() {
		return base.getID();
	}

}
