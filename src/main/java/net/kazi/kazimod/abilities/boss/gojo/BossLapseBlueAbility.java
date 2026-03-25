package net.kazi.kazimod.abilities.boss.gojo;

import net.minecraft.entity.LivingEntity;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.util.math.vector.Vector3d;
import xyz.pixelatedw.mineminenomi.api.abilities.Ability;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCategory;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AbilityComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ContinuousComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.DealDamageComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.HitTrackerComponent;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceElement;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceType;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.init.ModEffects;
import xyz.pixelatedw.mineminenomi.init.ModEntityPredicates;
import xyz.pixelatedw.mineminenomi.wypi.WyHelper;

import java.util.List;

/**
 * Boss-safe Lapse Blue: continuous pull toward boss, no grab/slam.
 *
 * Stripped of:
 *  - PoolComponent        → never blocked by other grab abilities sharing GRAB_ABILITY pool
 *  - canUseMomentumAbilities check → works on mobs (player-only check)
 *  - GrabEntityComponent  → grab requires player-side packets
 *  - RangeComponent       → was unused and caused isOutsideDistance to return 0
 *  - AnimationComponent   → skipped for simplicity on boss
 *
 * The ability fires via AbilityWrapperGoal.canUse() which calls ability.canUse().
 * Since we have NO addCanUseCheck calls, canUse() always passes (only blocked by
 * CooldownComponent and DisableComponent which AbilityWrapperGoal checks directly).
 */
public class BossLapseBlueAbility extends Ability {

    private static final float COOLDOWN      = 300.0F; // 15 seconds
    private static final float PULL_TIME     = 100.0F; // 5 seconds of pulling
    private static final float PULL_RANGE    = 20.0F;
    private static final float PULL_STRENGTH = 0.9F;

    public static final AbilityCore<BossLapseBlueAbility> INSTANCE;

    private final ContinuousComponent pullComponent =
            new ContinuousComponent(this, true)
                    .addStartEvent(this::onPullStart)
                    .addTickEvent(this::onPullTick)
                    .addEndEvent(this::onPullEnd);

    private final DealDamageComponent  dealDamageComponent  = new DealDamageComponent(this);
    private final HitTrackerComponent  hitTrackerComponent  = new HitTrackerComponent(this);

    public BossLapseBlueAbility(AbilityCore<BossLapseBlueAbility> core) {
        super(core);
        this.isNew = true;
        // Only these three components — NO PoolComponent, NO RangeComponent
        this.addComponents(new AbilityComponent[]{
                this.pullComponent,
                this.dealDamageComponent,
                this.hitTrackerComponent
        });
        // NO addCanUseCheck — canUse() passes freely for mobs
        this.addUseEvent(this::onUseEvent);
    }

    private void onUseEvent(LivingEntity entity, IAbility ability) {
        if (this.pullComponent.isContinuous()) {
            this.pullComponent.stopContinuity(entity);
            return;
        }
        if (!entity.level.isClientSide) {
            this.pullComponent.triggerContinuity(entity, PULL_TIME);
        }
    }

    private void onPullStart(LivingEntity entity, IAbility ability) {
        this.hitTrackerComponent.clearHits();
    }

    private void onPullTick(LivingEntity entity, IAbility ability) {
        if (entity.level.isClientSide) return;

        Vector3d look = entity.getLookAngle().normalize();

        for (int i = 5; i <= (int) PULL_RANGE; i += 3) {
            Vector3d point = entity.position().add(0, 1, 0).add(look.scale(i));

            List<LivingEntity> nearby = WyHelper.getNearbyLiving(
                    point, entity.level, 4.0,
                    ModEntityPredicates.getEnemyFactions(entity));

            for (LivingEntity target : nearby) {
                Vector3d toUser = entity.position()
                        .add(0, 1, 0)
                        .subtract(target.position())
                        .normalize();
                AbilityHelper.setDeltaMovement(target,
                        toUser.x * PULL_STRENGTH,
                        toUser.y * PULL_STRENGTH,
                        toUser.z * PULL_STRENGTH);
                target.addEffect(new EffectInstance(Effects.MOVEMENT_SLOWDOWN, 10, 3));
                target.addEffect(new EffectInstance(
                        (Effect) ModEffects.MOVEMENT_BLOCKED.get(), 10, 3));
            }
        }
    }

    private void onPullEnd(LivingEntity entity, IAbility ability) {
        if (!entity.level.isClientSide) {
            super.cooldownComponent.startCooldown(entity, COOLDOWN);
        }
    }

    public boolean isContinuous() {
        return pullComponent.isContinuous();
    }

    static {
        INSTANCE = new AbilityCore.Builder<>("Boss: Lapse Blue",
                AbilityCategory.DEVIL_FRUITS, BossLapseBlueAbility::new)
                .setSourceElement(SourceElement.SHOCKWAVE)
                .setSourceType(SourceType.INDIRECT, SourceType.INTERNAL)
                .setPhantomKey(new net.minecraft.util.ResourceLocation("kazimod", "boss_lapse_blue"))
                .build();
    }
}
