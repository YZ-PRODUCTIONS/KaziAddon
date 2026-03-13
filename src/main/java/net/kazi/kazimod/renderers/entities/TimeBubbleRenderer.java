package net.kazi.kazimod.renderers.entities;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.kazi.kazimod.entities.TimeBubbleEntity;
import net.kazi.kazimod.models.abilities.TimeBubbleModel;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderState;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.Vector3f;
import org.lwjgl.opengl.GL11;

public class TimeBubbleRenderer extends EntityRenderer<TimeBubbleEntity> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation("kazimod", "textures/models/time_ball.png");

    /**
     * Extends RenderState purely to expose its protected static fields.
     * This is the same pattern vanilla uses internally (e.g. RenderType itself
     * extends RenderState for exactly this reason).
     */
    private static final class States extends RenderState {
        // Constructor is required but never called.
        private States() { super(null, null, null); }

        static final TextureState      TEXTURE_STATE  = new TextureState(TEXTURE, false, false);
        static final TransparencyState TRANSPARENCY   = TRANSLUCENT_TRANSPARENCY;
        static final DepthTestState    DEPTH_TEST     = LEQUAL_DEPTH_TEST;
        static final WriteMaskState    WRITE_MASK     = COLOR_WRITE;   // no depth write
        static final CullState         CULL           = NO_CULL;
        static final LightmapState     LIGHT          = LIGHTMAP;
        static final OverlayState      OVERLAY_STATE  = OVERLAY;
    }

    private static final RenderType BUBBLE_RENDER_TYPE = RenderType.create(
            "kazimod_time_bubble",
            DefaultVertexFormats.NEW_ENTITY,
            GL11.GL_QUADS,
            256,
            RenderType.State.builder()
                    .setTextureState(States.TEXTURE_STATE)
                    .setTransparencyState(States.TRANSPARENCY)
                    .setDepthTestState(States.DEPTH_TEST)
                    .setWriteMaskState(States.WRITE_MASK)
                    .setCullState(States.CULL)
                    .setLightmapState(States.LIGHT)
                    .setOverlayState(States.OVERLAY_STATE)
                    .createCompositeState(false)
    );

    private final TimeBubbleModel<TimeBubbleEntity> model = new TimeBubbleModel<>();

    public TimeBubbleRenderer(EntityRendererManager manager) {
        super(manager);
        this.shadowRadius = 0.0F;
    }

    @Override
    public void render(TimeBubbleEntity entity, float entityYaw, float partialTicks,
                       MatrixStack stack, IRenderTypeBuffer buffer, int packedLight) {

        float scale = entity.getScale();

        stack.pushPose();

        stack.translate(0.0, scale * 0.5, 0.0);

        float targetDiameter = scale + 0.5F;
        float modelDiameter  = 40.0F / 16.0F;
        float s = targetDiameter / modelDiameter;
        stack.scale(s, s, s);

        stack.mulPose(Vector3f.YP.rotationDegrees(entity.tickCount * 1.5F + partialTicks * 1.5F));

        stack.translate(-1.0 / 16.0, 6.0 / 16.0, -1.0 / 16.0);

        model.setupAnim(entity, 0, 0, entity.tickCount + partialTicks, 0, 0);

        IVertexBuilder builder = buffer.getBuffer(BUBBLE_RENDER_TYPE);

        model.renderToBuffer(stack, builder, packedLight,
                OverlayTexture.NO_OVERLAY,
                0.4F, 0.8F, 1.0F, 0.7F);

        stack.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(TimeBubbleEntity entity) {
        return TEXTURE;
    }
}