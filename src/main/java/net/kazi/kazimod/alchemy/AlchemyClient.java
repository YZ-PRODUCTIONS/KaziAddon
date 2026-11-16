package net.kazi.kazimod.alchemy;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.AbstractClientPlayerEntity;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.culling.ClippingHelper;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.renderer.entity.PlayerRenderer;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.client.renderer.entity.model.PlayerModel;
import net.minecraft.client.renderer.model.ModelRenderer;
import net.minecraft.client.settings.ParticleStatus;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

public final class AlchemyClient {
    private static final IRenderTypeBuffer.Impl STONE_BUFFER = IRenderTypeBuffer.immediate(new BufferBuilder(262144));
    private static final IRenderTypeBuffer.Impl ENERGY_BUFFER = IRenderTypeBuffer.immediate(new BufferBuilder(262144));
    private static final Map<UUID, AlchemyMesh.SurfaceCache> SURFACES = new HashMap<>();
    private static final Map<Integer, Pending> PENDING = new LinkedHashMap<>();
    private static World world;
    private static boolean initialized;
    private static int handDepth;

    public static void init(IEventBus bus) {
        if (initialized) return;
        initialized = true;
        bus.addListener((FMLClientSetupEvent event) -> {
            RenderingRegistry.registerEntityRenderingHandler(AlchemyFeatures.FIELD.get(), FieldRenderer::new);
            event.enqueueWork(() -> Minecraft.getInstance().getEntityRenderDispatcher().getSkinMap().values()
                    .forEach(renderer -> renderer.addLayer(new BladeLayer(renderer))));
        });
        MinecraftForge.EVENT_BUS.addListener(AlchemyClient::tick);
    }

    private static void world() {
        World next = Minecraft.getInstance().level;
        if (next == world) return;
        world = next; SURFACES.clear(); PENDING.clear(); handDepth = 0;
    }

    public static void receive(AlchemyPacket packet) {
        world();
        if (world == null || packet == null || packet.data == null) return;
        Entity entity = world.getEntity(packet.id);
        if (entity instanceof LivingEntity) apply((LivingEntity)entity, packet.data);
        else {
            if (PENDING.size() >= 64) PENDING.remove(PENDING.keySet().iterator().next());
            PENDING.put(packet.id, new Pending(packet.data.copy(), world.getGameTime() + 40));
        }
    }

    private static void apply(LivingEntity entity, CompoundNBT data) {
        entity.getPersistentData().put(AlchemyFeatures.TAG, data.copy());
    }

    private static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        world();
        if (world == null || Minecraft.getInstance().isPaused()) return;
        Iterator<Map.Entry<Integer, Pending>> it = PENDING.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, Pending> entry = it.next();
            Entity entity = world.getEntity(entry.getKey());
            if (entity instanceof LivingEntity) { apply((LivingEntity)entity, entry.getValue().data); it.remove(); }
            else if (world.getGameTime() >= entry.getValue().expires) it.remove();
        }
        if (world.getGameTime() % 20 == 0) {
            java.util.Set<UUID> alive = new java.util.HashSet<>();
            for (Entity entity : Minecraft.getInstance().level.entitiesForRendering())
                if (entity instanceof AlchemyField && entity.isAlive()) alive.add(entity.getUUID());
            SURFACES.keySet().retainAll(alive);
        }
    }

    public static boolean pose(PlayerModel<?> model, LivingEntity player, float time) {
        return AlchemyPose.apply(model, player, time);
    }

    static boolean slim(LivingEntity player) {
        return player instanceof AbstractClientPlayerEntity && "slim".equals(((AbstractClientPlayerEntity)player).getModelName());
    }

    public static boolean renderingHand() { return handDepth > 0; }
    public static void beginHand() { handDepth++; }
    public static void endHand() { handDepth = Math.max(0, handDepth - 1); }

    public static void hand(PlayerRenderer renderer, MatrixStack stack, IRenderTypeBuffer buffers, int light,
                            AbstractClientPlayerEntity player, ModelRenderer arm) {
        if (arm != renderer.getModel().rightArm) return;
        AlchemyBlade.render(stack, buffers, light, player, arm, Minecraft.getInstance().getFrameTime());
    }

    /** Called after both particle paths and before Fabulous's transparency composite. */
    public static void render(MatrixStack stack, ActiveRenderInfo camera, float partial, Matrix4f projection) {
        world();
        Minecraft mc = Minecraft.getInstance();
        if (world == null) return;
        Vector3d eye = camera.getPosition();
        ClippingHelper frustum = new ClippingHelper(stack.last().pose(), projection);
        frustum.prepare(eye.x, eye.y, eye.z);
        List<AlchemyField> fields = new ArrayList<>();
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (!(entity instanceof AlchemyField) || !entity.isAlive()) continue;
            AlchemyField field = (AlchemyField)entity;
            if (field.kind() < 0 || field.kind() > 5 || field.age(partial) >= field.life()
                    || field.distanceToSqr(eye) > 112 * 112 || !frustum.isVisible(field.getBoundingBoxForCulling())) continue;
            fields.add(field);
            if (fields.size() == 128) break;
        }
        if (fields.isEmpty()) return;
        fields.sort(Comparator.comparingDouble(field -> field.distanceToSqr(eye)));
        int quality = mc.options.particles == ParticleStatus.MINIMAL ? 0 : mc.options.particles == ParticleStatus.DECREASED ? 1 : 2;
        int maximum = quality == 0 ? 8 : quality == 1 ? 12 : 16;
        AlchemyMesh.Budget budget = new AlchemyMesh.Budget(quality == 0 ? 18000 : 52000,
                quality == 0 ? 16000 : 46000, quality == 0 ? 160 : 384);
        // Separate builders prevent a texture/state switch invalidating a retained vertex builder.
        IVertexBuilder stone = STONE_BUFFER.getBuffer(AlchemyMesh.STONE);
        IVertexBuilder energy = ENERGY_BUFFER.getBuffer(AlchemyMesh.ENERGY);
        for (int i = 0; i < Math.min(maximum, fields.size()); i++) {
            AlchemyField field = fields.get(i);
            double distance = Math.sqrt(field.distanceToSqr(eye));
            float fade = 1 - AlchemyMesh.smooth((float)((distance - 88) / 24));
            int detail = distance > 56 || i >= 6 ? 0 : distance > 28 ? Math.min(quality, 1) : quality;
            if (SURFACES.size() >= 128 && !SURFACES.containsKey(field.getUUID())) SURFACES.clear();
            AlchemyMesh.SurfaceCache surface = SURFACES.computeIfAbsent(field.getUUID(), key -> new AlchemyMesh.SurfaceCache());
            Vector3d pos = field.getPosition(partial).subtract(eye);
            stack.pushPose();
            try {
                stack.translate(pos.x, pos.y, pos.z);
                AlchemyMesh.render(field, partial, stack, stone, energy, budget, surface, detail, fade);
            } finally { stack.popPose(); }
        }
        // MAIN_TARGET itself is a no-op state in 1.16; explicitly bind the framebuffer.
        mc.getMainRenderTarget().bindWrite(false);
        STONE_BUFFER.endBatch();
        ENERGY_BUFFER.endBatch();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.enableTexture();
        RenderSystem.defaultBlendFunc();
        RenderSystem.color4f(1, 1, 1, 1);
    }

    private static final class Pending {
        final CompoundNBT data; final long expires;
        Pending(CompoundNBT data, long expires) { this.data = data; this.expires = expires; }
    }

    private static final class BladeLayer extends LayerRenderer<AbstractClientPlayerEntity, PlayerModel<AbstractClientPlayerEntity>> {
        BladeLayer(PlayerRenderer renderer) { super(renderer); }
        @Override public void render(MatrixStack stack, IRenderTypeBuffer buffers, int light, AbstractClientPlayerEntity player,
                                     float walk, float amount, float partial, float time, float yaw, float pitch) {
            AlchemyBlade.render(stack, buffers, light, player, getParentModel().rightArm, partial);
        }
    }

    private static final class FieldRenderer extends EntityRenderer<AlchemyField> {
        FieldRenderer(EntityRendererManager manager) { super(manager); shadowRadius = 0; }
        @Override public ResourceLocation getTextureLocation(AlchemyField field) {
            return new ResourceLocation("minecraft", "textures/block/stone.png");
        }
        @Override public void render(AlchemyField field, float yaw, float partial, MatrixStack stack,
                                     IRenderTypeBuffer buffers, int light) {
            // The bounded world pass owns geometry, including effects outside the entity's small origin.
        }
    }

    private AlchemyClient() {}
}
