package net.kazi.kazimod.client.nagi;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.kazi.kazimod.abilities.NagiAwakening.IllusionShatterAbility;
import net.kazi.kazimod.abilities.NagiAwakening.IllusionShatterHealth;
import net.kazi.kazimod.abilities.NagiAwakening.NagiAwakeningSupport;
import net.kazi.kazimod.init.KaziEffects;
import net.minecraft.client.entity.player.AbstractClientPlayerEntity;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderState;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.model.PlayerModel;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.math.vector.Matrix4f;
import xyz.pixelatedw.mineminenomi.api.abilities.components.DisableComponent;
import xyz.pixelatedw.mineminenomi.data.entity.ability.AbilityDataCapability;
import xyz.pixelatedw.mineminenomi.data.entity.ability.IAbilityData;
import xyz.pixelatedw.mineminenomi.init.ModAbilityKeys;
import xyz.pixelatedw.mineminenomi.init.ModEffects;

/** Shared arm-light geometry, now driven by Illusion Shatter's synchronized passive state. */
public final class ImagineBreakerAuraRenderer extends RenderState {
    private static final RenderType GLOW = RenderType.create("kazimod_imagine_breaker_aura",
            DefaultVertexFormats.POSITION_COLOR, 7, 32768, false, false,
            RenderType.State.builder().setTransparencyState(LIGHTNING_TRANSPARENCY)
                    .setCullState(NO_CULL).setWriteMaskState(COLOR_WRITE)
                    .setDepthTestState(LEQUAL_DEPTH_TEST).setShadeModelState(SMOOTH_SHADE)
                    .createCompositeState(false));

    private ImagineBreakerAuraRenderer() { super(null, null, null); }

    public static boolean isActive(AbstractClientPlayerEntity player) {
        if (!player.isAlive() || player.isSpectator() || player.isInvisible()
                || !IllusionShatterHealth.active(player.getHealth(), player.getMaxHealth())
                || player.hasEffect(ModEffects.NO_HANDS.get()) || !NagiAwakeningSupport.canUnlock(player)
                || player.hasEffect(KaziEffects.IMAGINE_BROKEN.get())
                || player.hasEffect(KaziEffects.LOST_WORKS_FRUIT_LOCK.get())) return false;
        IAbilityData data = AbilityDataCapability.get(player);
        if (data == null) return false;
        IllusionShatterAbility passive = data.getPassiveAbility(IllusionShatterAbility.INSTANCE);
        // Keep the aura visible while the passive can function, including when armed.
        return passive != null && !passive.isPaused()
                && !passive.getComponent(ModAbilityKeys.DISABLE).map(DisableComponent::isDisabled).orElse(false);
    }

    public static void render(MatrixStack stack, IRenderTypeBuffer buffers,
                              PlayerModel<AbstractClientPlayerEntity> model, AbstractClientPlayerEntity player,
                              float partialTick, int detail, boolean firstPerson) {
        stack.pushPose();
        try {
            // Use the posed limb, including attack, crouch and MMNM pointing animations.
            model.rightArm.translateAndRotate(stack);
            stack.translate("slim".equals(player.getModelName()) ? -.03125 : -.0625, 0, 0);
            Matrix4f pose = stack.last().pose();
            IVertexBuilder vertices = buffers.getBuffer(GLOW);
            ImagineBreakerAuraMesh.render((x, y, z, rgb, alpha) -> vertices.vertex(pose, x, y, z)
                            .color(rgb >> 16 & 255, rgb >> 8 & 255, rgb & 255, (int) (alpha * 255)).endVertex(),
                    player.tickCount + partialTick + (player.getId() & 255) * .37F,
                    detail, firstPerson ? .7F : 1.0F);
        } finally {
            stack.popPose();
        }
    }
}
