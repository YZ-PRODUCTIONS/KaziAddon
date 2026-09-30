package net.kazi.kazimod.abilities.DokuRework;

import java.util.List;
import java.util.UUID;
import net.MrMagicalCart.cartaddon.abilities.dokuextra.NewDokuHelper;
import net.MrMagicalCart.cartaddon.init.CartMorphs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectInstance;
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
import xyz.pixelatedw.mineminenomi.api.abilities.components.BonusOperation;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ContinuousComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.CooldownComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.RangeComponent;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceElement;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.api.morph.MorphInfo;
import xyz.pixelatedw.mineminenomi.init.ModEffects;
import xyz.pixelatedw.mineminenomi.init.ModParticleEffects;
import xyz.pixelatedw.mineminenomi.particles.effects.ParticleEffect;
import xyz.pixelatedw.mineminenomi.wypi.WyHelper;
import xyz.pixelatedw.mineminenomi.wypi.WyRegistry;

public class NewDokuGumoRework
extends Ability {
    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText((String)"kazimod", (String)"doku_gumo", (Pair[])new Pair[]{ImmutablePair.of((Object)"Creates a dense cloud of poisonous smoke, which moves along with the user and poisons everyone inside.", null)});
    private static final UUID VENOM_RANGE_BONUS_UUID = UUID.fromString("77d2625c-c112-4ebf-9f1e-01311c66968b");
    private static final ITextComponent NORMAL_NAME = new TranslationTextComponent(WyRegistry.registerName((String)"ability.mineminenomi.doku_gumo", (String)"Doku Gumo"));
    private static final ITextComponent VENOM_NAME = new TranslationTextComponent(WyRegistry.registerName((String)"ability.mineminenomi.doku_gumo_venom", (String)"Venom Doku Gumo"));
    private static final ResourceLocation NORMAL_ICON = new ResourceLocation("mineminenomi", "textures/abilities/doku_gumo.png");
    private static final ResourceLocation VENOM_ICON = new ResourceLocation("mineminenomi", "textures/abilities/doku_gumo_venom.png");
    private static final int COOLDOWN = 800;
    private static final int HOLD_TIME = 200;
    private static final int RANGE = 10;
    public static final AbilityCore<NewDokuGumoRework> INSTANCE = new AbilityCore.Builder<NewDokuGumoRework>("Doku Gumo", AbilityCategory.DEVIL_FRUITS, NewDokuGumoRework::new).addDescriptionLine(DESCRIPTION).addAdvancedDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{AbilityDescriptionLine.NEW_LINE, ContinuousComponent.getTooltip((float)200.0f), CooldownComponent.getTooltip((float)800.0f), RangeComponent.getTooltip((float)10.0f, (RangeComponent.RangeType)RangeComponent.RangeType.AOE)}).setSourceElement(SourceElement.POISON).build();
    private final ContinuousComponent continuousComponent = new ContinuousComponent((IAbility)this).addTickEvent(this::duringContinuityEvent).addEndEvent(this::endContinuityEvent);
    private final RangeComponent rangeComponent = new RangeComponent((IAbility)this);
    private final AltModeComponent<Mode> altModeComponent = new AltModeComponent<Mode>((IAbility)this, Mode.class, Mode.NORMAL, true).addChangeModeEvent(this::onAltModeChange);

    public NewDokuGumoRework(AbilityCore<NewDokuGumoRework> core) {
        super(core);
        this.isNew = true;
        this.addComponents(new AbilityComponent[]{this.continuousComponent, this.rangeComponent, this.altModeComponent});
        this.addUseEvent(this::useEvent);
    }

    private void useEvent(LivingEntity entity, IAbility ability) {
        this.continuousComponent.triggerContinuity(entity, 200.0f);
    }

    private void duringContinuityEvent(LivingEntity player, IAbility ability) {
        int duration = 80;
        boolean hasVenomDemon = false;
        int poisonIntensity = 0;
        if (((MorphInfo)CartMorphs.VENOM_DEMON2.get()).isActive(player)) {
            hasVenomDemon = true;
            this.rangeComponent.getBonusManager().addBonus(VENOM_RANGE_BONUS_UUID, "Venom Range Bonus", BonusOperation.MUL, 2.0f);
            poisonIntensity = 1;
        }
        List<LivingEntity> targets = this.rangeComponent.getTargetsInArea(player, 10.0f);
        for (LivingEntity target : targets) {
            if (target.hasEffect((Effect)ModEffects.DOKU_POISON.get())) continue;
            target.addEffect(new EffectInstance((Effect)ModEffects.DOKU_POISON.get(), duration, poisonIntensity));
        }
        if (this.continuousComponent.getContinueTime() % 2.0f == 0.0f) {
            WyHelper.spawnParticleEffect((ParticleEffect)((ParticleEffect)ModParticleEffects.DOKU_GUMO.get()), (Entity)player, (double)player.getX(), (double)player.getY(), (double)player.getZ(), (ParticleEffect.Details)(hasVenomDemon ? NewDokuHelper.DETAILS : null));
        }
    }

    private void endContinuityEvent(LivingEntity player, IAbility ability) {
        this.cooldownComponent.startCooldown(player, 800.0f);
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

