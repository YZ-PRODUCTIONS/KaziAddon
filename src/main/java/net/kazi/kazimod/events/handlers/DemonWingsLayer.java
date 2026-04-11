package net.kazi.kazimod.events.handlers;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.kazi.kazimod.abilities.AkumaRework.DemonTransformationAbility;
import net.kazi.kazimod.client.models.DemonCloakModel;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.IEntityRenderer;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.client.renderer.entity.model.EntityModel;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.ResourceLocation;
import xyz.pixelatedw.mineminenomi.data.entity.ability.AbilityDataCapability;
import xyz.pixelatedw.mineminenomi.data.entity.ability.IAbilityData;

public class DemonWingsLayer<T extends LivingEntity, M extends EntityModel<T>>
        extends LayerRenderer<T, M> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation("kazimod", "textures/models/demon_cloak.png");

    private final DemonCloakModel<T> model = new DemonCloakModel<>();

    public DemonWingsLayer(IEntityRenderer<T, M> renderer) {
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

        DemonTransformationAbility demonForm =
                (DemonTransformationAbility) abilityData.getEquippedAbility(DemonTransformationAbility.INSTANCE);
        if (demonForm == null || !demonForm.isContinuous()) return;

        IVertexBuilder builder = buffer.getBuffer(RenderType.entityTranslucent(TEXTURE));

        model.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
        model.renderToBuffer(matrixStack, builder, packedLight, OverlayTexture.NO_OVERLAY,
                1.0F, 1.0F, 1.0F, 1.0F);
    }
}
