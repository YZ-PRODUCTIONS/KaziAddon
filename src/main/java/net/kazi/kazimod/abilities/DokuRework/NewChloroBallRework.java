package net.kazi.kazimod.abilities.DokuRework;

import net.MrMagicalCart.cartaddon.init.CartMorphs;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import xyz.pixelatedw.mineminenomi.api.abilities.Ability;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCategory;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityDescriptionLine;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AbilityComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AltModeComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ContinuousComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.CooldownComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ProjectileComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.RepeaterComponent;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceElement;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceHakiNature;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.api.morph.MorphInfo;
import xyz.pixelatedw.mineminenomi.entities.projectiles.doku.ChloroBallProjectile;
import xyz.pixelatedw.mineminenomi.wypi.WyRegistry;

public class NewChloroBallRework
extends Ability {
    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText((String)"kazimod", (String)"chloro_ball", (Pair[])new Pair[]{ImmutablePair.of((Object)"The user spits a bubble made of poison towards the enemy, which also leaves poison on the ground.", null)});
    private static final ITextComponent NORMAL_NAME = new TranslationTextComponent(WyRegistry.registerName((String)"ability.mineminenomi.chloro_ball", (String)"Chloro Ball"));
    private static final ITextComponent VENOM_NAME = new TranslationTextComponent(WyRegistry.registerName((String)"ability.mineminenomi.chloro_ball_venom", (String)"Venom Chloro Ball"));
    private static final ResourceLocation NORMAL_ICON = new ResourceLocation("mineminenomi", "textures/abilities/chloro_ball.png");
    private static final ResourceLocation VENOM_ICON = new ResourceLocation("mineminenomi", "textures/abilities/chloro_ball_venom.png");
    private static final int COOLDOWN = 300;
    public static final AbilityCore<NewChloroBallRework> INSTANCE = new AbilityCore.Builder<NewChloroBallRework>("Chloro Ball", AbilityCategory.DEVIL_FRUITS, NewChloroBallRework::new).addDescriptionLine(DESCRIPTION).addAdvancedDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{AbilityDescriptionLine.NEW_LINE, CooldownComponent.getTooltip((float)240.0f)}).addAdvancedDescriptionLine(ProjectileComponent.getProjectileTooltips()).setSourceHakiNature(SourceHakiNature.SPECIAL).setSourceElement(SourceElement.POISON).build();
    private final ContinuousComponent continuousComponent = new ContinuousComponent((IAbility)this).addStartEvent(this::startContinuityEvent).addEndEvent(this::endContinuityEvent);
    private final RepeaterComponent repeaterComponent = new RepeaterComponent((IAbility)this).addTriggerEvent(this::triggerRepeaterEvent).addStopEvent(this::endRepeaterEvent);
    private final ProjectileComponent projectileComponent = new ProjectileComponent((IAbility)this, this::createProjectile);
    private final AltModeComponent<Mode> altModeComponent = new AltModeComponent<Mode>((IAbility)this, Mode.class, Mode.NORMAL, true).addChangeModeEvent(this::onAltModeChange);

    public NewChloroBallRework(AbilityCore<NewChloroBallRework> core) {
        super(core);
        this.isNew = true;
        this.addComponents(new AbilityComponent[]{this.continuousComponent, this.repeaterComponent, this.projectileComponent, this.altModeComponent});
        this.addUseEvent(this::useEvent);
    }

    private void useEvent(LivingEntity entity, IAbility ability) {
        this.continuousComponent.triggerContinuity(entity);
    }

    private void startContinuityEvent(LivingEntity entity, IAbility ability) {
        boolean isDemonForm = ((MorphInfo)CartMorphs.VENOM_DEMON2.get()).isActive(entity);
        if (isDemonForm) {
            this.repeaterComponent.start(entity, 1, 5);
        } else {
            this.repeaterComponent.start(entity, 1, 5);
        }
    }

    private void triggerRepeaterEvent(LivingEntity entity, IAbility ability) {
        this.projectileComponent.shoot(entity, 2.0f, 1.0f);
    }

    private void endRepeaterEvent(LivingEntity entity, IAbility ability) {
        this.continuousComponent.stopContinuity(entity);
    }

    private void endContinuityEvent(LivingEntity entity, IAbility ability) {
        this.cooldownComponent.startCooldown(entity, 240.0f);
    }

    private ChloroBallProjectile createProjectile(LivingEntity entity) {
        boolean isDemonForm = ((MorphInfo)CartMorphs.VENOM_DEMON2.get()).isActive(entity);
        ChloroBallProjectile proj = new ChloroBallProjectile(entity.level, entity, isDemonForm);
        return proj;
    }

    public void setNormalMode(LivingEntity entity) {
        this.altModeComponent.setMode(entity, Mode.NORMAL);
    }

    public void setVenomMode(LivingEntity entity) {
        this.altModeComponent.setMode(entity, Mode.VENOM);
    }

    private void onAltModeChange(LivingEntity entity, IAbility ability, Mode mode) {
        if (mode == Mode.VENOM) {
            super.setDisplayName(VENOM_NAME);
            super.setDisplayIcon(VENOM_ICON);
        } else if (mode == Mode.NORMAL) {
            super.setDisplayName(NORMAL_NAME);
            super.setDisplayIcon(NORMAL_ICON);
        }
    }

    private static enum Mode {
        NORMAL,
        VENOM;

    }
}

