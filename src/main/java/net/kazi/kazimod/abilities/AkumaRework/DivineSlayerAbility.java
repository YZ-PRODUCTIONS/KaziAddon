package net.kazi.kazimod.abilities.AkumaRework;

import java.awt.Color;

import net.kazi.kazimod.entities.projectiles.SlashWaveProjectile;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.play.server.SAnimateHandPacket;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectInstance;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.SoundEvents;
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
import xyz.pixelatedw.mineminenomi.entities.LightningDischargeEntity;
import xyz.pixelatedw.mineminenomi.init.ModEffects;
import xyz.pixelatedw.mineminenomi.init.ModSounds;

public class DivineSlayerAbility extends Ability {

    private static final ITextComponent[] DESCRIPTION =
            AbilityHelper.registerDescriptionText("kazimod", "divine_slayer",
                    new Pair[]{ImmutablePair.of(
                            "The user charges up and sends out a devastating 15-block slash that destroys everything in its path.", null)});

    public static final float DAMAGE_VALUE = 60.0F;
    private static final float DAMAGE = DAMAGE_VALUE;
    private static final float COOLDOWN = 500.0F; // 25 seconds
    private static final float CHARGE_TIME = 30.0F; // 1.5 seconds
    private static final float SLASH_DISTANCE = 15.0F;

    public static final AbilityCore<DivineSlayerAbility> INSTANCE;

    private final ChargeComponent chargeComponent =
            new ChargeComponent(this)
                    .addStartEvent(this::startChargeEvent)
                    .addTickEvent(this::duringChargeEvent)
                    .addEndEvent(this::endChargeEvent);

    private final DealDamageComponent dealDamageComponent = new DealDamageComponent(this);
    private final RangeComponent rangeComponent = new RangeComponent(this);

    private LightningDischargeEntity discharge;

    public DivineSlayerAbility(AbilityCore<DivineSlayerAbility> core) {
        super(core);
        this.isNew = true;

        this.addComponents(new AbilityComponent[]{
                this.chargeComponent,
                this.dealDamageComponent,
                this.rangeComponent
        });

        this.addUseEvent(this::onUseEvent);
    }

    private void onUseEvent(LivingEntity entity, IAbility ability) {
        if (!this.chargeComponent.isCharging()) {
            this.chargeComponent.startCharging(entity, CHARGE_TIME);
        }
    }

    private void startChargeEvent(LivingEntity entity, IAbility ability) {
        entity.level.playSound((PlayerEntity) null, entity.blockPosition(),
                (SoundEvent) ModSounds.DASH_ABILITY_SWOOSH_SFX.get(),
                SoundCategory.PLAYERS, 2.0F, 0.5F);

        // Spawn the lightning discharge visual (like Divine Departure)
        this.discharge = new LightningDischargeEntity(entity,
                entity.getX(), entity.getY() + 1.5, entity.getZ(),
                entity.yRot, entity.xRot);
        this.discharge.setAliveTicks(-1);
        this.discharge.setUpdateRate(8);
        this.discharge.setLightningLength(30.0F);
        this.discharge.setColor(new Color(0, 0, 0, 100));
        this.discharge.setOutlineColor(new Color(128, 0, 200));
        this.discharge.setRenderTransparent();
        this.discharge.setDetails(16);
        this.discharge.setDensity(24);
        this.discharge.setSize(1.0F);
        this.discharge.setSkipSegments(1);
        entity.level.addFreshEntity(this.discharge);
    }

    private void duringChargeEvent(LivingEntity entity, IAbility ability) {
        entity.addEffect(new EffectInstance((Effect) ModEffects.MOVEMENT_BLOCKED.get(), 2, 0, false, false));

        // Dark energy gathering particles
        if (!entity.level.isClientSide) {
            for (int i = 0; i < 6; i++) {
                double angle = (entity.tickCount * 0.5 + i * 60) % (Math.PI * 2);
                double radius = 1.5;
                double px = entity.getX() + Math.cos(angle) * radius;
                double pz = entity.getZ() + Math.sin(angle) * radius;
                double py = entity.getY() + 1.0;
                ((ServerWorld) entity.level).sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                        px, py, pz, 1, 0, 0, 0, 0.01);
            }
        }

        // Update discharge position to follow player
        if (this.discharge != null) {
            this.discharge.setPos(entity.getX(), entity.getY() + 1.0, entity.getZ());
        }
    }

    private void endChargeEvent(LivingEntity entity, IAbility ability) {
        if (!entity.level.isClientSide) {
            // Spawn a slash wave projectile in the direction player is looking
            Vector3d look = entity.getLookAngle();
            SlashWaveProjectile slash = new SlashWaveProjectile(entity.level, entity);
            slash.setPos(
                    entity.getX() + look.x * 1.5,
                    entity.getY() + 1.0 + look.y * 1.5,
                    entity.getZ() + look.z * 1.5
            );
            slash.setDeltaMovement(look.x * 2.0, look.y * 2.0, look.z * 2.0);
            entity.level.addFreshEntity(slash);

            // Swing animation
            ((ServerWorld) entity.level).getChunkSource().broadcastAndSend(entity, new SAnimateHandPacket(entity, 0));
        }

        // Let the discharge linger briefly after the slash
        if (this.discharge != null) {
            this.discharge.setAliveTicks(20);
            this.discharge = null;
        }

        entity.level.playSound((PlayerEntity) null, entity.blockPosition(),
                SoundEvents.LIGHTNING_BOLT_IMPACT,
                SoundCategory.PLAYERS, 3.0F, 0.7F);
        entity.level.playSound((PlayerEntity) null, entity.blockPosition(),
                (SoundEvent) ModSounds.DASH_ABILITY_SWOOSH_SFX.get(),
                SoundCategory.PLAYERS, 3.0F, 0.6F);

        this.cooldownComponent.startCooldown(entity, COOLDOWN);
    }

    static {
        INSTANCE = (new AbilityCore.Builder<>("Divine Slayer", AbilityCategory.DEVIL_FRUITS, DivineSlayerAbility::new))
                .addDescriptionLine(DESCRIPTION)
                .addAdvancedDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{
                        AbilityDescriptionLine.NEW_LINE,
                        ChargeComponent.getTooltip(CHARGE_TIME),
                        CooldownComponent.getTooltip(COOLDOWN),
                        DealDamageComponent.getTooltip(DAMAGE),
                        RangeComponent.getTooltip(SLASH_DISTANCE, RangeType.LINE)
                })
                .setSourceHakiNature(SourceHakiNature.IMBUING)
                .build();
    }
}
