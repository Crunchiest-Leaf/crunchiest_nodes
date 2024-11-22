package com.crunchiest.listeners;

import com.crunchiest.CrunchiestNodes;
import com.crunchiest.database.DatabaseManager;
import com.crunchiest.util.ItemStackSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;

import java.io.IOException;
import java.sql.SQLException;
import java.util.UUID;

public class NodeEventListener implements Listener {
    private final CrunchiestNodes plugin;
    private final DatabaseManager dbManager;

    public NodeEventListener(CrunchiestNodes plugin, DatabaseManager dbManager) {
        this.plugin = plugin;
        this.dbManager = dbManager;
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        UUID playerUUID = player.getUniqueId();

        if (plugin.getNodeBuilderCommand().isNodeDeleter(playerUUID)) {
            handleNodeDeletion(player, event.getBlock(), event);
        } else {
            handleNodeMining(player, event.getBlock(), event);
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            handleNodeMining(event.getPlayer(), event.getClickedBlock(), event);
        }
    }

    private void handleNodeMining(Player player, Block block, Cancellable event) {
        try {
            // Check if the block is a registered node
            int nodeId = dbManager.getNodeId(block.getWorld().getName(), block.getX(), block.getY(), block.getZ());
            if (nodeId != -1) {
                // Check if the node is on cooldown
                boolean isGlobal = dbManager.isNodeGlobal(nodeId);
                boolean isOnCooldown = isGlobal ? dbManager.isNodeOnGlobalCooldown(nodeId) : dbManager.isNodeOnCooldown(nodeId, player.getUniqueId());
                if (isOnCooldown) {
                    player.sendMessage("This node is on cooldown.");
                    event.setCancelled(true);
                    return;
                }

                // Get the node's item stack and tool
                String itemStackBase64 = dbManager.getNodeItemStack(nodeId);
                String toolBase64 = dbManager.getNodeTool(nodeId);
                ItemStack itemStack = ItemStackSerializer.deserialize(itemStackBase64);
                ItemStack tool = ItemStackSerializer.deserialize(toolBase64);

                // Check if the player is using the correct tool
                boolean requireBetterTool = dbManager.isNodeRequireBetterTool(nodeId);
                if (!isToolValid(player.getInventory().getItemInMainHand(), tool, requireBetterTool)) {
                    player.sendMessage("You need to use the correct tool to mine this node.");
                    event.setCancelled(true);
                    return;
                }

                // Drop the item stack
                block.getWorld().dropItemNaturally(block.getLocation(), itemStack);

                // Set the block to bedrock to indicate cooldown
                if (isGlobal) {
                    block.setType(Material.BEDROCK);
                } else {
                    player.sendBlockChange(block.getLocation(), Material.BEDROCK.createBlockData());
                }

                // Set the cooldown
                long cooldownEnd = System.currentTimeMillis() + dbManager.getNodeCooldown(nodeId);
                if (isGlobal) {
                    dbManager.setGlobalCooldown(nodeId, cooldownEnd);
                } else {
                    dbManager.setCooldown(nodeId, player.getUniqueId(), cooldownEnd);
                }

                // Schedule a task to reset the block after the cooldown
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (isGlobal) {
                        block.setType(Material.STONE);
                    } else {
                        player.sendBlockChange(block.getLocation(), block.getBlockData());
                    }
                }, dbManager.getNodeCooldown(nodeId) / 50);
            }
        } catch (SQLException | IOException | ClassNotFoundException e) {
            player.sendMessage("An error occurred while processing the node.");
            e.printStackTrace();
        }
    }

    private void handleNodeDeletion(Player player, Block block, Cancellable event) {
        try {
            // Check if the block is a registered node
            int nodeId = dbManager.getNodeId(block.getWorld().getName(), block.getX(), block.getY(), block.getZ());
            if (nodeId != -1) {
                // Delete the node from the database
                dbManager.deleteNode(nodeId);
                player.sendMessage("Node deleted successfully.");
                event.setCancelled(true);
            }
        } catch (SQLException e) {
            player.sendMessage("An error occurred while deleting the node.");
            e.printStackTrace();
        }
    }

    private boolean isToolValid(ItemStack playerTool, ItemStack requiredTool, boolean requireBetterTool) {
        if (requireBetterTool) {
            // Check if the player's tool is equal to or better than the required tool
            return playerTool.getType().getMaxDurability() >= requiredTool.getType().getMaxDurability();
        } else {
            // Check if the player's tool is the same as the required tool
            return playerTool.isSimilar(requiredTool);
        }
    }
}