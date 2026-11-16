package net.kazi.kazimod.leopard;

import net.minecraft.entity.LivingEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.TranslationTextComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.*;
import xyz.pixelatedw.mineminenomi.api.abilities.components.*;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;

public final class KamisoriAbility extends Ability {
    public static final AbilityCore<KamisoriAbility> INSTANCE=new AbilityCore.Builder<>("Kamisori",AbilityCategory.DEVIL_FRUITS,KamisoriAbility::new)
            .setUnlockCheck(LeopardFeatures::awakened).setIcon(new ResourceLocation("kazimod","textures/abilities/kamisori.png"))
            .addDescriptionLine(new TranslationTextComponent("ability.kazimod.kamisori.description"))
            .addAdvancedDescriptionLine(CooldownComponent.getTooltip(240)).build();
    private final ContinuousComponent continuous=new ContinuousComponent(this);
    private Vector3d direction=Vector3d.ZERO;
    public KamisoriAbility(AbilityCore<KamisoriAbility> core) {
        super(core);isNew=true;addComponents(continuous,new RequireMorphComponent(this,LeopardFeatures.MORPH.get()));
        addCanUseCheck((u,a)->LeopardFeatures.empowered(u)&&!u.isPassenger()?AbilityUseResult.success():AbilityUseResult.fail(new TranslationTextComponent("message.kazimod.leopard.form_required")));
        addUseEvent((u,a)->continuous.triggerContinuity(u,16));
        continuous.addStartEvent((u,a)->{direction=u.getLookAngle();sound(u);});
        continuous.addTickEvent((u,a)->tickDash(u));
        continuous.addEndEvent((u,a)->{direction=Vector3d.ZERO;AbilityHelper.setDeltaMovement(u,Vector3d.ZERO);cooldownComponent.startCooldown(u,240);});
        addRemoveEvent((u,a)->{if(continuous.isContinuous())continuous.stopContinuity(u);});
    }
    private void tickDash(LivingEntity user) {
        if(!LeopardFeatures.empowered(user)||user.isPassenger()) {continuous.stopContinuity(user);return;}
        int t=(int)continuous.getContinueTime();
        if(t==10){direction=user.getLookAngle();sound(user);}
        if(t>=6&&t<10){AbilityHelper.setDeltaMovement(user,Vector3d.ZERO);user.fallDistance=0;return;}
        Vector3d blended=direction.scale(.65).add(user.getLookAngle().scale(.35));
        direction=blended.lengthSqr()<.01?user.getLookAngle():blended.normalize();
        if(!LeopardMobility.move(user,direction.scale(1.55))&&!user.level.isClientSide)continuous.stopContinuity(user);
    }
    private static void sound(LivingEntity u){if(!u.level.isClientSide)u.level.playSound(null,u.blockPosition(),SoundEvents.PLAYER_ATTACK_SWEEP,SoundCategory.PLAYERS,.7F,1.65F);}
    public boolean active(){return continuous.isContinuous();}
}
