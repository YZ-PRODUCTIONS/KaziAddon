package net.kazi.kazimod.init;

import net.kazi.kazimod.blocks.InfiniteVoidFloorBlock;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;

public class KaziBlocks {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, "kazimod");
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, "kazimod");

    public static final RegistryObject<Block> INFINITE_VOID_FLOOR =
            BLOCKS.register("infinite_void_floor", InfiniteVoidFloorBlock::new);

    /** Bedrock properties with the existing red sand texture. */
    public static final RegistryObject<Block> CUSTOM_COARSE_DIRT =
            BLOCKS.register("custom_coarse_dirt", () -> new Block(Block.Properties.copy(Blocks.BEDROCK)));

    public static final RegistryObject<BlockItem> CUSTOM_COARSE_DIRT_ITEM =
            ITEMS.register("custom_coarse_dirt", () -> new BlockItem(
                    CUSTOM_COARSE_DIRT.get(), new Item.Properties().tab(ItemGroup.TAB_BUILDING_BLOCKS)));

    /** Bedrock properties with the existing sand texture. */
    public static final RegistryObject<Block> CUSTOM_COARSE_SAND =
            BLOCKS.register("custom_coarse_sand", () -> new Block(Block.Properties.copy(Blocks.BEDROCK)));

    public static final RegistryObject<BlockItem> CUSTOM_COARSE_SAND_ITEM =
            ITEMS.register("custom_coarse_sand", () -> new BlockItem(
                    CUSTOM_COARSE_SAND.get(), new Item.Properties().tab(ItemGroup.TAB_BUILDING_BLOCKS)));

    /** Render-only item backing the standalone Reality Marble gear entity. */
    public static final RegistryObject<Item> GEAR_DISPLAY_ITEM =
            ITEMS.register("reality_marble_gear", () -> new Item(
                    new Item.Properties().tab(ItemGroup.TAB_MISC)));

    /** Lighter Mugen-only variant of the render-only Reality Marble gear item. */
    public static final RegistryObject<Item> MUGEN_GEAR_DISPLAY_ITEM =
            ITEMS.register("reality_marble_mugen_gear", () -> new Item(
                    new Item.Properties().tab(ItemGroup.TAB_MISC)));
}
