package net.kazi.kazimod.ghoul;

import java.util.*;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.attributes.*;
import net.minecraft.entity.player.*;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.*;
import net.minecraftforge.fml.network.*;
import net.minecraftforge.fml.network.simple.SimpleChannel;
import net.minecraftforge.registries.*;
import xyz.pixelatedw.mineminenomi.api.abilities.*;
import xyz.pixelatedw.mineminenomi.api.charactercreator.*;
import xyz.pixelatedw.mineminenomi.api.enums.AbilityCommandGroup;
import xyz.pixelatedw.mineminenomi.api.events.ability.*;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.data.entity.ability.*;
import xyz.pixelatedw.mineminenomi.packets.server.SSyncAbilityDataPacket;
import xyz.pixelatedw.mineminenomi.wypi.WyNetwork;

public final class GhoulFeatures {
    // Park the race without removing its registry entry or saved assets.
    public static final boolean AVAILABLE = false;
    private static final UUID POWER=UUID.fromString("a06397d1-daf7-481f-bc65-d18d1518efe0"),GUARD=UUID.fromString("a06397d1-daf7-481f-bc65-d18d1518efe1"),SLOW=UUID.fromString("a06397d1-daf7-481f-bc65-d18d1518efe2");
    private static final DeferredRegister<EntityType<?>> TYPES=DeferredRegister.create(ForgeRegistries.ENTITIES,"kazimod");
    public static final RegistryObject<EntityType<GhoulProjectile>> SHARD=TYPES.register("ghoul_crystal",()->EntityType.Builder.<GhoulProjectile>of(GhoulProjectile::new,EntityClassification.MISC).sized(.2F,.2F).clientTrackingRange(10).updateInterval(1).noSave().build("ghoul_crystal"));
    private static final SimpleChannel CHANNEL=NetworkRegistry.newSimpleChannel(new ResourceLocation("kazimod","ghoul"),()->"1","1"::equals,"1"::equals);
    public static RaceId race(){
        CharacterCreatorSelectionMap.SelectionInfo info=new CharacterCreatorSelectionMap.SelectionInfo(new ResourceLocation("kazimod","textures/gui/icons/ghoul.png"));
        info.addTopAbilities(GhoulAbility.CORES);
        RaceId id=new RaceId(info,AVAILABLE,10);id.setSubRaces(new ArrayList<>(Arrays.asList(GhoulState.TYPES)));return id;
    }
    public static void init(IEventBus bus){
        TYPES.register(bus);
        for(AbilityCore<?> c:GhoulAbility.CORES)net.kazi.kazimod.api.KaziRegistry.registerAbility(c);
        AbilityCommandGroup.create("GHOUL",()->GhoulAbility.CORES);
        CHANNEL.registerMessage(0,GhoulPacket.class,GhoulPacket::encode,GhoulPacket::decode,GhoulPacket::handle,Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        MinecraftForge.EVENT_BUS.addListener(GhoulFeatures::tick);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGH,GhoulFeatures::hurt);
        MinecraftForge.EVENT_BUS.addListener((LivingEvent.LivingJumpEvent e)->{if(!e.getEntityLiving().level.isClientSide&&GhoulState.ghoul(e.getEntityLiving()))GhoulAbility.launch(e.getEntityLiving(),e.getEntityLiving().getDeltaMovement().add(0,.12,0));});
        MinecraftForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent e)->{clear(e.getPlayer());reconcile(e.getPlayer());sync(e.getPlayer());});
        MinecraftForge.EVENT_BUS.addListener((PlayerEvent.PlayerRespawnEvent e)->{clear(e.getPlayer());reconcile(e.getPlayer());sync(e.getPlayer());});
        MinecraftForge.EVENT_BUS.addListener((PlayerEvent.PlayerChangedDimensionEvent e)->{clear(e.getPlayer());sync(e.getPlayer());});
        MinecraftForge.EVENT_BUS.addListener((LivingDeathEvent e)->{if(e.getEntityLiving() instanceof PlayerEntity){clear((PlayerEntity)e.getEntityLiving());sync((PlayerEntity)e.getEntityLiving());}});
        MinecraftForge.EVENT_BUS.addListener((PlayerEvent.StartTracking e)->{if(e.getPlayer() instanceof ServerPlayerEntity&&e.getTarget() instanceof PlayerEntity)CHANNEL.send(PacketDistributor.PLAYER.with(()->(ServerPlayerEntity)e.getPlayer()),packet((PlayerEntity)e.getTarget()));});
        MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGHEST,(AbilityUseEvent.Pre e)->{
            if(retired(e.getAbility().getCore()) || e.getEntityLiving().getPersistentData().getLong("KaziGhoulHeldUntil")>e.getEntityLiving().level.getGameTime()
                    || GhoulState.ghoul(e.getEntityLiving())&&GhoulState.action(e.getEntityLiving())>0&&!(e.getAbility() instanceof GhoulAbility)) e.setCanceled(true);
        });
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST,(UnlockAbilityEvent e)->{if(retired(e.getAbilityCore()))e.setResult(Event.Result.DENY);});
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->GhoulClient.init(bus));
    }
    public static boolean retired(AbilityCore<?> c){return c!=null&&c.getRegistryName()!=null&&"cartaddon".equals(c.getRegistryName().getNamespace())&&Arrays.asList("zombie_bite","zombie_scratch","dead_again","breathless","dead_appetite","zombie_passive","zombie_passive_bonuses").contains(c.getRegistryName().getPath());}
    public static void reconcile(PlayerEntity p){
        if(p.level.isClientSide)return;boolean ghoul=GhoulState.ghoul(p),changed=false;IAbilityData data=AbilityDataCapability.get(p);
        Set<AbilityCore<?>> existing=new HashSet<>();for(AbilityCoreUnlockWrapper<?> w:data.getUnlockedAbilities())existing.add(w.getAbilityCore());for(IAbility a:data.getEquippedAndPassiveAbilities())existing.add(a.getCore());
        for(AbilityCore<?> c:existing)if(retired(c)||Arrays.asList(GhoulAbility.CORES).contains(c)&&!c.canUnlock(p)){
            IAbility a=data.getEquippedAbility(c),b=data.getPassiveAbility(c);if(a!=null)AbilityHelper.emergencyStopAbility(p,a);if(b!=null)AbilityHelper.emergencyStopAbility(p,b);
            data.removeEquippedAbility(c);data.removePassiveAbility(c);data.removeUnlockedAbility(c);changed=true;
        }
        if(ghoul)for(AbilityCore<?> c:GhoulAbility.CORES)if(c.canUnlock(p)&&!data.hasUnlockedAbility(c)){data.addUnlockedAbility(c,AbilityUnlock.PROGRESSION);changed=true;}
        // Old saves can retain permanent modifiers after the original passive is filtered during loading.
        if(ghoul){
            p.getAttribute(Attributes.MAX_HEALTH).removeModifier(UUID.fromString("26575a16-2771-4696-82c3-3070169ca1b0"));
            p.getAttribute(Attributes.MOVEMENT_SPEED).removeModifier(UUID.fromString("d0408f50-0520-4ca3-8d64-bbf4f6425cf4"));
            ModifiableAttributeInstance swim=p.getAttribute(net.minecraftforge.common.ForgeMod.SWIM_SPEED.get());
            if(swim!=null)swim.removeModifier(UUID.fromString("121c7cbd-ec5a-48d4-b655-88be68a75642"));
        }
        if(changed)WyNetwork.sendTo(new SSyncAbilityDataPacket(p.getId(),data),p);
    }
    private static void clear(PlayerEntity p){if(!p.getPersistentData().contains(GhoulState.TAG,10))return;CompoundNBT n=GhoulState.get(p);n.putBoolean("Deployed",false);n.putLong("End",0);n.putBoolean("CounterHit",false);n.remove("Target");}
    private static void tick(TickEvent.PlayerTickEvent e){
        PlayerEntity p=e.player;if(e.phase!=TickEvent.Phase.END||p.level.isClientSide)return;
        boolean ghoul=GhoulState.ghoul(p);if(!ghoul&&!p.getPersistentData().contains(GhoulState.TAG,10))return;
        CompoundNBT n=GhoulState.get(p);
        if(p.tickCount%20==0)reconcile(p);
        if(!n.getBoolean("Init")){n.putBoolean("Init",true);n.putFloat("RC",100);}
        if(!ghoul||!p.isAlive()||p.isSpectator())clear(p);
        boolean active=ghoul&&GhoulState.active(p),koukaku=active&&GhoulState.type(p)==1;
        modifier(p,Attributes.ATTACK_DAMAGE,POWER,ghoul?2:0,AttributeModifier.Operation.ADDITION);
        modifier(p,Attributes.KNOCKBACK_RESISTANCE,GUARD,koukaku?.5:0,AttributeModifier.Operation.ADDITION);
        modifier(p,Attributes.MOVEMENT_SPEED,SLOW,koukaku?-.1:0,AttributeModifier.Operation.MULTIPLY_TOTAL);
        if(ghoul&&p.isAlive()){
            if(GhoulState.action(p)==0)n.putFloat("RC",Math.min(100,GhoulState.rc(p)+(GhoulState.type(p)==3?.16F:.1F)));
            if(p.tickCount%20==0&&p.level.getGameTime()-n.getLong("LastHit")>60&&p.getHealth()<p.getMaxHealth()&&GhoulState.spend(p,2))p.heal(GhoulState.type(p)==2?2:1);
            if(active&&GhoulState.type(p)==0&&!p.isOnGround()&&!p.isCrouching()&&p.getDeltaMovement().y<-.12){GhoulAbility.launch(p,p.getDeltaMovement().multiply(1,0,1).add(0,-.12,0));p.fallDistance=0;}
        }
        if(p.tickCount%5==0)sync(p);
    }
    private static void modifier(PlayerEntity p,Attribute a,UUID id,double value,AttributeModifier.Operation op){ModifiableAttributeInstance attr=p.getAttribute(a);if(attr==null)return;AttributeModifier old=attr.getModifier(id);if(old!=null&&old.getAmount()==value)return;if(old!=null)attr.removeModifier(id);if(value!=0)attr.addTransientModifier(new AttributeModifier(id,"Ghoul physiology",value,op));}
    private static void hurt(LivingHurtEvent e){
        LivingEntity p=e.getEntityLiving();if(p.level.isClientSide||!GhoulState.ghoul(p))return;
        CompoundNBT n=GhoulState.get(p);n.putLong("LastHit",p.level.getGameTime());
        Entity attacker=e.getSource().getDirectEntity();if(attacker==null||e.getSource().isBypassInvul()||!GhoulState.active(p))return;
        Vector3d toward=attacker.position().subtract(p.position()).multiply(1,0,1).normalize();
        if(p.getLookAngle().multiply(1,0,1).normalize().dot(toward)<.15)return;
        int action=GhoulState.action(p);
        if(action==5||action==7){boolean perfect=action==5&&p.level.getGameTime()-n.getLong("Start")<=5;e.setAmount(e.getAmount()*(perfect?.05F:.25F));effect(p,3,p.getEyePosition(1).add(p.getLookAngle().scale(.8)),1);}
        if(action==15&&!e.getSource().isProjectile()&&attacker instanceof LivingEntity&&!n.getBoolean("CounterHit")){e.setAmount(e.getAmount()*.2F);n.putBoolean("CounterHit",true);n.putInt("CounterTarget",attacker.getId());effect(p,3,p.getEyePosition(1).add(p.getLookAngle().scale(.8)),1);}
    }
    public static boolean canHit(LivingEntity p,LivingEntity t){return net.kazi.kazimod.saiyan.SaiyanAbility.canHit(p,t);}
    public static LivingEntity aim(LivingEntity p,double range){
        LivingEntity best=null;double nearest=range*range;Vector3d start=p.getEyePosition(1),end=start.add(p.getLookAngle().scale(range));
        for(LivingEntity t:p.level.getEntitiesOfClass(LivingEntity.class,p.getBoundingBox().inflate(range))){
            if(!canHit(p,t)||!p.canSee(t))continue;
            net.minecraft.util.math.AxisAlignedBB box=t.getBoundingBox().inflate(.35);Optional<Vector3d> hit=box.clip(start,end);
            double distance=box.contains(start)?0:hit.isPresent()?start.distanceToSqr(hit.get()):Double.POSITIVE_INFINITY;
            if(distance<=nearest){nearest=distance;best=t;}
        }return best;
    }
    private static GhoulPacket packet(PlayerEntity p){CompoundNBT n=GhoulState.get(p).copy();n.putBoolean("Ghoul",GhoulState.ghoul(p));n.putInt("Type",GhoulState.type(p));return new GhoulPacket(p.getId(),n);}
    public static void sync(PlayerEntity p){if(p instanceof ServerPlayerEntity)CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(()->p),packet(p));}
    public static void effect(LivingEntity p,int kind,Vector3d at,float size){
        if(!(p instanceof ServerPlayerEntity))return;CompoundNBT n=new CompoundNBT();n.putInt("Fx",kind);n.putInt("Type",GhoulState.type(p));n.putDouble("X",at.x);n.putDouble("Y",at.y);n.putDouble("Z",at.z);n.putFloat("Size",size);n.putFloat("Yaw",p.yRot);
        CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(()->p),new GhoulPacket(p.getId(),n));
    }
}
