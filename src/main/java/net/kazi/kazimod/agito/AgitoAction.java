package net.kazi.kazimod.agito;

public enum AgitoAction {
    IDLE("idle",0), SUMMON("summon",80), DISMISS("dismiss",60),
    PUNCH_RIGHT("punch_right",26), PUNCH_LEFT("punch_left",26),
    BARRAGE("punch_barrage",44), JUMP("jump",20), HEAL("embrace",0);

    public final String clip;
    public final int ticks;
    AgitoAction(String clip,int ticks){this.clip=clip;this.ticks=ticks;}
    public static AgitoAction from(int value){return values()[Math.max(0,Math.min(values().length-1,value))];}
}
