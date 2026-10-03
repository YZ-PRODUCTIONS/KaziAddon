package net.kazi.kazimod.preserved.cerberus;

import java.util.*;
import net.MrMagicalCart.cartaddon.abilities.inucerberus.*;
import net.MrMagicalCart.cartaddon.init.CartAbilities;
import net.kazi.kazimod.api.KaziRegistry;
import net.kazi.kazimod.util.FruitAbilityInjector;
import net.minecraft.entity.*;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.registries.*;
import xyz.pixelatedw.mineminenomi.api.abilities.*;
import xyz.pixelatedw.mineminenomi.api.abilities.components.CooldownComponent;
import xyz.pixelatedw.mineminenomi.api.enums.AbilityCommandGroup;
import xyz.pixelatedw.mineminenomi.api.damagesource.*;
import xyz.pixelatedw.mineminenomi.data.entity.ability.*;

public final class CerberusFeatures {
    public static final DeferredRegister<EntityType<?>> ENTITIES=DeferredRegister.create(ForgeRegistries.ENTITIES,"kazimod");
    public static final RegistryObject<EntityType<CerberusEffectEntity>> EFFECT=ENTITIES.register("cerberus_hellfire",()->
            EntityType.Builder.<CerberusEffectEntity>of(CerberusEffectEntity::new,EntityClassification.MISC)
                    .sized(2.2F,2.2F).clientTrackingRange(12).updateInterval(1).setShouldReceiveVelocityUpdates(true).noSave().build("cerberus_hellfire"));
    public static final Map<CerberusAbility.Move,AbilityCore<CerberusAbility>> CORES=new EnumMap<>(CerberusAbility.Move.class);
    public static final AbilityCore<CerberusHuntPassive> HUNT=new AbilityCore.Builder<CerberusHuntPassive>("Three-Headed Hunt",AbilityCategory.DEVIL_FRUITS,AbilityType.PASSIVE,CerberusHuntPassive::new)
            .setIcon(icon("fang")).addDescriptionLine(new StringTextComponent("Triple Fang: left, right, then a heavy center bite. Different attacks build three Hunt marks for 6 seconds. The next bite consumes them to slow prey; Threefold Execution also halves healing for 4 seconds. Retains Cerberus night vision.")).build();
    private static final List<AbilityCore<?>> OLD=Arrays.asList(MaulingAbility.INSTANCE,HadesCrossAbility.INSTANCE,
            HellHathNoFuryAbility.INSTANCE,HellHeadMotorAbility.INSTANCE,HellHeadMotor2Ability.INSTANCE,
            CerberusRideableAbility.INSTANCE,GatesOfHellAbility.INSTANCE,InuCerberusPassiveAbility.INSTANCE);
    public static ResourceLocation icon(String name){return new ResourceLocation("kazimod","textures/abilities/cerberus_"+name+".png");}
    public static void init(IEventBus bus){
        ENTITIES.register(bus);
        KaziRegistry.registerAbility(HUNT);
        for(CerberusAbility.Move move:CerberusAbility.Move.values()){
            AbilityCore.Builder<CerberusAbility> builder=new AbilityCore.Builder<CerberusAbility>(move.title,AbilityCategory.DEVIL_FRUITS,c->new CerberusAbility(c,move))
                    .setIcon(icon(move.icon)).addDescriptionLine(new StringTextComponent(move.description))
                    .addAdvancedDescriptionLine(CooldownComponent.getTooltip(move.cooldown));
            if(move==CerberusAbility.Move.GATES)builder.setSourceElement(SourceElement.FIRE).setSourceType(SourceType.PROJECTILE).setSourceHakiNature(SourceHakiNature.SPECIAL);
            else builder.setSourceType(SourceType.BLUNT).setSourceHakiNature(SourceHakiNature.IMBUING);
            AbilityCore<CerberusAbility> core=builder.build();
            CORES.put(move,core);KaziRegistry.registerAbility(core);
        }
        AbilityCommandGroup.create("CERBERUS_REWORK",()->CORES.values().toArray(new AbilityCore[0]));
        bus.addListener((FMLCommonSetupEvent event)->event.enqueueWork(()->{
            List<AbilityCore<?>> additions=new ArrayList<>(CORES.values());additions.add(0,HUNT);
            FruitAbilityInjector.updateFruitAbilities(CartAbilities.INU_INU_NO_MI_MODEL_CERBERUS,OLD,additions,false);
            InuCerberusGuardPointAbility.INSTANCE.setIcon(icon("full"));
            InuCerberusHeavyPointAbility.INSTANCE.setIcon(icon("hybrid"));
            InuCerberusPassiveAbility.INSTANCE.setIcon(icon("fang"));
            InuCerberusImmunityPassiveAbility.INSTANCE.setIcon(icon("fang"));
        }));
        MinecraftForge.EVENT_BUS.addListener(CerberusFeatures::login);
        MinecraftForge.EVENT_BUS.addListener(CerberusCombat::attack);
        MinecraftForge.EVENT_BUS.addListener(CerberusCombat::meleeHit);
        MinecraftForge.EVENT_BUS.addListener(CerberusCombat::heal);
        MinecraftForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent e)->CerberusCombat.clear(e.getPlayer()));
        MinecraftForge.EVENT_BUS.addListener((PlayerEvent.PlayerChangedDimensionEvent e)->CerberusCombat.clear(e.getPlayer()));
    }
    private static void login(PlayerEvent.PlayerLoggedInEvent event){
        if(event.getPlayer().level.isClientSide)return;
        IAbilityData data=AbilityDataCapability.get(event.getPlayer());
        if(data==null)return;
        // Removing an unlock also clears its equipped slots. Snapshot those first.
        List<IAbility> equipped=data.getRawEquippedAbilities();
        boolean changed=false;
        for(AbilityCore<?> old:OLD)if(data.hasUnlockedAbility(old)){
            changed=true;
            AbilityCore<?> replacement=replacement(old);
            if(!data.hasUnlockedAbility(replacement))data.addUnlockedAbility(replacement,data.getUnlockTypeForAbility(old));
            data.removeUnlockedAbility(old);
        }
        if(data.hasUnlockedAbility(InuCerberusGuardPointAbility.INSTANCE)
                || data.hasUnlockedAbility(InuCerberusHeavyPointAbility.INSTANCE)) {
            for(AbilityCore<?> core:CORES.values())if(!data.hasUnlockedAbility(core)){data.addUnlockedAbility(core,AbilityUnlock.PROGRESSION);changed=true;}
            if(!data.hasUnlockedAbility(HUNT)){data.addUnlockedAbility(HUNT,AbilityUnlock.PROGRESSION);changed=true;}
        }
        Set<AbilityCore<?>> used=new HashSet<>();
        for(IAbility a:equipped)if(a!=null)used.add(a.getCore());
        for(int slot=0;slot<equipped.size();slot++){
            IAbility a=equipped.get(slot);if(a==null || !OLD.contains(a.getCore()))continue;
            changed=true;
            AbilityCore<?> core=replacement(a.getCore());
            if(used.contains(core))data.setEquippedAbility(slot,null);
            else{data.setEquippedAbility(slot,core.createAbility());used.add(core);}
        }
        if(changed)xyz.pixelatedw.mineminenomi.wypi.WyNetwork.sendToAllTrackingAndSelf(
                new xyz.pixelatedw.mineminenomi.packets.server.SSyncAbilityDataPacket(event.getPlayer().getId(),data),event.getPlayer());
    }
    private static AbilityCore<?> replacement(AbilityCore<?> old){
        if(old==InuCerberusPassiveAbility.INSTANCE)return HUNT;
        CerberusAbility.Move move=old==GatesOfHellAbility.INSTANCE?CerberusAbility.Move.GATES:
                old==HellHathNoFuryAbility.INSTANCE?CerberusAbility.Move.FURY:
                old==MaulingAbility.INSTANCE?CerberusAbility.Move.EXECUTION:
                old==HadesCrossAbility.INSTANCE?CerberusAbility.Move.VIGIL:
                old==CerberusRideableAbility.INSTANCE?CerberusAbility.Move.TERRITORY:CerberusAbility.Move.HOWL;
        return CORES.get(move);
    }
    public static boolean legacy(AbilityCore<?> core){return OLD.contains(core);}
}
