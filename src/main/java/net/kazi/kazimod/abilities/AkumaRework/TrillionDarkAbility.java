package net.kazi.kazimod.abilities.AkumaRework;

import net.kazi.kazimod.entities.projectiles.DarkSpearProjectile;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.RayTraceResult;
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
import xyz.pixelatedw.mineminenomi.init.ModSounds;
import xyz.pixelatedw.mineminenomi.wypi.WyHelper;

public class TrillionDarkAbility extends Ability {

    private static final ITextComponent[] DESCRIPTION =
            AbilityHelper.registerDescriptionText("kazimod", "trillion_dark",
                    new Pair[]{ImmutablePair.of(
                            "Rains down endless amounts of purple spears from the sky for 6 seconds where you aim. The user is held in the sky during the attack.", null)});

    public static final float DAMAGE_VALUE = 8.0F;
    private static final float DAMAGE_PER_HIT = DAMAGE_VALUE;
    private static final float CONTINUOUS_DURATION = 120.0F; // 6 seconds
    private static final float COOLDOWN = 1000.0F; // 50 seconds
    private static final float RANGE = 10.0F;

    public static final AbilityCore<TrillionDarkAbility> INSTANCE;

    private final ContinuousComponent continuousComponent =
            new ContinuousComponent(this, true)
                    .addStartEvent(this::startContinuityEvent)
                    .addTickEvent(this::duringContinuityEvent)
                    .addEndEvent(this::endContinuityEvent);

    private final DealDamageComponent dealDamageComponent = new DealDamageComponent(this);
    private final RangeComponent rangeComponent = new RangeComponent(this);

    public TrillionDarkAbility(AbilityCore<TrillionDarkAbility> core) {
        super(core);
        this.isNew = true;

        this.addComponents(new AbilityComponent[]{
                this.continuousComponent,
                this.dealDamageComponent,
                this.rangeComponent
        });

        this.addUseEvent(this::onUseEvent);
    }

    private void onUseEvent(LivingEntity entity, IAbility ability) {
        if (!this.continuousComponent.isContinuous()) {
            this.continuousComponent.startContinuity(entity, CONTINUOUS_DURATION);
        }
    }

    private void startContinuityEvent(LivingEntity entity, IAbility ability) {
        entity.level.playSound((PlayerEntity) null, entity.blockPosition(),
                (SoundEvent) ModSounds.DASH_ABILITY_SWOOSH_SFX.get(),
                SoundCategory.PLAYERS, 3.0F, 0.3F);
    }

    private void duringContinuityEvent(LivingEntity entity, IAbility ability) {
        // Hold player in the sky
        AbilityHelper.slowEntityFall(entity);
        AbilityHelper.setDeltaMovement(entity, 0.0, 0.0, 0.0);

        // Get where the player is aiming
        RayTraceResult mop = WyHelper.rayTraceBlocksAndEntities(entity, 256.0);
        double aimX = mop.getLocation().x;
        double aimY = mop.getLocation().y;
        double aimZ = mop.getLocation().z;

        if (!entity.level.isClientSide) {
            // Spawn 8 dark spear projectiles per tick, raining from sky
            for (int i = 0; i < 8; i++) {
                double offsetX = (entity.getRandom().nextDouble() - 0.5) * RANGE * 2;
                double offsetZ = (entity.getRandom().nextDouble() - 0.5) * RANGE * 2;
                double startY = aimY + 15 + entity.getRandom().nextDouble() * 10;

                DarkSpearProjectile spear = new DarkSpearProjectile(entity.level, entity);
                spear.setPos(aimX + offsetX, startY, aimZ + offsetZ);
                spear.setDeltaMovement(0, -1.5, 0); // Fall straight down fast
                entity.level.addFreshEntity(spear);
            }

            // Impact smoke at ground level
            for (int i = 0; i < 3; i++) {
                double offsetX = (entity.getRandom().nextDouble() - 0.5) * RANGE * 2;
                double offsetZ = (entity.getRandom().nextDouble() - 0.5) * RANGE * 2;
                ((ServerWorld) entity.level).sendParticles(ParticleTypes.LARGE_SMOKE,
                        aimX + offsetX, aimY + 0.5, aimZ + offsetZ,
                        2, 0.3, 0.1, 0.3, 0.02);
            }

            // Every second play impact sound
            if (entity.tickCount % 20 == 0) {
                entity.level.playSound((PlayerEntity) null, entity.blockPosition(),
                        SoundEvents.LIGHTNING_BOLT_IMPACT,
                        SoundCategory.PLAYERS, 2.0F, 1.2F);
            }
        }
    }

    private void endContinuityEvent(LivingEntity entity, IAbility ability) {
        this.cooldownComponent.startCooldown(entity, COOLDOWN);
    }

    static {
        INSTANCE = (new AbilityCore.Builder<>("Trillion Dark", AbilityCategory.DEVIL_FRUITS, TrillionDarkAbility::new))
                .addDescriptionLine(DESCRIPTION)
                .addAdvancedDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{
                        AbilityDescriptionLine.NEW_LINE,
                        ContinuousComponent.getTooltip(CONTINUOUS_DURATION),
                        CooldownComponent.getTooltip(COOLDOWN),
                        DealDamageComponent.getTooltip(DAMAGE_PER_HIT),
                        RangeComponent.getTooltip(RANGE, RangeType.AOE)
                })
                .setSourceHakiNature(SourceHakiNature.IMBUING)
                .build();
    }
}
