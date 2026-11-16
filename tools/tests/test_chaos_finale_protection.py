"""Exercise the real cutscene coordinator with recording player, network and event fixtures."""
from pathlib import Path
import subprocess

ROOT = Path(__file__).resolve().parents[2]
OUT = ROOT / 'output/validation/chaos-finale-protection-tests'
JAVA = Path('C:/Users/th3wyz/.jdks/corretto-1.8.0_482/bin')
fixtures = {
    'net.minecraft.util.math.vector.Vector3d': '''
public final class Vector3d { public final double x,y,z; public Vector3d(double x,double y,double z){this.x=x;this.y=y;this.z=z;} }
''',
    'net.minecraft.world.World': 'public class World { public boolean isClientSide; }',
    'net.minecraft.entity.LivingEntity': '''
import java.util.UUID;
import net.minecraft.world.World;
import net.minecraft.util.math.vector.Vector3d;
public class LivingEntity {
 public World level;public boolean removed,alive=true,eligible=true;public double x,y,z;public float yBodyRot;
 public UUID uuid=UUID.randomUUID();public final int id=uuid.hashCode();
 public LivingEntity(World world){level=world;}
 public UUID getUUID(){return uuid;}public int getId(){return id;}public boolean isAlive(){return alive;}
 public float getHealth(){return 10;}public float getMaxHealth(){return 100;}
 public Vector3d position(){return new Vector3d(x,y,z);}
 public double distanceToSqr(LivingEntity other){double a=x-other.x,b=y-other.y,c=z-other.z;return a*a+b*b+c*c;}
}
''',
    'net.minecraft.entity.player.ServerPlayerEntity': '''
public class ServerPlayerEntity extends net.minecraft.entity.LivingEntity {
 public ServerPlayerEntity(net.minecraft.world.World world){super(world);}
}
''',
    'net.minecraft.world.server.ServerWorld': '''
import java.util.*;
import net.minecraft.entity.player.ServerPlayerEntity;
public class ServerWorld extends net.minecraft.world.World {
 public long gameTime;public final List<ServerPlayerEntity> players=new ArrayList<>();public final Server server=new Server();
 public long getGameTime(){return gameTime;}public List<ServerPlayerEntity> players(){return players;}public Server getServer(){return server;}
 public ServerPlayerEntity add(double x){ServerPlayerEntity p=new ServerPlayerEntity(this);p.x=x;players.add(p);server.list.connected.put(p.getUUID(),p);return p;}
 public static class Server { public final PlayerList list=new PlayerList();public PlayerList getPlayerList(){return list;} }
 public static class PlayerList { public final Map<UUID,ServerPlayerEntity> connected=new HashMap<>();public ServerPlayerEntity getPlayer(UUID id){return connected.get(id);} }
}
''',
    'net.minecraftforge.event.TickEvent': '''
public class TickEvent { public enum Phase{START,END}public static class ServerTickEvent{public final Phase phase;public ServerTickEvent(Phase p){phase=p;}} }
''',
    'net.minecraftforge.eventbus.api.EventPriority': 'public enum EventPriority { HIGHEST,LOWEST,NORMAL }',
    'net.minecraftforge.eventbus.api.SubscribeEvent': '''
@java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
public @interface SubscribeEvent { EventPriority priority() default EventPriority.NORMAL; }
''',
    'net.minecraftforge.fml.common.Mod': 'public final class Mod { public @interface EventBusSubscriber { String modid(); } }',
    'net.minecraftforge.fml.event.server.FMLServerStoppedEvent': 'public class FMLServerStoppedEvent {}',
    'net.minecraftforge.fml.network.PacketDistributor': '''
public class PacketDistributor { public static final PacketDistributor PLAYER=new PacketDistributor();public Object with(java.util.function.Supplier<?> supplier){return supplier.get();} }
''',
    'net.minecraftforge.event.entity.player.PlayerEvent': '''
import net.minecraft.entity.LivingEntity;
public class PlayerEvent {
 private final LivingEntity player;public PlayerEvent(LivingEntity player){this.player=player;}public LivingEntity getPlayer(){return player;}
 public static class PlayerLoggedOutEvent extends PlayerEvent{public PlayerLoggedOutEvent(LivingEntity p){super(p);}}
 public static class PlayerLoggedInEvent extends PlayerEvent{public PlayerLoggedInEvent(LivingEntity p){super(p);}}
 public static class PlayerChangedDimensionEvent extends PlayerEvent{public PlayerChangedDimensionEvent(LivingEntity p){super(p);}}
 public static class Clone extends PlayerEvent{public Clone(LivingEntity p){super(p);}public LivingEntity getOriginal(){return getPlayer();}}
}
''',
    'net.kazi.kazimod.network.InnerDragonsCutscenePacket': '''
import java.util.UUID;
import net.kazi.kazimod.abilities.cinematics.AwakeningCutsceneType;
import net.minecraft.util.math.vector.Vector3d;
public class InnerDragonsCutscenePacket {
 public final boolean cancel;public final AwakeningCutsceneType type;
 public InnerDragonsCutscenePacket(int id,UUID uuid,long start,Vector3d origin,float yaw,boolean cancel,AwakeningCutsceneType type){this.cancel=cancel;this.type=type;}
}
''',
    'net.kazi.kazimod.init.KaziPacketHandler': '''
import java.util.*;
import net.kazi.kazimod.network.InnerDragonsCutscenePacket;
public class KaziPacketHandler {
 public static final Channel CHANNEL=new Channel();
 public static class Delivery { public final Object player;public final InnerDragonsCutscenePacket packet;Delivery(Object p,Object m){player=p;packet=(InnerDragonsCutscenePacket)m;} }
 public static class Channel { public final List<Delivery> sent=new ArrayList<>();public void send(Object p,Object m){sent.add(new Delivery(p,m));} }
}
''',
    'net.kazi.kazimod.abilities.cinematics.ChaosBeamFinaleLift': '''
import net.minecraft.entity.LivingEntity;
public class ChaosBeamFinaleLift {
 public static int ticks,finishes,completed,aborted,recoveries;
 public ChaosBeamFinaleLift(LivingEntity user){}
 public void tick(double age){ticks++;}
 public void finish(boolean normal){finishes++;if(normal)completed++;else aborted++;}
 public static void recover(LivingEntity user){recoveries++;}
 public static void reset(){ticks=finishes=completed=aborted=recoveries=0;}
}
''',
    'net.kazi.kazimod.abilities.YagiAwakening.ChaosBeamRecoil': '''
import java.util.*;
import java.util.function.Consumer;
import net.minecraft.entity.LivingEntity;
import net.minecraft.world.server.ServerWorld;
import net.kazi.kazimod.abilities.NagiAwakening.InnerDragonsCutscenes;
public class ChaosBeamRecoil {
 public static final List<Hit> hits=new ArrayList<>();public static Consumer<LivingEntity> onHit;
 public static class Hit {
  public final LivingEntity caster;public final long tick;public final boolean protectedAtHit;
  Hit(LivingEntity caster){this.caster=caster;tick=((ServerWorld)caster.level).getGameTime();protectedAtHit=InnerDragonsCutscenes.isProtected(caster);}
 }
 public static void hurtCaster(LivingEntity caster){ChaosBeamHealing.order.add("hurt:"+caster.getUUID());hits.add(new Hit(caster));if(onHit!=null)onHit.accept(caster);}
 public static void reset(){hits.clear();onHit=null;}
}
''',
    'net.kazi.kazimod.abilities.YagiAwakening.ChaosBeamHealing': '''
import java.util.*;
import net.minecraft.entity.LivingEntity;
import net.minecraft.world.server.ServerWorld;
import net.kazi.kazimod.abilities.NagiAwakening.InnerDragonsCutscenes;
public class ChaosBeamHealing {
 public static final List<Heal> heals=new ArrayList<>();public static final List<String> order=new ArrayList<>();
 public static class Heal {
  public final LivingEntity caster;public final long tick;public final boolean casterAlive,protectedAtHeal;
  Heal(LivingEntity caster){this.caster=caster;tick=((ServerWorld)caster.level).getGameTime();casterAlive=caster.isAlive();protectedAtHeal=InnerDragonsCutscenes.isProtected(caster);}
 }
 public static void healAllies(LivingEntity caster){order.add("heal:"+caster.getUUID());heals.add(new Heal(caster));}
 public static void reset(){heals.clear();order.clear();}
}
''',
    'net.kazi.kazimod.abilities.NagiAwakening.InnerDragonsSequence': '''
public class InnerDragonsSequence { public static boolean belowThreshold(float health,float max){return health<max*.3F;} }
''',
    'net.kazi.kazimod.abilities.GoruRework.GoruCutsceneMusic': '''
public class GoruCutsceneMusic { public static void start(net.minecraft.entity.LivingEntity user){} }
''',
}

for name, method in [
    ('net.kazi.kazimod.abilities.GoruRework.SeriousnessAbility', 'canPlayDraw'),
    ('net.kazi.kazimod.abilities.YagiAwakening.YagiAwakeningSupport', 'canUnlock'),
    ('net.kazi.kazimod.abilities.YagiAwakening.HyperGonerCutscene', 'canPlay'),
]:
    fixtures[name] = 'public class '+name.rsplit('.', 1)[1]+' { public static boolean '+method+'(net.minecraft.entity.LivingEntity user){return user.eligible;} }'

for prefix, names in [
    ('net.minecraftforge.event.entity.living.', ['LivingAttackEvent', 'LivingDamageEvent', 'LivingHurtEvent']),
    ('xyz.pixelatedw.mineminenomi.api.events.', ['WyLivingAttackEvent', 'WyLivingDamageEvent', 'WyLivingHurtEvent']),
]:
    for name in names:
        fixtures[prefix+name] = '''
public class %s {
 private final net.minecraft.entity.LivingEntity entity;public boolean canceled;
 public %s(net.minecraft.entity.LivingEntity e){entity=e;}
 public net.minecraft.entity.LivingEntity getEntityLiving(){return entity;}public void setCanceled(boolean v){canceled=v;}
}
''' % (name, name)

sources = []
for name, code in fixtures.items():
    path = OUT / 'fixtures' / (name.replace('.', '/')+'.java')
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text('package '+name.rsplit('.', 1)[0]+';\n'+code, encoding='utf-8')
    sources.append(str(path))
sources += [
    str(ROOT/'src/main/java/net/kazi/kazimod/abilities/NagiAwakening/InnerDragonsCutscenes.java'),
    str(ROOT/'src/main/java/net/kazi/kazimod/abilities/cinematics/AwakeningCutsceneType.java'),
    str(Path(__file__).with_name('ChaosFinaleProtectionTest.java')),
]
classes = OUT/'classes'
classes.mkdir(parents=True, exist_ok=True)
subprocess.run([str(JAVA/'javac.exe'), '-encoding', 'UTF-8', '-d', str(classes), *sources], check=True)
subprocess.run([str(JAVA/'java.exe'), '-cp', str(classes), 'ChaosFinaleProtectionTest'], check=True)
