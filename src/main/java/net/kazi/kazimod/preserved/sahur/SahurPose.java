package net.kazi.kazimod.preserved.sahur;

/** Rotation-only animation curves. Dimensions, UVs and resting pivots remain unchanged. */
public final class SahurPose {
    public static final int IDLE=0, SWING=1, DIVINE=2, BARRAGE=3, PORTALS=4, ROAR=5, RUSH=6, COUNTER=7, SHIELD=8;
    public final float[][] joints = new float[8][3]; // head, body, right/left arm, right/left leg, right/left wing
    public float elbowRight, elbowLeft, bob, halo;
    private static float sin(double n){return (float)Math.sin(n);}
    public static SahurPose sample(float age,float stride,float movement,boolean sprint,boolean flying,boolean god,
                                   float yaw,float pitch,float attack,int action,float charge,float activeTime){
        SahurPose p=new SahurPose();
        float amplitude=movement*(sprint?.67F:.43F),cycle=stride*(sprint?.72F:.62F);
        float step=sin(cycle),breath=sin(age*.085);
        p.joints[0][1]=Math.max(-.42F,Math.min(.42F,yaw*.0174533F));
        p.joints[0][0]=Math.max(-.25F,Math.min(.25F,pitch*.012F)) + breath*.008F;
        p.joints[1][2]=sin(cycle)*movement*.025F;
        p.joints[1][1]=sin(cycle)*movement*.065F;
        p.joints[2][0]=-step*amplitude*.72F+breath*.025F;
        p.joints[3][0]=step*amplitude*.72F-breath*.025F;
        p.joints[2][2]=.055F;p.joints[3][2]=-.055F;
        p.joints[4][0]=step*amplitude;p.joints[5][0]=-step*amplitude;
        p.elbowRight=.08F+Math.max(0,step)*movement*.2F;
        p.elbowLeft=.08F+Math.max(0,-step)*movement*.2F;
        p.bob=-Math.abs(sin(cycle))*movement*(sprint?.48F:.24F)+breath*.05F;
        float flap=sin(age*(flying?.32:.065));
        p.joints[6][1]=-.12F-flap*(flying?.48F:.065F);p.joints[7][1]=-p.joints[6][1];
        p.joints[6][2]=-.04F-sin(age*(flying?.32:.065)-.6)*(flying?.12F:.018F);p.joints[7][2]=-p.joints[6][2];
        p.halo=sin(age*.07)*.3F;
        if(!flying && movement<.05 && god){p.joints[2][0]=-.08F;p.joints[3][0]=.03F;}
        if(flying){
            p.joints[1][0]=.1F;p.joints[4][0]=.3F+sin(age*.09)*.025F;p.joints[5][0]=.35F-sin(age*.09)*.025F;
            p.joints[2][2]=.24F;p.joints[3][2]=-.24F;p.joints[2][0]=-.15F;p.joints[3][0]=-.15F;
            p.bob=sin(age*.16)*.14F;
        }
        float swing=sin(Math.PI*Math.sqrt(Math.max(0,attack)));
        if(attack>0){p.joints[2][0]=-1.3F*swing;p.joints[2][1]=-.6F*swing;p.joints[2][2]=-.25F*swing;p.joints[1][1]=.2F*swing;p.elbowRight=.32F*swing;}
        switch(action){
            case SWING:
                p.joints[2][0]=-1.15F-charge*.65F;p.joints[2][1]=-.8F*charge;p.joints[3][0]=-.75F;
                p.joints[1][1]=.25F*charge;p.elbowRight=.45F;p.elbowLeft=.3F;
                if(charge==0){float cut=sin(Math.min(1,activeTime/10)*Math.PI);p.joints[2][0]=-1.8F+cut*1.6F;p.joints[2][1]=-.7F+cut*1.1F;p.joints[1][1]=-.3F*cut;}break;
            case DIVINE:
                p.joints[2][0]=-2.65F*charge;p.joints[2][2]=-.18F*charge;p.joints[3][0]=-.6F*charge;
                p.joints[3][2]=-.45F*charge;p.joints[0][0]=-.15F*charge;p.elbowRight=.08F;p.elbowLeft=.35F;break;
            case BARRAGE:
                p.joints[2][0]=-1.3F+sin(age*1.05)*.36F;p.joints[3][0]=-.75F+sin(age*1.05+Math.PI)*.23F;
                p.joints[1][1]=sin(age*.55)*.1F;p.elbowRight=.35F+sin(age*1.05)*.15F;break;
            case PORTALS:
                p.joints[2][0]=-1.0F;p.joints[3][0]=-.8F;p.joints[2][2]=.65F;p.joints[3][2]=-.65F;
                p.elbowRight=.3F;p.elbowLeft=.3F;p.joints[6][1]=-.45F;p.joints[7][1]=.45F;break;
            case ROAR:
                p.joints[0][0]=-.2F;p.joints[2][0]=-.5F;p.joints[3][0]=-.5F;
                p.joints[2][2]=.28F;p.joints[3][2]=-.28F;p.joints[1][0]=-.04F+sin(age*1.2)*.008F;break;
            case RUSH:
                p.joints[1][0]=.18F;p.joints[2][0]=.32F;p.joints[3][0]=.32F;
                p.joints[4][0]=.55F;p.joints[5][0]=.55F;p.joints[6][1]=-.42F-sin(age*.55)*.3F;p.joints[7][1]=-p.joints[6][1];break;
            case COUNTER:case SHIELD:
                p.joints[2][0]=-1.25F;p.joints[2][1]=-.45F;p.joints[3][0]=-1.0F;p.joints[3][1]=.45F;
                p.elbowRight=.48F;p.elbowLeft=.48F;
                if(action==SHIELD){p.joints[6][1]=-.5F+breath*.035F;p.joints[7][1]=.5F-breath*.035F;}break;
            default:break;
        }
        return p;
    }
}
