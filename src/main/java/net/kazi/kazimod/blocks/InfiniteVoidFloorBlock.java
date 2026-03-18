package net.kazi.kazimod.blocks;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.material.Material;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.shapes.VoxelShapes;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IWorld;
import net.minecraft.world.IWorldReader;

public class InfiniteVoidFloorBlock extends Block {

    public InfiniteVoidFloorBlock() {
        super(Properties.of(Material.STONE)
                .strength(-1.0F, 10000.0F)
                .noDrops());
    }

    @Override
    public VoxelShape getShape(BlockState state, IBlockReader world, BlockPos pos, ISelectionContext context) {
        return VoxelShapes.block(); // full solid cube
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, IBlockReader world, BlockPos pos, ISelectionContext context) {
        return VoxelShapes.block(); // full solid collision
    }

    @Override
    public boolean canSurvive(BlockState state, IWorldReader world, BlockPos pos) {
        return true; // survives anywhere — no support needed
    }

    @Override
    public BlockState updateShape(BlockState state, Direction facing, BlockState facingState,
                                  IWorld world, BlockPos pos, BlockPos facingPos) {
        return state; // never breaks from neighbor updates
    }

    @Override
    public boolean skipRendering(BlockState state, BlockState adjacentState, Direction side) {
        return adjacentState.getBlock() == this;
    }
}