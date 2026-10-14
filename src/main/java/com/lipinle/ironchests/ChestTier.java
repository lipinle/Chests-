package com.lipinle.ironchests;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;

/**
 * Every kind of chest this mod adds, with its inventory size and block properties.
 * GUI layout numbers are derived from the column/row count so any size works.
 */
public enum ChestTier {
    COPPER("copper", 9, 5, 3.0F, 6.0F, MapColor.COLOR_ORANGE, SoundType.COPPER),
    IRON("iron", 9, 6, 5.0F, 6.0F, MapColor.METAL, SoundType.METAL),
    GOLD("gold", 9, 9, 3.0F, 6.0F, MapColor.GOLD, SoundType.METAL),
    DIAMOND("diamond", 12, 9, 5.0F, 6.0F, MapColor.DIAMOND, SoundType.METAL),
    CRYSTAL("crystal", 12, 9, 3.0F, 6.0F, MapColor.NONE, SoundType.GLASS),
    OBSIDIAN("obsidian", 12, 9, 10.0F, 2000.0F, MapColor.COLOR_BLACK, SoundType.STONE),
    DIRT("dirt", 1, 1, 0.5F, 0.5F, MapColor.DIRT, SoundType.GRAVEL);

    private static final int SLOT = 18;
    private static final int MIN_WIDTH = 176;
    private static final int PLAYER_INV_WIDTH = 9 * SLOT;

    private final String name;
    private final int columns;
    private final int rows;
    private final float hardness;
    private final float resistance;
    private final MapColor mapColor;
    private final SoundType soundType;
    private final ResourceLocation texture;

    ChestTier(String name, int columns, int rows, float hardness, float resistance, MapColor mapColor, SoundType soundType) {
        this.name = name;
        this.columns = columns;
        this.rows = rows;
        this.hardness = hardness;
        this.resistance = resistance;
        this.mapColor = mapColor;
        this.soundType = soundType;
        this.texture = IronChests.id("textures/entity/chest/" + name + ".png");
    }

    public static ChestTier byId(int id) {
        ChestTier[] values = values();
        return id >= 0 && id < values.length ? values[id] : IRON;
    }

    public String getName() {
        return name;
    }

    public String getBlockName() {
        return name + "_chest";
    }

    public int getColumns() {
        return columns;
    }

    public int getRows() {
        return rows;
    }

    public int getSize() {
        return columns * rows;
    }

    public float getHardness() {
        return hardness;
    }

    public float getResistance() {
        return resistance;
    }

    public MapColor getMapColor() {
        return mapColor;
    }

    public SoundType getSoundType() {
        return soundType;
    }

    public ResourceLocation getTexture() {
        return texture;
    }

    /** Crystal chests show their contents through the glass. */
    public boolean showsContents() {
        return this == CRYSTAL;
    }

    public boolean isTransparent() {
        return this == CRYSTAL;
    }

    // ---- GUI layout ----

    public int getGuiWidth() {
        return Math.max(MIN_WIDTH, columns * SLOT + 14);
    }

    public int getGuiHeight() {
        return 114 + rows * SLOT;
    }

    /** X of the first chest slot (item position, i.e. inside the slot frame). */
    public int getChestSlotsX() {
        return (getGuiWidth() - columns * SLOT) / 2 + 1;
    }

    public int getChestSlotsY() {
        return 18;
    }

    public int getPlayerInvX() {
        return (getGuiWidth() - PLAYER_INV_WIDTH) / 2 + 1;
    }

    public int getPlayerInvY() {
        return 31 + rows * SLOT;
    }
}
