package net.kazi.kazimod.shikigami;

public enum ShikigamiKind {
    TOTALITY("divine_dog_totality","Divine Dog: Totality",260,18,.34,1.3F,2.7F,1F,
            "A swift guardian that tears enemies with alternating claw slashes."),
    ELEPHANT("max_elephant","Max Elephant",1000,28,.16,5.7F,6.3F,1.5F,
            "Appears 50 blocks overhead and crashes down for 200 damage in a 16-block radius. Slow, durable, and heavy-hitting."),
    TOAD("toad","Toad",100,2,.22,1.8F,1.25F,1F,
            "A fragile support summon that catches enemies within 24 blocks and reels them in with its tongue."),
    OX("piercing_ox","Piercing Ox",420,12,.3,2.5F,2.6F,1F,
            "Runs to build momentum, then charges an enemy. Charge damage grows with distance, up to 80. Repositions for another charge after each hit.");
    public final String id,title,description;
    public final double health,damage,speed;
    public final float width,height,modelScale;
    ShikigamiKind(String id,String title,double health,double damage,double speed,float width,float height,float scale,String description){
        this.id=id;this.title=title;this.health=health;this.damage=damage;this.speed=speed;this.width=width;this.height=height;this.modelScale=scale;this.description=description;
    }
    public String summonTag(){return "kazimod_"+id+"_summon";}
    public String reserveTag(){return "kazimod_"+id+"_reserve";}
    public String defeatedTag(){return "kazimod_"+id+"_defeated";}
    public static float chargeDamage(double distance){return (float)Math.min(80,12+Math.max(0,distance)*2.7);}
}
