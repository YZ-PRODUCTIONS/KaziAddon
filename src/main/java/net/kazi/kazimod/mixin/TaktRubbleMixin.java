package net.kazi.kazimod.mixin;

import java.util.*;
import net.MrMagicalCart.cartaddon.entities.projectiles.opeextra.TaktTossProjectile;
import net.kazi.kazimod.ope.TaktRubbleData;
import net.minecraft.block.*;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.datasync.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.pixelatedw.mineminenomi.entities.projectiles.AbilityProjectileEntity;

@Mixin(value = AbilityProjectileEntity.class, remap = false, priority = 900)
public abstract class TaktRubbleMixin implements TaktRubbleData {
    @Unique private static final DataParameter<CompoundNBT> kazi$palette = EntityDataManager.defineId(AbilityProjectileEntity.class, DataSerializers.COMPOUND_TAG);
    @Unique private AbilityProjectileEntity kazi$self() { return (AbilityProjectileEntity)(Object)this; }
    @Inject(method = "defineSynchedData", at = @At("TAIL"), remap = true)
    private void kazi$define(CallbackInfo ci) {
        if ((Object)this instanceof TaktTossProjectile) kazi$self().getEntityData().define(kazi$palette, new CompoundNBT());
    }
    @Inject(method = "tick", at = @At("HEAD"), remap = true)
    private void kazi$sampleGround(CallbackInfo ci) {
        AbilityProjectileEntity self = kazi$self();
        World level = self.level;
        if (!(self instanceof TaktTossProjectile) || level.isClientSide || self.getEntityData().get(kazi$palette).contains("Blocks")) return;
        List<Integer> blocks = new ArrayList<>();
        BlockPos origin = self.blockPosition().above(6);
        for (int x = -2; x <= 2; x += 2) for (int z = -2; z <= 2; z += 2) {
            for (int y = 2; y >= -12; y--) {
                BlockPos pos = origin.offset(x, y, z);
                if (!level.hasChunkAt(pos)) continue;
                BlockState state = level.getBlockState(pos);
                if (state.isAir() || !state.getFluidState().isEmpty() || state.hasTileEntity()
                        || state.getRenderShape() != BlockRenderType.MODEL || state.getDestroySpeed(level, pos) < 0) continue;
                blocks.add(Block.getId(state));
                if (blocks.size() >= 18) break;
            }
            if (blocks.size() >= 18) break;
        }
        if (blocks.isEmpty()) blocks.add(Block.getId(Blocks.STONE.defaultBlockState()));
        CompoundNBT tag = new CompoundNBT(); tag.putIntArray("Blocks", blocks.stream().mapToInt(Integer::intValue).limit(18).toArray());
        self.getEntityData().set(kazi$palette, tag);
    }
    @Override public int[] kazi$rubblePalette() { return kazi$self().getEntityData().get(kazi$palette).getIntArray("Blocks"); }
}
