import net.kazi.kazimod.abilities.NagiAwakening.InnerDragonsCutsceneCooldown;
import net.kazi.kazimod.abilities.NagiAwakening.InnerDragonsSequence;

/** Exercises the production cooldown clock together with the existing low-health latch. */
public final class InnerDragonsCooldownTest {
    private static int checks;

    public static void main(String[] args) {
        exactBoundary();
        skippedScene();
        independentCasters();
        restoredClock();
        System.out.println("Inner Dragons cutscene cooldown: " + checks + " checks passed.");
    }

    private static void exactBoundary() {
        check(InnerDragonsCutsceneCooldown.COOLDOWN_TICKS == 10 * 60 * 20,
                "cutscene cooldown must be ten minutes at twenty ticks per second");
        check(InnerDragonsCutsceneCooldown.ready(0, 0), "new players must be ready immediately");
        long start = 45000;
        long next = InnerDragonsCutsceneCooldown.nextAllowedTick(start);
        check(next == 57000, "deadline must be ten minutes after the scene starts");
        check(!InnerDragonsCutsceneCooldown.ready(start, next), "a scene must consume its cooldown immediately");
        check(!InnerDragonsCutsceneCooldown.ready(start + InnerDragonsSequence.DURATION_TICKS, next),
                "ending the cinematic must not reset its cooldown");
        check(!InnerDragonsCutsceneCooldown.ready(next - 1, next), "last cooldown tick must still block replay");
        check(InnerDragonsCutsceneCooldown.ready(next, next), "exact deadline must permit a new scene");
        check(InnerDragonsCutsceneCooldown.ready(next + 1, next), "elapsed cooldown must remain ready");
        check(!InnerDragonsCutsceneCooldown.ready(-1, 0), "invalid negative server time must not start a scene");
        check(InnerDragonsCutsceneCooldown.nextAllowedTick(-1) == 12000,
                "negative input must not shorten the reserved cooldown");
        check(InnerDragonsCutsceneCooldown.nextAllowedTick(Long.MAX_VALUE - 1) == Long.MAX_VALUE,
                "overflow must not wrap a cooldown into an immediately available negative timestamp");
    }

    private static void skippedScene() {
        InnerDragonsSequence sequence = new InnerDragonsSequence();
        long firstStart = 100;
        check(sequence.update(20, 100, firstStart, true), "first low-health cycle should begin");
        long next = InnerDragonsCutsceneCooldown.nextAllowedTick(firstStart);
        check(sequence.isPlaying(firstStart), "first cinematic should be active");
        sequence.update(40, 100, 300, true);
        check(sequence.update(20, 100, 301, true), "healing must rearm the health threshold");
        check(!InnerDragonsCutsceneCooldown.ready(301, next), "health cycling must not bypass ten minutes");
        sequence.stop();
        check(!sequence.isPlaying(301), "a skipped cinematic must not delay the actual dragon summons");
        for (long tick = 302; tick <= next + 10; tick += 31) {
            check(!sequence.update(20, 100, tick, true),
                    "a skipped scene must not get queued while health remains low");
            check(!sequence.isPlaying(tick), "cooldown skips must leave the cinematic gate open for summons");
        }
        check(InnerDragonsCutsceneCooldown.ready(next + 11, next), "the cooldown should expire normally");
        check(!sequence.update(20, 100, next + 11, true),
                "cooldown expiry alone must not seize the camera while dragons are already active");
        sequence.update(30, 100, next + 12, true);
        check(sequence.update(29, 100, next + 13, true), "next downward crossing after expiry must play");
        check(InnerDragonsCutsceneCooldown.ready(next + 13, next), "new crossing should satisfy the cooldown");
    }

    private static void independentCasters() {
        long firstNext = InnerDragonsCutsceneCooldown.nextAllowedTick(1000);
        long secondNext = 0;
        check(!InnerDragonsCutsceneCooldown.ready(1001, firstNext), "first caster should be cooling down");
        check(InnerDragonsCutsceneCooldown.ready(1001, secondNext), "another caster must have their own cooldown");
        secondNext = InnerDragonsCutsceneCooldown.nextAllowedTick(1001);
        check(InnerDragonsCutsceneCooldown.ready(firstNext, firstNext), "first caster should become ready first");
        check(!InnerDragonsCutsceneCooldown.ready(firstNext, secondNext), "later caster must keep their own deadline");
    }

    private static void restoredClock() {
        long savedDeadline = InnerDragonsCutsceneCooldown.nextAllowedTick(5000);
        InnerDragonsSequence recreatedAbility = new InnerDragonsSequence();
        check(recreatedAbility.update(20, 100, 5100, true), "fresh ability instance sees low health");
        check(!InnerDragonsCutsceneCooldown.ready(5100, savedDeadline),
                "persisted deadline must block rejoin/respawn/ability replacement replays");
        recreatedAbility.stop();
        check(!recreatedAbility.isPlaying(5100), "restored cooldown must not leave a phantom cinematic delay");
        check(!InnerDragonsCutsceneCooldown.ready(savedDeadline - 1, savedDeadline),
                "restoring the timer must preserve every remaining tick");
        check(InnerDragonsCutsceneCooldown.ready(savedDeadline, savedDeadline),
                "restored timer must expire at its original deadline");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
        checks++;
    }
}
