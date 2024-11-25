package com.crunchiest.database;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import com.crunchiest.models.CooldownBlock;

public class DatabaseManager {
    private Connection connection;

    public CompletableFuture<Void> connect() {
        return CompletableFuture.runAsync(() -> {
            try {
                // Ensure the directory exists
                File dbDir = new File("plugins/crunchiest_nodes");
                if (!dbDir.exists()) {
                    dbDir.mkdirs();
                }

                connection = DriverManager.getConnection("jdbc:sqlite:plugins/crunchiest_nodes/nodes.db");
                createTables();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public CompletableFuture<Void> disconnect() {
        return CompletableFuture.runAsync(() -> {
            try {
                if (connection != null && !connection.isClosed()) {
                    connection.close();
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private void createTables() throws SQLException {
        String createNodesTable = "CREATE TABLE IF NOT EXISTS nodes (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "world TEXT NOT NULL," +
                "x INTEGER NOT NULL," +
                "y INTEGER NOT NULL," +
                "z INTEGER NOT NULL," +
                "item_stack TEXT NOT NULL," +
                "tool TEXT NOT NULL," +
                "cooldown INTEGER NOT NULL," +
                "is_global BOOLEAN NOT NULL," +
                "require_better_tool BOOLEAN NOT NULL," +
                "original_block_type TEXT NOT NULL" +
                ");";

        String createCooldownsTable = "CREATE TABLE IF NOT EXISTS cooldowns (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "node_id INTEGER NOT NULL," +
                "player_uuid TEXT," +
                "cooldown_end INTEGER NOT NULL," +
                "FOREIGN KEY(node_id) REFERENCES nodes(id)" +
                ");";

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createNodesTable);
            stmt.execute(createCooldownsTable);
        }
    }

    public CompletableFuture<Void> registerNode(String world, int x, int y, int z, String itemStack, String tool, long cooldown, boolean isGlobal, boolean requireBetterTool, String originalBlockType) {
        return CompletableFuture.runAsync(() -> {
            String insertNode = "INSERT INTO nodes (world, x, y, z, item_stack, tool, cooldown, is_global, require_better_tool, original_block_type) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement pstmt = connection.prepareStatement(insertNode)) {
                pstmt.setString(1, world);
                pstmt.setInt(2, x);
                pstmt.setInt(3, y);
                pstmt.setInt(4, z);
                pstmt.setString(5, itemStack);
                pstmt.setString(6, tool);
                pstmt.setLong(7, cooldown);
                pstmt.setBoolean(8, isGlobal);
                pstmt.setBoolean(9, requireBetterTool);
                pstmt.setString(10, originalBlockType);
                pstmt.executeUpdate();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public CompletableFuture<Void> setCooldown(int nodeId, UUID playerUUID, long cooldownEnd) {
        return CompletableFuture.runAsync(() -> {
            String insertCooldown = "INSERT INTO cooldowns (node_id, player_uuid, cooldown_end) VALUES (?, ?, ?)";
            try (PreparedStatement pstmt = connection.prepareStatement(insertCooldown)) {
                pstmt.setInt(1, nodeId);
                pstmt.setString(2, playerUUID != null ? playerUUID.toString() : null);
                pstmt.setLong(3, cooldownEnd);
                pstmt.executeUpdate();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public CompletableFuture<Boolean> isNodeOnCooldown(int nodeId, UUID playerUUID) {
        return CompletableFuture.supplyAsync(() -> {
            String query = "SELECT cooldown_end FROM cooldowns WHERE node_id = ? AND (player_uuid IS NULL OR player_uuid = ?) AND cooldown_end > ?";
            try (PreparedStatement pstmt = connection.prepareStatement(query)) {
                pstmt.setInt(1, nodeId);
                pstmt.setString(2, playerUUID != null ? playerUUID.toString() : null);
                pstmt.setLong(3, System.currentTimeMillis());
                try (ResultSet rs = pstmt.executeQuery()) {
                    return rs.next();
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public CompletableFuture<Integer> getNodeId(String world, int x, int y, int z) {
        return CompletableFuture.supplyAsync(() -> {
            String query = "SELECT id FROM nodes WHERE world = ? AND x = ? AND y = ? AND z = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(query)) {
                pstmt.setString(1, world);
                pstmt.setInt(2, x);
                pstmt.setInt(3, y);
                pstmt.setInt(4, z);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getInt("id");
                    }
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
            return -1;
        });
    }

    public CompletableFuture<String> getNodeItemStack(int nodeId) {
        return CompletableFuture.supplyAsync(() -> {
            String query = "SELECT item_stack FROM nodes WHERE id = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(query)) {
                pstmt.setInt(1, nodeId);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getString("item_stack");
                    }
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
            return null;
        });
    }

    public CompletableFuture<String> getNodeTool(int nodeId) {
        return CompletableFuture.supplyAsync(() -> {
            String query = "SELECT tool FROM nodes WHERE id = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(query)) {
                pstmt.setInt(1, nodeId);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getString("tool");
                    }
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
            return null;
        });
    }

    public CompletableFuture<Long> getNodeCooldown(int nodeId) {
        return CompletableFuture.supplyAsync(() -> {
            String query = "SELECT cooldown FROM nodes WHERE id = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(query)) {
                pstmt.setInt(1, nodeId);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getLong("cooldown");
                    }
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
            return 0L;
        });
    }

    public CompletableFuture<Boolean> isNodeGlobal(int nodeId) {
        return CompletableFuture.supplyAsync(() -> {
            String query = "SELECT is_global FROM nodes WHERE id = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(query)) {
                pstmt.setInt(1, nodeId);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getBoolean("is_global");
                    }
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
            return false;
        });
    }

    public CompletableFuture<Boolean> isNodeOnGlobalCooldown(int nodeId) {
        return CompletableFuture.supplyAsync(() -> {
            String query = "SELECT cooldown_end FROM cooldowns WHERE node_id = ? AND player_uuid IS NULL AND cooldown_end > ?";
            try (PreparedStatement pstmt = connection.prepareStatement(query)) {
                pstmt.setInt(1, nodeId);
                pstmt.setLong(2, System.currentTimeMillis());
                try (ResultSet rs = pstmt.executeQuery()) {
                    return rs.next();
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public CompletableFuture<Void> setGlobalCooldown(int nodeId, long cooldownEnd) {
        return CompletableFuture.runAsync(() -> {
            String insertCooldown = "INSERT INTO cooldowns (node_id, player_uuid, cooldown_end) VALUES (?, NULL, ?)";
            try (PreparedStatement pstmt = connection.prepareStatement(insertCooldown)) {
                pstmt.setInt(1, nodeId);
                pstmt.setLong(2, cooldownEnd);
                pstmt.executeUpdate();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public CompletableFuture<List<CooldownBlock>> getBlocksOnCooldown() {
        return CompletableFuture.supplyAsync(() -> {
            String query = "SELECT node_id, world, x, y, z, cooldown_end, is_global FROM nodes JOIN cooldowns ON nodes.id = cooldowns.node_id WHERE cooldown_end > ?";
            List<CooldownBlock> blocks = new ArrayList<>();
            try (PreparedStatement pstmt = connection.prepareStatement(query)) {
                pstmt.setLong(1, System.currentTimeMillis());
                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        blocks.add(new CooldownBlock(
                            rs.getInt("node_id"),
                            rs.getString("world"),
                            rs.getInt("x"),
                            rs.getInt("y"),
                            rs.getInt("z"),
                            rs.getLong("cooldown_end"),
                            rs.getBoolean("is_global")
                        ));
                    }
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
            return blocks;
        });
    }

    public CompletableFuture<String> getNodeOriginalBlockType(int nodeId) {
        return CompletableFuture.supplyAsync(() -> {
            String query = "SELECT original_block_type FROM nodes WHERE id = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(query)) {
                pstmt.setInt(1, nodeId);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getString("original_block_type");
                    }
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
            return null;
        });
    }

    public CompletableFuture<Boolean> isNodeRequireBetterTool(int nodeId) {
        return CompletableFuture.supplyAsync(() -> {
            String query = "SELECT require_better_tool FROM nodes WHERE id = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(query)) {
                pstmt.setInt(1, nodeId);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getBoolean("require_better_tool");
                    }
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
            return false;
        });
    }

    public CompletableFuture<Void> deleteNode(int nodeId) {
        return CompletableFuture.runAsync(() -> {
            String deleteNode = "DELETE FROM nodes WHERE id = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(deleteNode)) {
                pstmt.setInt(1, nodeId);
                pstmt.executeUpdate();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }

            String deleteCooldowns = "DELETE FROM cooldowns WHERE node_id = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(deleteCooldowns)) {
                pstmt.setInt(1, nodeId);
                pstmt.executeUpdate();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }

    // Additional methods to retrieve node information, update cooldowns, etc.
}