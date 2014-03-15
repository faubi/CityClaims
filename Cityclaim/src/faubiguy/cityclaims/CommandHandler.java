package faubiguy.cityclaims;

import java.util.HashMap;
import java.util.Map;

import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.GriefPrevention;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public final class CommandHandler {
	
	public static Map<String,Command> commands;
	public static Map<String,CommandConfirmationTimeoutTask> unconfirmed; //Map from sender's username to unconfirmed command name
	
	public static final String NO_PERMISSION_MESSAGE = "You do not have permission to do this."; 
	
	public static interface Command {
		public abstract void run(CommandSender sender, String[] arguments, boolean confirmed);
		

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
			sender.sendMessage("No such command: /city " + commandName);
			return;
		}
		command.run(sender, arguments, false);
	}
	
	static void initialize() {
		commands = new HashMap<>();
		unconfirmed = new HashMap<>();
		
		commands.put("create", new Command() {public void run(CommandSender sender, String[] arguments, boolean confirmed) {
			if (!(sender.hasPermission("cityclaims.admin.create"))) {
				sender.sendMessage(NO_PERMISSION_MESSAGE);
				return;
			}
			if (!(arguments.length >= 1)) {
				sender.sendMessage("Usage: /city newcity <name> [claim-id]");
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
		
		commands.put("delete", new Command() {public void run(CommandSender sender, String[] arguments, boolean confirmed) {
			if (!(sender.hasPermission("cityclaims.admin.delete"))) {
				sender.sendMessage(NO_PERMISSION_MESSAGE);
				return;
			}
			if (!confirmed) {
				requireConfirm(sender, "deletecity", arguments);
				return;
			}
			City city;
			if (arguments.length >= 1) {
				city = City.getCity(arguments[0]);
				if (city == null) {
					sender.sendMessage("There is no city with that name");
					return;
				}
			} else {
				city = City.getCity(getClaim(sender));
				if (city == null) {
					return;
				}
			}
			city.delete();				
		}});
		
		commands.put("list", new Command() {public void run(CommandSender sender, String[] arguments, boolean confirmed) {
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
		
		commands.put("confirm", new Command() {public void run(CommandSender sender, String[] arguments,	boolean confirmed) {
			CommandConfirmationTimeoutTask timeout = unconfirmed.get(sender.getName());
			if(timeout == null) {
				sender.sendMessage("You have no command awaiting confirmation!");
				return;
			}
			unconfirmed.remove(sender.getName());
			timeout.cancel();
			commands.get(timeout.commandName).run(timeout.sender, timeout.arguments, true);
			return;
		}});
		
		commands.put("cancel",  new Command() {public void run(CommandSender sender, String[] arguments,	boolean confirmed) {
			CommandConfirmationTimeoutTask timeout = unconfirmed.get(sender.getName());
			if(timeout == null) {
				sender.sendMessage("You have no command awaiting confirmation!");
				return;
			}
			unconfirmed.remove(sender.getName());
			timeout.cancel();
			sender.sendMessage("The command has been cancelled.");
			return;
		}});
		
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

}