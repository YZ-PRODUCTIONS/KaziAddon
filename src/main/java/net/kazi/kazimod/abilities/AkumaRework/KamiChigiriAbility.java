package net.kazi.kazimod.abilities.AkumaRework;

import java.util.List;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectInstance;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.server.ServerWorld;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import xyz.pixelatedw.mineminenomi.api.abilities.Ability;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCategory;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityDescriptionLine;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.components.*;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceHakiNature;
import xyz.pixelatedw.mineminenomi.api.abilities.components.RangeComponent.RangeType;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.init.ModEffects;
import xyz.pixelatedw.mineminenomi.init.ModSounds;
import xyz.pixelatedw.mineminenomi.wypi.WyHelper;

public class KamiChigiriAbility extends Ability {

    private static final ITextComponent[] DESCRIPTION =
            AbilityHelper.registerDescriptionText("kazimod", "kami_chigiri",
                    new Pair[]{ImmutablePair.of(
                            "The user is launched 50 blocks into the sky, then slams down at lightning speed where they are aiming, dealing massive AOE damage on impact.", null)});

    private static final float DAMAGE = 45.0F;
    private static final float COOLDOWN = 600.0F; // 30 seconds
    private static final float RANGE = 8.0F;

    public static final AbilityCore<KamiChigiriAbility> INSTANCE;

    private final ContinuousComponent continuousComponent =
            new ContinuousComponent(this, true)
                    .addStartEvent(this::startContinuityEvent)
                    .addTickEvent(this::duringContinuityEvent)
                    .addEndEvent(this::endContinuityEvent);

    private final DealDamageComponent dealDamageComponent = new DealDamageComponent(this);
    private final RangeComponent rangeComponent = new RangeComponent(this);
    private final HitTrackerComponent hitTrackerComponent = new HitTrackerComponent(this);

    private boolean launched = false;
    private int ticksInAir = 0;
    private double targetX, targetY, targetZ;

    public KamiChigiriAbility(AbilityCore<KamiChigiriAbility> core) {
        super(core);
        this.isNew = true;

        this.addComponents(new AbilityComponent[]{
                this.continuousComponent,
                this.dealDamageComponent,
                this.rangeComponent,
                this.hitTrackerComponent
        });

        this.addUseEvent(this::onUseEvent);
    }

    private void onUseEvent(LivingEntity entity, IAbility ability) {
        if (!this.continuousComponent.isContinuous()) {
            // Store where the player is aiming for the slam
            RayTraceResult mop = WyHelper.rayTraceBlocksAndEntities(entity, 256.0);
            this.targetX = mop.getLocation().x;
            this.targetY = mop.getLocation().y;
            this.targetZ = mop.getLocation().z;

            this.launched = false;
            this.ticksInAir = 0;
            this.continuousComponent.startContinuity(entity, 60.0F); // 3 seconds max
        }
    }

    private void startContinuityEvent(LivingEntity entity, IAbility ability) {
        this.hitTrackerComponent.clearHits();

        // Launch player 50 blocks up
        entity.teleportToWithTicket(entity.getX(), entity.getY() + 50.0, entity.getZ());

        entity.level.playSound((PlayerEntity) null, entity.blockPosition(),
                (SoundEvent) ModSounds.DASH_ABILITY_SWOOSH_SFX.get(),
                SoundCategory.PLAYERS, 3.0F, 1.5F);

        this.launched = true;
    }

    private void duringContinuityEvent(LivingEntity entity, IAbility ability) {
        this.ticksInAir++;

        // Hold player in sky briefly (20 ticks = 1 second)
        if (this.ticksInAir <= 20) {
            AbilityHelper.slowEntityFall(entity);
            AbilityHelper.setDeltaMovement(entity, 0.0, 0.0, 0.0);
        }

        // After brief pause, slam down
        if (this.ticksInAir == 21) {
            // Teleport to target location
            entity.teleportToWithTicket(this.targetX, this.targetY, this.targetZ);

            // Deal AOE damage at landing
            List<LivingEntity> targets = this.rangeComponent.getTargetsInArea(entity, RANGE);
            for (LivingEntity target : targets) {
                if (this.hitTrackerComponent.canHit(target)) {
                    this.dealDamageComponent.hurtTarget(entity, target, DAMAGE);

                    // Knockback targets
                    Vector3d knockback = target.position().subtract(entity.position()).normalize().scale(1.5);
                    AbilityHelper.setDeltaMovement(target, knockback.x, 0.5, knockback.z);
                }
            }

            // Impact particles
            if (!entity.level.isClientSide) {
                for (int i = 0; i < 50; i++) {
                    double angle = entity.getRandom().nextDouble() * Math.PI * 2;
                    double dist = entity.getRandom().nextDouble() * RANGE;
                    double px = entity.getX() + Math.cos(angle) * dist;
                    double pz = entity.getZ() + Math.sin(angle) * dist;
                    ((ServerWorld) entity.level).sendParticles(ParticleTypes.EXPLOSION,
                            px, entity.getY() + 0.5, pz, 1, 0, 0, 0, 0);
                    ((ServerWorld) entity.level).sendParticles(ParticleTypes.LARGE_SMOKE,
                            px, entity.getY() + 0.5, pz, 2, 0, 0.3, 0, 0.1);
                    ((ServerWorld) entity.level).sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                            px, entity.getY() + 0.5, pz, 1, 0, 0.2, 0, 0.05);
                }
            }

            entity.level.playSound((PlayerEntity) null, entity.blockPosition(),
                    SoundEvents.GENERIC_EXPLODE,
                    SoundCategory.PLAYERS, 4.0F, 0.8F);

            this.continuousComponent.stopContinuity(entity);
        }
    }

    private void endContinuityEvent(LivingEntity entity, IAbility ability) {
        this.launched = false;
        this.ticksInAir = 0;
        this.cooldownComponent.startCooldown(entity, COOLDOWN);
    }

    static {
        INSTANCE = (new AbilityCore.Builder<>("Kami Chigiri", AbilityCategory.DEVIL_FRUITS, KamiChigiriAbility::new))
                .addDescriptionLine(DESCRIPTION)
                .addAdvancedDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{
                        AbilityDescriptionLine.NEW_LINE,
                        CooldownComponent.getTooltip(COOLDOWN),
                        DealDamageComponent.getTooltip(DAMAGE),
                        RangeComponent.getTooltip(RANGE, RangeType.AOE)
                })
                .setSourceHakiNature(SourceHakiNature.IMBUING)
                .build();
    }
}
