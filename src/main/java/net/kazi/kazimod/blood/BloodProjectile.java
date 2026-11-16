package net.kazi.kazimod.blood;

import java.util.*;
import net.kazi.kazimod.petal.PetalFeatures;
import net.minecraft.entity.*;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.datasync.*;
import net.minecraft.util.math.*;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.entities.projectiles.AbilityProjectileEntity;

public final class BloodProjectile extends AbilityProjectileEntity {
    private static final DataParameter<Boolean> STEERED=EntityDataManager.defineId(BloodProjectile.class,DataSerializers.BOOLEAN);
    private static final DataParameter<CompoundNBT> ORIGIN=EntityDataManager.defineId(BloodProjectile.class,DataSerializers.COMPOUND_TAG);
    private static final DataParameter<Integer> KIND=EntityDataManager.defineId(BloodProjectile.class,DataSerializers.INT),
            FIRING_TICKS=EntityDataManager.defineId(BloodProjectile.class,DataSerializers.INT),
            PHASE=EntityDataManager.defineId(BloodProjectile.class,DataSerializers.INT),
            START=EntityDataManager.defineId(BloodProjectile.class,DataSerializers.INT),
            PHASE_AT=EntityDataManager.defineId(BloodProjectile.class,DataSerializers.INT);
    private final Set<UUID> hit=new HashSet<>();
    private UUID receipt,original;
    private Vector3d curveAim;
    private int curveTicks;
    private boolean curved,processing;
    private LivingEntity grabbed,homingTarget;
    public BloodProjectile(EntityType<?> type,World world){super(type,world);}
    public BloodProjectile(LivingEntity p,int kind,UUID receipt,float damage){
        super(BloodFeatures.PROJECTILE.get(),p.level,p,BloodAbility.core(kind));
        this.receipt=receipt;original=p.getUUID();
        entityData.set(KIND,kind);entityData.set(PHASE,kind==1?0:1);
        entityData.set(START,(int)p.level.getGameTime());entityData.set(PHASE_AT,(int)p.level.getGameTime());
        entityData.set(STEERED,kind==0);
        entityData.set(FIRING_TICKS,damage>=34?60:40);
        setDamage(damage*1.5F);setGravity(0);setMaxLife(kind==1?96:kind==2?42:kind==4?45:firingTicks()+12);
        setAffectedByImbuing();setProjectile();setEntityCollisionSize(kind==2?1.25:.35);setBlockCollisionSize(.15);setCollideWithBlocks(true);
        Vector3d origin=p.getEyePosition(1).add(0,-.35,0);
        Vector3d palm=origin.add(p.getLookAngle().scale(.65));
        CompoundNBT data=new CompoundNBT();data.putDouble("X",palm.x);data.putDouble("Y",palm.y);data.putDouble("Z",palm.z);
        Vector3d direction=p.getLookAngle();data.putDouble("DX",direction.x);data.putDouble("DY",direction.y);data.putDouble("DZ",direction.z);entityData.set(ORIGIN,data);
        setPos(origin.x,origin.y,origin.z);
        setDeltaMovement(p.getLookAngle().scale(kind==2?1.15:kind==4?1.6:0));
        if(kind==4){BloodFeatures.state(p).putInt("TetherId",getId());BloodFeatures.state(p).putLong("TetherUntil",p.level.getGameTime()+45);BloodFeatures.sync(p);}
        if(kind==1){BloodFeatures.state(p).putInt("NovaId",getId());BloodFeatures.state(p).putLong("NovaHoldUntil",p.level.getGameTime()+40);BloodFeatures.sync(p);}
    }
    @Override public void defineSynchedData(){super.defineSynchedData();entityData.define(STEERED,false);entityData.define(FIRING_TICKS,60);entityData.define(KIND,0);entityData.define(PHASE,1);entityData.define(START,0);entityData.define(PHASE_AT,0);entityData.define(ORIGIN,new CompoundNBT());}
    public boolean steered(){return kind()==0&&entityData.get(STEERED);}
    public int firingTicks(){return entityData.get(FIRING_TICKS);}
    private double beamReach(float partial){return Math.min(firingTicks()==60?90:50,(age(partial)+1)*5);}
    public Vector3d beamStart(float partial){
        LivingEntity owner=getThrower();
        return steered()&&phase()==1&&owner!=null?owner.getEyePosition(level.isClientSide?partial:1).add(0,-.35,0):origin();
    }
    public Vector3d beamDirection(float partial){
        LivingEntity owner=getThrower();
        if(!steered()||phase()!=1||owner==null)return visualDirection();
        if(!level.isClientSide)return owner.getLookAngle().normalize();
        return Vector3d.directionFromRotation(MathHelper.lerp(partial,owner.xRotO,owner.xRot),
                owner.yRotO+MathHelper.wrapDegrees(owner.yRot-owner.yRotO)*partial).normalize();
    }
    public Vector3d beamEnd(float partial){
        Vector3d from=beamStart(partial),end=from.add(beamDirection(partial).scale(beamReach(partial)));
        return level.clip(new RayTraceContext(from,end,RayTraceContext.BlockMode.COLLIDER,RayTraceContext.FluidMode.NONE,this)).getLocation();
    }
    public void stopFiring(){if(!level.isClientSide&&steered()&&phase()==1){phase(2);setDeltaMovement(Vector3d.ZERO);setLife(12);}}
    public Vector3d origin(){CompoundNBT n=entityData.get(ORIGIN);return new Vector3d(n.getDouble("X"),n.getDouble("Y"),n.getDouble("Z"));}
    public Vector3d visualDirection(){
        if(kind()==1&&phase()==0&&getThrower()!=null)return getThrower().getLookAngle();
        if(phase()==1&&getDeltaMovement().lengthSqr()>.01)return getDeltaMovement().normalize();
        CompoundNBT n=entityData.get(ORIGIN);Vector3d d=new Vector3d(n.getDouble("DX"),n.getDouble("DY"),n.getDouble("DZ"));
        return d.lengthSqr()>.01?d.normalize():new Vector3d(0,0,1);
    }
    public Vector3d visualPosition(float partial){
        CompoundNBT n=entityData.get(ORIGIN);
        if(phase()==2&&n.contains("StopX"))return new Vector3d(n.getDouble("StopX"),n.getDouble("StopY"),n.getDouble("StopZ"));
        return getPosition(partial);
    }
    public void aim(Vector3d direction,double speed){
        if(kind()==0)entityData.set(STEERED,false);
        direction=direction.normalize();setDeltaMovement(direction.scale(speed));
        CompoundNBT n=entityData.get(ORIGIN).copy();n.putDouble("DX",direction.x);n.putDouble("DY",direction.y);n.putDouble("DZ",direction.z);
        if(kind()==0){Vector3d palm=position().add(direction.scale(.65));n.putDouble("X",palm.x);n.putDouble("Y",palm.y);n.putDouble("Z",palm.z);}
        entityData.set(ORIGIN,n);
    }
    public int kind(){return entityData.get(KIND);} public int phase(){return entityData.get(PHASE);}
    public float age(float partial){return level.getGameTime()-entityData.get(START)+partial;}
    public float phaseAge(float partial){return level.getGameTime()-entityData.get(PHASE_AT)+partial;}
    public UUID originalOwner(){return original;}
    private void phase(int phase){
        if(phase==2){
            CompoundNBT n=entityData.get(ORIGIN).copy();Vector3d d=visualDirection();
            n.putDouble("DX",d.x);n.putDouble("DY",d.y);n.putDouble("DZ",d.z);
            boolean anchored=steered()&&n.contains("EndX");
            n.putDouble("StopX",anchored?n.getDouble("EndX"):getX());
            n.putDouble("StopY",anchored?n.getDouble("EndY"):getY());
            n.putDouble("StopZ",anchored?n.getDouble("EndZ"):getZ());entityData.set(ORIGIN,n);
        }
        entityData.set(PHASE,phase);entityData.set(PHASE_AT,(int)level.getGameTime());
    }
    public void launch(){
        if(level.isClientSide||kind()!=1||phase()!=0||getThrower()==null)return;
        aim(getThrower().getLookAngle(),.85);phase(1);gesture(1);
    }
    private void gesture(int gesture){
        LivingEntity owner=getThrower();if(owner==null)return;
        BloodFeatures.state(owner).putLong("NovaHoldUntil",0);
        BloodFeatures.state(owner).putInt("NovaGesture",gesture);BloodFeatures.state(owner).putLong("NovaAt",level.getGameTime());BloodFeatures.sync(owner);
    }
    public void curve(){if(kind()!=2||curved||getThrower()==null)return;curved=true;curveTicks=8;homingTarget=null;curveAim=getThrower().getLookAngle().normalize();}
    private boolean trackable(LivingEntity owner,LivingEntity target,double range){
        if(target==null||!target.isAlive()||target.level!=level||!PetalFeatures.canHit(owner,target))return false;
        LivingEntity body=PetalFeatures.bodyOwner(target);
        if(body==null||hit.contains(body.getUUID()))return false;
        Vector3d center=target.getBoundingBox().getCenter();
        return center.distanceToSqr(position())<=range*range
                &&level.clip(new RayTraceContext(position(),center,RayTraceContext.BlockMode.COLLIDER,RayTraceContext.FluidMode.NONE,this)).getType()!=RayTraceResult.Type.BLOCK;
    }
    private void home(LivingEntity owner){
        if(kind()!=1&&kind()!=2&&kind()!=4)return;
        if(!trackable(owner,homingTarget,64))homingTarget=null;
        Vector3d velocity=getDeltaMovement();double speed=velocity.length();
        if(speed<.01)return;
        Vector3d forward=velocity.scale(1/speed);
        if(homingTarget==null&&tickCount%5==0){
            LivingEntity aimed=PetalFeatures.target(owner,48);
            if(trackable(owner,aimed,48))homingTarget=aimed;
            else{
                double best=Double.MAX_VALUE;
                for(LivingEntity candidate:level.getEntitiesOfClass(LivingEntity.class,getBoundingBox().inflate(48),t->PetalFeatures.canHit(owner,t))){
                    Vector3d delta=candidate.getBoundingBox().getCenter().subtract(position());
                    double alignment=forward.dot(delta.normalize());
                    double score=(1-alignment)*48+delta.length();
                    if(alignment>=.35&&score<best&&trackable(owner,candidate,48)){homingTarget=candidate;best=score;}
                }
            }
        }
        if(homingTarget==null)return;
        Vector3d delta=homingTarget.getBoundingBox().getCenter().subtract(position());
        if(delta.lengthSqr()<.0001)return;
        // Keep launch speed, with tighter steering near contact to avoid circling a target.
        Vector3d desired=delta.normalize();
        Vector3d direction=delta.lengthSqr()<16?desired:forward.scale(.65).add(desired.scale(.35)).normalize();
        setDeltaMovement(direction.scale(speed));hasImpulse=true;
    }
    @Override public void tick(){
        LivingEntity owner=getThrower();
        if(!level.isClientSide){
            if(owner==null||!owner.isAlive()||original!=null&&original.equals(owner.getUUID())&&!BloodFeatures.usable(owner)){remove();return;}
            if(steered()&&phase()==1){
                // A reflected shot must leave the caster rather than snap back to their hands.
                if(getDeltaMovement().lengthSqr()>.01||original!=null&&!original.equals(owner.getUUID())){
                    entityData.set(STEERED,false);
                }else if(age(0)>=firingTicks())stopFiring();
                else{
                    Vector3d from=beamStart(1),direction=beamDirection(1);
                    Vector3d to=from.add(direction.scale(beamReach(0)));
                    to=level.clip(new RayTraceContext(from,to,RayTraceContext.BlockMode.COLLIDER,RayTraceContext.FluidMode.NONE,this)).getLocation();
                    CompoundNBT n=entityData.get(ORIGIN).copy();
                    n.putDouble("X",from.x);n.putDouble("Y",from.y);n.putDouble("Z",from.z);
                    n.putDouble("DX",direction.x);n.putDouble("DY",direction.y);n.putDouble("DZ",direction.z);
                    n.putDouble("EndX",to.x);n.putDouble("EndY",to.y);n.putDouble("EndZ",to.z);entityData.set(ORIGIN,n);
                    // Track the stream at its caster, even when its tip leaves the client's view-distance range.
                    setPos(from.x,from.y,from.z);
                    sweep(from,to);
                    if(isAlive())super.tick();
                    return;
                }
            }
            if(kind()==1&&phase()==0){
                Vector3d start=owner.getEyePosition(1), end=start.add(owner.getLookAngle().scale(2.3));
                Vector3d at=level.clip(new RayTraceContext(start,end,RayTraceContext.BlockMode.COLLIDER,RayTraceContext.FluidMode.NONE,this)).getLocation();
                setPos(at.x,at.y,at.z);setDeltaMovement(Vector3d.ZERO);
                if(age(0)>=38)launch();
            }
            if(kind()==1&&phase()==1&&phaseAge(0)>=24)burst();
            if(phase()==2){setDeltaMovement(Vector3d.ZERO);if(phaseAge(0)>12){remove();return;}}
            if(kind()==4&&phase()==3){pull(owner);if(!isAlive())return;}
            if(phase()==1){
                if(kind()==0&&getDeltaMovement().lengthSqr()>.01){
                    CompoundNBT n=entityData.get(ORIGIN);Vector3d previous=new Vector3d(n.getDouble("DX"),n.getDouble("DY"),n.getDouble("DZ"));
                    Vector3d direction=getDeltaMovement().normalize();
                    if(direction.dot(previous)<.98){
                        n=n.copy();n.putDouble("DX",direction.x);n.putDouble("DY",direction.y);n.putDouble("DZ",direction.z);
                        n.putDouble("X",getX());n.putDouble("Y",getY());n.putDouble("Z",getZ());entityData.set(ORIGIN,n);
                    }
                }
                if(curveTicks>0){setDeltaMovement(getDeltaMovement().normalize().scale(.72).add(curveAim.scale(.28)).normalize().scale(1.15));curveTicks--;}
                else home(owner);
                sweep(position(),position().add(getDeltaMovement()));
                if(!isAlive())return;
            }
        }
        super.tick();
    }
    private void sweep(Vector3d from,Vector3d end){
        BlockRayTraceResult block=level.clip(new RayTraceContext(from,end,RayTraceContext.BlockMode.COLLIDER,RayTraceContext.FluidMode.NONE,this));
        Vector3d to=block.getLocation(); double radius=kind()==2?.7:.22;
        List<LivingEntity> targets=level.getEntitiesOfClass(LivingEntity.class,new AxisAlignedBB(from,to).inflate(radius),t->getThrower()!=null&&PetalFeatures.canHit(getThrower(),t));
        targets.sort(Comparator.comparingDouble(t->t.getBoundingBox().getCenter().distanceToSqr(from)));
        for(LivingEntity part:targets){
            AxisAlignedBB box=part.getBoundingBox().inflate(radius);
            Optional<Vector3d> contact=box.clip(from,to);
            if(!box.contains(from)&&!contact.isPresent())continue;
            Vector3d at=contact.orElse(from);
            if(net.minecraftforge.event.ForgeEventFactory.onProjectileImpact(this,new EntityRayTraceResult(part,at)))return;
            if(!isAlive())return;
            if(kind()==1){setPos(at.x,at.y,at.z);burst();return;}
            LivingEntity body=PetalFeatures.bodyOwner(part);
            if(body==null||hit.contains(body.getUUID()))continue;
            if(steered()&&hit.size()>=(firingTicks()==60?3:1))continue;
            boolean landed=strike(body,getDamage());
            hit.add(body.getUUID());
            if(kind()==4){
                if(landed){grabbed=body;setPos(at.x,at.y,at.z);phase(3);setDeltaMovement(Vector3d.ZERO);setLife(24);}
                else remove();
                return;
            }
            if(kind()==0&&!steered()&&hit.size()>=(firingTicks()==60?3:1)){setPos(at.x,at.y,at.z);phase(2);setLife(10);setDeltaMovement(Vector3d.ZERO);return;}
        }
        if(block.getType()==RayTraceResult.Type.BLOCK)impactBlock(block);
    }
    private boolean strike(LivingEntity t,float amount){
        LivingEntity owner=getThrower();
        if(owner==null||!PetalFeatures.canHit(owner,t))return false;
        xyz.pixelatedw.mineminenomi.api.protection.ProtectedArea area=xyz.pixelatedw.mineminenomi.data.world.ProtectedAreasData.get(level).getProtectedArea(t.blockPosition().getX(),t.blockPosition().getY(),t.blockPosition().getZ());
        if(area!=null&&!area.canUseAbility(getParent()))return false;
        float before=t.getHealth()+t.getAbsorptionAmount();
        boolean result=t.hurt(getDamageSource(),amount);
        boolean actual=result&&t.getHealth()+t.getAbsorptionAmount()<before;
        if(actual)BloodFeatures.applyHitEffects(t);
        if(actual&&kind()==2)t.addEffect(new net.minecraft.potion.EffectInstance(xyz.pixelatedw.mineminenomi.init.ModEffects.BLEEDING.get(),60,0,false,false));
        if(actual&&original!=null&&original.equals(owner.getUUID()))BloodLedger.hit(owner,receipt);
        return actual;
    }
    public void burst(){
        if(level.isClientSide||kind()!=1||phase()>=2||processing)return;
        processing=true;phase(2);setDeltaMovement(Vector3d.ZERO);setLife(14);gesture(2);
        LivingEntity p=getThrower();
        if(p!=null)for(LivingEntity part:level.getEntitiesOfClass(LivingEntity.class,getBoundingBox().inflate(5),t->PetalFeatures.canHit(p,t))){
            LivingEntity body=PetalFeatures.bodyOwner(part);
            if(body==null||hit.contains(body.getUUID()))continue;
            Vector3d at=part.getBoundingBox().getCenter();
            if(at.distanceToSqr(position())>25||level.clip(new RayTraceContext(position(),at,RayTraceContext.BlockMode.COLLIDER,RayTraceContext.FluidMode.NONE,this)).getType()==RayTraceResult.Type.BLOCK)continue;
            hit.add(body.getUUID());strike(body,getDamage());
        }
        processing=false;
    }
    private void impactBlock(BlockRayTraceResult b){
        if(steered())return;
        if(phase()!=1)return;
        Vector3d at=b.getLocation().subtract(getDeltaMovement().normalize().scale(.05));setPos(at.x,at.y,at.z);
        if(kind()==1)burst();
        else if(kind()==4){phase(3);setDeltaMovement(Vector3d.ZERO);setLife(24);if(getThrower()!=null)BloodLedger.neutral(getThrower(),receipt);}
        else {phase(2);setDeltaMovement(Vector3d.ZERO);setLife(10);}
    }
    private void pull(LivingEntity p){
        if(grabbed!=null){if(!grabbed.isAlive()||!PetalFeatures.canHit(p,grabbed)){remove();return;}Vector3d at=grabbed.getBoundingBox().getCenter();setPos(at.x,at.y,at.z);}
        Vector3d delta=position().subtract(p.getBoundingBox().getCenter());
        if(delta.length()<1.5){
            if(grabbed!=null&&p.canSee(grabbed)){hit.remove(grabbed.getUUID());strike(grabbed,18);}
            AbilityHelper.setDeltaMovement(p,Vector3d.ZERO);remove();return;
        }
        Vector3d v=delta.normalize().scale(BloodFeatures.scale(p)?1.4:1.05);
        if(p.isOnGround()&&v.y<0)v=new Vector3d(v.x,0,v.z);
        if(phaseAge(0)>20||!p.level.noCollision(p,p.getBoundingBox().move(v))||level.clip(new RayTraceContext(p.getEyePosition(1),position(),RayTraceContext.BlockMode.COLLIDER,RayTraceContext.FluidMode.NONE,p)).getType()==RayTraceResult.Type.BLOCK){remove();return;}
        AbilityHelper.setDeltaMovement(p,v);p.fallDistance=0;
    }
    @Override public void onModHit(RayTraceResult hit){
        if(level.isClientSide||!isAlive())return;
        if(hit instanceof EntityRayTraceResult&&((EntityRayTraceResult)hit).getEntity() instanceof AbilityProjectileEntity)super.onModHit(hit);
        // Swept, block-clipped tests above own living collisions; the inherited pass probes one step ahead.
    }
    @Override protected void onHitBlock(BlockRayTraceResult hit){if(!level.isClientSide)impactBlock(hit);}
    @Override public void remove(){
        if(!level.isClientSide&&!removed&&getThrower()!=null&&original!=null&&original.equals(getThrower().getUUID()))BloodLedger.miss(getThrower(),receipt);
        if(!level.isClientSide&&kind()==4&&getThrower()!=null&&BloodFeatures.state(getThrower()).getInt("TetherId")==getId()){
            BloodFeatures.state(getThrower()).putLong("TetherUntil",0);BloodFeatures.sync(getThrower());
        }
        if(!level.isClientSide&&kind()==1&&getThrower()!=null&&BloodFeatures.state(getThrower()).getInt("NovaId")==getId()){
            BloodFeatures.state(getThrower()).putLong("NovaHoldUntil",0);BloodFeatures.sync(getThrower());
        }
        super.remove();
    }
    @Override public AxisAlignedBB getBoundingBoxForCulling(){return super.getBoundingBoxForCulling().inflate(kind()==0?10:kind()==4?30:7);}
}
