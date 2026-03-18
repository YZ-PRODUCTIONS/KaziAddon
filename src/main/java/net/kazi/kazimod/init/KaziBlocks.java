package net.kazi.kazimod.init;

import net.kazi.kazimod.blocks.InfiniteVoidFloorBlock;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraft.block.Block;

public class KaziBlocks {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, "kazimod");

    public static final RegistryObject<Block> INFINITE_VOID_FLOOR =
            BLOCKS.register("infinite_void_floor", InfiniteVoidFloorBlock::new);
}