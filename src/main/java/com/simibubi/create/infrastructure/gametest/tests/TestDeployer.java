package com.simibubi.create.infrastructure.gametest.tests;

import java.util.function.Consumer;

import com.simibubi.create.infrastructure.gametest.CreateGameTestHelper;
import com.simibubi.create.infrastructure.gametest.GameTestGroup;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.TurtleEggBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;

// Separate batch to avoid breaking other tests
@GameTestGroup(path = "deployer", batch = "deployer")
public class TestDeployer {

  // Block positions for deployer_general.nbt
  // The output filter for the deployer disallows all pickaxes and an iron shovel
  // All other tools will go through to the output and cause a test to fail
  private static final BlockPos LEVER = new BlockPos(2, 2, 2);
  private static final BlockPos TARGET = new BlockPos(2, 3, 1);
  private static final BlockPos INPUT_CHEST = new BlockPos(2, 5, 3);
  private static final BlockPos OUTPUT_DEPOT = new BlockPos(1, 2, 3);

  private static void runDeployerTest(CreateGameTestHelper helper, ItemStack heldItem, BlockState targetBlock,
      Consumer<CreateGameTestHelper> additionalSetup, Consumer<CreateGameTestHelper> assertion) {
    ItemHandlerHelper.insertItem(helper.itemStorageAt(INPUT_CHEST), heldItem, false);
    helper.setBlock(TARGET, targetBlock);
    if (additionalSetup != null) {
      additionalSetup.accept(helper);
    }

    // Activate deployer
    helper.pullLever(LEVER);

    helper.succeedWhen(() -> assertion.accept(helper));
  }

  @GameTest(template = "deployer_general", timeoutTicks = CreateGameTestHelper.TEN_SECONDS)
  public static void dirtBaseCase(CreateGameTestHelper helper) {
    runDeployerTest(helper,
        new ItemStack(Items.IRON_PICKAXE, 1),
        Blocks.DIRT.defaultBlockState(),
        null,
        h -> {
          h.assertContainerContains(OUTPUT_DEPOT, new ItemStack(Items.DIRT, 1));
          h.assertBlockPresent(Blocks.AIR, TARGET);
        });
  }

  @GameTest(template = "deployer_general", timeoutTicks = CreateGameTestHelper.TEN_SECONDS)
  public static void glassSilkTouch(CreateGameTestHelper helper) {
    ItemStack silkTouchPickaxe = new ItemStack(Items.IRON_PICKAXE, 1);
    silkTouchPickaxe.enchant(Enchantments.SILK_TOUCH, 1);

    runDeployerTest(helper,
        silkTouchPickaxe,
        Blocks.GLASS.defaultBlockState(),
        null,
        h -> {
          h.assertContainerContains(OUTPUT_DEPOT, new ItemStack(Items.GLASS, 1));
          h.assertBlockPresent(Blocks.AIR, TARGET);
        });
  }

  @GameTest(template = "deployer_general", timeoutTicks = CreateGameTestHelper.TEN_SECONDS)
  public static void gravelFortune(CreateGameTestHelper helper) {
    // Fortune 3 tools drop flint 100% of the time
    ItemStack fortunePickaxe = new ItemStack(Items.IRON_PICKAXE, 1);
    fortunePickaxe.enchant(Enchantments.BLOCK_FORTUNE, 3);

    runDeployerTest(helper,
        fortunePickaxe,
        Blocks.GRAVEL.defaultBlockState(),
        null,
        h -> {
          h.assertBlockPresent(Blocks.AIR, TARGET);
          h.assertContainerContains(OUTPUT_DEPOT, new ItemStack(Items.FLINT, 1));
        });
  }

  @GameTest(template = "deployer_general", timeoutTicks = CreateGameTestHelper.TEN_SECONDS)
  public static void turtleEgg(CreateGameTestHelper helper) {
    runDeployerTest(helper,
        new ItemStack(Items.IRON_PICKAXE, 1),
        Blocks.TURTLE_EGG.defaultBlockState().setValue(TurtleEggBlock.EGGS, 4),
        null,
        h -> {
          // No eggs should drop without silk touch
          h.assertContainerEmpty(OUTPUT_DEPOT);
          h.assertBlockProperty(TARGET, TurtleEggBlock.EGGS, 3);
        });
  }

  @GameTest(template = "deployer_general", timeoutTicks = CreateGameTestHelper.TEN_SECONDS)
  public static void turtleEggSilkTouch(CreateGameTestHelper helper) {
    ItemStack silkTouchPickaxe = new ItemStack(Items.IRON_PICKAXE, 1);
    silkTouchPickaxe.enchant(Enchantments.SILK_TOUCH, 1);

    runDeployerTest(helper,
        silkTouchPickaxe,
        Blocks.TURTLE_EGG.defaultBlockState().setValue(TurtleEggBlock.EGGS, 4),
        null,
        h -> {
          // Should drop only 1 turtle egg at a time with silk touch
          h.assertContainerContains(OUTPUT_DEPOT, new ItemStack(Items.TURTLE_EGG, 1));
          h.assertBlockProperty(TARGET, TurtleEggBlock.EGGS, 3);
        });
  }

  @GameTest(template = "deployer_general", timeoutTicks = CreateGameTestHelper.TEN_SECONDS)
  public static void seaPickle(CreateGameTestHelper helper) {
    runDeployerTest(helper,
        new ItemStack(Items.STONE_PICKAXE, 1),
        Blocks.SEA_PICKLE.defaultBlockState()
            .setValue(net.minecraft.world.level.block.SeaPickleBlock.PICKLES, 4)
            .setValue(net.minecraft.world.level.block.SeaPickleBlock.WATERLOGGED, false),
        null,
        h -> {
          // Should drop 4 sea pickles at a time
          h.assertContainerContains(OUTPUT_DEPOT, new ItemStack(Items.SEA_PICKLE, 4));
          h.assertBlockPresent(Blocks.AIR, TARGET);
        });
  }

  @GameTest(template = "deployer_general", timeoutTicks = CreateGameTestHelper.TEN_SECONDS)
  public static void bedrockUnbreakable(CreateGameTestHelper helper) {
    runDeployerTest(helper,
        new ItemStack(Items.NETHERITE_PICKAXE, 1),
        Blocks.BEDROCK.defaultBlockState(),
        null,
        h -> {
          h.assertSecondsPassed(5);
          h.assertBlockPresent(Blocks.BEDROCK, TARGET);
        });
  }

  @GameTest(template = "deployer_general", timeoutTicks = CreateGameTestHelper.TWENTY_SECONDS)
  public static void diamondStonePickaxe(CreateGameTestHelper helper) {
    runDeployerTest(helper,
        // Stone pick cannot harvest diamond block
        new ItemStack(Items.STONE_PICKAXE, 1),
        Blocks.DIAMOND_BLOCK.defaultBlockState(),
        null,
        h -> {
          h.assertBlockPresent(Blocks.AIR, TARGET);
          h.assertContainerEmpty(OUTPUT_DEPOT);
        });
  }

  @GameTest(template = "deployer_general", timeoutTicks = CreateGameTestHelper.TEN_SECONDS)
  public static void tallFlower(CreateGameTestHelper helper) {
    runDeployerTest(helper,
        // Any item can break flowers
        new ItemStack(Items.STONE_PICKAXE, 1),
        Blocks.ROSE_BUSH.defaultBlockState().setValue(net.minecraft.world.level.block.DoublePlantBlock.HALF,
            DoubleBlockHalf.LOWER),
        h -> {
          // Place grass block below so flower is placeable
          h.setBlock(TARGET.below(), Blocks.GRASS_BLOCK.defaultBlockState());
          // Place the upper half of the flower
          h.setBlock(TARGET.above(), Blocks.ROSE_BUSH.defaultBlockState()
              .setValue(net.minecraft.world.level.block.DoublePlantBlock.HALF, DoubleBlockHalf.UPPER));
        },
        h -> {
          // Should drop 1 rose bush
          h.assertContainerContains(OUTPUT_DEPOT, new ItemStack(Items.ROSE_BUSH, 1));
          // Both halves of 2 block tall flower should be gone
          h.assertBlockPresent(Blocks.AIR, TARGET);
          h.assertBlockPresent(Blocks.AIR, TARGET.above());
        });
  }

  @GameTest(template = "deployer_general", timeoutTicks = CreateGameTestHelper.TEN_SECONDS)
  public static void shulkerBoxWithItem(CreateGameTestHelper helper) {
    runDeployerTest(helper,
        new ItemStack(Items.IRON_PICKAXE, 1),
        Blocks.SHULKER_BOX.defaultBlockState(),
        h -> {
          // Put 1 diamond in the shulker box
          ItemStack diamond = new ItemStack(Items.DIAMOND, 1);
          ItemHandlerHelper.insertItem(h.itemStorageAt(TARGET), diamond, false);
        },
        h -> {
          h.assertBlockPresent(Blocks.AIR, TARGET);

          // Verify shulker box was dropped and contains 1 diamond
          IItemHandler storage = h.itemStorageAt(OUTPUT_DEPOT);
          boolean foundShulkerWithDiamond = false;
          for (int slot = 0; slot < storage.getSlots(); slot++) {
            ItemStack stack = storage.getStackInSlot(slot);
            if (stack.getItem() == Items.SHULKER_BOX) {
              net.minecraft.nbt.CompoundTag tag = stack.getTag();
              if (tag != null && tag.contains("BlockEntityTag")) {
                net.minecraft.nbt.CompoundTag blockEntityTag = tag.getCompound("BlockEntityTag");
                if (blockEntityTag.contains("Items", 9)) {
                  net.minecraft.nbt.ListTag items = blockEntityTag.getList("Items", 10);
                  for (int i = 0; i < items.size(); i++) {
                    net.minecraft.nbt.CompoundTag itemTag = items.getCompound(i);
                    if (itemTag.getString("id").equals("minecraft:diamond")) {
                      foundShulkerWithDiamond = true;
                      break;
                    }
                  }
                }
              }
            }
          }
          if (!foundShulkerWithDiamond) {
            h.fail("Shulker box with diamond not found in depot");
          }
        });
  }

  @GameTest(template = "deployer_general", timeoutTicks = CreateGameTestHelper.TEN_SECONDS)
  public static void iceToWater(CreateGameTestHelper helper) {
    runDeployerTest(helper,
        new ItemStack(Items.IRON_PICKAXE, 1),
        Blocks.ICE.defaultBlockState(),
        null,
        h -> {
          // Ice should turn to water when broken since there is a block below it
          h.assertBlockPresent(Blocks.WATER, TARGET);
          h.assertContainerEmpty(OUTPUT_DEPOT);
        });
  }

  @GameTest(template = "deployer_general", timeoutTicks = CreateGameTestHelper.TEN_SECONDS)
  public static void iceToNothing(CreateGameTestHelper helper) {
    runDeployerTest(helper,
        new ItemStack(Items.IRON_PICKAXE, 1),
        Blocks.ICE.defaultBlockState(),
        h -> {
          // Remove the block below so water cannot form
          h.setBlock(TARGET.below(), Blocks.AIR.defaultBlockState());
        },
        h -> {
          // Ice should disappear without turning to water when no block below
          h.assertBlockPresent(Blocks.AIR, TARGET);
          h.assertContainerEmpty(OUTPUT_DEPOT);
        });
  }

  @GameTest(template = "deployer_general", timeoutTicks = CreateGameTestHelper.TEN_SECONDS)
  public static void iceSilkTouch(CreateGameTestHelper helper) {
    ItemStack silkTouchPickaxe = new ItemStack(Items.IRON_PICKAXE, 1);
    silkTouchPickaxe.enchant(Enchantments.SILK_TOUCH, 1);

    runDeployerTest(helper,
        silkTouchPickaxe,
        Blocks.ICE.defaultBlockState(),
        null,
        h -> {
          h.assertContainerContains(OUTPUT_DEPOT, new ItemStack(Items.ICE, 1));
          h.assertBlockPresent(Blocks.AIR, TARGET);
        });
  }

  @GameTest(template = "deployer_general", timeoutTicks = CreateGameTestHelper.TEN_SECONDS)
  public static void infestedStone(CreateGameTestHelper helper) {
    runDeployerTest(helper,
        new ItemStack(Items.IRON_PICKAXE, 1),
        Blocks.INFESTED_STONE.defaultBlockState(),
        null,
        h -> {
          h.assertEntityPresent(net.minecraft.world.entity.EntityType.SILVERFISH);
          h.assertBlockPresent(Blocks.AIR, TARGET);
          h.assertContainerEmpty(OUTPUT_DEPOT);
        });
  }

  @GameTest(template = "deployer_general", timeoutTicks = CreateGameTestHelper.TEN_SECONDS)
  public static void infestedStoneSilkTouch(CreateGameTestHelper helper) {
    ItemStack silkTouchPickaxe = new ItemStack(Items.IRON_PICKAXE, 1);
    silkTouchPickaxe.enchant(Enchantments.SILK_TOUCH, 1);

    runDeployerTest(helper,
        silkTouchPickaxe,
        Blocks.INFESTED_STONE.defaultBlockState(),
        null,
        h -> {
          h.assertContainerContains(OUTPUT_DEPOT, new ItemStack(Items.STONE, 1));
          h.assertBlockPresent(Blocks.AIR, TARGET);
          h.assertEntityNotPresent(net.minecraft.world.entity.EntityType.SILVERFISH);
        });
  }

  @GameTest(template = "deployer_general", timeoutTicks = CreateGameTestHelper.TEN_SECONDS)
  public static void suspiciousSand(CreateGameTestHelper helper) {
    runDeployerTest(helper,
        new ItemStack(Items.IRON_PICKAXE, 1),
        Blocks.SUSPICIOUS_SAND.defaultBlockState(),
        null,
        h -> {
          h.assertBlockPresent(Blocks.AIR, TARGET);
          h.assertContainerEmpty(OUTPUT_DEPOT);
        });
  }

  @GameTest(template = "deployer_general", timeoutTicks = CreateGameTestHelper.TEN_SECONDS)
  public static void suspiciousSandSilkTouch(CreateGameTestHelper helper) {
    ItemStack silkTouchPickaxe = new ItemStack(Items.IRON_PICKAXE, 1);
    silkTouchPickaxe.enchant(Enchantments.SILK_TOUCH, 1);

    runDeployerTest(helper,
        silkTouchPickaxe,
        Blocks.SUSPICIOUS_SAND.defaultBlockState(),
        null,
        h -> {
          h.assertBlockPresent(Blocks.AIR, TARGET);
          h.assertContainerEmpty(OUTPUT_DEPOT);
        });
  }

  @GameTest(template = "deployer_general", timeoutTicks = CreateGameTestHelper.TEN_SECONDS)
  public static void snowLayers(CreateGameTestHelper helper) {
    runDeployerTest(helper,
        new ItemStack(Items.IRON_SHOVEL, 1),
        Blocks.SNOW.defaultBlockState().setValue(net.minecraft.world.level.block.SnowLayerBlock.LAYERS, 4),
        null,
        h -> {
          h.assertSecondsPassed(2);
          // 1 snowball per layer
          h.assertContainerContains(OUTPUT_DEPOT, new ItemStack(Items.SNOWBALL, 4));
          h.assertBlockPresent(Blocks.AIR, TARGET);
        });
  }

  @GameTest(template = "deployer_general", timeoutTicks = CreateGameTestHelper.TEN_SECONDS)
  public static void waterloggedStairs(CreateGameTestHelper helper) {
    runDeployerTest(helper,
        new ItemStack(Items.IRON_PICKAXE, 1),
        Blocks.OAK_STAIRS.defaultBlockState().setValue(net.minecraft.world.level.block.StairBlock.WATERLOGGED, true),
        null,
        h -> {
          h.assertContainerContains(OUTPUT_DEPOT, new ItemStack(Items.OAK_STAIRS, 1));
          h.assertBlockPresent(Blocks.WATER, TARGET);
        });
  }
}
