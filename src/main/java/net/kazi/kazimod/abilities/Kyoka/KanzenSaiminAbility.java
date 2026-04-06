package net.kazi.kazimod.abilities.Kyoka;

import net.kazi.kazimod.init.KaziEffects;
import net.minecraft.entity.LivingEntity;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.text.ITextComponent;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import xyz.pixelatedw.mineminenomi.api.abilities.Ability;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCategory;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityDescriptionLine;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AbilityComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.CooldownComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.RangeComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.RangeComponent.RangeType;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceHakiNature;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceType;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.init.ModEffects;
import xyz.pixelatedw.mineminenomi.init.ModSounds;

public class KanzenSaiminAbility extends Ability {
    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText(
            "kazimod",
            "kanzen_saimin",
            new Pair[]{ImmutablePair.of("Instantly traps anyone who sees the release in a crushing illusion, blurring reality and leaving them disoriented.", null)}
    );
    private static final float COOLDOWN = 320.0F;
    private static final float RANGE = 24.0F;
    public static final AbilityCore<KanzenSaiminAbility> INSTANCE;

    private final CooldownComponent cooldownComponent = new CooldownComponent(this);
    private final RangeComponent rangeComponent = new RangeComponent(this);

    public KanzenSaiminAbility(AbilityCore<KanzenSaiminAbility> core) {
        super(core);
        this.isNew = true;
        this.addComponents(new AbilityComponent[]{this.cooldownComponent, this.rangeComponent});
        this.addUseEvent(this::onUseEvent);
    }

    private void onUseEvent(LivingEntity entity, IAbility ability) {
        entity.level.playSound(null, entity.blockPosition(), ModSounds.KENBUNSHOKU_HAKI_ON_SFX.get(), SoundCategory.PLAYERS, 1.35F, 0.75F);

        for (LivingEntity target : this.rangeComponent.getTargetsInArea(entity, RANGE)) {
            if (target == entity || !target.canSee(entity)) {
                continue;
            }

            target.addEffect(new EffectInstance(Effects.BLINDNESS, 100, 1, false, true));
            target.addEffect(new EffectInstance(Effects.CONFUSION, 140, 1, false, true));
            target.addEffect(new EffectInstance(Effects.WEAKNESS, 100, 1, false, true));
            target.addEffect(new EffectInstance(Effects.MOVEMENT_SLOWDOWN, 100, 1, false, true));
            target.addEffect(new EffectInstance(ModEffects.DIZZY.get(), 40, 0, false, true));
            target.addEffect(new EffectInstance(KaziEffects.WEAKENED_MOVEMENT.get(), 100, 1, false, true));
        }

        this.cooldownComponent.startCooldown(entity, COOLDOWN);
    }

    static {
        INSTANCE = (new AbilityCore.Builder("Kanzen Saimin", AbilityCategory.DEVIL_FRUITS, KanzenSaiminAbility::new))
                .addDescriptionLine(DESCRIPTION)
                .addAdvancedDescriptionLine(
                        AbilityDescriptionLine.NEW_LINE,
                        CooldownComponent.getTooltip(COOLDOWN),
                        RangeComponent.getTooltip(RANGE, RangeType.AOE)
                )
                .setSourceHakiNature(SourceHakiNature.SPECIAL)
                .setSourceType(new SourceType[]{SourceType.INDIRECT})
                .build();
    }
}
