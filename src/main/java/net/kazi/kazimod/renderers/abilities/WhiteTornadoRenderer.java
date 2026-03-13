package net.kazi.kazimod.renderers.abilities;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.kazi.kazimod.entities.WhiteTornadoEntity;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.renderer.entity.model.EntityModel;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.Vector3f;
import net.minecraftforge.fml.client.registry.IRenderFactory;
import xyz.pixelatedw.mineminenomi.entities.TornadoEntity;
import xyz.pixelatedw.mineminenomi.models.abilities.TornadoModel;
import xyz.pixelatedw.mineminenomi.renderers.abilities.TornadoRenderer;

@SuppressWarnings({"unchecked", "rawtypes"})
public class WhiteTornadoRenderer extends TornadoRenderer {

    private static final ResourceLocation[] TEXTURES = new ResourceLocation[]{
            new ResourceLocation("kazimod", "textures/models/projectiles/windtornado1.png"),
            new ResourceLocation("kazimod", "textures/models/projectiles/windtornado2.png"),
            new ResourceLocation("kazimod", "textures/models/projectiles/windtornado3.png")
    };

    private final TornadoModel model = new TornadoModel();

    public WhiteTornadoRenderer(EntityRendererManager manager) {
        super(manager);
    }

    public void render(TornadoEntity entity, float entityYaw, float partialTicks, MatrixStack matrixStack, IRenderTypeBuffer buffer, int packedLight) {
        float scale    = entity.getSize();
        float ageInTicks = entity.tickCount + partialTicks;
        float rotAmount  = ageInTicks * 8.0F;
        float spread     = Math.min(entity.getSpeed(), 1.0F);

        // Read color from entity — defaults to white if not a WhiteTornadoEntity
        float r = 1.0F, g = 1.0F, b = 1.0F;
        if (entity instanceof WhiteTornadoEntity) {
            WhiteTornadoEntity wte = (WhiteTornadoEntity) entity;
            r = wte.getRed();
            g = wte.getGreen();
            b = wte.getBlue();
        }

        matrixStack.pushPose();
        matrixStack.scale(scale * 1.5F, scale * 1.5F, scale * 1.5F);
        matrixStack.mulPose(Vector3f.XP.rotationDegrees(180.0F));
        matrixStack.translate(0.0, -1.5, 0.0);

        for (int i = 0; i < 3; i++) {
            matrixStack.pushPose();
            matrixStack.mulPose(Vector3f.YP.rotationDegrees(rotAmount + (i * 45.0F)));

            RenderType renderType = RenderType.entityTranslucent(TEXTURES[i]);
            IVertexBuilder vertexBuilder = buffer.getBuffer(renderType);
            model.setupAnim(entity, ageInTicks, spread, ageInTicks, 0.0F, 0.0F);
            ((EntityModel) model).renderToBuffer(matrixStack, vertexBuilder, packedLight,
                    OverlayTexture.NO_OVERLAY,
                    r, g, b, 0.35F);

            matrixStack.popPose();
        }

        matrixStack.popPose();
    }

    public void func_225623_a_(Entity entityIn, float entityYaw, float partialTicks, MatrixStack matrixStack, IRenderTypeBuffer buffer, int packedLight) {
        if (entityIn instanceof TornadoEntity) {
            this.render((TornadoEntity) entityIn, entityYaw, partialTicks, matrixStack, buffer, packedLight);
        }
    }

    public ResourceLocation func_110775_a(Entity entity) {
        return TEXTURES[0];
    }

    public static class Factory implements IRenderFactory<WhiteTornadoEntity> {
        @Override
        public WhiteTornadoRenderer createRenderFor(EntityRendererManager manager) {
            return new WhiteTornadoRenderer(manager);
        }
    }
}