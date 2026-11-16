package net.kazi.kazimod.petal;
import java.util.*;
import net.minecraft.entity.*;
import net.minecraft.entity.player.*;
import net.minecraft.entity.projectile.AbstractArrowEntity;
import net.minecraft.item.*;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.*;
import net.minecraft.util.math.*;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.*;
import net.minecraftforge.fml.network.*;
import net.minecraftforge.fml.network.simple.SimpleChannel;
import net.minecraftforge.registries.*;
import xyz.pixelatedw.mineminenomi.api.abilities.*;
import xyz.pixelatedw.mineminenomi.api.enums.*;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.data.entity.ability.*;
import xyz.pixelatedw.mineminenomi.data.entity.devilfruit.DevilFruitCapability;
import xyz.pixelatedw.mineminenomi.items.AkumaNoMiItem;
import xyz.pixelatedw.mineminenomi.entities.projectiles.AbilityProjectileEntity;
import xyz.pixelatedw.mineminenomi.packets.server.*;
import xyz.pixelatedw.mineminenomi.wypi.WyNetwork;
public final class PetalFeatures {
 private static final DeferredRegister<EntityType<?>> TYPES=DeferredRegister.create(ForgeRegistries.ENTITIES,"kazimod");
 public static final RegistryObject<EntityType<PetalField>> FIELD=TYPES.register("petal_field",()->EntityType.Builder.<PetalField>of(PetalField::new,EntityClassification.MISC).sized(1,1).clientTrackingRange(12).updateInterval(2).noSave().build("petal_field"));
 private static final SimpleChannel CHANNEL=NetworkRegistry.newSimpleChannel(new ResourceLocation("kazimod","petal"),()->"1","1"::equals,"1"::equals);
 public static void init(IEventBus bus){
  TYPES.register(bus);for(AbilityCore<?> c:PetalAbility.CORES)net.kazi.kazimod.api.KaziRegistry.registerAbility(c);
  net.kazi.kazimod.api.KaziRegistry.registerItem("petal_petal_no_mi",()->new AkumaNoMiItem("Hanabira Hanabira no Mi",3,FruitType.PARAMECIA,PetalAbility.available()));
  AbilityCommandGroup.create("PETAL",PetalAbility::available);
  CHANNEL.registerMessage(0,PetalPacket.class,PetalPacket::encode,PetalPacket::decode,PetalPacket::handle,Optional.of(NetworkDirection.PLAY_TO_CLIENT));
  MinecraftForge.EVENT_BUS.addListener(PetalFeatures::tick);
  MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGH,PetalFeatures::hurt);
  MinecraftForge.EVENT_BUS.addListener(PetalFeatures::projectile);
  MinecraftForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent e)->{PetalState.reset(e.getPlayer());reconcile(e.getPlayer());sync(e.getPlayer());});
  MinecraftForge.EVENT_BUS.addListener((PlayerEvent.PlayerChangedDimensionEvent e)->{PetalState.reset(e.getPlayer());sync(e.getPlayer());});
  MinecraftForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent e)->PetalState.reset(e.getPlayer()));
  MinecraftForge.EVENT_BUS.addListener((LivingDeathEvent e)->{if(e.getEntityLiving().getPersistentData().contains(PetalState.TAG))PetalState.reset(e.getEntityLiving());});
  MinecraftForge.EVENT_BUS.addListener((PlayerEvent.StartTracking e)->{if(e.getPlayer() instanceof ServerPlayerEntity&&e.getTarget() instanceof PlayerEntity)CHANNEL.send(PacketDistributor.PLAYER.with(()->(ServerPlayerEntity)e.getPlayer()),packet((PlayerEntity)e.getTarget()));});
  DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->PetalClient.init(bus));
 }
 public static boolean awaken(PlayerEntity p,ItemStack stack){
  if(p.level.isClientSide||!xyz.pixelatedw.mineminenomi.config.CommonConfig.INSTANCE.hasAwakeningsEnabled()||!PetalState.fruit(p)||PetalState.awakened(p)||stack.isEmpty()||!(stack.getItem() instanceof net.kazi.kazimod.items.AwakeningEssenceItem))return false;
  DevilFruitCapability.get(p).setAwakenedFruit(true);
  if(!p.isCreative())stack.shrink(1);reconcile(p);sync(p);WyNetwork.sendTo(new SSyncDevilFruitPacket(p.getId(),DevilFruitCapability.get(p)),p);
  p.sendMessage(new StringTextComponent("Senbonzakura Kageyoshi awakened."),p.getUUID());return true;
 }
 public static void reconcile(PlayerEntity p){if(p.level.isClientSide)return;IAbilityData data=AbilityDataCapability.get(p);boolean changed=false;
  for(AbilityCore<?> c:PetalAbility.CORES){if(c.canUnlock(p)){if(!data.hasUnlockedAbility(c)){data.addUnlockedAbility(c,AbilityUnlock.PROGRESSION);changed=true;}}
   else if(data.hasUnlockedAbility(c)){IAbility a=data.getEquippedAbility(c);if(a!=null)AbilityHelper.emergencyStopAbility(p,a);data.removeEquippedAbility(c);data.removeUnlockedAbility(c);changed=true;}}
  if(changed)WyNetwork.sendTo(new SSyncAbilityDataPacket(p.getId(),data),p);
 }
 private static void tick(TickEvent.PlayerTickEvent e){PlayerEntity p=e.player;if(e.phase!=TickEvent.Phase.END||p.level.isClientSide)return;
  if(!PetalState.fruit(p)&&!p.getPersistentData().contains(PetalState.TAG))return;
  if(p.tickCount%20==0)reconcile(p);
  if(!usable(p))PetalState.reset(p);
  if(p.tickCount%4==0)sync(p);
 }
 public static boolean usable(LivingEntity p){return p.isAlive()&&!p.isSpectator()&&PetalState.fruit(p)&&!AbilityHelper.isWeakenedByKairosekiOrWater(p);}
 public static LivingEntity bodyOwner(LivingEntity t){if(t instanceof net.kazi.kazimod.seiryu.SeiryuPartEntity)return ((net.kazi.kazimod.seiryu.SeiryuPartEntity)t).owner();if(t instanceof net.kazi.kazimod.worldturtle.WorldTurtlePartEntity)return ((net.kazi.kazimod.worldturtle.WorldTurtlePartEntity)t).owner();return t;}
 public static boolean canHit(LivingEntity p,LivingEntity t){LivingEntity owner=bodyOwner(t);return owner!=null&&!allied(p,owner)&&net.kazi.kazimod.saiyan.SaiyanAbility.canHit(p,owner);}
 public static LivingEntity summonOwner(LivingEntity t){
  if(t instanceof net.kazi.kazimod.shikigami.TenShadowsBeast)return ((net.kazi.kazimod.shikigami.TenShadowsBeast)t).owner();
  if(t instanceof net.minecraft.entity.passive.TameableEntity)return ((net.minecraft.entity.passive.TameableEntity)t).getOwner();
  if(t instanceof net.kazi.kazimod.entities.ShadowDoppelmanEntity)return ((net.kazi.kazimod.entities.ShadowDoppelmanEntity)t).getOwner();
  return null;
 }
 public static boolean allied(LivingEntity p,LivingEntity t){
  if(p==t||net.kazi.kazimod.mahoraga.MahoragaFeatures.allied(p,t))return true;
  LivingEntity owner=summonOwner(t);if(owner!=null&&owner!=t)return allied(p,owner);
  // A faction label is not an alliance: unrelated pirate crews can fight each other.
  return xyz.pixelatedw.mineminenomi.init.ModEntityPredicates.getFriendlyFactions(p).test(t);
 }
 public static Vector3d aim(LivingEntity p,double range){Vector3d a=p.getEyePosition(1),b=a.add(p.getLookAngle().scale(range));return p.level.clip(new RayTraceContext(a,b,RayTraceContext.BlockMode.COLLIDER,RayTraceContext.FluidMode.NONE,p)).getLocation();}
 public static Vector3d ground(LivingEntity p,double range){Vector3d at=aim(p,range);BlockRayTraceResult r=p.level.clip(new RayTraceContext(at.add(0,1,0),at.add(0,-32,0),RayTraceContext.BlockMode.COLLIDER,RayTraceContext.FluidMode.NONE,p));return r.getType()==RayTraceResult.Type.BLOCK?r.getLocation():null;}
 public static LivingEntity target(LivingEntity p,double range){LivingEntity best=null;double distance=range*range;Vector3d start=p.getEyePosition(1),end=aim(p,range);
  for(LivingEntity t:p.level.getEntitiesOfClass(LivingEntity.class,p.getBoundingBox().inflate(range),x->canHit(p,x))){Optional<Vector3d> hit=t.getBoundingBox().inflate(.4).clip(start,end);if(hit.isPresent()&&start.distanceToSqr(hit.get())<distance){best=t;distance=start.distanceToSqr(hit.get());}}return best;}
 private static boolean front(LivingEntity p,Entity e){return e!=null&&e.position().subtract(p.position()).normalize().dot(p.getLookAngle())>.3;}
 private static void hurt(LivingHurtEvent e){LivingEntity p=e.getEntityLiving();if(p.level.isClientSide)return;
  if(usable(p)&&PetalState.action(p)==2&&front(p,e.getSource().getDirectEntity())&&!e.getSource().isBypassInvul()){
   e.setAmount(e.getAmount()*.25F);
  }
  Entity attacker=e.getSource().getEntity();if(attacker instanceof LivingEntity&&"player".equals(e.getSource().getMsgId())){LivingEntity a=(LivingEntity)attacker;
   if(usable(a)&&!PetalState.scattered(a)&&a.getMainHandItem().isEmpty()&&canHit(a,p))e.setAmount(e.getAmount()+5);
   if(usable(a)&&PetalState.bankai(a)&&PetalState.get(a).getLong("Senkei")>a.level.getGameTime()&&canHit(a,p))e.setAmount(e.getAmount()*1.3F);}
 }
 private static void projectile(ProjectileImpactEvent e){if(e.getEntity().level.isClientSide||!(e.getRayTraceResult() instanceof EntityRayTraceResult))return;Entity target=((EntityRayTraceResult)e.getRayTraceResult()).getEntity(),shot=e.getEntity();
  Entity shooter=shot instanceof net.minecraft.entity.projectile.ProjectileEntity?((net.minecraft.entity.projectile.ProjectileEntity)shot).getOwner():null;
  LivingEntity defender=target instanceof PetalField?((PetalField)target).owner():target instanceof LivingEntity?(LivingEntity)target:null;
  if(defender!=null&&shooter instanceof LivingEntity&&!canHit(defender,(LivingEntity)shooter))return;
  float amount=shot instanceof AbilityProjectileEntity?((AbilityProjectileEntity)shot).getDamage():shot instanceof AbstractArrowEntity?(float)(((AbstractArrowEntity)shot).getBaseDamage()*shot.getDeltaMovement().length()):Float.POSITIVE_INFINITY;
  if(target instanceof PetalField&&((PetalField)target).isPickable()&&Float.isFinite(amount)){target.hurt(DamageSource.GENERIC,amount);shot.remove();e.setCanceled(true);return;}
  if(!(target instanceof LivingEntity))return;LivingEntity p=(LivingEntity)target;
  if(usable(p)&&PetalState.action(p)==2&&front(p,shot)&&amount<=(PetalState.bankai(p)?45:30)){shot.remove();e.setCanceled(true);}
 }
 private static PetalPacket packet(PlayerEntity p){CompoundNBT n=PetalState.get(p).copy();n.putBoolean("Fruit",PetalState.fruit(p));return new PetalPacket(p.getId(),n);}
 public static void sync(PlayerEntity p){if(p instanceof ServerPlayerEntity)CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(()->p),packet(p));}
}
