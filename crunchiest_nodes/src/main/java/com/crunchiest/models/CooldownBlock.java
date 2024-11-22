package com.crunchiest.models;

public class CooldownBlock {
    private final int nodeId;
    private final String world;
    private final int x, y, z;
    private final long cooldownEnd;
    private final boolean isGlobal;

    public CooldownBlock(int nodeId, String world, int x, int y, int z, long cooldownEnd, boolean isGlobal) {
        this.nodeId = nodeId;
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
        this.cooldownEnd = cooldownEnd;
        this.isGlobal = isGlobal;
    }

    public int getNodeId() {
        return nodeId;
    }

    public String getWorld() {
        return world;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getZ() {
        return z;
    }

    public long getCooldownEnd() {
        return cooldownEnd;
    }

    public boolean isGlobal() {
        return isGlobal;
    }
}