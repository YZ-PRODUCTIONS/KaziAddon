package net.kazi.kazimod.abilities.KameRework;

import net.kazi.kazimod.init.KaziMorphs;
import net.kazi.kazimod.worldturtle.WorldTurtleEffects;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.StringTextComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.*;
import xyz.pixelatedw.mineminenomi.api.abilities.components.*;
import xyz.pixelatedw.mineminenomi.data.entity.ability.AbilityDataCapability;

public final class DivineShieldAbility extends Ability {
    public static final int DURATION=100, COOLDOWN=400;
    public static final AbilityCore<DivineShieldAbility> INSTANCE=new AbilityCore.Builder<>("Divine Shield",AbilityCategory.DEVIL_FRUITS,DivineShieldAbility::new)
            .setUnlockCheck(WorldTurtleFormAbility::canUnlock)
            .setIcon(new ResourceLocation("kazimod","textures/abilities/divine_shield.png"))
            .addDescriptionLine(new StringTextComponent("Retreat into your shell for 5 seconds. Ignore damage, return 10% of incoming damage, and reflect projectiles. Use again to cancel."))
            .addAdvancedDescriptionLine(CooldownComponent.getTooltip(COOLDOWN),ContinuousComponent.getTooltip()).build();
    private final ContinuousComponent continuous=new ContinuousComponent(this,true);
    public DivineShieldAbility(AbilityCore<DivineShieldAbility> core){
        super(core);isNew=true;addComponents(continuous);
        addCanUseCheck((e,a)->continuous.isContinuous()||KaziMorphs.WORLD_TURTLE.get().isActive(e)
                ?AbilityUseResult.success():AbilityUseResult.fail(new StringTextComponent("Enter World Turtle Form first.")));
        addUseEvent((e,a)->{if(continuous.isContinuous())continuous.stopContinuity(e);else continuous.startContinuity(e,DURATION);});
        continuous.addStartEvent((e,a)->WorldTurtleEffects.shield(e));
        continuous.addTickEvent((e,a)->{
            if(!KaziMorphs.WORLD_TURTLE.get().isActive(e)||!WorldTurtleFormAbility.canUnlock(e)){continuous.stopContinuity(e);return;}
            WorldTurtleEffects.deflectNearby(e);
        });
        continuous.addEndEvent((e,a)->{if(!e.level.isClientSide)cooldownComponent.startCooldown(e,COOLDOWN);});
        addRemoveEvent((e,a)->{if(continuous.isContinuous())continuous.stopContinuity(e);});
    }
    public static boolean active(LivingEntity e){
        if(!e.isAlive()||!KaziMorphs.WORLD_TURTLE.get().isActive(e))return false;
        IAbility a=AbilityDataCapability.get(e).getEquippedAbility(INSTANCE);
        return a instanceof DivineShieldAbility&&((DivineShieldAbility)a).continuous.isContinuous();
    }
}
