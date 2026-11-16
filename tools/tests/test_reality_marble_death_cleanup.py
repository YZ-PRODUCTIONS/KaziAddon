"""Exercise production Reality Marble cleanup and event methods with small world fixtures."""
from pathlib import Path
import re
import subprocess

ROOT = Path(__file__).resolve().parents[2]
OUT = ROOT / 'build/reality-marble-death-cleanup-tests'
JAVA = Path('C:/Users/th3wyz/.jdks/corretto-1.8.0_482/bin')
SOURCE = ROOT / 'src/main/java/net/kazi/kazimod/abilities/SupaRework/RealityMarbleAbility.java'
source = SOURCE.read_text(encoding='utf-8')


def member(signature):
    start = source.index(signature)
    opening = source.index('{', start)
    depth = 1
    end = opening + 1
    while depth:
        depth += (source[end] == '{') - (source[end] == '}')
        end += 1
    # Only visibility is relaxed, to let the fixture inspect post-cleanup state.
    return source[start:end].replace('private ', '')


signatures = [
    'public enum Mode {',
    'private void clearChargeMovementLock(',
    'private void fadeChargeMusic(',
    'private static boolean isContinuousMarbleMode(',
    'private void onContinuityEnd(',
    'private void finishLostWorks(',
    'private void clearLostWorks(',
    'private static float getCooldown(',
    'private void removeWeaponDisplays(',
    'private void removeGearDisplays(',
    'private void removeGroundGearDisplays(',
    'private void removeAerialGearDisplays(',
    'private void removeFirstModeSphere(',
    'private void removeSecondModeSphere(',
    'private static void beginMarbleTracking(',
    'private static void trackMarbleBlock(',
    'private void cleanupMarble(',
    'private static void queueLogoutRestoration(UUID',
    'private void processFirstModeReplacement(',
    'private void processSecondModeReplacement(',
    'private static void replaceWithCustomMarbleTerrain(',
    'private static boolean isCustomMarbleTerrain(',
    'private static void queueRestoration(',
    'private static void processRestoration(',
    'private static void cleanupCasterMarble(',
    'public static class LogoutCleanupHandler {',
    'private static class LogoutRestoration {',
    'private static class ActiveMarble {',
]
constants = []
for name in ('BLOCKS_PER_TICK', 'RESTORE_BLOCKS_PER_TICK', 'MARBLE_HOLD_TICKS',
             'MARBLE_MIN_COOLDOWN', 'FIRST_COOLDOWN', 'SECOND_COOLDOWN', 'THIRD_COOLDOWN'):
    constants.append(re.search(r'private static final [^\n]+\b' + name + r'\s*=[^;]+;', source).group().replace('private ', ''))

fields_start = source.index('    private final Deque<BlockPos> firstModeReplacementQueue')
fields_end = source.index('\n\n', source.index('    private final List<ServerPlayerEntity> musicListeners', fields_start))
fields = source[fields_start:fields_end].replace('private ', '')
fixture = '''package net.kazi.kazimod.abilities.SupaRework;
import java.util.*;
import java.util.function.Supplier;

class RealityMarbleAbility {
 static final Object INSTANCE = new Object();
 static final Map<UUID, ActiveMarble> ACTIVE_MARBLES = new HashMap<>();
 static final Map<UUID, LogoutRestoration> PENDING_LOGOUT_RESTORATIONS = new HashMap<>();
 final ChargeComponent chargeComponent = new ChargeComponent();
 final ContinuousComponent continuousComponent = new ContinuousComponent(this);
 final CooldownComponent cooldownComponent = new CooldownComponent();
 Mode activeMode = Mode.MUGEN_NO_KENSEI;
 UnlimitedLostWorksEntity lostWorks;
''' + '\n'.join(constants) + '\n' + fields + '\n' + '\n'.join(member(s) for s in signatures) + '\n}\n'

fixture += '''
interface IAbility {}
class ChargeComponent {
 boolean active;int forceStops;
 boolean isCharging(){return active;}
 void forceStopCharging(LivingEntity entity){active=false;forceStops++;}
}
class ContinuousComponent {
 final RealityMarbleAbility owner;boolean active;int stops;
 ContinuousComponent(RealityMarbleAbility owner){this.owner=owner;}
 boolean isContinuous(){return active;}
 float getContinueTime(){return 200;}
 void stopContinuity(LivingEntity entity){active=false;stops++;owner.onContinuityEnd(entity,null);}
}
class CooldownComponent {float cooldown;void startCooldown(LivingEntity entity,float ticks){cooldown=ticks;}}
class Block {
 final String name;Block(String name){this.name=name;}
 BlockState defaultBlockState(){return new BlockState(this);}
}
class BlockState {
 final Block block;BlockState(Block block){this.block=block;}
 Block getBlock(){return block;}boolean isAir(){return block==Blocks.AIR;}
 boolean is(Block other){return block==other;}boolean hasTileEntity(){return false;}
 float getDestroySpeed(World world,BlockPos pos){return 1;}
 FluidState getFluidState(){return new FluidState();}
}
class FluidState {boolean isEmpty(){return true;}}
class Blocks {
 static final Block AIR=new Block("air"),BEDROCK=new Block("bedrock"),BARRIER=new Block("barrier");
}
class Registry<T> {final T value;Registry(T value){this.value=value;}T get(){return value;}}
class KaziBlocks {
 static final Registry<Block> CUSTOM_COARSE_SAND=new Registry<>(new Block("marble_sand"));
 static final Registry<Block> CUSTOM_COARSE_DIRT=new Registry<>(new Block("marble_dirt"));
}
class BlockPos {
 final int x,y,z;BlockPos(int x,int y,int z){this.x=x;this.y=y;this.z=z;}
 BlockPos immutable(){return this;}int getX(){return x;}int getY(){return y;}int getZ(){return z;}
 public boolean equals(Object other){if(!(other instanceof BlockPos))return false;BlockPos p=(BlockPos)other;return x==p.x&&y==p.y&&z==p.z;}
 public int hashCode(){return Objects.hash(x,y,z);}
}
class World {
 boolean isClientSide;int writes;
 final Map<BlockPos,BlockState> blocks=new HashMap<>();
 BlockState getBlockState(BlockPos pos){return blocks.getOrDefault(pos,Blocks.AIR.defaultBlockState());}
 void setBlock(BlockPos pos,BlockState state,int flags){blocks.put(pos,state);writes++;}
}
class EffectInstance {
 final int duration,amplifier;EffectInstance(int duration,int amplifier){this.duration=duration;this.amplifier=amplifier;}
 int getDuration(){return duration;}int getAmplifier(){return amplifier;}
}
class ModEffects {static final Registry<Object> MOVEMENT_BLOCKED=new Registry<>(new Object());}
class LivingEntity {
 World level;final UUID uuid=UUID.randomUUID();IAbilityData abilities;EffectInstance lock;
 LivingEntity(World level){this.level=level;}UUID getUUID(){return uuid;}
 EffectInstance getEffect(Object effect){return lock;}void removeEffect(Object effect){lock=null;}
}
class ServerPlayerEntity extends LivingEntity {int fades;ServerPlayerEntity(World world){super(world);}}
class IAbilityData {
 RealityMarbleAbility ability;IAbilityData(RealityMarbleAbility ability){this.ability=ability;}
 RealityMarbleAbility getEquippedAbility(Object core){return ability;}
}
class AbilityDataCapability {static IAbilityData get(LivingEntity entity){return entity.abilities;}}
class DisplayEntity {boolean alive=true;int removes;boolean isAlive(){return alive;}void remove(){alive=false;removes++;}}
class RealityMarbleWeaponEntity extends DisplayEntity {}
class RealityMarbleGearEntity extends DisplayEntity {}
class SphereEntity extends DisplayEntity {}
class UnlimitedLostWorksEntity extends DisplayEntity {}
class RealityMarbleMusicPacket {
 final UUID session;final boolean start;RealityMarbleMusicPacket(UUID session,boolean start){this.session=session;this.start=start;}
}
class PacketDistributor {
 static final PacketDistributor PLAYER=new PacketDistributor();
 ServerPlayerEntity with(Supplier<ServerPlayerEntity> target){return target.get();}
}
class KaziPacketHandler {
 static final Channel CHANNEL=new Channel();
 static class Channel {void send(ServerPlayerEntity target,RealityMarbleMusicPacket packet){if(!packet.start)target.fades++;}}
}
enum EventPriority { NORMAL,LOWEST }
@interface SubscribeEvent {EventPriority priority() default EventPriority.NORMAL;}
class Mod {@interface EventBusSubscriber {String modid();}}
class LivingDeathEvent {
 final LivingEntity entity;boolean canceled;LivingDeathEvent(LivingEntity entity){this.entity=entity;}
 LivingEntity getEntityLiving(){return entity;}boolean isCanceled(){return canceled;}
}
class PlayerEvent {
 static class PlayerLoggedOutEvent {final LivingEntity player;PlayerLoggedOutEvent(LivingEntity player){this.player=player;}LivingEntity getPlayer(){return player;}}
 static class PlayerChangedDimensionEvent {final LivingEntity player;PlayerChangedDimensionEvent(LivingEntity player){this.player=player;}LivingEntity getPlayer(){return player;}}
}
class TickEvent {
 enum Phase {START,END}
 static class WorldTickEvent {World world;Phase phase;}
}
'''

OUT.mkdir(parents=True, exist_ok=True)
generated = OUT / 'RealityMarbleAbility.java'
generated.write_text(fixture, encoding='utf-8')
classes = OUT / 'classes'
classes.mkdir(exist_ok=True)
subprocess.run([str(JAVA / 'javac.exe'), '-encoding', 'UTF-8', '-d', str(classes),
                str(generated), str(Path(__file__).with_name('RealityMarbleDeathCleanupTest.java'))], check=True)
subprocess.run([str(JAVA / 'java.exe'), '-cp', str(classes),
                'net.kazi.kazimod.abilities.SupaRework.RealityMarbleDeathCleanupTest'], check=True)
