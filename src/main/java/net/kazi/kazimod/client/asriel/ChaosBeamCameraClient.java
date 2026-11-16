package net.kazi.kazimod.client.asriel;

import java.util.LinkedHashSet;
import java.util.Set;
import net.kazi.kazimod.entities.asriel.ChaosBeamAttackEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.util.MovementInput;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.InputUpdateEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Hold the caster's position and view; vanilla still handles perspective, menus and camera effects. */
@Mod.EventBusSubscriber(modid = "kazimod", value = Dist.CLIENT)
public final class ChaosBeamCameraClient {
    private static final Set<ChaosBeamAttackEntity> FIELDS = new LinkedHashSet<>();
    private ChaosBeamCameraClient() { }

    @SubscribeEvent public static void joined(EntityJoinWorldEvent event) {
        // Spawn arrives before synced caster/aim data, so resolve ownership on the following ticks.
        if (event.getWorld().isClientSide && event.getEntity() instanceof ChaosBeamAttackEntity && FIELDS.size() < 96)
            FIELDS.add((ChaosBeamAttackEntity) event.getEntity());
    }

    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        Minecraft mc = Minecraft.getInstance();
        FIELDS.removeIf(field -> field.removed || field.level != mc.level
                || (field.isInitialized() ? !field.hasActiveAimLock() : field.tickCount > 45));
        lockView(mc);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void render(TickEvent.RenderTickEvent event) {
        // Runs after vanilla consumes mouse movement, before the camera interpolates the player.
        if (event.phase == TickEvent.Phase.START) lockView(Minecraft.getInstance());
    }

    private static void lockView(Minecraft mc) {
        ChaosBeamAttackEntity field = ownedField(mc);
        if (field == null) return;
        field.lockMovement(mc.player);
        if (mc.getCameraEntity() == mc.player) field.lockAim(mc.player);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void input(InputUpdateEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (event.getPlayer() != mc.player || ownedField(mc) == null) return;
        MovementInput input = event.getMovementInput();
        input.leftImpulse = input.forwardImpulse = 0;
        input.up = input.down = input.left = input.right = input.jumping = input.shiftKeyDown = false;
        lockView(mc);
    }

    private static ChaosBeamAttackEntity ownedField(Minecraft mc) {
        if (mc.player == null || !mc.player.isAlive() || mc.player.isSpectator()) return null;
        for (ChaosBeamAttackEntity field : FIELDS) {
            if (field.level == mc.level && field.hasActiveAimLock() && field.getCasterId() == mc.player.getId()) return field;
        }
        return null;
    }

    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggedOutEvent event) { FIELDS.clear(); }
}
