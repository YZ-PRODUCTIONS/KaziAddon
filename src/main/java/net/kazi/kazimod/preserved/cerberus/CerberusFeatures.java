package net.kazi.kazimod.preserved.cerberus;

import java.util.*;
import net.MrMagicalCart.cartaddon.abilities.inucerberus.*;
import net.kazi.kazimod.api.KaziRegistry;
import net.minecraft.entity.*;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.*;
import xyz.pixelatedw.mineminenomi.api.abilities.*;
import xyz.pixelatedw.mineminenomi.data.entity.ability.*;
import xyz.pixelatedw.mineminenomi.packets.server.SSyncAbilityDataPacket;
import xyz.pixelatedw.mineminenomi.wypi.WyNetwork;

/** Keeps old save IDs readable, but leaves Cart Addon's Cerberus moveset untouched. */
public final class CerberusFeatures {
    public static final DeferredRegister<EntityType<?>> ENTITIES=DeferredRegister.create(ForgeRegistries.ENTITIES,"kazimod");
    public static final RegistryObject<EntityType<CerberusEffectEntity>> EFFECT=ENTITIES.register("cerberus_hellfire",()->
            EntityType.Builder.<CerberusEffectEntity>of(CerberusEffectEntity::new,EntityClassification.MISC)
                    .sized(2.2F,2.2F).clientTrackingRange(12).updateInterval(1).setShouldReceiveVelocityUpdates(true).noSave().build("cerberus_hellfire"));
    public static final Map<CerberusAbility.Move,AbilityCore<CerberusAbility>> CORES=new EnumMap<>(CerberusAbility.Move.class);
    public static final AbilityCore<CerberusHuntPassive> HUNT=new AbilityCore.Builder<CerberusHuntPassive>(
            "Three-Headed Hunt",AbilityCategory.DEVIL_FRUITS,AbilityType.PASSIVE,CerberusHuntPassive::new).build();

    public static ResourceLocation icon(String name) {
        return new ResourceLocation("kazimod","textures/abilities/cerberus_"+name+".png");
    }

    public static void init(IEventBus bus) {
        ENTITIES.register(bus);
        // Hidden, inert entries allow existing player saves to migrate without missing registry IDs.
        HUNT.setHidden();
        HUNT.setIcon(InuCerberusPassiveAbility.INSTANCE.getIcon());
        KaziRegistry.registerAbility(HUNT);
        for(CerberusAbility.Move move:CerberusAbility.Move.values()) {
            AbilityCore<CerberusAbility> core=new AbilityCore.Builder<CerberusAbility>(
                    move.title,AbilityCategory.DEVIL_FRUITS,c->new CerberusAbility(c,move)).build();
            core.setHidden();
            core.setIcon(original(move).getIcon());
            CORES.put(move,core);
            KaziRegistry.registerAbility(core);
        }
        MinecraftForge.EVENT_BUS.addListener(CerberusFeatures::login);
    }

    private static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if(event.getPlayer().level.isClientSide) return;
        IAbilityData data=AbilityDataCapability.get(event.getPlayer());
        if(data!=null && restoreAbilities(data))
            WyNetwork.sendToAllTrackingAndSelf(new SSyncAbilityDataPacket(event.getPlayer().getId(),data),event.getPlayer());
    }

    static boolean restoreAbilities(IAbilityData data) {
        List<IAbility> equipped=new ArrayList<>(data.getRawEquippedAbilities());
        Map<AbilityCore<?>,AbilityCore<?>> replacements=new LinkedHashMap<>();
        replacements.put(HUNT,InuCerberusPassiveAbility.INSTANCE);
        for(CerberusAbility.Move move:CerberusAbility.Move.values())
            replacements.put(CORES.get(move),original(move));

        boolean changed=false;
        for(Map.Entry<AbilityCore<?>,AbilityCore<?>> entry:replacements.entrySet()) {
            AbilityCore<?> retired=entry.getKey(), original=entry.getValue();
            boolean present=data.hasUnlockedAbility(retired) || data.hasPassiveAbility(retired)
                    || equipped.stream().anyMatch(a->a!=null && a.getCore()==retired);
            if(!present) continue;
            AbilityUnlock unlock=data.getUnlockTypeForAbility(retired);
            if(unlock==null || unlock==AbilityUnlock.NONE) unlock=AbilityUnlock.PROGRESSION;
            // Removing the unlock also clears equipped/passive instances, so retain the slot snapshot above.
            data.removeUnlockedAbility(retired);
            data.removeEquippedAbility(retired);
            data.removePassiveAbility(retired);
            if(!data.hasUnlockedAbility(original)) data.addUnlockedAbility(original,unlock);
            if(original.getType()==AbilityType.PASSIVE && !data.hasPassiveAbility(original))
                data.addPassiveAbility(original.createAbility());
            changed=true;
        }
        if(!changed) return false;

        Set<AbilityCore<?>> used=new HashSet<>();
        for(IAbility ability:data.getEquippedAbilities()) used.add(ability.getCore());
        for(int slot=0;slot<equipped.size();slot++) {
            IAbility previous=equipped.get(slot);
            if(previous==null) continue;
            AbilityCore<?> replacement=replacements.get(previous.getCore());
            if(replacement==null) continue;
            data.setEquippedAbility(slot,null);
            if(replacement.getType()==AbilityType.PASSIVE || !used.add(replacement)) continue;
            data.setEquippedAbility(slot,replacement.createAbility());
        }
        return true;
    }

    static AbilityCore<?> original(CerberusAbility.Move move) {
        switch(move) {
            case FURY: return HellHathNoFuryAbility.INSTANCE;
            case EXECUTION: return MaulingAbility.INSTANCE;
            case VIGIL: return HadesCrossAbility.INSTANCE;
            case TERRITORY: return CerberusRideableAbility.INSTANCE;
            case GATES: return GatesOfHellAbility.INSTANCE;
            // Both replacements came from the head-launch move; restore its actual fruit-menu variant.
            default: return HellHeadMotor2Ability.INSTANCE;
        }
    }

    public static boolean legacy(AbilityCore<?> core) {
        // The old blocker is no longer used, including if an older config still lists its mixin.
        return false;
    }
}
