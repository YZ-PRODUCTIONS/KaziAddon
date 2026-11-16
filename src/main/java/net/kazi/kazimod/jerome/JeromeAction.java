package net.kazi.kazimod.jerome;

public enum JeromeAction {
    IDLE("idle",0), ARRIVAL("arrival",60), COMBO("combo",40), RUSH("rush",46),
    SLAM("slam",48), SPHERE("sphere",60), BEAM("beam",78), SURGE("surge",56);
    public final String clip;
    public final int ticks;
    JeromeAction(String clip,int ticks){this.clip=clip;this.ticks=ticks;}
    public static JeromeAction from(int value){return values()[Math.max(0,Math.min(values().length-1,value))];}
}


