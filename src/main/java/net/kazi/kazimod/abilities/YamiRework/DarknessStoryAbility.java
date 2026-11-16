package net.kazi.kazimod.abilities.YamiRework;

import net.kazi.kazimod.yami.*;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MobEntity;
import net.minecraft.potion.EffectInstance;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.StringTextComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.*;
import xyz.pixelatedw.mineminenomi.api.abilities.components.*;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceElement;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.init.ModEffects;

public class DarknessStoryAbility extends Ability {
    public static final int DURATION=120,COOLDOWN=500;
    public static final double RADIUS=15,HEIGHT=24;
    public static final AbilityCore<DarknessStoryAbility> INSTANCE=new AbilityCore.Builder<DarknessStoryAbility>(
            "Darkness Story",AbilityCategory.DEVIL_FRUITS,DarknessStoryAbility::new)
            .addDescriptionLine(new StringTextComponent("Surrounds the user with a huge tornado of darkness, drawing enemies within 15 blocks into its center and stunning them for six seconds."))
            .addAdvancedDescriptionLine(AbilityDescriptionLine.NEW_LINE,CooldownComponent.getTooltip(COOLDOWN),ContinuousComponent.getTooltip(DURATION))
            .setIcon(new ResourceLocation("kazimod","textures/abilities/darkness_storm.png"))
            .setSourceElement(SourceElement.GRAVITY).build();
    private final ContinuousComponent continuous=new ContinuousComponent(this,true)
            .addStartEvent(this::start).addTickEvent(this::during).addEndEvent(this::end);
    private int elapsed;
    public DarknessStoryAbility(AbilityCore<DarknessStoryAbility> core) {
        super(core);isNew=true;addComponents(continuous);
        addUseEvent((owner,ability)->continuous.triggerContinuity(owner,DURATION));
        addRemoveEvent((owner,ability)->continuous.stopContinuity(owner));
    }
    private void start(LivingEntity owner,IAbility ability) {
        elapsed=0;
        if(!owner.level.isClientSide) {
            YamiVisualPacket.send(owner,YamiEffects.TORNADO,owner.position());
            owner.level.playSound(null,owner.blockPosition(),SoundEvents.PORTAL_TRIGGER,SoundCategory.PLAYERS,.45F,.5F);
        }
    }
    private void during(LivingEntity owner,IAbility ability) {
        if(owner.level.isClientSide)return;
        if(!owner.isAlive()){continuous.stopContinuity(owner);return;}
        Vector3d center=owner.position();
        if(elapsed++%5==0)YamiVisualPacket.send(owner,YamiEffects.TORNADO,center);
        AxisAlignedBB area=new AxisAlignedBB(center.x-RADIUS,center.y-2,center.z-RADIUS,center.x+RADIUS,center.y+HEIGHT,center.z+RADIUS);
        for(LivingEntity target:owner.level.getEntitiesOfClass(LivingEntity.class,area,t->YamiCombat.enemy(owner,t))) {
            Vector3d offset=center.subtract(target.position());
            double horizontal=offset.x*offset.x+offset.z*offset.z;
            if(horizontal>RADIUS*RADIUS||AbilityHelper.isDodging(target))continue;
            YamiCombat.stun(target);
            if(elapsed%4==1)AbilityHelper.disableAbilities(target,6,a->true);
            if(target instanceof MobEntity)((MobEntity)target).getNavigation().stop();
            Vector3d goal=center.add(0,.6,0).subtract(target.position());
            double distance=goal.length();
            Vector3d velocity=distance<1.5?target.getDeltaMovement().scale(.2)
                    :target.getDeltaMovement().scale(.45).add(goal.normalize().scale(Math.min(.85,.25+distance*.045)));
            if(velocity.lengthSqr()>1.44)velocity=velocity.normalize().scale(1.2);
            AbilityHelper.setDeltaMovement(target,velocity);target.hurtMarked=true;target.fallDistance=0;
        }
    }
    private void end(LivingEntity owner,IAbility ability) {
        if(!owner.level.isClientSide) {
            YamiVisualPacket.send(owner,-3,owner.position());
            cooldownComponent.startCooldown(owner,COOLDOWN);
        }
    }
}
