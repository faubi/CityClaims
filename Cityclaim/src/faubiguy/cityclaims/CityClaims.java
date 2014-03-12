package faubiguy.cityclaims;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Set;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import java.io.File;

public class CityClaims extends JavaPlugin {

	public static CityClaims instance;

	public File dataPath = getDataFolder();
	public CityFlags defaults;
	public Set<String> lockedFlags;

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
		String error = City.loadIDFile();
		if (error != null) {
			return error; // Cancel initialization because of error
		}
		CityFlags.loadGlobalFlags();
		City.cities = new HashMap<>();
		File cityDir = new File(dataPath, "cities");
		if (cityDir.isDirectory()){
			for (String cityname : cityDir.list()) {
				try {
					City.loadCity(cityname);
				} catch (CityLoadingException e) {
				getLogger().warning(
						"Error loading " + e.cityname + ":" + e.getMessage());
				}
			}
		}
		return null;
	}

	public boolean onCommand(CommandSender sender, Command cmd, String label,
			String[] args) {
		if (cmd.getName().equalsIgnoreCase("city")) {
			String error = CommandHandler.handleCommand(sender, args[0],
					Arrays.copyOfRange(args, 1, args.length - 1));
			if (error != null) {
				sender.sendMessage(error);
			}
			return true;
		}
		return false;
	}

	public void onDisable() {
		getLogger().info(getName() + "has been disabled.");
	}

}
