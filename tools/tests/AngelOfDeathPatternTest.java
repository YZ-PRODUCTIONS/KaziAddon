import net.kazi.kazimod.abilities.YagiAwakening.AngelOfDeathPattern;

/** Source cadence and physical bounds for the assisted, alternating-hand fan. */
public final class AngelOfDeathPatternTest {
    private static int checks;
    private static void check(boolean value, String reason) {
        checks++;
        if (!value) throw new AssertionError(reason);
    }
    private static void near(double value, double expected, double tolerance, String reason) {
        check(Math.abs(value - expected) <= tolerance, reason);
    }
    private static double length(double[] vector) {
        return Math.sqrt(vector[0]*vector[0]+vector[1]*vector[1]+vector[2]*vector[2]);
    }
    private static double angle(double[] a, double[] b) {
        return Math.acos(Math.max(-1, Math.min(1,
                (a[0]*b[0]+a[1]*b[1]+a[2]*b[2])/(length(a)*length(b)))));
    }
    public static void main(String[] args) {
        cadence();
        launchFans();
        assistedFlight();
        finiteGuidanceArcs();
        System.out.println("Angel of death: " + checks + " source schedule, fan and bounded homing checks passed.");
    }
    private static void cadence() {
        int[] firstFrames={11,31,51,71,106}, counts={8,8,7,7,11}, firstTicks={21,34,47,61,84};
        int[] flashTicks={19,32,45,59,82};
        int shot=0, sameTick=0, previous=-1;
        check(AngelOfDeathPattern.SHOT_COUNT==41, "Five source bursts contain forty-one shots");
        check(AngelOfDeathPattern.flashCount()==5, "One hand-flash cue precedes each source burst");
        for(int volley=0;volley<5;volley++) {
            check(AngelOfDeathPattern.flashTick(volley)==flashTicks[volley], "Hand flash retains its source cadence");
            check(AngelOfDeathPattern.shotTick(shot)==firstTicks[volley], "First shot follows its hand flash");
            check(AngelOfDeathPattern.flashSide(volley)==(volley==4?0:volley%2==0?-1:1),
                    "The first four flashes alternate hands and the finale illuminates both");
            for(int lane=0;lane<counts[volley];lane++,shot++) {
                int tick=AngelOfDeathPattern.shotTick(shot);
                check(tick==Math.round((20+firstFrames[volley]+lane)*20.0F/30),
                        "Each absolute source frame is rounded independently without accumulated drift");
                check(AngelOfDeathPattern.volleyIndex(shot)==volley, "Each shot belongs to exactly one burst");
                check(tick>=previous, "A scheduler may drain ordered shots without missing a shot");
                if(tick==previous)sameTick++;
                check(AngelOfDeathPattern.side(shot)==(volley==4?lane%2==0?-1:1:volley%2==0?-1:1),
                        "Shots retain left/right/left/right then both-hand order");
                previous=tick;
            }
        }
        check(shot==41 && sameTick>0, "Quantization preserves all source shots, including shared-tick pairs");
        check(AngelOfDeathPattern.DURATION==107 && previous==91, "The final fan completes before the original recovery ends");
        check(AngelOfDeathPattern.MAX_LIFE==80, "Projectile lifetime remains bounded at the existing eighty ticks");
        for(int invalid:new int[]{-1,41,Integer.MAX_VALUE}) {
            try { AngelOfDeathPattern.shotTick(invalid); throw new AssertionError("Invalid shot accepted"); }
            catch(IndexOutOfBoundsException expected) { checks++; }
        }
    }
    private static void launchFans() {
        double minimumAngle=Math.PI, maximumAngle=0;
        for(int shot=0;shot<AngelOfDeathPattern.SHOT_COUNT;shot++) {
            double[] direction=AngelOfDeathPattern.initialDirection(shot);
            near(length(direction),1,1e-12,"All fan lanes are unit directions");
            check(direction[0]*AngelOfDeathPattern.side(shot)>0 && direction[2]>0,
                    "Each lane initially travels out from its hand and forward");
            near(direction[1],0,0,"Launch fan does not drive grounded casters' shots straight into the floor");
            double outward=Math.atan2(Math.abs(direction[0]),direction[2]);
            minimumAngle=Math.min(minimumAngle,outward);maximumAngle=Math.max(maximumAngle,outward);
            double[] unassisted=AngelOfDeathPattern.steer(direction[0],direction[1],direction[2],
                    -direction[0],3,-direction[2],5.999F);
            near(angle(direction,unassisted),0,1e-7,"Targets cannot flatten the initial six-tick launch fan");
        }
        check(maximumAngle-minimumAngle>Math.toRadians(35), "The launch is a broad fan, not a stack of parallel bolts");
        for(int lane=0;lane<8;lane++) {
            double[] left=AngelOfDeathPattern.initialDirection(lane), right=AngelOfDeathPattern.initialDirection(lane+8);
            near(left[0],-right[0],1e-12,"The first two volleys mirror their outward directions");
            near(left[2],right[2],1e-12,"Mirrored volleys retain the same forward travel");
        }
    }
    private static void assistedFlight() {
        double maxTurn=AngelOfDeathPattern.MAX_TURN_RADIANS;
        for(double[] start:new double[][]{{0,0,1},{0,1,0},{1,0,0},{-.9,.15,.3}}) {
            for(double[] target:new double[][]{{0,0,100},{0,0,-100},{0,-100,0},{80,30,20},{0,0,0}}) {
                double[] velocity=start;
                for(int tick=0;tick<80;tick++) {
                    double[] next=AngelOfDeathPattern.steer(velocity[0],velocity[1],velocity[2],target[0],target[1],target[2],tick);
                    near(length(next),1.45,1e-10,"Aim assistance must not accelerate or slow projectiles");
                    double turned=angle(velocity,next);
                    check(turned<=maxTurn+1e-7,"No tick may exceed the bounded eight-degree turn");
                    if(tick<6)near(turned,0,1e-7,"Steering cannot begin before the launch delay ends");
                    velocity=next;
                }
                if(length(target)>0)near(angle(velocity,target),0,1e-7,"Bounded steering eventually converges, including opposite/vertical aims");
            }
        }
        for(double invalid:new double[]{Double.NaN,Double.POSITIVE_INFINITY,Double.NEGATIVE_INFINITY}) {
            check(length(AngelOfDeathPattern.steer(invalid,0,1,0,0,1,8))==0,"Invalid velocity is safely rejected");
            check(length(AngelOfDeathPattern.steer(0,0,1,invalid,0,1,8))==0,"Invalid target offset is safely rejected");
        }
        check(length(AngelOfDeathPattern.steer(0,0,0,0,0,1,8))==0,"A stationary projectile cannot produce NaN steering");
        check(length(AngelOfDeathPattern.steer(0,0,1,0,0,1,Float.NaN))==0,"Invalid clocks do not steer");
    }

    private static void finiteGuidanceArcs() {
        for(double distance:new double[]{12,32,48}) {
            for(int shot=0;shot<AngelOfDeathPattern.SHOT_COUNT;shot++) {
                AngelOfDeathPattern.Guidance guidance=new AngelOfDeathPattern.Guidance();
                double[] initial=AngelOfDeathPattern.initialDirection(shot);
                double[] velocity={initial[0]*1.45,initial[1]*1.45,initial[2]*1.45};
                double[] position={AngelOfDeathPattern.side(shot)*1.1,0,.6};
                double totalTurn=0, closest=Double.POSITIVE_INFINITY;
                boolean bent=false, sawComplete=false;
                for(int tick=0;tick<80;tick++) {
                    double[] offset={-position[0],-position[1],distance-position[2]};
                    closest=Math.min(closest,length(offset));
                    double[] next=guidance.steer(velocity[0],velocity[1],velocity[2],offset[0],offset[1],offset[2],tick);
                    double turned=angle(velocity,next);
                    totalTurn+=turned;
                    near(length(next),1.45,1e-10,"Stateful assistance keeps the authored speed");
                    check(turned<=Math.toRadians(8)+1e-7,"Stateful assistance never exceeds the per-tick turn limit");
                    check(totalTurn<=Math.PI+1e-5,"A beam may make at most one inward half-turn, never repeated orbits");
                    if(tick<6) near(turned,0,1e-7,"Guidance preserves every lane's initial launch fan");
                    if(tick==6) {
                        check(turned>.001,"Both nearby and distant targets receive an initial inward correction");
                        check(angle(next,offset)<angle(velocity,offset),"The first correction turns toward the goal, even when it starts behind the outward velocity");
                        bent=true;
                    }
                    if(sawComplete) {
                        check(guidance.isComplete(),"Completed guidance cannot reactivate");
                        near(turned,0,1e-7,"A completed beam flies straight beyond its target");
                    }
                    sawComplete=guidance.isComplete();
                    velocity=next;
                    for(int axis=0;axis<3;axis++)position[axis]+=velocity[axis];
                }
                check(bent && guidance.isComplete(),"Every tested fan lane curves inward, then stops guiding within its finite life");
                if(distance>=32)check(closest<1,"Distant goals converge naturally within the beam's collision neighborhood");
                double[] after=guidance.steer(velocity[0],velocity[1],velocity[2],-velocity[0],-velocity[1],-velocity[2],90);
                near(angle(velocity,after),0,1e-7,"A target moving behind an exhausted beam cannot make it circle back");
            }
        }
        AngelOfDeathPattern.Guidance moving=new AngelOfDeathPattern.Guidance();
        double[] velocity={-1.2,0,.8};
        double totalTurn=0;
        for(int age=0;age<100;age++) {
            // A moving target then an absent offset exercises finite steering without reacquisition.
            double tx=20*Math.sin(age*.6),ty=3*Math.cos(age*.4),tz=25*Math.cos(age*.6);
            if(age>=50 && age<60)tx=ty=tz=0;
            double[] next=moving.steer(velocity[0],velocity[1],velocity[2],tx,ty,tz,age);
            totalTurn+=angle(velocity,next);
            near(length(next),1.45,1e-10,"Moving or dropped target offsets retain finite constant-speed flight");
            check(totalTurn<=Math.PI+1e-5,"A moving target cannot replenish the inward-turn budget");
            velocity=next;
        }
        check(moving.isComplete(),"Moving-target guidance still finishes instead of orbiting");
        AngelOfDeathPattern.Guidance atGoal=new AngelOfDeathPattern.Guidance();
        atGoal.steer(0,0,1,0,0,0,2);
        check(!atGoal.isComplete(),"A temporary zero aim offset during launch cannot prematurely exhaust guidance");
        atGoal.steer(0,0,1,0,0,0,6);
        check(atGoal.isComplete(),"Reaching the goal after launch permanently ends guidance");
    }
}
