package net.kazi.kazimod.models.zoan;

/** Render-time jump phases; no movement, collision or ability state is modified. */
public final class CerberusJumpMotion {
    private boolean initialized, wasGrounded, airborne;
    private float takeoff, landing = -1000, lastAge, landingStart, landingWeight;
    private float time, weight;
    public void update(float age, boolean grounded, boolean excluded, double velocityY) {
        if (excluded) { initialized=false; airborne=false; landing=-1000; time=weight=0; return; }
        float dt=initialized?clamp(age-lastAge,0,2):0;
        lastAge=age;
        if(!initialized){initialized=true;wasGrounded=grounded;takeoff=age;time=velocityY>0?0:.52F;}
        if(!grounded){
            if(wasGrounded){takeoff=age;time=velocityY>.05?0:.52F;}
            airborne=true;landing=-1000;
            float target;
            if(velocityY>0 && age-takeoff<6) target=.08F+(age-takeoff)*.05F;
            else if(velocityY>0) target=.38F+.14F*(1-clamp((float)velocityY/.3F,0,1));
            else target=.52F+.16F*clamp((float)-velocityY/.3F,0,1);
            time+=clamp(target-time,0,dt*.1F);
            weight=clamp(weight+dt/3,0,1);
        }else if(airborne){
            airborne=false;landing=age;landingStart=time;landingWeight=weight;
        }
        if(grounded && landing>-1000){
            float elapsed=age-landing;
            time=elapsed<2?landingStart+(.82F-landingStart)*elapsed/2:Math.min(1.1F,.82F+(elapsed-2)*.035F);
            weight=landingWeight*(1-clamp((elapsed-6)/4,0,1));
            if(elapsed>=10){landing=-1000;weight=0;}
        }
        wasGrounded=grounded;
    }
    public float time(){return time;}
    public float weight(){return weight;}
    private static float clamp(float value,float min,float max){return Math.max(min,Math.min(value,max));}
}
