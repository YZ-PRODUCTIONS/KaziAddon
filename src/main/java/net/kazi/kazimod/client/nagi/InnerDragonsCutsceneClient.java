package net.kazi.kazimod.client.nagi;

import com.mojang.authlib.GameProfile;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.kazi.kazimod.abilities.cinematics.AwakeningCutsceneType;
import net.kazi.kazimod.abilities.cinematics.ChaosBeamFinaleLift;
import net.kazi.kazimod.abilities.NagiAwakening.InnerDragonsSequence;
import net.kazi.kazimod.client.asriel.ChaosBeamFinale;
import net.kazi.kazimod.client.asriel.ChaosBeamTitleOverlay;
import net.kazi.kazimod.network.InnerDragonsCutscenePacket;
import net.kazi.kazimod.jjk.ClashStage;
import net.kazi.kazimod.petal.PetalField;
import net.kazi.kazimod.petal.PetalState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SimpleSound;
import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.TickableSound;
import net.minecraft.util.SoundCategory;
import net.kazi.kazimod.init.KaziSounds;
import net.minecraft.client.entity.player.RemoteClientPlayerEntity;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.network.play.NetworkPlayerInfo;
import net.minecraft.client.settings.PointOfView;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntitySize;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MoverType;
import net.minecraft.entity.Pose;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.IPacket;
import net.minecraft.util.MovementInput;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceContext;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.EntityViewRenderEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.InputUpdateEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.client.event.RenderBlockOverlayEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Local cinematic camera and caster ascent prediction, without game-mode or persistent settings changes. */
@Mod.EventBusSubscriber(modid = "kazimod", value = Dist.CLIENT)
public final class InnerDragonsCutsceneClient {
    private static Scene scene;

    private InnerDragonsCutsceneClient() { }

    public static void receive(InnerDragonsCutscenePacket packet) {
        Minecraft mc = Minecraft.getInstance();
        if (packet.cancel) {
            if (scene != null && scene.packet.casterUuid.equals(packet.casterUuid)
                    && scene.packet.startTick == packet.startTick && scene.packet.type == packet.type
                    && (packet.release == null || packet.release.fieldId == scene.packet.release.fieldId)) stop();
            return;
        }
        if (mc.level == null || mc.player == null || !mc.player.isAlive()
                || !finite(packet.origin) || !Float.isFinite(packet.yaw)) return;
        if (packet.clash != null && (!finite(packet.clash.origin)
                || packet.casterUuid.equals(packet.clash.uuid))) return;
        long elapsed = mc.level.getGameTime() - packet.startTick;
        if (elapsed < -20 || elapsed >= packet.type.durationTicks) return;
        // Simultaneous awakenings must not repeatedly seize the camera from one another.
        if (scene != null) {
            if (active()) return;
            stop();
        }
        ChaosBeamFinale.clearReturnFade();
        scene = new Scene(mc, packet);
        mc.options.setCameraType(PointOfView.FIRST_PERSON);
        mc.setCameraEntity(scene.camera);
        positionCamera(mc.getFrameTime());
        if (packet.type == AwakeningCutsceneType.CHAOS_BEAM_FINALE)
            ChaosBeamTitleOverlay.beginFinale(packet.casterUuid, packet.startTick);
    }

    public static boolean active() {
        Minecraft mc = Minecraft.getInstance();
        return scene != null && mc.level == scene.world && mc.player == scene.viewer
                && mc.player != null
                && mc.player.isAlive() && mc.getCameraEntity() == scene.camera
                && scene.packet.type.active(age(0));
    }

    public static float age(float partial) {
        if (scene == null) return 0;
        return Math.max(0, scene.world.getGameTime() - scene.packet.startTick + partial);
    }

    public static UUID subjectUuid() { return scene == null ? null : scene.packet.casterUuid; }

    public static LivingEntity subject() {
        if (scene == null) return null;
        return tracked(scene.packet.casterId, scene.packet.casterUuid, scene.proxy);
    }

    private static LivingEntity tracked(int id, UUID uuid, LivingEntity fallback) {
        if (scene.viewer.getUUID().equals(uuid)) return scene.viewer;
        Entity candidate = scene.world.getEntity(id);
        if (candidate instanceof LivingEntity && candidate.getUUID().equals(uuid))
            return (LivingEntity) candidate;
        for (LivingEntity player : scene.world.players())
            if (player.getUUID().equals(uuid)) return player;
        return fallback;
    }

    public static ClashStage clashStage() { return isDomainClash() ? scene.clashStage : null; }
    public static LivingEntity clashOpponent() {
        if (!isDomainClash()) return null;
        InnerDragonsCutscenePacket.ClashPartner b = scene.packet.clash;
        return tracked(b.id, b.uuid, scene.partnerProxy);
    }
    public static boolean clashPlayer(UUID who) {
        return isDomainClash() && (scene.packet.casterUuid.equals(who) || scene.packet.clash.uuid.equals(who));
    }

    /** The visual renderer draws this skin-backed stand-in if the caster is outside entity tracking. */
    public static boolean fallbackSubject() { return scene != null && subject() == scene.proxy; }

    public static Vector3d origin(float partial) {
        LivingEntity actor = subject();
        if (actor == null) return Vector3d.ZERO;
        return new Vector3d(MathHelper.lerp(partial, actor.xOld, actor.getX()),
                MathHelper.lerp(partial, actor.yOld, actor.getY()),
                MathHelper.lerp(partial, actor.zOld, actor.getZ()));
    }

    public static boolean isGoru() { return active() && scene.packet.type == AwakeningCutsceneType.EA_DRAW; }

    public static boolean isGoat() { return active() && scene.packet.type == AwakeningCutsceneType.HYPER_GONER; }
    public static boolean isChaosFinale() { return active() && scene.packet.type == AwakeningCutsceneType.CHAOS_BEAM_FINALE; }
    public static boolean isNagi() { return active() && scene.packet.type == AwakeningCutsceneType.INNER_DRAGONS; }
    public static boolean isDomainClash() { return active() && scene.packet.type == AwakeningCutsceneType.DOMAIN_CLASH; }
    public static boolean isKageyoshi() { return active() && scene.packet.release != null; }
    public static boolean isInstinct() { return active() && scene.packet.type == AwakeningCutsceneType.ULTRA_INSTINCT; }
    public static boolean isInstinctCaster(Entity actor) {
        return scene != null && scene.packet.type == AwakeningCutsceneType.ULTRA_INSTINCT
                && scene.packet.casterUuid.equals(actor.getUUID()) && active();
    }
    public static boolean isKageyoshiCaster(Entity actor) {
        return isKageyoshi() && scene.packet.casterUuid.equals(actor.getUUID());
    }
    static float castYaw() { return scene.packet.yaw; }
    public static boolean stagesRelease(PetalField field) {
        return isKageyoshi() && field.level == scene.world && field.getId() == scene.packet.release.fieldId;
    }
    static PetalField releaseForShot() {
        Entity tracked = scene.world.getEntity(scene.packet.release.fieldId);
        if (tracked instanceof PetalField && !tracked.removed && ((PetalField) tracked).kind() == 6) return (PetalField)tracked;
        if (scene.preview == null) scene.preview = PetalField.releasePreview(scene.world, scene.packet.casterId,
                scene.packet.startTick, scene.packet.origin, scene.packet.yaw);
        return scene.preview;
    }
    public static boolean activeAt(float age) { return scene != null && scene.packet.type.active(age); }

    public static float yaw(float partial) {
        LivingEntity actor = subject();
        return actor == null ? 0 : MathHelper.rotLerp(partial, actor.yBodyRotO, actor.yBodyRot);
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (scene == null) return;
        LivingEntity actor = subject();
        if (!active() || actor == null || actor.removed || !actor.isAlive()) {
            stop(actor != null && !actor.removed && actor.isAlive() && age(0) >= scene.packet.type.durationTicks);
            return;
        }
        if (event.phase == TickEvent.Phase.START) {
            if (scene.packet.type.cameraOnly()) {
                boolean down = Minecraft.getInstance().options.keyShift.isDown();
                boolean dismiss = down && !scene.shiftHeld;
                scene.shiftHeld = down;
                if (dismiss) { stop(); return; }
            }
            scene.beginLiftTick();
            return;
        }
        float now = age(0);
        try {
            if (isChaosFinale() && !Minecraft.getInstance().isPaused()) {
                if (scene.isLocalCaster())
                    ChaosBeamFinaleLift.advance(scene.viewer, scene.packet.origin, now);
                scene.updateLiftProxy(now);
            }
        } finally {
            scene.restoreLiftGravity();
        }
        if (isChaosFinale()) {
            if (scene.lastSoundAge < 0) {
                SimpleSound audio = SimpleSound.forUI(KaziSounds.CHAOS_BEAM_FINALE_SFX.get(), 1F, 1F);
                scene.sounds.add(audio);
                Minecraft.getInstance().getSoundManager().play(audio);
            }
        } else if (isInstinct()) {
            // Punctuate the release without restarting the existing form-owned theme.
            cue(scene.lastSoundAge, now, UltraInstinctPresentation.REVEAL, SoundEvents.GENERIC_EXPLODE, .43F, .67F);
            cue(scene.lastSoundAge, now, UltraInstinctPresentation.REVEAL + 2, SoundEvents.BEACON_POWER_SELECT, .29F, 1.45F);
        } else if (isDomainClash()) {
            cue(scene.lastSoundAge, now, 0, SoundEvents.BEACON_ACTIVATE, .55F, .66F);
            cue(scene.lastSoundAge, now, 48, SoundEvents.RESPAWN_ANCHOR_CHARGE, .78F, .64F);
            cue(scene.lastSoundAge, now, 54, SoundEvents.GENERIC_EXPLODE, .50F, .62F);
            cue(scene.lastSoundAge, now, 116, SoundEvents.GLASS_BREAK, .38F, .83F);
        } else if (isGoat()) {
            // Original Hyper Goner laugh/charge and the original return-from-whiteout cue.
            cue(scene.lastSoundAge, now, 12, KaziSounds.HYPER_GONER_LAUGH_SFX.get(), .8F, 1F);
            if (scene.lastSoundAge < 48 && now >= 48 && now - 48 <= 3) {
                HyperGonerChargeSound charge = new HyperGonerChargeSound(scene);
                scene.sounds.add(charge);
                Minecraft.getInstance().getSoundManager().play(charge);
            }
            cue(scene.lastSoundAge, now, 244, KaziSounds.HYPER_GONER_RETURN_SFX.get(), .9F, .8F);
        } else if (scene.packet.type == AwakeningCutsceneType.EA_DRAW) {
            cue(scene.lastSoundAge, now, 8, SoundEvents.BEACON_ACTIVATE, .7F, .65F);
            cue(scene.lastSoundAge, now, 58, SoundEvents.RESPAWN_ANCHOR_CHARGE, .8F, .6F);
            cue(scene.lastSoundAge, now, 90, SoundEvents.GLASS_BREAK, 1F, .65F);
            cue(scene.lastSoundAge, now, 94, SoundEvents.GENERIC_EXPLODE, .7F, .7F);
        } else if (isNagi()) {
            cue(scene.lastSoundAge, now, 0, SoundEvents.BEACON_AMBIENT, .45F, .5F);
            cue(scene.lastSoundAge, now, 16, SoundEvents.RESPAWN_ANCHOR_CHARGE, .65F, .65F);
            cue(scene.lastSoundAge, now, InnerDragonsSequence.ARM_BREAK_TICK,
                    SoundEvents.GLASS_BREAK, .95F, .8F);
            cue(scene.lastSoundAge, now, InnerDragonsSequence.ARM_BREAK_TICK,
                    SoundEvents.GENERIC_EXPLODE, .8F, .7F);
            cue(scene.lastSoundAge, now, InnerDragonsSequence.ARM_BREAK_TICK + 3,
                    SoundEvents.GENERIC_EXPLODE, .3F, .5F);
            for (int i = 0; i < 8; i++)
                cue(scene.lastSoundAge, now, InnerDragonsSequence.EMERGENCE_TICK + i * InnerDragonsSequence.STAGGER_TICKS,
                        SoundEvents.ENDER_DRAGON_GROWL, .19F, .55F + i * .08F);
        }
        scene.lastSoundAge = now;
        scene.proxy.tickCount++;
    }

    private static void cue(float previous, float now, int tick, SoundEvent sound, float volume, float pitch) {
        // Do not replay a pile of missed cues when an initial packet arrives late.
        if (previous < tick && now >= tick && now - tick <= 3) {
            SimpleSound playing = SimpleSound.forUI(sound, pitch, volume);
            scene.sounds.add(playing);
            Minecraft.getInstance().getSoundManager().play(playing);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void camera(EntityViewRenderEvent.CameraSetup event) {
        if (!active()) return;
        Minecraft mc = Minecraft.getInstance();
        float partial = (float) event.getRenderPartialTicks();
        mc.options.setCameraType(PointOfView.FIRST_PERSON);
        event.getInfo().setup(scene.world, scene.camera, false, false, partial);
        // setup() interpolates xo/yo/zo and a private eye-height smoother. Preserve its current
        // offset when replacing the pose so the first frames do not drift after camera handoff.
        double eyeOffset = event.getInfo().getPosition().y
                - MathHelper.lerp(partial, scene.camera.yo, scene.camera.getY());
        positionCamera(partial);
        Vector3d desired = scene.camera.position();
        scene.camera.place(desired.add(0, -eyeOffset, 0), scene.camera.yRot, scene.camera.xRot);
        mc.options.setCameraType(PointOfView.FIRST_PERSON);
        event.getInfo().setup(scene.world, scene.camera, false, false, partial);
        float time = age(partial);
        float sinceBreak = time - (isDomainClash() ? 54 : isGoat() ? 224 :
                scene.packet.type == AwakeningCutsceneType.EA_DRAW ? 90 : InnerDragonsSequence.ARM_BREAK_TICK);
        float impact = isInstinct() ? UltraInstinctPresentation.shake(time)
                : !isChaosFinale() && !isKageyoshi() && sinceBreak >= 0 && sinceBreak < 12 ? (float) Math.exp(-sinceBreak / 3F) : 0;
        event.setYaw(scene.camera.yRot + (float) Math.sin(time * 3.7) * impact * 2.1F);
        event.setPitch(scene.camera.xRot + ((float) Math.cos(time * 4.1) * 1.5F - .65F) * impact);
        event.setRoll(scene.shotRoll + (float) Math.sin(time * 3.1) * impact * .9F);
    }

    private static void positionCamera(float partial) {
        if (scene == null) return;
        float t = age(partial);
        if (scene.packet.type == AwakeningCutsceneType.ULTRA_INSTINCT) {
            UltraInstinctPresentation.Shot shot = UltraInstinctPresentation.shot(origin(partial), scene.packet.yaw, t);
            placeLookingAt(shot.focus, shot.eye);
            return;
        }
        if (scene.packet.release != null) {
            // One low, uninterrupted dolly along the sword aisle. Both axes belong to
            // the cast, not to a viewer turning their mouse during the release.
            Minecraft mc = Minecraft.getInstance();
            double yaw = Math.toRadians(scene.packet.yaw);
            Vector3d front = new Vector3d(-Math.sin(yaw), 0, Math.cos(yaw));
            Vector3d hand = new Vector3d(-Math.cos(yaw), 0, -Math.sin(yaw));
            float rise = smooth(t / 47F), settle = smooth((t - 30) / 42F);
            double aspect = mc.getWindow().getWidth() / (double)Math.max(1, mc.getWindow().getHeight());
            double lens = MathHelper.clamp(Math.tan(Math.toRadians(35)) /
                    Math.tan(Math.toRadians(MathHelper.clamp(mc.options.fov, 30, 110) * .5)), .74, 1.9);
            double distance = (1.48 + 2.42 * rise + .42 * settle) * lens
                    * MathHelper.clamp(1.5 / aspect, 1, 1.65);
            Vector3d base = scene.packet.origin;
            Vector3d focus = base.add(hand.scale(.18 * (1 - rise))).add(0, 1.02 + .73 * rise + .16 * settle, 0);
            Vector3d eye = base.add(front.scale(distance)).add(hand.scale(.20 * (1 - rise)))
                    .add(0, .27 + .10 * rise, 0);
            placeLookingAt(focus, eye);
            return;
        }
        if (scene.clashStage != null) {
            Minecraft mc = Minecraft.getInstance();
            ClashStage.Shot shot = scene.clashStage.shot(t, (double)mc.getWindow().getWidth()
                    / Math.max(1,mc.getWindow().getHeight()), mc.options.fov);
            scene.shotRoll = shot.roll;
            placeLookingAt(shot.focus, shot.eye);
            return;
        }
        double bodyYaw = Math.toRadians(yaw(partial));
        Vector3d right = new Vector3d(-Math.cos(bodyYaw), 0, -Math.sin(bodyYaw));
        Vector3d forward = new Vector3d(-Math.sin(bodyYaw), 0, Math.cos(bodyYaw));
        Vector3d base = origin(partial);
        float reveal = smooth((t - 32) / 92F);
        double orbit = reveal * .42;
        Vector3d side = right.scale(Math.cos(orbit)).add(forward.scale(-Math.sin(orbit)));
        Vector3d front = forward.scale(Math.cos(orbit)).add(right.scale(Math.sin(orbit)));
        Vector3d focus = base.add(right.scale(.34 * (1 - reveal))).add(0, 1.35 + reveal * .8, 0);
        Vector3d desired = base.add(side.scale(1.55 + reveal * 3.25))
                .add(front.scale(2.15 + reveal * 7.35)).add(0, 1.9 + reveal * 3.3, 0);
        if (scene.packet.type == AwakeningCutsceneType.EA_DRAW) {
            focus = base.add(right.scale(.3)).add(forward.scale(.4)).add(0, 1.45, 0);
            desired = base.add(side.scale(2.1 + reveal * 1.2)).add(front.scale(2.5 + reveal * 2.3)).add(0, 1.85 + reveal * .4, 0);
        }
        if (isGoat()) {
            // The maw faces forward behind the caster; pull back to reveal its horns and vortex.
            float wide = smooth((t - 12) / 50F);
            focus = base.add(forward.scale(-1)).add(0, 2 + wide * 3.8, 0);
            desired = base.add(right.scale(.45 * (1 - wide))).add(forward.scale(5 + wide * 17))
                    .add(0, 2.1 + wide * 4.4, 0);
        }
        if (isChaosFinale()) {
            float widen = smooth(t / 230F);
            double turn = -.28 + .69 * widen;
            LivingEntity actor = subject();
            focus = base.add(0, (actor == null ? 1.4 : actor.getBbHeight() * .72) + 1.6*widen, 0);
            Vector3d radial = forward.scale(Math.cos(turn)).add(right.scale(Math.sin(turn)));
            desired = focus.add(radial.scale(5 + 7*widen)).add(0, 1.5 + 6*widen, 0);
        }
        placeLookingAt(focus, desired);
    }

    private static void placeLookingAt(Vector3d focus, Vector3d desired) {
        RayTraceResult hit = scene.world.clip(new RayTraceContext(focus, desired,
                RayTraceContext.BlockMode.COLLIDER, RayTraceContext.FluidMode.NONE, scene.camera));
        if (hit.getType() != RayTraceResult.Type.MISS) {
            Vector3d back = focus.subtract(hit.getLocation()).normalize();
            desired = hit.getLocation().add(back.scale(.2));
        }
        Vector3d look = focus.subtract(desired);
        float cameraYaw = (float) (-Math.atan2(look.x, look.z) * 180 / Math.PI);
        float cameraPitch = (float) (-Math.atan2(look.y, Math.sqrt(look.x * look.x + look.z * look.z)) * 180 / Math.PI);
        scene.camera.place(desired, cameraYaw, cameraPitch);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void overlay(RenderGameOverlayEvent.Pre event) {
        if (!active()) return;
        if (event.getType() != RenderGameOverlayEvent.ElementType.ALL) return;
        int width = event.getWindow().getGuiScaledWidth();
        int height = event.getWindow().getGuiScaledHeight();
        float t = age(event.getPartialTicks());
        if (isInstinct()) {
            UltraInstinctPresentation.overlay(event.getMatrixStack(), width, height, t);
            event.setCanceled(true);
            return;
        }
        if (isChaosFinale()) {
            net.kazi.kazimod.client.asriel.ChaosBeamFinale.render(event.getMatrixStack(), width, height, t);
            event.setCanceled(true);
            return;
        }
        float fade = Math.max(1 - smooth(t / 8), smooth((t - (scene.packet.type.durationTicks - 9)) / 9));
        if (isGoat()) net.kazi.kazimod.client.asriel.HyperGonerVisuals.overlay(event.getMatrixStack(), width, height, t);
        int bar = Math.round(height * (isKageyoshi() ? .04F : isDomainClash() ? .0625F : .105F));
        AbstractGui.fill(event.getMatrixStack(), 0, 0, width, bar, 0xFF07070D);
        AbstractGui.fill(event.getMatrixStack(), 0, height - bar, width, height, 0xFF07070D);
        if (isKageyoshi() && Minecraft.getInstance().options.showSubtitles) {
            net.minecraft.util.text.TranslationTextComponent line = new net.minecraft.util.text.TranslationTextComponent(
                    t < 20 ? "cinematic.kazimod.bankai_release" : "ability.kazimod.petal_senbonzakura_kageyoshi");
            AbstractGui.drawCenteredString(event.getMatrixStack(), Minecraft.getInstance().font, line,
                    width / 2, height - bar - 12, 0xFFFFFFFF);
        }
        if (!isGoat() && !isDomainClash() && !isKageyoshi() && fade > .001F)
            AbstractGui.fill(event.getMatrixStack(), 0, 0, width, height, ((int) (fade * 255) << 24));
        // Cancel this frame's HUD, without mutating the user's hide-GUI preference.
        event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void hand(RenderHandEvent event) { if (active()) event.setCanceled(true); }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void firstPersonOverlay(RenderBlockOverlayEvent event) {
        if (active()) event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void movement(InputUpdateEvent event) {
        if (!active() || event.getPlayer() != Minecraft.getInstance().player) return;
        MovementInput input = event.getMovementInput();
        input.forwardImpulse = input.leftImpulse = 0;
        input.up = input.down = input.left = input.right = input.jumping = input.shiftKeyDown = false;
        event.getPlayer().setSprinting(false);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void click(InputEvent.ClickInputEvent event) {
        if (active()) { event.setSwingHand(false); event.setCanceled(true); }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void scroll(InputEvent.MouseScrollEvent event) { if (active()) event.setCanceled(true); }

    @SubscribeEvent
    public static void logout(ClientPlayerNetworkEvent.LoggedOutEvent event) { stop(); }

    public static void stop() { stop(false); }

    private static void stop(boolean completed) {
        if (!completed) ChaosBeamFinale.clearReturnFade();
        if (scene == null) return;
        Scene ending = scene;
        scene = null;
        if (ending.packet.type == AwakeningCutsceneType.CHAOS_BEAM_FINALE)
            ChaosBeamTitleOverlay.endFinale(ending.packet.casterUuid, ending.packet.startTick);
        ending.restoreLiftGravity();
        Minecraft mc = Minecraft.getInstance();
        for (ISound sound : ending.sounds) mc.getSoundManager().stop(sound);
        // Another system may have taken ownership of the view (death/spectating/dimension travel).
        // Restore only our own camera, and never revive an entity from an old world.
        Entity currentCamera = mc.getCameraEntity();
        boolean ownsCamera = currentCamera == ending.camera;
        boolean lifecycleEnded = mc.level != ending.world || mc.player != ending.viewer
                || mc.player == null || !mc.player.isAlive();
        boolean vanillaCameraReset = lifecycleEnded && (currentCamera == null
                || currentCamera == mc.player || currentCamera == ending.viewer
                || currentCamera == ending.previousCamera);
        if (completed && ownsCamera && !lifecycleEnded
                && ending.packet.type == AwakeningCutsceneType.CHAOS_BEAM_FINALE)
            ChaosBeamFinale.beginReturnFade(ending.world, ending.viewer,
                    ending.packet.startTick + ending.packet.type.durationTicks);
        if (ownsCamera) {
            Entity previous = ending.previousCamera;
            if (previous == null || previous.removed || previous.level != mc.level) previous = mc.player;
            mc.setCameraEntity(previous);
        }
        // Vanilla can reset the camera first during death/logout/dimension travel. The old
        // perspective still belongs to us then; a different custom camera remains untouched.
        if ((ownsCamera || vanillaCameraReset) && mc.options.getCameraType() == PointOfView.FIRST_PERSON)
            mc.options.setCameraType(ending.previousPerspective);
    }

    private static float smooth(float value) {
        float t = MathHelper.clamp(value, 0, 1);
        return t * t * (3 - 2 * t);
    }

    private static boolean finite(Vector3d value) {
        return value != null && Double.isFinite(value.x) && Double.isFinite(value.y) && Double.isFinite(value.z);
    }

    private static final class Scene {
        final InnerDragonsCutscenePacket packet;
        final ClientWorld world;
        final LivingEntity viewer;
        final Entity previousCamera;
        final PointOfView previousPerspective;
        final CameraAnchor camera;
        final RemoteClientPlayerEntity proxy;
        final RemoteClientPlayerEntity partnerProxy;
        final ClashStage clashStage;
        PetalField preview;
        boolean shiftHeld;
        float shotRoll;
        final List<ISound> sounds = new ArrayList<>();
        float lastSoundAge = -1;
        LivingEntity liftTickCaster;
        boolean gravityBeforeLiftTick;

        boolean isLocalCaster() {
            return packet.type == AwakeningCutsceneType.CHAOS_BEAM_FINALE
                    && viewer.getUUID().equals(packet.casterUuid);
        }

        void beginLiftTick() {
            restoreLiftGravity();
            if (!isLocalCaster() || Minecraft.getInstance().isPaused()) return;
            // Suppress local gravity only around this simulation tick. Server metadata
            // owns the scene's real gravity state, including the preceding beam handoff.
            liftTickCaster = viewer;
            gravityBeforeLiftTick = viewer.isNoGravity();
            viewer.setNoGravity(true);
            viewer.setDeltaMovement(Vector3d.ZERO);
            viewer.fallDistance = 0;
        }

        void restoreLiftGravity() {
            if (liftTickCaster == null) return;
            // A release update can arrive between START and END. Never overwrite
            // an authoritative restored gravity state with our earlier snapshot.
            if (liftTickCaster.isNoGravity()) liftTickCaster.setNoGravity(gravityBeforeLiftTick);
            liftTickCaster = null;
        }

        void updateLiftProxy(double age) {
            // Only the fallback stand-in follows this timeline directly. Tracked remote
            // players retain their server position updates and normal interpolation.
            proxy.xOld = proxy.xo = proxy.getX();
            proxy.yOld = proxy.yo = proxy.getY();
            proxy.zOld = proxy.zo = proxy.getZ();
            ChaosBeamFinaleLift.advance(proxy, packet.origin, age);
        }

        Scene(Minecraft mc, InnerDragonsCutscenePacket packet) {
            this.packet = packet;
            world = mc.level;
            viewer = mc.player;
            previousCamera = mc.getCameraEntity();
            previousPerspective = mc.options.getCameraType();
            shiftHeld = mc.options.keyShift.isDown();
            camera = new CameraAnchor(world);
            NetworkPlayerInfo info = mc.getConnection() == null ? null : mc.getConnection().getPlayerInfo(packet.casterUuid);
            GameProfile profile = info == null ? new GameProfile(packet.casterUuid,
                    packet.release != null ? packet.release.name : packet.type == AwakeningCutsceneType.ULTRA_INSTINCT ? "Saiyan"
                    : packet.clash == null ? "Nagi" : packet.clash.kokuName) : info.getProfile();
            proxy = packet.release != null ? new ClashSkinProxy(world, profile, packet.release.skinParts)
                    : packet.type == AwakeningCutsceneType.ULTRA_INSTINCT ? new ClashSkinProxy(world, profile, 127)
                    : packet.clash == null ? new RemoteClientPlayerEntity(world, profile)
                    : new ClashSkinProxy(world, profile, packet.clash.kokuParts);
            if (packet.release != null) {
                CompoundNBT tag = PetalState.get(proxy);
                tag.putBoolean("Fruit", true);
                tag.putInt("Action", 6);
                tag.putLong("Start", packet.startTick);
                tag.putLong("End", packet.startTick + 40);
            }
            if (packet.clash == null) {
                clashStage = null;
                partnerProxy = null;
            } else {
                InnerDragonsCutscenePacket.ClashPartner b = packet.clash;
                clashStage = new ClashStage(packet.origin,b.origin,packet.yaw);
                NetworkPlayerInfo partner = mc.getConnection() == null ? null : mc.getConnection().getPlayerInfo(b.uuid);
                partnerProxy = new ClashSkinProxy(world, partner == null
                        ? new GameProfile(b.uuid,b.kamaName) : partner.getProfile(), b.kamaParts);
                partnerProxy.setPos(b.origin.x,b.origin.y,b.origin.z);
            }
            double lift = packet.type == AwakeningCutsceneType.CHAOS_BEAM_FINALE
                    ? ChaosBeamFinaleLift.height(world.getGameTime() - packet.startTick) : 0;
            proxy.setPos(packet.origin.x, packet.origin.y, packet.origin.z);
            if (lift > 0) proxy.move(MoverType.SELF, new Vector3d(0, lift, 0));
            proxy.xOld = proxy.xo = proxy.getX();
            proxy.yOld = proxy.yo = proxy.getY();
            proxy.zOld = proxy.zo = proxy.getZ();
            proxy.yBodyRot = proxy.yBodyRotO = proxy.yRot = proxy.yRotO = proxy.yHeadRot = proxy.yHeadRotO = packet.yaw;
            camera.place(packet.origin, packet.yaw, 0);
        }
    }

    private static final class ClashSkinProxy extends RemoteClientPlayerEntity {
        final int parts;
        ClashSkinProxy(ClientWorld world, GameProfile profile, int parts) {
            super(world,profile); this.parts=parts;
        }
        @Override public boolean isModelPartShown(net.minecraft.entity.player.PlayerModelPart part) {
            return (parts & part.getMask()) != 0;
        }
    }

    /** One continuous original recording, faded into the flash without restarting or pitch shifts. */
    private static final class HyperGonerChargeSound extends TickableSound {
        private final Scene owner;
        HyperGonerChargeSound(Scene owner) {
            super(KaziSounds.HYPER_GONER_CHARGE_SFX.get(), SoundCategory.MASTER);
            this.owner = owner;
            this.volume = 1F;
            this.pitch = 1F;
            this.looping = false;
            this.relative = true;
            this.attenuation = AttenuationType.NONE;
        }
        @Override public void tick() {
            if (scene != owner || !active()) { stop(); return; }
            float age = owner.world.getGameTime() - owner.packet.startTick;
            this.volume = 1 - smooth((age - 224) / 20F);
            if (age >= 244) stop();
        }
    }

    /** Never added to ClientWorld or sent to the server. */
    private static final class CameraAnchor extends Entity {
        CameraAnchor(ClientWorld world) {
            super(EntityType.ARMOR_STAND, world);
            noPhysics = true;
            setNoGravity(true);
        }
        void place(Vector3d point, float yaw, float pitch) {
            setPos(point.x, point.y, point.z);
            xo = xOld = point.x;
            yo = yOld = point.y;
            zo = zOld = point.z;
            yRot = yRotO = yaw;
            xRot = xRotO = pitch;
        }
        @Override protected float getEyeHeight(Pose pose, EntitySize size) { return 0; }
        @Override protected void defineSynchedData() { }
        @Override protected void readAdditionalSaveData(CompoundNBT tag) { }
        @Override protected void addAdditionalSaveData(CompoundNBT tag) { }
        @Override public IPacket<?> getAddEntityPacket() { throw new UnsupportedOperationException("Local camera"); }
    }
}
