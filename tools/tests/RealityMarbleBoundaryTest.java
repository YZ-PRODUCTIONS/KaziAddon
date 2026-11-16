import net.kazi.kazimod.abilities.SupaRework.RealityMarbleBoundary;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.world.WorldEvent;
import xyz.pixelatedw.mineminenomi.entities.SphereEntity;

public final class RealityMarbleBoundaryTest {
    private static int checks;
    private static void check(boolean result, String message) {
        checks++;
        if (!result) throw new AssertionError(message);
    }
    private static SphereEntity sphere(Object world, String texture) {
        SphereEntity sphere = new SphereEntity(world);
        sphere.textures = new ResourceLocation[]{new ResourceLocation("kazimod", texture)};
        RealityMarbleBoundary.joined(new EntityJoinWorldEvent(sphere));
        return sphere;
    }
    public static void main(String[] args) {
        Object world = new Object();
        ServerPlayerEntity player = new ServerPlayerEntity(world);
        check(player.spectatorCalls == 0, "Construction must not query the uninitialized player game mode");
        Vector3d movement = new Vector3d(5, 0, 0);
        player.spectator = null;
        check(RealityMarbleBoundary.collide(player, movement) == movement, "Initial movement needs the same safe guard");
        check(RealityMarbleBoundary.clampTeleportDestination(player, movement) == movement,
                "Dash clipping must not query an uninitialized player");
        check(player.spectatorCalls == 0, "Unticked entities must not run spectator logic");
        player.spectator = false;
        player.tickCount = 1;
        check(!RealityMarbleBoundary.blocksTeleport(player, 20, 0, 0), "No marble means free teleport");
        check(RealityMarbleBoundary.clampTeleportDestination(player, new Vector3d(20,0,0)).x == 20,
                "Without a marble the dash keeps its full range");
        SphereEntity blue = sphere(world, "textures/skyboxes/reality_marble_blue_sky.png");
        Vector3d dashEnd = RealityMarbleBoundary.clampTeleportDestination(player, new Vector3d(20,0,0));
        check(dashEnd.x > 8 && dashEnd.x < 10, "A long dash ends just inside the shell");
        check(!RealityMarbleBoundary.blocksTeleport(player, dashEnd.x, dashEnd.y, dashEnd.z),
                "The position hook accepts the clipped dash");
        player.setPos(dashEnd.x, dashEnd.y, dashEnd.z);
        check(player.x == dashEnd.x, "A clipped dash moves the player instead of silently failing");
        check(RealityMarbleBoundary.clampTeleportDestination(player, new Vector3d(30,0,0)).x <= 10,
                "Repeated dashes cannot cross the wall");
        player.x = 0;
        check(RealityMarbleBoundary.clampTeleportDestination(player, new Vector3d(2,0,0)).x == 2,
                "A dash wholly inside the marble is unchanged");
        check(RealityMarbleBoundary.blocksTeleport(player, 20, 0, 0), "An initialized player cannot teleport out");
        check(!RealityMarbleBoundary.blocksTeleport(player, 2, 0, 0), "Teleport inside remains allowed");
        player.setPos(20, 0, 0);
        check(player.x == 0, "The injected position hook rejects escape");
        player.x = 8;
        Vector3d clipped = RealityMarbleBoundary.collide(player, movement);
        check(clipped.x > 0 && clipped.x < movement.x, "Walking into the shell is clipped");
        player.x = 12;
        check(!RealityMarbleBoundary.blocksTeleport(player, 0, 0, 0), "Teleport entry remains allowed");
        check(RealityMarbleBoundary.collide(player, movement.scale(-1)).x == -5, "Walking into the dome remains allowed");
        player.x = 8;
        player.spectator = true;
        check(RealityMarbleBoundary.clampTeleportDestination(player, new Vector3d(20,0,0)).x == 20,
                "Spectator dashes retain their exemption");
        check(!RealityMarbleBoundary.blocksTeleport(player, 20, 0, 0), "Spectators can teleport out");
        check(RealityMarbleBoundary.collide(player, movement) == movement, "Spectators can move through the shell");
        player.spectator = false;
        player.noPhysics = true;
        check(RealityMarbleBoundary.collide(player, movement) == movement, "No-physics movement remains exempt");
        check(RealityMarbleBoundary.clampTeleportDestination(player, new Vector3d(20,0,0)).x < 10,
                "No-physics does not bypass the existing living teleport boundary");
        player.noPhysics = false;
        player.level = new Object();
        check(RealityMarbleBoundary.clampTeleportDestination(player, new Vector3d(20,0,0)).x == 20,
                "Dash destinations ignore marbles in other worlds");
        check(!RealityMarbleBoundary.blocksTeleport(player, 20, 0, 0), "Other worlds do not share boundaries");
        check(RealityMarbleBoundary.collide(player, movement).x == 5, "Other-world movement is unchanged");
        player.level = world;
        blue.alive = false;
        check(!RealityMarbleBoundary.blocksTeleport(player, 20, 0, 0), "Dead spheres stop trapping players");
        SphereEntity unrelated = sphere(world, "textures/other.png");
        check(!RealityMarbleBoundary.blocksTeleport(player, 20, 0, 0), "Unrelated sphere textures do not trap players");
        unrelated.textures = null;
        check(!RealityMarbleBoundary.blocksTeleport(player, 20, 0, 0), "Uninitialized sphere textures are ignored");
        sphere(world, "textures/skyboxes/reality_marble_orange_sky.png");
        check(RealityMarbleBoundary.blocksTeleport(player, 20, 0, 0), "The orange marble still contains players");
        Entity item = new Entity(world);
        item.tickCount = 1;
        item.x = 8;
        check(!RealityMarbleBoundary.blocksTeleport(item, 20, 0, 0), "Nonliving direct reposition remains exempt");
        check(RealityMarbleBoundary.collide(item, movement).x < 5, "Nonliving movement still collides");
        Vector3d zero = new Vector3d(0, 0, 0);
        check(RealityMarbleBoundary.collide(player, zero) == zero, "Zero movement remains stable");
        ServerPlayerEntity joiningInside = new ServerPlayerEntity(world);
        check(joiningInside.spectatorCalls == 0, "Player construction is safe even with a live marble");
        RealityMarbleBoundary.unloaded(new WorldEvent.Unload(world));
        check(!RealityMarbleBoundary.blocksTeleport(player, 20, 0, 0), "Unloading the world clears its boundaries");
        check(!RealityMarbleBoundary.blocksTeleport(null, 20, 0, 0), "An absent mover is harmless");
        System.out.println(checks + " Reality Marble boundary and login regression checks passed.");
    }
}
