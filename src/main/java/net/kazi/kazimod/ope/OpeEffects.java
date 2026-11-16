package net.kazi.kazimod.ope;

import java.util.*;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.*;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "kazimod", value = Dist.CLIENT)
public final class OpeEffects {
    private static final Map<UUID, State[]> ACTIVE = new LinkedHashMap<>();
    private static final IRenderTypeBuffer.Impl BUFFERS = IRenderTypeBuffer.immediate(new BufferBuilder(8192));
    private static World world;
    private static int clock;
    private static final class State { int id, born, expiry; }

    private static void world(World current) {
        if (world != current) { world = current; ACTIVE.clear(); clock = 0; }
    }
    public static void receive(World level, OpeVisualPacket packet) {
        world(level);
        if (packet.kind < 0 || packet.kind > 2 || packet.life < 0 || packet.life > 80) return;
        State[] states = ACTIVE.get(packet.uuid);
        if (packet.life == 0) { if (states != null) states[packet.kind] = null; return; }
        if (states == null) {
            if (ACTIVE.size() >= 128) ACTIVE.remove(ACTIVE.keySet().iterator().next());
            states = new State[3]; ACTIVE.put(packet.uuid, states);
        }
        State state = states[packet.kind];
        if (state == null || state.id != packet.id) { state = new State(); state.id = packet.id; state.born = clock; states[packet.kind] = state; }
        state.expiry = clock + packet.life;
    }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance(); world(mc.level);
        if (world == null || mc.isPaused()) return;
        clock++;
        ACTIVE.entrySet().removeIf(entry -> {
            State[] states = entry.getValue(); boolean empty = true;
            for (int i = 0; i < 3; i++) {
                State state = states[i]; if (state == null) continue;
                Entity entity = world.getEntity(state.id);
                if (clock >= state.expiry || entity != null && (!entity.isAlive() || !entity.getUUID().equals(entry.getKey()))) states[i] = null;
                else empty = false;
            }
            return empty;
        });
    }
    public static IRenderTypeBuffer split(LivingEntity entity, MatrixStack stack, IRenderTypeBuffer buffers, float partial) {
        if (world != entity.level || buffers instanceof OpeSplitBuffer || !entity.isAlive()) return buffers;
        State[] states = ACTIVE.get(entity.getUUID());
        State state = states == null ? null : states[OpeVisualPacket.SPLIT];
        if (state == null || clock >= state.expiry) return buffers;
        return new OpeSplitBuffer(buffers, stack.last().pose(), entity.getBbHeight() * .5F,
                MathHelper.rotLerp(partial, entity.yBodyRotO, entity.yBodyRot), amount(state, partial), entity.getBbWidth());
    }
    private static float amount(State state, float partial) {
        return MathHelper.clamp((clock - state.born + partial) / 4, 0, 1)
                * MathHelper.clamp((state.expiry - clock - partial) / 4, 0, 1);
    }
    public static void render(MatrixStack stack, ActiveRenderInfo view, float partial) {
        Minecraft mc = Minecraft.getInstance();
        if (world == null || world != mc.level || ACTIVE.isEmpty()) return;
        Vector3d camera = view.getPosition();
        int drawn = 0;
        for (Map.Entry<UUID, State[]> entry : ACTIVE.entrySet()) for (int kind = 0; kind < 3; kind++) {
            State state = entry.getValue()[kind]; if (state == null) continue;
            Entity entity = world.getEntity(state.id);
            if (!(entity instanceof LivingEntity) || !entity.isAlive() || !entity.getUUID().equals(entry.getKey())) continue;
            boolean firstPerson = entity == view.getEntity() && !view.isDetached();
            if (entity.isInvisible() && !firstPerson || kind == OpeVisualPacket.SPLIT && firstPerson) continue;
            Vector3d position = new Vector3d(MathHelper.lerp(partial, entity.xOld, entity.getX()),
                    MathHelper.lerp(partial, entity.yOld, entity.getY()), MathHelper.lerp(partial, entity.zOld, entity.getZ()));
            double distance = position.distanceToSqr(camera);
            if (distance > 96 * 96 || drawn++ >= 48) continue;
            float age = clock - state.born + partial, alpha = amount(state, partial);
            if (alpha <= 0) continue;
            stack.pushPose(); stack.translate(position.x - camera.x, position.y - camera.y, position.z - camera.z);
            if (kind == OpeVisualPacket.SPLIT) {
                stack.translate(0, entity.getBbHeight() * .5, 0);
                stack.mulPose(Vector3f.YP.rotationDegrees(-MathHelper.rotLerp(partial, ((LivingEntity)entity).yBodyRotO, ((LivingEntity)entity).yBodyRot)));
                OpeMesh.cut(stack, BUFFERS, entity.getBbWidth(), age, alpha);
            } else {
                stack.translate(0, firstPerson ? entity.getEyeHeight() - .12 : entity.getBbHeight() * .72, 0);
                stack.mulPose(Vector3f.YP.rotationDegrees(-MathHelper.rotLerp(partial, entity.yRotO, entity.yRot)));
                stack.mulPose(Vector3f.XP.rotationDegrees(MathHelper.lerp(partial, entity.xRotO, entity.xRot)));
                if (kind == OpeVisualPacket.CURTAIN) {
                    stack.translate(0, -.1, 1.7);
                    OpeMesh.curtain(stack, BUFFERS, age, alpha, distance > 32 * 32, firstPerson);
                } else {
                    stack.translate(0, 0, 2.35);
                    OpeMesh.radio(stack, BUFFERS, age, alpha, distance > 32 * 32);
                }
            }
            stack.popPose();
        }
        BUFFERS.endBatch();
    }
    private OpeEffects() {}
}
