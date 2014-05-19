package faubiguy.cityclaims;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
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
import net.milkbowl.vault.economy.Economy;

import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permissible;
import org.bukkit.permissions.PermissionAttachmentInfo;

public final class CommandHandler {
	
	public static Map<String,Command> commands;
	public static Map<String,CommandConfirmationTimeoutTask> unconfirmed; //Map from sender's username to unconfirmed command name
	public static Economy eco;
	
	public static final String NO_PERMISSION_MESSAGE = "§cYou do not have permission to do this."; 
	
	public static abstract class Command {
		public abstract void run(CommandSender sender, String[] arguments, boolean confirmed);
		
		public Set<String> permissions; //Permissions for command. Used for /city help.		
		public String description; //Description returned when useing /city help.		
		public String name; //Name of the command.
		public String usage; //Usage string shown on incorect usage
		public List<String> subusages;
		
		private static final Pattern permissionSubstitutionPattern = Pattern.compile("\\[[0-9]+\\]");
		
		public boolean checkPermission(CommandSender sender, String[] arguments) {
			if (sender.isOp()) {
				return true;
			}
			for (String permission : permissions) {
				Matcher m = permissionSubstitutionPattern.matcher(permission);
				boolean skip = false;
				boolean subnode = arguments == null;
				while (m.find()) {
					String match = m.group();
					int value = Integer.parseInt(match.substring(1, match.length() - 1));
					if (arguments != null && arguments.length < value) {
						skip = true;
						break;
					}
					permission = permission.substring(0,m.start()) + (subnode ? "*" : arguments[value-1]) + permission.substring(m.end(), permission.length());
					m.reset(permission);
				}
				if (skip) {
					continue;
				}
				if (checkPermissionWithWildcard(sender, permission, subnode)) {
					return true;
				}
			}
			return false;
		}
		
		public void sendUsage(CommandSender sender) {
			sender.sendMessage("§cUsage: " + usage);
		}
		
		protected void requireConfirm(CommandSender sender, String[] arguments) {
			CommandConfirmationTimeoutTask timeout = new CommandConfirmationTimeoutTask(sender, name, arguments);
			timeout.runTaskLater(CityClaims.instance, 300); //Wait for 15 seconds
			if (unconfirmed.put(sender.getName(), timeout) != null) {//If there was already an unconfirmed command
				sender.sendMessage("The previous command waiting for confimation has been cancelled");
			}
			sender.sendMessage("Type '/city confirm' within 15 seconds to confirm this command. Type /city cancel to cancel it");
		}
		
		public void init() {}
		
	}
	
	public static boolean checkPermissionWithWildcard(Permissible p, String permission, boolean subnode) {
		//CityClaims.instance.getLogger().info("Subnode = " + Boolean.toString(subnode));
		//CityClaims.instance.getLogger().info("Checking " + permission);
		if (subnode ? checkPermissionAnySubnode(p, permission, true) : p.hasPermission(permission)) {
			//CityClaims.instance.getLogger().info("Found");
			return true;
		}
		//CityClaims.instance.getLogger().info("Not found");
		String[] nodes = permission.split("\\.");
		permission = "";
		for(int i=0;i<nodes.length-1;i++) {
			permission = permission + nodes[i] + ".*";
			//CityClaims.instance.getLogger().info("Checking " + permission);
			if (subnode ? checkPermissionAnySubnode(p, permission, false) : p.hasPermission(permission)) {
				//CityClaims.instance.getLogger().info("Found");
				return true;
			}
			//CityClaims.instance.getLogger().info("Not found");
			permission = permission.substring(0, permission.length() - 1);
		}
		return false;
	}
	
	//
	private static boolean checkPermissionAnySubnode(Permissible p, String permission, boolean replaceLast) {
		//CityClaims.instance.getLogger().info("AnySubnode: replaceLast = " + Boolean.toString(replaceLast));
		permission = Pattern.quote(permission);
		String permRegex;
		if (replaceLast) {
			permRegex = permission.replace("*", "[-\\.]+");
		} else {
			int lastIndex = permission.lastIndexOf('*');
			lastIndex = lastIndex > 0 ? lastIndex : permission.length();
			permRegex = permission.substring(0,lastIndex).replace("*", "[-\\.]+") + permission.substring(lastIndex, permission.length());
		}
		Pattern pattern = Pattern.compile(permRegex);
		//CityClaims.instance.getLogger().info("AnySubnode: Pattern = " + pattern.toString());
		Matcher m;
		for(PermissionAttachmentInfo permInfo : p.getEffectivePermissions()) {
			m = pattern.matcher(permInfo.getPermission());
			if (m.find()) {
				//CityClaims.instance.getLogger().info("AnySubnode: Permission found: " + m.group());
				return true;
			}
		}
		//CityClaims.instance.getLogger().info("AnySubnode: Permission not found");
		return false;
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
		commands = new TreeMap<String, Command>();
		unconfirmed = new HashMap<String, CommandConfirmationTimeoutTask>();
		
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
			if (!(checkPermission(sender, arguments))) {
				sender.sendMessage(NO_PERMISSION_MESSAGE);
				return;
			}
			if (!confirmed) {
				requireConfirm(sender, arguments);
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
			if (!checkPermission(sender, arguments)) {
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
			if (arguments.length < 2) {
				sendUsage(sender);
				return;
			}
			if (!checkPermission(sender, arguments)) {
				sender.sendMessage(NO_PERMISSION_MESSAGE);
				return;
			}
			City city = City.getCity(arguments[0]);
			if (city == null) {
				sender.sendMessage("§cThere is no city with that name");
				return;
			}
			flagCommand(sender, city.flags, arguments[1], arguments.length >=3 ? arguments[2] : null, arguments.length >= 4 ? arguments[3] : null);
		}});
		
		addCommand("globalflags", new Command() {public void run(CommandSender sender, String[] arguments, boolean confirmed) {
			if (!checkPermission(sender, arguments)) {
				sender.sendMessage(NO_PERMISSION_MESSAGE);
				return;
			}
			if (arguments.length < 1) {
				sendUsage(sender);
				return;
			}
			flagCommand(sender, CityClaims.instance.defaults, arguments[0], arguments.length >=2 ? arguments[1] : null, arguments.length >= 3 ? arguments[2] : null);
		}});
		
		addCommand("type", new Command() {public void run(CommandSender sender, String[] arguments, boolean confirmed) {
			if (!checkPermission(sender, arguments)) {
				sender.sendMessage(NO_PERMISSION_MESSAGE);
				return;
			}
			if (arguments.length < 2) {
				sendUsage(sender);
				return;
			}
			City city = City.getCity(arguments[0]);
			if (city == null) {
				sender.sendMessage("§cThere is no city with that name");
				return;
			}
			String mode = arguments[1];
			arguments = Arrays.copyOfRange(arguments, 2, 6);
			if (mode.equalsIgnoreCase("add")) {
				if (arguments[0] == null) {
					sender.sendMessage("§cYou must specify a name for the new type");
				}
				double price = 0;
				if (arguments[1] != null) {
					try {
						price = Double.parseDouble(arguments[1]);
					} catch (NumberFormatException e) {
						sender.sendMessage("§c" + arguments[1] + "is not a valid number.");
						return;
					}
				}
				int limit = -1;
				if (arguments[2] != null) {
					try {
						price = Integer.parseInt(arguments[1]);
					} catch (NumberFormatException e) {
						sender.sendMessage("§c" + arguments[1] + "is not a valid integer.");
						return;
					}
				}
				if (city.getType(arguments[0]) != null) {
					sender.sendMessage("§There is already a type with that name.");
					return;
				}
				city.addType(new PlotType(city, arguments[0], price, limit));
				sender.sendMessage("Type " + arguments[0] + " added successfully.");
			} else if (mode.equalsIgnoreCase("remove")) {
				if (arguments[0] == null) {
					sender.sendMessage("§cYou must specify the type to delete.");
					return;
				}
				if (city.removeType(arguments[0])) {
					sender.sendMessage("Type " + arguments[0] + " has been deleted successfully.");
				} else {
					sender.sendMessage("§cNo type exists with that name");
				}
			} else if (mode.equalsIgnoreCase("flags")) {
				if (arguments[0] == null) {
					sender.sendMessage("§cYou must specify a type.");
					return;
				}
				if (arguments[1] == null) {
					sender.sendMessage("§cYou must specify a mode (get, set, unset, or list).");
					return;
				}
				PlotType type = city.getType(arguments[0]);
				if (type == null) {
					sender.sendMessage("§cThere is no type with that name.");
					return;
				}
				if (!arguments[1].equals("get") && !arguments[1].equals("set") && !arguments[1].equals("list") && !arguments[1].equals("unset")) {
					sender.sendMessage("§cInvalid flag mode: " + arguments[1]);
				}
				flagCommand(sender, type.flags, arguments[1], arguments[2], arguments[3]);
			} else if (mode.equalsIgnoreCase("list")) {
				String typesList = "";
				for (PlotType type : city.types) {
					typesList += (typesList.equals("") ? "" : ", ") + type.name;
				}
				sender.sendMessage("Types: " + typesList);
			} else if (mode.equalsIgnoreCase("advancedpricing")) {
				if (arguments[0] == null) {
					sender.sendMessage("§cYou must specify a type.");
					return;
				}
				if (arguments[1] == null) {
					sender.sendMessage("§cYou must specify a mode (get, set, unset, list, or reset).");
					return;
				}
				PlotType type = city.getType(arguments[0]);
				if (type == null) {
					sender.sendMessage("§cThere is no type with that name.");
					return;
				}
				if (arguments[1].equalsIgnoreCase("set")) {
					if (arguments[2] == null) {
						sender.sendMessage("§cYou must specify a range or value. Examples: 2-4, 6, 3+");
						return;
					}
					if (arguments[3] == null) {
						sender.sendMessage("§cYou must specify a price.");
						return;
					}
					AdvancedPricing.Range range = AdvancedPricing.Range.fromString(arguments[2]);
					if (range == null) {
						sender.sendMessage("§cUnable to read range: " + arguments[2]);
						return;
					}
					double price;
					try {
						price = Double.parseDouble(arguments[3]);
					} catch (NumberFormatException e) {
						sender.sendMessage("Invalid number: " + arguments[3]);
						return;
					}
					AdvancedPricing.Range conflicting = type.advancedPricing.addRange(range, price);
					if(conflicting == null) {
						sender.sendMessage("Price for range " + arguments[2] + " set to " + Double.toString(price));
						type.save();
					} else {
						sender.sendMessage("§cCan't set range due to conflict with existing range: " + conflicting.toString());
					}
				} else if (arguments[1].equalsIgnoreCase("get")) {
					if (arguments[2] == null) {
						sender.sendMessage("§cYou must specify a range or value. Examples: 2-4, 6, 3+");
						return;
					}
					AdvancedPricing.Range range = AdvancedPricing.Range.fromString(arguments[2]);
					if (range == null) {
						sender.sendMessage("§cUnable to read range: " + arguments[2]);
						return;
					}
					Double price = type.advancedPricing.getRanges().get(range);
					if (price == null) {
						sender.sendMessage("There is no price set for range " + range.toString());
					} else {
						sender.sendMessage("Price for range " + range.toString() + ": " + price.toString());
					}
				} else if (arguments[1].equalsIgnoreCase("unset")) {
					if (arguments[2] == null) {
						sender.sendMessage("§cYou must specify a range or value. Examples: 2-4, 6, 3+");
						return;
					}
					AdvancedPricing.Range range = AdvancedPricing.Range.fromString(arguments[2]);
					if (range == null) {
						sender.sendMessage("§cUnable to read range: " + arguments[2]);
						return;
					}
					type.advancedPricing.removeRange(range);
					sender.sendMessage("Range " + range.toString() + " no longer has a price set");
					if(type.advancedPricing.getRanges().isEmpty()) {
						type.advancedPricing = null;
					}
					type.save();
				} else if (arguments[1].equalsIgnoreCase("reset")) {
					type.advancedPricing = null;
					type.save();
				} else if (arguments[1].equalsIgnoreCase("list")) {
					String rangesString = "";
					for (AdvancedPricing.Range range : type.advancedPricing.getRanges().keySet()) {
						rangesString += (rangesString.equals("") ? "" : ", ") + range.toString();
					}
					sender.sendMessage("Ranges with price set: " + rangesString);
				} else {
					sender.sendMessage("§cInvalid advancedpricing mode: " + arguments[1]);
				}
			} else {
				sender.sendMessage("Invalid subcommand: " + mode);
			}
		}});
		
		addCommand("sizetype", new Command() {public void run(CommandSender sender, String[] arguments,	boolean confirmed) {
			if (!checkPermission(sender, arguments)) {
				sender.sendMessage(NO_PERMISSION_MESSAGE);
				return;
			}
			if (arguments.length < 2) {
				sendUsage(sender);
				return;
			}
			City city = City.getCity(arguments[0]);
			if (city == null) {
				sender.sendMessage("§cThere is no city with that name");
				return;
			}
			String mode = arguments[1];
			arguments = Arrays.copyOfRange(arguments, 2, 4);
			if (mode.equalsIgnoreCase("set")) {
				if (arguments[0] == null) {
					sender.sendMessage("§cYou must specify a size. Example: 16x16");
					return;
				}
				if (arguments[1] == null) {
					sender.sendMessage("§cYou must specify a type.");
					return;
				}
				PlotSize size = PlotSize.fromString(arguments[0]);
				if (size == null) {
					sender.sendMessage("Invalid plot size: " + arguments[0]);
					return;
				}
				PlotType type = city.getType(arguments[1]);
				if (type == null) {
					sender.sendMessage("§cNo type exists with name: " + arguments[1]);
					return;
				}
				city.addSizeType(size, type);
				sender.sendMessage("Type for size " + arguments[0] + " successfully set to " + arguments[1]);
			} else if (mode.equalsIgnoreCase("get")) {
				if (arguments[0] == null) {
					sender.sendMessage("§cYou must specify a size. Example: 16x16");
					return;
				}
				PlotSize size = PlotSize.fromString(arguments[0]);
				if (size == null) {
					sender.sendMessage("Invalid plot size: " + arguments[0]);
					return;
				}
				PlotType type = city.getType(size);
				if (type == null) {
					sender.sendMessage("§cNo type is set for that size.");
					return;
				}
				sender.sendMessage("Type for " + size.toString() + ": " + type.name);
			} else if (mode.equalsIgnoreCase("unset")) {
				if (arguments[0] == null) {
					sender.sendMessage("§cYou must specify a size. Example: 16x16");
					return;
				}
				PlotSize size = PlotSize.fromString(arguments[0]);
				if (size == null) {
					sender.sendMessage("Invalid plot size: " + arguments[0]);
					return;
				}
				if(city.removeSizeType(size)) {
					sender.sendMessage("Successfully removed type from size " + arguments[0]);
				} else {
					sender.sendMessage("§cNo type is set for that size");
				}
			} else if (mode.equalsIgnoreCase("list")) {
				String sizesString = "";
				for (PlotSize size : city.sizeTypes.keySet()) {
					sizesString += (sizesString.equals("") ? "" : ", ") + size.toString();
				}
				sender.sendMessage("Sizes with type set: " + sizesString);
			}
		}});
		
		addCommand("treasury", new Command() {public void run(CommandSender sender, String[] arguments,	boolean confirmed) {
			if (!(sender instanceof Player)) {
				sender.sendMessage("§cThis command must be run by a player");
				return;
			}
			if (!checkPermission(sender, arguments)) {
				sender.sendMessage(NO_PERMISSION_MESSAGE);
				return;
			}
			if (arguments.length < 2) {
				sendUsage(sender);
				return;
			}
			City city = City.getCity(arguments[0]);
			if (city == null) {
				sender.sendMessage("§cThere is no city with that name");
				return;
			}
			if (arguments[1].equalsIgnoreCase("balance")) {
				sender.sendMessage("Treasury balance: " + Double.toString(city.treasury));
			} else if (arguments[1].equalsIgnoreCase("withdraw")) {
				if (arguments[2] == null) {
					sender.sendMessage("§cYou must specify an amount to withdraw.");
					return;
				}
				Double amount = parseCurrency(arguments[2]);
				if (amount == null) {
					sender.sendMessage("§cInvalid number: " + arguments[2]);
					return;
				}
				if (amount <= 0) {
					sender.sendMessage("§cAmount to withdraw must be positive.");
					return;
				}
				if (amount > city.treasury) {
					sender.sendMessage("§cThe treasury doesn't contain that much.");
					return;
				}
				city.treasury -= amount;
				eco.depositPlayer(((Player)sender).getName(), city.getWorld().getName(), amount);
			} else if (arguments[1].equalsIgnoreCase("deposit")) {
				if (arguments[2] == null) {
					sender.sendMessage("§cYou must specify an amount to deposit.");
					return;
				}
				Double amount = parseCurrency(arguments[2]);
				if (amount == null) {
					sender.sendMessage("§cInvalid number: " + arguments[2]);
					return;
				}
				if (amount <= 0) {
					sender.sendMessage("§cAmount to deposit must be positive.");
					return;
				}
				if (amount > CityClaims.instance.economy.getBalance(((Player)sender).getName(), city.getWorld().getName())) {
					sender.sendMessage("§cYou don't have that much money!.");
					return;
				}
				city.treasury += amount;
				eco.withdrawPlayer(((Player)sender).getName(), city.getWorld().getName(), amount);
			} else {
				sender.sendMessage("§cInvalid subcommand: " + arguments[1]);
			}
		}});
		
		/*addCommand("setplotowner", new Command() {public void run(CommandSender sender, String[] arguments,	boolean confirmed) {
			if (!checkPermission(sender, arguments)) {
				sender.sendMessage(NO_PERMISSION_MESSAGE);
				return;
			}
			if (arguments.length < 3) {
				sendUsage(sender);
				return;
			}
			City city = City.getCity(arguments[0]);
			if (city == null) {
				sender.sendMessage("§cThere is no city with that name");
				return;
			}
			Plot plot = Plot.getPlot(city, arguments[1]);
			if (plot == null) {
				sender.sendMessage("§cInvalid plot name/id: " + arguments[1]);
				return;
			}
			if (plot.owner.equalsIgnoreCase(arguments[2])) {
				sender.sendMessage("That user already owns that plot");
			}
			plot.setOwner(arguments[2] == "unowned" ? null : arguments[2]);
			sender.sendMessage("Plot owner set");
		}});*/
		
		addCommand("rename", new Command() {public void run(CommandSender sender, String[] arguments,	boolean confirmed) {
			if (!checkPermission(sender, arguments)) {
				sender.sendMessage(NO_PERMISSION_MESSAGE);
				return;
			}
			if (arguments.length < 2) {
				sendUsage(sender);
				return;
			}
			City city = City.getCity(arguments[0]);
			if (city == null) {
				sender.sendMessage("§cThere is no city with that name");
				return;
			}
			city.setName(arguments[1]);
			sender.sendMessage("City name set to " + arguments[1]);
		}});
		
		addCommand("plotconfig", new Command() {public void run(CommandSender sender, String[] arguments,	boolean confirmed) {
			if (!checkPermission(sender, arguments)) {
				sender.sendMessage(NO_PERMISSION_MESSAGE);
				return;
			}
			if (arguments.length < 4) {
				sendUsage(sender);
				return;
			}
			City city = City.getCity(arguments[0]);
			if (city == null) {
				sender.sendMessage("§cThere is no city with that name");
				return;
			}
			Plot plot = Plot.getPlot(city, arguments[3]);
			if (plot == null) {
				sender.sendMessage("§cInvalid plot name/id: " + arguments[1]);
				return;
			}
			if (arguments[1].equalsIgnoreCase("type")) {
				if (arguments[2].equalsIgnoreCase("set")) {//type set
					if (arguments.length < 5) {
						sender.sendMessage("§cYou must specify the type to set");
						return;
					}
					PlotType type = city.getType(arguments[4]);
					if (type == null) {
						sender.sendMessage("§cThere is no type with that name");
						return;
					}
					plot.setType(type);
					sender.sendMessage("Plot type successfully set to " + type.name);
				} else if (arguments[2].equalsIgnoreCase("unset")) {//type unset
					plot.setType(null);
					sender.sendMessage("Plot type successfully unset");
				} else {
					sender.sendMessage("§cInvalid mode (must be set/unset)");
				}
			} else if (arguments[1].equalsIgnoreCase("owner")) {
				if (arguments[2].equalsIgnoreCase("set")) {//owner set
					if (arguments.length < 5) {
						sender.sendMessage("§cYou must specify the type to set");
						return;
					}
					if (plot.owner != null && plot.owner.equalsIgnoreCase(arguments[4])) {
						sender.sendMessage("That user already owns that plot");
					}
					plot.setOwner(arguments[4]);
					plot.update();
					sender.sendMessage("Plot owner set to " + arguments[4]);
				} else if (arguments[2].equalsIgnoreCase("unset")) {//owner unset
					if (plot.owner == null) {
						sender.sendMessage("The plot is now unowned");
					}
					plot.setOwner(null);
					sender.sendMessage("That plot is now unowned");
				} else {
					sender.sendMessage("§cInvalid mode (must be set/unset)");
				}
			} else if (arguments[1].equalsIgnoreCase("name")) {
				if (arguments[2].equalsIgnoreCase("set")) {//name set
					if (arguments.length < 5) {
						sender.sendMessage("§cYou must specify the name for the plot");
						return;
					}
					plot.setName(arguments[4]);
					sender.sendMessage("Plot name successfully set to " + arguments[4]);
				} else if (arguments[2].equalsIgnoreCase("unset")) {//name unset
					plot.setName(null);
					sender.sendMessage("Plot name successfully unset.");					
				} else {
					sender.sendMessage("§cInvalid mode (must be set/unset)");
				}
			} else if (arguments[1].equalsIgnoreCase("surfacelevel")) {
				if (arguments[2].equalsIgnoreCase("set")) {//name set
					if (arguments.length < 5) {
						sender.sendMessage("§cYou must specify the name for the plot.");
						return;
					}
					if (plot.parent.getPlot(arguments[4]) != null) {
						sender.sendMessage("§cThere is already a plot with that name.");
						return;
					}
					int surfaceLevel;
					try {
						surfaceLevel = Integer.parseInt(arguments[4]);
					} catch (NumberFormatException e) {
						sender.sendMessage("§cInvalid integer: " + arguments[4]);
						return;
					}
					if (surfaceLevel < 0 || surfaceLevel >= plot.parent.getWorld().getMaxHeight()) {
						sender.sendMessage("§cThat level is higher or lower than the world boundaries");
						return;
					}
					plot.surfaceLevel = surfaceLevel;
					plot.update();
					sender.sendMessage("Plot surface level successfully set to " + arguments[4]);
				} else {
					sender.sendMessage("§cInvalid mode (must be set)");
				}
			} else {
				sender.sendMessage("§cInvalid subcommand");
			}
		}});
		
		addCommand("info", new Command() {public void run(CommandSender sender, String[] arguments,	boolean confirmed) {
			if (!checkPermission(sender, arguments)) {
				sender.sendMessage(NO_PERMISSION_MESSAGE);
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
			Location loc = city.getLocation();
			String[] cityInfo = {
				"==== City information ====",
				"Name: " + city.name,
				"Id: " + Long.toString(city.getID()),
				"X: " + loc.getX(),
				"Y: " + loc.getY(),
				"Z: " + loc.getZ()
			};
			sender.sendMessage(cityInfo);
		}});
		
		addCommand("plotlist", new Command() {public void run(CommandSender sender, String[] arguments,	boolean confirmed) {
			if (!checkPermission(sender, arguments)) {
				sender.sendMessage(NO_PERMISSION_MESSAGE);
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
			String plots = "";
			for (Plot plot : city.plots.values()) {
				plots += (plots.equals("") ? "" : ", ") + (plot.name != null ? plot.name : plot.id);
			}
			sender.sendMessage("Plots: " + plots);
		}});
		
		addCommand("plot", new Command() {public void run(CommandSender sender, String[] arguments,	boolean confirmed) {
			if (!checkPermission(sender, arguments)) {
				sender.sendMessage(NO_PERMISSION_MESSAGE);
				return;
			}
			if (arguments.length < 1) {
				sendUsage(sender);
				return;
			}
			if (!arguments[0].equalsIgnoreCase("info") && !(sender instanceof Player)) {
				sender.sendMessage("§cThis subcommand (" + arguments[0] + ") must be run by a player");
				return;
			}
			String subcommand = arguments[0].toLowerCase();
			String[] originalArguments = arguments.clone();
			Plot plot = null;
			Integer argumentsNumber = argumentCount.get(subcommand);
			argumentsNumber = (argumentsNumber == null ? 0 : argumentsNumber);
			if (arguments.length >= argumentsNumber + 3) {
				City city = City.getCity(arguments[1]);
				if (city == null) {
					sender.sendMessage("§cThere is no city with that name");
					return;
				}
				plot = Plot.getPlot(city, arguments[2]);
				if (plot == null) {
					sender.sendMessage("§cThere's no plot with that name or id.");
					return;
				}
				arguments = Arrays.copyOfRange(arguments, 3, 3 + argumentsNumber);
			} else {
				if (!(sender instanceof Player)) {
					sender.sendMessage("§cThis command requires a city and plot if not run by a player");
					return;
				}
				plot = Plot.getPlot(((Player)sender).getLocation());
				if (plot == null) {
					sender.sendMessage("§cThere's no plot where you're standing.");
					return;
				}
				arguments = Arrays.copyOfRange(arguments, 1, 1 + argumentsNumber);
			}
			if (subcommand.equals("info")) {
				String[] plotInfo = {
					"==== Plot info ====",
					"Name: " + (plot.name != null ? plot.name : "Unnamed"),
					"Id: " + plot.id,
					"Owner: " + (plot.owner != null ? plot.owner : "Unowned"),
					"Type: " + (plot.getType() != null ? plot.getType().name : "No type"),
					"Size: " + plot.size.toString(),
					"Sale Offer: " + (plot.getSale() != null ? eco.format(plot.getSale().price) : "Not offered"),
					"Surface level: " + plot.surfaceLevel
				};
				sender.sendMessage(plotInfo);
			} else if (subcommand.equals("price")) {
				if (plot.parent.reachedLimit((Player)sender, null)) {
					sender.sendMessage("You have reached the plot limit for this city");
					return;
				}
				if (plot.parent.reachedLimit((Player)sender, plot.type)) {
					sender.sendMessage("You have reached the limit for this type of plot");
					return;
				}
				Double price = plot.getPrice((Player)sender);
				if (price == null) {
					sender.sendMessage("That plot is not for sale");
				} else {
					sender.sendMessage("The price to buy this plot is " + eco.format(price));
				}
			} else if (subcommand.equals("buy")) {
				Double price = plot.getPrice((Player)sender);
				if (price == null) {
					sender.sendMessage("That plot is not for sale");
					return;
				}
				if (plot.parent.reachedLimit((Player)sender, null)) {
					sender.sendMessage("You have reached the plot limit for this city");
					return;
				}
				if (plot.parent.reachedLimit((Player)sender, plot.type)) {
					sender.sendMessage("You have reached the limit for this type of plot");
					return;
				}
				if (!eco.has(((Player)sender).getName(), price)) {
					sender.sendMessage("You don't have enough money to buy that plot!");
					return;
				}
				if (!confirmed) {
					sender.sendMessage("Are you sure you want to buy this plot for " + eco.format(price) + "?");
					requireConfirm(sender, originalArguments);
					return;
				}
				if (plot.getSale() == null) {
					if (plot.getFlags().getFlagBoolean("usetreasury")) {
						plot.parent.treasury += price;
						plot.parent.saveTreasury();
					}
				} else {
					eco.depositPlayer(plot.owner, plot.parent.getWorld().getName(), price);
				}
				eco.withdrawPlayer(((Player)sender).getName(), plot.parent.getWorld().getName(), price);
				plot.setOwner(((Player)sender).getName());
				plot.update();
				sender.sendMessage("You have bought the plot for " + eco.format(price));
			} else if (subcommand.equals("sell")) {
				if (!plot.getFlags().getFlagBoolean("allowsell")) {
					sender.sendMessage("Selling plots to the city is disabled in this city");
					return;
				}
				if (plot.owner == null || !plot.owner.equalsIgnoreCase(((Player)sender).getName())) {
					sender.sendMessage("You can't sell a plot you don't own!");
					return;
				}
				if (!plot.getFlags().getFlagBoolean("allowsell")) {
					sender.sendMessage("§cThis type of plot can't be sold.");
					return;
				}
				if (plot.getFlags().getFlagBoolean("requireempty") && !plot.isEmpty()) {
					sender.sendMessage("Plots must be empty to sell in this city");
					return;
				}
				Double price = plot.getCityPrice((Player)sender, true) * plot.getFlags().getFlagDouble("sellmultiplier");
				if(plot.getFlags().getFlagBoolean("usetreasury") && plot.parent.treasury < price) {
					sender.sendMessage("There isn't enough money in the city treasury to buy this plot");
				}
				if (!confirmed) {
					sender.sendMessage("Are you sure you want to sell this plot for " + eco.format(price) + "?");
					requireConfirm(sender, originalArguments);
					return;
				}
				if(plot.getFlags().getFlagBoolean("usetreasury")) {
					plot.parent.treasury -= price;
					plot.parent.saveTreasury();
				}
				eco.depositPlayer(((Player)sender).getName(), plot.parent.getWorld().getName(), price);
				plot.setOwner(null);
				sender.sendMessage("You have sold the plot for " + eco.format(price));
			} else if (subcommand.equals("offer")) {
				if (!plot.getFlags().getFlagBoolean("playersell")) {
					sender.sendMessage("Putting plots on sale to other players is disabled in this city");
					return;
				}
				if (plot.owner == null || !plot.owner.equalsIgnoreCase(((Player)sender).getName())) {
					sender.sendMessage("You can't put a plot on sale that you don't own!");
					return;
				}
				if (!plot.getFlags().getFlagBoolean("playersell")) {
					sender.sendMessage("§cThis type of plot can't be offered for sale.");
					return;
				}
				if (plot.getSale() != null) {
					sender.sendMessage("This plot is already offered. If you want to change it, please cancel the current offer first.");
					return;
				}
				if (arguments.length < 1 || arguments[0] == null) {
					sender.sendMessage("§cYou must specify the price to offer for.");
					return;
				}
				Double price = parseCurrency(arguments[0]);
				if (price == null) {
					sender.sendMessage("Invalid price: " + arguments[0]);
					return;
				}
				String daysToExpireString = arguments.length >= 2 ? arguments[1] : null;
				Date expires = null;
				if (daysToExpireString != null) {
					int days;
					try {
						days = Integer.parseInt(daysToExpireString);
					} catch (NumberFormatException e) {
						sender.sendMessage("§cInvalid integer: " + daysToExpireString);
						return;
					}
					Calendar cal = Calendar.getInstance();
					cal.add(Calendar.DAY_OF_YEAR, days);
					expires = cal.getTime();
				}
				plot.putForSale(price, expires);
				plot.update();
				sender.sendMessage("The plot has been offered for sale for " + eco.format(price));
				if (daysToExpireString != null) {
					sender.sendMessage("The sale offer will expire in " + daysToExpireString + " syas");
				}
			} else if (subcommand.equals("canceloffer")) {
				if (plot.owner == null || !plot.owner.equalsIgnoreCase(((Player)sender).getName())) {
					sender.sendMessage("You can't cancel a sale offer for plot you don't own!");
					return;
				}
				if (plot.getSale() == null) {
					sender.sendMessage("This plot is not offered for sale.");
					return;
				}
				plot.removeSale();
				plot.update();
				sender.sendMessage("The offer has been canceled");
			} else if (subcommand.equals("abandon")) {
				if (plot.owner == null || !plot.owner.equalsIgnoreCase(((Player)sender).getName())) {
					sender.sendMessage("You can't abandon a plot you don't own!");
					return;
				}
				if (!plot.getFlags().getFlagBoolean("allowabandon")) {
					sender.sendMessage("§cThis type of plot can't be abandoned.");
					return;
				}
				if (plot.getFlags().getFlagBoolean("requireempty") && !plot.isEmpty()) {
					sender.sendMessage("Plots must be empty to abandon in this city");
					return;
				}
				if (!confirmed) {
					sender.sendMessage("Are you sure you want to abandon this plot?");
					requireConfirm(sender, originalArguments);
					return;
				}
				plot.setOwner(null);
				sender.sendMessage("The plot has been abandoned.");
			} else {
				sender.sendMessage("§cInvalid subcommand: " + arguments[0]);
			}
		}
		
		private Map<String,Integer> argumentCount = new HashMap<String, Integer>();
		
		public void init() {
			argumentCount.put("offer", 2);
		}
		
		});
		
		addCommand("addplot", new Command() {public void run(CommandSender sender, String[] arguments,	boolean confirmed) {
			if (!checkPermission(sender, arguments)) {
				sender.sendMessage(NO_PERMISSION_MESSAGE);
				return;
			}
			Claim claim = getClaim(sender, null, "claim", true);
			Claim parentClaim = claim.parent;
			if (parentClaim == null) {
				sender.sendMessage("§cThat claim is not a subdivision");
				return;
			}
			City city = City.getCity(parentClaim);
			if (city == null) {
				sender.sendMessage("That claim is not in a city");
				return;
			}
			if (city.addPlot(claim, true)) {
				sender.sendMessage("Plot successfully added to city");				
			} else {
				sender.sendMessage("§cThat claim is already a plot");
			}
		}});
		
		addCommand("removeplot", new Command() {public void run(CommandSender sender, String[] arguments,	boolean confirmed) {
			if (!checkPermission(sender, arguments)) {
				sender.sendMessage(NO_PERMISSION_MESSAGE);
				return;
			}
			Plot plot;
			City city;
			if (arguments.length >= 2) {
				city = City.getCity(arguments[1]);
				if (city == null) {
					sender.sendMessage("§cThere is no city with that name");
					return;
				}
				plot = Plot.getPlot(city, arguments[2]);
				if (plot == null) {
					sender.sendMessage("§cThere's no plot with that name or id.");
					return;
				}
			} else {
				if (!(sender instanceof Player)) {
					sender.sendMessage("§cThis command requires a city and plot if not run by a player");
					return;
				}
				plot = Plot.getPlot(((Player)sender).getLocation());
				if (plot == null) {
					sender.sendMessage("§cThere's no plot where you're standing.");
					return;
				}
				city = plot.parent;
			}
			if (!confirmed) {
				requireConfirm(sender, arguments);
			}
			city.removePlot(plot, true);
			sender.sendMessage("Plot successfully removed frome city");
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
				List<String> helpLines = new ArrayList<String>(commands.size());
				for (Command command : commands.values()) {
					if (command.checkPermission(sender, null)) {
						helpLines.add("§9/city " + command.name + ": §b" + command.description);
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
				int lines = page == pages ? helpLines.size() % 10 : 10;
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
				outputText = new String[3 + command.subusages.size()];
				outputText[0] = "§9Help for command: §b/city " + command.name;
				outputText[1] = "§9Description: §b" + command.description;
				outputText[2] = "§9Usage: §b" + command.usage;
				int line = 3;
				for (String subusage : command.subusages) {
					outputText[line] = "§9Sub-usage: §b" + subusage;
					line++;
				}
			}
			sender.sendMessage(outputText);			
		}});
		
		FileConfiguration commandFile = YamlConfiguration.loadConfiguration(CommandHandler.class.getResourceAsStream("/commands.yml"));
		for (Command command : commands.values()) {
			if (commandFile.isConfigurationSection(command.name)) {
				command.description = commandFile.getString(command.name + ".description", "Description is mising");
				command.usage = commandFile.getString(command.name + ".usage", "Usage is missing");
				command.permissions = new HashSet<String>(commandFile.getStringList(command.name + ".permissions"));
				command.subusages = commandFile.getStringList(command.name + ".subusages");
			}
			command.init();
		}
		
		eco = CityClaims.instance.economy;
	}
	
	private static Claim getClaim(CommandSender sender, String sub1, String sub2) {//Utility method. Returns claim if successful and error message if not.
		return getClaim(sender, sub1, sub2, false);	
	}
	
	private static Claim getClaim(CommandSender sender, String sub1, String sub2, boolean requirePlayer) {//Utility method. Returns claim if successful and error message if not.
		if (!(sender instanceof Player)) {
			sender.sendMessage(requirePlayer ? "This command must be run by a player" : "This command requires a " + sub1 + " if not run by a player");
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
	
	private static void flagCommand(CommandSender sender, Flags flags, String mode, String arg1, String arg2) {
		if (mode.equals("list")) {
			String flagList = "";
			for (String flag : flags.listFlags()) {
				flagList = flagList + ((arg2 == "all" || flags.getFlag(flag) != null) ? ((flagList == "" ? "" : ", ") + flag) : "");
			}
			sender.sendMessage("Flags: " + ((flagList != "") ? flagList : "None"));
		} else if (mode.equals("get")) {
			if (arg1 == null) {
				sender.sendMessage("You must specify a flag.");
				return;
			}
			Object value = flags.getFlag(arg1);
			if (value == null) {
				sender.sendMessage("That flag does not exist.");
				return;
			}
			sender.sendMessage(arg1 + ": " + value.toString());
		} else if (mode.equals("set")) {
			if (arg2 == null) {
				sender.sendMessage("You must specify a flag and a value.");
				return;
			}
			String type = flags.getFlagTypes().get(arg1);
			if(type == null) {
				sender.sendMessage("That flag does not exist.");
				return;
			}
			Object value = flags.getFlagValueFromString(arg1, arg2);
			if (value == null || type == "boolean" && !(value instanceof Boolean) || type == "integer" && !(value instanceof Integer) || type == "double" && !(value instanceof Double) || type == "string" && !(value instanceof String)) {
				sender.sendMessage("Flag value is not a valid " + type);
				return;
			}
			if (flags instanceof CityFlags ? ((CityFlags)flags).setFlag(arg1, value, sender.hasPermission("cityclaims.admin.overridelocks")) : flags.setFlag(arg1, value)) {
				sender.sendMessage("Successfully set value of flag " + arg1 + " to " + arg2);
			} else {
				sender.sendMessage("That flag has been globally locked");
			}
		} else if (mode.equals("unset")) {
			if (arg1 == null) {
				sender.sendMessage("You must specify a flag.");
				return;
			}
			if (flags.removeFlag(arg1)) {
				sender.sendMessage("Flag is no longer set.");
			} else {
				sender.sendMessage("That flag is not set!");
			}
		} else 
		if (mode.equals("lock")) {
			if (arg1 == null) {
				sender.sendMessage("You must specify a flag.");
				return;
			}
			if (flags.getFlagTypes().get(arg1) == null) {
				sender.sendMessage("That flag does not exist.");
				return;
			}
			CityClaims.instance.lockedFlags.add(arg1);
		} else if (mode.equals("unlock")) {
			if (arg1 == null) {
				sender.sendMessage("You must specify a flag.");
				return;
			}
			if (!CityClaims.instance.lockedFlags.remove(arg1)) {
				sender.sendMessage("That flag does not exist.");
				return;
			}
			CityClaims.instance.lockedFlags.remove(arg1);
		} else if (mode.equals("listlocks")) {
			String lockedString = "";
			for (String flag : CityClaims.instance.lockedFlags) {
				lockedString = lockedString + (lockedString == "" ? "" : ", ") + flag;
			}
			sender.sendMessage("Locked Flags: " + lockedString);
		} else {
			sender.sendMessage("Invalid flag mode: " + mode);
		}
	}
	
	private static Pattern currencyPattern = Pattern.compile("[-0-9]*([0-9]+(\\.[0-9]+)?)");
	public static Double parseCurrency(String currency) {
		try {
			Matcher m = currencyPattern.matcher(currency);
			if (!m.find()) {
				return null;
			}
			return Double.parseDouble(m.group());
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static void addCommand(String name, Command command) {
		command.name = name;
		commands.put(name, command);
	}
}