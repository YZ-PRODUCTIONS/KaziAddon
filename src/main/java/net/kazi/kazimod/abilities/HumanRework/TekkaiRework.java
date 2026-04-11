//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package net.kazi.kazimod.abilities.HumanRework;

import net.minecraft.entity.LivingEntity;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectInstance;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import xyz.pixelatedw.mineminenomi.api.abilities.Ability;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCategory;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityDescriptionLine;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityPool2;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AbilityComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AltModeComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AnimationComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ContinuousComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.CooldownComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.PoolComponent;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.data.entity.devilfruit.DevilFruitCapability;
import xyz.pixelatedw.mineminenomi.data.entity.entitystats.EntityStatsCapability;
import xyz.pixelatedw.mineminenomi.data.entity.entitystats.IEntityStats;
import xyz.pixelatedw.mineminenomi.init.ModAbilities;
import xyz.pixelatedw.mineminenomi.init.ModAbilityPools;
import xyz.pixelatedw.mineminenomi.init.ModAnimations;
import xyz.pixelatedw.mineminenomi.init.ModEffects;

public class TekkaiRework extends Ability {
    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText("mineminenomi", "tekkai", new Pair[]{ImmutablePair.of("Hardens the user's body to protect themselves, but they're unable to move", (Object)null), ImmutablePair.of("  §aHEAVY§r immobile, more protection", (Object)null), ImmutablePair.of("  §aWALK§r can move, less protection", (Object)null)});
    private static final ResourceLocation TEKKAI_HEAVY_ICON = new ResourceLocation("mineminenomi", "textures/abilities/tekkai.png");
    private static final ResourceLocation TEKKAI_WALK_ICON = new ResourceLocation("mineminenomi", "textures/abilities/tekkai_walk.png");
    private static final int CONTINUITY_THRESHOLD = 200;
    private static final int MIN_COOLDOWN = 60;
    private static final int MAX_COOLDOWN = 260;
    public static final AbilityCore<TekkaiRework> INSTANCE;
    private final ContinuousComponent continuousComponent = (new ContinuousComponent(this, true)).addStartEvent(100, this::onContinuityStart).addTickEvent(this::onContinuityTick).addEndEvent(this::onContinuityEnd);
    private final AltModeComponent<Mode> altModeComponent;
    private final AnimationComponent animationComponent;
    private final PoolComponent poolComponent;

    public TekkaiRework(AbilityCore<TekkaiRework> core) {
        super(core);
        this.altModeComponent = (new AltModeComponent<>(this, Mode.class, TekkaiRework.Mode.HEAVY)).addChangeModeEvent(this::onAltModeChange);
        this.animationComponent = new AnimationComponent(this);
        this.poolComponent = new PoolComponent(this, ModAbilityPools.TEKKAI_LIKE, new AbilityPool2[0]);
        super.isNew = true;
        super.setDisplayIcon(TEKKAI_HEAVY_ICON);
        super.addComponents(new AbilityComponent[]{this.continuousComponent, this.altModeComponent, this.animationComponent, this.poolComponent});
        super.addUseEvent(this::onUseEvent);
    }

    private void onUseEvent(LivingEntity entity, IAbility ability) {
        if (this.continuousComponent.isContinuous()) {
            this.continuousComponent.stopContinuity(entity);
            return;
        }
        this.continuousComponent.triggerContinuity(entity, (float)CONTINUITY_THRESHOLD);
    }

    private void onContinuityStart(LivingEntity entity, IAbility ability) {
        this.animationComponent.start(entity, ModAnimations.CROSSED_ARMS);
    }

    private void onContinuityTick(LivingEntity entity, IAbility ability) {
        if (this.altModeComponent.isMode(TekkaiRework.Mode.HEAVY)) {
            entity.addEffect(new EffectInstance((Effect)ModEffects.GUARDING.get(), 2, 1, false, false));
            AbilityHelper.setDeltaMovement(entity, (double)0.0F, (double)-5.0F, (double)0.0F);
        } else {
            entity.addEffect(new EffectInstance((Effect)ModEffects.PHYSICAL_MOVING_GUARD.get(), 2, 0, false, false));
        }

    }

    private void onContinuityEnd(LivingEntity entity, IAbility ability) {
        this.animationComponent.stop(entity);
        super.cooldownComponent.startCooldown(entity, this.continuousComponent.getContinueTime() + 60.0F);
    }

    private void onAltModeChange(LivingEntity entity, IAbility ability, Mode mode) {
        if (mode == TekkaiRework.Mode.HEAVY) {
            super.setDisplayIcon(TEKKAI_HEAVY_ICON);
        } else if (mode == TekkaiRework.Mode.WALK) {
            super.setDisplayIcon(TEKKAI_WALK_ICON);
        }

    }

    public void switchToHeavyMode(LivingEntity entity) {
        if (this.continuousComponent.isContinuous() && this.altModeComponent.isMode(TekkaiRework.Mode.HEAVY)) {
            this.continuousComponent.stopContinuity(entity);
            return;
        }
        this.altModeComponent.setMode(entity, TekkaiRework.Mode.HEAVY);
        if (!this.continuousComponent.isContinuous()) {
            this.continuousComponent.triggerContinuity(entity, (float)CONTINUITY_THRESHOLD);
        }
    }

    public void switchToWalkMode(LivingEntity entity) {
        if (this.continuousComponent.isContinuous() && this.altModeComponent.isMode(TekkaiRework.Mode.WALK)) {
            this.continuousComponent.stopContinuity(entity);
            return;
        }
        this.altModeComponent.setMode(entity, TekkaiRework.Mode.WALK);
        if (!this.continuousComponent.isContinuous()) {
            this.continuousComponent.triggerContinuity(entity, (float)CONTINUITY_THRESHOLD);
        }
    }

    private static boolean canUnlock(LivingEntity user) {
        IEntityStats props = EntityStatsCapability.get(user);
        boolean raceCheck = props.isHuman() || DevilFruitCapability.get(user).hasDevilFruit(ModAbilities.HITO_HITO_NO_MI);
        return raceCheck && props.getDoriki() >= (double)1000.0F;
    }

    static {
        INSTANCE = (new AbilityCore.Builder("Tekkai", AbilityCategory.RACIAL, TekkaiRework::new)).addDescriptionLine(DESCRIPTION).addAdvancedDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{AbilityDescriptionLine.NEW_LINE, CooldownComponent.getTooltip(60.0F, 260.0F), ContinuousComponent.getTooltip(200.0F)}).setUnlockCheck(TekkaiRework::canUnlock).build();
    }

    public static enum Mode {
        HEAVY,
        WALK;

        private Mode() {
        }
    }
}
