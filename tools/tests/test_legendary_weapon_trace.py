"""Exercise the production Legendary Weapon Trace manager with server fixtures.

These fixtures model capability ownership, slots, component state and server
events; the roll/expiry/restoration implementation is compiled from src/main.
"""
from pathlib import Path
import re
import subprocess

ROOT = Path(__file__).resolve().parents[2]
OUT = ROOT / 'build/legendary-weapon-trace-tests'
JAVA = Path('C:/Users/th3wyz/.jdks/corretto-1.8.0_482/bin')
MANAGER = ROOT / 'src/main/java/net/kazi/kazimod/abilities/SupaRework/LegendaryWeaponTrace.java'
sources = {}


def put(name, code):
    sources[name] = 'package ' + name.rsplit('.', 1)[0] + ';\n' + code


put('net.minecraft.nbt.INBT', 'public interface INBT {INBT copy();}')
put('net.minecraft.nbt.CompoundNBT', '''public class CompoundNBT implements INBT {
 private final java.util.Map<String,Object> values=new java.util.HashMap<>();
 public void putString(String key,String value){values.put(key,value);}public String getString(String key){Object value=values.get(key);return value instanceof String?(String)value:"";}
 public void putInt(String key,int value){values.put(key,value);}public int getInt(String key){Object value=values.get(key);return value instanceof Number?((Number)value).intValue():0;}
 public void putLong(String key,long value){values.put(key,value);}public long getLong(String key){Object value=values.get(key);return value instanceof Number?((Number)value).longValue():0;}
 public void putFloat(String key,float value){values.put(key,value);}public float getFloat(String key){Object value=values.get(key);return value instanceof Number?((Number)value).floatValue():0;}
 public void putBoolean(String key,boolean value){values.put(key,value);}public boolean getBoolean(String key){return Boolean.TRUE.equals(values.get(key));}
 public void put(String key,INBT value){values.put(key,value);}public INBT get(String key){Object value=values.get(key);return value instanceof INBT?(INBT)value:null;}
 public CompoundNBT getCompound(String key){Object value=values.get(key);return value instanceof CompoundNBT?(CompoundNBT)value:new CompoundNBT();}
 public boolean contains(String key){return values.containsKey(key);}public boolean contains(String key,int type){return type==10?values.get(key) instanceof CompoundNBT:contains(key);}
 public void remove(String key){values.remove(key);}public boolean isEmpty(){return values.isEmpty();}
 public java.util.Set<String> getAllKeys(){return values.keySet();}
 public CompoundNBT copy(){CompoundNBT result=new CompoundNBT();for(java.util.Map.Entry<String,Object> e:values.entrySet())result.values.put(e.getKey(),e.getValue() instanceof INBT?((INBT)e.getValue()).copy():e.getValue());return result;}
}''')
put('net.minecraft.util.ResourceLocation', '''public class ResourceLocation {
 private final String value;public ResourceLocation(String value){if(value==null||value.isEmpty()||value.indexOf(' ')>=0)throw new IllegalArgumentException(value);this.value=value.contains(":")?value:"minecraft:"+value;}
 public ResourceLocation(String namespace,String path){this(namespace+":"+path);}public String getNamespace(){return value.split(":",2)[0];}public String getPath(){return value.split(":",2)[1];}
 public String toString(){return value;}public boolean equals(Object other){return other instanceof ResourceLocation&&value.equals(((ResourceLocation)other).value);}public int hashCode(){return value.hashCode();}
}''')
put('net.minecraft.util.RegistryKey', '''public class RegistryKey<T> {private final ResourceLocation key;public RegistryKey(String key){this.key=new ResourceLocation(key);}public ResourceLocation location(){return key;}}''')
put('net.minecraft.world.World', '''public class World {
 public boolean isClientSide;public long gameTime;public String dimension="minecraft:overworld";
 public long getGameTime(){return gameTime;}public net.minecraft.util.RegistryKey<World> dimension(){return new net.minecraft.util.RegistryKey<>(dimension);}
}''')
put('net.minecraft.entity.LivingEntity', '''public class LivingEntity {
 public net.minecraft.world.World level;public boolean alive=true,shift;public int tickCount;public java.util.Random random=new java.util.Random(1);
 private final net.minecraft.nbt.CompoundNBT persistent=new net.minecraft.nbt.CompoundNBT();
 public LivingEntity(net.minecraft.world.World world){level=world;}public net.minecraft.nbt.CompoundNBT getPersistentData(){return persistent;}
 public java.util.Random getRandom(){return random;}public boolean isAlive(){return alive;}public boolean isShiftKeyDown(){return shift;}public boolean isSpectator(){return false;}
 public int getId(){return System.identityHashCode(this);}
 public java.util.UUID getUUID(){return new java.util.UUID(0,System.identityHashCode(this));}
}''')
put('net.minecraft.entity.player.PlayerEntity', '''public class PlayerEntity extends net.minecraft.entity.LivingEntity {
 public int syncs;public final java.util.List<String> messages=new java.util.ArrayList<>();public boolean spectator;
 public PlayerEntity(net.minecraft.world.World world){super(world);}public boolean isSpectator(){return spectator;}
 public void sendMessage(net.minecraft.util.text.ITextComponent text,java.util.UUID sender){messages.add(text.getString());}
 public void displayClientMessage(net.minecraft.util.text.ITextComponent text,boolean bar){messages.add(text.getString());}
 public int getId(){return System.identityHashCode(this);}
}''')
put('net.minecraft.entity.player.ServerPlayerEntity', 'public class ServerPlayerEntity extends PlayerEntity {public ServerPlayerEntity(net.minecraft.world.World world){super(world);}}')
put('net.minecraft.util.text.ITextComponent', 'public interface ITextComponent {String getString();}')
put('net.minecraft.util.text.StringTextComponent', 'public class StringTextComponent implements ITextComponent {private final String value;public StringTextComponent(String value){this.value=value;}public String getString(){return value;}}')
put('net.minecraft.util.text.TranslationTextComponent', 'public class TranslationTextComponent extends StringTextComponent {public TranslationTextComponent(String value,Object... args){super(value);}}')
put('net.minecraftforge.eventbus.api.EventPriority', 'public enum EventPriority {HIGHEST,HIGH,NORMAL,LOW,LOWEST}')
put('net.minecraftforge.eventbus.api.SubscribeEvent', '''@java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
public @interface SubscribeEvent {EventPriority priority() default EventPriority.NORMAL;boolean receiveCanceled() default false;}''')
put('net.minecraftforge.fml.common.Mod', 'public @interface Mod {public @interface EventBusSubscriber {String modid();}}')
put('net.minecraftforge.event.TickEvent', '''public class TickEvent {
 public enum Phase {START,END}public static class PlayerTickEvent {public final net.minecraft.entity.player.PlayerEntity player;public final Phase phase;public PlayerTickEvent(net.minecraft.entity.player.PlayerEntity player,Phase phase){this.player=player;this.phase=phase;}}
}''')
put('net.minecraftforge.event.entity.player.PlayerEvent', '''public class PlayerEvent {
 private final net.minecraft.entity.player.PlayerEntity player;public PlayerEvent(net.minecraft.entity.player.PlayerEntity player){this.player=player;}public net.minecraft.entity.player.PlayerEntity getPlayer(){return player;}
 public static class PlayerLoggedOutEvent extends PlayerEvent {public PlayerLoggedOutEvent(net.minecraft.entity.player.PlayerEntity player){super(player);}}
 public static class PlayerLoggedInEvent extends PlayerEvent {public PlayerLoggedInEvent(net.minecraft.entity.player.PlayerEntity player){super(player);}}
 public static class PlayerRespawnEvent extends PlayerEvent {public PlayerRespawnEvent(net.minecraft.entity.player.PlayerEntity player){super(player);}}
 public static class PlayerChangedDimensionEvent extends PlayerEvent {public PlayerChangedDimensionEvent(net.minecraft.entity.player.PlayerEntity player){super(player);}}
 public static class StartTracking extends PlayerEvent {private final net.minecraft.entity.LivingEntity target;public StartTracking(net.minecraft.entity.player.PlayerEntity viewer,net.minecraft.entity.LivingEntity target){super(viewer);this.target=target;}public net.minecraft.entity.LivingEntity getTarget(){return target;}}
 public static class Clone extends PlayerEvent {private final net.minecraft.entity.player.PlayerEntity original;public Clone(net.minecraft.entity.player.PlayerEntity player,net.minecraft.entity.player.PlayerEntity original){super(player);this.original=original;}public net.minecraft.entity.player.PlayerEntity getOriginal(){return original;}}
}''')
put('net.minecraftforge.event.entity.living.LivingDeathEvent', '''public class LivingDeathEvent {private final net.minecraft.entity.LivingEntity entity;public LivingDeathEvent(net.minecraft.entity.LivingEntity entity){this.entity=entity;}public net.minecraft.entity.LivingEntity getEntityLiving(){return entity;}}''')
put('xyz.pixelatedw.mineminenomi.api.abilities.AbilityUnlock', 'public enum AbilityUnlock {COMMAND,PROGRESSION}')
put('xyz.pixelatedw.mineminenomi.api.abilities.AbilityUseResult', '''public class AbilityUseResult {
 private final boolean success;private AbilityUseResult(boolean success){this.success=success;}public static AbilityUseResult success(){return new AbilityUseResult(true);}
 public static AbilityUseResult fail(Object message){return new AbilityUseResult(false);}public boolean isSuccess(){return success;}public boolean isFail(){return !success;}
}''')
put('xyz.pixelatedw.mineminenomi.api.abilities.AbilityKey', '''public class AbilityKey<T> {private final net.minecraft.util.ResourceLocation id;public AbilityKey(String id){this.id=new net.minecraft.util.ResourceLocation("fixture",id);}public net.minecraft.util.ResourceLocation getId(){return id;}}''')
put('xyz.pixelatedw.mineminenomi.api.abilities.IAbility', '''public interface IAbility {
 AbilityCore<?> getCore();<T> java.util.Optional<T> getComponent(AbilityKey<T> key);java.util.Map<AbilityKey<?>,xyz.pixelatedw.mineminenomi.api.abilities.components.AbilityComponent<?>> getComponents();
 net.minecraft.nbt.CompoundNBT save(net.minecraft.nbt.CompoundNBT nbt);void load(net.minecraft.nbt.CompoundNBT nbt);boolean isDisabled();
}''')
put('xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore', '''public class AbilityCore<T extends IAbility> {
 private final net.minecraft.util.ResourceLocation key;private final java.util.function.Function<AbilityCore<T>,T> factory;
 public AbilityCore(String key,java.util.function.Function<AbilityCore<T>,T> factory){this.key=new net.minecraft.util.ResourceLocation(key);this.factory=factory;xyz.pixelatedw.mineminenomi.api.ModRegistries.ABILITIES.register(this);}
 public T createAbility(){return factory.apply(this);}public net.minecraft.util.ResourceLocation getRegistryName(){return key;}
 public net.minecraft.util.text.ITextComponent getLocalizedName(){return new net.minecraft.util.text.StringTextComponent(key.toString());}
}''')
put('xyz.pixelatedw.mineminenomi.api.abilities.components.AbilityComponent', '''public class AbilityComponent<T> {
 public net.minecraft.nbt.CompoundNBT save(){return new net.minecraft.nbt.CompoundNBT();}public void load(net.minecraft.nbt.CompoundNBT nbt){}
}''')
put('xyz.pixelatedw.mineminenomi.api.abilities.components.ChargeComponent', '''public class ChargeComponent extends AbilityComponent<Object> {
 public boolean charging;public int stops;public Runnable onStop;public boolean isCharging(){return charging;}public void stopCharging(net.minecraft.entity.LivingEntity entity){stops++;boolean running=charging;charging=false;if(running&&onStop!=null)onStop.run();}
}''')
put('xyz.pixelatedw.mineminenomi.api.abilities.components.ContinuousComponent', '''public class ContinuousComponent extends AbilityComponent<Object> {
 public boolean continuous;public int stops;public boolean isContinuous(){return continuous;}public void stopContinuity(net.minecraft.entity.LivingEntity entity){stops++;continuous=false;}
}''')
put('xyz.pixelatedw.mineminenomi.api.abilities.components.CooldownComponent', '''public class CooldownComponent extends AbilityComponent<Object> {
 public interface IStartCooldownEvent {void startCooldown(net.minecraft.entity.LivingEntity entity,xyz.pixelatedw.mineminenomi.api.abilities.IAbility ability);}
 private final xyz.pixelatedw.mineminenomi.api.abilities.IAbility ability;private final java.util.List<IStartCooldownEvent> starts=new java.util.ArrayList<>();public float ticks,start;
 public CooldownComponent(xyz.pixelatedw.mineminenomi.api.abilities.IAbility ability){this.ability=ability;}
 public CooldownComponent addStartEvent(IStartCooldownEvent event){starts.add(event);return this;}public CooldownComponent addStartEvent(int priority,IStartCooldownEvent event){return addStartEvent(event);}
 public void startCooldown(net.minecraft.entity.LivingEntity entity,float ticks){if(isOnCooldown())return;this.ticks=Math.max(0,ticks);this.start=this.ticks;for(IStartCooldownEvent event:starts)event.startCooldown(entity,ability);}
 public void stopCooldown(net.minecraft.entity.LivingEntity entity){ticks=0;}public boolean isOnCooldown(){return ticks>0;}public float getStartCooldown(){return start;}public float getCooldown(){return ticks;}
 public net.minecraft.nbt.CompoundNBT save(){net.minecraft.nbt.CompoundNBT result=new net.minecraft.nbt.CompoundNBT();result.putFloat("ticks",ticks);result.putFloat("start",start);return result;}public void load(net.minecraft.nbt.CompoundNBT nbt){ticks=nbt.getFloat("ticks");start=nbt.getFloat("start");}
}''')
put('xyz.pixelatedw.mineminenomi.init.ModAbilityKeys', '''public class ModAbilityKeys {
 public static final xyz.pixelatedw.mineminenomi.api.abilities.AbilityKey<xyz.pixelatedw.mineminenomi.api.abilities.components.ChargeComponent> CHARGE=new xyz.pixelatedw.mineminenomi.api.abilities.AbilityKey<>("charge");
 public static final xyz.pixelatedw.mineminenomi.api.abilities.AbilityKey<xyz.pixelatedw.mineminenomi.api.abilities.components.ContinuousComponent> CONTINUOUS=new xyz.pixelatedw.mineminenomi.api.abilities.AbilityKey<>("continuous");
 public static final xyz.pixelatedw.mineminenomi.api.abilities.AbilityKey<xyz.pixelatedw.mineminenomi.api.abilities.components.CooldownComponent> COOLDOWN=new xyz.pixelatedw.mineminenomi.api.abilities.AbilityKey<>("cooldown");
}''')
put('xyz.pixelatedw.mineminenomi.api.abilities.Ability', '''public class Ability implements IAbility {
 private final AbilityCore<?> core;public boolean disabled;public int stops;public final xyz.pixelatedw.mineminenomi.api.abilities.components.ChargeComponent charge=new xyz.pixelatedw.mineminenomi.api.abilities.components.ChargeComponent();
 public final xyz.pixelatedw.mineminenomi.api.abilities.components.ContinuousComponent continuous=new xyz.pixelatedw.mineminenomi.api.abilities.components.ContinuousComponent();public final xyz.pixelatedw.mineminenomi.api.abilities.components.CooldownComponent cooldown=new xyz.pixelatedw.mineminenomi.api.abilities.components.CooldownComponent(this);
 private final java.util.Map<AbilityKey<?>,xyz.pixelatedw.mineminenomi.api.abilities.components.AbilityComponent<?>> components=new java.util.LinkedHashMap<>();
 public Ability(AbilityCore<?> core){this.core=core;components.put(xyz.pixelatedw.mineminenomi.init.ModAbilityKeys.CHARGE,charge);components.put(xyz.pixelatedw.mineminenomi.init.ModAbilityKeys.CONTINUOUS,continuous);components.put(xyz.pixelatedw.mineminenomi.init.ModAbilityKeys.COOLDOWN,cooldown);}
 public AbilityCore<?> getCore(){return core;}@SuppressWarnings("unchecked")public <T>java.util.Optional<T> getComponent(AbilityKey<T> key){return java.util.Optional.ofNullable((T)components.get(key));}
 public java.util.Map<AbilityKey<?>,xyz.pixelatedw.mineminenomi.api.abilities.components.AbilityComponent<?>> getComponents(){return components;}
 public net.minecraft.nbt.CompoundNBT save(net.minecraft.nbt.CompoundNBT nbt){return nbt;}public void load(net.minecraft.nbt.CompoundNBT nbt){}public boolean isDisabled(){return disabled;}
 public boolean isCharging(){return charge.charging;}public boolean isContinuous(){return continuous.continuous;}
}''')
put('xyz.pixelatedw.mineminenomi.api.ModRegistries', '''public class ModRegistries {
 public static final Registry ABILITIES=new Registry();public static class Registry {
 private final java.util.Map<net.minecraft.util.ResourceLocation,xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore<?>> values=new java.util.LinkedHashMap<>();
 public void register(xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore<?> core){values.put(core.getRegistryName(),core);}
 public net.minecraft.util.ResourceLocation getKey(xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore<?> core){return core==null?null:core.getRegistryName();}
 public xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore<?> getValue(net.minecraft.util.ResourceLocation key){return values.get(key);}
 public java.util.Collection<xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore<?>> getValues(){return values.values();}
 }}''')
put('xyz.pixelatedw.mineminenomi.data.entity.ability.IAbilityData', '''public class IAbilityData {
 public final java.util.List<xyz.pixelatedw.mineminenomi.api.abilities.IAbility> equipped=new java.util.ArrayList<>(java.util.Collections.nCopies(6,null));
 public final java.util.Set<xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore<?>> passives=new java.util.HashSet<>();
 public final java.util.Map<xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore<?>,xyz.pixelatedw.mineminenomi.api.abilities.AbilityUnlock> unlocked=new java.util.HashMap<>();
 public boolean denyGrant;public xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore<?> denyEquipCore;
 public java.util.List<xyz.pixelatedw.mineminenomi.api.abilities.IAbility> getRawEquippedAbilities(){return equipped;}
 public boolean setEquippedAbility(int slot,xyz.pixelatedw.mineminenomi.api.abilities.IAbility ability){if(ability!=null&&(ability.getCore()==denyEquipCore||(hasEquippedAbility(ability.getCore())&&equipped.get(slot)!=ability)))return false;equipped.set(slot,ability);return true;}
 public int getEquippedAbilitySlot(xyz.pixelatedw.mineminenomi.api.abilities.IAbility ability){return equipped.indexOf(ability);}
 public xyz.pixelatedw.mineminenomi.api.abilities.IAbility getEquippedAbility(int slot){return equipped.get(slot);}
 public boolean hasEquippedAbility(xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore<?> core){return getEquippedAbility(core)!=null;}
 public boolean hasPassiveAbility(xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore<?> core){return passives.contains(core);}
 @SuppressWarnings("unchecked")public <T extends xyz.pixelatedw.mineminenomi.api.abilities.IAbility>T getEquippedAbility(xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore<T> core){for(xyz.pixelatedw.mineminenomi.api.abilities.IAbility ability:equipped)if(ability!=null&&ability.getCore()==core)return (T)ability;return null;}
 public boolean hasUnlockedAbility(xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore<?> core){return unlocked.containsKey(core);}
 public xyz.pixelatedw.mineminenomi.api.abilities.AbilityUnlock getUnlockTypeForAbility(xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore<?> core){return unlocked.get(core);}
 public boolean addUnlockedAbility(xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore<?> core,xyz.pixelatedw.mineminenomi.api.abilities.AbilityUnlock type){if(denyGrant||unlocked.containsKey(core))return false;unlocked.put(core,type);return true;}
 public boolean removeUnlockedAbility(xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore<?> core){boolean result=unlocked.remove(core)!=null;removeEquippedAbility(core);return result;}
 public boolean removeEquippedAbility(xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore<?> core){boolean removed=false;for(int i=0;i<equipped.size();i++)if(equipped.get(i)!=null&&equipped.get(i).getCore()==core){equipped.set(i,null);removed=true;}return removed;}
 public java.util.List<xyz.pixelatedw.mineminenomi.api.abilities.IAbility> getEquippedAndPassiveAbilities(){java.util.List<xyz.pixelatedw.mineminenomi.api.abilities.IAbility> result=new java.util.ArrayList<>(equipped);result.removeIf(java.util.Objects::isNull);return result;}
}''')
put('xyz.pixelatedw.mineminenomi.data.entity.ability.AbilityDataCapability', '''public class AbilityDataCapability {
 public static final java.util.Map<net.minecraft.entity.LivingEntity,IAbilityData> DATA=new java.util.IdentityHashMap<>();public static IAbilityData get(net.minecraft.entity.LivingEntity entity){return DATA.get(entity);}
}''')
put('xyz.pixelatedw.mineminenomi.data.entity.devilfruit.IDevilFruit', '''public class IDevilFruit {public boolean awakened=true,supa=true;public boolean hasAwakenedFruit(){return awakened;}public boolean hasDevilFruit(Object fruit){return supa;}}''')
put('xyz.pixelatedw.mineminenomi.data.entity.devilfruit.DevilFruitCapability', '''public class DevilFruitCapability {
 public static final java.util.Map<net.minecraft.entity.LivingEntity,IDevilFruit> DATA=new java.util.IdentityHashMap<>();
 public static IDevilFruit get(net.minecraft.entity.LivingEntity entity){return DATA.computeIfAbsent(entity,key->new IDevilFruit());}
}''')
put('xyz.pixelatedw.mineminenomi.init.ModAbilities', 'public class ModAbilities {public static final Object SUPA_SUPA_NO_MI=new Object();}')
put('xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper', '''public class AbilityHelper {
 public static final java.util.Set<xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore<?>> BANNED=new java.util.HashSet<>();
 public static java.util.function.BiConsumer<net.minecraft.entity.LivingEntity,xyz.pixelatedw.mineminenomi.api.abilities.IAbility> onEmergencyStop;
 public static boolean verifyIfAbilityIsBanned(xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore<?> core){return BANNED.contains(core);}
 public static void emergencyStopAbility(net.minecraft.entity.LivingEntity entity,xyz.pixelatedw.mineminenomi.api.abilities.IAbility ability){if(onEmergencyStop!=null)onEmergencyStop.accept(entity,ability);xyz.pixelatedw.mineminenomi.api.abilities.Ability a=(xyz.pixelatedw.mineminenomi.api.abilities.Ability)ability;a.stops++;a.charge.stopCharging(entity);a.continuous.stopContinuity(entity);a.cooldown.startCooldown(entity,40);}
}''')
put('xyz.pixelatedw.mineminenomi.config.CommonConfig', '''public class CommonConfig {public static final CommonConfig INSTANCE=new CommonConfig();public boolean fraudChecks=true;public boolean isAbilityFraudChecksEnabled(){return fraudChecks;}}''')
put('xyz.pixelatedw.mineminenomi.api.events.ability.AbilityUseEvent', '''public class AbilityUseEvent {
 private final net.minecraft.entity.LivingEntity entity;private final xyz.pixelatedw.mineminenomi.api.abilities.IAbility ability;
 public AbilityUseEvent(net.minecraft.entity.LivingEntity entity,xyz.pixelatedw.mineminenomi.api.abilities.IAbility ability){this.entity=entity;this.ability=ability;}
 public net.minecraft.entity.LivingEntity getEntityLiving(){return entity;}public xyz.pixelatedw.mineminenomi.api.abilities.IAbility getAbility(){return ability;}
 public static class Pre extends AbilityUseEvent {public boolean canceled;public Pre(net.minecraft.entity.LivingEntity entity,xyz.pixelatedw.mineminenomi.api.abilities.IAbility ability){super(entity,ability);}public void setCanceled(boolean value){canceled=value;}public boolean isCanceled(){return canceled;}}
 public static class Post extends AbilityUseEvent {public Post(net.minecraft.entity.LivingEntity entity,xyz.pixelatedw.mineminenomi.api.abilities.IAbility ability){super(entity,ability);}}
}''')
put('net.kazi.kazimod.abilities.SupaRework.ProjectionAbility', '''public class ProjectionAbility extends xyz.pixelatedw.mineminenomi.api.abilities.Ability {
 public static final xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore<ProjectionAbility> INSTANCE=new xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore<>("kazimod:projection",ProjectionAbility::new);
 public String mode="LEGENDARY_WEAPON_TRACE";public ProjectionAbility(xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore<ProjectionAbility> core){super(core);}
 public net.minecraft.nbt.CompoundNBT save(net.minecraft.nbt.CompoundNBT nbt){nbt.putString("projectionMode",mode);return super.save(nbt);}public void load(net.minecraft.nbt.CompoundNBT nbt){super.load(nbt);mode=nbt.getString("projectionMode");}
 void restoreTraceState(net.minecraft.nbt.CompoundNBT nbt){load(nbt);net.minecraft.nbt.CompoundNBT components=nbt.getCompound("components");getComponents().forEach((key,value)->{String id=key.getId().toString();if(components.contains(id,10))value.load(components.getCompound(id));});}
 public void startTraceReturnCooldown(net.minecraft.entity.LivingEntity entity,float ticks){cooldown.startCooldown(entity,ticks);}
}''')
put('net.kazi.kazimod.abilities.SupaRework.RealityMarbleAbility', '''public class RealityMarbleAbility extends xyz.pixelatedw.mineminenomi.api.abilities.Ability {
 public enum Mode {MUGEN_NO_KENSEI,INFINITE_CREATION_OF_SWORDS,VOID_CREATION_OF_SWORDS}
 public static final xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore<RealityMarbleAbility> INSTANCE=new xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore<>("kazimod:reality_marble",RealityMarbleAbility::new);
 public Mode mode=Mode.MUGEN_NO_KENSEI;public boolean active=true;public RealityMarbleAbility(xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore<RealityMarbleAbility> core){super(core);}
 public boolean isModeActive(Mode mode){return active&&this.mode==mode;}public boolean isActive(){return active;}
}''')
put('net.kazi.kazimod.abilities.Nusu.SkillHunterEXAbility', 'public class SkillHunterEXAbility {public static final long ROLLED_LIFETIME_TICKS=300;public static final float COOLDOWN=200;}')
put('net.kazi.kazimod.abilities.Nusu.NusuEvents', '''public class NusuEvents {public static void syncAbilityData(net.minecraft.entity.player.PlayerEntity player,xyz.pixelatedw.mineminenomi.data.entity.ability.IAbilityData data){player.syncs++;}}''')
put('net.kazi.kazimod.abilities.Nusu.SkillHunterAbility', '''public class SkillHunterAbility {public static void sendMsg(net.minecraft.entity.LivingEntity entity,String text){if(entity instanceof net.minecraft.entity.player.PlayerEntity)((net.minecraft.entity.player.PlayerEntity)entity).messages.add(text);}}''')
put('xyz.pixelatedw.mineminenomi.packets.server.SSyncAbilityDataPacket', '''public class SSyncAbilityDataPacket {public final int id;public SSyncAbilityDataPacket(int id,xyz.pixelatedw.mineminenomi.data.entity.ability.IAbilityData data){this.id=id;}}''')
put('net.kazi.kazimod.network.LegendaryWeaponTracePacket', '''public class LegendaryWeaponTracePacket {public final int id;public final net.minecraft.nbt.CompoundNBT state;public LegendaryWeaponTracePacket(int id,net.minecraft.nbt.CompoundNBT state){this.id=id;this.state=state.copy();}}''')
put('net.minecraftforge.fml.network.PacketDistributor', '''public class PacketDistributor {
 public static final Distributor PLAYER=new Distributor("player"),TRACKING_ENTITY_AND_SELF=new Distributor("tracking_and_self");
 public static class Target {public final String kind;public final net.minecraft.entity.LivingEntity entity;public Target(String kind,net.minecraft.entity.LivingEntity entity){this.kind=kind;this.entity=entity;}}
 public static class Distributor {private final String kind;public Distributor(String kind){this.kind=kind;}public Target with(java.util.function.Supplier<? extends net.minecraft.entity.LivingEntity> entity){return new Target(kind,entity.get());}}
}''')
put('net.kazi.kazimod.testing.TraceNetwork', '''public class TraceNetwork {
 public static final java.util.List<Record> events=new java.util.ArrayList<>();
 public static class Record {public final String kind,audience;public final int subject;public final net.minecraft.entity.LivingEntity recipient;public final net.minecraft.nbt.CompoundNBT state;
 public Record(String kind,String audience,int subject,net.minecraft.entity.LivingEntity recipient,net.minecraft.nbt.CompoundNBT state){this.kind=kind;this.audience=audience;this.subject=subject;this.recipient=recipient;this.state=state==null?null:state.copy();}}
 public static void record(String audience,net.minecraft.entity.LivingEntity recipient,Object packet){
 if(packet instanceof net.kazi.kazimod.network.LegendaryWeaponTracePacket){net.kazi.kazimod.network.LegendaryWeaponTracePacket p=(net.kazi.kazimod.network.LegendaryWeaponTracePacket)packet;events.add(new Record("state",audience,p.id,recipient,p.state));}
 else {xyz.pixelatedw.mineminenomi.packets.server.SSyncAbilityDataPacket p=(xyz.pixelatedw.mineminenomi.packets.server.SSyncAbilityDataPacket)packet;events.add(new Record("abilities",audience,p.id,recipient,null));if(recipient instanceof net.minecraft.entity.player.PlayerEntity)((net.minecraft.entity.player.PlayerEntity)recipient).syncs++;}
 }
}''')
put('net.kazi.kazimod.init.KaziPacketHandler', '''public class KaziPacketHandler {
 public static final Channel CHANNEL=new Channel();public static class Channel {public void send(net.minecraftforge.fml.network.PacketDistributor.Target target,Object packet){net.kazi.kazimod.testing.TraceNetwork.record(target.kind,target.entity,packet);}}
}''')
put('xyz.pixelatedw.mineminenomi.wypi.WyNetwork', '''public class WyNetwork {
 public static void sendTo(Object packet,net.minecraft.entity.player.PlayerEntity player){net.kazi.kazimod.testing.TraceNetwork.record("player",player,packet);}
 public static void sendToAllTrackingAndSelf(Object packet,net.minecraft.entity.LivingEntity player){net.kazi.kazimod.testing.TraceNetwork.record("tracking_and_self",player,packet);}
}''')

# Only the chosen moves' external implementations are fixtures. Their actual
# production class references come from the manager, preserving the fixed pool.
manager = MANAGER.read_text(encoding='utf-8')
imports = dict((name.rsplit('.', 1)[1], name) for name in re.findall(r'import ([\w.]+);', manager))
for name in re.findall(r'([\w.]+)\.INSTANCE', manager):
    qualified = imports.get(name, name if '.' in name else 'net.kazi.kazimod.abilities.SupaRework.' + name)
    if qualified in sources:
        continue
    simple = qualified.rsplit('.', 1)[1]
    put(qualified, '''public class %s extends xyz.pixelatedw.mineminenomi.api.abilities.Ability {
 public static final xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore<%s> INSTANCE=new xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore<>("fixture:%s",%s::new);
 public %s(xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore<%s> core){super(core);}
}''' % (simple, simple, simple.lower(), simple, simple, simple))

paths = []
for name, code in sources.items():
    path = OUT / 'fixtures' / (name.replace('.', '/') + '.java')
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(code, encoding='utf-8')
    paths.append(str(path))
paths.append(str(Path(__file__).with_name('LegendaryWeaponTraceTest.java')))


def compile_and_run(manager_source, classes, capture=False):
    classes.mkdir(parents=True, exist_ok=True)
    subprocess.run([str(JAVA / 'javac.exe'), '-encoding', 'UTF-8', '-d', str(classes),
                    *paths, str(manager_source)], check=True)
    return subprocess.run([str(JAVA / 'java.exe'), '-cp', str(classes),
                           'LegendaryWeaponTraceTest'], capture_output=capture,
                          text=True, check=not capture)


# A fixed RNG reproduces the old repeat streak deterministically. Only the
# temporary build copy uses independent draws; production source is untouched.
old_draw = 'List<AbilityCore<?>> remaining = remainingCycle(user, choices);'
assert manager.count(old_draw) == 1
baseline = OUT / 'baseline-source/LegendaryWeaponTrace.java'
baseline.parent.mkdir(parents=True, exist_ok=True)
baseline.write_text(manager.replace(old_draw,
                    'List<AbilityCore<?>> remaining = new ArrayList<>(choices);'),
                    encoding='utf-8')
result = compile_and_run(baseline, OUT / 'baseline-classes', capture=True)
assert result.returncode != 0 and 'All eleven configured moves can be rolled' in result.stderr, result
print('Confirmed independent random draws can starve moves; shuffled cycles prevent the streak.', flush=True)
compile_and_run(MANAGER, OUT / 'classes')
