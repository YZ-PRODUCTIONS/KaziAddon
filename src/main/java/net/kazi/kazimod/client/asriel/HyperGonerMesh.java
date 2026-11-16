package net.kazi.kazimod.client.asriel;

import net.kazi.kazimod.abilities.cinematics.AwakeningCutsceneType;

/** Volumetric skull and an open-world gravity well, without a background plane. */
public final class HyperGonerMesh {
    public interface Sink { void vertex(float x,float y,float z,int rgb,float alpha); }
    private static final float TAU=(float)(Math.PI*2);
    private HyperGonerMesh() {}
    public static float smooth(float t){t=Math.max(0,Math.min(1,t));return t*t*(3-2*t);}
    public static float reveal(float age){return smooth((age-8)/28);}
    public static float suction(float age){return smooth((age-48)/48)*(1-smooth((age-222)/18));}
    public static float flash(float age){return smooth((age-223)/14)*(1-smooth((age-245)/12));}
    public static float fade(float age){return 1-smooth((age-239)/18);}

    public static void render(Sink body,Sink glow,float age,int detail) {
        if(!AwakeningCutsceneType.HYPER_GONER.active(age))return;
        float show=reveal(age)*fade(age);if(show<=0)return;
        int segments=detail==0?40:detail==1?28:20;
        float open=smooth((age-37)/38),pull=suction(age);
        float mouthY=-.78F-open*.49F,mouthH=1.61F+open*.84F;
        Sink skull=(x,y,z,c,a)->{
            float tremor=((float)Math.sin(age*.63+y*2.7)+(float)Math.sin(age*1.13+y*5.1))*.018F*pull;
            body.vertex(x+tremor,y,z,c,a*show);
        };
        HyperGonerSkull.render(skull,open,detail);
        HyperGonerGravity.render(body,glow,age,detail,mouthY,mouthH,show,pull);
        float burst=smooth((age-216)/26),opacity=flash(age)*show;
        if(opacity>0) {
            disc(glow,0,mouthY,1.3F,.1F+burst*9,segments,0xFFFFFF,opacity);
            ring(glow,0,mouthY,1.4F,.2F+burst*13,.13F,segments,0xD7E9FF,opacity*.7F);
        }
    }
    private static void disc(Sink s,float x,float y,float z,float r,int n,int color,float alpha) {
        for(int i=0;i<n;i++) {
            float a=TAU*i/n,b=TAU*(i+1)/n;
            s.vertex(x,y,z,color,alpha);
            s.vertex(x+(float)Math.cos(a)*r,y+(float)Math.sin(a)*r,z,color,alpha);
            s.vertex(x+(float)Math.cos(b)*r,y+(float)Math.sin(b)*r,z,color,alpha);
            s.vertex(x+(float)Math.cos(b)*r,y+(float)Math.sin(b)*r,z,color,alpha);
        }
    }
    private static void ring(Sink s,float x,float y,float z,float r,float width,int n,int color,float alpha) {
        for(int i=0;i<n;i++) {
            float a=TAU*i/n,b=TAU*(i+1)/n,ca=(float)Math.cos(a),sa=(float)Math.sin(a),cb=(float)Math.cos(b),sb=(float)Math.sin(b);
            s.vertex(x+ca*r,y+sa*r,z,color,alpha);s.vertex(x+cb*r,y+sb*r,z,color,alpha);
            s.vertex(x+cb*(r+width),y+sb*(r+width),z,color,alpha);s.vertex(x+ca*(r+width),y+sa*(r+width),z,color,alpha);
        }
    }
}
