package com.yourusername.bordercontrol.commands;

import com.yourusername.bordercontrol.BorderControl;
import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class BorderCommand implements CommandExecutor {

    private final BorderControl plugin;

    public BorderCommand(BorderControl plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(ChatColor.RED + "Usage: /border <subcommand>");
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "set":
                handleSet(sender, args);
                break;
            case "reload":
                handleReload(sender);
                break;
            case "logo":
                handleLogo(sender, args);
                break;
            case "see":
                handleSee(sender);
                break;
            case "help":
                handleHelp(sender);
                break;
            default:
                sender.sendMessage(ChatColor.RED + "Unknown subcommand. Use /border help.");
                break;
        }
        return true;
    }

    private void handleSet(CommandSender sender, String[] args) {
        if (!plugin.getConfig().getBoolean("commands.admin.border_set")) {
            sender.sendMessage(ChatColor.RED + "This command is disabled.");
            return;
        }
        if (!sender.isOp()) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            return;
        }
        if (args.length != 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /border set <size>");
            return;
        }
        try {
            double size = Double.parseDouble(args[1]);
            World world = (sender instanceof Player) ? ((Player) sender).getWorld() : Bukkit.getWorlds().get(0);

            if (world != null) {
                WorldBorder border = world.getWorldBorder();
                border.setSize(size);
                sender.sendMessage(ChatColor.GREEN + "World border for '" + world.getName() + "' has been set to " + size + "x" + size + ".");
                playTotemEffectToAll();
            } else {
                sender.sendMessage(ChatColor.RED + "Could not find a world to apply the border to.");
            }
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "Invalid size. Please enter a number.");
        }
    }

    private void handleReload(CommandSender sender) {
        if (!plugin.getConfig().getBoolean("commands.admin.border_reload")) {
            sender.sendMessage(ChatColor.RED + "This command is disabled.");
            return;
        }
        if (!sender.isOp()) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            return;
        }
        plugin.reloadConfig();
        sender.sendMessage(ChatColor.GREEN + "BorderControl configuration has been reloaded.");
    }

    private void handleLogo(CommandSender sender, String[] args) {
        if (!plugin.getConfig().getBoolean("commands.admin.border_logo")) {
            sender.sendMessage(ChatColor.RED + "This command is disabled.");
            return;
        }
        if (!sender.isOp()) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            return;
        }
        if (args.length != 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /border logo <url>");
            return;
        }
        String url = args[1];
        plugin.getConfig().set("logo_url", url);
        plugin.saveConfig();
        sender.sendMessage(ChatColor.GREEN + "Logo URL has been updated.");
    }

    private void handleSee(CommandSender sender) {
        if (!plugin.getConfig().getBoolean("commands.user.border_see")) {
            sender.sendMessage(ChatColor.RED + "This command is disabled.");
            return;
        }
        if (sender instanceof Player) {
            Player player = (Player) sender;
            WorldBorder border = player.getWorld().getWorldBorder();
            sender.sendMessage(ChatColor.AQUA + "Current border size: " + String.format("%.1f", border.getSize()));
        } else {
            sender.sendMessage(ChatColor.RED + "This command can only be used by players.");
        }
    }

    private void handleHelp(CommandSender sender) {
        if (sender.isOp()) {
            if (!plugin.getConfig().getBoolean("commands.admin.border_help")) {
                sender.sendMessage(ChatColor.RED + "This command is disabled.");
                return;
            }
            sender.sendMessage(ChatColor.GOLD + "--- BorderControl Admin Help ---");
            sender.sendMessage(ChatColor.YELLOW + "/border set <size> - Sets the world border size.");
            sender.sendMessage(ChatColor.YELLOW + "/border reload - Reloads the config file.");
            sender.sendMessage(ChatColor.YELLOW + "/border logo <url> - Sets the logo URL in the config.");
            sender.sendMessage(ChatColor.YELLOW + "/border see - Shows the current border size.");
            sender.sendMessage(ChatColor.YELLOW + "/border help - Shows this help message.");
        } else {
            if (!plugin.getConfig().getBoolean("commands.user.border_help")) {
                sender.sendMessage(ChatColor.RED + "This command is disabled.");
                return;
            }
            sender.sendMessage(ChatColor.GOLD + "--- BorderControl Help ---");
            sender.sendMessage(ChatColor.YELLOW + "/border see - Shows the current border size.");
            sender.sendMessage(ChatColor.YELLOW + "/border help - Shows this help message.");
        }
    }

    private void playTotemEffectToAll() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.playSound(player.getLocation(), Sound.ITEM_TOTEM_USE, 1.0f, 1.0f);
            player.spawnParticle(Particle.TOTEM_OF_UNDYING, player.getLocation().add(0, 1, 0), 50, 0.5, 0.5, 0.5, 0.1);
        }
    }
}