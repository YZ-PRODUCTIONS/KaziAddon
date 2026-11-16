"""Run the production Trace finisher against focused collision/combat fixtures."""
from pathlib import Path
import subprocess

ROOT = Path(__file__).resolve().parents[2]
OUT = ROOT / 'build/traced-final-execution-tests'
JAVA = Path('C:/Users/th3wyz/.jdks/corretto-1.8.0_482/bin')
SOURCE = ROOT / 'src/main/java/net/kazi/kazimod/abilities/SupaRework/TracedFinalExecution.java'
fixtures = {}


def put(name, code):
    fixtures[name] = 'package ' + name.rsplit('.', 1)[0] + ';\n' + code


put('net.minecraft.util.math.vector.Vector3d', '''public class Vector3d {
 public final double x,y,z;public Vector3d(double x,double y,double z){this.x=x;this.y=y;this.z=z;}
 public Vector3d add(Vector3d v){return new Vector3d(x+v.x,y+v.y,z+v.z);}public Vector3d scale(double n){return new Vector3d(x*n,y*n,z*n);}
 public double length(){return Math.sqrt(x*x+y*y+z*z);}public Vector3d normalize(){double n=length();return n<1e-8?new Vector3d(0,0,0):scale(1/n);}
 public double distanceTo(Vector3d v){return Math.sqrt((x-v.x)*(x-v.x)+(y-v.y)*(y-v.y)+(z-v.z)*(z-v.z));}
}''')
put('net.minecraft.util.math.AxisAlignedBB', '''public class AxisAlignedBB {
 public final double minX,minY,minZ,maxX,maxY,maxZ;
 public AxisAlignedBB(double x0,double y0,double z0,double x1,double y1,double z1){minX=Math.min(x0,x1);minY=Math.min(y0,y1);minZ=Math.min(z0,z1);maxX=Math.max(x0,x1);maxY=Math.max(y0,y1);maxZ=Math.max(z0,z1);}
 public AxisAlignedBB(net.minecraft.util.math.vector.Vector3d a,net.minecraft.util.math.vector.Vector3d b){this(a.x,a.y,a.z,b.x,b.y,b.z);}
 public AxisAlignedBB inflate(double x,double y,double z){return new AxisAlignedBB(minX-x,minY-y,minZ-z,maxX+x,maxY+y,maxZ+z);}
 public AxisAlignedBB move(net.minecraft.util.math.vector.Vector3d v){return new AxisAlignedBB(minX+v.x,minY+v.y,minZ+v.z,maxX+v.x,maxY+v.y,maxZ+v.z);}
 public boolean contains(net.minecraft.util.math.vector.Vector3d v){return v.x>=minX&&v.x<=maxX&&v.y>=minY&&v.y<=maxY&&v.z>=minZ&&v.z<=maxZ;}
 public java.util.Optional<net.minecraft.util.math.vector.Vector3d> clip(net.minecraft.util.math.vector.Vector3d a,net.minecraft.util.math.vector.Vector3d b){
  double[] origin={a.x,a.y,a.z},delta={b.x-a.x,b.y-a.y,b.z-a.z},lo={minX,minY,minZ},hi={maxX,maxY,maxZ};double enter=0,exit=1;
  for(int axis=0;axis<3;axis++){if(Math.abs(delta[axis])<1e-10){if(origin[axis]<lo[axis]||origin[axis]>hi[axis])return java.util.Optional.empty();}
   else {double left=(lo[axis]-origin[axis])/delta[axis],right=(hi[axis]-origin[axis])/delta[axis];enter=Math.max(enter,Math.min(left,right));exit=Math.min(exit,Math.max(left,right));if(enter>exit)return java.util.Optional.empty();}}
  return java.util.Optional.of(new net.minecraft.util.math.vector.Vector3d(a.x+delta[0]*enter,a.y+delta[1]*enter,a.z+delta[2]*enter));
 }
}''')
put('net.minecraft.util.math.RayTraceResult', '''public class RayTraceResult {public enum Type {MISS,BLOCK}}
''')
put('net.minecraft.util.math.BlockRayTraceResult', '''public class BlockRayTraceResult extends RayTraceResult {
 private final Type type;private final net.minecraft.util.math.vector.Vector3d location;
 public BlockRayTraceResult(Type type,net.minecraft.util.math.vector.Vector3d location){this.type=type;this.location=location;}public Type getType(){return type;}public net.minecraft.util.math.vector.Vector3d getLocation(){return location;}
}''')
put('net.minecraft.util.math.RayTraceContext', '''public class RayTraceContext {
 public enum BlockMode {COLLIDER}public enum FluidMode {NONE}public final net.minecraft.util.math.vector.Vector3d start,end;
 public RayTraceContext(net.minecraft.util.math.vector.Vector3d start,net.minecraft.util.math.vector.Vector3d end,BlockMode block,FluidMode fluid,net.minecraft.entity.Entity user){this.start=start;this.end=end;}
}''')
put('net.minecraft.util.SoundCategory', 'public enum SoundCategory {PLAYERS}')
put('net.minecraft.world.World', '''public class World {
 public boolean isClientSide;public double rayWall=Double.POSITIVE_INFINITY,bodyWall=Double.POSITIVE_INFINITY,marbleWall=Double.POSITIVE_INFINITY;public int sounds,collisionChecks;
 public final java.util.List<net.minecraft.entity.projectile.ProjectileEntity> projectiles=new java.util.ArrayList<>();
 public net.minecraft.util.math.BlockRayTraceResult clip(net.minecraft.util.math.RayTraceContext ray){
  if(rayWall<ray.end.x)return new net.minecraft.util.math.BlockRayTraceResult(net.minecraft.util.math.RayTraceResult.Type.BLOCK,new net.minecraft.util.math.vector.Vector3d(rayWall,ray.start.y,ray.start.z));
  return new net.minecraft.util.math.BlockRayTraceResult(net.minecraft.util.math.RayTraceResult.Type.MISS,ray.end);}
 public <T extends net.minecraft.entity.Entity>java.util.List<T> getEntitiesOfClass(Class<T> type,net.minecraft.util.math.AxisAlignedBB area){java.util.List<T> matches=new java.util.ArrayList<>();for(net.minecraft.entity.Entity e:projectiles)if(type.isInstance(e))matches.add(type.cast(e));return matches;}
 public boolean noCollision(net.minecraft.entity.Entity user,net.minecraft.util.math.AxisAlignedBB box){collisionChecks++;return box.maxX<=bodyWall;}
 public void playSound(Object listener,Object position,Object sound,net.minecraft.util.SoundCategory category,float volume,float pitch){sounds++;}
}''')
put('net.minecraft.world.server.ServerWorld', '''public class ServerWorld extends net.minecraft.world.World {
 public final ChunkSource chunks=new ChunkSource();public ChunkSource getChunkSource(){return chunks;}
 public static class ChunkSource {public int broadcasts;public void broadcastAndSend(net.minecraft.entity.Entity user,Object packet){broadcasts++;}}
}''')
put('net.minecraft.entity.Entity', '''public class Entity {
 public net.minecraft.world.World level;public double x,y,z;public boolean alive=true,friendlyFaction;public int team;public float width=.6f,height=1.8f;
 public Entity(net.minecraft.world.World level){this.level=level;}public net.minecraft.util.math.vector.Vector3d position(){return new net.minecraft.util.math.vector.Vector3d(x,y,z);}
 public double getX(){return x;}public double getY(){return y;}public double getZ(){return z;}public boolean isAlive(){return alive;}public boolean isAlliedTo(Entity other){return team!=0&&team==other.team;}
 public net.minecraft.util.math.AxisAlignedBB getBoundingBox(){return new net.minecraft.util.math.AxisAlignedBB(x-width/2,y,z-width/2,x+width/2,y+height,z+width/2);}
 public float getBbWidth(){return width;}public float getBbHeight(){return height;}
}''')
put('net.minecraft.entity.LivingEntity', '''public class LivingEntity extends Entity {
 public float health=100,maxHealth=100;public int teleports,unrides,particles;public net.minecraft.util.math.vector.Vector3d look=new net.minecraft.util.math.vector.Vector3d(1,0,0);
 public final java.util.List<net.minecraft.potion.EffectInstance> effects=new java.util.ArrayList<>();public LivingEntity(net.minecraft.world.World level){super(level);}
 public net.minecraft.util.math.vector.Vector3d getLookAngle(){return look;}public net.minecraft.util.math.vector.Vector3d getEyePosition(float partial){return new net.minecraft.util.math.vector.Vector3d(x,y+1.6,z);}
 public double getEyeY(){return y+1.6;}public float getHealth(){return health;}public float getMaxHealth(){return maxHealth;}
 public void addEffect(net.minecraft.potion.EffectInstance effect){effects.add(effect);}public void stopRiding(){unrides++;}
 public void teleportToWithTicket(double x,double y,double z){teleports++;this.x=x;this.y=y;this.z=z;}public Object blockPosition(){return position();}
}''')
put('net.minecraft.entity.projectile.ProjectileEntity', '''public class ProjectileEntity extends net.minecraft.entity.Entity {
 public net.minecraft.entity.Entity owner;public ProjectileEntity(net.minecraft.world.World level){super(level);width=.2f;height=.2f;}public net.minecraft.entity.Entity getOwner(){return owner;}
}''')
put('xyz.pixelatedw.mineminenomi.entities.projectiles.AbilityProjectileEntity', '''public class AbilityProjectileEntity extends net.minecraft.entity.projectile.ProjectileEntity {
 public net.minecraft.entity.LivingEntity thrower;public AbilityProjectileEntity(net.minecraft.world.World level){super(level);}public net.minecraft.entity.LivingEntity getThrower(){return thrower;}
}''')
put('net.minecraft.network.play.server.SAnimateHandPacket', 'public class SAnimateHandPacket {public SAnimateHandPacket(net.minecraft.entity.Entity user,int hand){}}')
put('net.minecraft.potion.EffectInstance', '''public class EffectInstance {
 public final Object effect;public final int duration,amplifier;public EffectInstance(Object effect,int duration,int amplifier,boolean ambient,boolean visible){this.effect=effect;this.duration=duration;this.amplifier=amplifier;}
}''')
put('net.kazi.kazimod.testing.Ref', 'public class Ref<T> {private final T value;public Ref(T value){this.value=value;}public T get(){return value;}}')
for name,field in [('net.MrMagicalCart.cartaddon.init.CartEffects','HEAVYBLEEDING'),('net.MrMagicalCart.cartaddon.init.CartParticleEffects','FINAL_EXECUTION'),('xyz.pixelatedw.mineminenomi.init.ModSounds','DASH_ABILITY_SWOOSH_SFX')]:
    put(name,'public class '+name.rsplit('.',1)[1]+' {public static final net.kazi.kazimod.testing.Ref<Object> '+field+'=new net.kazi.kazimod.testing.Ref<>(new Object());}')
put('xyz.pixelatedw.mineminenomi.init.ModDamageSource', 'public class ModDamageSource {public ModDamageSource setSlash(){return this;}}')
put('xyz.pixelatedw.mineminenomi.api.damagesource.AbilityDamageSource', 'public class AbilityDamageSource extends xyz.pixelatedw.mineminenomi.init.ModDamageSource {}')
put('xyz.pixelatedw.mineminenomi.api.abilities.AbilityKey', 'public class AbilityKey<T> {}')
put('xyz.pixelatedw.mineminenomi.api.abilities.IAbility', 'public interface IAbility {<T>java.util.Optional<T> getComponent(AbilityKey<T> key);}')
put('xyz.pixelatedw.mineminenomi.api.abilities.components.CooldownComponent', '''public class CooldownComponent {
 public float start;public int starts;public void startCooldown(net.minecraft.entity.LivingEntity user,float ticks){if(start>0)return;start=ticks;starts++;}
}''')
put('xyz.pixelatedw.mineminenomi.init.ModAbilityKeys', '''public class ModAbilityKeys {
 public static final xyz.pixelatedw.mineminenomi.api.abilities.AbilityKey<xyz.pixelatedw.mineminenomi.api.abilities.components.CooldownComponent> COOLDOWN=new xyz.pixelatedw.mineminenomi.api.abilities.AbilityKey<>();
}''')
put('xyz.pixelatedw.mineminenomi.api.abilities.components.AnimationComponent', 'public class AnimationComponent {public int stops;public void stop(net.minecraft.entity.LivingEntity user){stops++;}}')
put('xyz.pixelatedw.mineminenomi.api.abilities.components.DealDamageComponent', '''public class DealDamageComponent {
 public final java.util.List<Float> amounts=new java.util.ArrayList<>();public final java.util.List<net.minecraft.entity.LivingEntity> targets=new java.util.ArrayList<>();
 public Object getDamageSource(net.minecraft.entity.LivingEntity user){return new xyz.pixelatedw.mineminenomi.api.damagesource.AbilityDamageSource();}
 public boolean hurtTarget(net.minecraft.entity.LivingEntity user,net.minecraft.entity.LivingEntity target,float amount,xyz.pixelatedw.mineminenomi.api.damagesource.AbilityDamageSource source){targets.add(target);amounts.add(amount);return true;}
}''')
put('xyz.pixelatedw.mineminenomi.api.abilities.components.RangeComponent', '''public class RangeComponent {
 public float distance,width;public final java.util.List<net.minecraft.entity.LivingEntity> targets=new java.util.ArrayList<>();
 public java.util.List<net.minecraft.entity.LivingEntity> getTargetsInLine(net.minecraft.entity.LivingEntity user,float distance,float width){this.distance=distance;this.width=width;java.util.List<net.minecraft.entity.LivingEntity> found=new java.util.ArrayList<>();for(net.minecraft.entity.LivingEntity target:targets)if(target.x>=user.x&&target.x-user.x<=distance+width)found.add(target);return found;}
}''')
put('xyz.pixelatedw.mineminenomi.api.abilities.components.HitTrackerComponent', '''public class HitTrackerComponent {
 public final java.util.Set<net.minecraft.entity.Entity> hit=new java.util.HashSet<>();public boolean canHit(net.minecraft.entity.Entity target){return hit.add(target);}
}''')
put('xyz.pixelatedw.mineminenomi.init.ModEntityPredicates', '''public class ModEntityPredicates {
 public static java.util.function.Predicate<net.minecraft.entity.Entity> getEnemyFactions(net.minecraft.entity.LivingEntity user){return entity->!entity.friendlyFaction;}
}''')
put('xyz.pixelatedw.mineminenomi.wypi.WyHelper', 'public class WyHelper {public static void spawnParticleEffect(Object effect,net.minecraft.entity.LivingEntity entity,double x,double y,double z){entity.particles++;}}')
put('net.kazi.kazimod.abilities.SupaRework.RealityMarbleBoundary', '''public class RealityMarbleBoundary {
 public static net.minecraft.util.math.vector.Vector3d clampTeleportDestination(net.minecraft.entity.Entity user,net.minecraft.util.math.vector.Vector3d target){return new net.minecraft.util.math.vector.Vector3d(Math.min(user.level.marbleWall,target.x),target.y,target.z);}
 public static boolean blocksTeleport(net.minecraft.entity.Entity user,double x,double y,double z){return x>user.level.marbleWall;}
}''')

paths=[]
for name,code in fixtures.items():
    path=OUT/'fixtures'/(name.replace('.','/')+'.java')
    path.parent.mkdir(parents=True,exist_ok=True)
    path.write_text(code,encoding='utf-8')
    paths.append(str(path))
paths.extend([str(SOURCE),str(Path(__file__).with_name('TracedFinalExecutionTest.java'))])
classes=OUT/'classes'
classes.mkdir(parents=True,exist_ok=True)
subprocess.run([str(JAVA/'javac.exe'),'-encoding','UTF-8','-d',str(classes),*paths],check=True)
subprocess.run([str(JAVA/'java.exe'),'-cp',str(classes),'TracedFinalExecutionTest'],check=True)
