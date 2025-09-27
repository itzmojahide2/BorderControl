package com.yourusername.bordercontrol;

import com.yourusername.bordercontrol.commands.BorderCommand;
import org.bukkit.plugin.java.JavaPlugin;

public class BorderControl extends JavaPlugin {

    @Override
    public void onEnable() {
        saveDefaultConfig();
        getCommand("border").setExecutor(new BorderCommand(this));
        getLogger().info("BorderControl has been enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("BorderControl has been disabled!");
    }
}