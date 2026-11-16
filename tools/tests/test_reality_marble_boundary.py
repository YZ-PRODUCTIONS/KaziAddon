"""Exercise the production boundary hooks, including player construction at login."""
from pathlib import Path
import re
import subprocess

ROOT = Path(__file__).resolve().parents[2]
OUT = ROOT / 'build/reality-marble-boundary-tests'
JAVA = Path('C:/Users/th3wyz/.jdks/corretto-1.8.0_482/bin')
SOURCE = ROOT / 'src/main/java/net/kazi/kazimod/abilities/SupaRework'
fixtures = {}


def put(name, code):
    fixtures[name] = 'package ' + name.rsplit('.', 1)[0] + ';\n' + code


put('net.minecraft.util.math.vector.Vector3d', '''public class Vector3d {
 public final double x,y,z; public Vector3d(double x,double y,double z){this.x=x;this.y=y;this.z=z;}
 public Vector3d add(double x,double y,double z){return new Vector3d(this.x+x,this.y+y,this.z+z);}
 public Vector3d scale(double n){return new Vector3d(x*n,y*n,z*n);}
 public double lengthSqr(){return x*x+y*y+z*z;}
 public double distanceToSqr(Vector3d v){double a=x-v.x,b=y-v.y,c=z-v.z;return a*a+b*b+c*c;}
}''')
put('net.minecraft.util.math.AxisAlignedBB', '''public class AxisAlignedBB {
 private final net.minecraft.util.math.vector.Vector3d center;
 public AxisAlignedBB(double x,double y,double z){center=new net.minecraft.util.math.vector.Vector3d(x,y,z);}
 public net.minecraft.util.math.vector.Vector3d getCenter(){return center;}
 public double getXsize(){return 1;}public double getYsize(){return 1;}public double getZsize(){return 1;}
}''')
put('net.minecraft.util.ResourceLocation', '''public class ResourceLocation {
 private final String namespace,path;public ResourceLocation(String namespace,String path){this.namespace=namespace;this.path=path;}
 public String getNamespace(){return namespace;}public String getPath(){return path;}
}''')
put('net.minecraft.entity.Entity', '''public class Entity {
 public Object level;public int tickCount;public boolean noPhysics,alive=true;public double x,y,z;
 // Minecraft's Entity constructor calls setPos before ServerPlayerEntity has its game mode.
 public Entity(Object level){this.level=level;setPos(0,0,0);}
 public void setPos(double x,double y,double z){
  if(net.kazi.kazimod.abilities.SupaRework.RealityMarbleBoundary.blocksTeleport(this,x,y,z))return;
  this.x=x;this.y=y;this.z=z;
 }
 public boolean isSpectator(){return false;}public boolean isAlive(){return alive;}
 public double getX(){return x;}public double getY(){return y;}public double getZ(){return z;}
 public net.minecraft.util.math.vector.Vector3d position(){return new net.minecraft.util.math.vector.Vector3d(x,y,z);}
 public net.minecraft.util.math.AxisAlignedBB getBoundingBox(){return new net.minecraft.util.math.AxisAlignedBB(x,y,z);}
}''')
put('net.minecraft.entity.LivingEntity', 'public class LivingEntity extends Entity {public LivingEntity(Object world){super(world);}}')
put('net.minecraft.entity.player.ServerPlayerEntity', '''public class ServerPlayerEntity extends net.minecraft.entity.LivingEntity {
 public Boolean spectator;public int spectatorCalls;
 public ServerPlayerEntity(Object world){super(world);spectator=Boolean.FALSE;}
 public boolean isSpectator(){spectatorCalls++;return spectator.booleanValue();}
}''')
put('xyz.pixelatedw.mineminenomi.entities.SphereEntity', '''public class SphereEntity extends net.minecraft.entity.Entity {
 public double radius=10;public net.minecraft.util.ResourceLocation[] textures;
 public SphereEntity(Object world){super(world);}
 public net.minecraft.util.ResourceLocation[] getTexture(){return textures;}public double getRadius(){return radius;}
}''')
put('net.minecraftforge.event.entity.EntityJoinWorldEvent', '''public class EntityJoinWorldEvent {
 private final net.minecraft.entity.Entity entity;public EntityJoinWorldEvent(net.minecraft.entity.Entity entity){this.entity=entity;}
 public net.minecraft.entity.Entity getEntity(){return entity;}
}''')
put('net.minecraftforge.event.world.WorldEvent', '''public class WorldEvent {
 public static class Unload {private final Object world;public Unload(Object world){this.world=world;}public Object getWorld(){return world;}}
}''')
put('net.minecraftforge.eventbus.api.SubscribeEvent', 'public @interface SubscribeEvent {}')
put('net.minecraftforge.fml.common.Mod', 'public @interface Mod {public @interface EventBusSubscriber {String modid();}}')

paths = []
for name, code in fixtures.items():
    path = OUT / 'fixtures' / (name.replace('.', '/') + '.java')
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(code, encoding='utf-8')
    paths.append(str(path))
paths.extend([str(SOURCE / 'MarbleBoundaryMath.java'),
              str(Path(__file__).with_name('RealityMarbleBoundaryTest.java'))])


def compile_and_run(boundary, directory, capture=False):
    directory.mkdir(parents=True, exist_ok=True)
    subprocess.run([str(JAVA / 'javac.exe'), '-encoding', 'UTF-8', '-d', str(directory),
                    *paths, str(boundary)], check=True)
    return subprocess.run([str(JAVA / 'java.exe'), '-cp', str(directory),
                           'RealityMarbleBoundaryTest'], capture_output=capture, text=True,
                          check=not capture)


# Demonstrate that the exact old evaluation order fails the construction test.
# The altered source stays in build/; production files are never changed here.
boundary = SOURCE / 'RealityMarbleBoundary.java'
code = boundary.read_text(encoding='utf-8')
old_order, replacements = re.subn(
    r'(if \(!\(mover instanceof net\.minecraft\.entity\.LivingEntity\) \|\| )'
    r'mover\.tickCount <= 0\s*\|\| mover\.isSpectator\(\)\) return false;',
    r'\1mover.isSpectator() || mover.tickCount <= 0) return false;', code)
assert replacements == 1, 'Expected to replace exactly the blocksTeleport constructor guard'
baseline = OUT / 'baseline-source/RealityMarbleBoundary.java'
baseline.parent.mkdir(parents=True, exist_ok=True)
baseline.write_text(old_order, encoding='utf-8')
result = compile_and_run(baseline, OUT / 'baseline-classes', capture=True)
assert result.returncode != 0, 'The original constructor guard unexpectedly succeeded'
for frame in ('NullPointerException',
              'net.minecraft.entity.player.ServerPlayerEntity.isSpectator(',
              'RealityMarbleBoundary.blocksTeleport(',
              'net.minecraft.entity.Entity.setPos(',
              'net.minecraft.entity.Entity.<init>('):
    assert frame in result.stderr, 'Missing login-crash frame: ' + frame + '\n' + result.stderr
print('Confirmed the original guard order reproduces the player login crash.', flush=True)
compile_and_run(boundary, OUT / 'classes')
