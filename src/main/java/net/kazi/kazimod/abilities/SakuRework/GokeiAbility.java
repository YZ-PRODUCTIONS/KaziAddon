package net.kazi.kazimod.abilities.SakuRework;

import java.util.List;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particles.ParticleType;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectInstance;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
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
import net.kazi.kazimod.init.KaziParticleTypes;
import xyz.pixelatedw.mineminenomi.init.ModSounds;
import xyz.pixelatedw.mineminenomi.particles.data.SimpleParticleData;
import xyz.pixelatedw.mineminenomi.wypi.WyHelper;

public class GokeiAbility extends Ability {

    private static final ITextComponent[] DESCRIPTION =
            AbilityHelper.registerDescriptionText("kazimod", "gokei",
                    new Pair[]{ImmutablePair.of(
                            "The user surrounds a target in a sphere of petal blades, crushing them with devastating force.", null)});

    private static final float DAMAGE = 55.0F;
    private static final float COOLDOWN = 700.0F; // 35 seconds
    private static final float RANGE = 12.0F;
    private static final float CHARGE_TIME = 10.0F; // 0.5 seconds

    public static final AbilityCore<GokeiAbility> INSTANCE;

    private final ChargeComponent chargeComponent =
            new ChargeComponent(this)
                    .addStartEvent(this::startChargeEvent)
                    .addTickEvent(this::duringChargeEvent)
                    .addEndEvent(this::endChargeEvent);

    private final DealDamageComponent dealDamageComponent = new DealDamageComponent(this);
    private final RangeComponent rangeComponent = new RangeComponent(this);
    private final HitTrackerComponent hitTrackerComponent = new HitTrackerComponent(this);

    private LivingEntity lockedTarget = null;

    public GokeiAbility(AbilityCore<GokeiAbility> core) {
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
            List<LivingEntity> targets = this.rangeComponent.getTargetsInArea(entity, RANGE);
            if (!targets.isEmpty()) {
                targets.sort((a, b) -> Double.compare(a.distanceTo(entity), b.distanceTo(entity)));
                this.lockedTarget = targets.get(0);
                this.chargeComponent.startCharging(entity, CHARGE_TIME);
            }
        }
    }

    private void startChargeEvent(LivingEntity entity, IAbility ability) {
        this.hitTrackerComponent.clearHits();
    }

    private void duringChargeEvent(LivingEntity entity, IAbility ability) {
        entity.addEffect(new EffectInstance((Effect) ModEffects.MOVEMENT_BLOCKED.get(), 2, 0, false, false));

        if (this.lockedTarget != null && this.lockedTarget.isAlive() && !entity.level.isClientSide) {
            this.lockedTarget.addEffect(new EffectInstance((Effect) ModEffects.MOVEMENT_BLOCKED.get(), 2, 0, false, false));

            for (int i = 0; i < 15; i++) {
                double theta = entity.getRandom().nextDouble() * Math.PI * 2;
                double phi = entity.getRandom().nextDouble() * Math.PI;
                double radius = 2.0 + entity.getRandom().nextDouble();
                double px = this.lockedTarget.getX() + Math.sin(phi) * Math.cos(theta) * radius;
                double py = this.lockedTarget.getY() + 1.0 + Math.cos(phi) * radius;
                double pz = this.lockedTarget.getZ() + Math.sin(phi) * Math.sin(theta) * radius;
                SimpleParticleData data = new SimpleParticleData((ParticleType) KaziParticleTypes.PETAL_BLADE.get());
                data.setLife(15);
                data.setSize(3.0F);
                WyHelper.spawnParticles(data, (ServerWorld) entity.level, px, py, pz);
            }
        }
    }

    private void endChargeEvent(LivingEntity entity, IAbility ability) {
        if (this.lockedTarget != null && this.lockedTarget.isAlive()) {
            this.dealDamageComponent.hurtTarget(entity, this.lockedTarget, DAMAGE);

            if (!entity.level.isClientSide) {
                // Implosion particles
                for (int i = 0; i < 50; i++) {
                    double theta = entity.getRandom().nextDouble() * Math.PI * 2;
                    double phi = entity.getRandom().nextDouble() * Math.PI;
                    double radius = 3.0;
                    double px = this.lockedTarget.getX() + Math.sin(phi) * Math.cos(theta) * radius;
                    double py = this.lockedTarget.getY() + 1.0 + Math.cos(phi) * radius;
                    double pz = this.lockedTarget.getZ() + Math.sin(phi) * Math.sin(theta) * radius;
                    SimpleParticleData data = new SimpleParticleData((ParticleType) KaziParticleTypes.PETAL_BLADE.get());
                    data.setLife(20);
                    data.setSize(4.0F);
                    WyHelper.spawnParticles(data, (ServerWorld) entity.level, px, py, pz);
                }

                // Explosion at impact
                for (int i = 0; i < 30; i++) {
                    double offsetX = (entity.getRandom().nextDouble() - 0.5) * 3.0;
                    double offsetY = entity.getRandom().nextDouble() * 3.0;
                    double offsetZ = (entity.getRandom().nextDouble() - 0.5) * 3.0;
                    SimpleParticleData data = new SimpleParticleData((ParticleType) KaziParticleTypes.PETAL_BLADE.get());
                    data.setLife(25);
                    data.setSize(5.0F);
                    WyHelper.spawnParticles(data, (ServerWorld) entity.level,
                            this.lockedTarget.getX() + offsetX, this.lockedTarget.getY() + 1.0 + offsetY, this.lockedTarget.getZ() + offsetZ);
                }
            }

            entity.level.playSound((PlayerEntity) null, this.lockedTarget.blockPosition(),
                    (SoundEvent) ModSounds.DASH_ABILITY_SWOOSH_SFX.get(),
                    SoundCategory.PLAYERS, 3.0F, 0.5F);
        }

        this.lockedTarget = null;
        this.cooldownComponent.startCooldown(entity, COOLDOWN);
    }

    static {
        INSTANCE = (new AbilityCore.Builder<>("Gokei", AbilityCategory.DEVIL_FRUITS, GokeiAbility::new))
                .addDescriptionLine(DESCRIPTION)
                .addAdvancedDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{
                        AbilityDescriptionLine.NEW_LINE,
                        ChargeComponent.getTooltip(CHARGE_TIME),
                        CooldownComponent.getTooltip(COOLDOWN),
                        DealDamageComponent.getTooltip(DAMAGE),
                        RangeComponent.getTooltip(RANGE, RangeType.AOE)
                })
                .setSourceHakiNature(SourceHakiNature.IMBUING)
                .build();
    }
}
