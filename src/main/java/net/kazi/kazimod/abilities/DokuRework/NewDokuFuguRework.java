package net.kazi.kazimod.abilities.DokuRework;

import java.awt.Color;
import net.kazi.kazimod.abilities.DokuRework.NewVenomDemonRework;
import net.MrMagicalCart.cartaddon.api.helpers.HakiUsedChecker;
import net.MrMagicalCart.cartaddon.init.CartMorphs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectInstance;
import net.minecraft.util.DamageSource;
import net.minecraft.util.text.ITextComponent;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import xyz.pixelatedw.mineminenomi.api.abilities.Ability;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCategory;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityDescriptionLine;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityOverlay;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityUseResult;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AbilityComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ContinuousComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.CooldownComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.DamageTakenComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.DealDamageComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.SkinOverlayComponent;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceHakiNature;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.api.morph.MorphInfo;
import xyz.pixelatedw.mineminenomi.events.FactionEvents;
import xyz.pixelatedw.mineminenomi.init.ModDamageSource;
import xyz.pixelatedw.mineminenomi.init.ModEffects;
import xyz.pixelatedw.mineminenomi.init.ModResources;

public class NewDokuFuguRework
extends Ability {
    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText((String)"kazimod", (String)"doku_fugu", (Pair[])new Pair[]{ImmutablePair.of((Object)"The user covers themselves in poison creating a thin protective layer to damage. Cannot be used while %s is active.", (Object)new Object[]{NewVenomDemonRework.INSTANCE})});
    private static final int MIN_COOLDOWN = 300;
    private static final int MAX_COOLDOWN = 900;
    private static final int HOLD_TIME = 1100;
    public static final AbilityCore<NewDokuFuguRework> INSTANCE = new AbilityCore.Builder<NewDokuFuguRework>("Doku Fugu", AbilityCategory.DEVIL_FRUITS, NewDokuFuguRework::new).addDescriptionLine(DESCRIPTION).addAdvancedDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{AbilityDescriptionLine.NEW_LINE, DealDamageComponent.getTooltip((float)5.0f), ContinuousComponent.getTooltip((float)1200.0f), CooldownComponent.getTooltip((float)200.0f, (float)800.0f)}).setSourceHakiNature(SourceHakiNature.SPECIAL).build();
    private static final AbilityOverlay OVERLAY = new AbilityOverlay.Builder().setTexture(ModResources.DOKU_COATING).setColor(new Color(1.0f, 1.0f, 1.0f, 0.6f)).build();
    private final ContinuousComponent continuousComponent = new ContinuousComponent((IAbility)this, true).addStartEvent(this::startContinuityEvent).addTickEvent(this::duringContinuityEvent).addEndEvent(this::endContinuityEvent);
    private final SkinOverlayComponent skinOverlayComponent;
    private final DamageTakenComponent damageTakenComponent = new DamageTakenComponent((IAbility)this, this::damageTakenEvent, DamageTakenComponent.DamageState.HURT);
    private final DealDamageComponent dealDamageComponent = new DealDamageComponent((IAbility)this);

    public NewDokuFuguRework(AbilityCore<NewDokuFuguRework> core) {
        super(core);
        this.skinOverlayComponent = new SkinOverlayComponent((IAbility)this, OVERLAY, new AbilityOverlay[0]);
        this.isNew = true;
        this.addComponents(new AbilityComponent[]{this.damageTakenComponent, this.dealDamageComponent, this.continuousComponent, this.skinOverlayComponent});
        this.addCanUseCheck((entity, ability) -> ((MorphInfo)CartMorphs.VENOM_DEMON2.get()).isActive(entity) ? AbilityUseResult.fail((ITextComponent)null) : AbilityUseResult.success());
        this.addUseEvent(this::useEvent);
    }

    private void useEvent(LivingEntity entity, IAbility ability) {
        this.continuousComponent.triggerContinuity(entity, 1200.0f);
    }

    private void startContinuityEvent(LivingEntity entity, IAbility ability) {
        this.skinOverlayComponent.showAll(entity);
    }

    private void duringContinuityEvent(LivingEntity entity, IAbility ability) {
        if (this.canUse(entity).isFail()) {
            this.continuousComponent.stopContinuity(entity);
        }
        entity.addEffect(new EffectInstance((Effect)ModEffects.PHYSICAL_MOVING_GUARD.get(), 2, 2, false, false));
    }

    private void endContinuityEvent(LivingEntity entity, IAbility ability) {
        this.skinOverlayComponent.hideAll(entity);
        this.cooldownComponent.startCooldown(entity, 200.0f + this.continuousComponent.getContinueTime() / 2.0f);
    }

    private float damageTakenEvent(LivingEntity entity, IAbility ability, DamageSource damageSource, float damage) {
        Entity sourceEntity;
        if (this.continuousComponent.isContinuous() && (sourceEntity = damageSource.getEntity()) != null && sourceEntity instanceof LivingEntity && FactionEvents.isDirectHit((DamageSource)damageSource)) {
            if (HakiUsedChecker.hasHasoInfusionOn((LivingEntity)sourceEntity)) {
                return damage;
            }
            if (HakiUsedChecker.hasInternalOn((LivingEntity)sourceEntity)) {
                return damage;
            }
            if (HakiUsedChecker.hasEmissionOn((LivingEntity)sourceEntity)) {
                return damage;
            }
            this.dealDamageComponent.hurtTarget(entity, (LivingEntity)sourceEntity, 5.0f, ModDamageSource.POISON);
            ((LivingEntity)sourceEntity).addEffect(new EffectInstance((Effect)ModEffects.DOKU_POISON.get(), 60, 0));
        }
        return damage;
    }
}

