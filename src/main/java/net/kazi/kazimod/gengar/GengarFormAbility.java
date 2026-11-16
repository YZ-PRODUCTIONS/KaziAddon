package net.kazi.kazimod.gengar;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.text.StringTextComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.*;
import xyz.pixelatedw.mineminenomi.api.abilities.components.*;
import xyz.pixelatedw.mineminenomi.api.helpers.*;
import xyz.pixelatedw.mineminenomi.api.morph.MorphInfo;
import xyz.pixelatedw.mineminenomi.init.ModAttributes;
public final class GengarFormAbility extends MorphAbility2 {
    public static final AbilityCore<GengarFormAbility> INSTANCE=new AbilityCore.Builder<>("Mega Phantom Form",AbilityCategory.DEVIL_FRUITS,GengarFormAbility::new)
            .setUnlockCheck(GengarFeatures::awakened).setIcon(GengarAbility.icon("mega_phantom_form"))
            .addDescriptionLine(new StringTextComponent("Become Mega Gengar. Grants 60 additional HP, 25% damage reduction and floating movement. Unlocks the awakened Horo techniques. Use again to return."))
            .addAdvancedDescriptionLine(ContinuousComponent.getTooltip()).build();
    public GengarFormAbility(AbilityCore<GengarFormAbility> core){
        super(core);
        addCanUseCheck((user,ability)->AbilityUseResult.fail(new StringTextComponent("This awakening has been removed.")));
        statsComponent.addAttributeModifier(Attributes.MAX_HEALTH,new AbilityAttributeModifier(AttributeHelper.MORPH_HEALTH_UUID,INSTANCE,"Mega Gengar vitality",60,Operation.ADDITION));
        statsComponent.addAttributeModifier(Attributes.MOVEMENT_SPEED,new AbilityAttributeModifier(AttributeHelper.MORPH_MOVEMENT_SPEED_UUID,INSTANCE,"Mega Gengar drift",.15,Operation.MULTIPLY_BASE));
        statsComponent.addAttributeModifier(ModAttributes.STEP_HEIGHT,new AbilityAttributeModifier(AttributeHelper.MORPH_STEP_HEIGHT_UUID,INSTANCE,"Mega Gengar step",.6,Operation.ADDITION));
        continuousComponent.addStartEvent((user,ability)->{if(!user.level.isClientSide){
            GengarFeatures.state(user).putBoolean("Active",true);GengarFeatures.pose(user,1,32);
            GengarEffect.spawn(user,GengarEffect.PORTAL,user.position(),36,2);
            GengarFeatures.sound(user,SoundEvents.EVOKER_PREPARE_SUMMON,.65F,.65F);
        }}).addEndEvent((user,ability)->{if(!user.level.isClientSide){
            GengarFeatures.state(user).putBoolean("Active",false);GengarFeatures.state(user).putLong("End",0);
            GengarFeatures.sync(user);
            cooldownComponent.startCooldown(user,200);
        }}).addTickEvent((user,ability)->{if(!user.level.isClientSide&&(!GengarFeatures.awakened(user)||AbilityHelper.isWeakenedByKairosekiOrWater(user)))continuousComponent.stopContinuity(user);});
    }
    @Override public MorphInfo getTransformation(){return GengarFeatures.MORPH.get();}
}
