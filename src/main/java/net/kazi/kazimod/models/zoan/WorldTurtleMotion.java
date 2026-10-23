package net.kazi.kazimod.models.zoan;

import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;

/** Render-time state per turtle, with frame-rate-independent locomotion blending. */
public final class WorldTurtleMotion {
    private float lastAge = -1, phase, flight, movement, fast, bank, lookPitch, lookYaw;
    private float transitionAt = -100, strikeAt = -100, previousAttack;
    private boolean airborne;
    private float shield,spin,spinAngle;
    private final Map<String,CerberusRig.Pose> pose = new HashMap<>();
    private final Map<String,CerberusRig.Clip> clips = new HashMap<>();
    public float shield() { return shield; }
    public void shieldAt(boolean active,float age) { shield(active,lastAge<0?1:clamp(age-lastAge,0,2)); }
    public void shield(boolean active,float dt) { shield += ((active?1:0)-shield)*(1-(float)Math.exp(-Math.max(0,dt)/3)); }
    public void spinAt(boolean active,float age) {
        float dt=lastAge<0?1:clamp(age-lastAge,0,2);
        spin+=((active?1:0)-spin)*(1-(float)Math.exp(-dt/2));
        if(active)spinAngle=(spinAngle+48*spin*dt+540)%360-180;
        else spinAngle*=(float)Math.exp(-dt/3);
    }

    public Map<String, CerberusRig.Pose> sample(CerberusRig rig, float age, float amount,
            boolean inAir, boolean sprinting, float yaw, float pitch, float turnRate, float attack) {
        float dt = lastAge < 0 ? 1 : clamp(age-lastAge, 0, 2);
        if (lastAge < 0) { flight = inAir ? 1 : 0; airborne = inAir; }
        lastAge = age;
        if (airborne != inAir) { transitionAt = age; airborne = inAir; }
        float blend = 1-(float)Math.exp(-dt/7);
        flight += ((inAir ? 1 : 0)-flight)*blend;
        movement += (clamp(amount*2,0,1)-movement)*blend;
        fast += ((sprinting ? 1 : 0)-fast)*blend;
        bank += (clamp(turnRate*3,-10,10)*flight-bank)*blend;
        lookPitch += (clamp(pitch,-30,30)-lookPitch)*blend;
        lookYaw += (clamp(yaw,-30,30)-lookYaw)*blend;
        phase = (phase + dt/20*(1/3.6F+fast*(1/2.4F-1/3.6F)))%1;
        if(clips.isEmpty())for(CerberusRig.Clip c:rig.animations)clips.put(c.name,c);
        for(CerberusRig.Pose p:pose.values()){Arrays.fill(p.position,0);Arrays.fill(p.rotation,0);}
        apply("idle",age/20,1);
        apply("fly",phase*3.6F,flight*(1-fast)*(1-shield));
        apply("fast_fly",phase*2.4F,flight*fast*(1-shield));
        apply("walk",phase*3,movement*(1-flight)*(1-shield));
        float transition=(age-transitionAt)/20;
        if (transition<1.4F) apply(airborne?"takeoff":"land",transition,(airborne?flight:1-flight)*(1-shield));
        if (attack>0 && (previousAttack<=0 || attack<previousAttack)) strikeAt=age;
        previousAttack=attack;
        if (age-strikeAt<12) apply("bite",(age-strikeAt)/20,1-shield);
        CerberusRig.Pose root=pose.computeIfAbsent("World Turtle",k->new CerberusRig.Pose());
        root.rotation[2]+=bank;
        root.rotation[0]-=lookPitch*.25F*flight;
        root.rotation[0]*=1-spin;root.rotation[2]*=1-spin;
        root.rotation[1]+=spinAngle;
        CerberusRig.Pose head=pose.computeIfAbsent("Head",k->new CerberusRig.Pose());
        head.rotation[1]-=lookYaw*.45F;
        head.rotation[0]-=lookPitch*.35F;
        tuck("Neck",0,-18,55);
        tuck("Front flipper swimming",62,2,10);tuck("Front_flipper_swimming2",-62,2,10);
        tuck("Rear flipper left",40,2,-10);tuck("Rear flipper right",-40,2,-10);tuck("Tail",0,3,-24);
        return pose;
    }
    private void tuck(String bone,float x,float y,float z){CerberusRig.Pose p=pose.computeIfAbsent(bone,k->new CerberusRig.Pose());p.position[0]+=x*shield;p.position[1]+=y*shield;p.position[2]+=z*shield;}
    private void apply(String name,float time,float weight){
        if(weight<.001F)return;
        CerberusRig.Clip c=clips.get(name);time=c.loop?(time%c.length+c.length)%c.length:clamp(time,0,c.length);
        float frame=time/c.length*64;int index=Math.min(63,(int)frame);float t=frame-index;
        for(CerberusRig.Track track:c.tracks){
            CerberusRig.Pose p=pose.computeIfAbsent(track.bone,k->new CerberusRig.Pose());
            // The exporter samples each channel uniformly at 65 keys; direct indexing avoids scans.
            for(int start=0;start<track.keys.length;start+=65){CerberusRig.Key a=track.keys[start+index],b=track.keys[start+index+1];
                float[] dst=a.channel.equals("position")?p.position:p.rotation;
                for(int k=0;k<3;k++)dst[k]+=weight*(a.value[k]+(b.value[k]-a.value[k])*t);
            }
        }
    }
    private static float clamp(float x,float min,float max) { return Math.max(min,Math.min(max,x)); }
}
