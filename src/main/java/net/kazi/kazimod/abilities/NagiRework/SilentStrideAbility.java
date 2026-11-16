package net.kazi.kazimod.abilities.NagiRework;

import net.minecraft.entity.LivingEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
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
import xyz.pixelatedw.mineminenomi.api.abilities.components.CooldownComponent;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.init.ModEffects;
import xyz.pixelatedw.mineminenomi.init.ModSounds;

public class SilentStrideAbility extends Ability {

    private static final ITextComponent[] DESCRIPTION =
            AbilityHelper.registerDescriptionText("kazimod", "silent_stride",
                    new Pair[]{ImmutablePair.of(
                            "The user silences their presence, becoming invisible and gaining incredible speed for 6 seconds.", null)});

    private static final ResourceLocation ICON = new ResourceLocation("kazimod", "textures/abilities/silent_stride.png");
    private static final float COOLDOWN = 420.0F; // 21 seconds
    private static final int DURATION = 120; // 6 seconds

    public static final AbilityCore<SilentStrideAbility> INSTANCE;

    public SilentStrideAbility(AbilityCore<SilentStrideAbility> core) {
        super(core);
        this.isNew = true;
        this.addUseEvent(this::onUseEvent);
    }

    private void onUseEvent(LivingEntity entity, IAbility ability) {
        if (!entity.level.isClientSide) {
            // Suke-style invisibility
            entity.addEffect(new EffectInstance(
                    (Effect) ModEffects.SUKE_INVISIBILITY.get(), DURATION, 0, false, false));
            // Speed V
            entity.addEffect(new EffectInstance(
                    Effects.MOVEMENT_SPEED, DURATION, 4, false, false));
        }

        entity.level.playSound((PlayerEntity) null, entity.blockPosition(),
                (SoundEvent) ModSounds.TELEPORT_SFX.get(),
                SoundCategory.PLAYERS, 1.0F, 1.5F);

        this.cooldownComponent.startCooldown(entity, COOLDOWN);
    }

    static {
        INSTANCE = (new AbilityCore.Builder<>("Silent Stride", AbilityCategory.DEVIL_FRUITS, SilentStrideAbility::new))
                .setIcon(ICON)
                .addDescriptionLine(DESCRIPTION)
                .addAdvancedDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{
                        AbilityDescriptionLine.NEW_LINE,
                        CooldownComponent.getTooltip(COOLDOWN)
                })
                .build();
    }
}
