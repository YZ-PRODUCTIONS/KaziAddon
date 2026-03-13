package net.kazi.kazimod.events.handlers;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.kazi.kazimod.abilities.GomuRework.GomuGomuNoGigantRework;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.IEntityRenderer;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.client.renderer.entity.model.EntityModel;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Util;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ContinuousComponent;
import xyz.pixelatedw.mineminenomi.data.entity.ability.AbilityDataCapability;
import xyz.pixelatedw.mineminenomi.data.entity.ability.IAbilityData;
import xyz.pixelatedw.mineminenomi.models.abilities.GomuSmokeModel;

public class GigantSmokeLayer<T extends LivingEntity, M extends EntityModel<T>>
        extends LayerRenderer<T, M> {

    private static final ResourceLocation[] SMOKE_ANIM = new ResourceLocation[]{
            new ResourceLocation("mineminenomi", "textures/models/zoanmorph/g5/smoke_0.png"),
            new ResourceLocation("mineminenomi", "textures/models/zoanmorph/g5/smoke_1.png"),
            new ResourceLocation("mineminenomi", "textures/models/zoanmorph/g5/smoke_2.png"),
            new ResourceLocation("mineminenomi", "textures/models/zoanmorph/g5/smoke_3.png")
    };

    // MegaRenderer applies 4.5x scale globally. The smoke model is designed for
    // a 1x player. We undo the renderer's scale (1/4.5) then apply the desired
    // visual size. A value of ~1.0 gives smoke that fits the giant body correctly.
    private static final float MEGA_RENDERER_SCALE = 4.5F;
    private static final float DESIRED_SMOKE_SCALE = 1.0F;
    private static final float SCALE_CORRECTION = DESIRED_SMOKE_SCALE / MEGA_RENDERER_SCALE;
    private static final float EXTRA_SCALE_MULTIPLIER = 1.2F;

    private final GomuSmokeModel model = new GomuSmokeModel();

    public GigantSmokeLayer(IEntityRenderer<T, M> renderer) {
        super(renderer);
    }

    @Override
    public void render(MatrixStack matrixStack,
                       IRenderTypeBuffer buffer,
                       int packedLight,
                       T entity,
                       float limbSwing,
                       float limbSwingAmount,
                       float partialTicks,
                       float ageInTicks,
                       float netHeadYaw,
                       float headPitch) {

        if (!entity.isAlive()) return;

        IAbilityData abilityData = AbilityDataCapability.get(entity);
        if (abilityData == null) return;

        GomuGomuNoGigantRework gigant = (GomuGomuNoGigantRework) abilityData.getEquippedAbility(GomuGomuNoGigantRework.INSTANCE);
        if (gigant == null) return;

        ContinuousComponent cont = gigant.getContinuousComponent();
        if (cont == null || cont.getContinueTime() <= 0) return;

        // Animate through smoke frames
        float speed = 1000.0F;
        float anim = (Util.getMillis() % speed) / (speed / (float) SMOKE_ANIM.length);
        int frame = (int) Math.floor(anim);

        matrixStack.pushPose();

        float height = 1.8F;

        // Undo the renderer's 4.5x scale so we control absolute size ourselves
        float baseScale = SCALE_CORRECTION * EXTRA_SCALE_MULTIPLIER;
        float horizontalScale = baseScale * 1.20F;
        float verticalScale = baseScale;

        matrixStack.translate(0.0D, height * -0.08D, 0.0D);
        matrixStack.scale(horizontalScale, verticalScale, horizontalScale);

        IVertexBuilder builder =
                buffer.getBuffer(RenderType.entityTranslucent(SMOKE_ANIM[frame]));

        float time = (entity.tickCount + partialTicks) * 0.08F;
        float offset = frame * 0.8F;
        float wave = (float) Math.sin(time + offset);
        float normalized = wave * 0.5F + 0.5F;

        float pulse = 1.0F + (float) Math.sin(time) * 0.04F;
        matrixStack.scale(pulse, pulse, pulse);

        model.setupAnim(entity, 0, 0, 0, 0, 0);

        float alpha = 0.75F + (0.15F * normalized);
        model.renderToBuffer(
                matrixStack, builder, packedLight, OverlayTexture.NO_OVERLAY,
                1.0F, 1.0F, 1.0F, alpha
        );
        matrixStack.popPose();
    }
}