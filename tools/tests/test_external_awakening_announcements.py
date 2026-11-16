"""Run the actual awakening tracker and broadcast method against player/event fixtures."""
from pathlib import Path
import subprocess

ROOT = Path(__file__).resolve().parents[2]
OUT = ROOT / 'build/external-awakening-announcement-tests'
JAVA = Path('C:/Users/th3wyz/.jdks/corretto-1.8.0_482/bin')
fixtures = {
    'net.minecraft.util.ResourceLocation': '''public final class ResourceLocation {
 private final String value;public ResourceLocation(String namespace,String path){value=namespace+":"+path;}
 public boolean equals(Object other){return other instanceof ResourceLocation&&value.equals(((ResourceLocation)other).value);}
 public int hashCode(){return value.hashCode();}public String toString(){return value;}
}''',
    'net.minecraft.util.text.StringTextComponent': '''public class StringTextComponent {
 private final String text;public StringTextComponent(String text){this.text=text;}public String getString(){return text;}
}''',
    'net.minecraft.entity.player.PlayerEntity': '''public class PlayerEntity {
 public xyz.pixelatedw.mineminenomi.data.entity.devilfruit.IDevilFruit fruit;
}''',
    'net.minecraft.entity.player.ServerPlayerEntity': '''public class ServerPlayerEntity extends PlayerEntity {
 public int messages;public java.util.UUID getUUID(){return new java.util.UUID(0,1);}
 public net.minecraft.util.text.StringTextComponent getName(){return new net.minecraft.util.text.StringTextComponent("Tester");}
 public void sendMessage(net.minecraft.util.text.StringTextComponent text,java.util.UUID sender){messages++;}
}''',
    'net.minecraft.server.MinecraftServer': '''public class MinecraftServer {
 public final PlayerList list=new PlayerList();public PlayerList getPlayerList(){return list;}
 public static class PlayerList {
 public final java.util.List<net.minecraft.entity.player.ServerPlayerEntity> players=new java.util.ArrayList<>();
 public java.util.List<net.minecraft.entity.player.ServerPlayerEntity> getPlayers(){return players;}}
}''',
    'net.minecraftforge.fml.server.ServerLifecycleHooks': '''public class ServerLifecycleHooks {
 public static net.minecraft.server.MinecraftServer server=new net.minecraft.server.MinecraftServer();
 public static net.minecraft.server.MinecraftServer getCurrentServer(){return server;}
}''',
    'net.minecraftforge.event.TickEvent': '''public class TickEvent {
 public enum Phase{START,END}public static class PlayerTickEvent {
 public final net.minecraft.entity.player.PlayerEntity player;public final Phase phase;
 public PlayerTickEvent(net.minecraft.entity.player.PlayerEntity p,Phase phase){player=p;this.phase=phase;}}
}''',
    'net.minecraftforge.event.entity.player.PlayerEvent': '''public class PlayerEvent {
 private final net.minecraft.entity.player.PlayerEntity player;
 public PlayerEvent(net.minecraft.entity.player.PlayerEntity p){player=p;}
 public net.minecraft.entity.player.PlayerEntity getPlayer(){return player;}
 public static class PlayerLoggedInEvent extends PlayerEvent{public PlayerLoggedInEvent(net.minecraft.entity.player.PlayerEntity p){super(p);}}
 public static class PlayerLoggedOutEvent extends PlayerEvent{public PlayerLoggedOutEvent(net.minecraft.entity.player.PlayerEntity p){super(p);}}
 public static class PlayerRespawnEvent extends PlayerEvent{public PlayerRespawnEvent(net.minecraft.entity.player.PlayerEntity p){super(p);}}
 public static class PlayerChangedDimensionEvent extends PlayerEvent{public PlayerChangedDimensionEvent(net.minecraft.entity.player.PlayerEntity p){super(p);}}
 public static class Clone extends PlayerEvent{
 private final net.minecraft.entity.player.PlayerEntity original;
 public Clone(net.minecraft.entity.player.PlayerEntity p,net.minecraft.entity.player.PlayerEntity old){super(p);original=old;}
 public net.minecraft.entity.player.PlayerEntity getOriginal(){return original;}}
}''',
    'net.minecraftforge.eventbus.api.EventPriority': 'public enum EventPriority { NORMAL,LOWEST }',
    'net.minecraftforge.eventbus.api.SubscribeEvent': 'public @interface SubscribeEvent { EventPriority priority() default EventPriority.NORMAL; }',
    'net.minecraftforge.fml.common.Mod': '''public class Mod {public @interface EventBusSubscriber {
 String modid();Bus bus();enum Bus{FORGE,MOD}}}''',
    'xyz.pixelatedw.mineminenomi.data.entity.devilfruit.IDevilFruit': '''public interface IDevilFruit {
 java.util.Optional<net.minecraft.util.ResourceLocation> getDevilFruit();boolean hasAwakenedFruit();
}''',
    'xyz.pixelatedw.mineminenomi.data.entity.devilfruit.DevilFruitCapability': '''public class DevilFruitCapability {
 public static IDevilFruit get(net.minecraft.entity.player.PlayerEntity p){return p.fruit;}
}''',
}

# Compile the real helper as well, so deduplication covers the production call site.
handler = (ROOT / 'src/main/java/net/kazi/kazimod/events/AwakeningEssenceDeathHandler.java').read_text(encoding='utf-8')
start = handler.index('    static void broadcastAwakening(')
end = handler.index('{', start) + 1
depth = 1
while depth:
    depth += (handler[end] == '{') - (handler[end] == '}')
    end += 1
broadcast = handler[start:end]
fixtures['net.kazi.kazimod.events.AwakeningEssenceDeathHandler'] = '''
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.fml.server.ServerLifecycleHooks;
import xyz.pixelatedw.mineminenomi.data.entity.devilfruit.IDevilFruit;
public class AwakeningEssenceDeathHandler {
''' + broadcast + '''
 private static String getAwakeningAnnouncement(String fruit,String player){return player;}
}
'''
sources = []
for name, body in fixtures.items():
    path = OUT / 'fixtures' / (name.replace('.', '/') + '.java')
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text('package ' + name.rsplit('.', 1)[0] + ';\n' + body, encoding='utf-8')
    sources.append(str(path))
sources.extend([
    str(ROOT / 'src/main/java/net/kazi/kazimod/events/ExternalAwakeningAnnouncementHandler.java'),
    str(Path(__file__).with_name('ExternalAwakeningAnnouncementTest.java')),
])
classes = OUT / 'classes'
classes.mkdir(parents=True, exist_ok=True)
subprocess.run([str(JAVA / 'javac.exe'), '-encoding', 'UTF-8', '-d', str(classes), *sources], check=True)
subprocess.run([str(JAVA / 'java.exe'), '-cp', str(classes), 'net.kazi.kazimod.events.ExternalAwakeningAnnouncementTest'], check=True)
