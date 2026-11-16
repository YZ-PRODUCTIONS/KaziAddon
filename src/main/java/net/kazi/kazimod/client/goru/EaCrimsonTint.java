package net.kazi.kazimod.client.goru;

import net.kazi.kazimod.entities.EaRuptureShape;
import net.kazi.kazimod.entities.WindsRaptureShape;

/** Charge buildup and release tint, cleared immediately when the beam disappears. */
public final class EaCrimsonTint {
    public static final int RGB=0x780018;
    public static final float MAX_OPACITY=.56F;
    public static final float WINDS_OPACITY=.24F;
    private static final int FADE_IN_TICKS=20;
    public static final int LINGER_TICKS=0;
    public static final int WINDS_LINGER_TICKS=0;
    // Keep the tint through the existing beam collapse, with no extra screen hold or fade.
    public static final int HOLD_TICKS=EaRuptureShape.RELEASE_TICKS+EaRuptureShape.FADE_TICKS;
    public static final int TOTAL_TICKS=HOLD_TICKS;
    public static final int CHARGE_TIMEOUT_TICKS=EaRuptureShape.CHARGE_TICKS+10*20;
    private final int totalTicks,chargeTimeoutTicks;
    private final float peakOpacity;
    private enum Phase { CHARGE, RELEASE }
    private int age=-1;
    private Phase phase=Phase.RELEASE;

    public EaCrimsonTint() {this(false);}

    public EaCrimsonTint(boolean winds) {
        int chargeTicks=winds?WindsRaptureShape.CHARGE_TICKS:EaRuptureShape.CHARGE_TICKS;
        totalTicks=winds?WindsRaptureShape.RELEASE_TICKS+WindsRaptureShape.FADE_TICKS:TOTAL_TICKS;
        chargeTimeoutTicks=chargeTicks+10*20;
        peakOpacity=winds?WINDS_OPACITY:MAX_OPACITY;
    }

    public void startCharge() {phase=Phase.CHARGE;age=0;}
    public void trigger() {phase=Phase.RELEASE;age=0;}
    public void clear() {age=-1;}
    public boolean isActive() {return age>=0;}

    public void cancel() {
        if(!isActive() || phase!=Phase.CHARGE)return;
        clear();
    }

    public void tick() {
        if(!isActive())return;
        age++;
        if(phase==Phase.CHARGE && age>=chargeTimeoutTicks)cancel();
        else if(phase==Phase.RELEASE && age>=totalTicks)clear();
    }

    public float opacity(float partialTicks) {
        if(age<0)return 0;
        float partial=Float.isFinite(partialTicks)?Math.max(0,Math.min(1,partialTicks)):0;
        if(phase==Phase.CHARGE)return peakOpacity*smooth((age+partial)/FADE_IN_TICKS);
        return age+partial<totalTicks?peakOpacity:0;
    }

    private static float smooth(float value) {
        float t=Math.max(0,Math.min(1,value));
        return t*t*(3-2*t);
    }
}
