package net.kazi.kazimod.ghoul;

import net.minecraft.util.math.vector.Vector3d;

/** Model-space curves, in Blockbench units. Roots stay fixed throughout every strike. */
public final class GhoulMotion {
    public static float smooth(float x){x=Math.max(0,Math.min(1,x));return x*x*(3-2*x);}
    public static float pulse(float tick,float strike,float windup,float recovery){
        return tick<=strike?smooth((tick-strike+windup)/windup):1-smooth((tick-strike)/recovery);
    }
    public static Vector3d rest(int type,int limb,double t){
        if(type==3)return new Vector3d(Math.sin(t*4.3)*13,12-9*Math.sin(t*2.7)+t*t*9,3+43*t);
        double side=limb%2==0?-1:1,upper=limb<2?1:-1;
        return new Vector3d(side*(2+t*26+Math.sin(t*Math.PI)*5),14+upper*Math.sin(t*Math.PI*.85)*15+t*5,3+22*t+Math.sin(t*Math.PI)*8);
    }
    public static Vector3d point(int type,int limb,double t,int move,float tick,float clock,Vector3d target){
        Vector3d rest=rest(type,limb,t);double wave=Math.sin(clock*.065-t*5+limb*1.9)*t*t;
        rest=rest.add(wave*1.5,Math.cos(clock*.05-t*4+limb)*t*1.2,0);
        float power=0;Vector3d end=new Vector3d((limb%2==0?-1:1)*5,20,-60);
        switch(move){
            case 9: power=pulse(tick,6+limb*6,5,5);end=(target==null?new Vector3d(0,20,-68):target).add((limb%2==0?-1:1)*3,limb<2?3:-3,0);break;
            case 10: power=smooth(tick/7)*(1-smooth((tick-24)/8));end=target==null?new Vector3d(0,20,-72):target; if(limb>0)power*=.25F;break;
            case 11: power=pulse(tick,5,5,18);end=new Vector3d((limb%2==0?-1:1)*14,-5,4);break;
            case 12: power=Math.max(pulse(tick,9,7,5),Math.max(pulse(tick,18,6,5),pulse(tick,28,7,8)));double a=tick*.24+limb*Math.PI/2;end=new Vector3d(Math.cos(a)*67,14+limb*2,Math.sin(a)*67);break;
            case 13:power=pulse(tick,7,7,9);end=target==null?new Vector3d(0,20,-84):target;break;
            case 14:power=pulse(tick,12,10,9);double sweep=(tick-3)/18*Math.PI*1.4;end=new Vector3d(Math.cos(sweep)*64,5,-Math.sin(sweep)*64);break;
            case 15:power=smooth(tick/5);end=new Vector3d(-10,24,-16);break;
            case 16:power=Math.max(pulse(tick,12,7,5),Math.max(pulse(tick,22,6,5),pulse(tick,34,10,9)));end=(target==null?new Vector3d(0,20,-52):target).add(tick<27?(tick<17?-12:12):0,tick<27?0:Math.max(-12,28-(tick-27)*5),0);break;
            default:break;
        }
        Vector3d start=rest(type,limb,0),c1=start.add((limb%2==0?-1:1)*22,10,16),c2=end.add((limb%2==0?-1:1)*14,10,22);
        Vector3d attack=bezier(start,c1,c2,end,t);return rest.scale(1-power).add(attack.scale(power));
    }
    private static Vector3d bezier(Vector3d a,Vector3d b,Vector3d c,Vector3d d,double t){double s=1-t;return a.scale(s*s*s).add(b.scale(3*s*s*t)).add(c.scale(3*s*t*t)).add(d.scale(t*t*t));}
    private GhoulMotion(){}
}
