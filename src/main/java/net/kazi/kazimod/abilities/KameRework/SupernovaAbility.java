package net.kazi.kazimod.abilities.KameRework;

import net.kazi.kazimod.worldturtle.*;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.StringTextComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.*;
import xyz.pixelatedw.mineminenomi.api.abilities.components.*;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceHakiNature;
import xyz.pixelatedw.mineminenomi.data.entity.ability.AbilityDataCapability;

public final class SupernovaAbility extends Ability {
    public static final int CHARGE=40,DURATION=120,COOLDOWN=1200;
    public static final AbilityCore<SupernovaAbility> INSTANCE=new AbilityCore.Builder<>("Supernova",AbilityCategory.DEVIL_FRUITS,SupernovaAbility::new)
            .setUnlockCheck(WorldTurtleFormAbility::canUnlock).setSourceHakiNature(SourceHakiNature.SPECIAL)
            .setIcon(new ResourceLocation("kazimod","textures/abilities/supernova.png"))
            .addDescriptionLine(new StringTextComponent("Summon five giant stars around World Turtle. After a 2-second charge, aim their beams for 4 seconds. Use again to cancel."))
            .addAdvancedDescriptionLine(CooldownComponent.getTooltip(COOLDOWN),ContinuousComponent.getTooltip()).build();
    private final ContinuousComponent continuous=new ContinuousComponent(this,true);
    private final DealDamageComponent damage=new DealDamageComponent(this);
    private SupernovaEntity effect;
    public SupernovaAbility(AbilityCore<SupernovaAbility> core){
        super(core);isNew=true;addComponents(continuous,damage);
        addCanUseCheck((user,a)->continuous.isContinuous()||WorldTurtleHitboxes.active(user)
                ?AbilityUseResult.success():AbilityUseResult.fail(new StringTextComponent("Enter World Turtle Form first.")));
        addUseEvent((user,a)->{if(continuous.isContinuous())continuous.stopContinuity(user);else continuous.startContinuity(user,DURATION);});
        continuous.addStartEvent((user,a)->{if(!user.level.isClientSide)effect=SupernovaEntity.spawn(user);});
        continuous.addTickEvent((user,a)->{if(!WorldTurtleHitboxes.active(user)||!WorldTurtleFormAbility.canUnlock(user))continuous.stopContinuity(user);});
        continuous.addEndEvent((user,a)->{if(effect!=null){effect.remove();effect=null;}if(!user.level.isClientSide)cooldownComponent.startCooldown(user,COOLDOWN);});
        addRemoveEvent((user,a)->{if(continuous.isContinuous())continuous.stopContinuity(user);});
    }
    public static SupernovaAbility active(LivingEntity owner){
        IAbility ability=AbilityDataCapability.get(owner).getEquippedAbility(INSTANCE);
        return ability instanceof SupernovaAbility&&((SupernovaAbility)ability).continuous.isContinuous()?(SupernovaAbility)ability:null;
    }
    public void hit(LivingEntity owner,LivingEntity target){damage.hurtTarget(owner,target,10);}
}
