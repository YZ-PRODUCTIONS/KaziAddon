//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package net.kazi.kazimod.abilities.KirinRework;

import net.MrMagicalCart.cartaddon.init.CartMorphs;
import net.kazi.kazimod.entities.projectiles.KnockoutBeamReworkProjectile;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.text.ITextComponent;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import xyz.pixelatedw.mineminenomi.api.abilities.Ability;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCategory;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityDescriptionLine;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AbilityComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.CooldownComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ProjectileComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.RequireMorphComponent;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.api.morph.MorphInfo;
import xyz.pixelatedw.mineminenomi.init.ModSounds;

public class DreamwavePulseAbility extends Ability {
    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText("kazimod", "dreamwave_pulse", new Pair[]{ImmutablePair.of("The user shoots a small beam at the target that makes them tired for a short duration.", (Object)null)});
    private static final float COOLDOWN = 200.0F;
    public static final AbilityCore<DreamwavePulseAbility> INSTANCE;
    private final ProjectileComponent projectileComponent = new ProjectileComponent(this, this::createProjectile);
    private final RequireMorphComponent requireMorphComponent;

    public DreamwavePulseAbility(AbilityCore<DreamwavePulseAbility> core) {
        super(core);
        this.requireMorphComponent = new RequireMorphComponent(this, (MorphInfo)CartMorphs.KIRIN_HEAVY.get(), new MorphInfo[]{(MorphInfo)CartMorphs.KIRIN_FLY.get(), (MorphInfo)CartMorphs.KIRIN_HEAVY_ALT.get()});
        this.isNew = true;
        this.addComponents(new AbilityComponent[]{this.projectileComponent, this.requireMorphComponent});
        this.addUseEvent(this::useEvent);
    }

    private void useEvent(LivingEntity entity, IAbility ability) {
        this.projectileComponent.shoot(entity, 4.0F, 1.25F);
        entity.level.playSound((PlayerEntity)null, entity.blockPosition(), (SoundEvent)ModSounds.NORO_SFX.get(), SoundCategory.PLAYERS, 2.0F, 1.0F);
        this.cooldownComponent.startCooldown(entity, 200.0F);
    }

    private KnockoutBeamReworkProjectile createProjectile(LivingEntity entity) {
        KnockoutBeamReworkProjectile proj = new KnockoutBeamReworkProjectile(entity.level, entity, this);
        return proj;
    }

    static {
        INSTANCE = (new AbilityCore.Builder("Dreamwave Pulse", AbilityCategory.DEVIL_FRUITS, DreamwavePulseAbility::new)).addDescriptionLine(DESCRIPTION).addAdvancedDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{AbilityDescriptionLine.NEW_LINE, CooldownComponent.getTooltip(200.0F)}).addAdvancedDescriptionLine(ProjectileComponent.getProjectileTooltips()).build();
    }
}