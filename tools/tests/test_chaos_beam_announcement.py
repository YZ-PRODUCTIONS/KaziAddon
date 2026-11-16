"""Compile the real announcement/packet against recording network and audience fixtures."""
from pathlib import Path
import subprocess

ROOT = Path(__file__).resolve().parents[2]
OUT = ROOT / 'output/validation/chaos-beam-announcement-tests'
JAVA = Path('C:/Users/th3wyz/.jdks/corretto-1.8.0_482/bin')
fixtures = {
    'net.minecraft.util.ResourceLocation': '''public final class ResourceLocation {
 public final String id;public ResourceLocation(String id){this.id=id;}
 public boolean equals(Object o){return o instanceof ResourceLocation&&id.equals(((ResourceLocation)o).id);}public int hashCode(){return id.hashCode();}
}''',
    'net.minecraft.world.World': 'public class World {}',
    'net.minecraft.world.server.ServerWorld': '''public class ServerWorld extends net.minecraft.world.World {
 public long time;public final java.util.List<net.minecraft.entity.player.ServerPlayerEntity> players=new java.util.ArrayList<>();
 private final net.minecraft.util.ResourceLocation dimension;public ServerWorld(String id){dimension=new net.minecraft.util.ResourceLocation(id);}
 public java.util.List<net.minecraft.entity.player.ServerPlayerEntity> players(){return players;}
 public long getGameTime(){return time;}public Key dimension(){return new Key(dimension);}
 public static final class Key {private final net.minecraft.util.ResourceLocation id;Key(net.minecraft.util.ResourceLocation id){this.id=id;}public net.minecraft.util.ResourceLocation location(){return id;}}
}''',
    'net.minecraft.entity.LivingEntity': '''public class LivingEntity {
 public net.minecraft.world.World level;public boolean alive=true,removed;public double x,y,z;
 public LivingEntity(net.minecraft.world.World world,double x,double y,double z){level=world;this.x=x;this.y=y;this.z=z;}
 public boolean isAlive(){return alive;}public double distanceToSqr(LivingEntity other){double dx=x-other.x,dy=y-other.y,dz=z-other.z;return dx*dx+dy*dy+dz*dz;}
}''',
    'net.minecraft.entity.player.ServerPlayerEntity': '''public final class ServerPlayerEntity extends net.minecraft.entity.LivingEntity {
 public ServerPlayerEntity(net.minecraft.world.World world,double x,double y,double z){super(world,x,y,z);}
}''',
    'net.minecraftforge.fml.network.PacketDistributor': '''public final class PacketDistributor {
 public static final PacketDistributor PLAYER=new PacketDistributor();
 public net.minecraft.entity.player.ServerPlayerEntity with(java.util.function.Supplier<net.minecraft.entity.player.ServerPlayerEntity> supplier){return supplier.get();}
}''',
    'net.kazi.kazimod.init.KaziPacketHandler': '''public final class KaziPacketHandler {
 public static final Channel CHANNEL=new Channel();
 public static final class Send {public final net.minecraft.entity.player.ServerPlayerEntity player;public final net.kazi.kazimod.network.ChaosBeamTitlePacket packet;
 Send(net.minecraft.entity.player.ServerPlayerEntity player,net.kazi.kazimod.network.ChaosBeamTitlePacket packet){this.player=player;this.packet=packet;}}
 public static final class Channel {public final java.util.List<Send> sends=new java.util.ArrayList<>();
 public void send(net.minecraft.entity.player.ServerPlayerEntity player,net.kazi.kazimod.network.ChaosBeamTitlePacket packet){sends.add(new Send(player,packet));}
 public int count(net.minecraft.entity.player.ServerPlayerEntity player){int n=0;for(Send s:sends)if(s.player==player)n++;return n;}}
}''',
    'net.minecraft.network.PacketBuffer': '''public final class PacketBuffer {
 private final java.util.List<Object> data=new java.util.ArrayList<>();private int index;
 public void writeUUID(java.util.UUID value){data.add(value);}public java.util.UUID readUUID(){return (java.util.UUID)data.get(index++);}
 public void writeResourceLocation(net.minecraft.util.ResourceLocation value){data.add(value);}public net.minecraft.util.ResourceLocation readResourceLocation(){return (net.minecraft.util.ResourceLocation)data.get(index++);}
 public void writeLong(long value){data.add(value);}public long readLong(){return (Long)data.get(index++);}
 public void writeUtf(String value,int max){if(value.length()>max)throw new IllegalArgumentException();data.add(value);}
 public String readUtf(int max){String value=(String)data.get(index++);if(value.length()>max)throw new IllegalArgumentException();return value;}
}''',
    'net.minecraftforge.fml.network.NetworkDirection': 'public enum NetworkDirection { PLAY_TO_CLIENT,PLAY_TO_SERVER }',
    'net.minecraftforge.fml.network.NetworkEvent': '''public final class NetworkEvent {
 public static final class Context {public final NetworkDirection direction;public boolean handled;public int queued;
 public Context(NetworkDirection direction){this.direction=direction;}public NetworkDirection getDirection(){return direction;}
 public void enqueueWork(Runnable work){queued++;work.run();}public void setPacketHandled(boolean value){handled=value;}}
}''',
    'net.minecraftforge.api.distmarker.Dist': 'public enum Dist { CLIENT, DEDICATED_SERVER }',
    'net.minecraftforge.api.distmarker.OnlyIn': 'public @interface OnlyIn { Dist value(); }',
    'net.kazi.kazimod.client.asriel.ChaosBeamTitleOverlay': '''public final class ChaosBeamTitleOverlay {
 public static net.kazi.kazimod.network.ChaosBeamTitlePacket last;
 public static void receive(net.kazi.kazimod.network.ChaosBeamTitlePacket packet){last=packet;}
}''',
}
sources = []
for name, code in fixtures.items():
    path = OUT / 'fixtures' / (name.replace('.', '/') + '.java')
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text('package ' + name.rsplit('.', 1)[0] + ';\n' + code, encoding='utf-8')
    sources.append(str(path))
sources += [str(ROOT / 'src/main/java/net/kazi/kazimod' / file) for file in (
    'network/ChaosBeamTitlePacket.java', 'abilities/YagiAwakening/ChaosBeamAnnouncement.java')]
sources.append(str(Path(__file__).with_name('ChaosBeamAnnouncementTest.java')))
classes = OUT / 'classes'
classes.mkdir(parents=True, exist_ok=True)
subprocess.run([str(JAVA / 'javac.exe'), '-encoding', 'UTF-8', '-d', str(classes), *sources], check=True)
subprocess.run([str(JAVA / 'java.exe'), '-cp', str(classes), 'ChaosBeamAnnouncementTest'], check=True)
