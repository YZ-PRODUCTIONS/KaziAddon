package net.kazi.kazimod.abilities.KameRework;

import java.util.*;
import net.kazi.kazimod.worldturtle.*;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.*;
import net.minecraft.util.math.*;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.StringTextComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.*;
import xyz.pixelatedw.mineminenomi.api.abilities.components.*;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceHakiNature;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.api.protection.ProtectedArea;
import xyz.pixelatedw.mineminenomi.data.entity.ability.AbilityDataCapability;
import xyz.pixelatedw.mineminenomi.data.world.ProtectedAreasData;

public final class WorldShakingSpinAbility extends Ability {
    public static final int DURATION=60,COOLDOWN=600,DAMAGE=60;
    public static final double SPEED=4.05;
    public static final AbilityCore<WorldShakingSpinAbility> INSTANCE=new AbilityCore.Builder<>("World Shaking Spin",AbilityCategory.DEVIL_FRUITS,WorldShakingSpinAbility::new)
            .setUnlockCheck(WorldTurtleFormAbility::canUnlock).setSourceHakiNature(SourceHakiNature.HARDENING)
            .setIcon(new ResourceLocation("kazimod","textures/abilities/world_shaking_spin.png"))
            .addDescriptionLine(new StringTextComponent("Tuck into your shell and spin toward your aim for 3 seconds. Ignore all damage and ram enemies for 60 damage, at most once per second per target. Use again to cancel."))
            .addAdvancedDescriptionLine(CooldownComponent.getTooltip(COOLDOWN),ContinuousComponent.getTooltip(),DealDamageComponent.getTooltip(DAMAGE)).build();
    private final ContinuousComponent continuous=new ContinuousComponent(this);
    private final DealDamageComponent damage=new DealDamageComponent(this);
    private final Map<UUID,Long> hits=new HashMap<>();
    private AxisAlignedBB previousShell;
    public WorldShakingSpinAbility(AbilityCore<WorldShakingSpinAbility> core){
        super(core);isNew=true;addComponents(continuous,damage);
        addCanUseCheck((user,a)->continuous.isContinuous()||WorldTurtleHitboxes.active(user)
                ?AbilityUseResult.success():AbilityUseResult.fail(new StringTextComponent("Enter World Turtle Form first.")));
        addUseEvent((user,a)->{if(continuous.isContinuous())continuous.stopContinuity(user);else continuous.startContinuity(user,DURATION);});
        continuous.addStartEvent((user,a)->{
            hits.clear();previousShell=WorldTurtleHitboxes.bounds(user,0).inflate(.5);
            if(!user.level.isClientSide)user.level.playSound(null,user.blockPosition(),SoundEvents.TRIDENT_RIPTIDE_3,SoundCategory.PLAYERS,5,.6F);
        });
        continuous.addTickEvent((user,a)->tickSpin(user));
        continuous.addEndEvent((user,a)->{
            hits.clear();previousShell=null;user.fallDistance=0;
            if(!user.level.isClientSide){
                Vector3d brake=user.getDeltaMovement().scale(.15);
                AbilityHelper.setDeltaMovement(user,brake.x,brake.y,brake.z);
                cooldownComponent.startCooldown(user,COOLDOWN);
            }
        });
        addRemoveEvent((user,a)->{if(continuous.isContinuous())continuous.stopContinuity(user);});
    }
    private void tickSpin(LivingEntity user){
        if(!WorldTurtleHitboxes.active(user)||!WorldTurtleFormAbility.canUnlock(user)){continuous.stopContinuity(user);return;}
        user.fallDistance=0;
        if(user.level.isClientSide)return;
        AxisAlignedBB shell=WorldTurtleHitboxes.bounds(user,0).inflate(.5);
        if(previousShell==null)previousShell=shell;
        Vector3d travel=shell.getCenter().subtract(previousShell.getCenter());
        // Sweep only the path actually travelled; never bridge teleports or dimensions.
        if(travel.lengthSqr()>144){continuous.stopContinuity(user);return;}
        Vector3d direction=user.getLookAngle().normalize();
        strike(user,previousShell,travel,direction);
        previousShell=shell;
        Vector3d speed=direction.scale(SPEED);
        AbilityHelper.setDeltaMovement(user,speed.x,speed.y,speed.z);
        if(user.tickCount%10==0)user.level.playSound(null,user.blockPosition(),SoundEvents.PLAYER_ATTACK_SWEEP,SoundCategory.PLAYERS,4,.55F);
    }
    private void strike(LivingEntity user,AxisAlignedBB shell,Vector3d travel,Vector3d direction){
        long now=user.level.getGameTime();
        for(LivingEntity part:user.level.getEntitiesOfClass(LivingEntity.class,shell.expandTowards(travel).inflate(.1))){
            LivingEntity target=part instanceof WorldTurtlePartEntity?((WorldTurtlePartEntity)part).owner():part;
            if(target==null||target==user||!target.isAlive()||target.isSpectator()||user.isAlliedTo(target)||hits.getOrDefault(target.getUUID(),Long.MIN_VALUE)>now)continue;
            if(part instanceof WorldTurtlePartEntity&&((WorldTurtlePartEntity)part).isShell())continue;
            if(!WorldTurtleSpinMath.intersects(bounds(shell),travel.x,travel.y,travel.z,bounds(part.getBoundingBox())))continue;
            Vector3d contact=part.getBoundingBox().getCenter();
            if(user.level.clip(new RayTraceContext(shell.getCenter(),contact,RayTraceContext.BlockMode.COLLIDER,RayTraceContext.FluidMode.NONE,user)).getType()==RayTraceResult.Type.BLOCK)continue;
            BlockPos pos=part.blockPosition();ProtectedArea area=ProtectedAreasData.get(user.level).getProtectedArea(pos.getX(),pos.getY(),pos.getZ());
            if(area!=null&&!area.canHurtEntities())continue;
            hits.put(target.getUUID(),now+20);
            if(damage.hurtTarget(user,target,DAMAGE)){
                Vector3d push=direction.scale(2.2).add(0,.4,0);
                AbilityHelper.setDeltaMovement(target,push.x,push.y,push.z);
                user.level.playSound(null,part.blockPosition(),SoundEvents.PLAYER_ATTACK_STRONG,SoundCategory.PLAYERS,3,.6F);
            }
        }
    }
    private static double[] bounds(AxisAlignedBB b){return new double[]{b.minX,b.minY,b.minZ,b.maxX,b.maxY,b.maxZ};}
    public static boolean active(LivingEntity owner){
        if(!WorldTurtleHitboxes.active(owner))return false;
        IAbility ability=AbilityDataCapability.get(owner).getEquippedAbility(INSTANCE);
        return ability instanceof WorldShakingSpinAbility&&((WorldShakingSpinAbility)ability).continuous.isContinuous();
    }
}
