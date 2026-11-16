package net.kazi.kazimod.decay;

import java.util.*;
import net.minecraft.entity.*;
import net.minecraft.entity.player.*;
import net.minecraft.entity.projectile.*;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.*;
import net.minecraft.util.math.EntityRayTraceResult;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.network.*;
import net.minecraftforge.fml.network.simple.SimpleChannel;
import net.minecraftforge.registries.*;
import xyz.pixelatedw.mineminenomi.api.abilities.*;
import xyz.pixelatedw.mineminenomi.api.enums.AbilityCommandGroup;
import xyz.pixelatedw.mineminenomi.api.events.ability.*;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.data.entity.ability.*;
import xyz.pixelatedw.mineminenomi.data.entity.devilfruit.DevilFruitCapability;
import xyz.pixelatedw.mineminenomi.init.ModAbilities;
import xyz.pixelatedw.mineminenomi.packets.server.*;
import xyz.pixelatedw.mineminenomi.wypi.WyNetwork;

public final class DecayFeatures {
    public static final String TAG="KaziDecay";
    private static final DeferredRegister<EntityType<?>> TYPES=DeferredRegister.create(ForgeRegistries.ENTITIES,"kazimod");
    public static final RegistryObject<EntityType<DecayField>> FIELD=TYPES.register("decay_field",()->EntityType.Builder.<DecayField>of(DecayField::new,EntityClassification.MISC).sized(.1F,.1F).clientTrackingRange(12).updateInterval(2).noSave().build("decay_field"));
    private static final SimpleChannel CHANNEL=NetworkRegistry.newSimpleChannel(new ResourceLocation("kazimod","decay"),()->"1","1"::equals,"1"::equals);
    public static void init(IEventBus bus){
        TYPES.register(bus);for(AbilityCore<?> c:DecayAbility.CORES)net.kazi.kazimod.api.KaziRegistry.registerAbility(c);
        AbilityCommandGroup.create("SABI_AWAKENING",()->DecayAbility.CORES);
        bus.addListener((FMLCommonSetupEvent e)->e.enqueueWork(()->{net.kazi.kazimod.util.FruitAbilityInjector.addAbilities(ModAbilities.SABI_SABI_NO_MI,DecayAbility.CORES);net.kazi.kazimod.events.AwakeningAbilityLoginFix.invalidateCache();}));
        CHANNEL.registerMessage(0,DecayPacket.class,DecayPacket::encode,DecayPacket::decode,DecayPacket::handle,Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        MinecraftForge.EVENT_BUS.addListener(DecayFeatures::tick);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGHEST,(AbilityUseEvent.Pre e)->{if(blocked(e.getEntityLiving(),e.getAbility().getCore())||action(e.getEntityLiving())>=0&&e.getAbility().getCore()!=DecayAbility.CORES[action(e.getEntityLiving())])e.setCanceled(true);});
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST,(UnlockAbilityEvent e)->{if(blocked(e.getEntityLiving(),e.getAbilityCore()))e.setResult(Event.Result.DENY);});
        MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGH,DecayFeatures::hurt);
        MinecraftForge.EVENT_BUS.addListener(DecayFeatures::projectile);
        MinecraftForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent e)->{clear(e.getPlayer());reconcile(e.getPlayer());});
        MinecraftForge.EVENT_BUS.addListener((PlayerEvent.PlayerRespawnEvent e)->{clear(e.getPlayer());reconcile(e.getPlayer());});
        MinecraftForge.EVENT_BUS.addListener((PlayerEvent.PlayerChangedDimensionEvent e)->clear(e.getPlayer()));
        MinecraftForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent e)->clear(e.getPlayer()));
        MinecraftForge.EVENT_BUS.addListener((LivingDeathEvent e)->{if(e.getEntityLiving() instanceof PlayerEntity)clear((PlayerEntity)e.getEntityLiving());});
        MinecraftForge.EVENT_BUS.addListener((PlayerEvent.StartTracking e)->{if(e.getTarget() instanceof PlayerEntity&&e.getPlayer() instanceof ServerPlayerEntity)CHANNEL.send(PacketDistributor.PLAYER.with(()->(ServerPlayerEntity)e.getPlayer()),new DecayPacket(e.getTarget().getId(),state((LivingEntity)e.getTarget()).copy()));});
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->DecayClient.init(bus));
    }
    public static boolean fruit(LivingEntity p){return DevilFruitCapability.get(p).hasDevilFruit(ModAbilities.SABI_SABI_NO_MI);}
    public static boolean awakened(LivingEntity p){return fruit(p)&&DevilFruitCapability.get(p).hasAwakenedFruit();}
    public static boolean usable(LivingEntity p){return awakened(p)&&p.isAlive()&&!p.isSpectator()&&!AbilityHelper.isWeakenedByKairosekiOrWater(p);}
    public static boolean ours(AbilityCore<?> c){return Arrays.asList(DecayAbility.CORES).contains(c);}
    public static boolean original(AbilityCore<?> c){return !ours(c)&&(Arrays.asList(ModAbilities.SABI_SABI_NO_MI.getAbilities()).contains(c)||c==xyz.pixelatedw.mineminenomi.abilities.sabi.RustTouchAbility.INSTANCE||c==xyz.pixelatedw.mineminenomi.abilities.sabi.RustSkinAbility.INSTANCE||c==xyz.pixelatedw.mineminenomi.abilities.sabi.RustBreakAbility.INSTANCE);}
    public static boolean blocked(LivingEntity p,AbilityCore<?> c){return awakened(p)&&original(c);}
    public static CompoundNBT state(LivingEntity p){if(!p.getPersistentData().contains(TAG,10))p.getPersistentData().put(TAG,new CompoundNBT());return p.getPersistentData().getCompound(TAG);}
    public static int action(LivingEntity p){CompoundNBT n=p.getPersistentData().getCompound(TAG);return n.getLong("End")>p.level.getGameTime()?n.getInt("Action"):-1;}
    public static void pose(LivingEntity p,int action,int life){state(p).putInt("Action",action);state(p).putLong("Start",p.level.getGameTime());state(p).putLong("End",p.level.getGameTime()+life);sync(p);}
    public static void sync(LivingEntity p){if(p instanceof ServerPlayerEntity)CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(()->p),new DecayPacket(p.getId(),state(p).copy()));}
    public static boolean awaken(PlayerEntity p,ItemStack stack){
        if(p.level.isClientSide||!xyz.pixelatedw.mineminenomi.config.CommonConfig.INSTANCE.hasAwakeningsEnabled()||!fruit(p)||awakened(p)||stack.isEmpty()||!(stack.getItem() instanceof net.kazi.kazimod.items.AwakeningEssenceItem))return false;
        DevilFruitCapability.get(p).setAwakenedFruit(true);if(!p.isCreative())stack.shrink(1);reconcile(p);WyNetwork.sendTo(new SSyncDevilFruitPacket(p.getId(),DevilFruitCapability.get(p)),p);p.displayClientMessage(new StringTextComponent("Sabi awakened: Decay. Your rust techniques have been replaced."),false);return true;
    }
    public static void reconcile(PlayerEntity p){
        if(p.level.isClientSide)return;IAbilityData data=AbilityDataCapability.get(p);Set<AbilityCore<?>> all=new HashSet<>();boolean changed=false;List<Integer> slots=new ArrayList<>();
        for(AbilityCoreUnlockWrapper<?> w:data.getUnlockedAbilities())all.add(w.getAbilityCore());for(IAbility a:data.getEquippedAndPassiveAbilities())all.add(a.getCore());
        for(AbilityCore<?> c:all)if(awakened(p)?original(c):ours(c)){IAbility a=data.getEquippedAbility(c),passive=data.getPassiveAbility(c);if(a!=null){slots.add(data.getEquippedAbilitySlot(a));AbilityHelper.emergencyStopAbility(p,a);}if(passive!=null)AbilityHelper.emergencyStopAbility(p,passive);data.removeEquippedAbility(c);data.removePassiveAbility(c);data.removeUnlockedAbility(c);changed=true;}
        if(awakened(p)){for(AbilityCore<?> c:DecayAbility.CORES)if(!data.hasUnlockedAbility(c)){data.addUnlockedAbility(c,AbilityUnlock.PROGRESSION);changed=true;}Collections.sort(slots);int i=0;for(AbilityCore<?> c:DecayAbility.CORES)if(data.getEquippedAbility(c)==null&&i<slots.size())data.setEquippedAbility(slots.get(i++),c.createAbility());}
        if(changed)WyNetwork.sendTo(new SSyncAbilityDataPacket(p.getId(),data),p);
    }
    public static void clear(PlayerEntity p){if(p.level.isClientSide)return;for(IAbility a:new ArrayList<>(AbilityDataCapability.get(p).getEquippedAndPassiveAbilities()))if(ours(a.getCore()))AbilityHelper.emergencyStopAbility(p,a);pose(p,-1,0);for(DecayField f:p.level.getEntitiesOfClass(DecayField.class,p.getBoundingBox().inflate(96),f->f.owner()==p))f.remove();}
    private static void tick(TickEvent.PlayerTickEvent e){if(e.phase!=TickEvent.Phase.END||e.player.level.isClientSide)return;PlayerEntity p=e.player;if(!fruit(p)&&!p.getPersistentData().contains(TAG))return;if(p.tickCount%20==0)reconcile(p);if(action(p)>=0&&!usable(p))clear(p);}
    private static boolean front(LivingEntity p,Entity e){return e!=null&&e.position().subtract(p.position()).normalize().dot(p.getLookAngle())>.2;}
    private static void hurt(LivingHurtEvent e){LivingEntity p=e.getEntityLiving();if(p.level.isClientSide||!usable(p))return;int action=action(p);if(action==5&&e.getAmount()>=8){state(p).putBoolean("Interrupted",true);}
        if(action==3&&front(p,e.getSource().getDirectEntity())&&!e.getSource().isBypassInvul()){e.setAmount(e.getAmount()*.5F);Entity a=e.getSource().getEntity();long now=p.level.getGameTime();if(a instanceof LivingEntity&&p.distanceToSqr(a)<9&&state(p).getLong("Retaliate")<=now&&net.kazi.kazimod.petal.PetalFeatures.canHit(p,(LivingEntity)a)){state(p).putLong("Retaliate",now+20);DecayAbility.CORES[3].createAbility().strike(p,(LivingEntity)a,6);}}
    }
    private static void projectile(ProjectileImpactEvent e){if(e.getEntity().level.isClientSide||!(e.getRayTraceResult() instanceof EntityRayTraceResult))return;Entity target=((EntityRayTraceResult)e.getRayTraceResult()).getEntity(),shot=e.getEntity();if(!(target instanceof LivingEntity))return;LivingEntity p=(LivingEntity)target;if(!usable(p)||action(p)!=3||!front(p,shot))return;
        Entity shooter=shot instanceof ProjectileEntity?((ProjectileEntity)shot).getOwner():null;if(shooter instanceof LivingEntity&&!net.kazi.kazimod.petal.PetalFeatures.canHit(p,(LivingEntity)shooter))return;
        float damage=shot instanceof xyz.pixelatedw.mineminenomi.entities.projectiles.AbilityProjectileEntity?((xyz.pixelatedw.mineminenomi.entities.projectiles.AbilityProjectileEntity)shot).getDamage():shot instanceof AbstractArrowEntity?(float)(((AbstractArrowEntity)shot).getBaseDamage()*shot.getDeltaMovement().length()):Float.POSITIVE_INFINITY;
        if(damage<35){DecayField.spawn(p,6,shot.position(),20,1).target(p);shot.remove();e.setCanceled(true);}
    }
    private DecayFeatures(){}
}
