package faubiguy.cityclaims;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Set;

import net.milkbowl.vault.economy.Economy;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import java.io.File;

public class CityClaims extends JavaPlugin {

	public static CityClaims instance;

	public File dataPath = getDataFolder();
	public CityFlags defaults;
	public Set<String> lockedFlags;
	
	public Economy economy;

	public void onEnable() {
		getLogger().info("CityClaims is loading");
		instance = this;
		String error = initialize();
		if (error != null) { // If there's an error initializing, cancel loading
								// the plugin
			getLogger().severe(error);
			this.getPluginLoader().disablePlugin(this);
		}
	}

	private String initialize() {
		RegisteredServiceProvider<Economy> economyProvider = getServer().getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class);
		if (economyProvider == null) {
			return "Unable to find a Vault-compatible economy plugin. Canceling loading CityClaims"; //Cancel loading due to error;
		}
		economy = economyProvider.getProvider();
		String error = City.loadIDFile();
		if (error != null) {
			return error; // Cancel initialization because of error
		}
		CityFlags.loadGlobalFlags();
		City.cities = new HashMap<String, City>();
		File cityDir = new File(dataPath, "cities");
		if (cityDir.isDirectory()){
			for (String cityname : cityDir.list()) {
				if (!cityname.endsWith(".yml")) {
					continue;
				}
				cityname = cityname.substring(0, cityname.length() - 4);
				try {
					City.loadCity(cityname);
				} catch (CityLoadingException e) {
				getLogger().warning(
						"Error loading " + e.cityname + ": " + e.getMessage());
				}
			}
		}
		CommandHandler.initialize();
		return null;
	}

	public boolean onCommand(CommandSender sender, Command cmd, String label,
			String[] args) {
		if (cmd.getName().equalsIgnoreCase("city")) {
			if (args.length == 0) {
				sender.sendMessage("§cYou must specify a subcommand. Type /city help for help.");
				return true;
			}
			CommandHandler.handleCommand(sender, args[0], args.length >= 2 ? Arrays.copyOfRange(args, 1, args.length) : new String[0]);
			return true;
		} else if (cmd.getName().equalsIgnoreCase("buyplot")) {
			CommandHandler.handleCommand(sender, "plot", new String[] {"buy"});
			return true;
		} else if (cmd.getName().equalsIgnoreCase("sellplot")) {
			CommandHandler.handleCommand(sender, "plot", new String[] {"sell"});
			return true;
		} else if (cmd.getName().equalsIgnoreCase("abandonplot")) {
			CommandHandler.handleCommand(sender, "plot", new String[] {"abandon"});
			return true;
		}
		return false;
	}

	public void onDisable() {
		getLogger().info(getName() + "has been disabled.");
	}

}
