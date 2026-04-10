package net.kazi.kazimod.abilities.SakuRework;

import net.kazi.kazimod.entities.projectiles.PetalBladeProjectile;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particles.ParticleType;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectInstance;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
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

public class SenbonzakuraKageyoshiAbility extends Ability {

    private static final ITextComponent[] DESCRIPTION =
            AbilityHelper.registerDescriptionText("kazimod", "senbonzakura_kageyoshi",
                    new Pair[]{ImmutablePair.of(
                            "The user unleashes a massive petal storm, engulfing a huge area in a devastating whirlwind of blade fragments.", null)});

    public static final float DAMAGE_VALUE = 35.0F;
    private static final float DAMAGE = DAMAGE_VALUE;
    private static final float COOLDOWN = 500.0F; // 25 seconds
    private static final float RANGE = 20.0F;
    private static final float CHARGE_TIME = 15.0F; // 0.75 seconds

    public static final AbilityCore<SenbonzakuraKageyoshiAbility> INSTANCE;

    private final ChargeComponent chargeComponent =
            new ChargeComponent(this)
                    .addStartEvent(this::startChargeEvent)
                    .addTickEvent(this::duringChargeEvent)
                    .addEndEvent(this::endChargeEvent);

    private final DealDamageComponent dealDamageComponent = new DealDamageComponent(this);
    private final RangeComponent rangeComponent = new RangeComponent(this);

    public SenbonzakuraKageyoshiAbility(AbilityCore<SenbonzakuraKageyoshiAbility> core) {
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
        entity.addEffect(new EffectInstance((Effect) ModEffects.MOVEMENT_BLOCKED.get(), 2, 0, false, false));
    }

    private void duringChargeEvent(LivingEntity entity, IAbility ability) {
        entity.addEffect(new EffectInstance((Effect) ModEffects.MOVEMENT_BLOCKED.get(), 2, 0, false, false));

        if (!entity.level.isClientSide) {
            for (int i = 0; i < 10; i++) {
                double angle = entity.getRandom().nextDouble() * Math.PI * 2;
                double dist = entity.getRandom().nextDouble() * 3.0;
                double px = entity.getX() + Math.cos(angle) * dist;
                double pz = entity.getZ() + Math.sin(angle) * dist;
                double py = entity.getY() + entity.getRandom().nextDouble() * 3.0;
                SimpleParticleData data = new SimpleParticleData((ParticleType) KaziParticleTypes.PETAL_BLADE.get());
                data.setLife(15);
                data.setSize(3.0F);
                WyHelper.spawnParticles(data, (ServerWorld) entity.level, px, py, pz);
            }
        }
    }

    private void endChargeEvent(LivingEntity entity, IAbility ability) {
        if (!entity.level.isClientSide) {
            // Spawn 30 petal blade projectiles in a massive burst
            for (int i = 0; i < 30; i++) {
                PetalBladeProjectile petal = new PetalBladeProjectile(entity.level, entity, DAMAGE);
                petal.setPos(entity.getX(), entity.getY() + 1.5, entity.getZ());

                double angle = entity.getRandom().nextDouble() * Math.PI * 2;
                double elevAngle = (entity.getRandom().nextDouble() - 0.3) * 0.6;
                double speed = 1.0 + entity.getRandom().nextDouble() * 0.6;
                petal.setDeltaMovement(
                        Math.cos(angle) * speed,
                        elevAngle * speed,
                        Math.sin(angle) * speed
                );
                entity.level.addFreshEntity(petal);
            }

            // Massive petal storm particles
            for (int i = 0; i < 80; i++) {
                double pAngle = entity.getRandom().nextDouble() * Math.PI * 2;
                double dist = entity.getRandom().nextDouble() * RANGE;
                double px = entity.getX() + Math.cos(pAngle) * dist;
                double pz = entity.getZ() + Math.sin(pAngle) * dist;
                double py = entity.getY() + entity.getRandom().nextDouble() * 4.0;
                SimpleParticleData data = new SimpleParticleData((ParticleType) KaziParticleTypes.PETAL_BLADE.get());
                data.setLife(25);
                data.setSize(5.0F);
                WyHelper.spawnParticles(data, (ServerWorld) entity.level, px, py, pz);
            }
        }

        entity.level.playSound((PlayerEntity) null, entity.blockPosition(),
                (SoundEvent) ModSounds.DASH_ABILITY_SWOOSH_SFX.get(),
                SoundCategory.PLAYERS, 3.0F, 0.8F);

        this.cooldownComponent.startCooldown(entity, COOLDOWN);
    }

    static {
        INSTANCE = (new AbilityCore.Builder<>("Senbonzakura Kageyoshi", AbilityCategory.DEVIL_FRUITS, SenbonzakuraKageyoshiAbility::new))
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
