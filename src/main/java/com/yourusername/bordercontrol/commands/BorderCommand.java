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

    // NEW HELPER FUNCTION TO PARSE TIME (e.g., 5m, 1h)
    private long parseTime(String timeString) {
        if (timeString == null || timeString.isEmpty()) {
            return 0;
        }
        char unit = timeString.charAt(timeString.length() - 1);
        String value = timeString.substring(0, timeString.length() - 1);
        long multiplier = 1;

        switch (Character.toLowerCase(unit)) {
            case 's':
                multiplier = 1;
                break;
            case 'm':
                multiplier = 60;
                break;
            case 'h':
                multiplier = 3600; // 60 minutes * 60 seconds
                break;
            default:
                // If no unit is found, assume the whole string is seconds
                value = timeString;
                break;
        }
        return Long.parseLong(value) * multiplier;
    }


    private void handleSet(CommandSender sender, String[] args) {
        if (!plugin.getConfig().getBoolean("commands.admin.border_set", true)) {
            sender.sendMessage(getMessage("no_permission"));
            return;
        }
        if (!sender.isOp()) {
            sender.sendMessage(getMessage("no_permission"));
            return;
        }
        if (args.length < 2 || args.length > 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /border set <size> [time]");
            return;
        }

        try {
            double size = Double.parseDouble(args[1]);
            long seconds;

            if (args.length == 3) {
                // Use the new smart time parser!
                seconds = parseTime(args[2]);
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
        if (!plugin.getConfig().getBoolean("commands.admin.border_reload", true)) {
            sender.sendMessage(getMessage("no_permission"));
            return;
        }
        if (!sender.isOp()) {
            sender.sendMessage(getMessage("no_permission"));
            return;
        }
        plugin.reloadConfig();
        sender.sendMessage(getMessage("reload_success"));
    }

    private void handleSee(CommandSender sender) {
        if (!plugin.getConfig().getBoolean("commands.user.border_see", true)) {
            sender.sendMessage(getMessage("no_permission"));
            return;
        }
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
        // UPDATED HELP MESSAGE to explain the new time format.
        if (sender.isOp()) {
            if (!plugin.getConfig().getBoolean("commands.admin.border_help", true)) return;
            sender.sendMessage(ChatColor.GOLD + "--- BorderControl Admin Help ---");
            sender.sendMessage(ChatColor.YELLOW + "/border set <size> [time] - Sets border size (time e.g., 60s, 5m, 1h).");
            sender.sendMessage(ChatColor.YELLOW + "/border reload - Reloads the config file.");
            sender.sendMessage(ChatColor.YELLOW + "/border see - Shows the current border size.");
            sender.sendMessage(ChatColor.YELLOW + "/border help - Shows this help message.");
        } else {
            if (!plugin.getConfig().getBoolean("commands.user.border_help", true)) return;
            sender.sendMessage(ChatColor.GOLD + "--- BorderControl Help ---");
            sender.sendMessage(ChatColor.YELLOW + "/border see - Shows the current border size.");
            sender.sendMessage(ChatColor.YELLOW + "/border help - Shows this help message.");
        }
    }

    private void playEffectsToAll() {
        FileConfiguration config = plugin.getConfig();
        boolean animationEnabled = config.getBoolean("effects.totem_animation", true);
        boolean particlesEnabled = config.getBoolean("effects.particles.enabled", true);
        boolean soundEnabled = config.getBoolean("effects.sound.enabled", true);

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (animationEnabled) {
                EntityResurrectEvent resurrectEvent = new EntityResurrectEvent(player);
                Bukkit.getPluginManager().callEvent(resurrectEvent);
            }

            if (particlesEnabled) {
                try {
                    Particle particle = Particle.valueOf(config.getString("effects.particles.particle_type", "TOTEM_OF_UNDYING").toUpperCase());
                    int count = config.getInt("effects.particles.count", 75);
                    player.spawnParticle(particle, player.getLocation().add(0, 1, 0), count, 0.5, 0.5, 0.5, 0.1);
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid particle name in config.yml!");
                }
            }

            if (soundEnabled) {
                try {
                    Sound sound = Sound.valueOf(config.getString("effects.sound.sound_name", "ITEM_TOTEM_USE").toUpperCase());
                    float volume = (float) config.getDouble("effects.sound.volume", 1.0);
                    float pitch = (float) config.getDouble("effects.sound.pitch", 1.0);
                    player.playSound(player.getLocation(), sound, volume, pitch);
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid sound name in config.yml!");
                }
            }
        }
    }
          }
