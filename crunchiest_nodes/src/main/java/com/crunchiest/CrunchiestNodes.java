package com.crunchiest;

import java.util.logging.Logger;

import org.bukkit.plugin.java.JavaPlugin;

import com.crunchiest.commands.NodeBuilderCommand;
import com.crunchiest.database.DatabaseManager;
import com.crunchiest.listeners.NodeEventListener;

public class CrunchiestNodes extends JavaPlugin {
    private static final Logger LOGGER = Logger.getLogger("crunchiest_nodes");
    private DatabaseManager dbManager;
    private NodeBuilderCommand nodeBuilderCommand;

    @Override
    public void onEnable() {
        LOGGER.info("crunchiest_nodes plugin enabled");

        // Initialize database
        try {
            dbManager = new DatabaseManager();
            dbManager.connect();
            // Perform any necessary database setup
        } catch (Exception e) {
            LOGGER.severe("Failed to connect to the database: " + e.getMessage());
        }

                // Register command
                nodeBuilderCommand = new NodeBuilderCommand(this, dbManager);
                this.getCommand("nodebuilder").setExecutor(nodeBuilderCommand);

                // Register event listener
                getServer().getPluginManager().registerEvents(new NodeEventListener(this, dbManager), this);
    }

    @Override
    public void onDisable() {
        LOGGER.info("crunchiest_nodes plugin disabled");
        // Close database connection
        try {
            DatabaseManager dbManager = new DatabaseManager();
            dbManager.disconnect();
        } catch (Exception e) {
            LOGGER.severe("Failed to disconnect from the database: " + e.getMessage());
        }
    }

    public NodeBuilderCommand getNodeBuilderCommand() {
      return this.nodeBuilderCommand;
   }
}