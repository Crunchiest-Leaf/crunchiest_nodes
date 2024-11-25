package com.crunchiest.commands;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;

import com.crunchiest.CrunchiestNodes;
import com.crunchiest.database.DatabaseManager;

import net.md_5.bungee.api.ChatColor;

public class NodeBuilderCommand implements CommandExecutor {
    private final CrunchiestNodes plugin;
    private final DatabaseManager dbManager;
    private final Set<UUID> nodeBuilders = new HashSet<>();
    private final Set<UUID> nodeDeleters = new HashSet<>();
    private final Set<UUID> nodeHighlighters = new HashSet<>();

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

            if (nodeDeleters.contains(playerUUID)) {
                nodeDeleters.remove(playerUUID);
                player.sendMessage(ChatColor.GREEN + "Node deletion mode disabled.");
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
            if (nodeBuilders.contains(playerUUID)) {
                nodeBuilders.remove(playerUUID);
                player.sendMessage(ChatColor.GREEN + "Node registration mode disabled.");
            }
            nodeDeleters.add(playerUUID);
            player.sendMessage(ChatColor.GREEN + "Node deletion mode enabled. Left-click nodes to delete them. Run the command again to disable.");
        } else if (label.equalsIgnoreCase("nodehighlighter")) {
            if (nodeHighlighters.contains(playerUUID)) {
                nodeHighlighters.remove(playerUUID);
                player.sendMessage(ChatColor.GREEN + "Node highlighting mode disabled.");
                return true;
            }

            nodeHighlighters.add(playerUUID);
            player.sendMessage(ChatColor.GREEN + "Node highlighting mode enabled. Nodes within 30 blocks will flash for 10 seconds.");

            new BukkitRunnable() {
                @Override
                public void run() {
                    if (!nodeHighlighters.contains(playerUUID)) {
                        cancel();
                        return;
                    }

                    Location playerLocation = player.getLocation();
                    int radius = 10;
                    List<Block> blocksToHighlight = new ArrayList<>();

                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            for (int x = -radius; x <= radius; x++) {
                                for (int y = -radius; y <= radius; y++) {
                                    for (int z = -radius; z <= radius; z++) {
                                        Block block = playerLocation.clone().add(x, y, z).getBlock();
                                        try {
                                            if (isNode(block)) {
                                                blocksToHighlight.add(block);
                                            }
                                        } catch (SQLException ex) {
                                            ex.printStackTrace();
                                        } catch (InterruptedException | ExecutionException ex) {
                                        }
                                    }
                                }
                            }

                            // Schedule the block change updates to run on the main thread
                            Bukkit.getScheduler().runTask(plugin, () -> {
                                for (Block block : blocksToHighlight) {
                                    Material originalMaterial = block.getType();
                                    player.sendBlockChange(block.getLocation(), Material.WHITE_CONCRETE.createBlockData());
                                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                                        player.sendBlockChange(block.getLocation(), originalMaterial.createBlockData());
                                    }, 5L);
                                }
                            });
                        }
                    }.runTaskAsynchronously(plugin);
                }
            }.runTaskTimer(plugin, 0L, 10L);

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                nodeHighlighters.remove(playerUUID);
                player.sendMessage(ChatColor.GREEN + "Node highlighting mode automatically disabled after 10 seconds.");
            }, 200L); // 10 seconds later
        }

        return true;
    }

    private boolean isNode(Block block) throws SQLException, InterruptedException, ExecutionException {
        int nodeId = dbManager.getNodeId(block.getWorld().getName(), block.getX(), block.getY(), block.getZ()).get();
        return nodeId != -1;
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