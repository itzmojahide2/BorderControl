package com.yourusername.bordercontrol.commands;

import com.yourusername.bordercontrol.BorderControl;
import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityResurrectEvent;

public class BorderCommand implements CommandExecutor {

    private final BorderControl plugin;

    public BorderCommand(BorderControl plugin) {
        this.plugin = plugin;
    }

    // Helper method to get messages from config and add color/prefix
    private String getMessage(String path) {
        FileConfiguration config = plugin.getConfig();
        String prefix = config.getString("plugin_prefix", "&6[BorderControl] &r");
        String message = config.getString("messages." + path, "&cMessage not found: " + path);
        return ChatColor.translateAlternateColorCodes('&', prefix + message);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(getMessage("unknown_command"));
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
                sender.sendMessage(getMessage("unknown_command"));
                break;
        }
        return true;
    }

    private void handleSet(CommandSender sender, String[] args) {
        if (!plugin.getConfig().getBoolean("commands.admin.border_set")) {
            sender.sendMessage(getMessage("no_permission"));
            return;
        }
        if (!sender.isOp()) {
            sender.sendMessage(getMessage("no_permission"));
            return;
        }
        if (args.length < 2 || args.length > 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /border set <size> [seconds]");
            return;
        }

        try {
            double size = Double.parseDouble(args[1]);
            long seconds;

            if (args.length == 3) {
                seconds = Long.parseLong(args[2]);
            } else {
                seconds = plugin.getConfig().getLong("border_settings.default_change_duration", 0);
            }

            World world = (sender instanceof Player) ? ((Player) sender).getWorld() : Bukkit.getWorlds().get(0);

            if (world != null) {
                WorldBorder border = world.getWorldBorder();
                String message;

                if (seconds > 0) {
                    border.setSize(size, seconds);
                    message = getMessage("border_set_smooth")
                            .replace("%size%", String.valueOf(size))
                            .replace("%seconds%", String.valueOf(seconds));
                } else {
                    border.setSize(size);
                    message = getMessage("border_set_instant")
                            .replace("%size%", String.valueOf(size));
                }
                sender.sendMessage(message);
                playEffectsToAll();
            }
        } catch (NumberFormatException e) {
            sender.sendMessage(getMessage("invalid_number"));
        }
    }

    private void handleReload(CommandSender sender) {
        if (!sender.isOp()) {
            sender.sendMessage(getMessage("no_permission"));
            return;
        }
        plugin.reloadConfig();
        sender.sendMessage(getMessage("reload_success"));
    }

    private void handleLogo(CommandSender sender, String[] args) {
        if (!sender.isOp()) {
            sender.sendMessage(getMessage("no_permission"));
            return;
        }
        if (args.length != 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /border logo <url>");
            return;
        }
        String url = args[1];
        plugin.getConfig().set("logo_url", url);
        plugin.saveConfig();
        sender.sendMessage(getMessage("logo_updated"));
    }

    private void handleSee(CommandSender sender) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            WorldBorder border = player.getWorld().getWorldBorder();
            String message = getMessage("current_size")
                    .replace("%size%", String.format("%.1f", border.getSize()));
            player.sendMessage(message);
        } else {
            sender.sendMessage(ChatColor.RED + "This command can only be used by players.");
        }
    }

    private void handleHelp(CommandSender sender) {
        if (sender.isOp()) {
            sender.sendMessage(ChatColor.GOLD + "--- BorderControl Admin Help ---");
            // THIS IS THE CORRECTED LINE:
            sender.sendMessage(ChatColor.YELLOW + "/border set <size> [seconds] - Sets the border size, optionally over time.");
            sender.sendMessage(ChatColor.YELLOW + "/border reload - Reloads the config file.");
            sender.sendMessage(ChatColor.YELLOW + "/border logo <url> - Sets the logo URL in the config.");
            sender.sendMessage(ChatColor.YELLOW + "/border see - Shows the current border size.");
            sender.sendMessage(ChatColor.YELLOW + "/border help - Shows this help message.");
        } else {
            sender.sendMessage(ChatColor.GOLD + "--- BorderControl Help ---");
            sender.sendMessage(ChatColor.YELLOW + "/border see - Shows the current border size.");
            sender.sendMessage(ChatColor.YELLOW + "/border help - Shows this help message.");
        }
    }

    private void playEffectsToAll() {
        FileConfiguration config = plugin.getConfig();
        boolean animationEnabled = config.getBoolean("effects.totem_animation", true);
        boolean soundEnabled = config.getBoolean("effects.sound.enabled", true);

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (animationEnabled) {
                EntityResurrectEvent resurrectEvent = new EntityResurrectEvent(player);
                Bukkit.getPluginManager().callEvent(resurrectEvent);
            }

            if (soundEnabled) {
                try {
                    Sound sound = Sound.valueOf(config.getString("effects.sound.sound_name", "ITEM_TOTEM_USE").toUpperCase());
                    float volume = (float) config.getDouble("effects.sound.volume", 1.0);
                    float pitch = (float) config.getDouble("effects.sound.pitch", 1.0);
                    player.playSound(player.getLocation(), sound, volume, pitch);
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid sound name in config.yml: " + config.getString("effects.sound.sound_name"));
                }
            }
        }
    }
              }
