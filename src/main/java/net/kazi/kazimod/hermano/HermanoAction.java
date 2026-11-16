package net.kazi.kazimod.hermano;

public enum HermanoAction {
    IDLE("idle",0), ARRIVAL("arrival",60), COMBO("combo",40), RUSH("rush",46),
    GLARE("glare",38), IMPACT("impact",64), COUNTER("counter",32), FULL_POWER("full_power",70);
    public final String clip;
    public final int ticks;
    HermanoAction(String clip,int ticks){this.clip=clip;this.ticks=ticks;}
    public static HermanoAction from(int value){return values()[Math.max(0,Math.min(values().length-1,value))];}
}
