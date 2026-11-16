"""Compile the production field with adapted copies of the existing Shocker server fixtures."""
from pathlib import Path
import subprocess

ROOT = Path(__file__).resolve().parents[2]
OUT = ROOT / 'build/chaos-beam-entity-tests'
SRC = OUT / 'fixtures'
CLASSES = OUT / 'classes'
JAVA = Path('C:/Users/th3wyz/.jdks/corretto-1.8.0_482/bin')
sources = {str(p.relative_to(Path(__file__).parent / 'shocker_entity_fixtures')).replace('\\','/'): p.read_text()
           for p in (Path(__file__).parent / 'shocker_entity_fixtures').rglob('*.java')}

def put(name, code):
    sources[name.replace('.', '/') + '.java'] = 'package ' + name.rsplit('.',1)[0] + ';\n' + code

put('net.minecraft.nbt.CompoundNBT', '''
import java.util.*;
public class CompoundNBT {
 private final Map<String,Object> data=new HashMap<>();
 public void putLong(String k,long v){data.put(k,v);} public long getLong(String k){return data.containsKey(k)?((Number)data.get(k)).longValue():0;}
 public void putInt(String k,int v){data.put(k,v);} public int getInt(String k){return (int)getLong(k);}
 public void putDouble(String k,double v){data.put(k,v);} public double getDouble(String k){return data.containsKey(k)?((Number)data.get(k)).doubleValue():0;}
 public void putBoolean(String k,boolean v){data.put(k,v);} public boolean getBoolean(String k){return Boolean.TRUE.equals(data.get(k));}
 public boolean contains(String k){return data.containsKey(k);} public CompoundNBT copy(){CompoundNBT c=new CompoundNBT();c.data.putAll(data);return c;}
 public void remove(String k){data.remove(k);}
 public boolean equals(Object o){return o instanceof CompoundNBT&&data.equals(((CompoundNBT)o).data);} public int hashCode(){return data.hashCode();}
}''')
put('net.minecraft.util.math.vector.Vector3d', '''
public final class Vector3d {
 public static final Vector3d ZERO=new Vector3d(0,0,0);
 public final double x,y,z;
 public Vector3d(double x,double y,double z){this.x=x;this.y=y;this.z=z;}
 public Vector3d add(double x,double y,double z){return new Vector3d(this.x+x,this.y+y,this.z+z);}
 public Vector3d add(Vector3d v){return add(v.x,v.y,v.z);} public Vector3d subtract(Vector3d v){return add(-v.x,-v.y,-v.z);}
 public Vector3d scale(double s){return new Vector3d(x*s,y*s,z*s);} public double lengthSqr(){return x*x+y*y+z*z;}
 public Vector3d normalize(){double l=Math.sqrt(lengthSqr());return l<1e-10?new Vector3d(0,0,0):scale(1/l);}
 public double length(){return Math.sqrt(lengthSqr());}
 public double dot(Vector3d v){return x*v.x+y*v.y+z*v.z;} public double distanceTo(Vector3d v){return Math.sqrt(subtract(v).lengthSqr());}
 public double distanceToSqr(double x,double y,double z){double dx=this.x-x,dy=this.y-y,dz=this.z-z;return dx*dx+dy*dy+dz*dz;}
}''')
path='net/minecraft/util/math/AxisAlignedBB.java'
sources[path]=sources[path].replace('    public boolean intersects', '''
    public AxisAlignedBB(Vector3d a,Vector3d b){this(Math.min(a.x,b.x),Math.min(a.y,b.y),Math.min(a.z,b.z),Math.max(a.x,b.x),Math.max(a.y,b.y),Math.max(a.z,b.z));}
    public AxisAlignedBB inflate(double d){return new AxisAlignedBB(minX-d,minY-d,minZ-d,maxX+d,maxY+d,maxZ+d);}
    public boolean intersects''')
path='net/minecraft/entity/LivingEntity.java'
sources[path]=sources[path].replace('    public boolean alive', '''
    public boolean friendlyFaction;
    public final java.util.Set<Entity> allies=new java.util.HashSet<>();
    public int heals;public final java.util.List<Float> healAmounts=new java.util.ArrayList<>();
    public int invulnerableTime,observedHurtWindow=-1;public float armorMultiplier=1,magicMultiplier=1;public Runnable onDeath;
    public float incomingMultiplier=1,finalDamageMultiplier=1;
    public float lateDamageMultiplier=1,minimumHealthWritten=Float.POSITIVE_INFINITY;
    public boolean skipDamageEvents,wrapDamageSource;public int healthWrites,deathCallbacks;
    public final java.util.List<Float> actualDamageAmounts=new java.util.ArrayList<>();
    public net.minecraft.util.DamageSource lastDamageSource;
    public boolean isAlliedTo(Entity other){return other==this||allies.contains(other);}
    public void heal(float amount){heals++;healAmounts.add(amount);health=Math.min(maxHealth,health+amount);}
    public void setHealth(float value){healthWrites++;health=Math.max(0,Math.min(maxHealth,value));minimumHealthWritten=Math.min(minimumHealthWritten,health);}
    public float yRot,yRotO,xRot,xRotO,yHeadRot,yHeadRotO,yBodyRot,yBodyRotO;
    public float xxa,yya,zza; public boolean jumping,sprinting;
    public void setJumping(boolean value){jumping=value;}public void setSprinting(boolean value){sprinting=value;}
    public net.minecraft.util.math.vector.Vector3d getLookAngle(){double y=Math.toRadians(yRot),p=Math.toRadians(xRot);return new net.minecraft.util.math.vector.Vector3d(-Math.sin(y)*Math.cos(p),-Math.sin(p),Math.cos(y)*Math.cos(p));}
    public net.minecraft.util.math.vector.Vector3d getEyePosition(float p){return position().add(0,1.6,0);}
    public float getBbWidth(){return .6F;}
    @Override public boolean hurt(net.minecraft.util.DamageSource source,float amount){
     damageAttempts++;lastDamageSource=source;boolean recoil=net.kazi.kazimod.abilities.YagiAwakening.ChaosBeamRecoil.isRecoil(source);
     if(recoil)observedHurtWindow=invulnerableTime;
     if(invulnerable||recoil&&invulnerableTime>10)return false;
     successfulHits++;damageAmounts.add(amount);hitTimes.add(level.getGameTime());
     if(recoil)invulnerableTime=20;
     float dealt=amount*incomingMultiplier;
     if(!source.isBypassArmor())dealt*=armorMultiplier;if(!source.isBypassMagic())dealt*=magicMultiplier;
     net.minecraft.util.DamageSource finalSource=wrapDamageSource?new net.minecraft.util.DamageSource(source.getMsgId()):source;
     xyz.pixelatedw.mineminenomi.api.events.WyLivingDamageEvent nativeEvent=
       new xyz.pixelatedw.mineminenomi.api.events.WyLivingDamageEvent(this,finalSource,dealt);
     if(!skipDamageEvents)net.kazi.kazimod.abilities.YagiAwakening.ChaosBeamDamage.nativeDamage(nativeEvent);
     net.minecraftforge.event.entity.living.LivingDamageEvent forgeEvent=
       new net.minecraftforge.event.entity.living.LivingDamageEvent(this,finalSource,nativeEvent.getAmount()*finalDamageMultiplier);
     if(!skipDamageEvents)net.kazi.kazimod.abilities.YagiAwakening.ChaosBeamDamage.damage(forgeEvent);
     dealt=forgeEvent.getAmount()*lateDamageMultiplier;float before=health;
     testsupport.BeamHealthWriter.write(this,health-dealt,finalSource,amount);
     actualDamageAmounts.add(before-health);
     if(health<=0){alive=false;deathCallbacks++;if(onDeath!=null)onDeath.run();}
     return true;
    }
    public boolean alive''')
put('xyz.pixelatedw.mineminenomi.init.ModEntityPredicates', '''
public final class ModEntityPredicates {
 public static java.util.function.Predicate<net.minecraft.entity.Entity> getFriendlyFactions(net.minecraft.entity.LivingEntity owner){
  return entity->entity instanceof net.minecraft.entity.LivingEntity&&((net.minecraft.entity.LivingEntity)entity).friendlyFaction;
 }
}''')
for name in ['xyz.pixelatedw.mineminenomi.api.events.WyLivingDamageEvent',
             'net.minecraftforge.event.entity.living.LivingDamageEvent']:
    put(name, '''
public class %s {
 private final net.minecraft.entity.LivingEntity target;private final net.minecraft.util.DamageSource source;private float amount;
 public %s(net.minecraft.entity.LivingEntity target,net.minecraft.util.DamageSource source,float amount){this.target=target;this.source=source;this.amount=amount;}
 public net.minecraft.entity.LivingEntity getEntityLiving(){return target;}public net.minecraft.util.DamageSource getSource(){return source;}
 public float getAmount(){return amount;}public void setAmount(float value){amount=value;}
}''' % (name.rsplit('.',1)[1],name.rsplit('.',1)[1]))
put('net.minecraft.util.math.BlockPos', 'public class BlockPos { public final double z; public BlockPos(net.minecraft.util.math.vector.Vector3d v){z=v.z;} }')
put('net.minecraft.util.math.RayTraceResult', '''
public class RayTraceResult {
 public enum Type { MISS,BLOCK } private final Type type; private final net.minecraft.util.math.vector.Vector3d point;
 public RayTraceResult(Type type,net.minecraft.util.math.vector.Vector3d point){this.type=type;this.point=point;}
 public Type getType(){return type;} public net.minecraft.util.math.vector.Vector3d getLocation(){return point;}
}''')
put('net.minecraft.util.math.RayTraceContext', '''
public class RayTraceContext {
 public enum BlockMode {COLLIDER} public enum FluidMode {NONE}
 public final net.minecraft.util.math.vector.Vector3d from,to;
 public RayTraceContext(net.minecraft.util.math.vector.Vector3d f,net.minecraft.util.math.vector.Vector3d t,BlockMode b,FluidMode m,net.minecraft.entity.Entity e){from=f;to=t;}
}''')
path='net/minecraft/world/World.java'
sources[path]=sources[path].replace('    public boolean isClientSide;', '''
    public double wallZ=Double.POSITIVE_INFINITY,loadedUntil=Double.POSITIVE_INFINITY;
    public boolean hasChunkAt(net.minecraft.util.math.BlockPos p){return p.z<loadedUntil;}
    public net.minecraft.util.math.RayTraceResult clip(net.minecraft.util.math.RayTraceContext c){
     if(c.from.z<wallZ && c.to.z>=wallZ){double t=(wallZ-c.from.z)/(c.to.z-c.from.z);return new net.minecraft.util.math.RayTraceResult(net.minecraft.util.math.RayTraceResult.Type.BLOCK,c.from.add(c.to.subtract(c.from).scale(t)));}
     return new net.minecraft.util.math.RayTraceResult(net.minecraft.util.math.RayTraceResult.Type.MISS,c.to);
    }
    public boolean isClientSide;''')
path='net/minecraft/network/datasync/DataSerializers.java'
sources[path]=sources[path].replace('public final class DataSerializers {','public final class DataSerializers { public static final DataSerializer<net.minecraft.nbt.CompoundNBT> COMPOUND_TAG=new DataSerializer<>();')
put('net.kazi.kazimod.init.KaziEntities', '''
public final class KaziEntities {
 public static final BeamType CHAOS_BEAM=new BeamType();
 public static class BeamType {public net.minecraft.entity.EntityType<net.kazi.kazimod.entities.asriel.ChaosBeamAttackEntity> get(){return new net.minecraft.entity.EntityType<>();}}
}''')
put('net.kazi.kazimod.abilities.YagiAwakening.ShockerBreakerAbility', '''
public class ShockerBreakerAbility {
 public static final Core INSTANCE=new Core(); public boolean enabled=true;
 public net.kazi.kazimod.entities.asriel.ChaosBeamAttackEntity field;
 public boolean ownsChaosBeam(net.kazi.kazimod.entities.asriel.ChaosBeamAttackEntity f){return enabled&&f==field;}
 public static class Core {public String getId(){return "shocker_breaker";}}
}''')
put('xyz.pixelatedw.mineminenomi.data.entity.ability.IAbilityData', '''
public interface IAbilityData {net.kazi.kazimod.abilities.YagiAwakening.ShockerBreakerAbility getEquippedAbility(Object core);}''')
put('xyz.pixelatedw.mineminenomi.data.entity.ability.AbilityDataCapability', '''
public class AbilityDataCapability {
 public static final java.util.Map<net.minecraft.entity.LivingEntity,net.kazi.kazimod.abilities.YagiAwakening.ShockerBreakerAbility> DATA=new java.util.HashMap<>();
 public static IAbilityData get(net.minecraft.entity.LivingEntity e){return core->DATA.get(e);}
}''')
put('xyz.pixelatedw.mineminenomi.api.damagesource.SourceElement','public enum SourceElement {LIGHT}')
put('net.minecraft.util.DamageSource', '''
public class DamageSource {
 public final String msgId;public DamageSource(){this("");}public DamageSource(String id){msgId=id;}
 public String getMsgId(){return msgId;}
 private boolean armor,magic,invulnerability;
 public DamageSource bypassArmor(){armor=true;return this;}public boolean isBypassArmor(){return armor;}
 public DamageSource bypassMagic(){magic=true;return this;}public boolean isBypassMagic(){return magic;}
 public DamageSource bypassInvul(){invulnerability=true;return this;}public boolean isBypassInvul(){return invulnerability;}
}''')
put('xyz.pixelatedw.mineminenomi.events.DamageCalcsEvents','public final class DamageCalcsEvents {}')
put('org.spongepowered.asm.mixin.Mixin', '''
@java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
public @interface Mixin {Class<?>[] value();boolean remap() default true;}
''')
put('org.spongepowered.asm.mixin.injection.At', '''
@java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
public @interface At {String value();String target() default "";}
''')
put('org.spongepowered.asm.mixin.injection.Redirect', '''
@java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
public @interface Redirect {String[] method();At at();int require() default -1;}
''')
put('net.minecraft.entity.player.PlayerEntity', '''
public class PlayerEntity extends net.minecraft.entity.LivingEntity {
 public PlayerEntity(net.minecraft.world.World world){super(world);}
}''')
put('testsupport.BeamHealthWriter', '''
import java.lang.reflect.Method;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.DamageSource;
import net.kazi.kazimod.mixin.ChaosBeamLivingDamageMixin;
import net.kazi.kazimod.mixin.ChaosBeamPlayerDamageMixin;
public final class BeamHealthWriter {
 private static final Object LIVING=new ChaosBeamLivingDamageMixin(){};
 private static final Object PLAYER=new ChaosBeamPlayerDamageMixin(){};
 public static final Method LIVING_WRITE=method(ChaosBeamLivingDamageMixin.class,"kazi$keepChaosBeamTargetAlive",LivingEntity.class);
 public static final Method PLAYER_WRITE=method(ChaosBeamPlayerDamageMixin.class,"kazi$keepChaosBeamPlayerAlive",PlayerEntity.class);
 private static Method method(Class<?> type,String name,Class<?> receiver){
  try{Method m=type.getDeclaredMethod(name,receiver,float.class,DamageSource.class,float.class);m.setAccessible(true);return m;}
  catch(ReflectiveOperationException error){throw new AssertionError(error);}
 }
 public static void write(LivingEntity target,float health,DamageSource source,float amount){
  try{boolean player=target instanceof PlayerEntity;(player?PLAYER_WRITE:LIVING_WRITE).invoke(player?PLAYER:LIVING,target,health,source,amount);}
  catch(ReflectiveOperationException error){throw new AssertionError(error);}
 }
}''')
put('org.spongepowered.asm.mixin.injection.Inject', '''
@java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
public @interface Inject {String[] method();At at();boolean cancellable() default false;boolean remap() default true;}
''')
put('org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable', '''
public class CallbackInfoReturnable<T> {
 private T value;private boolean cancelled;
 public CallbackInfoReturnable(String name,boolean cancellable,T original){value=original;}
 public void setReturnValue(T next){value=next;cancelled=true;}
 public T getReturnValue(){return value;}public boolean isCancelled(){return cancelled;}
}''')
put('xyz.pixelatedw.mineminenomi.api.damagesource.AbilityDamageSource', '''
public class AbilityDamageSource extends net.minecraft.util.DamageSource {
 public AbilityDamageSource(String id,net.minecraft.entity.LivingEntity owner,Object core){super(id);}
 public AbilityDamageSource setSourceElement(SourceElement e){return this;} public AbilityDamageSource setProjectile(){return this;}
}''')
path='net/minecraft/entity/Entity.java'
sources[path]=sources[path].replace('    private static int nextId;', '    public int tickCount; private static int nextId;')
sources[path]=sources[path].replace('    public void setNoGravity(boolean value) { }', '''
    public boolean noGravity,passenger,spectator,hurtMarked;public float fallDistance;
    public double xo,yo,zo,xOld,yOld,zOld;public int teleports;
    private net.minecraft.util.math.vector.Vector3d motion=net.minecraft.util.math.vector.Vector3d.ZERO;
    private final net.minecraft.nbt.CompoundNBT persistent=new net.minecraft.nbt.CompoundNBT();
    public boolean isSpectator(){return spectator;}
    public boolean isNoGravity(){return noGravity;} public void setNoGravity(boolean value){noGravity=value;}
    public boolean isPassenger(){return passenger;} public void stopRiding(){passenger=false;}
    public net.minecraft.util.math.vector.Vector3d getDeltaMovement(){return motion;}
    public void setDeltaMovement(net.minecraft.util.math.vector.Vector3d value){motion=value;}
    public net.minecraft.nbt.CompoundNBT getPersistentData(){return persistent;}
    public void teleportTo(double x,double y,double z){teleports++;setPos(x,y,z);}
''').replace('    public void remove() { removed = true; }', '    public void remove() { remove(false); } public void remove(boolean keepData){removed=true;}')
put('net.minecraft.client.Minecraft', '''
public class Minecraft {
 private static final Minecraft INSTANCE=new Minecraft(); public static Minecraft getInstance(){return INSTANCE;}
 public net.minecraft.world.World level; public net.minecraft.entity.LivingEntity player; public net.minecraft.entity.Entity camera;
 public net.minecraft.entity.Entity getCameraEntity(){return camera;}
}''')
put('net.minecraftforge.api.distmarker.Dist','public enum Dist {CLIENT}')
put('net.minecraftforge.eventbus.api.EventPriority','public enum EventPriority {NORMAL,LOWEST}')
put('net.minecraftforge.eventbus.api.SubscribeEvent','public @interface SubscribeEvent {EventPriority priority() default EventPriority.NORMAL;}')
put('net.minecraftforge.fml.common.Mod','public @interface Mod {public @interface EventBusSubscriber {String modid();net.minecraftforge.api.distmarker.Dist[] value() default {};}}')
put('net.minecraftforge.event.TickEvent', '''
public class TickEvent {
 public enum Phase {START,END}
 public static class ClientTickEvent {public Phase phase;public ClientTickEvent(Phase phase){this.phase=phase;}}
 public static class RenderTickEvent {public Phase phase;public RenderTickEvent(Phase phase){this.phase=phase;}}
}''')
put('net.minecraftforge.client.event.ClientPlayerNetworkEvent','public class ClientPlayerNetworkEvent {public static class LoggedOutEvent {}}')
put('net.minecraft.util.MovementInput', '''
public class MovementInput {
 public float leftImpulse,forwardImpulse;public boolean up,down,left,right,jumping,shiftKeyDown;
}''')
put('net.minecraftforge.client.event.InputUpdateEvent', '''
public class InputUpdateEvent {
 private final net.minecraft.entity.LivingEntity player;private final net.minecraft.util.MovementInput input;
 public InputUpdateEvent(net.minecraft.entity.LivingEntity player,net.minecraft.util.MovementInput input){this.player=player;this.input=input;}
 public net.minecraft.entity.LivingEntity getPlayer(){return player;}public net.minecraft.util.MovementInput getMovementInput(){return input;}
}''')
put('net.minecraftforge.event.entity.EntityJoinWorldEvent', '''
public class EntityJoinWorldEvent {
 private final net.minecraft.entity.Entity e;public EntityJoinWorldEvent(net.minecraft.entity.Entity e){this.e=e;}
 public net.minecraft.entity.Entity getEntity(){return e;}public net.minecraft.world.World getWorld(){return e.level;}
}''')
put('net.kazi.kazimod.abilities.cinematics.AwakeningCutsceneType', '''
public enum AwakeningCutsceneType { CHAOS_BEAM_FINALE }''')
put('net.kazi.kazimod.abilities.NagiAwakening.InnerDragonsCutscenes', '''
public class InnerDragonsCutscenes {
 public static int starts;
 public static void start(net.minecraft.entity.LivingEntity owner,long tick,
      net.kazi.kazimod.abilities.cinematics.AwakeningCutsceneType type){starts++;}
}''')
put('net.kazi.kazimod.abilities.NagiAwakening.ImagineBreakerNegation', '''
public class ImagineBreakerNegation {
 public static boolean negateBeam(net.minecraft.entity.Entity field,net.minecraft.entity.Entity owner,Object source,
       net.minecraft.util.math.AxisAlignedBB bounds,java.util.function.Function<net.minecraft.util.math.vector.Vector3d,net.minecraft.util.math.vector.Vector3d> contact){return false;}
}''')
paths=[]
for name,code in sources.items():
    path=SRC/name;path.parent.mkdir(parents=True,exist_ok=True);path.write_text(code,encoding='utf-8');paths.append(str(path))
CLASSES.mkdir(parents=True,exist_ok=True)
paths += [str(ROOT/'src/main/java/net/kazi/kazimod'/p) for p in
          ['abilities/YagiAwakening/ChaosBeamPattern.java','abilities/YagiAwakening/ChaosBeamHealing.java','abilities/YagiAwakening/ChaosBeamRecoil.java','abilities/YagiAwakening/ChaosBeamDamage.java','abilities/YagiAwakening/YagiAwakeningMath.java','entities/asriel/ChaosBeamAttackEntity.java','client/asriel/ChaosBeamCameraClient.java','mixin/ChaosBeamRecoilMixin.java','mixin/ChaosBeamLivingDamageMixin.java','mixin/ChaosBeamPlayerDamageMixin.java']]
paths.append(str(Path(__file__).with_name('ChaosBeamEntityTest.java')))
paths.append(str(Path(__file__).with_name('ChaosBeamCameraTest.java')))
subprocess.run([str(JAVA/'javac.exe'),'-encoding','UTF-8','-d',str(CLASSES),*paths],check=True)
subprocess.run([str(JAVA/'java.exe'),'-cp',str(CLASSES),'ChaosBeamEntityTest'],check=True)
subprocess.run([str(JAVA/'java.exe'),'-cp',str(CLASSES),'ChaosBeamCameraTest'],check=True)
