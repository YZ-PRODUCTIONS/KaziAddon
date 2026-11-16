"""Exercise the production finale lift against small recording physics/lifecycle fixtures."""
from pathlib import Path
import subprocess

ROOT = Path(__file__).resolve().parents[2]
OUT = ROOT / 'output/validation/chaos-beam-finale-lift-tests'
JAVA = Path('C:/Users/th3wyz/.jdks/corretto-1.8.0_482/bin')
fixtures = {
    'net.minecraft.util.math.vector.Vector3d': '''
public final class Vector3d {
 public static final Vector3d ZERO = new Vector3d(0,0,0);
 public final double x,y,z;
 public Vector3d(double x,double y,double z){this.x=x;this.y=y;this.z=z;}
}''',
    'net.minecraft.world.World': '''
public class World { public double ceilingY = Double.POSITIVE_INFINITY; }
''',
    'net.minecraft.entity.MoverType': 'public enum MoverType { SELF }',
    'net.minecraft.nbt.CompoundNBT': '''
public class CompoundNBT {
 private final java.util.Map<String,Boolean> values = new java.util.HashMap<>();
 public boolean getBoolean(String key){return Boolean.TRUE.equals(values.get(key));}
 public void putBoolean(String key,boolean value){values.put(key,value);}
 public void remove(String key){values.remove(key);}
 public boolean isEmpty(){return values.isEmpty();}
}''',
    'net.minecraft.potion.Effects': 'public final class Effects { public static final Object SLOW_FALLING=new Object(); }',
    'net.minecraft.potion.EffectInstance': '''
public final class EffectInstance {
 public final Object effect;public final int ticks,amplifier;
 public EffectInstance(Object effect,int ticks,int amplifier,boolean ambient,boolean visible,boolean icon){
  this.effect=effect;this.ticks=ticks;this.amplifier=amplifier;
 }
}''',
    'net.minecraft.entity.LivingEntity': '''
import net.minecraft.world.World;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.potion.EffectInstance;
public class LivingEntity {
 public World level;public boolean removed,alive=true,gravity,passenger,jumping,sprinting;
 public float xxa,yya,zza,fallDistance;public int teleports,moves;
 public double x=2,y=64,z=3;public Vector3d motion=Vector3d.ZERO;
 public final java.util.List<EffectInstance> effects=new java.util.ArrayList<>();
 private final CompoundNBT data=new CompoundNBT();
 public LivingEntity(World world){level=world;}
 public Vector3d position(){return new Vector3d(x,y,z);}
 public double getX(){return x;}public double getY(){return y;}public double getZ(){return z;}
 public boolean isPassenger(){return passenger;}public void stopRiding(){passenger=false;}
 public void setJumping(boolean value){jumping=value;}public void setSprinting(boolean value){sprinting=value;}
 public void setDeltaMovement(Vector3d value){motion=value;}
 public boolean isNoGravity(){return gravity;}public void setNoGravity(boolean value){gravity=value;}
 public boolean isAlive(){return alive;}public CompoundNBT getPersistentData(){return data;}
 public void move(MoverType kind,Vector3d delta){
  if(kind!=MoverType.SELF)throw new AssertionError("Movement must retain normal collision handling");
  x+=delta.x;y=Math.min(level.ceilingY,y+delta.y);z+=delta.z;moves++;
 }
 public void addEffect(EffectInstance effect){effects.add(effect);}
 public void teleportTo(double x,double y,double z){this.x=x;this.y=y;this.z=z;teleports++;}
}''',
    'net.minecraft.entity.player.ServerPlayerEntity': '''
public class ServerPlayerEntity extends net.minecraft.entity.LivingEntity {
 public ServerPlayerEntity(net.minecraft.world.World world){super(world);}
}''',
}

sources=[]
for name,code in fixtures.items():
    path=OUT/'fixtures'/(name.replace('.','/')+'.java')
    path.parent.mkdir(parents=True,exist_ok=True)
    path.write_text('package '+name.rsplit('.',1)[0]+';\n'+code,encoding='utf-8')
    sources.append(str(path))
sources += [str(ROOT/'src/main/java/net/kazi/kazimod/abilities/cinematics/ChaosBeamFinaleLift.java'),
            str(Path(__file__).with_name('ChaosBeamFinaleLiftTest.java'))]
classes=OUT/'classes'
classes.mkdir(parents=True,exist_ok=True)
subprocess.run([str(JAVA/'javac.exe'),'-encoding','UTF-8','-d',str(classes),*sources],check=True)
subprocess.run([str(JAVA/'java.exe'),'-cp',str(classes),'ChaosBeamFinaleLiftTest'],check=True)
