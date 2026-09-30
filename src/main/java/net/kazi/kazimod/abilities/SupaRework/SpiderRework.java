package net.kazi.kazimod.abilities.SupaRework;

import net.minecraft.util.ResourceLocation;
import java.awt.Color;
import net.minecraft.entity.LivingEntity;
import net.minecraft.potion.EffectInstance;
import net.minecraft.util.text.StringTextComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.*;
import xyz.pixelatedw.mineminenomi.api.abilities.components.*;
import xyz.pixelatedw.mineminenomi.init.ModAbilityPools;
import xyz.pixelatedw.mineminenomi.init.ModAnimations;
import xyz.pixelatedw.mineminenomi.init.ModEffects;

/** Kazi's standalone copy of Spider, retaining the original defensive mechanics. */
public final class SpiderRework extends Ability {
    private static final float HOLD_TIME = 100.0F;
    public static final AbilityCore<SpiderRework> INSTANCE = new AbilityCore.Builder<SpiderRework>(
            "Spider", AbilityCategory.DEVIL_FRUITS, SpiderRework::new)
            .setIcon(new ResourceLocation("mineminenomi", "textures/abilities/spider.png"))
            .addDescriptionLine(new StringTextComponent("Hardens the user's body to protect themselves, but they're unable to move"))
            .addAdvancedDescriptionLine(AbilityDescriptionLine.NEW_LINE,
                    CooldownComponent.getTooltip(0.0F, HOLD_TIME * 2.25F),
                    ContinuousComponent.getTooltip(HOLD_TIME)).build();

    private static final AbilityOverlay OVERLAY = new AbilityOverlay.Builder()
            .setColor(new Color(100, 100, 100, 70)).build();
    public final ContinuousComponent continuousComponent = new ContinuousComponent(this)
            .addStartEvent(this::startContinuityEvent)
            .addTickEvent(this::duringContinuityEvent)
            .addEndEvent(this::onContinuityStops);
    public final PoolComponent poolComponent = new PoolComponent(this, ModAbilityPools.TEKKAI_LIKE);
    public final SkinOverlayComponent skinOverlayComponent = new SkinOverlayComponent(this, OVERLAY);
    public final AnimationComponent animationComponent = new AnimationComponent(this);

    public SpiderRework(AbilityCore<SpiderRework> core) {
        super(core);
        this.isNew = true;
        addComponents(continuousComponent, poolComponent, skinOverlayComponent, animationComponent);
        addUseEvent((entity, ability) -> continuousComponent.triggerContinuity(entity, HOLD_TIME));
    }

    private void startContinuityEvent(LivingEntity entity, IAbility ability) {
        animationComponent.start(entity, ModAnimations.CROSSED_ARMS);
        skinOverlayComponent.showAll(entity);
    }

    private void duringContinuityEvent(LivingEntity entity, IAbility ability) {
        entity.addEffect(new EffectInstance(ModEffects.GUARDING.get(), 2, 3, false, false));
    }

    private void onContinuityStops(LivingEntity entity, IAbility ability) {
        float heldTicks = Math.max(0.0F, Math.min(HOLD_TIME, continuousComponent.getContinueTime()));
        animationComponent.stop(entity);
        cooldownComponent.startCooldown(entity, heldTicks * 2.25F);
        skinOverlayComponent.hideAll(entity);
    }
}
