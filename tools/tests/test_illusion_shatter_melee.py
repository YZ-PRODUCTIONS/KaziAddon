"""Run the real passive event callbacks against small server/ability fixtures."""
from pathlib import Path
import subprocess

ROOT = Path(__file__).resolve().parents[2]
OUT = ROOT / 'build/illusion-shatter-melee-tests'
JAVA = Path('C:/Users/th3wyz/.jdks/corretto-1.8.0_482/bin')
sources = {}

def put(name, code):
    sources[name] = 'package ' + name.rsplit('.', 1)[0] + ';\n' + code

put('net.minecraft.world.World', '''public class World {
 public boolean isClientSide,wall;public long gameTime;public final java.util.Set<Integer> denied=new java.util.HashSet<>();
 public long getGameTime(){return gameTime;}
 public net.minecraft.util.math.RayTraceResult clip(net.minecraft.util.math.RayTraceContext context){return new net.minecraft.util.math.RayTraceResult(wall?net.minecraft.util.math.RayTraceResult.Type.BLOCK:net.minecraft.util.math.RayTraceResult.Type.MISS);}
}''')
put('net.minecraft.util.math.vector.Vector3d', '''public class Vector3d {
 public final double x,y,z;public Vector3d(){this(0,0,0);}public Vector3d(double x,double y,double z){this.x=x;this.y=y;this.z=z;}
 public double distanceToSqr(Vector3d other){double a=x-other.x,b=y-other.y,c=z-other.z;return a*a+b*b+c*c;}
}''')
put('net.minecraft.util.math.AxisAlignedBB', '''public class AxisAlignedBB {
 public final double minX,minY,minZ,maxX,maxY,maxZ;
 public AxisAlignedBB(double a,double b,double c,double d,double e,double f){minX=a;minY=b;minZ=c;maxX=d;maxY=e;maxZ=f;}
}''')
put('net.minecraft.util.math.MathHelper','public class MathHelper {public static double clamp(double value,double min,double max){return Math.max(min,Math.min(max,value));}}')
put('net.minecraft.util.math.RayTraceContext', '''public class RayTraceContext {
 public enum BlockMode {COLLIDER}public enum FluidMode {NONE}
 public RayTraceContext(net.minecraft.util.math.vector.Vector3d from,net.minecraft.util.math.vector.Vector3d to,BlockMode blocks,FluidMode fluids,net.minecraft.entity.Entity user){}
}''')
put('net.minecraft.util.math.BlockPos', '''public class BlockPos {
 private final int x;public BlockPos(int x){this.x=x;}public int getX(){return x;}public int getY(){return 0;}public int getZ(){return 0;}
}''')
put('net.minecraft.entity.Entity', '''public class Entity {
 private static int nextId;public final int id=++nextId;public final java.util.UUID uuid=new java.util.UUID(0,id);
 public net.minecraft.world.World level;public boolean alive=true,spectator,removed;public double x,y,z;
 public Entity(net.minecraft.world.World world){level=world;}public java.util.UUID getUUID(){return uuid;}
 public boolean isAlive(){return alive&&!removed;}public boolean isSpectator(){return spectator;}
 public net.minecraft.util.math.vector.Vector3d position(){return new net.minecraft.util.math.vector.Vector3d(x,y,z);}
 public net.minecraft.util.math.AxisAlignedBB getBoundingBox(){return new net.minecraft.util.math.AxisAlignedBB(x-.3,y,z-.3,x+.3,y+1.8,z+.3);}
 public net.minecraft.util.math.BlockPos blockPosition(){return new net.minecraft.util.math.BlockPos(id);}
}''')
put('net.minecraft.entity.LivingEntity', '''public class LivingEntity extends Entity {
 public float health=100,maxHealth=100;public boolean unlock=true,enemy=true,invulnerable,dodging;
 public int suppressed,duration,damageCalls,sounds,particles;public final java.util.Set<String> effects=new java.util.HashSet<>();
 public LivingEntity(net.minecraft.world.World world){super(world);}public float getHealth(){return health;}public float getMaxHealth(){return maxHealth;}
 public boolean hasEffect(String effect){return effects.contains(effect);}public boolean isInvulnerableTo(net.minecraft.util.DamageSource source){return invulnerable;}
 public boolean hurt(net.minecraft.util.DamageSource source,float amount){damageCalls++;health-=amount;return true;}
}''')
put('net.minecraft.server.MinecraftServer', 'public class MinecraftServer {public boolean pvp=true;public boolean isPvpAllowed(){return pvp;}}')
put('net.minecraft.item.ItemStack', 'public class ItemStack {public final String item;public ItemStack(String item){this.item=item;}public boolean isEmpty(){return item.isEmpty();}}')
put('xyz.pixelatedw.mineminenomi.api.ISweep','public interface ISweep {boolean isSweeping();}')
put('net.minecraft.entity.player.PlayerEntity', '''public class PlayerEntity extends net.minecraft.entity.LivingEntity implements xyz.pixelatedw.mineminenomi.api.ISweep {
 public boolean canHarm=true,sweeping;public net.minecraft.server.MinecraftServer server=new net.minecraft.server.MinecraftServer();
 public net.minecraft.item.ItemStack held=new net.minecraft.item.ItemStack("");public net.minecraft.util.math.RayTraceResult trace;
 public PlayerEntity(net.minecraft.world.World world){super(world);}public boolean canHarmPlayer(PlayerEntity target){return canHarm;}
 public net.minecraft.server.MinecraftServer getServer(){return server;}public net.minecraft.item.ItemStack getMainHandItem(){return held;}
 public boolean isSweeping(){return sweeping;}public net.minecraft.util.math.vector.Vector3d getEyePosition(float partial){return new net.minecraft.util.math.vector.Vector3d(x,y+1.62,z);}
}''')
put('net.minecraft.util.DamageSource', '''public class DamageSource {
 public net.minecraft.entity.Entity owner,direct;public boolean projectile,explosion,magic,fruit;public String msgId="player";
 public DamageSource(net.minecraft.entity.Entity owner,net.minecraft.entity.Entity direct){this.owner=owner;this.direct=direct;}
 public net.minecraft.entity.Entity getEntity(){return owner;}public net.minecraft.entity.Entity getDirectEntity(){return direct;}
 public boolean isProjectile(){return projectile;}public boolean isExplosion(){return explosion;}public boolean isMagic(){return magic;}
 public String getMsgId(){return msgId;}
 public DamageSource bypassLogia(){return this;}
}''')
put('xyz.pixelatedw.mineminenomi.api.damagesource.AbilityDamageSource', '''public class AbilityDamageSource extends net.minecraft.util.DamageSource {
 public AbilityDamageSource(net.minecraft.entity.Entity owner,net.minecraft.entity.Entity direct){super(owner,direct);}
}''')
put('net.minecraft.util.ResourceLocation', 'public class ResourceLocation {public ResourceLocation(String a,String b){}}')
put('net.minecraft.util.text.TranslationTextComponent', 'public class TranslationTextComponent {public TranslationTextComponent(String key){}}')
put('net.minecraft.util.math.RayTraceResult', '''public class RayTraceResult {
 public enum Type {MISS,BLOCK,ENTITY}private final Type type;public RayTraceResult(){this(Type.MISS);}public RayTraceResult(Type type){this.type=type;}
 public Type getType(){return type;}public net.minecraft.util.math.vector.Vector3d getLocation(){return new net.minecraft.util.math.vector.Vector3d();}
}''')
put('net.minecraft.util.math.EntityRayTraceResult', '''public class EntityRayTraceResult extends RayTraceResult {
 private final net.minecraft.entity.Entity entity;public EntityRayTraceResult(net.minecraft.entity.Entity entity){this.entity=entity;}
 public net.minecraft.entity.Entity getEntity(){return entity;}
}''')
put('net.minecraftforge.eventbus.api.EventPriority', 'public enum EventPriority {HIGHEST,NORMAL,LOWEST}')
put('net.minecraftforge.eventbus.api.SubscribeEvent', '''@java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
public @interface SubscribeEvent {EventPriority priority() default EventPriority.NORMAL;boolean receiveCanceled() default false;}''')
put('net.minecraftforge.fml.common.Mod', 'public @interface Mod {public @interface EventBusSubscriber {String modid();}}')
put('net.minecraftforge.event.entity.player.AttackEntityEvent', '''public class AttackEntityEvent {
 private final net.minecraft.entity.player.PlayerEntity user;private final net.minecraft.entity.Entity target;public boolean canceled;
 public AttackEntityEvent(net.minecraft.entity.player.PlayerEntity user,net.minecraft.entity.Entity target){this.user=user;this.target=target;}
 public net.minecraft.entity.player.PlayerEntity getPlayer(){return user;}public net.minecraft.entity.Entity getTarget(){return target;}
 public void setCanceled(boolean value){canceled=value;}
}''')
for name in ['xyz.pixelatedw.mineminenomi.api.events.WyLivingAttackEvent',
             'net.minecraftforge.event.entity.living.LivingAttackEvent','net.minecraftforge.event.entity.living.LivingHurtEvent']:
    simple = name.rsplit('.', 1)[1]
    put(name, '''public class %s {
 private final net.minecraft.entity.LivingEntity target;private final net.minecraft.util.DamageSource source;public boolean canceled;public float amount=12;
 public %s(net.minecraft.entity.LivingEntity target,net.minecraft.util.DamageSource source){this.target=target;this.source=source;}
 public net.minecraft.entity.LivingEntity getEntityLiving(){return target;}public net.minecraft.util.DamageSource getSource(){return source;}
 public void setCanceled(boolean value){canceled=value;}
}''' % (simple,simple))
put('xyz.pixelatedw.mineminenomi.api.abilities.AbilityCategory','public enum AbilityCategory {DEVIL_FRUITS}')
put('xyz.pixelatedw.mineminenomi.api.abilities.AbilityType','public enum AbilityType {PASSIVE}')
put('xyz.pixelatedw.mineminenomi.api.damagesource.SourceType','public enum SourceType {FIST}')
put('xyz.pixelatedw.mineminenomi.api.abilities.AbilityDescriptionLine','public class AbilityDescriptionLine {public static final Object NEW_LINE=new Object();}')
put('xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore', '''public class AbilityCore<T> {
 public static class Builder<T> {
  public Builder(String name,String id,AbilityCategory category,AbilityType type,java.util.function.Function<AbilityCore<T>,T> factory){}
  public Builder<T> setUnlockCheck(java.util.function.Predicate<net.minecraft.entity.LivingEntity> check){return this;}
  public Builder<T> setIcon(Object icon){return this;}public Builder<T> setSourceType(Object type){return this;}
  public Builder<T> addDescriptionLine(Object line){return this;}public Builder<T> addAdvancedDescriptionLine(Object... lines){return this;}
  public AbilityCore<T> build(){return new AbilityCore<>();}
 }
}''')
put('xyz.pixelatedw.mineminenomi.api.abilities.PassiveAbility2', '''public class PassiveAbility2 {
 public final Disable disableComponent=new Disable();public final Cooldown cooldownComponent=new Cooldown();public boolean paused,useAllowed=true;
 public PassiveAbility2(AbilityCore<?> core){}public void tick(net.minecraft.entity.LivingEntity user){}
 public boolean isPaused(){return paused;}public Result canUse(net.minecraft.entity.LivingEntity user){return new Result(useAllowed&&!paused&&!disableComponent.disabled);}
 public static class Result {private final boolean success;public Result(boolean success){this.success=success;}public boolean isSuccess(){return success;}}
 public static class Disable {public boolean disabled;public boolean isDisabled(){return disabled;}}
 public static class Cooldown {public boolean running;public boolean isOnCooldown(){return running;}public void stopCooldown(net.minecraft.entity.LivingEntity user){running=false;}}
}''')
put('xyz.pixelatedw.mineminenomi.api.abilities.components.RangeComponent', '''public class RangeComponent {
 public enum RangeType {LINE}public static Object getTooltip(float range,RangeType type){return null;}
}''')
put('xyz.pixelatedw.mineminenomi.data.entity.ability.IAbilityData', '''public class IAbilityData {
 public Object passive;public IAbilityData(Object passive){this.passive=passive;}
 @SuppressWarnings("unchecked")public <T>T getPassiveAbility(xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore<T> core){return (T)passive;}
}''')
put('xyz.pixelatedw.mineminenomi.data.entity.ability.AbilityDataCapability', '''public class AbilityDataCapability {
 public static final java.util.Map<net.minecraft.entity.LivingEntity,IAbilityData> DATA=new java.util.IdentityHashMap<>();
 public static IAbilityData get(net.minecraft.entity.LivingEntity entity){return DATA.get(entity);}
}''')
for name, effects in [('net.kazi.kazimod.init.KaziEffects',['IMAGINE_BROKEN','LOST_WORKS_FRUIT_LOCK']),
                      ('xyz.pixelatedw.mineminenomi.init.ModEffects',['NO_HANDS','GUARDING'])]:
    put(name, 'public class '+name.rsplit('.',1)[1]+' {public static class Entry {private final String name;public Entry(String name){this.name=name;}public String get(){return name;}}'+
        ''.join('public static final Entry '+effect+'=new Entry("'+effect+'");' for effect in effects)+'}')
put('xyz.pixelatedw.mineminenomi.api.protection.ProtectedArea', 'public class ProtectedArea {public boolean canUseAbility(Object core){return false;}}')
put('xyz.pixelatedw.mineminenomi.data.world.ProtectedAreasData', '''public class ProtectedAreasData {
 private final net.minecraft.world.World world;public ProtectedAreasData(net.minecraft.world.World world){this.world=world;}
 public static ProtectedAreasData get(net.minecraft.world.World world){return new ProtectedAreasData(world);}
 public xyz.pixelatedw.mineminenomi.api.protection.ProtectedArea getProtectedArea(int x,int y,int z){return world.denied.contains(x)?new xyz.pixelatedw.mineminenomi.api.protection.ProtectedArea():null;}
}''')
put('xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper','public class AbilityHelper {public static boolean isDodging(net.minecraft.entity.LivingEntity target){return target.dodging;}}')
put('xyz.pixelatedw.mineminenomi.init.ModDamageSource', '''public class ModDamageSource {
 public static net.minecraft.util.DamageSource causeAbilityDamage(net.minecraft.entity.LivingEntity user,Object passive){return new net.minecraft.util.DamageSource(user,user);}
}''')
put('xyz.pixelatedw.mineminenomi.wypi.WyHelper', '''public class WyHelper {
 public static net.minecraft.util.math.RayTraceResult rayTraceBlocksAndEntities(net.minecraft.entity.player.PlayerEntity user,double range){return user.trace;}
}''')
put('net.kazi.kazimod.abilities.NagiAwakening.NagiAwakeningSupport', '''public class NagiAwakeningSupport {
 public static boolean canUnlock(net.minecraft.entity.LivingEntity user){return user.unlock;}
 public static boolean isEnemy(net.minecraft.entity.LivingEntity user,net.minecraft.entity.LivingEntity target){return user!=target&&target.enemy;}
 public static boolean isDevilFruitAttack(net.minecraft.util.DamageSource source){return source!=null&&source.fruit;}
 public static void playNullificationSound(net.minecraft.entity.LivingEntity user,net.minecraft.util.math.vector.Vector3d point){user.sounds++;}
 public static void shatterParticles(net.minecraft.entity.LivingEntity user,net.minecraft.util.math.vector.Vector3d point){user.particles++;}
}''')
put('net.kazi.kazimod.abilities.NagiAwakening.ImagineBrokenEffect', '''public class ImagineBrokenEffect {
 public static void apply(net.minecraft.entity.LivingEntity target,int ticks){target.suppressed++;target.duration=ticks;}
}''')
put('net.kazi.kazimod.abilities.NagiAwakening.ImagineBreakerNegation','public class ImagineBreakerNegation {public static void tick(net.minecraft.entity.LivingEntity user){}}')

paths=[]
for name,code in sources.items():
    path=OUT/'fixtures'/(name.replace('.','/')+'.java');path.parent.mkdir(parents=True,exist_ok=True)
    path.write_text(code,encoding='utf-8');paths.append(str(path))
paths += [str(ROOT/'src/main/java/net/kazi/kazimod/abilities/NagiAwakening'/name) for name in
          ['IllusionShatterAbility.java','IllusionShatterHealth.java','InnerDragonsSequence.java']]
paths.append(str(Path(__file__).with_name('IllusionShatterMeleeTest.java')))
classes=OUT/'classes';classes.mkdir(parents=True,exist_ok=True)
subprocess.run([str(JAVA/'javac.exe'),'-encoding','UTF-8','-d',str(classes),*paths],check=True)
subprocess.run([str(JAVA/'java.exe'),'-cp',str(classes),'IllusionShatterMeleeTest'],check=True)
