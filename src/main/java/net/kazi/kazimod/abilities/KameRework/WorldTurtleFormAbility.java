package net.kazi.kazimod.abilities.KameRework;

import net.kazi.kazimod.init.KaziMorphs;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.StringTextComponent;
import xyz.pixelatedw.mineminenomi.abilities.PropelledFlightAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.*;
import xyz.pixelatedw.mineminenomi.api.abilities.components.*;
import xyz.pixelatedw.mineminenomi.api.helpers.AttributeHelper;
import xyz.pixelatedw.mineminenomi.api.morph.MorphInfo;
import xyz.pixelatedw.mineminenomi.data.entity.devilfruit.DevilFruitCapability;
import xyz.pixelatedw.mineminenomi.data.entity.ability.AbilityDataCapability;
import xyz.pixelatedw.mineminenomi.init.ModAbilities;
import xyz.pixelatedw.mineminenomi.init.ModAttributes;

public final class WorldTurtleFormAbility extends MorphAbility2 {
    public static final AbilityCore<WorldTurtleFormAbility> INSTANCE =
            new AbilityCore.Builder<>("World Turtle Form", AbilityCategory.DEVIL_FRUITS, WorldTurtleFormAbility::new)
                    .setUnlockCheck(WorldTurtleFormAbility::canUnlock)
                    .setIcon(new ResourceLocation("kazimod", "textures/abilities/world_turtle_form.png"))
                    .addDescriptionLine(new StringTextComponent("Kame awakening: become a colossal world-bearing turtle with Zoan flight, 200 additional HP, 50% body damage reduction and immunity to stuns and knockouts."))
                    .addAdvancedDescriptionLine(CooldownComponent.getTooltip(10), ContinuousComponent.getTooltip()).build();

    public WorldTurtleFormAbility(AbilityCore<WorldTurtleFormAbility> core) {
        super(core);
        addCanUseCheck((user, ability) -> continuousComponent.isContinuous() || canUnlock(user)
                ? AbilityUseResult.success() : AbilityUseResult.fail(new StringTextComponent("Awaken Kame Kame no Mi first.")));
        statsComponent.addAttributeModifier(Attributes.MAX_HEALTH, new AbilityAttributeModifier(AttributeHelper.MORPH_HEALTH_UUID, INSTANCE, "World Turtle health", 200, Operation.ADDITION));
        statsComponent.addAttributeModifier(Attributes.ARMOR, new AbilityAttributeModifier(AttributeHelper.MORPH_ARMOR_UUID, INSTANCE, "World Turtle shell", 25, Operation.ADDITION));
        statsComponent.addAttributeModifier(Attributes.KNOCKBACK_RESISTANCE, new AbilityAttributeModifier(AttributeHelper.MORPH_KNOCKBACK_RESISTANCE_UUID, INSTANCE, "World Turtle weight", 1, Operation.ADDITION));
        statsComponent.addAttributeModifier(Attributes.MOVEMENT_SPEED, new AbilityAttributeModifier(AttributeHelper.MORPH_MOVEMENT_SPEED_UUID, INSTANCE, "World Turtle crawl", -.6, Operation.MULTIPLY_BASE));
        statsComponent.addAttributeModifier(ModAttributes.STEP_HEIGHT, new AbilityAttributeModifier(AttributeHelper.MORPH_STEP_HEIGHT_UUID, INSTANCE, "World Turtle step", 2, Operation.ADDITION));
        statsComponent.addAttributeModifier(ModAttributes.FALL_RESISTANCE, new AbilityAttributeModifier(AttributeHelper.MORPH_FALL_RESISTANCE_UUID, INSTANCE, "World Turtle landing", 2, Operation.ADDITION));
        continuousComponent.addStartEvent((user, ability) -> {
            if (!user.level.isClientSide && user instanceof PlayerEntity) {
                IAbility flight = AbilityDataCapability.get(user).getPassiveAbility(WorldTurtleFlightAbility.INSTANCE);
                if (flight instanceof PropelledFlightAbility && !((PropelledFlightAbility) flight).isPaused())
                    WorldTurtleFlightAbility.keepAirborne((PlayerEntity) user);
            }
        }).addEndEvent((user, ability) -> {
            if (!user.level.isClientSide && user instanceof PlayerEntity)
                PropelledFlightAbility.disableFlight((PlayerEntity) user);
        }).addTickEvent(1, (user, ability) -> {
            if (!user.level.isClientSide && !canUnlock(user)) continuousComponent.stopContinuity(user);
        });
    }

    public static boolean canUnlock(LivingEntity user) {
        return DevilFruitCapability.get(user).hasAwakenedFruit()
                && DevilFruitCapability.get(user).hasDevilFruit(ModAbilities.KAME_KAME_NO_MI);
    }
    @Override public MorphInfo getTransformation() { return KaziMorphs.WORLD_TURTLE.get(); }
}
