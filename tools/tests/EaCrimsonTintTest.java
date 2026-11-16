import net.kazi.kazimod.client.goru.EaCrimsonTint;
import net.kazi.kazimod.entities.EaRuptureShape;
import net.kazi.kazimod.entities.WindsRaptureShape;

/** Runs the production overlay clock without Minecraft or a rendering context. */
public final class EaCrimsonTintTest {
    private static int checks;

    private static void check(boolean condition,String message) {
        checks++;
        if(!condition)throw new AssertionError(message);
    }

    private static void equal(float actual,float expected,String message) {
        check(Math.abs(actual-expected)<.000001F,message+": "+actual+" != "+expected);
    }

    public static void main(String[] args) {
        check(EaCrimsonTint.RGB==0x780018,"deep crimson palette");
        check(EaCrimsonTint.MAX_OPACITY>EaCrimsonTint.WINDS_OPACITY,"Winds remains milder than Enuma");
        check(EaCrimsonTint.MAX_OPACITY<1&&EaCrimsonTint.WINDS_OPACITY>0,"both overlays remain translucent");
        profile(false,EaRuptureShape.CHARGE_TICKS,
                EaRuptureShape.RELEASE_TICKS+EaRuptureShape.FADE_TICKS,EaCrimsonTint.MAX_OPACITY);
        profile(true,WindsRaptureShape.CHARGE_TICKS,
                WindsRaptureShape.RELEASE_TICKS+WindsRaptureShape.FADE_TICKS,EaCrimsonTint.WINDS_OPACITY);
        EaCrimsonTint defaultProfile=new EaCrimsonTint(),explicitEnuma=new EaCrimsonTint(false);
        defaultProfile.startCharge();explicitEnuma.startCharge();
        for(int tick=0;tick<=EaRuptureShape.CHARGE_TICKS;tick++) {
            equal(defaultProfile.opacity(.5F),explicitEnuma.opacity(.5F),"default profile retains Enuma timing");
            defaultProfile.tick();explicitEnuma.tick();
        }
        System.out.println("Ea crimson tint: "+checks+" checks passed; both variants clear instantly at attack end while preserving charge fade-in.");
    }

    private static void profile(boolean winds,int charge,int attackEnd,float peak) {
        EaCrimsonTint tint=new EaCrimsonTint(winds);
        equal(tint.opacity(0),0,"new overlay is clear");
        tint.tick();
        check(!tint.isActive(),"inactive clocks do not start themselves");

        tint.startCharge();
        equal(tint.opacity(0),0,"charge starts clear");
        float previous=0;
        for(int tick=0;tick<charge;tick++) {
            float alpha=tint.opacity(0);
            check(alpha>=previous&&alpha<=peak,"charge builds smoothly without overshoot");
            if(tick==10)equal(alpha,peak/2,"charge fade-in still reaches half strength after half a second");
            if(tick>=20)equal(alpha,peak,"charge fade-in still reaches full strength after one second");
            previous=alpha;
            tint.tick();
        }
        equal(tint.opacity(0),peak,"charge waits for release at its original peak");

        tint.trigger();
        for(int tick=0;tick<attackEnd;tick++) {
            tint.cancel();
            equal(tint.opacity(0),peak,"released tint stays full through the existing beam lifetime");
            equal(tint.opacity(.99F),peak,"there is no premature fade during the final partial frame");
            if(tick==attackEnd-1)equal(tint.opacity(1),0,"the exact attack-end render boundary clears immediately");
            tint.tick();
        }
        equal(tint.opacity(0),0,"attack completion has no lingering hold or screen fade");
        check(!tint.isActive(),"attack completion releases the overlay session immediately");
        for(int tick=0;tick<100;tick++)tint.tick();
        equal(tint.opacity(.5F),0,"completed overlay remains clear");

        // A release packet received without its charge still uses the correct profile.
        tint.trigger();
        equal(tint.opacity(0),peak,"release-only packet starts the correct intensity");
        for(float invalid:new float[]{Float.NaN,Float.POSITIVE_INFINITY,Float.NEGATIVE_INFINITY,-10,10}) {
            float alpha=tint.opacity(invalid);
            check(Float.isFinite(alpha)&&alpha>=0&&alpha<=peak,"invalid partial ticks stay bounded");
        }
        equal(tint.opacity(0),peak,"render queries never advance the clock");
        tint.clear();
        check(!tint.isActive()&&tint.opacity(.5F)==0,"world cleanup immediately removes the tint");

        tint.startCharge();
        for(int tick=0;tick<10;tick++)tint.tick();
        equal(tint.opacity(0),peak/2,"mid-charge cancellation setup");
        tint.cancel();
        check(!tint.isActive()&&tint.opacity(0)==0,"a canceled charge also clears immediately");
        tint.cancel();tint.tick();
        equal(tint.opacity(.5F),0,"duplicate cancellation cannot restart the overlay");

        tint.startCharge();
        for(int tick=0;tick<charge+200-1;tick++)tint.tick();
        check(tint.isActive(),"orphaned charge keeps the original grace period");
        tint.tick();
        check(!tint.isActive()&&tint.opacity(0)==0,"orphaned charge clears at its timeout without a fade tail");

        EaCrimsonTint lowFps=new EaCrimsonTint(winds),highFps=new EaCrimsonTint(winds);
        lowFps.startCharge();highFps.startCharge();
        for(int tick=0;tick<charge+attackEnd+5;tick++) {
            if(tick==charge){lowFps.trigger();highFps.trigger();}
            lowFps.opacity(.5F);
            for(int frame=0;frame<240;frame++)highFps.opacity(frame/240F);
            equal(lowFps.opacity(0),highFps.opacity(0),"charge and exact removal timing are frame-rate independent");
            lowFps.tick();highFps.tick();
        }
        check(!lowFps.isActive()&&!highFps.isActive(),"both render rates finish at the attack deadline");
    }
}
