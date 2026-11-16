"""Run the actual client sound controller against a recording audio engine, then check assets."""
from pathlib import Path
import hashlib
import json
import subprocess

ROOT=Path(__file__).resolve().parents[2]
WORK=ROOT/'build/chaos-beam-sound-tests'
JAVA=Path('C:/Users/th3wyz/.jdks/corretto-1.8.0_482/bin')
fixtures={
'net.kazi.kazimod.entities.asriel.ChaosBeamAttackEntity': '''
public class ChaosBeamAttackEntity {
 public static class World {public boolean isClientSide=true;}
 public final World level;public boolean removed,firing,initialized=true;public float age;public double x,y,z;
 public ChaosBeamAttackEntity(World level){this.level=level;}
 public boolean isInitialized(){return initialized;}public boolean isFiring(){return firing;}public float getVisualAge(float p){return age;}
 public double getX(){return x;}public double getY(){return y;}public double getZ(){return z;}
}''',
'net.minecraft.client.Minecraft': '''
public class Minecraft {
 private static final Minecraft INSTANCE=new Minecraft();public static Minecraft getInstance(){return INSTANCE;}
 public net.kazi.kazimod.entities.asriel.ChaosBeamAttackEntity.World level=new net.kazi.kazimod.entities.asriel.ChaosBeamAttackEntity.World();
 public final net.minecraft.client.audio.SoundHandler sound=new net.minecraft.client.audio.SoundHandler();
 public net.minecraft.client.audio.SoundHandler getSoundManager(){return sound;}
}''',
'net.minecraft.client.audio.TickableSound': '''
public abstract class TickableSound {
 public enum AttenuationType {LINEAR}public final String name;protected boolean looping,relative;protected int delay;
 protected float volume,pitch;protected double x,y,z;protected AttenuationType attenuation;private boolean stopped;
 protected TickableSound(net.minecraft.util.SoundEvent e,net.minecraft.util.SoundCategory c){name=e.name;}
 protected void stop(){stopped=true;}public boolean isStopped(){return stopped;}public boolean isLooping(){return looping;}
 public float getVolume(){return volume;}public float getPitch(){return pitch;}public double getX(){return x;}
 public boolean canStartSilent(){return false;}public abstract void tick();
}''',
'net.minecraft.client.audio.SoundHandler': '''
public class SoundHandler {
 public final java.util.List<TickableSound> played=new java.util.ArrayList<>(),active=new java.util.ArrayList<>();
 public void play(TickableSound s){played.add(s);active.add(s);}public void stop(TickableSound s){active.remove(s);}
 public void tick(){for(TickableSound s:new java.util.ArrayList<>(active)){s.tick();if(s.isStopped())active.remove(s);}}
}''',
'net.minecraft.util.SoundEvent':'public class SoundEvent {public final String name;public SoundEvent(String name){this.name=name;}}',
'net.minecraft.util.SoundCategory':'public enum SoundCategory {PLAYERS}',
'net.kazi.kazimod.init.KaziSounds': '''
public class KaziSounds {
 public static class Entry {private final String name;Entry(String n){name=n;}public net.minecraft.util.SoundEvent get(){return new net.minecraft.util.SoundEvent(name);}}
 public static final Entry CHAOS_BEAM_CHARGE_SFX=new Entry("charge"),CHAOS_BEAM_LAUNCH_SFX=new Entry("launch"),CHAOS_BEAM_HOLD_SFX=new Entry("hold");
}''',
'net.minecraftforge.api.distmarker.Dist':'public enum Dist {CLIENT}',
'net.minecraftforge.eventbus.api.SubscribeEvent':'public @interface SubscribeEvent {}',
'net.minecraftforge.fml.common.Mod':'public @interface Mod {public @interface EventBusSubscriber {String modid();net.minecraftforge.api.distmarker.Dist value();}}',
'net.minecraftforge.event.TickEvent':'public class TickEvent {public enum Phase {START,END}public static class ClientTickEvent {public Phase phase=Phase.END;}}',
'net.minecraftforge.client.event.ClientPlayerNetworkEvent':'public class ClientPlayerNetworkEvent {public static class LoggedOutEvent {}}',
'net.minecraftforge.event.entity.EntityJoinWorldEvent': '''
public class EntityJoinWorldEvent {
 private final net.kazi.kazimod.entities.asriel.ChaosBeamAttackEntity e;
 public EntityJoinWorldEvent(net.kazi.kazimod.entities.asriel.ChaosBeamAttackEntity e){this.e=e;}
 public Object getEntity(){return e;}public net.kazi.kazimod.entities.asriel.ChaosBeamAttackEntity.World getWorld(){return e.level;}
}''',
}
sources=[]
for name,code in fixtures.items():
    path=WORK/'fixtures'/(name.replace('.','/')+'.java');path.parent.mkdir(parents=True,exist_ok=True)
    path.write_text('package '+name.rsplit('.',1)[0]+';\n'+code,encoding='utf-8');sources.append(str(path))
sources += [str(ROOT/'src/main/java/net/kazi/kazimod'/p) for p in
            ['abilities/YagiAwakening/ChaosBeamPattern.java','client/asriel/ChaosBeamAudioTiming.java','client/asriel/ChaosBeamSoundClient.java']]
sources.append(str(Path(__file__).with_name('ChaosBeamSoundTest.java')))
classes=WORK/'classes';classes.mkdir(parents=True,exist_ok=True)
subprocess.run([str(JAVA/'javac.exe'),'-encoding','UTF-8','-d',str(classes),*sources],check=True)
subprocess.run([str(JAVA/'java.exe'),'-cp',str(classes),'ChaosBeamSoundTest'],check=True)

assets=ROOT/'src/main/resources/assets/kazimod'
sounds=json.loads((assets/'sounds.json').read_text(encoding='utf-8'))
lang=json.loads((assets/'lang/en_us.json').read_text(encoding='utf-8'))
records=json.loads((assets/'sounds/asriel/sources.json').read_text(encoding='utf-8'))['assets']
for cue in ['charge','launch','hold']:
    name='chaos_beam_'+cue;entry=sounds[name];assert entry['subtitle'] in lang
    path=assets/'sounds/asriel'/(name+'.ogg');assert path.read_bytes().startswith(b'OggS')
    assert entry['sounds'][0]['name']=='kazimod:asriel/'+name
    provenance=next(r for r in records if r['file']==name+'.ogg')
    assert hashlib.sha256(path.read_bytes()).hexdigest()==provenance['output_sha256']
    assert provenance['source_correlation']>.99
print('Chaos Beam audio assets: all three registered recordings and subtitles verified.')
