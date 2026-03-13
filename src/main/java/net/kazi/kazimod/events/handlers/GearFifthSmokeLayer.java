package net.kazi.kazimod.events.handlers;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.kazi.kazimod.abilities.GomuRework.GearFifthRework;
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

public class GearFifthSmokeLayer<T extends LivingEntity, M extends EntityModel<T>>
        extends LayerRenderer<T, M> {

    private static final ResourceLocation[] SMOKE_ANIM = new ResourceLocation[]{
            new ResourceLocation("mineminenomi", "textures/models/zoanmorph/g5/smoke_0.png"),
            new ResourceLocation("mineminenomi", "textures/models/zoanmorph/g5/smoke_1.png"),
            new ResourceLocation("mineminenomi", "textures/models/zoanmorph/g5/smoke_2.png"),
            new ResourceLocation("mineminenomi", "textures/models/zoanmorph/g5/smoke_3.png")
    };

    private static final float EXTRA_SCALE_MULTIPLIER = 1.2F;

    private final GomuSmokeModel model = new GomuSmokeModel();

    public GearFifthSmokeLayer(IEntityRenderer<T, M> renderer) {
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

        GearFifthRework gearFifth = (GearFifthRework) abilityData.getEquippedAbility(GearFifthRework.INSTANCE);
        if (gearFifth == null) return;

        ContinuousComponent cont = gearFifth.getContinuousComponent();
        if (cont == null || cont.getContinueTime() <= 0) return;

        // Animate through smoke frames
        float speed = 1000.0F;
        float anim = (Util.getMillis() % speed) / (speed / (float) SMOKE_ANIM.length);
        int frame = (int) Math.floor(anim);

        matrixStack.pushPose();

        float dynamicScale = 1.0F;
        float height = 1.8F;

        float horizontalScale = dynamicScale * 1.20F;
        float verticalScale = dynamicScale;

        matrixStack.translate(0.0D, height * -0.08D, 0.0D);

        matrixStack.scale(
                horizontalScale * EXTRA_SCALE_MULTIPLIER,
                verticalScale * EXTRA_SCALE_MULTIPLIER,
                horizontalScale * EXTRA_SCALE_MULTIPLIER
        );

        IVertexBuilder builder =
                buffer.getBuffer(RenderType.entityTranslucent(SMOKE_ANIM[frame]));

        float time = (entity.tickCount + partialTicks) * 0.08F;
        float offset = frame * 0.8F;
        float wave = (float) Math.sin(time + offset);
        float normalized = wave * 0.5F + 0.5F;

        // NEW
        float pulse = 1.0F + (float) Math.sin(time) * 0.04F;
        matrixStack.scale(pulse, pulse, pulse);

        model.setupAnim(entity, 0, 0, 0, 0, 0);

// White: R, G, B all at 1.0F; slight alpha pulse for a soft glow effect
        float alpha = 0.75F + (0.15F * normalized);
        model.renderToBuffer(
                matrixStack, builder, packedLight, OverlayTexture.NO_OVERLAY,
                1.0F, 1.0F, 1.0F, alpha
        );
        matrixStack.popPose();
    }
}