package com.crunchiest.commands;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.crunchiest.CrunchiestNodes;
import com.crunchiest.database.DatabaseManager;
import com.crunchiest.util.ItemStackSerializer;

public class NodeBuilderCommand implements CommandExecutor {
    private final CrunchiestNodes plugin;
    private final DatabaseManager dbManager;
    private final Set<UUID> nodeBuilders = new HashSet<>();
    private final Set<UUID> nodeDeleters = new HashSet<>();

    public NodeBuilderCommand(CrunchiestNodes plugin, DatabaseManager dbManager) {
        this.plugin = plugin;
        this.dbManager = dbManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be run by a player.");
            return true;
        }

        Player player = (Player) sender;
        UUID playerUUID = player.getUniqueId();

        if (label.equalsIgnoreCase("nodebuilder")) {
            if (nodeBuilders.contains(playerUUID)) {
                nodeBuilders.remove(playerUUID);
                player.sendMessage("Node registration mode disabled.");
                return true;
            }

            if (args.length < 3) {
                player.sendMessage("Usage: /nodebuilder <cooldown> <global|player> <tool|better>");
                return true;
            }

            long cooldown;
            try {
                cooldown = Long.parseLong(args[0]);
            } catch (NumberFormatException e) {
                player.sendMessage("Invalid cooldown value.");
                return true;
            }

            boolean isGlobal = args[1].equalsIgnoreCase("global");
            boolean requireBetterTool = args[2].equalsIgnoreCase("better");

            try {
                String itemStackSerialized = ItemStackSerializer.serialize(player.getInventory().getItemInOffHand());
                String toolSerialized = ItemStackSerializer.serialize(player.getInventory().getItemInMainHand());
                dbManager.registerNode(player.getWorld().getName(), player.getLocation().getBlockX(), player.getLocation().getBlockY(), player.getLocation().getBlockZ(), itemStackSerialized, toolSerialized, cooldown, isGlobal, requireBetterTool);
                player.sendMessage("Node registered successfully.");
                nodeBuilders.add(playerUUID);
                player.sendMessage("Node registration mode enabled. Run the command again to disable.");
            } catch (Exception e) {
                player.sendMessage("Failed to register node: " + e.getMessage());
            }
        } else if (label.equalsIgnoreCase("nodedeleter")) {
            if (nodeDeleters.contains(playerUUID)) {
                nodeDeleters.remove(playerUUID);
                player.sendMessage("Node deletion mode disabled.");
                return true;
            }

            nodeDeleters.add(playerUUID);
            player.sendMessage("Node deletion mode enabled. Left-click nodes to delete them. Run the command again to disable.");
        }

        return true;
    }

    public boolean isNodeDeleter(UUID playerUUID) {
        return nodeDeleters.contains(playerUUID);
    }
}