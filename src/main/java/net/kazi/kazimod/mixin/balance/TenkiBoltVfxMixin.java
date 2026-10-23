package net.kazi.kazimod.mixin.balance;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import xyz.pixelatedw.mineminenomi.entities.projectiles.goro.LightningEntity;

@Mixin(targets={"net.kazi.kazimod.abilities.Tenki.ThunderstormAbility","net.kazi.kazimod.abilities.Tenki.TornadoWrathAbility"},remap=false)
public abstract class TenkiBoltVfxMixin {
    @ModifyVariable(method="setBoltProps",at=@At("HEAD"),argsOnly=true,ordinal=0)
    private LightningEntity kazimod$markTenkiBolt(LightningEntity bolt){bolt.getPersistentData().putBoolean("KaziTenkiVfx",true);return bolt;}
}
