//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package net.kazi.kazimod.abilities.KirinRework;

import net.MrMagicalCart.cartaddon.entities.projectiles.ryukirin.KnockoutBeamProjectile;
import net.MrMagicalCart.cartaddon.init.CartMorphs;
import net.kazi.kazimod.entities.projectiles.KnockoutBeamReworkProjectile;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.text.ITextComponent;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import xyz.pixelatedw.mineminenomi.api.abilities.Ability;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCategory;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityDescriptionLine;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AbilityComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ChargeComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.CooldownComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ProjectileComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.RequireMorphComponent;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.api.morph.MorphInfo;
import xyz.pixelatedw.mineminenomi.init.ModSounds;

public class KnockoutBeamRework extends Ability {
    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText("cartaddon", "knockout_beam", new Pair[]{ImmutablePair.of("The user shoots a small beam at the target that puts them to sleep for a short duration.", (Object)null)});
    private static final float COOLDOWN = 800.0F;
    private static final float CHARGE_TIME = 50.0F;
    public static final AbilityCore<KnockoutBeamRework> INSTANCE;
    private final ProjectileComponent projectileComponent = new ProjectileComponent(this, this::createProjectile);
    private final RequireMorphComponent requireMorphComponent;
    private final ChargeComponent chargeComponent = (new ChargeComponent(this)).addStartEvent(this::startChargeEvent).addTickEvent(this::tickChargeEvent).addEndEvent(this::endChargeEvent);

    public KnockoutBeamRework(AbilityCore<KnockoutBeamRework> core) {
        super(core);
        this.requireMorphComponent = new RequireMorphComponent(this, (MorphInfo)CartMorphs.KIRIN_HEAVY.get(), new MorphInfo[]{(MorphInfo)CartMorphs.KIRIN_FLY.get(), (MorphInfo)CartMorphs.KIRIN_HEAVY_ALT.get()});
        this.isNew = true;
        this.addComponents(new AbilityComponent[]{this.chargeComponent, this.projectileComponent, this.requireMorphComponent});
        this.addUseEvent(this::useEvent);
    }

    private void useEvent(LivingEntity entity, IAbility ability) {
        if (!this.chargeComponent.isCharging()) {
            this.chargeComponent.startCharging(entity, 50.0F);
        }

    }

    private void startChargeEvent(LivingEntity entity, IAbility ability) {
    }

    private void tickChargeEvent(LivingEntity entity, IAbility ability) {
        if (this.chargeComponent.getChargeTime() % 10.0F == 0.0F) {
            entity.level.playSound((PlayerEntity)null, entity.blockPosition(), SoundEvents.PHANTOM_AMBIENT, SoundCategory.PLAYERS, 2.0F, 2.0F);
        }

    }

    private void endChargeEvent(LivingEntity entity, IAbility ability) {
        this.projectileComponent.shoot(entity, 4.0F, 1.25F);
        entity.level.playSound((PlayerEntity)null, entity.blockPosition(), (SoundEvent)ModSounds.NORO_SFX.get(), SoundCategory.PLAYERS, 2.0F, 1.0F);
        this.cooldownComponent.startCooldown(entity, 800.0F);
    }

    private KnockoutBeamReworkProjectile createProjectile(LivingEntity entity) {
        KnockoutBeamReworkProjectile proj = new KnockoutBeamReworkProjectile(entity.level, entity, this);
        return proj;
    }

    static {
        INSTANCE = (new AbilityCore.Builder("Knockout Beam", AbilityCategory.DEVIL_FRUITS, KnockoutBeamRework::new)).addDescriptionLine(DESCRIPTION).addAdvancedDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{AbilityDescriptionLine.NEW_LINE, ChargeComponent.getTooltip(50.0F), CooldownComponent.getTooltip(800.0F)}).addAdvancedDescriptionLine(ProjectileComponent.getProjectileTooltips()).build();
    }
}
