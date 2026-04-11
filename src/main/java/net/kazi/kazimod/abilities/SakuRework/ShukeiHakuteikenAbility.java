package net.kazi.kazimod.abilities.SakuRework;

import java.util.List;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.server.SAnimateHandPacket;
import net.minecraft.particles.ParticleType;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectInstance;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.BlockPos;
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
import xyz.pixelatedw.mineminenomi.api.abilities.components.RangeComponent.RangeType;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceHakiNature;
import xyz.pixelatedw.mineminenomi.api.damagesource.AbilityDamageSource;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.init.ModDamageSource;
import xyz.pixelatedw.mineminenomi.init.ModEffects;
import net.kazi.kazimod.init.KaziParticleTypes;
import xyz.pixelatedw.mineminenomi.init.ModSounds;
import xyz.pixelatedw.mineminenomi.particles.data.SimpleParticleData;
import xyz.pixelatedw.mineminenomi.wypi.WyHelper;

public class ShukeiHakuteikenAbility extends Ability {

    private static final ITextComponent[] DESCRIPTION =
            AbilityHelper.registerDescriptionText("kazimod", "shukei_hakuteiken",
                    new Pair[]{ImmutablePair.of(
                            "The user condenses all petals into a single devastating blade of pure white energy, dashing forward with an ultimate strike.", null)});

    private static final float DAMAGE = 80.0F;
    private static final float COOLDOWN = 1200.0F; // 60 seconds
    private static final float CHARGE_TIME = 30.0F; // 1.5 seconds
    private static final float DASH_DISTANCE = 15.0F;
    private static final float RANGE = 3.0F;

    public static final AbilityCore<ShukeiHakuteikenAbility> INSTANCE;

    private final ChargeComponent chargeComponent =
            new ChargeComponent(this)
                    .addStartEvent(this::startChargeEvent)
                    .addTickEvent(this::duringChargeEvent)
                    .addEndEvent(this::endChargeEvent);

    private final DealDamageComponent dealDamageComponent = new DealDamageComponent(this);
    private final RangeComponent rangeComponent = new RangeComponent(this);
    private final HitTrackerComponent hitTrackerComponent = new HitTrackerComponent(this);

    public ShukeiHakuteikenAbility(AbilityCore<ShukeiHakuteikenAbility> core) {
        super(core);
        this.isNew = true;

        this.addComponents(new AbilityComponent[]{
                this.chargeComponent,
                this.dealDamageComponent,
                this.rangeComponent,
                this.hitTrackerComponent
        });

        this.addUseEvent(this::onUseEvent);
    }

    private void onUseEvent(LivingEntity entity, IAbility ability) {
        if (!this.chargeComponent.isCharging()) {
            this.chargeComponent.startCharging(entity, CHARGE_TIME);
        }
    }

    private void startChargeEvent(LivingEntity entity, IAbility ability) {
        this.hitTrackerComponent.clearHits();

        entity.level.playSound((PlayerEntity) null, entity.blockPosition(),
                (SoundEvent) ModSounds.DASH_ABILITY_SWOOSH_SFX.get(),
                SoundCategory.PLAYERS, 2.0F, 0.5F);
    }

    private void duringChargeEvent(LivingEntity entity, IAbility ability) {
        entity.addEffect(new EffectInstance((Effect) ModEffects.MOVEMENT_BLOCKED.get(), 2, 0, false, false));

        // Petals converging into the sword
        if (!entity.level.isClientSide) {
            for (int i = 0; i < 12; i++) {
                double angle = (entity.tickCount * 0.3 + i * 30) % (Math.PI * 2);
                double shrinkRadius = 4.0 - (this.chargeComponent.getChargeTime() / CHARGE_TIME) * 3.0;
                if (shrinkRadius < 0.5) shrinkRadius = 0.5;
                double px = entity.getX() + Math.cos(angle) * shrinkRadius;
                double pz = entity.getZ() + Math.sin(angle) * shrinkRadius;
                double py = entity.getY() + 1.0 + Math.sin(entity.tickCount * 0.2 + i) * 0.3;
                SimpleParticleData data = new SimpleParticleData((ParticleType) KaziParticleTypes.PETAL_BLADE.get());
                data.setLife(15);
                data.setSize(3.0F);
                WyHelper.spawnParticles(data, (ServerWorld) entity.level, px, py, pz);
            }
        }
    }

    private void endChargeEvent(LivingEntity entity, IAbility ability) {
        BlockPos.Mutable blockPos = WyHelper.rayTraceBlockSafe(entity, DASH_DISTANCE).mutable();

        AbilityDamageSource source = (AbilityDamageSource) ((ModDamageSource) this.dealDamageComponent.getDamageSource(entity)).setSlash();
        source.setUnavoidable();

        List<LivingEntity> targets = this.rangeComponent.getTargetsInLine(entity, DASH_DISTANCE, RANGE);

        for (LivingEntity target : targets) {
            if (this.hitTrackerComponent.canHit(target)) {
                this.dealDamageComponent.hurtTarget(entity, target, DAMAGE, source);

                if (!entity.level.isClientSide) {
                    for (int i = 0; i < 40; i++) {
                        double offsetX = (entity.getRandom().nextDouble() - 0.5) * 2.0;
                        double offsetY = entity.getRandom().nextDouble() * 2.0;
                        double offsetZ = (entity.getRandom().nextDouble() - 0.5) * 2.0;
                        SimpleParticleData data = new SimpleParticleData((ParticleType) KaziParticleTypes.PETAL_BLADE.get());
                        data.setLife(25);
                        data.setSize(5.0F);
                        WyHelper.spawnParticles(data, (ServerWorld) entity.level,
                                target.getX() + offsetX, target.getY() + 1.0 + offsetY, target.getZ() + offsetZ);
                    }
                    WyHelper.spawnParticles(ParticleTypes.END_ROD, (ServerWorld) entity.level,
                            target.getX(), target.getY() + (double) target.getEyeHeight(), target.getZ());
                }
            }
        }

        // Teleport to destination
        entity.stopRiding();
        entity.teleportToWithTicket((double) blockPos.getX(), (double) blockPos.getY(), (double) blockPos.getZ());

        if (!entity.level.isClientSide) {
            ((ServerWorld) entity.level).getChunkSource().broadcastAndSend(entity, new SAnimateHandPacket(entity, 0));
        }

        // Trail of petals along the dash path
        if (!entity.level.isClientSide) {
            Vector3d start = entity.position();
            for (int i = 0; i < 30; i++) {
                double t = i / 30.0;
                double trailX = start.x + (blockPos.getX() - start.x) * t;
                double trailY = start.y + (blockPos.getY() - start.y) * t + 1.0;
                double trailZ = start.z + (blockPos.getZ() - start.z) * t;
                SimpleParticleData data = new SimpleParticleData((ParticleType) KaziParticleTypes.PETAL_BLADE.get());
                data.setLife(20);
                data.setSize(4.0F);
                WyHelper.spawnParticles(data, (ServerWorld) entity.level, trailX, trailY, trailZ);
            }
        }

        entity.level.playSound((PlayerEntity) null, entity.blockPosition(),
                SoundEvents.LIGHTNING_BOLT_IMPACT,
                SoundCategory.PLAYERS, 2.0F, 1.5F);
        entity.level.playSound((PlayerEntity) null, entity.blockPosition(),
                (SoundEvent) ModSounds.DASH_ABILITY_SWOOSH_SFX.get(),
                SoundCategory.PLAYERS, 3.0F, 0.6F);

        this.cooldownComponent.startCooldown(entity, COOLDOWN);
    }

    static {
        INSTANCE = (new AbilityCore.Builder<>("Shukei Hakuteiken", AbilityCategory.DEVIL_FRUITS, ShukeiHakuteikenAbility::new))
                .addDescriptionLine(DESCRIPTION)
                .addAdvancedDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{
                        AbilityDescriptionLine.NEW_LINE,
                        ChargeComponent.getTooltip(CHARGE_TIME),
                        CooldownComponent.getTooltip(COOLDOWN),
                        DealDamageComponent.getTooltip(DAMAGE),
                        RangeComponent.getTooltip(DASH_DISTANCE, RangeType.LINE)
                })
                .setSourceHakiNature(SourceHakiNature.IMBUING)
                .build();
    }
}
