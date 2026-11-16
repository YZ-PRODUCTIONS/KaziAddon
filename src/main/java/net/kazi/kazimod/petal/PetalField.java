package net.kazi.kazimod.petal;
import java.util.*;
import net.minecraft.entity.*;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.IPacket;
import net.minecraft.network.datasync.*;
import net.minecraft.potion.*;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.*;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraftforge.fml.network.NetworkHooks;
public final class PetalField extends Entity {
 public static final int SENKEI_SWORD_INTERVAL=40;
 private static final DataParameter<Integer> KIND=EntityDataManager.defineId(PetalField.class,DataSerializers.INT),OWNER=EntityDataManager.defineId(PetalField.class,DataSerializers.INT),START=EntityDataManager.defineId(PetalField.class,DataSerializers.INT),LIFE=EntityDataManager.defineId(PetalField.class,DataSerializers.INT);
 private static final DataParameter<Float> SIZE=EntityDataManager.defineId(PetalField.class,DataSerializers.FLOAT),X=EntityDataManager.defineId(PetalField.class,DataSerializers.FLOAT),Y=EntityDataManager.defineId(PetalField.class,DataSerializers.FLOAT),Z=EntityDataManager.defineId(PetalField.class,DataSerializers.FLOAT),YAW=EntityDataManager.defineId(PetalField.class,DataSerializers.FLOAT);
 private static final DataParameter<CompoundNBT> TARGETS=EntityDataManager.defineId(PetalField.class,DataSerializers.COMPOUND_TAG);
 private final Set<UUID> hit=new HashSet<>(),captured=new HashSet<>(),damagedThisTick=new HashSet<>();private float health=48,power=30;private PetalAbility attack;private UUID ownerUUID;
 public PetalField(EntityType<?> type,World world){super(type,world);noPhysics=true;setNoGravity(true);}
 @Override protected void defineSynchedData(){entityData.define(TARGETS,new CompoundNBT());entityData.define(KIND,0);entityData.define(OWNER,-1);entityData.define(START,0);entityData.define(LIFE,20);entityData.define(SIZE,1F);entityData.define(X,0F);entityData.define(Y,0F);entityData.define(Z,0F);entityData.define(YAW,0F);}
 public static PetalField spawn(LivingEntity p,int kind,Vector3d pos,int life,float size){
  for(PetalField old:p.level.getEntitiesOfClass(PetalField.class,p.getBoundingBox().inflate(96),e->e.owner()==p&&e.kind()==kind))old.remove();
  PetalField e=new PetalField(PetalFeatures.FIELD.get(),p.level);e.entityData.set(KIND,kind);e.entityData.set(OWNER,p.getId());e.ownerUUID=p.getUUID();e.entityData.set(START,(int)p.level.getGameTime());e.entityData.set(LIFE,life);e.entityData.set(SIZE,size);e.entityData.set(YAW,p.yRot);e.health=kind==9?96:48;e.setPos(pos.x,pos.y,pos.z);e.end(pos);p.level.addFreshEntity(e);return e;
 }
 public int kind(){return entityData.get(KIND);}public int life(){return entityData.get(LIFE);}public float size(){return entityData.get(SIZE);}public float yaw(){return entityData.get(YAW);}public float age(float partial){return Math.max(0,(int)level.getGameTime()-entityData.get(START)+partial);}
 public LivingEntity owner(){Entity e=level.getEntity(entityData.get(OWNER));return e instanceof LivingEntity&&(ownerUUID==null||ownerUUID.equals(e.getUUID()))?(LivingEntity)e:null;}
 public Vector3d end(){return new Vector3d(entityData.get(X),entityData.get(Y),entityData.get(Z));}public void end(Vector3d v){entityData.set(X,(float)v.x);entityData.set(Y,(float)v.y);entityData.set(Z,(float)v.z);}public void setPower(float p){power=p;}
 @Override public void tick(){super.tick();if(kind()==4||kind()==9){setBoundingBox(new AxisAlignedBB(getX()-size(),getY()-size()*.5,getZ()-size(),getX()+size(),getY()+size()*1.5,getZ()+size()));level.increaseMaxEntityRadius(size());}if(level.isClientSide)return;damagedThisTick.clear();LivingEntity p=owner();int age=(int)age(0),kind=kind();
  if(p==null||!PetalFeatures.usable(p)||!PetalState.scattered(p)||p.distanceToSqr(this)>96*96||age>=life()||(kind==7||kind==8||kind==9)&&!PetalState.bankai(p)){remove();return;}
  if(kind==1||kind==2||kind==8){setPos(p.getX(),p.getY(),p.getZ());entityData.set(YAW,p.yRot);end(PetalFeatures.aim(p,kind==1?24:5));}
  if(kind==1&&age>=8&&age%10==0){Vector3d a=p.getEyePosition(1),b=end();for(LivingEntity t:enemies(p,new AxisAlignedBB(a,b).inflate(.85)))if(t.getBoundingBox().inflate(.65).clip(a,b).isPresent()&&p.canSee(t))hurt(p,t,PetalState.bankai(p)?7:5,1);}
  if(kind==4||kind==9){float radius=size();if(age==20)for(LivingEntity t:enemies(p,getBoundingBox().inflate(radius,2,radius)))if(t.position().distanceToSqr(position())<radius*radius&&p.canSee(t))captured.add(t.getUUID());
   if(age>=20){radius=kind==9?Math.max(1.5F,size()*(1-(age-20)/110F)):size();
    for(LivingEntity t:enemies(p,getBoundingBox().inflate(size()+1,3,size()+1)))if(captured.contains(t.getUUID())){
     double d=t.position().distanceTo(position());if(d>size()+1||kind==9&&!p.canSee(t)){captured.remove(t.getUUID());continue;}
     if(age%10==0)hurt(p,t,kind==9?6:1,4);t.addEffect(new EffectInstance(Effects.MOVEMENT_SLOWDOWN,6,1));
     if(d>radius-.4&&d<size()+1){t.setDeltaMovement(t.getDeltaMovement().add(position().subtract(t.position()).normalize().scale(.12)));t.hurtMarked=true;}
    }
   }
  }
  if(kind==5&&age==28)for(LivingEntity t:enemies(p,getBoundingBox().inflate(size(),4,size())))if(horizontalDistanceSqr(t)<=size()*size()&&p.canSee(t)){hurt(p,t,PetalState.bankai(p)?34:26,5);t.knockback(.7F,getX()-t.getX(),getZ()-t.getZ());}
  if(kind==7){
   if(horizontalDistanceSqr(p)>size()*size()||p.getY()<getY()+SenkeiBoundary.FLOOR-1||p.getY()>getY()+SenkeiBoundary.CEILING){PetalState.get(p).putLong("Senkei",0);remove();return;}
   if(age%SENKEI_SWORD_INTERVAL==0){Set<UUID> seen=new HashSet<>();net.minecraft.nbt.ListNBT ids=new net.minecraft.nbt.ListNBT();
    for(LivingEntity t:enemies(p,getBoundingBox().inflate(size(),10,size()))){LivingEntity body=PetalFeatures.bodyOwner(t);if(body!=null&&t.position().distanceToSqr(position())<=size()*size()&&p.canSee(t)&&seen.add(body.getUUID()))ids.add(net.minecraft.nbt.IntNBT.valueOf(t.getId()));}
    CompoundNBT data=new CompoundNBT();data.put("Ids",ids);entityData.set(TARGETS,data);
   }
   if(age%SENKEI_SWORD_INTERVAL==12)for(LivingEntity t:swordTargets())if(PetalFeatures.canHit(p,t)&&t.position().distanceToSqr(position())<=size()*size()&&p.canSee(t))hurt(p,t,8,7);
  }
  if(kind==8&&HakuteikenTiming.rushing(age)){Vector3d a=p.getEyePosition(1),b=end();for(LivingEntity t:enemies(p,new AxisAlignedBB(a,b).inflate(1.3)))if(t.getBoundingBox().inflate(1).clip(a,b).isPresent()&&p.canSee(t)&&!hit.contains(PetalFeatures.bodyOwner(t).getUUID())){hurt(p,t,power,8);t.knockback(1.2F,p.getX()-t.getX(),p.getZ()-t.getZ());}}
 }
 public List<LivingEntity> swordTargets(){List<LivingEntity> result=new ArrayList<>();net.minecraft.nbt.ListNBT ids=entityData.get(TARGETS).getList("Ids",3);for(int i=0;i<ids.size();i++){Entity e=level.getEntity(((net.minecraft.nbt.IntNBT)ids.get(i)).getAsInt());if(e instanceof LivingEntity&&e.isAlive())result.add((LivingEntity)e);}return result;}
 private List<LivingEntity> enemies(LivingEntity p,AxisAlignedBB box){return level.getEntitiesOfClass(LivingEntity.class,box,t->PetalFeatures.canHit(p,t));}
 private double horizontalDistanceSqr(Entity target){double x=target.getX()-getX(),z=target.getZ()-getZ();return x*x+z*z;}
 private void hurt(LivingEntity p,LivingEntity t,float amount,int move){LivingEntity owner=PetalFeatures.bodyOwner(t);if(owner==null||!damagedThisTick.add(owner.getUUID()))return;if(move==8&&!hit.add(owner.getUUID()))return;if(attack==null)attack=PetalAbility.CORES[move].createAbility();attack.hurt(p,owner,amount);}
 @Override public boolean isPickable(){return (kind()==4||kind()==9)&&age(0)>=20&&!removed;}
 @Override public boolean canBeCollidedWith(){return kind()!=4&&isPickable();}
 @Override public boolean hurt(DamageSource s,float amount){if(level.isClientSide||!isPickable()||!Float.isFinite(amount)||amount<=0)return false;if(s.getEntity()==owner())return false;health-=amount;if(health<=0)remove();return true;}
 @Override public AxisAlignedBB getBoundingBoxForCulling(){return super.getBoundingBox().inflate(kind()==7?size()+3:kind()==1?26:kind()==6?32:24);}
 @Override protected void readAdditionalSaveData(CompoundNBT n){remove();}@Override protected void addAdditionalSaveData(CompoundNBT n){}
 @Override public IPacket<?> getAddEntityPacket(){return NetworkHooks.getEntitySpawningPacket(this);}
}
