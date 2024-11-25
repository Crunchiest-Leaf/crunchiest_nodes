package com.crunchiest.listeners;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import com.crunchiest.CrunchiestNodes;
import com.crunchiest.database.DatabaseManager;
import com.crunchiest.util.ItemStackSerializer;

import net.md_5.bungee.api.ChatColor;

public class NodeEventListener implements Listener {
    private final CrunchiestNodes plugin;
    private final DatabaseManager dbManager;
    private final Map<UUID, Long> lastInteractionTime = new HashMap<>();
    private static final long INTERACTION_COOLDOWN = 1000; // 1 second cooldown
    private final ConcurrentMap<Integer, BukkitTask> nodeTasks = new ConcurrentHashMap<>();

    public NodeEventListener(CrunchiestNodes plugin, DatabaseManager dbManager) {
        this.plugin = plugin;
        this.dbManager = dbManager;
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) throws SQLException {
        try {
            Player player = event.getPlayer();
            Block block = event.getBlock();
            int nodeId = dbManager.getNodeId(block.getWorld().getName(), block.getX(), block.getY(), block.getZ()).get();
            if (nodeId != -1) {
                if (isRateLimited(player)) {
                    event.setCancelled(true);
                    return;
                }
                handleNodeMining(player, block, event);
            }
        } catch (InterruptedException | ExecutionException ex) {
            ex.printStackTrace();
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) throws SQLException {
        Player player = event.getPlayer();
        UUID playerUUID = player.getUniqueId();
        Block block = event.getClickedBlock();

        if (block != null) {
            try {
                int nodeId = dbManager.getNodeId(block.getWorld().getName(), block.getX(), block.getY(), block.getZ()).get();
                if (nodeId != -1 && isRateLimited(player)) {
                    event.setCancelled(true);
                    return;
                }
                
                if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                    if (plugin.getNodeBuilderCommand().isNodeBuilder(playerUUID)) {
                        handleNodeRegistration(player, block, event);
                    } else {
                        handleNodeMining(player, block, event);
                    }
                } else if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
                    if (plugin.getNodeBuilderCommand().isNodeDeleter(playerUUID)) {
                        handleNodeDeletion(player, block, event);
                    }
                }
            } catch (InterruptedException | ExecutionException ex) {
                ex.printStackTrace();
            }
        }
    }

    /**
     * Checks if the player is rate-limited.
     *
     * @param player The player to check.
     * @return True if the player is rate-limited, false otherwise.
     */
    private boolean isRateLimited(Player player) {
        UUID playerUUID = player.getUniqueId();
        long currentTime = System.currentTimeMillis();
        if (lastInteractionTime.containsKey(playerUUID)) {
            long lastTime = lastInteractionTime.get(playerUUID);
            if (currentTime - lastTime < INTERACTION_COOLDOWN) {
                return true;
            }
        }
        lastInteractionTime.put(playerUUID, currentTime);
        return false;
    }

    /**
     * Handles the registration of a node.
     *
     * @param player The player registering the node.
     * @param block The block to register as a node.
     * @param event The cancellable event.
     */
    private void handleNodeRegistration(Player player, Block block, Cancellable event) throws InterruptedException, ExecutionException {
        try {
            // Check if the block is already a registered node
            int existingNodeId = dbManager.getNodeId(block.getWorld().getName(), block.getX(), block.getY(), block.getZ()).get();
            if (existingNodeId != -1) {
                player.sendMessage(ChatColor.RED + "This block is already registered as a node.");
                event.setCancelled(true);
                return;
            }

            long cooldown = player.getMetadata("nodebuilder_cooldown").get(0).asLong();
            boolean isGlobal = player.getMetadata("nodebuilder_isGlobal").get(0).asBoolean();
            boolean requireBetterTool = player.getMetadata("nodebuilder_requireBetterTool").get(0).asBoolean();

            String itemStackSerialized = ItemStackSerializer.serialize(player.getInventory().getItemInOffHand());
            String toolSerialized = ItemStackSerializer.serialize(player.getInventory().getItemInMainHand());
            String originalBlockType = block.getType().name();

            dbManager.registerNode(block.getWorld().getName(), block.getX(), block.getY(), block.getZ(), itemStackSerialized, toolSerialized, cooldown, isGlobal, requireBetterTool, originalBlockType);
            Bukkit.getScheduler().runTask(plugin, () -> {
              for (int i = 0; i < 5; i++) {
                  Bukkit.getScheduler().runTaskLater(plugin, () -> {
                      player.sendBlockChange(block.getLocation(), Material.GREEN_CONCRETE.createBlockData());
                      player.playSound(player.getLocation(), Sound.ENTITY_CHICKEN_EGG, 1.0f, 1.0f);
                  }, i * 10L); // 10 ticks delay between flashes

                  Bukkit.getScheduler().runTaskLater(plugin, () -> {
                      player.sendBlockChange(block.getLocation(), block.getBlockData());
                  }, i * 10L + 5L); // 5 ticks delay to revert back to original block
              }
            });

            plugin.getNodeBuilderCommand().removeNodeBuilder(player.getUniqueId());
            player.sendMessage(ChatColor.GREEN + "Node registration mode disabled.");
        } catch (IOException | IllegalArgumentException e) {
            player.sendMessage(ChatColor.RED + "Failed to register node: " + e.getMessage());
        }
    }

    /**
     * Handles the mining of a node.
     *
     * @param player The player mining the node.
     * @param block The block being mined.
     * @param event The cancellable event.
     */
    private void handleNodeMining(Player player, Block block, Cancellable event) {
        try {
            // Check if the block is a registered node
            int nodeId = dbManager.getNodeId(block.getWorld().getName(), block.getX(), block.getY(), block.getZ()).get();
            if (nodeId != -1) {
                // Check if the node is on cooldown
                boolean isGlobal = dbManager.isNodeGlobal(nodeId).get();
                boolean isOnCooldown = isGlobal ? dbManager.isNodeOnGlobalCooldown(nodeId).get() : dbManager.isNodeOnCooldown(nodeId, player.getUniqueId()).get();
                if (isOnCooldown) {
                    player.sendMessage(ChatColor.RED + "This node is on cooldown.");
                    event.setCancelled(true);
                    // Resend the block change packet two ticks later to ensure it stays as bedrock
                    Bukkit.getScheduler().runTaskLater(plugin, () -> player.sendBlockChange(block.getLocation(), Material.BEDROCK.createBlockData()), 2L);
                    return;
                }

                // Get the node's item stack and tool
                String itemStackBase64;
                itemStackBase64 = dbManager.getNodeItemStack(nodeId).get();
                String toolBase64 = dbManager.getNodeTool(nodeId).get();
                ItemStack itemStack = ItemStackSerializer.deserialize(itemStackBase64);
                ItemStack tool = ItemStackSerializer.deserialize(toolBase64);

                // Check if the player is using the correct tool
                boolean requireBetterTool = dbManager.isNodeRequireBetterTool(nodeId).get();
                
                if (!isToolValid(player.getInventory().getItemInMainHand(), tool, requireBetterTool)) {
                    player.sendMessage(ChatColor.RED + "You need to use the correct tool to mine this node.");
                    event.setCancelled(true);
                    return;
                }
                

                // Drop the item stack
                block.getWorld().dropItemNaturally(block.getLocation(), itemStack);

                // Play a sound to indicate successful mining
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);

                // Set the block to bedrock to indicate cooldown
                if (isGlobal) {
                    block.setType(Material.BEDROCK);
                } else {
                    Bukkit.getScheduler().runTaskLater(plugin, () -> player.sendBlockChange(block.getLocation(), Material.BEDROCK.createBlockData()), 2L);
                }

                // Set the cooldown
                long cooldownEnd = System.currentTimeMillis() + dbManager.getNodeCooldown(nodeId).get();
                if (isGlobal) {
                    dbManager.setGlobalCooldown(nodeId, cooldownEnd);
                } else {
                    dbManager.setCooldown(nodeId, player.getUniqueId(), cooldownEnd);
                }

                // Schedule a task to reset the block after the cooldown
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (isGlobal) {
                        try {
                            block.setType(Material.valueOf(dbManager.getNodeOriginalBlockType(nodeId).get()));
                        } catch (InterruptedException | ExecutionException ex) {
                            ex.printStackTrace();
                        }
                    } else {
                        player.sendBlockChange(block.getLocation(), block.getBlockData());
                    }
                }, dbManager.getNodeCooldown(nodeId).get() / 50);
            }
        } catch (ExecutionException | InterruptedException | IOException | ClassNotFoundException e) {
            player.sendMessage(ChatColor.RED + "An error occurred while processing the node.");
            e.printStackTrace();
        }
    }

    /**
     * Handles the deletion of a node.
     *
     * @param player The player deleting the node.
     * @param block The block to delete.
     * @param event The cancellable event.
     */
    private void handleNodeDeletion(Player player, Block block, Cancellable event) {
        try {
            // Check if the block is a registered node
            int nodeId = dbManager.getNodeId(block.getWorld().getName(), block.getX(), block.getY(), block.getZ()).get();
            if (nodeId != -1) {
                // Cancel any tasks associated with the node
                cancelNodeTasks(nodeId);
                
                // Set the block back to its original block type
                String originalBlockType = dbManager.getNodeOriginalBlockType(nodeId).get();
                if (originalBlockType != null) {
                    block.setType(Material.valueOf(originalBlockType));
                } else {
                    player.sendMessage(ChatColor.RED + "Original block type is null, cannot delete node.");
                }

                Bukkit.getScheduler().runTask(plugin, () -> {
                  for (int i = 0; i < 5; i++) {
                      Bukkit.getScheduler().runTaskLater(plugin, () -> {
                          player.sendBlockChange(block.getLocation(), Material.RED_CONCRETE.createBlockData());
                          player.playSound(player.getLocation(), Sound.ENTITY_CHICKEN_EGG, 1.0f, 1.0f);
                      }, i * 10L); // 10 ticks delay between flashes
    
                      Bukkit.getScheduler().runTaskLater(plugin, () -> {
                          player.sendBlockChange(block.getLocation(), block.getBlockData());
                      }, i * 10L + 5L); // 5 ticks delay to revert back to original block
                  }
                });

                // Delete the node from the database
                dbManager.deleteNode(nodeId);
                
                player.sendMessage(ChatColor.GREEN + "Node deleted successfully.");
                event.setCancelled(true);
            }
        } catch (InterruptedException | ExecutionException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Cancels any tasks associated with the node.
     *
     * @param nodeId The ID of the node.
     */
    private void cancelNodeTasks(int nodeId) {
        BukkitTask task = nodeTasks.remove(nodeId);
        if (task != null) {
            task.cancel();
        }
    }

    /**
     * Checks if the player's tool is valid for mining the node.
     *
     * @param playerTool The player's tool.
     * @param requiredTool The required tool for the node.
     * @param requireBetterTool Whether a better tool is required.
     * @return True if the tool is valid, false otherwise.
     */
    private boolean isToolValid(ItemStack playerTool, ItemStack requiredTool, boolean requireBetterTool) {
        if (requireBetterTool) {
            // Extract the tool type from the tool ID
            String playerToolType = playerTool.getType().name().split("_")[1];
            String requiredToolType = requiredTool.getType().name().split("_")[1];

            // Check if the tools are of the same type (e.g., both are pickaxes)
            if (!playerToolType.equals(requiredToolType)) {
                return false;
            }

            // Use durability to rank the tools
            int playerToolDurability = playerTool.getType().getMaxDurability();
            int requiredToolDurability = requiredTool.getType().getMaxDurability();

            return playerToolDurability >= requiredToolDurability;
        } else {
            // Check if the player's tool is the same as the required tool
            return playerTool.isSimilar(requiredTool);
        }
    }
}