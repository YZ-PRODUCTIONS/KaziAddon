package net.kazi.kazimod.ope;

import java.util.*;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.entity.*;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.*;
import net.minecraft.world.World;

public final class WilleClient {
    private static final Map<UUID, Visual> ACTIVE = new LinkedHashMap<>();
    private static World world;
    private static final IRenderTypeBuffer.Impl BUFFER = IRenderTypeBuffer.immediate(new BufferBuilder(16384));
    private static class Visual { WillePacket packet; long received; }
    private static void world() {
        World next = Minecraft.getInstance().level;
        if (world != next) { world = next; ACTIVE.clear(); }
    }
    public static void receive(WillePacket packet) {
        world();
        if (world == null || !world.dimension().location().equals(packet.dimension)) return;
        if (packet.mode == 0) { ACTIVE.remove(packet.uuid); return; }
        if (packet.mode > 2 || !Float.isFinite(packet.progress) || packet.progress < 0 || packet.progress > 1.1F
                || packet.after < -1 || packet.after > 16 || packet.direction.lengthSqr() > 1.1) return;
        if (ACTIVE.size() >= 64 && !ACTIVE.containsKey(packet.uuid)) ACTIVE.remove(ACTIVE.keySet().iterator().next());
        Visual visual = new Visual(); visual.packet = packet; visual.received = world.getGameTime(); ACTIVE.put(packet.uuid, visual);
    }
    public static boolean active(LivingEntity user) {
        world();
        Visual visual = ACTIVE.get(user.getUUID());
        return world == user.level && visual != null && world.getGameTime() - visual.received < 12 && user.isAlive();
    }
    public static void render(MatrixStack stack, ActiveRenderInfo camera, float partial) {
        world(); if (world == null || ACTIVE.isEmpty()) return;
        long now = world.getGameTime();
        ACTIVE.entrySet().removeIf(e -> now - e.getValue().received > 12);
        for (Visual visual : ACTIVE.values()) {
            WillePacket packet = visual.packet;
            Entity entity = world.getEntity(packet.id);
            if (!(entity instanceof LivingEntity) || !entity.getUUID().equals(packet.uuid) || !entity.isAlive()) continue;
            LivingEntity user = (LivingEntity)entity;
            if (user.isInvisible() && user != camera.getEntity() || user.distanceToSqr(camera.getPosition()) > 160 * 160) continue;
            float elapsed = now - visual.received + partial;
            float after = packet.after < 0 ? -1 : packet.after + elapsed;
            if (after >= 16) continue;
            float progress = packet.after < 0 ? Math.min(1, packet.progress + elapsed / (packet.mode == 2 ? 100 : 40)) : 1;
            float retract = after < 7 ? 1 : 1 - MathHelper.clamp((after - 7) / 9, 0, 1);
            float length = 1.2F + (OpeWille.length(packet.mode == 2, progress) - 1.2F) * retract * retract;
            Vector3d grip = OpeWille.grip(user, partial), direction = packet.after < 0 ? user.getViewVector(partial) : packet.direction;
            Vector3d offset = grip.subtract(camera.getPosition());
            stack.pushPose(); stack.translate(offset.x, offset.y, offset.z);
            stack.mulPose(Vector3f.YP.rotation((float)Math.atan2(direction.x, direction.z)));
            stack.mulPose(Vector3f.XP.rotation((float)-Math.asin(MathHelper.clamp(direction.y, -1, 1))));
            WilleSwordGeometry.render(user, stack, BUFFER, length);
            WilleMesh.render(stack, BUFFER, length, (float)(now % 100000) + partial, progress, after, packet.mode == 2,
                    user == camera.getEntity() && !camera.isDetached());
            stack.popPose();
        }
        BUFFER.endBatch();
    }
    private WilleClient() {}
}
