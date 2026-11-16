"""Compile the actual chain cast entity against focused world/combat fixtures."""
from pathlib import Path
import subprocess

ROOT = Path(__file__).resolve().parents[2]
OUT = ROOT / 'build/enkidu-miss-tests'
JAVA = Path('C:/Users/th3wyz/.jdks/corretto-1.8.0_482/bin')
SOURCE = ROOT / 'src/main/java/net/kazi/kazimod/entities/EnkiduVfxEntity.java'
fixtures = {}


def put(name, code):
    fixtures[name] = 'package ' + name.rsplit('.', 1)[0] + ';\n' + code


put('net.minecraft.util.math.vector.Vector3d', '''public class Vector3d {
 public final double x,y,z;public Vector3d(double x,double y,double z){this.x=x;this.y=y;this.z=z;}
 public Vector3d add(double x,double y,double z){return new Vector3d(this.x+x,this.y+y,this.z+z);}
 public double distanceToSqr(Vector3d p){double a=x-p.x,b=y-p.y,c=z-p.z;return a*a+b*b+c*c;}
}''')
put('net.minecraft.util.math.AxisAlignedBB', '''public class AxisAlignedBB {
 public final double minX,minY,minZ,maxX,maxY,maxZ;
 public AxisAlignedBB(double a,double b,double c,double d,double e,double f){minX=a;minY=b;minZ=c;maxX=d;maxY=e;maxZ=f;}
 public AxisAlignedBB(net.minecraft.util.math.vector.Vector3d a,net.minecraft.util.math.vector.Vector3d b){this(a.x,a.y,a.z,b.x,b.y,b.z);}
 public AxisAlignedBB inflate(double x,double y,double z){return new AxisAlignedBB(minX-x,minY-y,minZ-z,maxX+x,maxY+y,maxZ+z);}
}''')
put('net.minecraft.util.math.MathHelper', '''public class MathHelper {
 public static float clamp(float n,float lo,float hi){return Math.max(lo,Math.min(hi,n));}
 public static double lerp(double t,double a,double b){return a+(b-a)*t;}
}''')
put('net.minecraft.nbt.CompoundNBT', '''public class CompoundNBT {
 private final java.util.Map<String,Object> values=new java.util.HashMap<>();
 public void putInt(String k,int v){values.put(k,v);}public int getInt(String k){return ((Number)values.getOrDefault(k,0)).intValue();}
 public void putLong(String k,long v){values.put(k,v);}public long getLong(String k){return ((Number)values.getOrDefault(k,0L)).longValue();}
 public void putFloat(String k,float v){values.put(k,v);}public float getFloat(String k){return ((Number)values.getOrDefault(k,0f)).floatValue();}
 public void putDouble(String k,double v){values.put(k,v);}public double getDouble(String k){return ((Number)values.getOrDefault(k,0d)).doubleValue();}
 public void putBoolean(String k,boolean v){values.put(k,v);}public boolean getBoolean(String k){return Boolean.TRUE.equals(values.get(k));}
 public void put(String k,ListNBT v){values.put(k,v);}public ListNBT getList(String k,int type){return (ListNBT)values.getOrDefault(k,new ListNBT());}
 public CompoundNBT copy(){CompoundNBT n=new CompoundNBT();values.forEach((k,v)->n.values.put(k,v instanceof ListNBT?((ListNBT)v).copy():v));return n;}
}''')
put('net.minecraft.nbt.ListNBT', '''public class ListNBT extends java.util.ArrayList<CompoundNBT> {
 public CompoundNBT getCompound(int i){return get(i);}public ListNBT copy(){ListNBT n=new ListNBT();for(CompoundNBT v:this)n.add(v.copy());return n;}
}''')
put('net.minecraft.network.IPacket', 'public interface IPacket<T> {}')
put('net.minecraft.network.datasync.DataParameter', 'public class DataParameter<T> {}')
put('net.minecraft.network.datasync.DataSerializers', '''public class DataSerializers {
 public static final DataParameter<net.minecraft.nbt.CompoundNBT> COMPOUND_TAG=new DataParameter<>();public static final DataParameter<Float> FLOAT=new DataParameter<>();
}''')
put('net.minecraft.network.datasync.EntityDataManager', '''public class EntityDataManager {
 private final java.util.Map<DataParameter<?>,Object> values=new java.util.HashMap<>();
 public static <T>DataParameter<T> defineId(Class<?> owner,DataParameter<T> serializer){return new DataParameter<>();}
 public <T>void define(DataParameter<T> key,T value){values.put(key,value);}public <T>void set(DataParameter<T> key,T value){values.put(key,value);}
 @SuppressWarnings("unchecked") public <T>T get(DataParameter<T> key){return (T)values.get(key);}
}''')
put('net.minecraft.util.DamageSource', 'public class DamageSource {}')
put('net.minecraft.util.SoundCategory', 'public enum SoundCategory {PLAYERS}')
put('net.minecraft.util.SoundEvent', 'public class SoundEvent {public final String name;public SoundEvent(String name){this.name=name;}}')
put('net.minecraft.util.SoundEvents', 'public class SoundEvents {' + ''.join(
    'public static final SoundEvent ' + name + '=new SoundEvent("' + name + '");'
    for name in ('BEACON_ACTIVATE', 'CHAIN_PLACE', 'TRIDENT_RETURN', 'CHAIN_BREAK',
                 'BEACON_DEACTIVATE', 'CHAIN_STEP', 'CHAIN_HIT', 'ANVIL_LAND')) + '}')
put('net.minecraft.entity.EntityType', 'public class EntityType<T> {}')
put('net.minecraft.world.World', '''public class World {
 public boolean isClientSide,wall;public long gameTime;public final java.util.List<net.minecraft.entity.Entity> entities=new java.util.ArrayList<>();
 public final java.util.List<String> sounds=new java.util.ArrayList<>();public long getGameTime(){return gameTime;}
 public net.minecraft.entity.Entity getEntity(int id){for(net.minecraft.entity.Entity e:entities)if(e.getId()==id)return e;return null;}
 public void playSound(Object listener,double x,double y,double z,net.minecraft.util.SoundEvent sound,net.minecraft.util.SoundCategory category,float volume,float pitch){sounds.add(sound.name);}
 public void playSound(Object listener,Object position,net.minecraft.util.SoundEvent sound,net.minecraft.util.SoundCategory category,float volume,float pitch){sounds.add(sound.name);}
}''')
put('net.minecraft.entity.Entity', '''public class Entity {
 private static int next;private final int id=++next;public net.minecraft.world.World level;public double x,y,z,xOld,yOld,zOld;
 public boolean noPhysics,noCulling,removed,alive=true;public int tickCount;protected final net.minecraft.network.datasync.EntityDataManager entityData=new net.minecraft.network.datasync.EntityDataManager();
 public Entity(EntityType<?> type,net.minecraft.world.World world){level=world;defineSynchedData();world.entities.add(this);}
 public int getId(){return id;}public boolean isAlive(){return alive&&!removed;}public void remove(){removed=true;}public void tick(){tickCount++;}
 public double getX(){return x;}public double getY(){return y;}public double getZ(){return z;}public void setPos(double x,double y,double z){this.x=x;this.y=y;this.z=z;}
 public net.minecraft.util.math.vector.Vector3d position(){return new net.minecraft.util.math.vector.Vector3d(x,y,z);}
 public Object blockPosition(){return position();}public double distanceToSqr(Entity target){return position().distanceToSqr(target.position());}
 public void setNoGravity(boolean v){}public void setBoundingBox(net.minecraft.util.math.AxisAlignedBB box){}
 public float getBbWidth(){return .6f;}public float getBbHeight(){return 1.8f;}
 public net.minecraft.util.math.AxisAlignedBB getBoundingBox(){return new net.minecraft.util.math.AxisAlignedBB(x-.3,y,z-.3,x+.3,y+1.8,z+.3);}
 protected void defineSynchedData(){}protected void readAdditionalSaveData(net.minecraft.nbt.CompoundNBT tag){}protected void addAdditionalSaveData(net.minecraft.nbt.CompoundNBT tag){}
 public boolean isPickable(){return true;}public boolean hurt(net.minecraft.util.DamageSource source,float amount){return false;}
 public net.minecraft.network.IPacket<?> getAddEntityPacket(){return null;}public boolean shouldRenderAtSqrDistance(double d){return true;}
}''')
put('net.minecraft.entity.LivingEntity', '''public class LivingEntity extends Entity {
 public boolean hostile=true,dodging,passenger,immune;public int boundApplications,floatApplications,strikes;public long boundUntil;
 public net.kazi.kazimod.abilities.GoruRework.EnkiduAbility equipped;
 private net.minecraft.util.math.vector.Vector3d delta=new net.minecraft.util.math.vector.Vector3d(0,0,0);
 public LivingEntity(net.minecraft.world.World world){super(new EntityType<>(),world);}
 public net.minecraft.util.math.vector.Vector3d getEyePosition(float partial){return position().add(0,1.6,0);}
 public boolean isPassenger(){return passenger;}public void stopRiding(){passenger=false;}public void setJumping(boolean b){}
 public void addEffect(net.minecraft.potion.EffectInstance effect){if(!immune){boundApplications++;boundUntil=level.gameTime+effect.duration;}}
 public boolean hasEffect(Object effect){return level.gameTime<boundUntil;}
 public net.minecraft.util.math.vector.Vector3d getDeltaMovement(){return delta;}public void setDeltaMovement(double x,double y,double z){delta=new net.minecraft.util.math.vector.Vector3d(x,y,z);}
}''')
put('net.minecraft.entity.player.PlayerEntity', 'public class PlayerEntity extends net.minecraft.entity.LivingEntity {public PlayerEntity(net.minecraft.world.World world){super(world);}}')
put('net.minecraft.potion.EffectInstance', 'public class EffectInstance {public final int duration;public EffectInstance(Object effect,int ticks,int amplifier,boolean ambient,boolean visible,boolean icon){duration=ticks;}}')
put('net.kazi.kazimod.init.KaziEffects', 'public class KaziEffects {public static final Ref HEAVEN_BOUND=new Ref();public static class Ref {public Object get(){return this;}}}')
put('net.minecraftforge.fml.network.NetworkHooks', 'public class NetworkHooks {public static net.minecraft.network.IPacket<?> getEntitySpawningPacket(net.minecraft.entity.Entity entity){return null;}}')
put('xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper', '''public class AbilityHelper {
 public static boolean isDodging(net.minecraft.entity.LivingEntity target){return target.dodging;}
 public static void slowEntityFall(net.minecraft.entity.LivingEntity target,int ticks){target.floatApplications++;}
 public static void setDeltaMovement(net.minecraft.entity.LivingEntity target,double x,double y,double z){target.setDeltaMovement(x,y,z);}
}''')
put('xyz.pixelatedw.mineminenomi.data.entity.ability.AbilityDataCapability', '''public class AbilityDataCapability {
 private final net.minecraft.entity.LivingEntity owner;private AbilityDataCapability(net.minecraft.entity.LivingEntity owner){this.owner=owner;}
 public static AbilityDataCapability get(net.minecraft.entity.LivingEntity owner){return new AbilityDataCapability(owner);}
 public net.kazi.kazimod.abilities.GoruRework.EnkiduAbility getEquippedAbility(Object core){return owner.equipped;}
}''')
put('net.kazi.kazimod.abilities.GoruRework.EnkiduAbility', '''public class EnkiduAbility {
 public static final Object INSTANCE=new Object();public static final double RANGE=64,AREA_RADIUS=12,AREA_HALF_HEIGHT=8,ABSOLUTE_RADIUS=40;public static final int MAX_AREA_TARGETS=8;
 public enum Mode {SINGLE(5),AREA(10),COUNTER(5),ABSOLUTE(10);public final int chargeTicks,bindTicks=40;Mode(int ticks){chargeTicks=ticks;}}
 public net.kazi.kazimod.entities.EnkiduVfxEntity cast;
 public boolean ownsCast(net.kazi.kazimod.entities.EnkiduVfxEntity v){return cast==v;}
 public boolean strike(net.minecraft.entity.LivingEntity owner,net.minecraft.entity.LivingEntity target,Mode mode){target.strikes++;return true;}
 public static boolean isEnemy(net.minecraft.entity.LivingEntity owner,net.minecraft.entity.LivingEntity target){return owner!=null&&target!=null&&target!=owner&&owner.isAlive()&&target.isAlive()&&target.level==owner.level&&target.hostile;}
 public static boolean clearPath(net.minecraft.entity.LivingEntity owner,net.minecraft.util.math.vector.Vector3d from,net.minecraft.util.math.vector.Vector3d to){return !owner.level.wall;}
 public static boolean insideArea(net.minecraft.util.math.vector.Vector3d center,net.minecraft.entity.LivingEntity target){double x=target.x-center.x,z=target.z-center.z;return x*x+z*z<=AREA_RADIUS*AREA_RADIUS&&target.getBoundingBox().maxY>=center.y-AREA_HALF_HEIGHT&&target.y<=center.y+AREA_HALF_HEIGHT;}
 public static java.util.List<net.minecraft.entity.LivingEntity> areaTargets(net.minecraft.entity.LivingEntity owner,net.minecraft.util.math.vector.Vector3d center){java.util.List<net.minecraft.entity.LivingEntity> result=new java.util.ArrayList<>();for(net.minecraft.entity.Entity e:owner.level.entities)if(e instanceof net.minecraft.entity.LivingEntity){net.minecraft.entity.LivingEntity target=(net.minecraft.entity.LivingEntity)e;if(isEnemy(owner,target)&&insideArea(center,target)&&!owner.level.wall&&result.size()<MAX_AREA_TARGETS)result.add(target);}return result;}
 public static boolean isAbsoluteTarget(net.minecraft.entity.LivingEntity owner,net.minecraft.entity.LivingEntity target){return isEnemy(owner,target)&&owner.distanceToSqr(target)<=ABSOLUTE_RADIUS*ABSOLUTE_RADIUS;}
 public static java.util.List<net.minecraft.entity.LivingEntity> absoluteTargets(net.minecraft.entity.LivingEntity owner){java.util.List<net.minecraft.entity.LivingEntity> result=new java.util.ArrayList<>();for(net.minecraft.entity.Entity e:owner.level.entities)if(e instanceof net.minecraft.entity.LivingEntity&&isAbsoluteTarget(owner,(net.minecraft.entity.LivingEntity)e))result.add((net.minecraft.entity.LivingEntity)e);return result;}
}''')

paths = []
for name, code in fixtures.items():
    path = OUT / 'fixtures' / (name.replace('.', '/') + '.java')
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(code, encoding='utf-8')
    paths.append(str(path))
classes = OUT / 'classes'
classes.mkdir(parents=True, exist_ok=True)
paths.extend([str(SOURCE), str(Path(__file__).with_name('EnkiduMissTest.java'))])
subprocess.run([str(JAVA / 'javac.exe'), '-encoding', 'UTF-8', '-d', str(classes), *paths], check=True)
subprocess.run([str(JAVA / 'java.exe'), '-cp', str(classes), 'EnkiduMissTest'], check=True)
