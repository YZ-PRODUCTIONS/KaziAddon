package net.kazi.kazimod.abilities.KameRework;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.play.server.SPlayerAbilitiesPacket;
import net.minecraft.util.math.vector.Vector3d;
import xyz.pixelatedw.mineminenomi.abilities.PropelledFlightAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.*;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;

/** Keeps the Zoan flight controls without a stamina-based grounding phase. */
public final class WorldTurtleFlightAbility extends PropelledFlightAbility {
    public static final AbilityCore<WorldTurtleFlightAbility> INSTANCE =
            new AbilityCore.Builder<>("World Turtle Flight", AbilityCategory.DEVIL_FRUITS, AbilityType.PASSIVE, WorldTurtleFlightAbility::new)
                    .setUnlockCheck(WorldTurtleFormAbility::canUnlock).build();
    public WorldTurtleFlightAbility(AbilityCore<WorldTurtleFlightAbility> core) {
        super(core);
        addCanUseCheck((user, ability) -> WorldTurtleFormAbility.canUnlock(user)
                && net.kazi.kazimod.init.KaziMorphs.WORLD_TURTLE.get().isActive(user)
                ? AbilityUseResult.success() : AbilityUseResult.fail(null));
    }
    @Override public float getMaxSpeed(LivingEntity user) { return user.isSprinting() ? .8F : .4F; }
    @Override public boolean canRenderGauge(PlayerEntity player) { return false; }
    @Override public void tick(LivingEntity user) {
        boolean active = net.kazi.kazimod.init.KaziMorphs.WORLD_TURTLE.get().isActive(user);
        if (active) resetRecovery();
        // The rush owns velocity while active; ordinary flying resumes immediately after it.
        boolean spinning = WorldShakingSpinAbility.active(user);
        if (!spinning) super.tick(user);
        if (!active || isPaused() || !(user instanceof PlayerEntity)) return;
        resetRecovery();
        PlayerEntity player = (PlayerEntity) user;
        keepAirborne(player);
        if (!spinning && player.isOnGround()) {
            Vector3d motion = player.getDeltaMovement();
            AbilityHelper.setDeltaMovement(player, motion.x, Math.max(motion.y, 0.32D), motion.z);
            player.fallDistance = 0;
        }
    }

    private void resetRecovery() {
        stamina = 100.0D;
        isRecovering = false;
        hasLanded = false;
    }

    static void keepAirborne(PlayerEntity player) {
        boolean wasFlying = player.abilities.flying;
        player.abilities.flying = true;
        if (!player.abilities.mayfly) {
            enableFlight(player);
        } else if (!wasFlying && player instanceof ServerPlayerEntity) {
            ((ServerPlayerEntity) player).connection.send(new SPlayerAbilitiesPacket(player.abilities));
        }
    }
    @Override protected float getAcceleration(LivingEntity user) { return user.isSprinting() ? .006F : .003F; }
    @Override protected int getHeightDifference(LivingEntity user) { return 80; }
}
