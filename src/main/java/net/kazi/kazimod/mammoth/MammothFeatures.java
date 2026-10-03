package net.kazi.kazimod.mammoth;

import java.util.*;
import net.kazi.kazimod.api.KaziRegistry;
import net.kazi.kazimod.util.FruitAbilityInjector;
import net.minecraft.entity.*;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.*;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.registries.*;
import xyz.pixelatedw.mineminenomi.abilities.zoumammoth.*;
import xyz.pixelatedw.mineminenomi.api.abilities.*;
import xyz.pixelatedw.mineminenomi.api.abilities.components.*;
import xyz.pixelatedw.mineminenomi.api.damagesource.*;
import xyz.pixelatedw.mineminenomi.api.enums.AbilityCommandGroup;
import xyz.pixelatedw.mineminenomi.data.entity.ability.*;
import xyz.pixelatedw.mineminenomi.data.entity.devilfruit.DevilFruitCapability;
import xyz.pixelatedw.mineminenomi.init.ModAbilities;
import xyz.pixelatedw.mineminenomi.packets.server.SSyncAbilityDataPacket;
import xyz.pixelatedw.mineminenomi.wypi.WyNetwork;

public final class MammothFeatures {
    private static final DeferredRegister<EntityType<?>> TYPES=DeferredRegister.create(ForgeRegistries.ENTITIES,"kazimod");
    public static final RegistryObject<EntityType<MammothVfxEntity>> EFFECT=TYPES.register("mammoth_tremor",()->EntityType.Builder.<MammothVfxEntity>of(MammothVfxEntity::new,EntityClassification.MISC).sized(1,1).clientTrackingRange(12).updateInterval(2).noSave().build("mammoth_tremor"));
    public static final RegistryObject<EntityType<MammothWindEntity>> WIND=TYPES.register("mammoth_wind",()->EntityType.Builder.<MammothWindEntity>of(MammothWindEntity::new,EntityClassification.MISC).sized(3,1.5F).clientTrackingRange(10).updateInterval(1).setShouldReceiveVelocityUpdates(true).noSave().build("mammoth_wind"));
    public static final Map<MammothAbility.Move,AbilityCore<MammothAbility>> CORES=new EnumMap<>(MammothAbility.Move.class);
    private static final Map<AbilityCore<?>,MammothAbility.Move> OLD=new LinkedHashMap<>();
    public static void init(IEventBus bus){
        TYPES.register(bus);
        OLD.put(AncientSweepAbility.INSTANCE,MammothAbility.Move.SWEEP);OLD.put(AncientStompAbility.INSTANCE,MammothAbility.Move.STOMP);OLD.put(AncientTrunkShotAbility.INSTANCE,MammothAbility.Move.VACUUM);
        for(MammothAbility.Move move:MammothAbility.Move.values()){
            String icon=move==MammothAbility.Move.SWEEP?"ancient_sweep":move==MammothAbility.Move.STOMP?"ancient_stomp":move==MammothAbility.Move.STAMPEDE?"ancient_stampede":"ancient_trunk_vacuum";
            AbilityCore.Builder<MammothAbility> b=new AbilityCore.Builder<MammothAbility>(move.title,AbilityCategory.DEVIL_FRUITS,c->new MammothAbility(c,move))
                    .setIcon(new ResourceLocation(move==MammothAbility.Move.SWEEP||move==MammothAbility.Move.STOMP?"mineminenomi":"kazimod","textures/abilities/"+icon+".png"))
                    .addDescriptionLine(new StringTextComponent(move.description)).addAdvancedDescriptionLine(CooldownComponent.getTooltip(move.cooldown))
                    .setSourceType(move==MammothAbility.Move.SWEEP?SourceType.PROJECTILE:SourceType.BLUNT)
                    .setSourceHakiNature(move==MammothAbility.Move.STAMPEDE?SourceHakiNature.IMBUING:SourceHakiNature.HARDENING);
            if(move.charge>0)b.addAdvancedDescriptionLine(ChargeComponent.getTooltip(move.charge));
            if(move.duration>0)b.addAdvancedDescriptionLine(ContinuousComponent.getTooltip(move.duration));
            if(move==MammothAbility.Move.STOMP)b.setSourceElement(SourceElement.SHOCKWAVE);
            AbilityCore<MammothAbility> core=b.build();CORES.put(move,core);KaziRegistry.registerAbility(core);
        }
        AbilityCommandGroup.create("MAMMOTH_REWORK",()->CORES.values().toArray(new AbilityCore[0]));
        bus.addListener((FMLCommonSetupEvent e)->e.enqueueWork(()->{
            for(Map.Entry<AbilityCore<?>,MammothAbility.Move> entry:OLD.entrySet())FruitAbilityInjector.replaceAbility(ModAbilities.ZOU_ZOU_NO_MI_MAMMOTH,entry.getKey(),CORES.get(entry.getValue()));
            FruitAbilityInjector.addAbilities(ModAbilities.ZOU_ZOU_NO_MI_MAMMOTH,CORES.get(MammothAbility.Move.STAMPEDE));
        }));
        MinecraftForge.EVENT_BUS.addListener(MammothFeatures::login);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST,MammothFeatures::damage);
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->MammothClient.init(bus));
    }
    public static boolean legacy(AbilityCore<?> core){return OLD.containsKey(core);}
    private static void damage(LivingDamageEvent event){
        LivingEntity e=event.getEntityLiving();if(!e.level.isClientSide&&MammothCombat.active(e))event.setAmount(MammothCombatMath.reducedDamage(event.getAmount(),MammothCombat.full(e)));
    }
    private static void login(PlayerEvent.PlayerLoggedInEvent event){
        if(event.getPlayer().level.isClientSide)return;
        IAbilityData data=AbilityDataCapability.get(event.getPlayer());List<IAbility> slots=new ArrayList<>(data.getRawEquippedAbilities());boolean changed=false;
        for(Map.Entry<AbilityCore<?>,MammothAbility.Move> entry:OLD.entrySet())if(data.hasUnlockedAbility(entry.getKey())){
            AbilityCore<?> replacement=CORES.get(entry.getValue());if(!data.hasUnlockedAbility(replacement))data.addUnlockedAbility(replacement,data.getUnlockTypeForAbility(entry.getKey()));
            data.removeUnlockedAbility(entry.getKey());changed=true;
        }
        if(DevilFruitCapability.get(event.getPlayer()).hasDevilFruit(ModAbilities.ZOU_ZOU_NO_MI_MAMMOTH)&&data.hasUnlockedAbility(MammothGuardPointAbility.INSTANCE)
                &&!data.hasUnlockedAbility(CORES.get(MammothAbility.Move.STAMPEDE))){data.addUnlockedAbility(CORES.get(MammothAbility.Move.STAMPEDE),AbilityUnlock.PROGRESSION);changed=true;}
        Set<AbilityCore<?>> used=new HashSet<>();for(IAbility ability:slots)if(ability!=null&&!legacy(ability.getCore()))used.add(ability.getCore());
        for(int i=0;i<slots.size();i++){
            IAbility old=slots.get(i);if(old==null||!legacy(old.getCore()))continue;AbilityCore<?> core=CORES.get(OLD.get(old.getCore()));
            data.setEquippedAbility(i,used.add(core)?core.createAbility():null);changed=true;
        }
        if(changed)WyNetwork.sendToAllTrackingAndSelf(new SSyncAbilityDataPacket(event.getPlayer().getId(),data),event.getPlayer());
    }
}
