//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package net.kazi.kazimod.abilities.BaraRework;

import net.minecraft.entity.LivingEntity;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectInstance;
import net.minecraft.util.text.ITextComponent;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import xyz.pixelatedw.mineminenomi.abilities.bara.BaraHelper;
import xyz.pixelatedw.mineminenomi.api.abilities.Ability;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCategory;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityDescriptionLine;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityPool2;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AbilityComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ContinuousComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.CooldownComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.GrabEntityComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.MorphComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.PoolComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ProjectileComponent;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceHakiNature;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceType;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.api.morph.MorphInfo;
import xyz.pixelatedw.mineminenomi.entities.projectiles.AbilityProjectileEntity;
import xyz.pixelatedw.mineminenomi.entities.projectiles.bara.DaiCircusProjectile;
import xyz.pixelatedw.mineminenomi.init.ModAbilityPools;
import xyz.pixelatedw.mineminenomi.init.ModEffects;
import xyz.pixelatedw.mineminenomi.init.ModMorphs;

public class KuchuKirimomiDaiCircusRework extends Ability {
    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText("mineminenomi", "kuchu_kirimomi_dai_circus", new Pair[]{ImmutablePair.of("Fires both fists at an enemy and lifts them up, moving them around according to the user's movements.", (Object)null)});
    private static final int COOLDOWN = 300;
    private static final int HOLD_TIME = 120;
    public static final AbilityCore<KuchuKirimomiDaiCircusRework> INSTANCE;
    private final ContinuousComponent continuousComponent = (new ContinuousComponent(this)).addTickEvent(this::duringContinuityEvent).addEndEvent(this::endContinuityEvent);
    private final ProjectileComponent projectileComponent = new ProjectileComponent(this, this::createProjectile);
    private final GrabEntityComponent grabEntityComponent = new GrabEntityComponent(this, true, true, 7.0F);
    private final MorphComponent morphComponent = new MorphComponent(this);
    private final PoolComponent poolComponent;

    public KuchuKirimomiDaiCircusRework(AbilityCore<KuchuKirimomiDaiCircusRework> core) {
        super(core);
        this.poolComponent = new PoolComponent(this, ModAbilityPools.GRAB_ABILITY, new AbilityPool2[0]);
        this.isNew = true;
        this.addComponents(new AbilityComponent[]{this.continuousComponent, this.projectileComponent, this.grabEntityComponent, this.morphComponent, this.poolComponent});
        this.addCanUseCheck(BaraHelper::hasLimbs);
        this.addTickEvent(this::tickEvent);
        this.addUseEvent(this::useEvent);
    }

    private void tickEvent(LivingEntity entity, IAbility ability) {
        if (!entity.level.isClientSide && !this.continuousComponent.isContinuous() && this.morphComponent.isMorphed()) {
            AbilityProjectileEntity projectile = this.projectileComponent.getShotProjectile();
            if (projectile == null || !projectile.isAlive()) {
                this.morphComponent.stopMorph(entity);
            }
        }

    }

    private void useEvent(LivingEntity entity, IAbility ability) {
        if (this.continuousComponent.isContinuous()) {
            this.continuousComponent.stopContinuity(entity);
        } else {
            this.projectileComponent.shoot(entity, 3.0F, 0.0F);
            int projectileLife = this.projectileComponent.getShotProjectile().getLife();
            this.morphComponent.startMorph(entity, (MorphInfo)ModMorphs.BARA_CIRCUS.get());
            entity.addEffect(new EffectInstance((Effect)ModEffects.NO_HANDS.get(), projectileLife, 0));
        }
    }

    private void duringContinuityEvent(LivingEntity entity, IAbility ability) {
        entity.addEffect(new EffectInstance((Effect)ModEffects.NO_HANDS.get(), 5, 0));
        if (!entity.level.isClientSide && this.grabEntityComponent.hasGrabbedEntity()) {
            LivingEntity target = this.grabEntityComponent.getGrabbedEntity();
            if (target != null) {
                target.addEffect(new EffectInstance((Effect)ModEffects.GRABBED.get(), 2, 3));
            }
        }
    }

    private void endContinuityEvent(LivingEntity entity, IAbility ability) {
        this.morphComponent.stopMorph(entity);
        this.grabEntityComponent.release(entity);
        this.cooldownComponent.startCooldown(entity, 300.0F);
    }

    private DaiCircusProjectile createProjectile(LivingEntity entity) {
        DaiCircusProjectile proj = new DaiCircusProjectile(entity.level, entity, this);
        proj.onEntityImpactEvent = (hit) -> {
            if (this.grabEntityComponent.grabManually(entity, hit)) {
                this.continuousComponent.startContinuity(entity, 120.0F);
            }

        };
        return proj;
    }

    static {
        INSTANCE = (new AbilityCore.Builder("Kuchu Kirimomi Dai Circus", AbilityCategory.DEVIL_FRUITS, KuchuKirimomiDaiCircusRework::new)).addDescriptionLine(DESCRIPTION).addAdvancedDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{AbilityDescriptionLine.NEW_LINE, CooldownComponent.getTooltip(300.0F), ContinuousComponent.getTooltip(120.0F)}).setSourceHakiNature(SourceHakiNature.HARDENING).setSourceType(new SourceType[]{SourceType.FIST}).build();
    }
}
