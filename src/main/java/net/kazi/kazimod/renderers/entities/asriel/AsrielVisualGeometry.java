package net.kazi.kazimod.renderers.entities.asriel;

/** Untextured, bounded meshes. Shared by the game and the offline visual previews. */
public final class AsrielVisualGeometry {
    public static final float SABER_VISUAL_SCALE = 0.25F;
    public interface Sink { void vertex(double x, double y, double z, float r, float g, float b, float a); }
    /** Tint every layer, including cores and trails, without changing geometry or opacity. */
    public static Sink palette(Sink out, float brightness) {
        if (brightness == 1F) return out;
        return (x,y,z,r,g,b,a) -> out.vertex(x,y,z,r*brightness,g*brightness,b*brightness,a);
    }
    /** Pack glowing color and ink coverage for ONE / ONE_MINUS_SRC_ALPHA blending in one pass. */
    public static Sink flowingPalette(Sink out, float brightness, float age, float scale) {
        final float phase = AsrielFlowPattern.phase(age);
        return (x,y,z,r,g,b,a) -> {
            float u = (float) ((x + z * .7) * scale);
            float v = (float) ((y + z * .25) * scale);
            float shade = AsrielFlowPattern.shade(phase + u * .115F + v * .075F, v);
            float emission = brightness * shade * a;
            float ink = Math.max(0,Math.min(1,(1-shade)/.96F));
            out.vertex(x,y,z,r*emission,g*emission,b*emission,a*ink);
        };
    }
    private static final float[][] COLORS = {
            {1.0F,.34F,.51F}, {1.0F,.66F,.28F}, {1.0F,.96F,.45F},
            {.46F,1.0F,.68F}, {.39F,.88F,1.0F}, {.54F,.51F,1.0F}, {.94F,.48F,1.0F}};
    private static final float[][] WARNING_COLORS = {{1,0,0},{1,160/255F,64/255F},{1,1,0}};
    private static final double TAU = Math.PI * 2;
    private AsrielVisualGeometry() { }

    public static float clamp(float v) { return Math.max(0, Math.min(1, v)); }
    public static float[] color(int index) { return COLORS[Math.floorMod(index, COLORS.length)]; }
    public static int count(int detail, int high, int middle, int low) { return detail == 0 ? high : detail == 1 ? middle : low; }
    private static void vertex(Sink out, double x, double y, double z, float[] c, float a) {
        out.vertex(x,y,z,c[0],c[1],c[2],a);
    }
    private static void quad(Sink out, double[] a,double[] b,double[] c,double[] d,float[] rgb,float alpha) {
        vertex(out,a[0],a[1],a[2],rgb,alpha);vertex(out,b[0],b[1],b[2],rgb,alpha);
        vertex(out,c[0],c[1],c[2],rgb,alpha);vertex(out,d[0],d[1],d[2],rgb,alpha);
    }
    private static double[] p(double x,double y,double z) { return new double[]{x,y,z}; }
    private static final float[] WHITE={1,.99F,.95F}, GOLD={1,.91F,.39F}, CYAN={.46F,.95F,1}, PINK={1,.52F,.86F};

    /** Two crossed ribbons, visible even when the camera is aligned with one of them. */
    public static void beamlet(Sink out,double ax,double ay,double az,double bx,double by,double bz,
                               double width,float[] rgb,float alpha) {
        if(width<=0 || alpha<=0) return;
        double dx=bx-ax,dy=by-ay,dz=bz-az,len=Math.sqrt(dx*dx+dy*dy+dz*dz);
        if(len<.00001)return;dx/=len;dy/=len;dz/=len;
        double sx,sy,sz;
        if(Math.abs(dy)>.9){sx=0;sy=dz;sz=-dy;}else{sx=-dz;sy=0;sz=dx;}
        double s=Math.sqrt(sx*sx+sy*sy+sz*sz);sx=sx/s*width;sy=sy/s*width;sz=sz/s*width;
        for(int cross=0;cross<2;cross++) {
            quad(out,p(ax+sx,ay+sy,az+sz),p(ax-sx,ay-sy,az-sz),p(bx-sx,by-sy,bz-sz),p(bx+sx,by+sy,bz+sz),rgb,alpha);
            double nx=dy*sz-dz*sy,ny=dz*sx-dx*sz,nz=dx*sy-dy*sx;sx=nx;sy=ny;sz=nz;
        }
    }

    public static void ring(Sink out,double radius,double y,double width,float rotation,float[] rgb,float alpha,int segments) {
        if(radius<=0 || width<=0 || alpha<=0)return;
        for(int i=0;i<segments;i++) {
            double a=TAU*i/segments+rotation,b=TAU*(i+1)/segments+rotation;
            quad(out,p(Math.cos(a)*(radius-width),y,Math.sin(a)*(radius-width)),
                    p(Math.cos(b)*(radius-width),y,Math.sin(b)*(radius-width)),
                    p(Math.cos(b)*radius,y,Math.sin(b)*radius),p(Math.cos(a)*radius,y,Math.sin(a)*radius),rgb,alpha);
        }
    }

    /** Compatibility entry point for offline previews; gameplay supplies the finisher flag. */
    public static void star(Sink out,float radius,float age,boolean enhanced,boolean small,int detail) {
        star(out,radius,age,enhanced,small,false,detail);
    }

    /** A single beveled star, with recognizable star-shaped echoes instead of long beam trails. */
    public static void star(Sink out,float radius,float age,boolean enhanced,boolean small,boolean finisher,int detail) {
        float spin=small?age*.09F:(float)Math.sin(age*.035F)*.055F;
        float[] pastel=starColor(age,enhanced);
        if(!small) {
            int echoes=count(detail,3,2,1);
            for(int echo=echoes;echo>=1;echo--) {
                double offset=radius*(finisher?.58:.65)*echo;
                float alpha=(.27F-.045F*echo)*(finisher?1.2F:1);
                starSilhouette(out,offset*.45,offset,offset*.12,radius*(1-.07*echo),
                        spin-.025F*echo,pastel,alpha,.075F);
            }
        }
        double depth=radius*(small?.09:.12);
        for(int i=0;i<10;i++) {
            double a=TAU*i/10+spin,b=TAU*(i+1)/10+spin;
            double ra=radius*(i%2==0?1:.43),rb=radius*(i%2==0?.43:1);
            double ax=Math.sin(a)*ra,ay=Math.cos(a)*ra,bx=Math.sin(b)*rb,by=Math.cos(b)*rb;
            double[] outerA=p(ax,ay,depth*.22),outerB=p(bx,by,depth*.22);
            double[] innerA=p(ax*.82,ay*.82,depth),innerB=p(bx*.82,by*.82,depth);
            double[] backA=p(ax,ay,-depth*.22),backB=p(bx,by,-depth*.22);
            double[] backInnerA=p(ax*.82,ay*.82,-depth),backInnerB=p(bx*.82,by*.82,-depth);
            double[] front=p(0,0,depth*1.25),back=p(0,0,-depth*1.25);
            quad(out,front,innerA,innerB,front,pastel,small?.65F:.76F);
            quad(out,back,backInnerB,backInnerA,back,pastel,.30F);
            quad(out,innerA,outerA,outerB,innerB,i%2==0?WHITE:pastel,.48F);
            quad(out,backInnerB,backB,backA,backInnerA,pastel,.20F);
            quad(out,outerA,backA,backB,outerB,pastel,.36F);
            if(!small && detail<2) {
                float shimmer=.28F+.24F*(float)(.5+.5*Math.sin(age*.65+i*1.2));
                beamlet(out,ax,ay,depth*.24,bx,by,depth*.24,radius*.008,WHITE,shimmer);
            }
        }
        // A sparse pinprick of light at each point keeps the large outline readable at distance.
        if(!small && detail==0)for(int i=0;i<5;i++) {
            double a=TAU*i/5+spin,x=Math.sin(a)*radius,y=Math.cos(a)*radius;
            float glow=.25F+.45F*(float)Math.pow(Math.max(0,Math.sin(age*.4+i*1.7)),4);
            beamlet(out,x-radius*.045,y,depth*.28,x+radius*.045,y,depth*.28,radius*.009,WHITE,glow);
            beamlet(out,x,y-radius*.045,depth*.28,x,y+radius*.045,depth*.28,radius*.009,WHITE,glow);
        }
    }

    private static float[] starColor(float age,boolean enhanced) {
        float phase=age*(enhanced?.52F:.44F)+(enhanced?2.6F:0);
        return new float[]{.67F+.30F*(float)(.5+.5*Math.sin(phase)),
                .67F+.30F*(float)(.5+.5*Math.sin(phase+TAU/3)),
                .67F+.30F*(float)(.5+.5*Math.sin(phase+TAU*2/3))};
    }

    /** Filled ghost with an independently brighter five-point outline. */
    private static void starSilhouette(Sink out,double x,double y,double z,double radius,float spin,
                                      float[] rgb,float outlineAlpha,float fillAlpha) {
        for(int i=0;i<10;i++) {
            double a=TAU*i/10+spin,b=TAU*(i+1)/10+spin;
            double ra=radius*(i%2==0?1:.43),rb=radius*(i%2==0?.43:1);
            double ax=Math.sin(a)*ra,ay=Math.cos(a)*ra,bx=Math.sin(b)*rb,by=Math.cos(b)*rb;
            if(fillAlpha>0)quad(out,p(x,y,z),p(x+ax,y+ay,z),p(x+bx,y+by,z),p(x,y,z),rgb,fillAlpha);
            quad(out,p(x+ax,y+ay,z+.002),p(x+bx,y+by,z+.002),
                    p(x+bx*.965,y+by*.965,z+.002),p(x+ax*.965,y+ay*.965,z+.002),rgb,outlineAlpha);
        }
    }

    /** Brief expanding ghost stars and rings of white star-shaped fragments, like the reference impact. */
    public static void starBurst(Sink out,float radius,float age,boolean enhanced,boolean finisher,int detail) {
        float duration=finisher?28:16,t=clamp(age/duration);
        if(age<0 || age>=duration)return;
        float fade=(float)Math.pow(1-clamp((t-.55F)/.45F),1.2),expansion=(float)(1-Math.pow(1-t,3));
        float[] pastel=starColor(age+12,enhanced);
        int ghosts=finisher?3:2;
        for(int i=0;i<ghosts;i++) {
            float delay=i*(finisher?3.5F:2.0F),local=clamp((age-delay)/(duration-delay));
            if(age<delay)continue;
            double rr=radius*(.55+local*(finisher?1.45:1.15));
            starSilhouette(out,0,radius*.25,-i*.035,rr,0,i==0?WHITE:pastel,
                    fade*(i==0?.74F:.34F),fade*(i==0?.075F:.025F));
        }
        int particles=count(detail,finisher?20:12,finisher?14:8,finisher?8:5);
        int rings=finisher?2:1;
        for(int layer=0;layer<rings;layer++) {
            for(int i=0;i<particles;i++) {
                double a=TAU*i/particles+layer*.17;
                double rr=radius*((layer==0?.16:.33)+expansion*(layer==0?1.65:1.05));
                double x=Math.sin(a)*rr,y=radius*.25+Math.cos(a)*rr;
                double z=Math.sin(i*2.4+layer)*radius*.17*expansion;
                double size=radius*(finisher?.043:.06)*(1-.35*t);
                miniStar(out,x,y,z,size,(float)(a+age*.04),fade*.8F);
            }
        }
        // Only the first few frames overexpose the impact center; the star rings carry the fade.
        float flash=clamp(1-age/5)*.8F;
        if(flash>0)starSilhouette(out,0,radius*.25,.08,radius*(.28+expansion*.7),0,WHITE,flash,flash*.32F);
    }

    private static void miniStar(Sink out,double x,double y,double z,double radius,float spin,float alpha) {
        for(int i=0;i<10;i++) {
            double a=TAU*i/10+spin,b=TAU*(i+1)/10+spin;
            double ra=radius*(i%2==0?1:.43),rb=radius*(i%2==0?.43:1);
            double[] center=p(x,y,z);
            quad(out,center,p(x+Math.sin(a)*ra,y+Math.cos(a)*ra,z),
                    p(x+Math.sin(b)*rb,y+Math.cos(b)*rb,z),center,WHITE,alpha);
        }
    }

    /** Shared entry point for the clip-shaped rainbow column and its repeated white flashes. */
    public static void lightning(Sink out,float age,float radius,boolean enhanced,int detail) {
        ShockerBreakerVisuals.render(out,age,radius,enhanced,detail);
    }

    /** Hollow oval heads and long curved pastel tails like the supplied Chaos Bullets reference. */
    public static void comet(Sink out,float age,int tint,boolean buster,int detail) {
        float[] c=color(tint);float[] pastel={.6F+.4F*c[0],.6F+.4F*c[1],.6F+.4F*c[2]};
        int segments=count(detail,18,14,10);
        if(buster) {
            busterBullet(out,age,detail);
            return;
        }
        oval(out,.34,.18,0,.075,pastel,.95F,segments,0);
        oval(out,.30,.16,.025,.025,WHITE,.95F,segments,0);
        // The second tilted head outline keeps the hollow silhouette readable at side-on angles.
        if(detail<2)oval(out,.29,.17,0,.05,pastel,.66F,segments,Math.PI/2);
        double ax=-.23,ay=-.06,az=-.08;
        int tail=count(detail,10,7,5);
        for(int i=1;i<=tail;i++) {
            double t=i/(double)tail,bx=-.23-.85*t*t,by=-.06+.3*Math.sin(t*2.6),bz=-.08-2.5*t;
            double width=.12*Math.pow(1-t,.7)+.008;
            beamlet(out,ax,ay,az,bx,by,bz,width,pastel,(float)(.9-.6*t));
            ax=bx;ay=by;az=bz;
        }
    }

    /** The cannon fires outlined diamonds; the open middle stays visible against the battlefield. */
    public static void busterBullet(Sink out,float age,int detail) {
        double rotation=Math.sin(age*.13)*.20;
        polygonRim(out,0,0,0,.32,.32,.062,4,rotation,WHITE,.97F);
        if(detail<2)polygonRim(out,0,0,-.015,.245,.245,.016,4,rotation,WHITE,.28F);
    }

    private static void polygonRim(Sink out,double x,double y,double z,double rx,double ry,double width,
                                   int sides,double rotation,float[] rgb,float alpha) {
        for(int i=0;i<sides;i++) {
            double a=TAU*i/sides+rotation,b=TAU*(i+1)/sides+rotation;
            quad(out,p(x+Math.sin(a)*rx,y+Math.cos(a)*ry,z),p(x+Math.sin(b)*rx,y+Math.cos(b)*ry,z),
                    p(x+Math.sin(b)*(rx-width),y+Math.cos(b)*(ry-width),z),
                    p(x+Math.sin(a)*(rx-width),y+Math.cos(a)*(ry-width),z),rgb,alpha);
        }
    }

    /** Thin lane previews use the exact angles supplied by the server's burst pattern. */
    public static void busterTelegraph(Sink out,float age,float progress,float length,double[] angles,int detail) {
        if(angles.length==0 || length<=0)return;
        float alpha=.24F+.20F*clamp(progress)+.04F*(float)Math.sin(age*.8);
        // The reference advances orange -> yellow -> red every frame at 30 fps.
        float[] warning=WARNING_COLORS[Math.floorMod((int)Math.floor(age*1.5F)+1,3)];
        for(double angle:angles) {
            double x=Math.sin(angle)*length,z=Math.cos(angle)*length;
            beamlet(out,0,0,.10,x,0,z,.018,warning,alpha);
            if(detail==0)beamlet(out,0,0,.10,x,0,z,.052,warning,alpha*.08F);
        }
    }

    /** Sculpted light cannon; its hollow muzzle remains at the beam's local origin. */
    public static void busterCannon(Sink out,float age,float fill,boolean enhanced,int detail) {
        ChaosCannonVisuals.cannon(out,age,fill,1,enhanced,detail,false);
    }

    private static void oval(Sink out,double rx,double ry,double z,double width,float[] c,float alpha,int segments,double tilt) {
        for(int i=0;i<segments;i++) {
            double a=TAU*i/segments,b=TAU*(i+1)/segments;
            double[][] v={p(Math.cos(a)*rx,Math.sin(a)*ry,z),p(Math.cos(b)*rx,Math.sin(b)*ry,z),
                    p(Math.cos(b)*(rx-width),Math.sin(b)*(ry-width*.65),z),p(Math.cos(a)*(rx-width),Math.sin(a)*(ry-width*.65),z)};
            if(tilt!=0)for(double[] q:v){double y=q[1];q[1]=y*Math.cos(tilt);q[2]+=y*Math.sin(tilt);}
            quad(out,v[0],v[1],v[2],v[3],c,alpha);
        }
    }

    /** Cosmetic scaling is independent of the combat dimensions supplied by the entity. */
    public static void sabers(Sink out,float age,float length,float radius,int detail) {
        ChaosSaberVisuals.render(out,age,length * SABER_VISUAL_SCALE,radius * SABER_VISUAL_SCALE,detail);
    }

    public static void charge(Sink out,float age,float duration,float radius,boolean enhanced,int detail) {
        busterCharge(out,age,clamp(age/Math.max(1,duration)),radius,enhanced,detail);
    }

    /** Explicit fill keeps the visual synchronized with both normal and enhanced attack timelines. */
    public static void busterCharge(Sink out,float age,float fill,float radius,boolean enhanced,int detail) {
        float arrival=enhanced?1:clamp(age/(10F*20F/30F));
        ChaosCannonVisuals.charge(out,age,fill,arrival,enhanced,detail);
    }

    /** The beam applies its visual scale to the supplied combat radius; the cannon keeps its own size. */
    public static void rainbowBeam(Sink out,float age,float duration,float length,float radius,boolean enhanced,int detail) {
        if(!Float.isFinite(age)||!Float.isFinite(duration)||!Float.isFinite(length)||!Float.isFinite(radius)
                ||age<0||age>=duration||duration<=0||length<=.001F||radius<=.001F)return;
        float fade=clamp((age+.5F)/2)*clamp((duration-age)/6);
        if(fade<=0)return;
        ChaosCannonVisuals.cannon(out,age,1,fade,enhanced,detail,true);
        ChaosBeamVisuals.render(out,age,duration,length,radius,enhanced,detail);
    }
}
