package net.kazi.kazimod.petal;
import net.minecraft.entity.LivingEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.ResourceLocation;
import xyz.pixelatedw.mineminenomi.data.entity.devilfruit.DevilFruitCapability;
public final class PetalState {
 public static final String TAG="KaziPetal";
 public static final ResourceLocation FRUIT=new ResourceLocation("kazimod","petal_petal_no_mi");
 public static CompoundNBT get(LivingEntity p){if(!p.getPersistentData().contains(TAG,10))p.getPersistentData().put(TAG,new CompoundNBT());return p.getPersistentData().getCompound(TAG);}
 public static boolean fruit(LivingEntity p){return DevilFruitCapability.get(p).getDevilFruit().filter(FRUIT::equals).isPresent();}
 public static boolean awakened(LivingEntity p){return fruit(p)&&DevilFruitCapability.get(p).hasAwakenedFruit();}
 public static boolean scattered(LivingEntity p){return p.level.isClientSide?get(p).getBoolean("Fruit"):net.kazi.kazimod.petal.PetalFeatures.usable(p);}
 public static boolean bankai(LivingEntity p){return get(p).getBoolean("Bankai");}
 public static int action(LivingEntity p){return get(p).getLong("End")>p.level.getGameTime()?get(p).getInt("Action"):-1;}
 public static void pose(LivingEntity p,int action,int ticks){get(p).putInt("Action",action);get(p).putLong("Start",p.level.getGameTime());get(p).putLong("End",p.level.getGameTime()+ticks);}
 public static void reset(LivingEntity p){get(p).putBoolean("Scatter",false);get(p).putBoolean("Bankai",false);get(p).putLong("End",0);get(p).putLong("Senkei",0);}
}
