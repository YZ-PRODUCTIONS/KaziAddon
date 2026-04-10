package net.kazi.kazimod.abilities.NagiRework;

import net.minecraft.entity.LivingEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectInstance;
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
import xyz.pixelatedw.mineminenomi.api.abilities.components.CooldownComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ContinuousComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.DealDamageComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.HitTriggerComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.HitTriggerComponent.HitResult;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceHakiNature;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceType;
import net.kazi.kazimod.init.KaziEffects;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.init.ModDamageSource;
import xyz.pixelatedw.mineminenomi.init.ModEffects;

public class SilentBoxAbility extends Ability {

    private static final ITextComponent[] DESCRIPTION =
            AbilityHelper.registerDescriptionText("kazimod", "silent_box",
                    new Pair[]{ImmutablePair.of(
                            "The user traps the enemy in a soundproof box, stunning them with blindness and silencing all sound around them.", null)});

    private static final ResourceLocation ICON = new ResourceLocation("kazimod", "textures/abilities/silent_box.png");
    private static final float COOLDOWN = 200.0F;
    private static final float DAMAGE = 10.0F;
    private static final int STUN_DURATION = 120; // 6 seconds

    public static final AbilityCore<SilentBoxAbility> INSTANCE;

    private final ContinuousComponent continuousComponent =
            new ContinuousComponent(this, true)
                    .addEndEvent(this::onToggleOff);

    private final HitTriggerComponent hitTriggerComponent =
            new HitTriggerComponent(this)
                    .addTryHitEvent(100, this::tryHitEvent)
                    .addOnHitEvent(100, this::onHitEvent);

    private final DealDamageComponent dealDamageComponent = new DealDamageComponent(this);

    public SilentBoxAbility(AbilityCore<SilentBoxAbility> core) {
        super(core);
        this.isNew = true;
        this.addComponents(new AbilityComponent[]{
                this.continuousComponent,
                this.hitTriggerComponent,
                this.dealDamageComponent
        });
        this.addUseEvent(this::onUseEvent);
    }

    private void onUseEvent(LivingEntity entity, IAbility ability) {
        if (this.continuousComponent.isContinuous()) {
            this.continuousComponent.stopContinuity(entity);
        } else {
            this.continuousComponent.startContinuity(entity, -1.0F);
        }
    }

    private void onToggleOff(LivingEntity entity, IAbility ability) {
        this.cooldownComponent.startCooldown(entity, COOLDOWN);
    }

    private HitResult tryHitEvent(LivingEntity entity, LivingEntity target, ModDamageSource source, IAbility ability) {
        if (this.continuousComponent.isContinuous()) return HitResult.HIT;
        return HitResult.PASS;
    }

    private boolean onHitEvent(LivingEntity entity, LivingEntity target, ModDamageSource source, IAbility ability) {
        if (!this.continuousComponent.isContinuous()) return false;

        // Black Box effect (visual overlay + movement block, like Kage Kage's Black Box)
        target.addEffect(new EffectInstance(
                (Effect) ModEffects.BLACK_BOX.get(), STUN_DURATION, 1, false, false));
        // Weakened movement (stun) for 6 seconds
        target.addEffect(new EffectInstance(
                (Effect) KaziEffects.WEAKENED_MOVEMENT.get(), STUN_DURATION, 1, false, false));

        // Squid ink particles for dark atmosphere
        if (!target.level.isClientSide) {
            ServerWorld sw = (ServerWorld) target.level;
            for (int i = 0; i < 40; i++) {
                double ox = (target.getRandom().nextDouble() - 0.5) * 2.0;
                double oy = target.getRandom().nextDouble() * 2.0;
                double oz = (target.getRandom().nextDouble() - 0.5) * 2.0;
                sw.sendParticles(ParticleTypes.SQUID_INK,
                        target.getX() + ox, target.getY() + oy, target.getZ() + oz,
                        1, 0, 0, 0, 0.01);
            }
        }

        // Stop the toggle and go on cooldown
        this.continuousComponent.stopContinuity(entity);

        return true;
    }

    static {
        INSTANCE = (new AbilityCore.Builder<>("Silent Box", AbilityCategory.DEVIL_FRUITS, SilentBoxAbility::new))
                .setIcon(ICON)
                .addDescriptionLine(DESCRIPTION)
                .addAdvancedDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{
                        AbilityDescriptionLine.NEW_LINE,
                        CooldownComponent.getTooltip(COOLDOWN),
                        DealDamageComponent.getTooltip(DAMAGE)
                })
                .setSourceType(new SourceType[]{SourceType.FIST})
                .setSourceHakiNature(SourceHakiNature.HARDENING)
                .build();
    }
}
