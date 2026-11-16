package net.kazi.kazimod.client.asriel;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.UUID;
import net.kazi.kazimod.init.KaziSounds;
import net.kazi.kazimod.network.ChaosBeamTitlePacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.SimpleSound;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Screen-space cast and finale text, independent of caster tracking and the spectator's camera. */
@Mod.EventBusSubscriber(modid = "kazimod", value = Dist.CLIENT)
public final class ChaosBeamTitleOverlay {
    private static final ResourceLocation FONT = new ResourceLocation("kazimod", "textures/gui/asriel_title.png");
    private static final ChaosBeamTitleVoiceCursor VOICE_CURSOR = new ChaosBeamTitleVoiceCursor();
    private static ISound voice;
    private static ClientWorld world;
    private static LivingEntity viewer;
    private static UUID session;
    private static long start, seed;
    private static String text;
    private static int textWidth;

    private ChaosBeamTitleOverlay() { }

    public static void receive(ChaosBeamTitlePacket packet) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || !mc.level.dimension().location().equals(packet.dimension)) return;
        begin(packet.session, packet.startTick, packet.text);
    }

    /** Start only after this viewer has accepted the finale's existing cutscene packet. */
    public static void beginFinale(UUID caster, long startTick) {
        begin(finaleSession(caster, startTick), startTick, "I CAN STILL SAVE MY FRIENDS");
    }

    public static void endFinale(UUID caster, long startTick) {
        if (finaleSession(caster, startTick).equals(session)) clear();
    }

    private static UUID finaleSession(UUID caster, long startTick) {
        return new UUID(caster.getMostSignificantBits() ^ 0x4348414F5346494EL,
                caster.getLeastSignificantBits() ^ startTick);
    }

    private static void begin(UUID titleSession, long startTick, String titleText) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || !mc.player.isAlive()) return;
        double elapsed = mc.level.getGameTime() - startTick;
        if (elapsed < -20 || elapsed >= ChaosBeamTitleMotion.DURATION_TICKS) return;
        if (world == mc.level && titleSession.equals(session)) return;
        stopVoice();
        world = mc.level;
        viewer = mc.player;
        session = titleSession;
        start = startTick;
        seed = session.getMostSignificantBits() ^ session.getLeastSignificantBits();
        text = titleText;
        textWidth = ChaosBeamTitleFont.width(text);
        VOICE_CURSOR.start(text, elapsed);
    }

    private static double age(float partial) {
        Minecraft mc = Minecraft.getInstance();
        if (world == null) return -1;
        if (mc.level != world || mc.player != viewer || viewer.removed || !viewer.isAlive()) {
            clear();
            return -1;
        }
        double age = world.getGameTime() - start + (mc.isPaused() ? 0 : partial);
        if (age >= ChaosBeamTitleMotion.DURATION_TICKS || age < -20) {
            clear();
            return -1;
        }
        return age;
    }

    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) age(0);
    }

    @SubscribeEvent public static void overlay(RenderGameOverlayEvent.Post event) {
        if (event.getType() == RenderGameOverlayEvent.ElementType.ALL)
            draw(event.getMatrixStack(), event.getWindow().getGuiScaledWidth(),
                    event.getWindow().getGuiScaledHeight(), event.getPartialTicks());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void cinematicOverlay(RenderGameOverlayEvent.Pre event) {
        if (event.getType() == RenderGameOverlayEvent.ElementType.ALL && event.isCanceled())
            draw(event.getMatrixStack(), event.getWindow().getGuiScaledWidth(),
                    event.getWindow().getGuiScaledHeight(), event.getPartialTicks());
    }

    @SubscribeEvent public static void hiddenHud(TickEvent.RenderTickEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (event.phase == TickEvent.Phase.END && mc.options.hideGui && mc.screen == null)
            draw(new MatrixStack(), mc.getWindow().getGuiScaledWidth(),
                    mc.getWindow().getGuiScaledHeight(), event.renderTickTime);
    }

    private static void draw(MatrixStack stack, int width, int height, float partial) {
        double age = age(partial);
        if (!ChaosBeamTitleMotion.active(age)) return;
        int visible = ChaosBeamTitleMotion.visibleCharacters(age, text.length());
        if (visible == 0) return;
        Minecraft mc = Minecraft.getInstance();
        if (!mc.isPaused() && VOICE_CURSOR.advance(text, age)) {
            stopVoice();
            voice = SimpleSound.forUI(KaziSounds.ASRIEL_TEXT_SFX.get(), 1.15F, .9F);
            mc.getSoundManager().play(voice);
        }
        float referenceScale = Math.min(width / 640F, height / 480F);
        float scale = Math.min(referenceScale * 2F, (width - 48 * referenceScale) / Math.max(1, textWidth));
        float x = (width - textWidth * scale) * .5F;
        float y = height * (280F / 480F);
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableAlphaTest();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableTexture();
        RenderSystem.color4f(1, 1, 1, 1);
        mc.getTextureManager().bind(FONT);
        try {
            BufferBuilder buffer = Tessellator.getInstance().getBuilder();
            buffer.begin(7, DefaultVertexFormats.POSITION_COLOR_TEX);
            for (int layer = 0; layer < ChaosBeamTitleMotion.LAYERS; layer++) {
                stack.pushPose();
                try {
                    stack.translate(x + ChaosBeamTitleMotion.offset(age, layer, false, seed) * referenceScale,
                            y + ChaosBeamTitleMotion.offset(age, layer, true, seed) * referenceScale, 0);
                    stack.scale(scale, scale, 1);
                    int cursor = 0;
                    for (int i = 0; i < visible; i++) {
                        char letter = text.charAt(i);
                        int glyph = ChaosBeamTitleFont.index(letter);
                        if (glyph > 0)
                            glyph(buffer, stack.last().pose(), cursor, letter, glyph, ChaosBeamTitleMotion.opacity(layer, age));
                        cursor += ChaosBeamTitleFont.advance(letter);
                    }
                } finally { stack.popPose(); }
            }
            // Every ghost and letter shares one atlas and one draw call.
            Tessellator.getInstance().end();
        } finally {
            RenderSystem.depthMask(true);
            RenderSystem.enableDepthTest();
            RenderSystem.enableAlphaTest();
            RenderSystem.color4f(1, 1, 1, 1);
        }
    }

    private static void glyph(BufferBuilder buffer, Matrix4f pose, int x, char letter, int index, float alpha) {
        int width = ChaosBeamTitleFont.glyphWidth(letter), height = ChaosBeamTitleFont.GLYPH_HEIGHT;
        float u = ChaosBeamTitleFont.u(index) / (float)ChaosBeamTitleFont.TEXTURE_WIDTH;
        float v = ChaosBeamTitleFont.v(index) / (float)ChaosBeamTitleFont.TEXTURE_HEIGHT;
        float u1 = u + width / (float)ChaosBeamTitleFont.TEXTURE_WIDTH;
        float v1 = v + height / (float)ChaosBeamTitleFont.TEXTURE_HEIGHT;
        buffer.vertex(pose, x, height, 0).color(1F, 1F, 1F, alpha).uv(u, v1).endVertex();
        buffer.vertex(pose, x + width, height, 0).color(1F, 1F, 1F, alpha).uv(u1, v1).endVertex();
        buffer.vertex(pose, x + width, 0, 0).color(1F, 1F, 1F, alpha).uv(u1, v).endVertex();
        buffer.vertex(pose, x, 0, 0).color(1F, 1F, 1F, alpha).uv(u, v).endVertex();
    }

    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggedOutEvent event) { clear(); }

    private static void clear() {
        stopVoice();
        VOICE_CURSOR.clear();
        world = null;
        viewer = null;
        session = null;
        text = null;
        textWidth = 0;
    }

    private static void stopVoice() {
        if (voice != null) Minecraft.getInstance().getSoundManager().stop(voice);
        voice = null;
    }
}
