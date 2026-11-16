import net.kazi.kazimod.abilities.cinematics.ChaosBeamFinaleLift;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.potion.Effects;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;

/** Production ascent, collision and lifecycle behavior, without launching a Minecraft client. */
public final class ChaosBeamFinaleLiftTest {
    private static int checks;

    public static void main(String[] args) {
        curve();
        ascentAndLanding();
        ceilings();
        gravityOwnership();
        cleanup();
        System.out.println("Chaos Beam finale lift: " + checks + " checks passed.");
    }

    private static void curve() {
        equal(ChaosBeamFinaleLift.height(-40),0,"Negative age stays at ground level");
        equal(ChaosBeamFinaleLift.height(0),0,"Ascent starts at its origin");
        equal(ChaosBeamFinaleLift.height(144),3,"Halfway rise is three blocks");
        equal(ChaosBeamFinaleLift.height(288),6,"Full whiteout reaches six blocks");
        equal(ChaosBeamFinaleLift.height(1000),6,"Ascent is bounded after completion");
        for(double invalid:new double[]{Double.NaN,Double.POSITIVE_INFINITY,Double.NEGATIVE_INFINITY})
            equal(ChaosBeamFinaleLift.height(invalid),0,"Invalid age cannot poison position");
        double previous=0;
        for(double age=0;age<=312;age+=.25) {
            double height=ChaosBeamFinaleLift.height(age);
            require(height>=previous && height<=6,"Height increases smoothly and never exceeds six blocks");
            require(height-previous<.008,"No abrupt step in the ascent curve");
            previous=height;
        }
    }

    private static void ascentAndLanding() {
        ServerPlayerEntity caster=new ServerPlayerEntity(new World());
        caster.passenger=caster.jumping=caster.sprinting=true;
        caster.xxa=caster.yya=caster.zza=1;
        caster.motion=new Vector3d(1,-3,1);caster.fallDistance=25;
        ChaosBeamFinaleLift lift=new ChaosBeamFinaleLift(caster);
        double previous=caster.getY();
        for(int age=0;age<312;age++) {
            lift.tick(age);
            equal(caster.getY(),64+ChaosBeamFinaleLift.height(age),"Caster follows the shared rise curve");
            require(caster.getY()-previous<=.08,"Per-tick ascent is capped");
            require(caster.gravity,"Gravity disabled during ascent");
            equal(caster.getX(),2,"No horizontal drift on X");equal(caster.getZ(),3,"No horizontal drift on Z");
            equal(caster.motion.x+caster.motion.y+caster.motion.z,0,"No residual velocity during scene");
            equal(caster.fallDistance,0,"No fall damage accumulated during scene");
            previous=caster.getY();
        }
        require(!caster.passenger&&!caster.jumping&&!caster.sprinting,"Caster dismounts and movement inputs stop");
        equal(caster.xxa+caster.yya+caster.zza,0,"Movement impulses are cleared");
        require(caster.teleports==0,"No per-tick player teleport spam");
        lift.finish(true);
        require(!caster.gravity&&caster.getPersistentData().isEmpty(),"Owned gravity and recovery marker are released");
        equal(caster.getY(),70,"Completion retains the real elevated position");
        require(caster.teleports==1,"Normal completion resyncs the caster once");
        require(caster.effects.size()==1&&caster.effects.get(0).effect==Effects.SLOW_FALLING
                &&caster.effects.get(0).ticks==100,"Elevated caster receives a five-second safe landing");
    }

    private static void ceilings() {
        World world=new World();world.ceilingY=66.25;
        LivingEntity caster=new LivingEntity(world);
        ChaosBeamFinaleLift lift=new ChaosBeamFinaleLift(caster);
        for(int age=0;age<288;age++) {
            lift.tick(age);
            require(caster.getY()<=66.25,"Collision-aware ascent cannot pass through a ceiling");
        }
        equal(caster.getY(),66.25,"Low ceiling limits reached height");
        world.ceilingY=Double.POSITIVE_INFINITY;
        lift.tick(288);
        equal(caster.getY(),66.33,"Removing obstruction cannot release a large accumulated launch");
        lift.finish(false);
        require(!caster.gravity,"Interrupted obstructed ascent restores gravity");
        require(caster.teleports==0,"Cancellation does not teleport the caster");
    }

    private static void gravityOwnership() {
        LivingEntity alreadyFlying=new LivingEntity(new World());alreadyFlying.gravity=true;
        ChaosBeamFinaleLift foreign=new ChaosBeamFinaleLift(alreadyFlying);
        for(int age=0;age<100;age++)foreign.tick(age);
        foreign.finish(true);
        require(alreadyFlying.gravity,"Pre-existing no-gravity state survives cleanup");
        require(alreadyFlying.effects.isEmpty(),"An already-floating caster needs no landing effect");
        require(alreadyFlying.getPersistentData().isEmpty(),"Foreign gravity is never tagged as owned");

        LivingEntity beamHandoff=new LivingEntity(new World());beamHandoff.gravity=true;
        ChaosBeamFinaleLift handoff=new ChaosBeamFinaleLift(beamHandoff);
        beamHandoff.gravity=false; // Beam removal occurs after scene creation, before its first tick.
        handoff.tick(1);
        require(beamHandoff.gravity&&!beamHandoff.getPersistentData().isEmpty(),"First tick acquires released beam gravity");
        handoff.finish(false);
        require(!beamHandoff.gravity,"Beam handoff does not leave no-gravity stuck on");

        LivingEntity saved=new LivingEntity(new World());
        new ChaosBeamFinaleLift(saved).tick(1);
        ChaosBeamFinaleLift.recover(saved);
        require(!saved.gravity&&saved.getPersistentData().isEmpty(),"Saved owned lock recovers without its original scene");
        ChaosBeamFinaleLift.recover(alreadyFlying);
        require(alreadyFlying.gravity,"Login recovery does not clear unrelated gravity");
    }

    private static void cleanup() {
        for(int lifecycle=0;lifecycle<3;lifecycle++) {
            ServerPlayerEntity caster=new ServerPlayerEntity(new World());
            ChaosBeamFinaleLift lift=new ChaosBeamFinaleLift(caster);
            for(int age=0;age<160;age++)lift.tick(age);
            if(lifecycle==0)caster.alive=false;
            else if(lifecycle==1)caster.removed=true;
            else caster.level=new World();
            caster.motion=new Vector3d(2,3,4);
            lift.finish(false);
            require(!caster.gravity&&caster.getPersistentData().isEmpty(),"Death/removal/dimension change releases owned gravity");
            require(caster.effects.isEmpty()&&caster.teleports==0,"Departed body is never given a landing effect or teleported");
            equal(caster.motion.y,3,"Cleanup does not alter motion in a new body/world");
        }
    }

    private static void equal(double actual,double expected,String message) {
        require(Math.abs(actual-expected)<1e-8,message+": "+actual+" != "+expected);
    }
    private static void require(boolean condition,String message) {
        checks++;
        if(!condition)throw new AssertionError(message);
    }
}
