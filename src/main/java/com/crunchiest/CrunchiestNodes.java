package com.crunchiest;

import java.util.logging.Logger;

import org.bukkit.plugin.java.JavaPlugin;

import com.crunchiest.commands.NodeBuilderCommand;
import com.crunchiest.commands.complete.NodeCommandTabCompleter;
import com.crunchiest.database.DatabaseManager;
import com.crunchiest.listeners.NodeEventListener;

public class CrunchiestNodes extends JavaPlugin {
    private static final Logger LOGGER = Logger.getLogger("crunchiest_nodes");
    private DatabaseManager dbManager;
    private NodeBuilderCommand nodeBuilderCommand;

    @Override
    public void onEnable() {
        LOGGER.info("crunchiest_nodes plugin enabled");

        dbManager = new DatabaseManager();
        try {
            dbManager.connect();
        } catch (Exception e) {
            LOGGER.severe("Failed to connect to the database: " + e.getMessage());
        }

        nodeBuilderCommand = new NodeBuilderCommand(this, dbManager);
        getCommand("nodebuilder").setExecutor(nodeBuilderCommand);
        getCommand("nodebuilder").setTabCompleter(new NodeCommandTabCompleter());
        getCommand("nodedeleter").setExecutor(nodeBuilderCommand);
        getCommand("nodedeleter").setTabCompleter(new NodeCommandTabCompleter());

        // Register event listener
        getServer().getPluginManager().registerEvents(new NodeEventListener(this, dbManager), this);
    }

    @Override
    public void onDisable() {
        LOGGER.info("crunchiest_nodes plugin disabled");
        // Close database connection
        try {
            dbManager.disconnect();
        } catch (Exception e) {
            LOGGER.severe("Failed to disconnect from the database: " + e.getMessage());
        }
    }

    public NodeBuilderCommand getNodeBuilderCommand() {
        return nodeBuilderCommand;
    }
}