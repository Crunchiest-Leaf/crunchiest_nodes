package com.crunchiest.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.crunchiest.models.CooldownBlock;

public class DatabaseManager {
    private Connection connection;

    public void connect() throws SQLException {
        connection = DriverManager.getConnection("jdbc:sqlite:plugins/crunchiest_nodes/nodes.db");
        createTables();
    }

    public void disconnect() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
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
                "is_global BOOLEAN NOT NULL" +
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

    public void registerNode(String world, int x, int y, int z, String itemStack, String tool, long cooldown, boolean isGlobal, boolean requireBetterTool) throws SQLException {
      String insertNode = "INSERT INTO nodes (world, x, y, z, item_stack, tool, cooldown, is_global, require_better_tool) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
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
          pstmt.executeUpdate();
      }
    }

    public void setCooldown(int nodeId, UUID playerUUID, long cooldownEnd) throws SQLException {
        String insertCooldown = "INSERT INTO cooldowns (node_id, player_uuid, cooldown_end) VALUES (?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(insertCooldown)) {
            pstmt.setInt(1, nodeId);
            pstmt.setString(2, playerUUID != null ? playerUUID.toString() : null);
            pstmt.setLong(3, cooldownEnd);
            pstmt.executeUpdate();
        }
    }

    public boolean isNodeOnCooldown(int nodeId, UUID playerUUID) throws SQLException {
        String query = "SELECT cooldown_end FROM cooldowns WHERE node_id = ? AND (player_uuid IS NULL OR player_uuid = ?) AND cooldown_end > ?";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setInt(1, nodeId);
            pstmt.setString(2, playerUUID != null ? playerUUID.toString() : null);
            pstmt.setLong(3, System.currentTimeMillis());
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    public int getNodeId(String world, int x, int y, int z) throws SQLException {
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
      }
      return -1;
  }
  
  public String getNodeItemStack(int nodeId) throws SQLException {
      String query = "SELECT item_stack FROM nodes WHERE id = ?";
      try (PreparedStatement pstmt = connection.prepareStatement(query)) {
          pstmt.setInt(1, nodeId);
          try (ResultSet rs = pstmt.executeQuery()) {
              if (rs.next()) {
                  return rs.getString("item_stack");
              }
          }
      }
      return null;
  }
  
  public String getNodeTool(int nodeId) throws SQLException {
      String query = "SELECT tool FROM nodes WHERE id = ?";
      try (PreparedStatement pstmt = connection.prepareStatement(query)) {
          pstmt.setInt(1, nodeId);
          try (ResultSet rs = pstmt.executeQuery()) {
              if (rs.next()) {
                  return rs.getString("tool");
              }
          }
      }
      return null;
  }
  
  public long getNodeCooldown(int nodeId) throws SQLException {
      String query = "SELECT cooldown FROM nodes WHERE id = ?";
      try (PreparedStatement pstmt = connection.prepareStatement(query)) {
          pstmt.setInt(1, nodeId);
          try (ResultSet rs = pstmt.executeQuery()) {
              if (rs.next()) {
                  return rs.getLong("cooldown");
              }
          }
      }
      return 0;
  }

  public boolean isNodeGlobal(int nodeId) throws SQLException {
    String query = "SELECT is_global FROM nodes WHERE id = ?";
    try (PreparedStatement pstmt = connection.prepareStatement(query)) {
        pstmt.setInt(1, nodeId);
        try (ResultSet rs = pstmt.executeQuery()) {
            if (rs.next()) {
                return rs.getBoolean("is_global");
            }
        }
    }
    return false;
  }

  public boolean isNodeOnGlobalCooldown(int nodeId) throws SQLException {
      String query = "SELECT cooldown_end FROM cooldowns WHERE node_id = ? AND player_uuid IS NULL AND cooldown_end > ?";
      try (PreparedStatement pstmt = connection.prepareStatement(query)) {
          pstmt.setInt(1, nodeId);
          pstmt.setLong(2, System.currentTimeMillis());
          try (ResultSet rs = pstmt.executeQuery()) {
              return rs.next();
          }
      }
  }

  public void setGlobalCooldown(int nodeId, long cooldownEnd) throws SQLException {
      String insertCooldown = "INSERT INTO cooldowns (node_id, player_uuid, cooldown_end) VALUES (?, NULL, ?)";
      try (PreparedStatement pstmt = connection.prepareStatement(insertCooldown)) {
          pstmt.setInt(1, nodeId);
          pstmt.setLong(2, cooldownEnd);
          pstmt.executeUpdate();
      }
  }

  public List<CooldownBlock> getBlocksOnCooldown() throws SQLException {
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
    }
    return blocks;
  }

  public String getNodeOriginalBlockType(int nodeId) throws SQLException {
      String query = "SELECT original_block_type FROM nodes WHERE id = ?";
      try (PreparedStatement pstmt = connection.prepareStatement(query)) {
          pstmt.setInt(1, nodeId);
          try (ResultSet rs = pstmt.executeQuery()) {
              if (rs.next()) {
                  return rs.getString("original_block_type");
              }
          }
      }
      return null;
  }

  public boolean isNodeRequireBetterTool(int nodeId) throws SQLException {
    String query = "SELECT require_better_tool FROM nodes WHERE id = ?";
    try (PreparedStatement pstmt = connection.prepareStatement(query)) {
        pstmt.setInt(1, nodeId);
        try (ResultSet rs = pstmt.executeQuery()) {
            if (rs.next()) {
                return rs.getBoolean("require_better_tool");
            }
        }
    }
    return false;
  }

  public void deleteNode(int nodeId) throws SQLException {
    String deleteNode = "DELETE FROM nodes WHERE id = ?";
    try (PreparedStatement pstmt = connection.prepareStatement(deleteNode)) {
        pstmt.setInt(1, nodeId);
        pstmt.executeUpdate();
    }

    String deleteCooldowns = "DELETE FROM cooldowns WHERE node_id = ?";
    try (PreparedStatement pstmt = connection.prepareStatement(deleteCooldowns)) {
        pstmt.setInt(1, nodeId);
        pstmt.executeUpdate();
    }
  }

    // Additional methods to retrieve node information, update cooldowns, etc.
}