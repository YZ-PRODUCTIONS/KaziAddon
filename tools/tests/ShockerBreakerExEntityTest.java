import java.util.ArrayList;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.kazi.kazimod.abilities.YagiAwakening.ShockerBreakerVisualTiming;
import net.kazi.kazimod.entities.asriel.AsrielLightningEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.server.ServerWorld;

/** Runs the actual production lightning entity; fixtures only provide a world, entities and damage observations. */
public final class ShockerBreakerExEntityTest {
    private static int checks;

    public static void main(String[] args) {
        liveHealthPalette();
        completeStationaryCast(40);
        completeStationaryCast(41);
        completeStationaryCast(43);
        overlappingPillarsAndInvulnerability();
        movingTargetsAndRosterLimit();
        freshPositionForEveryPillar();
        fewerTargetsAndFallback();
        cancellationAndOwnerCleanup();
        normalMode(40);
        normalMode(41);
        normalMode(43);
        normalSplashAndRejectedHits();
        normalIndependentCasts();
        normalAimOverridesNearbyEnemies();
        normalEmptyCastTracksAim();
        System.out.println("Shocker Breaker production entity: " + checks + " scheduling, warning, strike, damage and cleanup checks passed.");
    }

    private static void liveHealthPalette() {
        Fixture f = new Fixture();
        f.owner.health = 50;
        AsrielLightningEntity aura = f.cast(false);
        AsrielLightningEntity marker = f.markers().get(0);
        float radius = marker.getRadius();
        check(!aura.hasDarkPalette() && !marker.hasDarkPalette(), "Exactly half health uses the normal palette");
        f.owner.health = 49;
        f.world.tickEntities();
        check(aura.hasDarkPalette() && marker.hasDarkPalette(), "Existing aura and warning darken when their owner drops below half");
        check(!marker.isEnhanced() && marker.getRadius() == radius, "Live recoloring must not change the cast variant or its damage radius");
        AsrielLightningEntity fresh = new AsrielLightningEntity(f.world, f.owner, f.owner.position(), false);
        check(fresh.hasDarkPalette(), "A low-health projectile has its palette before the first server tick");
        f.owner.health = 50;
        f.world.tickEntities();
        check(!aura.hasDarkPalette() && !marker.hasDarkPalette(), "Healing back to half restores the normal palette");
        f.owner.maxHealth = 200;
        f.owner.health = 99;
        f.world.tickEntities();
        check(aura.hasDarkPalette() && marker.hasDarkPalette(), "The palette follows health percentage, not a fixed health value");
    }

    private static void completeStationaryCast(int armTick) {
        Fixture f = new Fixture();
        LivingEntity a = f.enemy(18, 0), b = f.enemy(-18, 0), c = f.enemy(0, 35);
        AsrielLightningEntity aura = f.cast();
        check(f.markers().size() == 3, "The first round must immediately spawn one warning for each of three enemies");
        Map<AsrielLightningEntity, Long> firstStrikes = new IdentityHashMap<>();
        for (int tick = 1; tick <= 200; tick++) {
            f.world.gameTime = tick;
            if (tick == armTick) aura.arm();
            f.world.tickEntities();
            List<AsrielLightningEntity> markers = f.markers();
            int expected = 3 * (Math.min(tick / 10, 4) + 1);
            check(markers.size() == expected, "Exactly three warnings are added every ten ticks, ending at fifteen");
            for (AsrielLightningEntity marker : markers) {
                long spawnTick = f.world.spawnTimes.get(marker);
                check(spawnTick % 10 == 0 && spawnTick <= 40, "Each warning belongs to exactly one of the five half-second rounds");
                if (marker.hasStruck() && !firstStrikes.containsKey(marker)) firstStrikes.put(marker, (long) tick);
                if (tick < Math.max(armTick, spawnTick + 40)) check(!marker.hasStruck(), "Every individual warning must last two seconds before its own strike");
                check(marker.removed == (tick >= spawnTick + ShockerBreakerVisualTiming.lifetime(true)),
                        "Each EX pillar must disappear after its own single impact and short afterglow");
            }
            if (tick < armTick) check(a.successfulHits + b.successfulHits + c.successfulHits == 0, "The initial charge cannot cause damage");
            check(aura.removed == (tick >= Math.max(40, armTick)),
                    "The completed scheduler must retain pending markers until charge release, then remove itself");
            if (tick == 80) check(firstStrikes.size() == 15, "Final pillars still strike after the scheduler expires");
        }
        check(new HashSet<>(f.markers()).size() == 15, "All fifteen pillars must be distinct entities with distinct warnings");
        for (Map.Entry<AsrielLightningEntity, Long> strike : firstStrikes.entrySet()) {
            long markTime = f.world.spawnTimes.get(strike.getKey());
            check(strike.getValue() == Math.max(armTick, markTime + 40), "Strike timing follows each marker's own spawn time, including late initial arming");
        }
        for (LivingEntity target : new LivingEntity[]{a, b, c}) {
            check(target.successfulHits == 5 && target.damageAttempts == 5, "All five targeted pillars damage each stationary enemy");
            check(target.damageAmounts.get(0) == 30F && target.damageAmounts.get(1) == 30F, "EX retains its existing thirty damage per successful pillar");
        }
        check(f.world.sounds.size() == 15, "Every EX pillar must play its own impact sound, including simultaneous pillars");
        checkOneSoundPerStruckMarker(f);
        for (Map.Entry<AsrielLightningEntity, Long> strike : firstStrikes.entrySet()) {
            String expectedSound = strike.getValue() + ":0:" + strike.getKey().getId();
            check(f.world.sounds.contains(expectedSound), "Each marker's sound must occur exactly when that marker strikes");
        }
        for (int round = 0; round < 5; round++) {
            String timestamp = Math.max(armTick, 40 + round * 10) + ":";
            check(f.world.sounds.stream().filter(sound -> sound.startsWith(timestamp)).count() == 3,
                    "All three EX sounds must overlap in each round without selecting a lead pillar");
        }
        for (AsrielLightningEntity marker : f.markers()) check(marker.removed, "All finished pillar entities eventually expire");
    }

    private static void overlappingPillarsAndInvulnerability() {
        Fixture f = new Fixture();
        LivingEntity a = f.enemy(10, 0), b = f.enemy(11, 0), c = f.enemy(12, 0);
        AsrielLightningEntity aura = f.cast();
        for (int tick = 1; tick <= 140; tick++) {
            f.world.gameTime = tick;
            if (tick == 40) aura.arm();
            f.world.tickEntities();
        }
        check(f.markers().size() == 15, "Overlapping targets still receive fifteen distinct pillars");
        check(f.world.sounds.size() == 15, "Overlapping pillars must each play sound");
        checkOneSoundPerStruckMarker(f);
        for (LivingEntity target : new LivingEntity[]{a, b, c}) {
            check(target.successfulHits == 15 && target.damageAttempts == 15, "All overlapping pillars can hit the same target");
            check(target.hitTimes.get(0) == 40 && target.hitTimes.get(2) == 40, "All three overlapping pillars can hit together");
        }

        Fixture evasion = new Fixture();
        LivingEntity evading = evasion.enemy(15, 0);
        evading.invulnerable = true;
        AsrielLightningEntity evasiveAura = evasion.cast();
        for (int tick = 1; tick <= 140; tick++) {
            evasion.world.gameTime = tick;
            if (tick == 40) evasiveAura.arm();
            if (tick == 51) evading.invulnerable = false;
            evasion.world.tickEntities();
        }
        check(evading.damageAttempts == 5 && evading.successfulHits == 3, "Only the first two hits are rejected by immunity");
        check(evading.hitTimes.get(0) == 60 && evading.hitTimes.get(1) == 70, "Later pillars can damage after immunity expires");
    }

    private static void movingTargetsAndRosterLimit() {
        Fixture f = new Fixture();
        LivingEntity a = f.enemy(10, 0), b = f.enemy(20, 0), c = f.enemy(30, 0), d = f.enemy(40, 0);
        AsrielLightningEntity aura = f.cast();
        List<AsrielLightningEntity> original = new ArrayList<>(f.markers());
        Set<Double> initialX = new HashSet<>();
        for (AsrielLightningEntity marker : original) initialX.add(marker.getX());
        check(initialX.size() == 3 && initialX.contains(10D) && initialX.contains(20D) && initialX.contains(30D), "The first three nearest enemies fill the cast roster");
        for (int tick = 1; tick <= 120; tick++) {
            f.world.gameTime = tick;
            if (tick == 10) { a.setPos(14, 0, 4); b.setPos(25, 0, 0); d.setPos(1, 0, 0); }
            if (tick == 40) aura.arm();
            f.world.tickEntities();
        }
        for (AsrielLightningEntity marker : original) check(initialX.contains(marker.getX()) && marker.getZ() == 0,
                "An already placed warning must stay at its original location after the enemy moves");
        int movedA = 0, movedB = 0;
        for (AsrielLightningEntity marker : f.markers()) {
            if (f.world.spawnTimes.get(marker) > 0) {
                check(marker.getX() != 1, "A closer fourth enemy cannot expand or replace the three-enemy roster");
                if (marker.getX() == 14 && marker.getZ() == 4) movedA++;
                if (marker.getX() == 25) movedB++;
            }
        }
        check(movedA == 4 && movedB == 4, "The four later rounds mark the enemies' updated positions");
        check(d.successfulHits == 1, "The unmarked enemy is hit by the one pillar in range");
    }

    private static void freshPositionForEveryPillar() {
        Fixture f = new Fixture();
        LivingEntity target = f.enemy(10, 0);
        AsrielLightningEntity aura = f.cast();
        Set<String> distinctAreas = new HashSet<>();
        for (int tick = 0; tick <= 140; tick++) {
            if (tick > 0) {
                f.world.gameTime = tick;
                // Continuous motion changes the target's position before every warning round and strike.
                target.setPos(10 + tick * .10, 0, tick * .05);
                if (tick == 40) aura.arm();
                f.world.tickEntities();
            }
            for (AsrielLightningEntity marker : f.markers()) {
                long birth = f.world.spawnTimes.get(marker);
                double expectedX = 10 + birth * .10, expectedZ = birth * .05;
                check(Math.abs(marker.getX() - expectedX) < 1e-9 && Math.abs(marker.getZ() - expectedZ) < 1e-9,
                        "Every pillar must keep its own position sampled when its warning was created, not a cast-wide or later target position");
                if (tick == birth) {
                    check(!marker.hasStruck() && !marker.removed && marker.getVisualAge(0) == 0,
                            "Every newly targeted area must begin as a distinct, fresh red-warning entity");
                    check(distinctAreas.add(marker.getX() + ":" + marker.getZ()),
                            "A moving enemy must create a different warning area for every individual pillar");
                }
                if (tick < birth + 40) {
                    check(!marker.hasStruck() && !marker.removed,
                            "Each separate warning must remain in place for its full two-second dodge window");
                } else {
                    check(marker.hasStruck(), "A pillar must strike only its own previously warned area after forty ticks");
                }
            }
        }
        check(distinctAreas.size() == 5 && f.markers().size() == 5,
                "A moving enemy receives five separate marked areas and exactly five pillars");
        check(f.world.sounds.size() == 5, "Each warned area produces one audible impact, with no old-location retriggers");
        check(target.successfulHits == 5, "All five pillars reach the moving enemy");
    }

    private static void fewerTargetsAndFallback() {
        for (int targetCount = 1; targetCount <= 2; targetCount++) {
            Fixture f = new Fixture();
            for (int i = 0; i < targetCount; i++) f.enemy(i == 0 ? 20 : -20, 0);
            AsrielLightningEntity aura = f.cast();
            f.runThrough(120, aura, 40);
            check(f.markers().size() == targetCount * 5, "Fewer targets receive five pillars each, without multiplying their personal allocation");
        }
        Fixture empty = new Fixture();
        AsrielLightningEntity emptyAura = empty.cast();
        empty.runThrough(120, emptyAura, 40);
        check(empty.markers().size() == 1 && empty.markers().get(0).hasStruck(), "A cast with no nearby enemies retains the original single aimed fallback");

        Fixture leaves = new Fixture();
        LivingEntity target = leaves.enemy(20, 0);
        AsrielLightningEntity leavingAura = leaves.cast();
        leaves.world.gameTime = 10;
        target.setPos(100, 0, 0);
        leaves.runThrough(120, leavingAura, 40);
        check(leaves.markers().size() == 1, "An enemy outside targeting range does not receive new warning locations");
        check(target.successfulHits == 0, "Leaving old warning locations avoids their eventual damage");
    }

    private static void cancellationAndOwnerCleanup() {
        cancellationAndOwnerCleanup(true);
        cancellationAndOwnerCleanup(false);
    }

    private static void cancellationAndOwnerCleanup(boolean enhanced) {
        Fixture cancelled = new Fixture();
        LivingEntity victim = cancelled.enemy(20, 0);
        AsrielLightningEntity aura = cancelled.cast(enhanced);
        cancelled.runThrough(15, aura, -1);
        check(cancelled.markers().size() == 2, "Two warning rounds exist before a mid-charge cancellation");
        aura.cancelCast();
        for (AsrielLightningEntity marker : cancelled.markers()) check(marker.removed, "Cancelling the initial charge immediately removes every pending warning");
        cancelled.runThrough(140, aura, -1);
        check(victim.successfulHits == 0 && cancelled.markers().size() == 2, "A cancelled cast causes no damage or additional rounds");
        check(cancelled.world.sounds.isEmpty(), "Cancelled warnings must never play a strike sound");

        Fixture absent = new Fixture();
        LivingEntity absentVictim = absent.enemy(20, 0);
        AsrielLightningEntity absentAura = absent.cast(enhanced);
        absent.runThrough(25, absentAura, -1);
        absent.owner.remove();
        absent.runThrough(120, absentAura, -1);
        check(absentAura.removed && absentVictim.successfulHits == 0, "An absent owner ends the scheduler and prevents pending damage");
        check(absent.world.sounds.isEmpty(), "Owner loss before striking must leave all pending warnings silent");
        for (AsrielLightningEntity marker : absent.markers()) check(marker.removed, "Owner loss also removes every pending pillar");

        Fixture timeout = new Fixture();
        LivingEntity timeoutVictim = timeout.enemy(20, 0);
        AsrielLightningEntity timeoutAura = timeout.cast(enhanced);
        timeout.runThrough(120, timeoutAura, -1);
        check(timeoutAura.removed && timeoutVictim.successfulHits == 0, "A charge never armed expires safely without striking");
        check(timeout.world.sounds.isEmpty(), "An unarmed cast must expire without playing any strike sounds");
        for (AsrielLightningEntity marker : timeout.markers()) check(marker.removed, "Unarmed charge timeout leaves no warnings behind");
    }

    private static void normalMode(int armTick) {
        Fixture f = new Fixture();
        LivingEntity target = f.enemy(20, 0);
        f.enemy(-35, 0);
        AsrielLightningEntity aura = f.cast(false);
        Set<String> warnedAreas = new HashSet<>();
        for (int tick = 0; tick <= 160; tick++) {
            if (tick > 0) {
                f.world.gameTime = tick;
                target.setPos(20 + tick * .05, 0, tick * .02);
                f.owner.aimPoint = target.position();
                if (tick == armTick) aura.arm();
                f.world.tickEntities();
            }
            check(f.markers().size() == Math.min(tick / 10, 4) + 1,
                    "Regular mode must add one fresh pillar per half-second for exactly five rounds");
            int expectedHits = 0;
            for (AsrielLightningEntity marker : f.markers()) {
                long birth = f.world.spawnTimes.get(marker);
                check(birth % 10 == 0 && birth <= 40,
                        "Each regular warning must start at its own half-second marking beat");
                check(!marker.isEnhanced() && marker.getRadius() == 9,
                        "Regular pillars must retain their normal variant and damage radius");
                check(Math.abs(marker.getX() - (20 + birth * .05)) < 1e-9
                                && Math.abs(marker.getZ() - birth * .02) < 1e-9,
                        "Each regular pillar must snapshot the crosshair at its own mark time and keep that warning fixed afterward");
                if (tick == birth) {
                    check(marker.getVisualAge(0) == 0 && !marker.hasStruck(),
                            "Every new regular warning must start from zero rather than reuse an earlier pillar's clock");
                    check(warnedAreas.add(marker.getX() + ":" + marker.getZ()),
                            "Moving the crosshair must create a fresh warning area for every pillar");
                }
                long strikeTick = Math.max(armTick, birth + 40);
                check(marker.hasStruck() == (tick >= strikeTick),
                        "Every regular pillar must honor its own full two-second warning before striking once");
                check(marker.removed == (tick >= birth + ShockerBreakerVisualTiming.lifetime(false)),
                        "Every regular pillar must clear after its own single impact and short fade");
                if (tick >= strikeTick) expectedHits++;
            }
            check(f.world.sounds.size() == expectedHits,
                    "Regular mode must emit one impact sound per fresh pillar without old-location repeats");
            check(target.successfulHits == expectedHits && target.damageAttempts == expectedHits,
                    "Every regular pillar may hit the same enemy");
            check(aura.removed == (tick >= Math.max(40, armTick)),
                    "The regular scheduler must retain all five pending pillars until the charge is armed");
        }
        check(f.markers().size() == 5 && warnedAreas.size() == 5 && target.successfulHits == 5,
                "A regular cast must create five distinct warned pillars and damage the moving target five times");
        for (float damage : target.damageAmounts) check(damage == 24F,
                "Regular pillars must retain their existing twenty-four damage per impact");
        for (AsrielLightningEntity marker : f.markers()) check(marker.removed,
                "All regular pillar entities must expire after the full sequence");
    }

    private static void normalSplashAndRejectedHits() {
        Fixture f = new Fixture();
        LivingEntity target = f.enemy(20, 0), splashA = f.enemy(22, 0), splashB = f.enemy(24, 0);
        AsrielLightningEntity aura = f.cast(false);
        f.runThrough(160, aura, 40);
        check(f.markers().size() == 5 && f.world.sounds.size() == 5,
                "All five regular pillars emit impact sounds");
        for (AsrielLightningEntity marker : f.markers()) check(marker.getX() == 20 && marker.hasStruck(),
                "Pillars use the aimed position, while adjacent enemies can be caught in its splash");
        for (LivingEntity victim : new LivingEntity[]{target, splashA, splashB}) {
            check(victim.successfulHits == 5 && victim.damageAttempts == 5,
                    "Every pillar hits both targeted and splash victims");
            check(victim.hitTimes.get(0) == 40 && victim.hitTimes.get(1) == 50,
                    "The first two regular pillars land ten ticks apart");
            for (float damage : victim.damageAmounts) check(damage == 24F,
                    "Regular hits must retain twenty-four damage rather than EX's thirty");
        }

        Fixture evasion = new Fixture();
        LivingEntity evading = evasion.enemy(20, 0);
        evading.invulnerable = true;
        AsrielLightningEntity evasiveAura = evasion.cast(false);
        for (int tick = 1; tick <= 160; tick++) {
            evasion.world.gameTime = tick;
            if (tick == 40) evasiveAura.arm();
            if (tick == 51) evading.invulnerable = false;
            evasion.world.tickEntities();
        }
        check(evading.damageAttempts == 5 && evading.successfulHits == 3,
                "All three pillars after immunity expires can hit");
        check(evading.hitTimes.get(0) == 60 && evading.hitTimes.get(1) == 70,
                "Later regular pillars hit after immunity ends");
        check(evasion.markers().size() == 5 && evasion.world.sounds.size() == 5,
                "Rejected damage must preserve all five warnings and impacts");
    }

    private static void normalIndependentCasts() {
        Fixture f = new Fixture();
        LivingEntity target = f.enemy(20, 0);
        AsrielLightningEntity first = f.cast(false);
        f.runThrough(160, first, 40);
        check(target.successfulHits == 5, "The first regular cast must finish with five hits");
        AsrielLightningEntity second = f.cast(false);
        f.runThrough(320, second, 200);
        check(target.successfulHits == 10 && target.damageAttempts == 10,
                "Both casts can hit the same enemy five times");
        check(target.hitTimes.get(5) == 200 && target.hitTimes.get(6) == 210,
                "The next cast lands its first two pillars on schedule");
        check(f.markers().size() == 10 && f.world.sounds.size() == 10,
                "Two independent regular casts must each retain all five pillars");
    }

    private static void normalAimOverridesNearbyEnemies() {
        Fixture f = new Fixture();
        LivingEntity a = f.enemy(20, 0), b = f.enemy(-35, 0);
        LivingEntity ally = f.enemy(1, 0);
        ally.hostile = false;
        f.owner.aimPoint = new Vector3d(20, 0, 0);
        AsrielLightningEntity aura = f.cast(false);
        for (int tick = 1; tick <= 160; tick++) {
            f.world.gameTime = tick;
            if (tick == 10) { b.setPos(10, 0, 0); f.owner.aimPoint = new Vector3d(30, 0, 0); }
            if (tick == 20) { a.setPos(6, 0, 0); f.owner.aimPoint = new Vector3d(28, 0, 0); }
            if (tick == 30) {
                a.setPos(100, 0, 0);
                b.remove();
                f.owner.setPos(10, 0, 0);
                f.owner.aimPoint = new Vector3d(40, 0, 0);
            }
            if (tick == 40) { f.enemy(5, 0); f.owner.aimPoint = new Vector3d(32, 0, 0); aura.arm(); }
            // Aim changes between marking beats cannot move existing warnings or spawn extras.
            if (tick == 41) f.owner.aimPoint = new Vector3d(-40, 0, 0);
            f.world.tickEntities();
        }
        double[] expectedX = {20, 30, 28, 40, 32};
        check(f.markers().size() == expectedX.length,
                "Changing aim, missing enemies and newly arriving enemies still produce exactly five regular pillars");
        for (int i = 0; i < expectedX.length; i++) {
            AsrielLightningEntity marker = f.markers().get(i);
            check(marker.getX() == expectedX[i] && marker.getZ() == 0,
                    "Each regular round samples the current crosshair even when another enemy is closer");
            check(f.world.spawnTimes.get(marker) == i * 10L && marker.hasStruck() && marker.removed,
                    "Every aimed warning remains fixed and independently completes and expires");
        }
        check(ally.successfulHits == 0, "The nearest ally cannot be targeted or damaged by regular pillars");
        check(f.world.sounds.size() == 5, "Changing crosshair aim must not skip or duplicate impact sounds");
    }

    private static void normalEmptyCastTracksAim() {
        Fixture f = new Fixture();
        AsrielLightningEntity aura = f.cast(false);
        for (int tick = 1; tick <= 160; tick++) {
            f.world.gameTime = tick;
            f.owner.setPos(tick * .1, 0, tick * .05);
            if (tick == 40) aura.arm();
            f.world.tickEntities();
        }
        check(f.markers().size() == 5, "Regular mode must still fire all five aimed pillars when no enemy is in range");
        for (AsrielLightningEntity marker : f.markers()) {
            long birth = f.world.spawnTimes.get(marker);
            check(Math.abs(marker.getX() - (20 + birth * .1)) < 1e-9
                            && Math.abs(marker.getZ() - birth * .05) < 1e-9,
                    "Every round refreshes the user's aim instead of reusing the original empty-cast position");
            check(marker.hasStruck() && marker.removed, "An empty-cast warning must strike and expire normally");
        }
        check(f.world.sounds.size() == 5 && aura.removed,
                "An empty regular cast must play five impacts and clean up its controller");
    }

    private static void checkOneSoundPerStruckMarker(Fixture f) {
        int struckMarkers = 0;
        for (AsrielLightningEntity marker : f.markers()) {
            String markerId = Integer.toString(marker.getId());
            long markerSounds = f.world.sounds.stream().filter(sound -> sound.split(":")[2].equals(markerId)).count();
            check(markerSounds == (marker.hasStruck() ? 1 : 0),
                    "Each marker must play exactly one sound on impact and remain silent throughout its afterglow");
            if (marker.hasStruck()) struckMarkers++;
        }
        check(f.world.sounds.size() == struckMarkers, "Only actual pillar impacts may produce strike sounds");
        for (String sound : f.world.sounds) check(sound.split(":")[1].equals("0"),
                "Sustained lightning visuals must not trigger any additional sound pulses");
    }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }

    private static final class Fixture {
        final ServerWorld world = new ServerWorld();
        final LivingEntity owner = new LivingEntity(world);
        Fixture() { owner.hostile = false; world.addFreshEntity(owner); }
        LivingEntity enemy(double x, double z) {
            LivingEntity target = new LivingEntity(world);
            target.setPos(x, 0, z);
            world.addFreshEntity(target);
            return target;
        }
        AsrielLightningEntity cast() {
            return cast(true);
        }
        AsrielLightningEntity cast(boolean enhanced) {
            AsrielLightningEntity aura = AsrielLightningEntity.chargeAura(world, owner);
            world.addFreshEntity(aura);
            if (enhanced) aura.startEnhancedSequence(owner);
            else aura.startRegularSequence(owner);
            return aura;
        }
        List<AsrielLightningEntity> markers() {
            List<AsrielLightningEntity> result = new ArrayList<>();
            for (Entity entity : world.entities) if (entity instanceof AsrielLightningEntity && !((AsrielLightningEntity) entity).isChargeAura()) result.add((AsrielLightningEntity) entity);
            return result;
        }
        void runThrough(int end, AsrielLightningEntity armedEntity, int armTick) {
            for (int tick = (int) world.getGameTime() + 1; tick <= end; tick++) {
                world.gameTime = tick;
                if (tick == armTick) armedEntity.arm();
                world.tickEntities();
            }
        }
    }
}
