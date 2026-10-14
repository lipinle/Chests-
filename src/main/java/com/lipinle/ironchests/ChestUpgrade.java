package com.lipinle.ironchests;

import org.jetbrains.annotations.Nullable;

/**
 * Upgrade kits. A {@code null} source means a vanilla wooden chest.
 */
public enum ChestUpgrade {
    WOOD_TO_COPPER(null, ChestTier.COPPER),
    WOOD_TO_IRON(null, ChestTier.IRON),
    COPPER_TO_IRON(ChestTier.COPPER, ChestTier.IRON),
    IRON_TO_GOLD(ChestTier.IRON, ChestTier.GOLD),
    GOLD_TO_DIAMOND(ChestTier.GOLD, ChestTier.DIAMOND),
    DIAMOND_TO_CRYSTAL(ChestTier.DIAMOND, ChestTier.CRYSTAL),
    DIAMOND_TO_OBSIDIAN(ChestTier.DIAMOND, ChestTier.OBSIDIAN);

    @Nullable
    private final ChestTier from;
    private final ChestTier to;

    ChestUpgrade(@Nullable ChestTier from, ChestTier to) {
        this.from = from;
        this.to = to;
    }

    @Nullable
    public ChestTier getFrom() {
        return from;
    }

    public ChestTier getTo() {
        return to;
    }

    public String getItemName() {
        return (from == null ? "wood" : from.getName()) + "_to_" + to.getName() + "_chest_upgrade";
    }
}
