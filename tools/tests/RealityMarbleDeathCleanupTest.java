package net.kazi.kazimod.abilities.SupaRework;

import java.util.Deque;
import java.util.Map;
import java.util.UUID;

/** Tests production lifecycle/restoration methods without needing a running game. */
public class RealityMarbleDeathCleanupTest {
    private static int checks;
    private static final Block STONE = new Block("stone"), PLACED = new Block("player_placed");
    private static final BlockPos A = new BlockPos(0, 64, 0), B = new BlockPos(1, 64, 0),
            C = new BlockPos(2, 64, 0), D = new BlockPos(3, 64, 0);

    public static void main(String[] args) {
        for (RealityMarbleAbility.Mode mode : new RealityMarbleAbility.Mode[]{
                RealityMarbleAbility.Mode.MUGEN_NO_KENSEI, RealityMarbleAbility.Mode.INFINITE_CREATION_OF_SWORDS}) {
            deathDuringConstruction(mode);
            deathDuringRestoration(mode);
            instanceOnlyFallback(mode);
            dimensionChangeCleanup(mode, false);
            dimensionChangeCleanup(mode, true);
            dimensionChangeWithoutEquippedAbility(mode);
        }
        missingAbilityUsesOriginalWorld();
        canceledAndClientDeaths();
        chargeAndLogoutCleanup();
        System.out.println("Reality Marble death cleanup: " + checks + " checks passed");
    }

    private static RealityMarbleAbility setup(ServerPlayerEntity owner, RealityMarbleAbility.Mode mode) {
        RealityMarbleAbility ability = new RealityMarbleAbility();
        ability.activeMode = mode;
        owner.abilities = new IAbilityData(ability);
        ability.continuousComponent.active = true;
        if (mode == RealityMarbleAbility.Mode.MUGEN_NO_KENSEI) ability.firstModeCenter = A;
        else ability.secondModeCenter = A;
        RealityMarbleAbility.beginMarbleTracking(owner, A);
        for (BlockPos pos : new BlockPos[]{A, B, C, D}) owner.level.setBlock(pos, STONE.defaultBlockState(), 3);
        for (BlockPos pos : new BlockPos[]{A, B, C}) queue(ability).add(pos);
        convert(ability, owner);
        return ability;
    }

    private static Deque<BlockPos> queue(RealityMarbleAbility ability) {
        return ability.activeMode == RealityMarbleAbility.Mode.MUGEN_NO_KENSEI
                ? ability.firstModeReplacementQueue : ability.secondModeReplacementQueue;
    }

    private static Map<BlockPos, BlockState> states(RealityMarbleAbility ability) {
        return ability.activeMode == RealityMarbleAbility.Mode.MUGEN_NO_KENSEI
                ? ability.firstModeReplacedBlocks : ability.secondModeReplacedBlocks;
    }

    private static Deque<BlockPos> restoreQueue(RealityMarbleAbility ability) {
        return ability.activeMode == RealityMarbleAbility.Mode.MUGEN_NO_KENSEI
                ? ability.firstModeRestorationQueue : ability.secondModeRestorationQueue;
    }

    private static void convert(RealityMarbleAbility ability, LivingEntity owner) {
        ability.processFirstModeReplacement(owner);
        ability.processSecondModeReplacement(owner);
    }

    private static void deathDuringConstruction(RealityMarbleAbility.Mode mode) {
        World world = new World();
        ServerPlayerEntity owner = new ServerPlayerEntity(world), listener = new ServerPlayerEntity(world);
        RealityMarbleAbility ability = setup(owner, mode);
        Block expected = mode == RealityMarbleAbility.Mode.MUGEN_NO_KENSEI
                ? KaziBlocks.CUSTOM_COARSE_SAND.get() : KaziBlocks.CUSTOM_COARSE_DIRT.get();
        check(world.getBlockState(A).getBlock() == expected, "production conversion uses expected custom block");
        queue(ability).add(D); // Further terrain remains queued when the owner dies.
        world.setBlock(C, PLACED.defaultBlockState(), 3);
        SphereEntity first = ability.firstModeSphere = new SphereEntity();
        SphereEntity second = ability.secondModeSphere = new SphereEntity();
        RealityMarbleWeaponEntity sword = new RealityMarbleWeaponEntity();
        RealityMarbleGearEntity ground = new RealityMarbleGearEntity(), aerial = new RealityMarbleGearEntity();
        ability.weaponDisplays.add(sword); ability.gearDisplays.add(ground); ability.aerialGearDisplays.add(aerial);
        ability.musicSession = UUID.randomUUID(); ability.musicListeners.add(listener);
        owner.lock = ability.chargeMovementLock = new EffectInstance(3, 0);

        RealityMarbleAbility.LogoutCleanupHandler.onCasterDeath(new LivingDeathEvent(owner));
        check(world.getBlockState(A).getBlock() == STONE && world.getBlockState(B).getBlock() == STONE,
                "death restores all converted blocks synchronously");
        check(world.getBlockState(C).getBlock() == PLACED, "cleanup preserves a replacement made by someone else");
        check(world.getBlockState(D).getBlock() == STONE, "unprocessed terrain is untouched");
        check(!ability.continuousComponent.active && ability.continuousComponent.stops == 1, "hold stops once");
        assertCleared(ability, owner);
        check(!first.alive && !second.alive && !sword.alive && !ground.alive && !aerial.alive,
                "both spheres and all display types disappear");
        check(listener.fades == 1 && ability.musicSession == null && ability.musicListeners.isEmpty(), "music fades once");
        check(owner.lock == null, "owned charge movement lock clears");
        int writes = world.writes;
        convert(ability, owner);
        RealityMarbleAbility.processRestoration(owner, restoreQueue(ability), states(ability));
        RealityMarbleAbility.LogoutCleanupHandler.onCasterDeath(new LivingDeathEvent(owner));
        check(world.writes == writes && listener.fades == 1, "later ticks and duplicate cleanup cannot repaint terrain");
    }

    private static void deathDuringRestoration(RealityMarbleAbility.Mode mode) {
        World world = new World();
        ServerPlayerEntity owner = new ServerPlayerEntity(world);
        RealityMarbleAbility ability = setup(owner, mode);
        ability.continuousComponent.stopContinuity(owner);
        // Simulate the first normal restoration batch already having finished A.
        world.setBlock(A, states(ability).remove(A), 3);
        restoreQueue(ability).remove(A);
        check(!restoreQueue(ability).isEmpty(), "there is unfinished normal restoration");
        RealityMarbleAbility.LogoutCleanupHandler.onCasterDeath(new LivingDeathEvent(owner));
        check(world.getBlockState(A).getBlock() == STONE && world.getBlockState(B).getBlock() == STONE
                && world.getBlockState(C).getBlock() == STONE, "death finishes remaining restoration");
        assertCleared(ability, owner);
    }

    private static void instanceOnlyFallback(RealityMarbleAbility.Mode mode) {
        World world = new World();
        ServerPlayerEntity owner = new ServerPlayerEntity(world);
        RealityMarbleAbility ability = setup(owner, mode);
        RealityMarbleAbility.ACTIVE_MARBLES.remove(owner.getUUID());
        RealityMarbleAbility.LogoutCleanupHandler.onCasterDeath(new LivingDeathEvent(owner));
        check(world.getBlockState(A).getBlock() == STONE && world.getBlockState(C).getBlock() == STONE,
                "instance-owned states restore if global tracking is unavailable");
        assertCleared(ability, owner);
    }

    private static void missingAbilityUsesOriginalWorld() {
        for (boolean missingCapability : new boolean[]{false, true}) {
            World original = new World(), destination = new World();
            ServerPlayerEntity owner = new ServerPlayerEntity(original);
            setup(owner, RealityMarbleAbility.Mode.MUGEN_NO_KENSEI);
            owner.abilities = missingCapability ? null : new IAbilityData(null);
            owner.level = destination;
            destination.setBlock(A, PLACED.defaultBlockState(), 3);
            RealityMarbleAbility.LogoutCleanupHandler.onCasterDeath(new LivingDeathEvent(owner));
            check(original.getBlockState(A).getBlock() == STONE && original.getBlockState(C).getBlock() == STONE,
                    "tracking fallback restores original world without equipped ability/capability");
            check(destination.getBlockState(A).getBlock() == PLACED, "current dimension remains unchanged");
            check(!RealityMarbleAbility.ACTIVE_MARBLES.containsKey(owner.getUUID()), "fallback releases tracking");
        }
    }

    private static void canceledAndClientDeaths() {
        World world = new World();
        ServerPlayerEntity owner = new ServerPlayerEntity(world);
        RealityMarbleAbility ability = setup(owner, RealityMarbleAbility.Mode.MUGEN_NO_KENSEI);
        LivingDeathEvent canceled = new LivingDeathEvent(owner); canceled.canceled = true;
        int writes = world.writes;
        RealityMarbleAbility.LogoutCleanupHandler.onCasterDeath(canceled);
        check(world.writes == writes && ability.continuousComponent.active, "canceled death keeps active marble");
        world.isClientSide = true;
        RealityMarbleAbility.LogoutCleanupHandler.onCasterDeath(new LivingDeathEvent(owner));
        check(world.writes == writes && ability.continuousComponent.active, "client event does not mutate marble");
        world.isClientSide = false;
        RealityMarbleAbility.LogoutCleanupHandler.onPlayerLogout(new PlayerEvent.PlayerLoggedOutEvent(owner));
        assertCleared(ability, owner);
        check(world.getBlockState(A).getBlock() == STONE, "logout still restores terrain");
        writes = world.writes;
        RealityMarbleAbility.LogoutCleanupHandler.onPlayerLogout(new PlayerEvent.PlayerLoggedOutEvent(owner));
        check(world.writes == writes, "logout cleanup remains idempotent");
    }

    private static void dimensionChangeCleanup(RealityMarbleAbility.Mode mode, boolean restoring) {
        World original = new World(), destination = new World();
        ServerPlayerEntity owner = new ServerPlayerEntity(original), listener = new ServerPlayerEntity(original);
        RealityMarbleAbility ability = setup(owner, mode);
        queue(ability).add(D);
        SphereEntity first = ability.firstModeSphere = new SphereEntity();
        SphereEntity second = ability.secondModeSphere = new SphereEntity();
        RealityMarbleWeaponEntity sword = new RealityMarbleWeaponEntity();
        RealityMarbleGearEntity ground = new RealityMarbleGearEntity(), aerial = new RealityMarbleGearEntity();
        ability.weaponDisplays.add(sword); ability.gearDisplays.add(ground); ability.aerialGearDisplays.add(aerial);
        ability.musicSession = UUID.randomUUID(); ability.musicListeners.add(listener);
        owner.lock = ability.chargeMovementLock = new EffectInstance(3, 0);
        if (restoring) {
            ability.continuousComponent.stopContinuity(owner);
            original.setBlock(A, states(ability).remove(A), 3);
            restoreQueue(ability).remove(A);
            check(!restoreQueue(ability).isEmpty(), "dimension change interrupts unfinished normal restoration");
        }
        destination.setBlock(A, KaziBlocks.CUSTOM_COARSE_SAND.get().defaultBlockState(), 3);
        destination.setBlock(B, PLACED.defaultBlockState(), 3);
        destination.setBlock(C, KaziBlocks.CUSTOM_COARSE_DIRT.get().defaultBlockState(), 3);
        int destinationWrites = destination.writes;
        owner.level = destination; // Forge fires this callback after the player has moved worlds.

        RealityMarbleAbility.LogoutCleanupHandler.onPlayerChangedDimension(new PlayerEvent.PlayerChangedDimensionEvent(owner));
        check(original.getBlockState(A).getBlock() == STONE && original.getBlockState(B).getBlock() == STONE
                && original.getBlockState(C).getBlock() == STONE, "dimension change restores old-world terrain");
        check(original.getBlockState(D).getBlock() == STONE, "old-world pending conversion is discarded");
        check(destination.getBlockState(A).getBlock() == KaziBlocks.CUSTOM_COARSE_SAND.get()
                && destination.getBlockState(B).getBlock() == PLACED
                && destination.getBlockState(C).getBlock() == KaziBlocks.CUSTOM_COARSE_DIRT.get(),
                "destination blocks, including another marble's blocks, remain untouched");
        check(!ability.continuousComponent.active && ability.continuousComponent.stops == 1,
                "dimension change ends continuity once");
        check(!first.alive && !second.alive && !sword.alive && !ground.alive && !aerial.alive,
                "dimension change removes spheres and every display type");
        check(listener.fades == 1 && ability.musicSession == null && ability.musicListeners.isEmpty(),
                "dimension change fades original-world listeners exactly once");
        check(owner.lock == null, "dimension change clears its movement lock");
        assertCleared(ability, owner);
        convert(ability, owner);
        RealityMarbleAbility.processRestoration(owner, restoreQueue(ability), states(ability));
        RealityMarbleAbility.LogoutCleanupHandler.onPlayerChangedDimension(new PlayerEvent.PlayerChangedDimensionEvent(owner));
        check(destination.writes == destinationWrites, "no deferred cleanup or repeated callback writes into destination");
    }

    private static void dimensionChangeWithoutEquippedAbility(RealityMarbleAbility.Mode mode) {
        for (boolean missingCapability : new boolean[]{false, true}) {
            World original = new World(), destination = new World();
            ServerPlayerEntity owner = new ServerPlayerEntity(original);
            setup(owner, mode);
            owner.abilities = missingCapability ? null : new IAbilityData(null);
            destination.setBlock(A, KaziBlocks.CUSTOM_COARSE_DIRT.get().defaultBlockState(), 3);
            int writes = destination.writes;
            owner.level = destination;
            RealityMarbleAbility.LogoutCleanupHandler.onPlayerChangedDimension(new PlayerEvent.PlayerChangedDimensionEvent(owner));
            check(original.getBlockState(A).getBlock() == STONE && original.getBlockState(C).getBlock() == STONE,
                    "dimension callback restores tracked terrain without equipped ability/capability");
            check(destination.writes == writes && destination.getBlockState(A).getBlock() == KaziBlocks.CUSTOM_COARSE_DIRT.get(),
                    "missing-ability dimension fallback does not touch destination marble");
            check(!RealityMarbleAbility.ACTIVE_MARBLES.containsKey(owner.getUUID()), "dimension fallback releases tracking");
        }
    }

    private static void chargeAndLogoutCleanup() {
        World world = new World();
        ServerPlayerEntity owner = new ServerPlayerEntity(world);
        RealityMarbleAbility ability = new RealityMarbleAbility(); owner.abilities = new IAbilityData(ability);
        ability.chargeComponent.active = true;
        ability.musicSession = UUID.randomUUID(); ability.musicListeners.add(owner);
        owner.lock = ability.chargeMovementLock = new EffectInstance(100, 0);
        RealityMarbleAbility.LogoutCleanupHandler.onCasterDeath(new LivingDeathEvent(owner));
        check(!ability.chargeComponent.active && ability.chargeComponent.forceStops == 1, "charge is force-stopped");
        check(world.writes == 0 && ability.continuousComponent.stops == 0, "charge death never spawns terrain");
        check(owner.fades == 1, "charge death fades soundtrack");
        check(owner.lock != null, "longer unrelated movement lock is preserved");
        assertCleared(ability, owner);
    }

    private static void assertCleared(RealityMarbleAbility ability, LivingEntity owner) {
        check(ability.firstModeReplacementQueue.isEmpty() && ability.secondModeReplacementQueue.isEmpty(), "replacement queues cleared");
        check(ability.firstModeRestorationQueue.isEmpty() && ability.secondModeRestorationQueue.isEmpty(), "restoration queues cleared");
        check(ability.firstModeReplacedBlocks.isEmpty() && ability.secondModeReplacedBlocks.isEmpty(), "instance snapshots cleared");
        check(ability.firstModeCenter == null && ability.secondModeCenter == null, "centers cleared");
        check(ability.firstModeSphere == null && ability.secondModeSphere == null, "sphere references cleared");
        check(ability.weaponDisplays.isEmpty() && ability.gearDisplays.isEmpty() && ability.aerialGearDisplays.isEmpty(), "display lists cleared");
        check(!RealityMarbleAbility.ACTIVE_MARBLES.containsKey(owner.getUUID()), "global snapshot released");
    }

    private static void check(boolean condition, String label) {
        checks++;
        if (!condition) throw new AssertionError(label);
    }
}
