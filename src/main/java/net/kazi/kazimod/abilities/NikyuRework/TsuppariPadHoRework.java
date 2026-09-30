//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package net.kazi.kazimod.abilities.NikyuRework;

import net.kazi.kazimod.entities.projectiles.PadHoReworkProjectile;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectInstance;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.text.ITextComponent;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import xyz.pixelatedw.mineminenomi.api.abilities.Ability;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCategory;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityDescriptionLine;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AbilityComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AnimationComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ContinuousComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.CooldownComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.HitTrackerComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ProjectileComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.RepeaterComponent;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceElement;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceHakiNature;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.entities.projectiles.nikyu.PadHoProjectile;
import xyz.pixelatedw.mineminenomi.init.ModAnimations;
import xyz.pixelatedw.mineminenomi.init.ModEffects;
import xyz.pixelatedw.mineminenomi.init.ModSounds;

public class TsuppariPadHoRework extends Ability {
    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText("mineminenomi", "tsuppari_pad_ho", new Pair[]{ImmutablePair.of("Launches a barrage of paw-shaped shockwaves at the opponent", (Object)null)});
    private static final int COOLDOWN = 300;
    public static final AbilityCore<TsuppariPadHoRework> INSTANCE;
    private final ContinuousComponent continuousComponent = (new ContinuousComponent(this)).addStartEvent(this::startContinuityEvent).addTickEvent(this::duringContinuityEvent).addEndEvent(this::endContinuityEvent);
    private final HitTrackerComponent hitTrackerComponent = new HitTrackerComponent(this);
    private final RepeaterComponent repeaterComponent = (new RepeaterComponent(this)).addTriggerEvent(this::triggerRepeaterEvent).addStopEvent(this::stopRepeaterEvent);
    private final AnimationComponent animationComponent = new AnimationComponent(this);
    private final ProjectileComponent projectileComponent = new ProjectileComponent(this, this::createProjectile);

    public TsuppariPadHoRework(AbilityCore<TsuppariPadHoRework> core) {
        super(core);
        this.isNew = true;
        this.addComponents(new AbilityComponent[]{this.continuousComponent, this.hitTrackerComponent, this.repeaterComponent, this.animationComponent, this.projectileComponent});
        this.addUseEvent(this::useEvent);
    }

    private void useEvent(LivingEntity entity, IAbility ability) {
        this.continuousComponent.triggerContinuity(entity);
    }

    private void startContinuityEvent(LivingEntity entity, IAbility ability) {
        this.repeaterComponent.start(entity, 5, 4);
        this.animationComponent.start(entity, ModAnimations.PUNCH_RUSH);
        entity.level.playSound((PlayerEntity)null, entity.blockPosition(), (SoundEvent)ModSounds.MULTIPLE_PAD_HO_SFX.get(), SoundCategory.PLAYERS, 2.0F, 1.0F);
    }

    private void duringContinuityEvent(LivingEntity entity, IAbility ability) {
    }

    private void triggerRepeaterEvent(LivingEntity entity, IAbility ability) {
        for(int i = 0; i < 3; ++i) {
            this.projectileComponent.shoot(entity, 3.0F, 15.0F);
        }

    }

    private void stopRepeaterEvent(LivingEntity entity, IAbility ability) {
        this.continuousComponent.stopContinuity(entity);
    }

    private void endContinuityEvent(LivingEntity entity, IAbility ability) {
        this.cooldownComponent.startCooldown(entity, 300.0F);
        this.animationComponent.stop(entity);
    }

    private PadHoReworkProjectile createProjectile(LivingEntity entity) {
        PadHoReworkProjectile proj = new PadHoReworkProjectile(entity.level, entity, this);
        proj.setDamage(5.0F);
        return proj;
    }

    static {
        INSTANCE = (new AbilityCore.Builder("Tsuppari Pad Ho", AbilityCategory.DEVIL_FRUITS, TsuppariPadHoRework::new)).addDescriptionLine(DESCRIPTION).addAdvancedDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{AbilityDescriptionLine.NEW_LINE, CooldownComponent.getTooltip(300.0F)}).setSourceHakiNature(SourceHakiNature.SPECIAL).setSourceElement(SourceElement.SHOCKWAVE).build();
    }
}

