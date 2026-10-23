package net.kazi.kazimod.mixin.client;

import net.kazi.kazimod.saiyan.SaiyanState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import xyz.pixelatedw.mineminenomi.data.entity.entitystats.IEntityStats;
import xyz.pixelatedw.mineminenomi.screens.PlayerStatsScreen;

@Mixin(PlayerStatsScreen.class)
public abstract class SaiyanRaceMenuMixin {
    @Shadow(remap=false) @Final private PlayerEntity player;
    @Unique private static final ResourceLocation SAIYAN_RACE=new ResourceLocation("kazimod","saiyan");

    @Redirect(method="render",at=@At(value="INVOKE",target="Lxyz/pixelatedw/mineminenomi/data/entity/entitystats/IEntityStats;getRace()Lnet/minecraft/util/ResourceLocation;",remap=false))
    private ResourceLocation saiyan$displayRace(IEntityStats stats){
        return player!=null&&SaiyanState.hasFruit(player)?SAIYAN_RACE:stats.getRace();
    }
}
