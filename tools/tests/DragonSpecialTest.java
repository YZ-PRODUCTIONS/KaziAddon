import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import net.kazi.kazimod.abilities.NagiAwakening.DragonKind;
import net.kazi.kazimod.abilities.NagiAwakening.DragonSpecial;
import net.kazi.kazimod.abilities.NagiAwakening.DragonSpecialCycle;

/** Executes the production special profiles and clocks; does not simulate Forge combat. */
public final class DragonSpecialTest {
    private static int checks;

    public static void main(String[] args) {
        profiles();
        for (DragonSpecial special : DragonSpecial.values()) {
            cadence(special);
            missedTarget(special);
        }
        idleAndInvalidStarts();
        independentSummons();
        System.out.println("Dragon specials: " + checks + " checks passed against actual profiles and cooldown clocks.");
    }

    private static void profiles() {
        check(DragonKind.values().length == 8 && DragonSpecial.values().length == 8,
                "all eight dragons must have a special");
        DragonSpecial[] expected = {
                DragonSpecial.NULLIFYING_ROAR, DragonSpecial.RENDING_BITE,
                DragonSpecial.VENOM_SPRAY, DragonSpecial.MIND_BEAM,
                DragonSpecial.SOULFIRE_BREATH, DragonSpecial.CRYSTAL_PRISON,
                DragonSpecial.THUNDER_LANCE, DragonSpecial.HEAVENLY_SLAM
        };
        EnumSet<DragonSpecial> assigned = EnumSet.noneOf(DragonSpecial.class);
        Set<String> names = new HashSet<>();
        for (DragonKind kind : DragonKind.values()) {
            check(kind.special == expected[kind.ordinal()], "wrong special for " + kind.id);
            check(assigned.add(kind.special), "two dragons share a special");
            check(names.add(kind.special.displayName) && !kind.special.displayName.trim().isEmpty(),
                    "specials need distinct readable names");
            check(Double.isFinite(kind.special.range) && kind.special.range > 0
                    && kind.special.range <= 32, "invalid special range");
            check(kind.special.windupTicks > 0 && kind.special.windupTicks < 300,
                    "windup must be finite and shorter than cooldown");
            check(DragonKind.byId(kind.ordinal()).special == kind.special,
                    "network kind must preserve its special");
        }
        check(assigned.size() == DragonSpecial.values().length, "unassigned special");
        check(DragonSpecialCycle.COOLDOWN_TICKS == 15 * 20, "special cooldown must be fifteen seconds");
    }

    private static void cadence(DragonSpecial special) {
        DragonSpecialCycle cycle = new DragonSpecialCycle(0);
        check(cycle.ready() && cycle.start(special), "first cast should be ready");
        check(cycle.cooldownRemaining() == 300 && cycle.windingUp(), "cast must reserve its whole cooldown");
        check(!cycle.start(special), "repeat start must not overlap a windup");
        check(!cycle.consumeRelease(), "windup must not release immediately");
        advance(cycle, special.windupTicks - 1);
        check(!cycle.consumeRelease(), "windup released one tick early");
        cycle.tick();
        check(cycle.consumeRelease(), "windup failed to release on its final tick");
        check(!cycle.consumeRelease() && !cycle.windingUp(), "release must be consumed exactly once");
        check(cycle.cooldownRemaining() == 300 - special.windupTicks,
                "windup must count toward start-to-start cooldown");
        advance(cycle, 299 - special.windupTicks);
        check(cycle.cooldownRemaining() == 1 && !cycle.ready() && !cycle.start(special),
                "second cast was allowed before fifteen seconds");
        cycle.tick();
        check(cycle.ready() && cycle.start(special), "second cast must be available after exactly 300 ticks");
        check(cycle.cooldownRemaining() == 300, "second cast did not reserve a fresh cooldown");
    }

    private static void missedTarget(DragonSpecial special) {
        DragonSpecialCycle cycle = new DragonSpecialCycle(0);
        cycle.start(special);
        int elapsed = special.windupTicks / 2;
        advance(cycle, elapsed);
        cycle.abandonTarget();
        check(!cycle.windingUp() && !cycle.consumeRelease(), "lost target must not release an attack");
        check(cycle.cooldownRemaining() == 300 - elapsed && !cycle.ready(),
                "losing a target must not reset or erase the cooldown");
        advance(cycle, 300 - elapsed);
        check(cycle.ready(), "missed attack must become ready on the original deadline");
    }

    private static void idleAndInvalidStarts() {
        DragonSpecialCycle cycle = new DragonSpecialCycle(0);
        check(!cycle.start(null) && cycle.ready(), "invalid profile must not consume readiness");
        advance(cycle, 1000);
        check(cycle.ready() && cycle.cooldownRemaining() == 0 && !cycle.consumeRelease(),
                "waiting without a target must keep readiness without generating attacks");
        cycle.abandonTarget();
        check(cycle.ready(), "discarding no target must not create a cooldown");
        DragonSpecialCycle negativeDelay = new DragonSpecialCycle(-20);
        check(negativeDelay.ready(), "invalid negative initial delay must not stall a summon");
    }

    private static void independentSummons() {
        DragonSpecialCycle[] cycles = new DragonSpecialCycle[8];
        int[] firstCast = new int[8];
        java.util.Arrays.fill(firstCast, -1);
        for (int i = 0; i < cycles.length; i++) cycles[i] = new DragonSpecialCycle(20 + i * 5);
        for (int tick = 0; tick <= 55; tick++) {
            for (int i = 0; i < cycles.length; i++) {
                if (tick > 0) cycles[i].tick();
                if (firstCast[i] < 0 && cycles[i].ready()) {
                    firstCast[i] = tick;
                    cycles[i].start(DragonKind.byId(i).special);
                }
                cycles[i].consumeRelease();
            }
        }
        for (int i = 0; i < cycles.length; i++) {
            check(firstCast[i] == 20 + i * 5, "initial stagger was lost for dragon " + i);
            check(cycles[i].cooldownRemaining() == 300 - (55 - firstCast[i]),
                    "one dragon changed another dragon's cooldown");
        }
        int untouched = cycles[1].cooldownRemaining();
        cycles[0].tick();
        cycles[0].abandonTarget();
        check(cycles[1].cooldownRemaining() == untouched, "dragon clocks must not share mutable state");
    }

    private static void advance(DragonSpecialCycle cycle, int ticks) {
        for (int tick = 0; tick < ticks; tick++) cycle.tick();
    }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }
}
