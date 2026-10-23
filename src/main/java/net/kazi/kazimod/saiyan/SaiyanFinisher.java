package net.kazi.kazimod.saiyan;

import java.util.Optional;
import net.minecraft.entity.*;
import net.minecraft.entity.player.*;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.potion.EffectInstance;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.*;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import xyz.pixelatedw.mineminenomi.api.abilities.components.*;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.api.protection.ProtectedArea;
import xyz.pixelatedw.mineminenomi.data.world.ProtectedAreasData;
import xyz.pixelatedw.mineminenomi.init.ModEffects;

/** Server-authoritative Ultra Instinct grab, ascent, pause and collision-tested slam. */
final class SaiyanFinisher {
    private static final String RECOVERY="KaziSaiyanFinisherRecovery";
    private final LivingEntity user;
    private final World world;
    private final GrabEntityComponent grab;
    private final DealDamageComponent damage;
    private final boolean dragon,userGravity;
    private final Vector3d direction,forward;
    private final float yaw,originalPitch;
    private LivingEntity victim;
    private Vector3d userReturn,victimReturn,lastMouth;
    private boolean victimGravity,released,impacted;
    private int phase,ticks,total;
    private double startY;
    private SaiyanEffect visual;

    SaiyanFinisher(LivingEntity user,GrabEntityComponent grab,DealDamageComponent damage,boolean dragon){
        this.user=user;world=user.level;this.grab=grab;this.damage=damage;this.dragon=dragon;
        userGravity=user.isNoGravity();originalPitch=user.xRot;yaw=user.yRot;
        direction=user.getLookAngle().normalize();
        Vector3d flat=direction.multiply(1,0,1);
        forward=flat.lengthSqr()<.01?new Vector3d(-Math.sin(Math.toRadians(yaw)),0,Math.cos(Math.toRadians(yaw))):flat.normalize();
        userReturn=user.position();lastMouth=user.getEyePosition(1);
        if(user instanceof PlayerEntity){
            PlayerEntity player=(PlayerEntity)user;SaiyanFlightAbility.stopFlight(player);
            player.abilities.flying=false;player.onUpdateAbilities();
        }
        user.stopRiding();user.setNoGravity(true);
        saveRecovery(user,userGravity,userReturn);
        if(dragon)visual=SaiyanEffect.spawn(user,SaiyanEffect.INSTINCT_DRAGON,user.position(),180,5);
        setPhase(SaiyanFinisherMotion.LUNGE);
    }
    boolean tick(){
        if(released||++total>160||!user.isAlive()||user.removed||user.level!=world||user.isSpectator()
                ||SaiyanState.form(user)!=SaiyanBalance.ULTRA_INSTINCT)return false;
        ticks++;lock(user,false);
        if(phase==SaiyanFinisherMotion.LUNGE)return lunge();
        if(victim==null||!victim.isAlive()||victim.removed||victim.level!=world||victim.isPassenger()
                ||!SaiyanAbility.canHit(user,victim)||grab.getGrabbedEntity()!=victim||!grab.canContinueGrab(user))return false;
        if(victim.position().subtract(victimReturn).multiply(1,0,1).lengthSqr()>64)return false;
        lock(victim,true);
        if(phase==SaiyanFinisherMotion.BITE){
            updateVisual();
            if(ticks>=6)setPhase(SaiyanFinisherMotion.ASCEND);
            return true;
        }
        if(phase==SaiyanFinisherMotion.ASCEND){
            double y=SaiyanFinisherMotion.ascent(startY,dragon,ticks);
            double expected=y-victim.getY();
            if(!movePair(new Vector3d(0,expected,0)))return false;
            updateVisual();updateSpace();
            if(Math.abs(victim.getY()-y)>.12){setPhase(SaiyanFinisherMotion.DIVE);return true;}
            if(ticks>=SaiyanFinisherMotion.ascentTicks(dragon))setPhase(SaiyanFinisherMotion.APEX);
            return true;
        }
        if(phase==SaiyanFinisherMotion.APEX){
            rotateCarrier();updateVisual();updateSpace();
            if(ticks>=SaiyanFinisherMotion.apexTicks(dragon))setPhase(SaiyanFinisherMotion.DIVE);
            return true;
        }
        double before=victim.getY(),speed=SaiyanFinisherMotion.diveSpeed(ticks),distance=Math.min(speed,Math.max(0,before-1));
        if(distance<=.001&&!victim.isOnGround())return false;
        if(!movePair(new Vector3d(0,-distance,0)))return false;
        updateVisual();updateSpace();
        if(victim.isOnGround()||victim.getY()>before-distance+.08||victim.isInWater()||victim.isInLava()){
            impact();return false;
        }
        return ticks<90;
    }
    private boolean lunge(){
        Vector3d before=user.position();
        if(!move(user,direction.scale(dragon?2.2:1.8)))return false;
        Vector3d from=dragon?lastMouth:before.add(0,user.getBbHeight()*.55,0);
        Vector3d to=dragon?user.position().add(0,user.getBbHeight()*.6,0).add(direction.scale(SaiyanFinisherMotion.dragonReach(ticks)))
                :user.position().add(0,user.getBbHeight()*.55,0);
        BlockRayTraceResult wall=world.clip(new RayTraceContext(from,to,RayTraceContext.BlockMode.COLLIDER,RayTraceContext.FluidMode.NONE,user));
        Vector3d end=wall.getLocation();LivingEntity closest=null;double best=Double.MAX_VALUE;
        double radius=dragon?3.8:user.getBbWidth()*.5+.2;
        for(LivingEntity enemy:world.getEntitiesOfClass(LivingEntity.class,new AxisAlignedBB(from,end).inflate(radius))){
            if(!SaiyanAbility.canHit(user,enemy)||enemy.isPassenger()||enemy.hasEffect(ModEffects.GRABBED.get())||!user.canSee(enemy))continue;
            if(enemy.getBoundingBox().getCenter().subtract(before).dot(direction)<=0)continue;
            AxisAlignedBB box=enemy.getBoundingBox().inflate(radius);
            Optional<Vector3d> hit=box.clip(from,end);
            if(!hit.isPresent()&&!box.contains(from))continue;
            double distance=box.contains(from)?0:hit.get().distanceToSqr(from);
            if(distance<best){best=distance;closest=enemy;}
        }
        lastMouth=to;
        if(closest!=null){
            if(!grab.grabManually(user,closest))return false;
            // Keep the ordinary technique's total damage; this mode changes its delivery.
            if(!damage.hurtTarget(user,closest,5*SaiyanState.power(user))){grab.release(user);return false;}
            victim=closest;victimGravity=victim.isNoGravity();victimReturn=victim.position();userReturn=user.position();
            saveRecovery(victim,victimGravity,victimReturn);saveRecovery(user,userGravity,userReturn);
            if(victim instanceof PlayerEntity){
                PlayerEntity player=(PlayerEntity)victim;player.abilities.flying=false;player.onUpdateAbilities();
            }
            victim.stopUsingItem();victim.setNoGravity(true);startY=victim.getY();
            if(!dragon){
                Vector3d hold=user.position().add(forward.scale(holdDistance())).add(0,.2,0);
                if(!move(victim,hold.subtract(victim.position()))||victim.distanceToSqr(hold)>.16)return false;
                startY=victim.getY();
            }
            setPhase(dragon?SaiyanFinisherMotion.BITE:SaiyanFinisherMotion.ASCEND);updateVisual();
            SaiyanEffect.spawn(user,4,victim.getBoundingBox().getCenter(),12,dragon?4:2);
            SaiyanFeatures.play(user,SoundEvents.ENDER_DRAGON_GROWL,dragon?.6F:.3F,dragon?.8F:1.6F);
            return true;
        }
        updateVisual();
        return ticks<18&&wall.getType()==RayTraceResult.Type.MISS&&user.position().distanceToSqr(before)>.01;
    }
    private double holdDistance(){return (user.getBbWidth()+victim.getBbWidth())*.5+.3;}
    private boolean movePair(Vector3d delta){
        if(!Double.isFinite(delta.lengthSqr())||delta.lengthSqr()>64*64)return false;
        // Interleave both bodies' substeps: a ceiling above either participant aborts the ascent.
        int steps=Math.max(1,(int)Math.ceil(delta.length()/.5));Vector3d step=delta.scale(1.0/steps);
        for(int i=0;i<steps;i++){
            Vector3d before=victim.position();
            if(!step(victim,step))return false;
            Vector3d actual=victim.position().subtract(before);
            if(!dragon){
                double lift=Math.max(1.1,victim.getBbHeight()-user.getEyeHeight()+holdDistance()*.75+.35);
                Vector3d desired=victim.position().subtract(forward.scale(holdDistance())).add(0,lift,0);
                if(!moveRaw(user,desired.subtract(user.position()))||user.distanceToSqr(desired)>.25)return false;
            }
            if(Math.abs(actual.y-step.y)>.03)break;
        }
        finishMovement(victim);
        if(!dragon){rotateCarrier();finishMovement(user);}
        return true;
    }
    private boolean move(LivingEntity entity,Vector3d delta){
        if(!moveRaw(entity,delta))return false;
        finishMovement(entity);return true;
    }
    private boolean moveRaw(LivingEntity entity,Vector3d delta){
        if(!Double.isFinite(delta.lengthSqr())||delta.lengthSqr()>64*64)return false;
        int steps=Math.max(1,(int)Math.ceil(delta.length()/.5));Vector3d part=delta.scale(1.0/steps);
        for(int i=0;i<steps;i++)if(!step(entity,part))return false;
        return true;
    }
    private boolean step(LivingEntity entity,Vector3d delta){
        Vector3d at=entity.position().add(delta);BlockPos pos=new BlockPos(at);
        // Entities can travel above the build ceiling; no terrain or new dimension is generated.
        if(at.y<1||at.y>4096||!world.hasChunkAt(pos)||!world.getWorldBorder().isWithinBounds(pos))return false;
        ProtectedArea area=ProtectedAreasData.get(world).getProtectedArea(pos.getX(),pos.getY(),pos.getZ());
        if(area!=null&&!area.canHurtEntities())return false;
        entity.move(MoverType.SELF,delta);entity.fallDistance=0;return true;
    }
    private void finishMovement(LivingEntity entity){
        AbilityHelper.setDeltaMovement(entity,Vector3d.ZERO);entity.fallDistance=0;
        if(entity instanceof ServerPlayerEntity)((ServerPlayerEntity)entity).connection.teleport(entity.getX(),entity.getY(),entity.getZ(),entity.yRot,entity.xRot);
    }
    private void rotateCarrier(){
        if(dragon)return;
        user.yRot=yaw;user.yHeadRot=yaw;user.yBodyRot=yaw;
        user.xRot=SaiyanFinisherMotion.meteorPitch(phase,ticks);
    }
    private void lock(LivingEntity entity,boolean target){
        entity.setNoGravity(true);entity.setDeltaMovement(Vector3d.ZERO);entity.fallDistance=0;entity.stopUsingItem();entity.setSprinting(false);
        entity.addEffect(new EffectInstance(target?ModEffects.GRABBED.get():ModEffects.MOVEMENT_BLOCKED.get(),3,target?3:0,false,false));
    }
    private void setPhase(int next){
        phase=next;ticks=0;
        if(!dragon&&next==SaiyanFinisherMotion.DIVE&&victim!=null){
            visual=SaiyanEffect.spawn(user,SaiyanEffect.METEOR_REENTRY,victim.getBoundingBox().getCenter(),100,1);
            updateVisual();
        }
        user.getPersistentData().getCompound(RECOVERY).putLong("Until",world.getGameTime()+200);
        if(victim!=null)victim.getPersistentData().getCompound(RECOVERY).putLong("Until",world.getGameTime()+200);
        SaiyanState.pose(user,dragon?14:next==SaiyanFinisherMotion.DIVE?16:15,180);
        publish(user);if(victim instanceof PlayerEntity)publish(victim);
        if(next==SaiyanFinisherMotion.DIVE)SaiyanFeatures.play(user,SoundEvents.ELYTRA_FLYING,.8F,.65F);
    }
    private void publish(LivingEntity entity){
        CompoundNBT n=SaiyanState.get(entity);
        n.putInt("Finisher",dragon?1:2);n.putInt("FinisherPhase",phase);n.putLong("FinisherStart",world.getGameTime());
        n.putLong("FinisherEnd",world.getGameTime()+180);n.putFloat("FinisherYaw",yaw);
        n.putInt("FinisherMove",(dragon?SaiyanAbility.Move.DRAGON:SaiyanAbility.Move.METEOR).ordinal());
        n.putBoolean("FinisherOwner",entity==user);
        if(entity==victim)n.putInt("FinisherCaptor",user.getId());
        sync(entity);
    }
    private void updateSpace(){
        if(dragon)return;
        float blend=SaiyanFinisherMotion.space(victim.getY(),startY);
        SaiyanState.get(user).putFloat("FinisherSpace",blend);sync(user);
        if(victim instanceof PlayerEntity){SaiyanState.get(victim).putFloat("FinisherSpace",blend);sync(victim);}
    }
    private void updateVisual(){
        if(visual==null)return;
        Vector3d at=victim==null?user.position():victim.getBoundingBox().getCenter();
        float pitch=victim==null?originalPitch:phase==SaiyanFinisherMotion.DIVE?90:phase>=SaiyanFinisherMotion.ASCEND?-90:originalPitch;
        visual.carry(at,yaw,pitch,victim==null?-1:victim.getId(),phase);
    }
    private void impact(){
        Vector3d at=victim.position();impacted=true;
        damage.hurtTarget(user,victim,(dragon?40:22)*SaiyanState.power(user));
        for(LivingEntity other:world.getEntitiesOfClass(LivingEntity.class,new AxisAlignedBB(at,at).inflate(5)))
            if(other!=victim&&SaiyanAbility.canHit(user,other)&&other.distanceToSqr(at)<=25&&victim.canSee(other)){
                damage.hurtTarget(user,other,8*SaiyanState.power(user));
                AbilityHelper.setDeltaMovement(other,other.position().subtract(at).multiply(1,0,1).normalize().scale(1.5).add(0,.6,0));
            }
        SaiyanEffect.spawn(user,SaiyanEffect.FINISHER_IMPACT,at,28,8);
        world.playSound(null,at.x,at.y,at.z,SoundEvents.GENERIC_EXPLODE,net.minecraft.util.SoundCategory.PLAYERS,1.4F,.6F);
        SaiyanState.pose(user,13,12);
    }
    void release(){
        if(released)return;released=true;
        if(visual!=null){visual.remove();visual=null;}
        grab.release(user);
        if(!impacted&&victim!=null){returnSafely(victim,victimReturn,world);if(!dragon)returnSafely(user,userReturn,world);}
        user.setNoGravity(userGravity);user.fallDistance=0;user.xRot=originalPitch;
        user.getPersistentData().remove(RECOVERY);
        if(victim!=null){victim.setNoGravity(victimGravity);victim.fallDistance=0;victim.getPersistentData().remove(RECOVERY);clear(victim);}
        clear(user);
    }
    private static void returnSafely(LivingEntity entity,Vector3d origin,World world){
        if(origin==null||!entity.isAlive()||entity.removed||entity.level!=world||!world.hasChunkAt(new BlockPos(origin)))return;
        for(int up=0;up<=6;up++){
            Vector3d at=origin.add(0,up,0);
            if(!world.getWorldBorder().isWithinBounds(new BlockPos(at))||!world.noCollision(entity,entity.getBoundingBox().move(at.subtract(entity.position()))))continue;
            entity.teleportTo(at.x,at.y,at.z);entity.setDeltaMovement(Vector3d.ZERO);return;
        }
    }
    private void saveRecovery(LivingEntity entity,boolean gravity,Vector3d home){
        CompoundNBT n=new CompoundNBT();n.putBoolean("Gravity",gravity);n.putDouble("X",home.x);n.putDouble("Y",home.y);n.putDouble("Z",home.z);
        n.putString("World",world.dimension().location().toString());n.putLong("Until",world.getGameTime()+200);
        entity.getPersistentData().put(RECOVERY,n);
    }
    static void recoverExpired(LivingEntity entity){
        if(entity.level.isClientSide||!entity.getPersistentData().contains(RECOVERY,10))return;
        if(entity.getPersistentData().getCompound(RECOVERY).getLong("Until")<=entity.level.getGameTime())recover(entity,true);
    }
    static void recover(LivingEntity entity,boolean returnHome){
        if(entity.level.isClientSide||!entity.getPersistentData().contains(RECOVERY,10))return;
        CompoundNBT n=entity.getPersistentData().getCompound(RECOVERY);
        if(returnHome&&n.getString("World").equals(entity.level.dimension().location().toString()))
            returnSafely(entity,new Vector3d(n.getDouble("X"),n.getDouble("Y"),n.getDouble("Z")),entity.level);
        entity.setNoGravity(n.getBoolean("Gravity"));entity.fallDistance=0;entity.getPersistentData().remove(RECOVERY);clear(entity);
    }
    private static void clear(LivingEntity entity){
        CompoundNBT n=SaiyanState.get(entity);
        for(String key:new String[]{"Finisher","FinisherPhase","FinisherStart","FinisherEnd","FinisherYaw","FinisherSpace","FinisherMove","FinisherOwner","FinisherCaptor"})n.remove(key);
        sync(entity);
    }
    private static void sync(LivingEntity entity){if(entity instanceof PlayerEntity)SaiyanFeatures.sync((PlayerEntity)entity);}
}
