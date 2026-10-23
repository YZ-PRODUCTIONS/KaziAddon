package net.kazi.kazimod.shikigami;

import java.util.*;
import net.kazi.kazimod.api.KaziRegistry;
import net.kazi.kazimod.mahoraga.*;
import net.kazi.kazimod.agito.AgitoAbility;
import net.kazi.kazimod.util.FruitAbilityInjector;
import net.minecraft.entity.*;
import net.minecraft.entity.player.*;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.*;
import net.minecraft.util.math.*;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.*;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.registries.*;
import xyz.pixelatedw.mineminenomi.api.abilities.*;
import xyz.pixelatedw.mineminenomi.abilities.kage.*;
import xyz.pixelatedw.mineminenomi.api.events.ability.*;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.data.entity.ability.*;
import xyz.pixelatedw.mineminenomi.init.ModAbilities;
import xyz.pixelatedw.mineminenomi.packets.server.SSyncAbilityDataPacket;
import xyz.pixelatedw.mineminenomi.wypi.WyNetwork;

public final class ShikigamiFeatures {
    private static final DeferredRegister<EntityType<?>> TYPES=DeferredRegister.create(ForgeRegistries.ENTITIES,"kazimod");
    private static final Map<ShikigamiKind,RegistryObject<EntityType<ShikigamiEntity>>> ENTITIES=new EnumMap<>(ShikigamiKind.class);
    static {for(ShikigamiKind k:ShikigamiKind.values())ENTITIES.put(k,TYPES.register(k.id,()->EntityType.Builder
            .<ShikigamiEntity>of((type,world)->new ShikigamiEntity(type,world,k),EntityClassification.CREATURE)
            .sized(k.width,k.height).clientTrackingRange(16).updateInterval(2).setShouldReceiveVelocityUpdates(true).noSave().build(k.id)));}
    public static EntityType<ShikigamiEntity> type(ShikigamiKind k){return ENTITIES.get(k).get();}
    public static boolean awakeningCore(AbilityCore<?> core){
        if(core==MahoragaAbility.INSTANCE||core==AgitoAbility.INSTANCE)return true;
        for(ShikigamiKind k:ShikigamiKind.values())if(core==ShikigamiAbility.core(k))return true;
        return false;
    }
    public static AbilityCore<?>[] awakeningAbilities(){
        List<AbilityCore<?>> cores=new ArrayList<>();cores.add(MahoragaAbility.INSTANCE);cores.add(AgitoAbility.INSTANCE);
        for(ShikigamiKind k:ShikigamiKind.values())cores.add(ShikigamiAbility.core(k));return cores.toArray(new AbilityCore<?>[0]);
    }
    public static boolean retired(AbilityCore<?> core){
        if(core==null||awakeningCore(core))return false;
        if(core==net.kazi.kazimod.abilities.KageRework.DoppelmanRework.INSTANCE)return true;
        for(AbilityCore<?> old:new AbilityCore<?>[]{DoppelmanAbility.INSTANCE,NightmareSoldiersAbility.INSTANCE,
                KageKakumeiAbility.INSTANCE,BrickBatAbility.INSTANCE,BlackBoxAbility.INSTANCE,ShadowsAsgardAbility.INSTANCE,
                KageGiriAbility.INSTANCE,TsunoTokageAbility.INSTANCE,KagemushaAbility.INSTANCE})if(old==core)return true;
        for(AbilityCore<?> old:ModAbilities.KAGE_KAGE_NO_MI.getAbilities())if(old==core)return true;
        return false;
    }
    public static void init(IEventBus bus){
        TYPES.register(bus);
        for(ShikigamiKind k:ShikigamiKind.values())KaziRegistry.registerAbility(ShikigamiAbility.core(k));
        bus.addListener((net.minecraftforge.event.entity.EntityAttributeCreationEvent e)->{
            for(ShikigamiKind k:ShikigamiKind.values())e.put(type(k),ShikigamiEntity.attributes(k).build());
        });
        bus.addListener((FMLCommonSetupEvent e)->e.enqueueWork(()->{
            for(ShikigamiKind k:ShikigamiKind.values())FruitAbilityInjector.addAbilities(ModAbilities.KAGE_KAGE_NO_MI,ShikigamiAbility.core(k));
            net.kazi.kazimod.events.AwakeningAbilityLoginFix.invalidateCache();
        }));
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST,(UnlockAbilityEvent e)->{
            if(MahoragaAbility.awakened(e.getEntityLiving())&&retired(e.getAbilityCore()))e.setResult(Event.Result.DENY);
        });
        MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGHEST,(AbilityUseEvent.Pre e)->{
            if(MahoragaAbility.awakened(e.getEntityLiving())&&retired(e.getAbility().getCore()))e.setCanceled(true);
        });
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST,ShikigamiFeatures::attacked);
        MinecraftForge.EVENT_BUS.addListener(ShikigamiFeatures::tick);
        MinecraftForge.EVENT_BUS.addListener(TenShadowsTaming::clonePlayer);
        MinecraftForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent e)->cleanup(e.getPlayer(),(ServerWorld)e.getPlayer().level));
        MinecraftForge.EVENT_BUS.addListener((PlayerEvent.PlayerChangedDimensionEvent e)->{
            if(e.getPlayer() instanceof ServerPlayerEntity)cleanup(e.getPlayer(),((ServerPlayerEntity)e.getPlayer()).getServer().getLevel(e.getFrom()));
        });
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->ShikigamiRenderer.init(bus));
    }
    private static void attacked(LivingAttackEvent e){
        LivingEntity victim=e.getEntityLiving();if(victim.level.isClientSide||!(e.getSource().getEntity() instanceof LivingEntity))return;
        LivingEntity attacker=(LivingEntity)e.getSource().getEntity();
        for(ShikigamiEntity summon:victim.level.getEntitiesOfClass(ShikigamiEntity.class,victim.getBoundingBox().inflate(50)))
            if(victim==summon||MahoragaFeatures.allied(summon.owner(),victim))summon.defend(attacker);
    }
    private static void tick(TickEvent.PlayerTickEvent e){
        PlayerEntity p=e.player;if(e.phase!=TickEvent.Phase.END||p.level.isClientSide||p.tickCount%10!=0)return;
        IAbilityData data=AbilityDataCapability.get(p);boolean awakened=MahoragaAbility.awakened(p),changed=false;
        if(awakened){
            Set<AbilityCore<?>> oldCores=new HashSet<>();
            for(AbilityCoreUnlockWrapper<?> wrapper:data.getUnlockedAbilities())oldCores.add(wrapper.getAbilityCore());
            for(IAbility equipped:data.getEquippedAbilities())oldCores.add(equipped.getCore());
            for(IAbility passive:data.getPassiveAbilities())oldCores.add(passive.getCore());
            for(AbilityCore<?> core:oldCores)if(retired(core)){
                IAbility ability=data.getEquippedAbility(core);if(ability!=null)AbilityHelper.emergencyStopAbility(p,ability);
                IAbility passive=data.getPassiveAbility(core);if(passive!=null)AbilityHelper.emergencyStopAbility(p,passive);
                data.removePassiveAbility(core);data.removeEquippedAbility(core);data.removeUnlockedAbility(core);changed=true;
            }
            for(AbilityCore<?> core:awakeningAbilities())if(!data.hasUnlockedAbility(core)){data.addUnlockedAbility(core,AbilityUnlock.PROGRESSION);changed=true;}
        }
        for(ShikigamiKind k:ShikigamiKind.values()){
            ShikigamiEntity summon=find(p,k);
            if(summon!=null)summon.preserveFor(p);else recover(p,k);
            ShikigamiAbility ability=data.getEquippedAbility(ShikigamiAbility.core(k));
            if(ability!=null){CompoundNBT reserve=p.getPersistentData().getCompound(k.reserveTag());
                ability.updateHud(p,summon!=null?summon.getHealth():reserve.contains("Health")?reserve.getFloat("Health"):0,
                        (float)k.health,summon!=null&&!summon.dismissing());}
            if(!awakened){dismiss(p,k);if(data.hasUnlockedAbility(ShikigamiAbility.core(k))){data.removeEquippedAbility(ShikigamiAbility.core(k));data.removeUnlockedAbility(ShikigamiAbility.core(k));changed=true;}}
        }
        if(changed)WyNetwork.sendTo(new SSyncAbilityDataPacket(p.getId(),data),p);
    }
    public static void recover(LivingEntity owner,ShikigamiKind k){
        if(!owner.getPersistentData().contains(k.reserveTag(),10))return;
        CompoundNBT n=owner.getPersistentData().getCompound(k.reserveTag());long now=owner.level.getGameTime(),last=n.getLong("LastHeal");
        if(last>now){n.putLong("LastHeal",now);return;}long seconds=(now-last)/20;
        if(seconds>0){n.putFloat("Health",Math.min((float)k.health,n.getFloat("Health")+(float)(k.health*.01*seconds)));n.putLong("LastHeal",last+seconds*20);}
    }
    public static ShikigamiEntity find(LivingEntity owner,ShikigamiKind k){
        if(!(owner.level instanceof ServerWorld)||!owner.getPersistentData().hasUUID(k.summonTag()))return null;
        Entity e=((ServerWorld)owner.level).getEntity(owner.getPersistentData().getUUID(k.summonTag()));
        return e instanceof ShikigamiEntity&&e.isAlive()&&((ShikigamiEntity)e).kind==k&&owner.getUUID().equals(((ShikigamiEntity)e).ownerId())?(ShikigamiEntity)e:null;
    }
    public static void dismiss(LivingEntity owner,ShikigamiKind k){ShikigamiEntity e=find(owner,k);if(e!=null)e.beginDismiss();else owner.getPersistentData().remove(k.summonTag());}
    private static void cleanup(PlayerEntity p,ServerWorld world){
        for(ShikigamiKind k:ShikigamiKind.values()){
            if(world!=null&&p.getPersistentData().hasUUID(k.summonTag())){Entity e=world.getEntity(p.getPersistentData().getUUID(k.summonTag()));
                if(e instanceof ShikigamiEntity){if(e.isAlive())((ShikigamiEntity)e).preserveFor(p);e.remove();}}
            p.getPersistentData().remove(k.summonTag());
        }
    }
    public static ShikigamiEntity summon(LivingEntity owner,ShikigamiKind k){
        ShikigamiEntity existing=find(owner,k);if(existing!=null)return existing.dismissing()?null:existing;
        if(!TenShadowsTaming.canSummon(owner,k.id))return null;
        ShikigamiEntity entity=new ShikigamiEntity(type(k),owner.level,k);
        Vector3d forward=owner.getLookAngle().multiply(1,0,1).normalize();
        Vector3d aim=owner.position().add(forward.scale(k==ShikigamiKind.ELEPHANT?12:4));
        if(k==ShikigamiKind.ELEPHANT){BlockRayTraceResult ray=owner.level.clip(new RayTraceContext(owner.getEyePosition(1),owner.getEyePosition(1).add(owner.getLookAngle().scale(40)),RayTraceContext.BlockMode.COLLIDER,RayTraceContext.FluidMode.NONE,owner));
            if(ray.getType()==RayTraceResult.Type.BLOCK)aim=ray.getLocation();}
        for(int attempt=0;attempt<12;attempt++){
            Vector3d at=aim.add(attempt==0?Vector3d.ZERO:new Vector3d(Math.cos(attempt*Math.PI/3)*3,0,Math.sin(attempt*Math.PI/3)*3));
            BlockPos top=new BlockPos(at).above(6);
            for(int down=0;down<32;down++){
                BlockPos ground=top.below(down);if(!owner.level.hasChunkAt(ground)||!owner.level.getBlockState(ground).isFaceSturdy(owner.level,ground,Direction.UP))continue;
                double y=ground.getY()+1.01+(k==ShikigamiKind.ELEPHANT?50:0);
                entity.setPos(ground.getX()+.5,y,ground.getZ()+.5);if(!owner.level.noCollision(entity))continue;
                recover(owner,k);entity.setOwner(owner);TenShadowsTaming.prepare(entity);entity.restoreFor(owner);entity.yRot=entity.yBodyRot=owner.yRot;entity.beginSummon();
                if(!owner.level.addFreshEntity(entity))return null;
                owner.getPersistentData().putUUID(k.summonTag(),entity.getUUID());owner.getPersistentData().remove(k.defeatedTag());TenShadowsTaming.started(entity);return entity;
            }
        }
        if(owner instanceof PlayerEntity)((PlayerEntity)owner).displayClientMessage(new TranslationTextComponent("message.kazimod.shikigami.no_room"),true);return null;
    }
    private ShikigamiFeatures(){}
}
