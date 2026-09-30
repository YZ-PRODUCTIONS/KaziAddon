/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.entity.Entity
 *  net.minecraft.entity.LivingEntity
 *  net.minecraft.entity.MoverType
 *  net.minecraft.potion.Effect
 *  net.minecraft.potion.EffectInstance
 *  net.minecraft.util.math.RayTraceResult
 *  net.minecraft.util.math.RayTraceResult$Type
 *  net.minecraft.util.math.vector.Vector3d
 *  net.minecraft.util.text.ITextComponent
 *  org.apache.commons.lang3.tuple.ImmutablePair
 *  org.apache.commons.lang3.tuple.Pair
 *  xyz.pixelatedw.mineminenomi.api.abilities.Ability
 *  xyz.pixelatedw.mineminenomi.api.abilities.AbilityCategory
 *  xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore
 *  xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore$Builder
 *  xyz.pixelatedw.mineminenomi.api.abilities.AbilityDescriptionLine
 *  xyz.pixelatedw.mineminenomi.api.abilities.AbilityDescriptionLine$IDescriptionLine
 *  xyz.pixelatedw.mineminenomi.api.abilities.IAbility
 *  xyz.pixelatedw.mineminenomi.api.abilities.components.AbilityComponent
 *  xyz.pixelatedw.mineminenomi.api.abilities.components.AnimationComponent
 *  xyz.pixelatedw.mineminenomi.api.abilities.components.ContinuousComponent
 *  xyz.pixelatedw.mineminenomi.api.abilities.components.CooldownComponent
 *  xyz.pixelatedw.mineminenomi.api.abilities.components.DealDamageComponent
 *  xyz.pixelatedw.mineminenomi.api.abilities.components.HitTrackerComponent
 *  xyz.pixelatedw.mineminenomi.api.abilities.components.ProjectileComponent
 *  xyz.pixelatedw.mineminenomi.api.abilities.components.RangeComponent
 *  xyz.pixelatedw.mineminenomi.api.abilities.components.RangeComponent$RangeType
 *  xyz.pixelatedw.mineminenomi.api.damagesource.SourceHakiNature
 *  xyz.pixelatedw.mineminenomi.api.damagesource.SourceType
 *  xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper
 *  xyz.pixelatedw.mineminenomi.init.ModAnimations
 *  xyz.pixelatedw.mineminenomi.init.ModEffects
 *  xyz.pixelatedw.mineminenomi.wypi.WyHelper
 */
package net.kazi.kazimod.abilities.SusuRework;

import java.util.List;
import net.MrMagicalCart.cartaddon.entities.projectiles.susu.SusuPillarEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MoverType;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectInstance;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.ITextComponent;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import xyz.pixelatedw.mineminenomi.api.abilities.Ability;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCategory;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityDescriptionLine;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AbilityComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AnimationComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ContinuousComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.CooldownComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.DealDamageComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.HitTrackerComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ProjectileComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.RangeComponent;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceHakiNature;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceType;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.init.ModAnimations;
import xyz.pixelatedw.mineminenomi.init.ModEffects;
import xyz.pixelatedw.mineminenomi.wypi.WyHelper;

public class ObelisusuRework
extends Ability {
    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText((String)"cartaddon", (String)"obelisusu", (Pair[])new Pair[]{ImmutablePair.of((Object)"The user dashes through targets marking them with soot, then summons soot spikes to impale the targets.", null)});
    private static final int HOLD_TIME = 20;
    private static final int COOLDOWN = 300;
    private static final float RANGE = 1.4f;
    private static final float DAMAGE = 35.0f;
    public static final AbilityCore<ObelisusuRework> INSTANCE = new AbilityCore.Builder("Obelisusu", AbilityCategory.DEVIL_FRUITS, ObelisusuRework::new).addDescriptionLine(DESCRIPTION).addAdvancedDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{AbilityDescriptionLine.NEW_LINE, DealDamageComponent.getTooltip((float)25.0f), ContinuousComponent.getTooltip((float)20.0f), CooldownComponent.getTooltip((float)300.0f), RangeComponent.getTooltip((float)1.4f, (RangeComponent.RangeType)RangeComponent.RangeType.LINE)}).setSourceType(new SourceType[]{SourceType.SLASH}).setSourceHakiNature(SourceHakiNature.SPECIAL).build();
    private final ContinuousComponent continuousComponent = new ContinuousComponent((IAbility)this, true).addStartEvent(this::startContinuityEvent).addTickEvent(this::duringContinuityEvent).addEndEvent(this::endContinuityEvent);
    private final RangeComponent rangeComponent = new RangeComponent((IAbility)this);
    private final DealDamageComponent dealDamageComponent = new DealDamageComponent((IAbility)this);
    private final AnimationComponent animationComponent = new AnimationComponent((IAbility)this);
    private final HitTrackerComponent hitTrackerComponent = new HitTrackerComponent((IAbility)this);
    private final ProjectileComponent projectileComponent = new ProjectileComponent((IAbility)this, this::createProjectile);

    public ObelisusuRework(AbilityCore<ObelisusuRework> core) {
        super(core);
        this.isNew = true;
        this.addComponents(new AbilityComponent[]{this.projectileComponent, this.continuousComponent, this.rangeComponent, this.dealDamageComponent, this.animationComponent, this.hitTrackerComponent});
        this.addCanUseCheck(AbilityHelper::canUseMomentumAbilities);
        this.addUseEvent(this::useEvent);
    }

    private void useEvent(LivingEntity entity, IAbility ability) {
        this.continuousComponent.triggerContinuity(entity, 20.0f);
    }

    private void startContinuityEvent(LivingEntity entity, IAbility ability) {
        this.hitTrackerComponent.clearHits();
        this.animationComponent.start(entity, ModAnimations.CHARGE_PUNCH);
    }

    private void duringContinuityEvent(LivingEntity entity, IAbility ability) {
        if (entity.isAlive()) {
            Vector3d look = entity.getLookAngle();
            Vector3d speed = look.scale(1.4);
            entity.move(MoverType.SELF, speed);
            List<LivingEntity> targets = this.rangeComponent.getTargetsInArea(entity, 2.0f);
            for (LivingEntity target : targets) {
                if (!this.hitTrackerComponent.canHit((Entity)target)) continue;
                this.dealDamageComponent.hurtTarget(entity, target, 25.0f);
                RayTraceResult mop = WyHelper.rayTraceBlocksAndEntities((Entity)entity, (double)64.0);
                if (mop.getType() == RayTraceResult.Type.MISS) continue;
                double i = mop.getLocation().x;
                double j = mop.getLocation().y;
                double k = mop.getLocation().z;
                SusuPillarEntity pillar = (SusuPillarEntity)this.projectileComponent.getNewProjectile(entity);
                pillar.moveTo(i, j - 6.0, k, 0.0f, 0.0f);
                pillar.shoot(0.0, 0.001, 0.0, 1.5f, 0.0f);
                entity.level.addFreshEntity((Entity)pillar);
                this.cooldownComponent.startCooldown(entity, 450.0f);
            }
        }
    }

    private SusuPillarEntity createProjectile(LivingEntity entity) {
        SusuPillarEntity pillar = new SusuPillarEntity(entity.level, entity);
        return pillar;
    }

    private void endContinuityEvent(LivingEntity entity, IAbility ability) {
        this.animationComponent.stop(entity);
        this.cooldownComponent.startCooldown(entity, 300.0f);
    }
}



