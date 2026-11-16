package local.autointentdodge;

import net.Zitoen.zitoenpiece.abilities.haki.kenbunshoku.IntentSenseHandler;
import net.Zitoen.zitoenpiece.init.ModAbilities;
import net.Zitoen.zitoenpiece.network.ModNetwork;
import net.Zitoen.zitoenpiece.network.StartIntentSenseDodgePacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.components.CooldownComponent;
import xyz.pixelatedw.mineminenomi.data.entity.ability.AbilityDataCapability;
import xyz.pixelatedw.mineminenomi.data.entity.ability.IAbilityData;
import xyz.pixelatedw.mineminenomi.init.ModAbilityKeys;

@Mod.EventBusSubscriber(modid = AutoIntentDodge.ID, value = Dist.CLIENT)
public final class DodgeEvents {
    private static ClientPlayerEntity trackedPlayer;
    private static int lastHurtTime;
    private static float lastHealth;
    private static int nextRequestTick;

    private static boolean isReady(PlayerEntity player) {
        IAbilityData abilities = AbilityDataCapability.get(player);
        if (abilities == null) {
            return false;
        }
        IAbility sense = abilities.getPassiveAbility(ModAbilities.KENBUNSHOKU_INTENT_SENSE.get());
        return sense != null && !sense.getComponent(ModAbilityKeys.COOLDOWN)
                .map(CooldownComponent::isOnCooldown).orElse(false);
    }

    @SubscribeEvent(priority = EventPriority.NORMAL)
    public static void onAttack(LivingAttackEvent event) {
        if (!(event.getEntityLiving() instanceof ServerPlayerEntity) || !AutoIntentDodge.SINGLEPLAYER.get()) {
            return;
        }
        ServerPlayerEntity player = (ServerPlayerEntity) event.getEntityLiving();
        MinecraftServer server = player.getServer();
        if (server == null || !server.isSingleplayer() || server.isPublished()
                || !server.isSingleplayerOwner(player.getGameProfile()) || !player.isAlive() || player.isSpectator()) {
            return;
        }
        // Zitoen's HIGH listener has already handled an existing stance or passive auto-dodge.
        if (event.getAmount() > 0 && IntentSenseHandler.canDodgeDamageSource(event.getSource())
                && !IntentSenseHandler.isDodgeStanceActive(player) && isReady(player)) {
            IntentSenseHandler.startDodgeStance(player);
            IntentSenseHandler.onLivingAttack(event);
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft game = Minecraft.getInstance();
        ClientPlayerEntity player = game.player;
        MinecraftServer localServer = game.getSingleplayerServer();
        if (player == null || !player.isAlive() || player.isSpectator() || !AutoIntentDodge.MULTIPLAYER.get()
                || (localServer != null && !localServer.isPublished())) {
            trackedPlayer = null;
            return;
        }
        if (player != trackedPlayer) {
            trackedPlayer = player;
            lastHurtTime = player.hurtTime;
            lastHealth = player.getHealth();
            nextRequestTick = player.tickCount;
            return;
        }
        boolean newHit = player.hurtTime > lastHurtTime || (player.hurtTime > 0 && player.getHealth() < lastHealth);
        lastHurtTime = player.hurtTime;
        lastHealth = player.getHealth();
        if (player.tickCount >= nextRequestTick && !IntentSenseHandler.isClientDodgeStanceActive(player.getUUID())
                && ModNetwork.CHANNEL != null && ModNetwork.CHANNEL.isRemotePresent(player.connection.getConnection())
                && isReady(player) && (newHit || AttackPrediction.hasThreat(player))) {
            // A hurt animation and a health update can arrive separately for the same hit.
            nextRequestTick = player.tickCount + 8;
            ModNetwork.sendToServer(new StartIntentSenseDodgePacket());
        }
    }
}
