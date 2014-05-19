package faubiguy.cityclaims;

import org.bukkit.command.CommandSender;
import org.bukkit.scheduler.BukkitRunnable;

public class CommandConfirmationTimeoutTask extends BukkitRunnable {
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
		if(CommandHandler.unconfirmed.remove(sender.getName()) != null) {
			sender.sendMessage("Confirmation for the command '/city " + commandName + "' has timed out.");
		}
		
	}
}