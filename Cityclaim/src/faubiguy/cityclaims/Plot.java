package faubiguy.cityclaims;

import java.util.Date;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.ClaimPermission;
import me.ryanhamshire.GriefPrevention.GriefPrevention;

public class Plot {

	public City parent;

	public String name;
	public Claim base;
	public PlotSize size;
	public PlotType type;
	public String owner;
	public Sale sale;
	public int surfaceLevel = 64;
	public long id;

	public static class Sale {

		public double price;
		public Date expires;

		public Sale(double price, Date expires) {
			this.price = price;
			this.expires = expires;
		}

	}
	
	public Plot(Claim base) {
		this.base = base;
		this.size = new PlotSize(base.getWidth(), base.getHeight());
	}

	public static Plot getPlot(City city, String name) {
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
		if (claim == null) {
			CityClaims.instance.getLogger().info("getPlot: No claim");
			return null;
		}
		if (claim.parent == null) {
			CityClaims.instance.getLogger().info("getPlot: Top level claim");
			return null;
		}
		City city = City.getCity(claim.parent.getID());
		if (city == null) {
			CityClaims.instance.getLogger().info("getPlot: No city");
			return null;
		}
		return city.getPlot(loc);
	}

	public void setOwner(String username) {
		base.clearPermissions();
		base.removeManager(owner);
		owner = username;
		if (!(owner == "" || owner == null)) {
			base.addManager(owner);
			base.setPermission(owner, ClaimPermission.Build);
		}
		sale = null;
		update();
	}

	public boolean setName(String name) {
		if (parent.getPlot(name) != null) {
			return false;
		}
		this.name = name;
		update();
		return true;
	}

	public Double getCityPrice(int owned) {
		return getCityPrice(owned, false);
	}

	public Double getCityPrice(int owned, boolean selling) {
		PlotType type = getType();
		if (type == null) {
			return null;
		}
		return type.getPrice(owned - (selling ? 1 : 0));
	}
	
	public Double getCityPrice(Player player) {
		return getCityPrice(player, false);
	}
	
	public Double getCityPrice(Player player, boolean selling) {
		return getCityPrice(parent.getOwnedPlots(player, type));
	}
	
	public Double getPrice(Player player) {
		if (owner == null) {
			return getCityPrice(player);
		} else if (sale != null) {
			return sale.price;
		} else {
			return null;
		}
	}

	public PlotType getType() {
		if (type != null) {
			return type;
		}
		return parent.getType(size);
	}

	public void setType(PlotType type) {
		if (this.type == null || !this.type.equals(type)) {
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
	
	public boolean isEmpty() {
		int x1 = base.getLesserBoundaryCorner().getBlockX();
		int x2 = base.getGreaterBoundaryCorner().getBlockX();
		if (x1 > x2) {
			int x = x1;
			x1 = x2;
			x2 = x;
		}
		int z1 = base.getLesserBoundaryCorner().getBlockZ();
		int z2 = base.getGreaterBoundaryCorner().getBlockZ();
		if (z1 > z2) {
			int z = z1;
			z1 = z2;
			z2 = z;
		}
		Location loc = new Location(parent.getWorld(),x1,surfaceLevel,z1);
		for (int y=surfaceLevel;y<=255;y++) {
			loc.setY(y);
			for (int x=x1;x<=x2;x++) {
				loc.setX(x);
				for (int z=z1;z<=z2;z++) {
					loc.setZ(z);
					if (!loc.getBlock().isEmpty()) {
						return false;
					}
				}
			}			
		}
		return true;
	}
	
	public void update() {
		parent.updatePlot(this);
	}

	public Location getCorner() {
		return base.getLesserBoundaryCorner();
	}
	
	public String getCornerString() {
		return getStringFromLocation(getCorner());
		
	}
	
	public static Location getLocationFromString(String str) {
		String[] parts = str.split(";");
		if (parts.length != 4) {
			return null;
		}
		try {
			return new Location(CityClaims.instance.getServer().getWorld(
					parts[0]), Double.parseDouble(parts[1]),
					Double.parseDouble(parts[2]), Double.parseDouble(parts[3]));
		} catch (NumberFormatException e) {
			return null;
		}
	}
	
	public static String getStringFromLocation(Location loc) {
		return loc.getWorld().getName() + ";" + loc.getBlockX() + ";" + loc.getBlockY() + ";" + loc.getBlockZ() + ";";
	}

}
