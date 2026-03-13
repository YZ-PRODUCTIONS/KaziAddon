package net.kazi.kazimod.abilities.KamaRework;

import java.util.List;
import net.kazi.kazimod.events.components.DashComboComponent;
import net.kazi.kazimod.init.KaziParticleEffects;
import net.kazi.kazimod.init.KaziSounds;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.potion.Effect;
import net.minecraft.util.ResourceLocation;
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
import xyz.pixelatedw.mineminenomi.api.abilities.components.AltModeComponent;
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
import xyz.pixelatedw.mineminenomi.init.ModAnimations;
import xyz.pixelatedw.mineminenomi.init.ModEffects;
import xyz.pixelatedw.mineminenomi.particles.effects.ParticleEffect;
import xyz.pixelatedw.mineminenomi.wypi.WyHelper;

public class DismantleAbility extends Ability {

    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText(
            "kazimod", "dismantle",
            new Pair[]{
                    ImmutablePair.of("The user slashes an opponent instantly", (Object) null)
            }
    );

    private static final float NORMAL_COOLDOWN  = 200.0F;
    private static final float BARRAGE_COOLDOWN = 600.0F; // 30 seconds
    private static final int   CHARGE_TIME      = 20;     // barrage only
    private static final int   DISTANCE         = 38;
    private static final float WIDTH            = 3.0F;
    private static final float DAMAGE           = 10.0F;
    private static final int   ANIMATION_TICKS  = 10;

    private static final int BARRAGE_COUNT = 5;
    private static final int BARRAGE_DELAY = 10; // 0.5 seconds between slashes


    public static final AbilityCore INSTANCE;

    // Counts how many barrage slashes have fired so we know when to apply cooldown
    private int barrageHitsFired = 0;

    private final ChargeComponent chargeComponent = new ChargeComponent(this)
            .addStartEvent(this::startChargeEvent)
            .addTickEvent(this::duringChargeEvent)
            .addEndEvent(this::endChargeEvent);

    private final AltModeComponent<Mode> altModeComponent;
    private final AnimationComponent     animationComponent  = new AnimationComponent(this);
    private final RangeComponent         rangeComponent      = new RangeComponent(this);
    private final DealDamageComponent    dealDamageComponent = new DealDamageComponent(this);
    private final DashComboComponent     dashComboComponent  = new DashComboComponent(this, this::executeSlash);

    public DismantleAbility(AbilityCore core) {
        super(core);
        this.altModeComponent = (new AltModeComponent<>(this, Mode.class, Mode.NORMAL))
                .addChangeModeEvent(this::onAltModeChange);
        this.isNew = true;
        this.addComponents(new AbilityComponent[]{
                this.chargeComponent,
                this.altModeComponent,
                this.animationComponent,
                this.rangeComponent,
                this.dealDamageComponent,
                this.dashComboComponent
        });
        this.addUseEvent(this::onUseEvent);
        this.addTickEvent(this::onTickEvent);
    }

    private void onTickEvent(LivingEntity entity, IAbility ability) {
        if (dashComboComponent.isActive()) {
            dashComboComponent.tick(ability);
        }
    }

    private void onUseEvent(LivingEntity entity, IAbility ability) {
        if (chargeComponent.isCharging() || dashComboComponent.isActive()) return;

        if (altModeComponent.isMode(Mode.BARRAGE)) {
            // Barrage: start charge windup first
            chargeComponent.startCharging(entity, CHARGE_TIME);
        } else {
            // Normal: fire instantly with no windup
            if (!entity.level.isClientSide) {
                entity.level.playSound((PlayerEntity) null, entity.blockPosition(),
                        KaziSounds.DISMANTLE_SFX.get(), SoundCategory.PLAYERS, 1.0F, 1.0F);
            }
            executeSlash(entity, ability);
            this.cooldownComponent.startCooldown(entity, NORMAL_COOLDOWN);
        }
    }

    // -------------------------------------------------------------------------
    // Charge events — only reached in BARRAGE mode
    // -------------------------------------------------------------------------
    private void startChargeEvent(LivingEntity entity, IAbility ability) {
        this.animationComponent.start(entity, ModAnimations.UPPER_SLASH, ANIMATION_TICKS);
        if (!entity.level.isClientSide) {
            entity.level.playSound((PlayerEntity) null, entity.blockPosition(),
                    KaziSounds.DISMANTLE_SFX.get(), SoundCategory.PLAYERS, 1.0F, 1.0F);
        }
    }

    private void duringChargeEvent(LivingEntity entity, IAbility ability) {}

    private void endChargeEvent(LivingEntity entity, IAbility ability) {
        animationComponent.stop(entity);
        barrageHitsFired = 0;
        dashComboComponent.startCombo(entity, BARRAGE_COUNT, BARRAGE_DELAY);
    }

    // -------------------------------------------------------------------------
    // Core slash logic — shared between normal and barrage modes
    // -------------------------------------------------------------------------
    private void executeSlash(LivingEntity entity, IAbility ability) {
        boolean isBarrage = altModeComponent.isMode(Mode.BARRAGE);
        float percentageDamage = entity.getMaxHealth() * (isBarrage ? 0.05F : 0.20F);

        List targets = this.rangeComponent.getTargetsInLine(entity, (float) DISTANCE, WIDTH);

        for (Object obj : targets) {
            LivingEntity target = (LivingEntity) obj;

            if (!target.hasEffect((Effect) ModEffects.SILENT.get())) {
                AbilityDamageSource source =
                        (AbilityDamageSource) this.dealDamageComponent.getDamageSource(entity);
                source.setInternal();
                source.setSlash();
                source.markIndirectDamage();
                source.setUnavoidable();
                source.bypassArmor();

                if (this.dealDamageComponent.hurtTarget(entity, target, percentageDamage, source)) {
                    Vector3d dist = target.position()
                            .subtract(entity.position())
                            .add(0.0, -1.0, 0.0)
                            .normalize();
                    double power  = isBarrage ? 1.5 : 4.5;
                    double xSpeed = -dist.x * power;
                    double zSpeed = -dist.z * power;
                    AbilityHelper.setDeltaMovement(target, -xSpeed, 0.1, -zSpeed);

                    if (!entity.level.isClientSide) {
                        ((ServerWorld) entity.level).playSound(null, target.blockPosition(),
                                KaziSounds.CLEAVE_HIT_SFX.get(), SoundCategory.PLAYERS, 4.0F, 1.0F);
                        WyHelper.spawnParticleEffect(
                                (ParticleEffect) KaziParticleEffects.DISMANTLE.get(),
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

        // Count each barrage slash; once all 5 are done, start the 30s cooldown
        if (isBarrage) {
            barrageHitsFired++;
            if (barrageHitsFired >= BARRAGE_COUNT) {
                barrageHitsFired = 0;
                this.cooldownComponent.startCooldown(entity, BARRAGE_COOLDOWN);
            }
        }
    }

    private void onAltModeChange(LivingEntity entity, IAbility ability, Mode mode) {
        super.setDisplayIcon(INSTANCE);
    }

    static {
        INSTANCE = (new AbilityCore.Builder("Dismantle", AbilityCategory.DEVIL_FRUITS, DismantleAbility::new))
                .addDescriptionLine(DESCRIPTION)
                .addAdvancedDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{
                        AbilityDescriptionLine.NEW_LINE,
                        CooldownComponent.getTooltip(NORMAL_COOLDOWN, BARRAGE_COOLDOWN),
                        ChargeComponent.getTooltip(CHARGE_TIME),
                        DealDamageComponent.getTooltip(DAMAGE),
                        RangeComponent.getTooltip((float) DISTANCE, RangeType.AOE)
                })
                .setSourceHakiNature(SourceHakiNature.SPECIAL)
                .setSourceType(new SourceType[]{SourceType.INTERNAL})
                .setSourceElement(SourceElement.SHOCKWAVE)
                .build();
    }

    public enum Mode {
        NORMAL,
        BARRAGE;

        private Mode() {}
    }
}