package com.lipinle.ironchests.test;

import com.lipinle.ironchests.ChestTier;
import com.lipinle.ironchests.ChestUpgrade;
import com.lipinle.ironchests.IronChests;
import com.lipinle.ironchests.block.MetalChestBlockEntity;
import com.lipinle.ironchests.registry.ModBlocks;
import com.lipinle.ironchests.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/**
 * In-world tests, run with {@code ./gradlew runGameTestServer}.
 */
@GameTestHolder(IronChests.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ChestGameTests {
    private static final BlockPos POS = new BlockPos(1, 1, 1);

    private ChestGameTests() {
    }

    @GameTest(template = "empty")
    public static void everyTierHasTheRightSize(GameTestHelper helper) {
        for (ChestTier tier : ChestTier.values()) {
            helper.setBlock(POS, ModBlocks.get(tier));
            MetalChestBlockEntity chest = (MetalChestBlockEntity) helper.getBlockEntity(POS);
            helper.assertTrue(chest.getTier() == tier, "Wrong tier for " + tier);
            helper.assertTrue(chest.getContainerSize() == tier.getSize(), "Wrong size for " + tier);
            helper.setBlock(POS, Blocks.AIR);
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void upgradesKeepContentsAndName(GameTestHelper helper) {
        helper.setBlock(POS, Blocks.CHEST);
        ChestBlockEntity wood = (ChestBlockEntity) helper.getBlockEntity(POS);
        wood.setItem(0, new ItemStack(Items.DIAMOND, 5));
        wood.setItem(26, new ItemStack(Items.APPLE, 3));
        wood.setCustomName(Component.literal("Loot"));

        upgrade(helper, ChestUpgrade.WOOD_TO_IRON);
        helper.assertBlockPresent(ModBlocks.get(ChestTier.IRON), POS);
        upgrade(helper, ChestUpgrade.IRON_TO_GOLD);
        helper.assertBlockPresent(ModBlocks.get(ChestTier.GOLD), POS);

        MetalChestBlockEntity chest = (MetalChestBlockEntity) helper.getBlockEntity(POS);
        helper.assertTrue(chest.getItem(0).is(Items.DIAMOND) && chest.getItem(0).getCount() == 5, "Diamonds lost");
        helper.assertTrue(chest.getItem(26).is(Items.APPLE) && chest.getItem(26).getCount() == 3, "Apples lost");
        helper.assertTrue(chest.hasCustomName() && "Loot".equals(chest.getName().getString()), "Name lost");
        helper.assertItemEntityNotPresent(Items.DIAMOND, POS, 3.0D);
        helper.assertItemEntityNotPresent(Items.APPLE, POS, 3.0D);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void wrongUpgradeDoesNothing(GameTestHelper helper) {
        helper.setBlock(POS, ModBlocks.get(ChestTier.COPPER));
        upgrade(helper, ChestUpgrade.IRON_TO_GOLD);
        helper.assertBlockPresent(ModBlocks.get(ChestTier.COPPER), POS);
        helper.succeed();
    }

    private static void upgrade(GameTestHelper helper, ChestUpgrade upgrade) {
        Player player = helper.makeMockPlayer();
        player.setShiftKeyDown(true);
        ItemStack kit = new ItemStack(ModItems.UPGRADES.get(upgrade).get());
        player.setItemInHand(InteractionHand.MAIN_HAND, kit);
        BlockPos absolute = helper.absolutePos(POS);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(absolute), Direction.UP, absolute, false);
        kit.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit));
    }
}
