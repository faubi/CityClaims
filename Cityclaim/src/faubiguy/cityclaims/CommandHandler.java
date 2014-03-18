package faubiguy.cityclaims;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.GriefPrevention;

import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public final class CommandHandler {
	
	public static Map<String,Command> commands;
	public static Map<String,CommandConfirmationTimeoutTask> unconfirmed; //Map from sender's username to unconfirmed command name
	
	public static final String NO_PERMISSION_MESSAGE = "§cYou do not have permission to do this."; 
	
	public static abstract class Command {
		public abstract void run(CommandSender sender, String[] arguments, boolean confirmed);
		
		public Set<String> permissions; //Permissions for command. Used for /city help.		
		public String description; //Description returned when useing /city help.		
		public String name; //Name of the command.
		public String usage; //Usage string shown on incorect usage
		
		private Pattern permissionSubstitutionPattern = Pattern.compile("\\[[0-9]+\\]");
		
		public boolean checkPermission(CommandSender sender, String[] arguments) {
			if (sender.isOp()) {
				return true;
			}
			for (String permission : permissions) {
				Matcher m = permissionSubstitutionPattern.matcher(permission);
				boolean skip = false;
				while (m.find()) {
					String match = m.group();
					int value = Integer.parseInt(match.substring(1, match.length() - 1));
					if (arguments != null && arguments.length < value) {
						skip = true;
						break;
					}
					permission = permission.substring(0,m.start()) + (arguments != null ? arguments[value-1] : "*") + permission.substring(m.end(), permission.length());
					m.reset(permission);
				}
				if (skip) {
					continue;
				}
				if (sender.hasPermission(permission)) {
					return true;
				}
			}
			return false;
		}
		
		public void sendUsage(CommandSender sender) {
			sender.sendMessage("Usage: " + usage);
		}
	}
	
	private static void requireConfirm(CommandSender sender, String commandName, String[] arguments) {
		CommandConfirmationTimeoutTask timeout = new CommandConfirmationTimeoutTask(sender, commandName, arguments);
		timeout.runTaskLater(CityClaims.instance, 300); //Wait for 15 seconds
		if (unconfirmed.put(sender.getName(), timeout) != null) {//If there was already an unconfirmed command
			sender.sendMessage("The previous command waiting for confimation has been cancelled");
		}
		sender.sendMessage("Type '/city confirm' within 15 seconds to confirm this command. Type /city cancel to cancel it");
	}
			
	public static class CommandConfirmationTimeoutTask extends BukkitRunnable {
		public String commandName;
		public CommandSender sender;
		public String[] arguments;
		
		public CommandConfirmationTimeoutTask(CommandSender sender, String commandName, String[] arguments) {
			this.commandName = commandName;
			this.sender = sender;
			this.arguments = arguments;
		}

		@Override
		public void run() {
			if(unconfirmed.remove(sender.getName()) != null) {
				sender.sendMessage("Confirmation for the command '/city " + commandName + "' has timed out.");
			}
			
		}
	}
	
	static void handleCommand(CommandSender sender, String commandName, String[] arguments) {
		Command command = commands.get(commandName);
		if (command == null) {
			sender.sendMessage("§cNo such command: /city " + commandName);
			return;
		}
		command.run(sender, arguments, false);
	}
	
	static void initialize() {
		commands = new TreeMap<>();
		unconfirmed = new HashMap<>();
		
		addCommand("create", new Command() {public void run(CommandSender sender, String[] arguments, boolean confirmed) {
			if (!checkPermission(sender, arguments)) {
				sender.sendMessage(NO_PERMISSION_MESSAGE);
				return;
			}
			if (arguments.length < 1) {
				sendUsage(sender);
				return;
			}
			Claim claim = arguments.length >= 2 ? GriefPrevention.instance.dataStore.getClaim(Long.parseLong(arguments[1])) : null;
			if (claim == null) {
				claim = getClaim(sender, "claim id", "claim");
				if (claim == null) {
					return;
				}
			}
			String result = City.newCity(arguments[0], claim);
			if (result != null) {
				sender.sendMessage(result);
				return;
			}
			sender.sendMessage("The city has successfully been created.");
		}});
		
		addCommand("delete", new Command() {public void run(CommandSender sender, String[] arguments, boolean confirmed) {
			if (!(sender.hasPermission("cityclaims.admin.delete"))) {
				sender.sendMessage(NO_PERMISSION_MESSAGE);
				return;
			}
			if (!confirmed) {
				requireConfirm(sender, "delete", arguments);
				return;
			}
			City city;
			if (arguments.length >= 1) {
				city = City.getCity(arguments[0]);
				if (city == null) {
					sender.sendMessage("§cThere is no city with that name");
					return;
				}
			} else {
				city = City.getCity(getClaim(sender));
				if (city == null) {
					return;
				}
			}
			city.delete();	
			sender.sendMessage("The city has successfully been deleted.");
		}});
		
		addCommand("list", new Command() {public void run(CommandSender sender, String[] arguments, boolean confirmed) {
			if (!(sender.hasPermission("cityclaims.general.list"))) {
				sender.sendMessage(NO_PERMISSION_MESSAGE);
				return;
			}
			boolean hidden = sender.hasPermission("cityclaims.admin.seehidden");
			String output = "";
			for(Map.Entry<String, City> cityEntry : City.cities.entrySet()) {
				if (hidden || !cityEntry.getValue().flags.getFlagBoolean("hidden")) {
					output = output + (output == "" ? "" : ", ") + cityEntry.getKey();
				}
			}
			if (output == "") {
				sender.sendMessage("There are no cities.");
			} else {
				sender.sendMessage("Cities: " + output);
			}
		}});
		
		addCommand("flags", new Command() {public void run(CommandSender sender, String[] arguments, boolean confirmed) {
			if (!checkPermission(sender, arguments)) {
				sender.sendMessage(NO_PERMISSION_MESSAGE);
				return;
			}
			if (arguments.length < 2) {
				sendUsage(sender);
			}
			City city = City.getCity(arguments[0]);
			if (city == null) {
				sender.sendMessage("§cThere is no city with that name");
				return;
			}
			flagCommand(sender, city, arguments[1], arguments.length >=3 ? arguments[2] : null, arguments.length >= 4 ? arguments[3] : null);
		}});
		
		addCommand("confirm", new Command() {public void run(CommandSender sender, String[] arguments,	boolean confirmed) {
			if (!checkPermission(sender, arguments)) {
				sender.sendMessage(NO_PERMISSION_MESSAGE);
				return;
			}
			CommandConfirmationTimeoutTask timeout = unconfirmed.get(sender.getName());
			if(timeout == null) {
				sender.sendMessage("You have no command awaiting confirmation!");
				return;
			}
			timeout.cancel();
			unconfirmed.remove(sender.getName());
			commands.get(timeout.commandName).run(timeout.sender, timeout.arguments, true);
			return;
		}});
		
		addCommand("cancel",  new Command() {public void run(CommandSender sender, String[] arguments,	boolean confirmed) {
			if (!checkPermission(sender, arguments)) {
				sender.sendMessage(NO_PERMISSION_MESSAGE);
				return;
			}
			CommandConfirmationTimeoutTask timeout = unconfirmed.get(sender.getName());
			if(timeout == null) {
				sender.sendMessage("You have no command awaiting confirmation!");
				return;
			}
			timeout.cancel();
			unconfirmed.remove(sender.getName());
			sender.sendMessage("The command has been cancelled.");
			return;
		}});
		
		addCommand("help",  new Command() {public void run(CommandSender sender, String[] arguments,	boolean confirmed) {
			if (!checkPermission(sender, arguments)) {
				sender.sendMessage(NO_PERMISSION_MESSAGE);
				return;
			}
			boolean usePage = true; //Whether the help is for a page or a specific command
			int page = 1;
			if (arguments.length >= 1) {
				try {
					page = Integer.parseInt(arguments[0]);
				} catch (NumberFormatException e) {
					usePage = false;					
				}
			}
			String[] outputText;
			if (usePage) {
				List<String> helpLines = new ArrayList<>(commands.size());
				for (Command command : commands.values()) {
					if (command.checkPermission(sender, null)) {
						helpLines.add("§9/city " + command.name + ": §b" + command.usage);
					}
				}
				int pages = (int)Math.ceil(helpLines.size() / 10D);
				if (page < 0) {
					sender.sendMessage("§cpage number must be at least 1");
					return;
				}
				if (page > pages) {
					sender.sendMessage("§cPage number too high. Only " + pages + " pages available");
					return;
				}
				int lines = helpLines.size() % 10;
				if (lines == 0) {
					lines = 10;
				}
				outputText = new String[lines + (page == pages ? 1 : 2)];
				outputText[0] = "§9CityClaims Help -- §bPage " + Integer.toString(page) + "/" + Integer.toString(pages);
				if (page < pages) {
					outputText[lines+1] = "§9Type §b/city help " + Integer.toString(page + 1) + " §9 to read the next page.";
				}
				int start = (page - 1) * 10;
				for (int i = 0; i < lines; i++) {
					outputText[i+1] = helpLines.get(start + i);
				}
			} else {
				Command command = commands.get(arguments[0]);
				if (command == null) {
					sender.sendMessage("§cNo such command: /city " + arguments[0]);
					return;
				}
				if (!command.checkPermission(sender, null)) {
					sender.sendMessage("§cYou do not have permission for that command");
					return;
				}
				outputText = new String[3];
				outputText[0] = "§9Help for command: §b/city " + command.name;
				outputText[1] = "§9Description: §b" + command.description;
				outputText[2] = "§9Usage: §b" + command.usage;
			}
			sender.sendMessage(outputText);			
		}});
		
		FileConfiguration commandFile = YamlConfiguration.loadConfiguration(CommandHandler.class.getResourceAsStream("/commands.yml"));
		for (Command command : commands.values()) {
			if (!commandFile.isConfigurationSection(command.name)) {
				continue;
			}
			command.description = commandFile.getString(command.name + ".description", "Description is mising");
			command.usage = commandFile.getString(command.name + ".usage", "Usage is missing");
			command.permissions = new HashSet<String>(commandFile.getStringList(command.name + ".permission"));
		}
	}
	
	private static Claim getClaim(CommandSender sender, String sub1, String sub2) {//Utility method. Returns claim if successful and error message if not.
		if (!(sender instanceof Player)) {
			sender.sendMessage("This command requires a " + sub1 + " if not run by a player");
			return null;
		}
		Claim claim = GriefPrevention.instance.dataStore.getClaimAt(((Player)sender).getLocation(), true, null);
		if (claim == null) {
			sender.sendMessage("No " + sub2 + " exists at that location");
			return null;
		}
		return claim;		
	}
	
	private static Claim getClaim(CommandSender sender) {
		return getClaim(sender, "city name", "city");
	}
	
	private static void flagCommand(CommandSender sender, City city, String mode, String arg1, String arg2) {
		CityFlags flags = city == null ? CityClaims.instance.defaults : city.flags;
		if (mode == "list") {
			String flagList = "";
			for (String flag : CityFlags.listFlags(city == null)) {
				flagList = flagList + ((arg2 == "all" || flags.getFlag(flag) != null) ? ((flagList == "" ? "" : ", ") + flag) : "");
			}
			sender.sendMessage("Flags: " + ((flagList != "") ? flagList : "None"));
		} else if (mode == "get") {
			if (arg1 == null) {
				sender.sendMessage("You must specify a flag.");
				return;
			}
			Object value = flags.getFlag(arg1);
			if (value == null) {
				sender.sendMessage("That flag does not exist.");
				return;
			}
			sender.sendMessage("Value of flag " + arg1 + " is " + value.toString());
		} else if (mode == "set") {
			if (arg2 == null) {
				sender.sendMessage("You must specify a flag and a value.");
				return;
			}
			if(CityFlags.FLAG_TYPES.get(arg1) == null) {
				sender.sendMessage("That flag does not exist.");
				return;
			}
			Object value = CityFlags.getFlagValueFromString(arg1, arg2);
			if (value == null) {
				sender.sendMessage("Flag value is not a valid " + CityFlags.FLAG_TYPES.get(arg1));
				return;
			}
			if (flags.setFlag(arg1, null)) {
				sender.sendMessage("Successfully set value of flag " + arg1 + " to " + arg2);
			} else {
				sender.sendMessage("That flag has been globally locked");
			}
		} else if (mode == "remove") {
			if (arg1 == null) {
				sender.sendMessage("You must specify a flag.");
				return;
			}
			if (flags.removeFlag(arg1)) {
				sender.sendMessage("Flag removed.");
			} else {
				sender.sendMessage("That flag is not set!");
			}
		} else if (mode == "lock") {
			
		} else if (mode == "unlock") {
			
		} else if (mode == "listlocks") {
			
		} else {
			sender.sendMessage("Unsupported mode: " + mode);
		}
	}

	private static void addCommand(String name, Command command) {
		command.name = name;
		commands.put(name, command);
	}
}