package me.evade.noMoreCollision;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class CollisionCommand implements CommandExecutor, TabCompleter {
    private final NoMoreCollision plugin;

    public CollisionCommand(NoMoreCollision plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("collision")) {
            if (!sender.hasPermission("nomorecollision.toggle")) {
                sender.sendMessage("§cYou don't have permission to use this command.");
                return true;
            }

            if (args.length == 0) {
                if (sender instanceof Player) {
                    Player player = (Player) sender;
                    plugin.openCollisionGUI(player);
                } else {
                    sender.sendMessage("§cConsole cannot open GUI. Use /collision global to toggle global collision.");
                }
                return true;
            } else if (args.length == 1) {
                if (args[0].equalsIgnoreCase("global")) {
                    if (!sender.hasPermission("nomorecollision.global")) {
                        sender.sendMessage("§cYou don't have permission to toggle global collision.");
                        return true;
                    }

                    boolean newState = !plugin.getGlobalCollisionState();
                    plugin.toggleGlobalCollision();
                    String status = newState ? "enabled" : "disabled";
                    sender.sendMessage("§6[NoMoreCollision] §eGlobal collision has been " + status + " for all players.");
                    return true;
                } else if (args[0].equalsIgnoreCase("self")) {
                    if (!sender.hasPermission("nomorecollision.self")) {
                        sender.sendMessage("§cYou don't have permission to toggle your own collision.");
                        return true;
                    }

                    if (sender instanceof Player) {
                        Player player = (Player) sender;
                        plugin.togglePlayerCollision(player);
                    } else {
                        sender.sendMessage("§cThis command can only be used by players.");
                    }
                    return true;
                } else if (args[0].equalsIgnoreCase("status")) {
                    if (!sender.hasPermission("nomorecollision.status")) {
                        sender.sendMessage("§cYou don't have permission to check collision status.");
                        return true;
                    }

                    if (sender instanceof Player) {
                        Player player = (Player) sender;
                        boolean playerState = plugin.getPlayerCollisionState(player);
                        String playerStatus = playerState ? "enabled" : "disabled";

                        boolean worldState = plugin.getWorldCollisionState(player.getWorld().getName());
                        String worldStatus = worldState ? "enabled" : "disabled";

                        boolean globalState = plugin.getGlobalCollisionState();
                        String globalStatus = globalState ? "enabled" : "disabled";

                        sender.sendMessage("§6[NoMoreCollision] §eYour collision: " + playerStatus);
                        sender.sendMessage("§6[NoMoreCollision] §eWorld collision: " + worldStatus);
                        sender.sendMessage("§6[NoMoreCollision] §eGlobal collision: " + globalStatus);
                    } else {
                        boolean globalState = plugin.getGlobalCollisionState();
                        String globalStatus = globalState ? "enabled" : "disabled";
                        sender.sendMessage("§6[NoMoreCollision] §eGlobal collision: " + globalStatus);
                    }
                    return true;
                } else if (args[0].equalsIgnoreCase("help")) {
                    sendHelp(sender);
                    return true;
                }
            }

            sendHelp(sender);
            return true;
        }
        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (command.getName().equalsIgnoreCase("collision")) {
            return plugin.getCommandSuggestions(args);
        }
        return new ArrayList<>();
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§6==== NoMoreCollision Help ====");
        sender.sendMessage("§e/collision §7- Open a GUI to toggle collision for each world");
        sender.sendMessage("§e/collision self §7- Toggle collision just for yourself");
        sender.sendMessage("§e/collision global §7- Toggle collision for all players on the server");
        sender.sendMessage("§e/collision status §7- Check collision status");
        sender.sendMessage("§e/collision help §7- Show this help message");
    }
}