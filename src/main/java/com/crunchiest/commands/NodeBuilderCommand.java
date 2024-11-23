package com.crunchiest.commands;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;

import com.crunchiest.CrunchiestNodes;
import com.crunchiest.database.DatabaseManager;

import net.md_5.bungee.api.ChatColor;

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
            sender.sendMessage(ChatColor.RED + "This command can only be run by a player.");
            return true;
        }

        Player player = (Player) sender;
        UUID playerUUID = player.getUniqueId();

        if (label.equalsIgnoreCase("nodebuilder")) {
            if (nodeBuilders.contains(playerUUID)) {
                nodeBuilders.remove(playerUUID);
                player.sendMessage(ChatColor.GREEN + "Node registration mode disabled.");
                return true;
            }

            if (args.length < 3) {
                player.sendMessage(ChatColor.GREEN + "Usage: /nodebuilder <cooldown> <global|player> <tool|better>");
                return true;
            }

            long cooldown;
            try {
              cooldown = Long.parseLong(args[0]) * 60 * 1000;
            } catch (NumberFormatException e) {
                player.sendMessage(ChatColor.RED + "Invalid cooldown value.");
                return true;
            }

            boolean isGlobal = args[1].equalsIgnoreCase("global");
            boolean requireBetterTool = args[2].equalsIgnoreCase("better");

            player.setMetadata("nodebuilder_cooldown", new FixedMetadataValue(plugin, cooldown));
            player.setMetadata("nodebuilder_isGlobal", new FixedMetadataValue(plugin, isGlobal));
            player.setMetadata("nodebuilder_requireBetterTool", new FixedMetadataValue(plugin, requireBetterTool));

            nodeBuilders.add(playerUUID);
            player.sendMessage(ChatColor.GREEN + "Node registration mode enabled. Click a block to register it as a node. Run the command again to disable.");
        } else if (label.equalsIgnoreCase("nodedeleter")) {
            if (nodeDeleters.contains(playerUUID)) {
                nodeDeleters.remove(playerUUID);
                player.sendMessage(ChatColor.GREEN + "Node deletion mode disabled.");
                return true;
            }

            nodeDeleters.add(playerUUID);
            player.sendMessage(ChatColor.GREEN + "Node deletion mode enabled. Left-click nodes to delete them. Run the command again to disable.");
        }

        return true;
    }

    public boolean isNodeBuilder(UUID playerUUID) {
        return nodeBuilders.contains(playerUUID);
    }

    public boolean isNodeDeleter(UUID playerUUID) {
        return nodeDeleters.contains(playerUUID);
    }

    public void removeNodeBuilder(UUID playerUUID) {
        nodeBuilders.remove(playerUUID);
    }
}