package net.kazi.kazimod.longbob;

public enum LongbobAction {
    IDLE("idle",0), ARRIVAL("arrival",55), MELEE("melee",32), HYDRA("hydra",72),
    CHLORO("chloro",48), GUMO("gumo",160), FUGU("fugu",44), ROAD("road",100);
    public final String clip;
    public final int ticks;
    LongbobAction(String clip,int ticks){this.clip=clip;this.ticks=ticks;}
    public static LongbobAction from(int id){return id>=0&&id<values().length?values()[id]:IDLE;}
}
