import java.util.*;
import net.kazi.kazimod.abilities.Yagi.*;
import net.kazi.kazimod.abilities.YagiAwakening.*;
import net.kazi.kazimod.config.KaziConfig;
import net.minecraft.entity.player.PlayerEntity;
import xyz.pixelatedw.mineminenomi.api.abilities.*;
import xyz.pixelatedw.mineminenomi.data.entity.ability.IAbilityData;

/** Executes production progression; fixtures model only native ability storage and callbacks. */
public final class YagiAwakeningProgressionTest {
    private static final AbilityCore<?>[] BASE = {HornRamAbility.INSTANCE, MountainLeapAbility.INSTANCE,
            MountainGoatFormAbility.INSTANCE};
    private static final AbilityCore<?>[] AWAKENED = {StarBlazingAbility.INSTANCE, ShockerBreakerAbility.INSTANCE,
            ChaosSaberAbility.INSTANCE, ChaosBusterAbility.INSTANCE};
    private static final AbilityCore<IAbility> HAKI = new AbilityCore<>("haki");
    private static final AbilityCore<IAbility> OTHER_FRUIT = new AbilityCore<>("other fruit");
    private static int checks;

    public static void main(String[] args) {
        retirementSet();
        existingAwakenedPlayer();
        freshAwakening();
        alreadyMigratedPlayer();
        retirementWithoutNewUnlocks();
        eligibilityNoOps();
        System.out.println("Yagi production progression: " + checks + " retirement, preservation, unlock and lifecycle checks passed.");
    }

    private static void retirementSet() {
        for (AbilityCore<?> core : BASE) check(YagiAwakeningProgression.isRetired(core), "Every retired Goat combat/form move must be removed: " + core);
        check(!YagiAwakeningProgression.isRetired(AstralFlightAbility.INSTANCE), "Astral Flight remains available after awakening");
        for (AbilityCore<?> core : AWAKENED) check(!YagiAwakeningProgression.isRetired(core), "Awakening moves must never retire: " + core);
        check(!YagiAwakeningProgression.isRetired(HAKI), "Unrelated haki is not retired");
        check(!YagiAwakeningProgression.isRetired(OTHER_FRUIT), "Other fruits are not retired");
        check(!YagiAwakeningProgression.isRetired(null), "Null cores are safe");
    }

    private static void existingAwakenedPlayer() {
        Fixture f = new Fixture();
        List<Move> retired = new ArrayList<>();
        for (int i = 0; i < BASE.length; i++) {
            retired.add(f.equip(i * 2, BASE[i]));
            retired.add(f.equip(i * 2 + 1, BASE[i]));
            retired.add(f.passive(BASE[i]));
            f.data.unlocked.add(BASE[i]);
        }
        Move star = f.equip(9, AWAKENED[0]);
        Move saber = f.equip(12, AWAKENED[2]);
        Move haki = f.equip(10, HAKI);
        Move otherFruit = f.equip(15, OTHER_FRUIT);
        Move unrelatedPassive = f.passive(HAKI);
        Move flight = f.passive(AstralFlightAbility.INSTANCE);
        f.data.unlocked.addAll(Arrays.asList(AWAKENED[0], AWAKENED[2], HAKI, OTHER_FRUIT, AstralFlightAbility.INSTANCE));
        List<IAbility> beforeSlots = new ArrayList<>(f.data.slots);
        List<IAbility> slotList = f.data.slots;
        YagiAwakeningProgression.sync(f.user);

        check(f.data.slots == slotList && f.data.slots.size() == beforeSlots.size(), "Retirement must not rebuild or compact the hotbar");
        for (int slot = 0; slot < beforeSlots.size(); slot++) {
            IAbility before = beforeSlots.get(slot);
            check(f.data.slots.get(slot) == (before != null && YagiAwakeningProgression.isRetired(before.getCore()) ? null : before),
                    "Only retired entries may change, at their original slot: " + slot);
        }
        for (Move move : retired) {
            check(move.stopCount == 1 && !move.running, "Every duplicate or passive base instance must stop exactly once before removal");
            check(move.cooldown == 137 && move.paused && move.disabled, "Retiring a move must not overwrite its existing state");
        }
        for (AbilityCore<?> core : BASE) {
            check(!f.data.unlocked.contains(core), "Retired move unlock must be removed: " + core);
            check(!f.data.passives.containsKey(core), "Retired passive must be removed: " + core);
        }
        check(f.data.passiveCallbacks == 3, "Only the three retired combat/form passives get remove callbacks");
        for (Move kept : Arrays.asList(star, saber, haki, otherFruit, unrelatedPassive, flight)) assertUntouched(kept);
        check(f.data.passives.size() == 2 && f.data.passives.get(HAKI) == unrelatedPassive, "Unrelated passive keeps the same instance");
        check(f.data.passives.get(AstralFlightAbility.INSTANCE) == flight && f.data.unlocked.contains(AstralFlightAbility.INSTANCE),
                "Awakening must preserve Astral Flight's exact passive instance and unlock");
        assertAwakenedUnlocks(f);
        check(f.data.unlocked.contains(HAKI) && f.data.unlocked.contains(OTHER_FRUIT), "Unrelated unlocks remain available");
        check(f.data.unlockAdds == 2 && f.data.progressionAdds == 2, "Only the two missing awakening moves are granted through progression");
        check(f.user.syncPackets == 1 && f.user.lastPacket.data == f.data && f.user.lastPacket.entityId == f.user.getId(), "Changed state syncs once to its owner with the correct capability");
        int mutations = f.data.mutations;
        for (int login = 0; login < 3; login++) YagiAwakeningProgression.sync(f.user);
        check(f.user.syncPackets == 1 && f.data.mutations == mutations, "Repeated login is idempotent and must not resend or rewrite unchanged state");
        for (Move kept : Arrays.asList(star, saber, haki, otherFruit, unrelatedPassive, flight)) assertUntouched(kept);
        check(f.data.passives.get(AstralFlightAbility.INSTANCE) == flight && f.data.unlocked.contains(AstralFlightAbility.INSTANCE),
                "Repeated login must preserve Astral Flight's instance and unlock");
    }

    private static void freshAwakening() {
        Fixture f = new Fixture();
        YagiAwakeningProgression.sync(f.user);
        assertAwakenedUnlocks(f);
        check(f.data.unlockAdds == 4 && f.data.progressionAdds == 4, "Fresh awakened players receive all four missing moves");
        check(f.data.slots.stream().allMatch(Objects::isNull) && f.data.passives.isEmpty(), "Granting awakening unlocks must not auto-bind moves or create unrelated passives");
        check(f.user.syncPackets == 1, "Fresh unlocks send one sync");
    }

    private static void alreadyMigratedPlayer() {
        Fixture f = new Fixture();
        f.data.unlocked.addAll(Arrays.asList(AWAKENED));
        List<Move> existing = new ArrayList<>();
        for (int i = 0; i < AWAKENED.length; i++) existing.add(f.equip(i * 3, AWAKENED[i]));
        YagiAwakeningProgression.sync(f.user);
        check(f.user.syncPackets == 0 && f.data.mutations == 0, "An already migrated player must cause no mutation or sync");
        for (int i = 0; i < existing.size(); i++) {
            check(f.data.slots.get(i * 3) == existing.get(i), "Existing awakening slots keep their exact instances");
            assertUntouched(existing.get(i));
        }
    }

    private static void retirementWithoutNewUnlocks() {
        Fixture f = new Fixture();
        f.data.unlocked.addAll(Arrays.asList(AWAKENED));
        Move stale = f.equip(6, HornRamAbility.INSTANCE);
        // Legacy saves can contain a bound/passive instance with no corresponding unlock.
        Move staleFlight = f.passive(AstralFlightAbility.INSTANCE);
        YagiAwakeningProgression.sync(f.user);
        check(f.data.slots.get(6) == null && stale.stopCount == 1, "Remove stale bindings even if their unlock is already absent");
        check(f.data.passives.get(AstralFlightAbility.INSTANCE) == staleFlight, "Preserve an existing Astral Flight passive even when its unlock is absent");
        assertUntouched(staleFlight);
        check(!f.data.unlocked.contains(AstralFlightAbility.INSTANCE), "Progression leaves native Astral Flight unlock repair to its existing lifecycle");
        check(f.user.syncPackets == 1 && f.data.unlockAdds == 0, "Retirement-only changes still send exactly one sync");
    }

    private static void eligibilityNoOps() {
        YagiAwakeningProgression.sync(null);
        Fixture missing = new Fixture();
        missing.user.data = null;
        YagiAwakeningProgression.sync(missing.user);
        check(missing.user.syncPackets == 0, "Missing capability is a safe no-op");
        for (String state : Arrays.asList("unawakened", "wrong fruit", "disabled config", "client")) {
            Fixture f = new Fixture();
            Move base = f.equip(3, HornRamAbility.INSTANCE);
            Move passive = f.passive(AstralFlightAbility.INSTANCE);
            f.data.unlocked.addAll(Arrays.asList(BASE));
            Set<AbilityCore<?>> oldUnlocks = new HashSet<>(f.data.unlocked);
            if (state.equals("unawakened")) f.user.awakened = false;
            if (state.equals("wrong fruit")) f.user.hasYagi = false;
            if (state.equals("disabled config")) KaziConfig.INSTANCE.disableFruitChanges.value = true;
            if (state.equals("client")) f.user.level.isClientSide = true;
            try { YagiAwakeningProgression.sync(f.user); }
            finally { KaziConfig.INSTANCE.disableFruitChanges.value = false; }
            check(f.user.syncPackets == 0 && f.data.mutations == 0, state + " must not mutate or sync");
            check(f.data.slots.get(3) == base && f.data.passives.get(AstralFlightAbility.INSTANCE) == passive,
                    state + " keeps its original base instances and slots");
            check(f.data.unlocked.equals(oldUnlocks), state + " must neither add awakening unlocks nor retire base unlocks");
            assertUntouched(base);
            assertUntouched(passive);
        }
    }

    private static void assertAwakenedUnlocks(Fixture f) {
        for (AbilityCore<?> core : AWAKENED) check(f.data.unlocked.contains(core), "Missing awakening unlock: " + core);
    }
    private static void assertUntouched(Move move) {
        check(move.stopCount == 0 && move.removeCount == 0 && move.running, "Retained instances, including Astral Flight, must not be stopped or removed");
        check(move.cooldown == 137 && move.paused && move.disabled, "Keep original cooldown, pause and disabled state");
    }
    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }

    private static final class Move implements IAbility {
        final AbilityCore<?> core;
        int stopCount, removeCount, cooldown = 137;
        boolean running = true, paused = true, disabled = true;
        Move(AbilityCore<?> core) { this.core = core; }
        public AbilityCore<?> getCore() { return core; }
        public void stop() { stopCount++; running = false; }
        public void onRemove() { removeCount++; running = false; }
    }
    private static final class Data implements IAbilityData {
        final List<IAbility> slots = new ArrayList<>(Collections.nCopies(16, null));
        final Map<AbilityCore<?>, IAbility> passives = new LinkedHashMap<>();
        final Set<AbilityCore<?>> unlocked = new HashSet<>();
        int mutations, unlockAdds, progressionAdds, passiveCallbacks;
        public List<IAbility> getEquippedAndPassiveAbilities() {
            List<IAbility> result = new ArrayList<>(slots);
            result.addAll(passives.values());
            return result;
        }
        // Native removal clears one matching slot without compacting the list.
        public boolean removeEquippedAbility(AbilityCore<?> core) {
            for (int slot = 0; slot < slots.size(); slot++) {
                Move move = (Move) slots.get(slot);
                if (move != null && move.core == core) {
                    check(move.stopCount == 1, "Stop every retired equipped instance before removing it");
                    slots.set(slot, null); mutations++; return true;
                }
            }
            return false;
        }
        public IAbility getPassiveAbility(AbilityCore<?> core) { return passives.get(core); }
        public void removePassiveAbility(AbilityCore<?> core) {
            Move move = (Move) passives.remove(core);
            if (move != null) {
                check(move.stopCount == 1, "Stop retired passive state before the native remove callback");
                move.onRemove(); passiveCallbacks++; mutations++;
            }
        }
        public boolean hasUnlockedAbility(AbilityCore<?> core) { return unlocked.contains(core); }
        public void removeUnlockedAbility(AbilityCore<?> core) { if (unlocked.remove(core)) mutations++; }
        public void addUnlockedAbility(AbilityCore<?> core, AbilityUnlock reason) {
            if (unlocked.add(core)) { mutations++; unlockAdds++; }
            if (reason == AbilityUnlock.PROGRESSION) progressionAdds++;
        }
    }
    private static final class Fixture {
        final PlayerEntity user = new PlayerEntity();
        final Data data = new Data();
        Fixture() { user.data = data; }
        Move equip(int slot, AbilityCore<?> core) { Move move = new Move(core); data.slots.set(slot, move); return move; }
        Move passive(AbilityCore<?> core) { Move move = new Move(core); data.passives.put(core, move); return move; }
    }
}
