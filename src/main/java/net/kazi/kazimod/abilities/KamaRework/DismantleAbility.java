package net.kazi.kazimod.abilities.KamaRework;

import java.util.List;
import net.kazi.kazimod.init.KaziParticleEffects;
import net.kazi.kazimod.init.KaziSounds;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectInstance;
import net.minecraft.util.SoundCategory;
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
import xyz.pixelatedw.mineminenomi.api.abilities.components.AbilityComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AnimationComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ChargeComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.CooldownComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.DealDamageComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.RangeComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.RangeComponent.RangeType;
import xyz.pixelatedw.mineminenomi.api.damagesource.AbilityDamageSource;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceElement;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceHakiNature;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceType;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.api.helpers.DevilFruitHelper;
import xyz.pixelatedw.mineminenomi.init.ModAnimations;
import xyz.pixelatedw.mineminenomi.init.ModEffects;
import xyz.pixelatedw.mineminenomi.particles.effects.ParticleEffect;
import xyz.pixelatedw.mineminenomi.wypi.WyHelper;

public class CleaveAbility extends Ability {

    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText(
            "kazimod", "Cleave",
            new Pair[]{ImmutablePair.of("The user slashes an opponent instantly", (Object) null)}
    );

    private static final float COOLDOWN        = 300.0F;
    private static final int   CHARGE_TIME     = 20; // 1 second windup
    private static final int   DISTANCE        = 38;
    private static final float WIDTH           = 3.0F;
    private static final float DAMAGE          = 10.0F;
    private static final int   ANIMATION_TICKS = 10;

    public static final AbilityCore INSTANCE;

    private final ChargeComponent chargeComponent = new ChargeComponent(this)
            .addStartEvent(this::startChargeEvent)
            .addTickEvent(this::duringChargeEvent)
            .addEndEvent(this::endChargeEvent);

    private final AnimationComponent  animationComponent  = new AnimationComponent(this);
    private final RangeComponent      rangeComponent      = new RangeComponent(this);
    private final DealDamageComponent dealDamageComponent = new DealDamageComponent(this);

    public CleaveAbility(AbilityCore core) {
        super(core);
        this.isNew = true;
        this.addComponents(new AbilityComponent[]{
                this.chargeComponent,
                this.animationComponent,
                this.rangeComponent,
                this.dealDamageComponent
        });
        this.addUseEvent(this::onUseEvent);
    }

    private void onUseEvent(LivingEntity entity, IAbility ability) {
        if (!chargeComponent.isCharging()) {
            chargeComponent.startCharging(entity, CHARGE_TIME);
        }
    }

    private void startChargeEvent(LivingEntity entity, IAbility ability) {
        this.animationComponent.start(entity, ModAnimations.UPPER_SLASH, ANIMATION_TICKS);
        if (!entity.level.isClientSide) {
            entity.level.playSound((PlayerEntity) null, entity.blockPosition(),
                    KaziSounds.CLEAVE_START_SFX.get(), SoundCategory.PLAYERS, 1.0F, 1.0F);
        }
    }

    private void duringChargeEvent(LivingEntity entity, IAbility ability) {
    }

    private void endChargeEvent(LivingEntity entity, IAbility ability) {
        animationComponent.stop(entity);

        List targets = this.rangeComponent.getTargetsInLine(entity, (float) DISTANCE, WIDTH);

        for (Object obj : targets) {
            LivingEntity target = (LivingEntity) obj;

            if (!target.hasEffect((Effect) ModEffects.SILENT.get())) {
                AbilityDamageSource source = (AbilityDamageSource) this.dealDamageComponent.getDamageSource(entity);
                source.setInternal();
                source.setSlash();
                source.markIndirectDamage();
                source.setUnavoidable();

                float percentageDamage = target.getMaxHealth() * 0.10F;

                if (this.dealDamageComponent.hurtTarget(entity, target, percentageDamage, source)) {

                    Vector3d dist = target.position()
                            .subtract(entity.position())
                            .add(0.0, -1.0, 0.0)
                            .normalize();
                    double power  = 4.5;
                    double xSpeed = -dist.x * power;
                    double zSpeed = -dist.z * power;
                    AbilityHelper.setDeltaMovement(target, -xSpeed, 0.1, -zSpeed);

                    if (!entity.level.isClientSide) {

                        ((ServerWorld) entity.level).playSound(null, target.blockPosition(),
                                KaziSounds.CLEAVE_HIT_SFX.get(), SoundCategory.PLAYERS, 4.0F, 1.0F);

                        WyHelper.spawnParticleEffect(
                                (ParticleEffect) KaziParticleEffects.CLEAVE.get(),
                                entity,
                                target.getX(),
                                target.getEyeY(),
                                target.getZ()
                        );
                    }

                    break;
                }
            }
        }

        this.cooldownComponent.startCooldown(entity, COOLDOWN);
    }

    static {
        INSTANCE = (new AbilityCore.Builder("Cleave", AbilityCategory.DEVIL_FRUITS, CleaveAbility::new))
                .addDescriptionLine(DESCRIPTION)
                .addAdvancedDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{
                        AbilityDescriptionLine.NEW_LINE,
                        CooldownComponent.getTooltip(COOLDOWN),
                        ChargeComponent.getTooltip(CHARGE_TIME),
                        DealDamageComponent.getTooltip(DAMAGE),
                        RangeComponent.getTooltip((float) DISTANCE, RangeType.AOE)
                })
                .setSourceHakiNature(SourceHakiNature.SPECIAL)
                .setSourceType(new SourceType[]{SourceType.INTERNAL})
                .setSourceElement(SourceElement.SHOCKWAVE)
                .build();
    }
}