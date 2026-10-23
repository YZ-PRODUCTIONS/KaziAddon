package net.kazi.kazimod.models.zoan;

import java.util.HashMap;
import java.util.Map;

/** Per-entity animation state. Geometry and clips remain shared and immutable. */
public final class MammothMotion {
    public static final int SWEEP = 0, STOMP = 1, TRUNK_SHOT = 2, STOMP_FINISH = 3;
    public static final class Input {
        public float age, limbSwing, limbAmount, yaw, pitch, verticalSpeed, swing, charge,stampedeProgress;
        public boolean grounded, swimming, running, crouching, charging, stomping, armed, rideable,stampeding,stampedeCharge,vacuum,rightHand = true;
    }
    private final boolean hybrid;
    private final Map<String, CerberusRig.Pose> pose = new HashMap<>();
    private float lastAge = Float.NaN, firstAge, lastLimb, phase, movement, run, air, crouch, lookYaw, lookPitch;
    private float lastSwing, attackAt = -1000, landAt = -1000, chargeWeight, heldCharge, readyWeight;
    private final float[] signals = {-1000, -1000, -1000, -1000};
    private float vacuumWeight,rushWeight,rushChargeWeight,rushProgress;
    private boolean wasGrounded = true, attackRight;
    public MammothMotion(boolean hybrid) { this.hybrid = hybrid; }
    private static float clamp(float f,float a,float b) { return Math.max(a,Math.min(b,f)); }
    private static float smooth(float f) { f=clamp(f,0,1);return f*f*(3-2*f); }
    public void signal(int action,float age) { if(action>=0&&action<signals.length)signals[action]=age; }
    public Map<String,CerberusRig.Pose> sample(CerberusRig rig,Input in) {
        if(Float.isNaN(lastAge)||in.age<lastAge||in.age-lastAge>20) {
            firstAge=in.age;lastAge=in.age;lastLimb=in.limbSwing;wasGrounded=in.grounded;
            phase=movement=run=air=crouch=0;
        }
        float dt=clamp(in.age-lastAge,0,3),response=1-(float)Math.exp(-dt/2.8F);
        lastAge=in.age;
        for(CerberusRig.Pose p:pose.values()) { java.util.Arrays.fill(p.rotation,0);java.util.Arrays.fill(p.position,0); }
        movement+=(clamp(in.limbAmount*1.8F,0,1)-movement)*response;
        run+=((in.running?1:0)-run)*response;
        air+=((!in.grounded&&!in.swimming?1:0)-air)*response;
        crouch+=((in.crouching?1:0)-crouch)*response;
        lookYaw+=(clamp(in.yaw,hybrid?-60:-28,hybrid?60:28)-lookYaw)*response;
        lookPitch+=(clamp(in.pitch,-30,30)-lookPitch)*response;
        // Giant forms need a longer step cycle than the unscaled player gait.
        phase=(phase+(in.stampeding?dt*.9F:clamp(in.limbSwing-lastLimb,0,3))/(hybrid?14-run*4:24-run*8))%1;
        lastLimb=in.limbSwing;
        if(in.grounded&&!wasGrounded&&!in.swimming)landAt=in.age;
        wasGrounded=in.grounded;
        if(in.swing>0&&(lastSwing<=0||in.swing<lastSwing)) {
            attackAt=in.age;attackRight=in.rightHand;
            if(in.armed)signal(TRUNK_SHOT,in.age);
        }
        lastSwing=in.swing;
        rig.apply(pose,"idle",in.age/20,1-movement*.65F);
        float locomotion=movement*(1-air)*(1-crouch*.45F)*(in.stomping?.12F:1);
        if(in.swimming)rig.apply(pose,"swim",in.age/20,.6F+.4F*movement);
        else {
            rig.apply(pose,"walk",phase*2.15F,locomotion*(1-run));
            rig.apply(pose,"run",phase*1.05F,locomotion*run);
            rig.apply(pose,"airborne",1,air);
            if(in.age-landAt<9)rig.apply(pose,"land",(in.age-landAt)/20,1);
        }
        rig.apply(pose,"crouch",1,crouch);
        if(in.rideable)rig.apply(pose,"rideable",1,1);
        float sweepTime=(in.age-signals[SWEEP])/20,shotTime=(in.age-signals[TRUNK_SHOT])/20;
        boolean sweep=sweepTime>=0&&sweepTime<.85F,shot=shotTime>=0&&shotTime<.72F;
        chargeWeight+=((in.charging?1:0)-chargeWeight)*response;
        if(in.charging)heldCharge=in.charge;
        readyWeight+=((in.armed?1:0)-readyWeight)*response;
        vacuumWeight+=((in.vacuum?1:0)-vacuumWeight)*response;
        rushWeight+=((in.stampeding?1:0)-rushWeight)*response;
        rushChargeWeight+=((in.stampedeCharge?1:0)-rushChargeWeight)*response;
        if(in.stampedeCharge)rushProgress=in.stampedeProgress;
        float finishTime=(in.age-signals[STOMP_FINISH])/20;
        if(rushWeight>.01F)rig.apply(pose,"stampede_run",in.age/20,rushWeight);
        if(finishTime>=0&&finishTime<.8F)rig.apply(pose,"stomp_finish",finishTime,1);
        else if(vacuumWeight>.01F)rig.apply(pose,"vacuum",in.age/20,vacuumWeight);
        else if(rushChargeWeight>.01F)rig.apply(pose,"stampede_charge",rushProgress,rushChargeWeight*(1-rushWeight));
        else if(sweep)rig.apply(pose,"sweep",sweepTime,1);
        else if(shot)rig.apply(pose,"trunk_shot",shotTime,1-smooth((shotTime-.4F)/.32F));
        else if(in.charging||chargeWeight>.01F)rig.apply(pose,"sweep_charge",heldCharge,chargeWeight);
        else if(in.stomping) {
            float stamp=signals[STOMP];
            rig.apply(pose,"stomp",stamp>-500?(in.age-stamp)/20:in.age/20,1);
        } else if(readyWeight>.01F)rig.apply(pose,"trunk_ready",1,readyWeight);
        else if(in.age-attackAt<9.6F)rig.apply(pose,attackRight?"attack_right":"attack_left",(in.age-attackAt)/20,1);
        if(in.age-firstAge<22)rig.apply(pose,"transform",(in.age-firstAge)/20,1);
        CerberusRig.Pose head=pose.computeIfAbsent("head",k->new CerberusRig.Pose());
        float lookWeight=(in.charging||sweep||shot||in.stomping||in.stampedeCharge)?.25F:in.vacuum?.7F:1;
        head.rotation[0]-=lookPitch*lookWeight*(hybrid?1:.55F);
        head.rotation[1]+=lookYaw*lookWeight;
        return pose;
    }
}
