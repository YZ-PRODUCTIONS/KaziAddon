package net.kazi.kazimod.models.abilities;

/** Ea's rotating drums and sculpted, hollow gold guard. Local +Z points to the tip. */
public final class EaSwordMesh {
    private static final float TAU = (float) (Math.PI * 2);
    private static final int GOLD = 0xD5B865;
    private static final int PALE_GOLD = 0xF5E3A0;
    private static final int GOLD_EDGE = 0xFFF0B1;
    private static final int GOLD_SHADOW = 0x816034;
    private static final int PURPLE = 0x21183B;
    private static final int BARREL = 0x30292E;
    private static final int RED = 0xF3223A;
    // Slim, constant-width drums: scale their seams, circuitry and end fitting
    // together, without changing the blade length, grip or sculpted gold guard.
    static final float DRUM_RADIUS = .125F;
    private static final float DRUM_RADIAL_SCALE = DRUM_RADIUS / .195F;

    private static final float[][] CIRCUITS = {
            {-.18F,-.18F,.34F,.34F,.03F,.03F,-.24F,-.24F},
            {.25F,.25F,-.20F,-.20F,.35F,.35F,.12F,.12F},
            {-.30F,.05F,.05F,.32F,.32F,-.24F,-.24F,.08F}
    };

    // All curved guard surfaces, engravings and fittings are built once per LOD.
    private static final Hilt[] HILTS = {new Hilt(0), new Hilt(1), new Hilt(2)};

    private static final class Hilt implements EaVfxMesh.Sink {
        private float[] points = new float[4096];
        private int[] colors = new int[1024];
        private int count;
        Hilt(int detail) { buildHilt(this, detail); }
        @Override public void vertex(float x, float y, float z, int color, float alpha) {
            if (count == colors.length) {
                colors = java.util.Arrays.copyOf(colors, colors.length * 2);
                points = java.util.Arrays.copyOf(points, points.length * 2);
            }
            int offset = count * 4;
            points[offset] = x; points[offset + 1] = y; points[offset + 2] = z; points[offset + 3] = alpha;
            colors[count++] = color;
        }
        void draw(EaVfxMesh.Sink out, float opacity) {
            for (int i = 0; i < count; i++) {
                int offset = i * 4;
                out.vertex(points[offset], points[offset + 1], points[offset + 2], colors[i], points[offset + 3] * opacity);
            }
        }
    }

    private EaSwordMesh() { }

    public static void draw(EaVfxMesh.Sink body, EaVfxMesh.Sink glow, int detail,
                            float age, float power, float opacity) {
        if (!Float.isFinite(age) || !Float.isFinite(power) || !Float.isFinite(opacity)) return;
        float alpha = clamp(opacity), charge = clamp(power);
        if (alpha <= .001F) return;
        int sides = detail == 0 ? 40 : detail == 1 ? 24 : 16;

        // The three blunt drums keep one continuous diameter, separated by fine seams.
        for (int segment = 0; segment < 3; segment++) {
            float rotation = age * (.034F + charge * .085F) * (segment == 1 ? -1 : 1)
                    + segment * 1.7F;
            float start = .10F + segment * .89F, end = start + .885F;
            barrel(body, start, end, DRUM_RADIUS, rotation, sides, BARREL, alpha);
            barrel(body, start, start + .009F, .196F * DRUM_RADIAL_SCALE, rotation, sides, 0x0D0B10, alpha);
            for (int path = 0; path < 6; path++) {
                float[] bends = CIRCUITS[(path + segment) % CIRCUITS.length];
                float phase = rotation + path * TAU / 6;
                for (int step = 0; step < bends.length - 1; step++) {
                    float z0 = start + .035F + step * .116F;
                    float z1 = z0 + .116F;
                    circuit(body, phase + bends[step], phase + bends[step + 1], z0, z1,
                            .200F * DRUM_RADIAL_SCALE, .080F, 0xBA1429, alpha, detail);
                    float ignition = Math.max(0, sin(age * .23F - z0 * 5.5F + path * .8F));
                    ignition = ignition * ignition * ignition * charge;
                    int circuitColor = mix(RED, 0xFFD9BA, ignition * .75F);
                    circuit(glow, phase + bends[step], phase + bends[step + 1], z0, z1,
                            .201F * DRUM_RADIAL_SCALE, .080F, circuitColor, alpha * (.33F + charge * .24F + ignition * .24F), detail);
                    if (detail == 0) circuit(glow, phase + bends[step], phase + bends[step + 1], z0, z1,
                            .202F * DRUM_RADIAL_SCALE, .15F, RED, alpha * charge * .08F, detail);
                }
            }
        }

        HILTS[detail == 0 ? 0 : detail == 1 ? 1 : 2].draw(body, alpha);
    }

    private static void buildHilt(EaVfxMesh.Sink body, int detail) {
        int sides = detail == 0 ? 48 : detail == 1 ? 32 : 20;
        int rows = detail == 0 ? 14 : detail == 1 ? 10 : 6;
        // Keep the end fitting proportional to the slimmer drums. This adapter
        // runs only during the static LOD bake, never in the per-frame draw path.
        EaVfxMesh.Sink cap = (x,y,z,color,alpha) ->
                body.vertex(x * DRUM_RADIAL_SCALE,y * DRUM_RADIAL_SCALE,z,color,alpha);
        lathe(cap, new float[][]{{2.764F,.145F,0},{2.78F,.169F,0},
                {2.82F,.157F,0},{2.90F,.111F,0},{2.93F,.025F,0},{2.936F,0,0}}, sides);
        spiralCap(cap, sides, detail == 0 ? 8 : 6);

        // Curved grip and rounded pommel retain the renderer's z=-0.60 hand pivot.
        lathe(body, new float[][]{{-1.135F,0,-.115F},{-1.12F,.046F,-.113F},
                {-1.08F,.085F,-.103F},{-1.015F,.093F,-.091F},{-.965F,.082F,-.08F},
                {-.86F,.068F,-.061F},{-.65F,.057F,-.036F},{-.43F,.063F,-.020F},
                {-.27F,.082F,-.010F},{-.16F,.119F,0},{-.105F,.135F,0}}, sides);
        lathe(body, new float[][]{{-.985F,.083F,-.083F},{-.975F,.089F,-.081F},
                {-.954F,.088F,-.078F},{-.944F,.079F,-.076F}}, sides);

        // A thin, continuously curved shell wraps around the barrel. Its swept
        // front and rear lips create Ea's asymmetric hooks without extruded slabs.
        for (int i = 0; i < sides; i++) {
            float a = TAU * i / sides, b = TAU * (i + 1) / sides;
            for (int j = 0; j < rows; j++) {
                float u = (float) j / rows, v = (float) (j + 1) / rows;
                shellQuad(body, a,b,u,v,0,false);
                shellQuad(body, b,a,u,v,-.023F,true);
            }
            // Rolled lips close the shell and catch a narrow polished highlight.
            shellLip(body,a,b,0,detail);
            shellLip(body,a,b,1,detail);
        }

        // Sweeping indigo flame engravings follow the curved shell, rather than
        // floating on flat planes. Broad roots split into slender pointed tips.
        for (int i = 0; i < 6; i++) {
            float a = TAU * i / 6 + .24F;
            flame(body,a,.10F,.86F,.24F,.50F,sides,rows);
            flame(body,a+.26F,.30F,.94F,.115F,-.23F,sides,rows);
        }
        if (detail < 2) for (int i = 0; i < 44; i++) {
            float a = TAU * i / 44;
            // Small dark alternating cuts on the skirt's lower engraved border.
            shellMark(body,a-.014F,a+.014F,.035F,.069F+(i%3)*.006F,sides,rows);
        }
    }

    /** Closed cylinder so the blunt ends remain solid from above and below. */
    private static void barrel(EaVfxMesh.Sink out,float z0,float z1,float radius,
                                float rotation,int sides,int color,float alpha) {
        for(int i=0;i<sides;i++){
            float a=TAU*i/sides+rotation,b=TAU*(i+1)/sides+rotation;
            float x=cos(a)*radius,y=sin(a)*radius,xx=cos(b)*radius,yy=sin(b)*radius;
            // POSITION_COLOR has no lighting normals: shade each shared edge
            // identically so interpolation reads as a cylinder, not flat facets.
            int tint=steel(color,a),nextTint=steel(color,b);
            out.vertex(x,y,z0,tint,alpha);out.vertex(xx,yy,z0,nextTint,alpha);
            out.vertex(xx,yy,z1,nextTint,alpha);out.vertex(x,y,z1,tint,alpha);
            triangle(out,0,0,z0,xx,yy,z0,x,y,z0,shade(color,.65F),alpha);
            triangle(out,0,0,z1,x,y,z1,xx,yy,z1,shade(color,.86F),alpha);
        }
    }

    private static void circuit(EaVfxMesh.Sink out,float a,float b,float z0,float z1,
                                 float radius,float halfWidth,int color,float alpha,int detail){
        // Subdivide bends around the cylinder instead of cutting through its surface.
        int pieces=Math.abs(a-b)<.0001F?1:detail==0?4:detail==1?3:2;
        for(int i=0;i<pieces;i++){
            float u=(float)i/pieces,v=(float)(i+1)/pieces;
            float aa=a+(b-a)*u,bb=a+(b-a)*v,z=z0+(z1-z0)*u,zz=z0+(z1-z0)*v;
            quad(out,cos(aa-halfWidth)*radius,sin(aa-halfWidth)*radius,z,
                    cos(bb-halfWidth)*radius,sin(bb-halfWidth)*radius,zz,
                    cos(bb+halfWidth)*radius,sin(bb+halfWidth)*radius,zz,
                    cos(aa+halfWidth)*radius,sin(aa+halfWidth)*radius,z,color,alpha);
        }
    }

    /** Polar swept surface: rounded lower skirt, narrow waist, slanted hooked mouth. */
    private static float[] shell(float a,float u,float offset){
        float c=cos(a),s=sin(a),horn=positivePower(cos(a+.12F),8);
        float rear=-.30F-.27F*c+.045F*sin(a*2);
        float front=.26F+.68F*horn+.12F*positivePower(-c,6);
        float root=.405F+.115F*c+.020F*sin(a*2);
        float tip=.222F+.115F*horn;
        float v=1-u;
        float radius=v*v*root+2*v*u*.229F+u*u*tip+offset;
        return new float[]{-.018F*v+c*radius,s*radius*.88F,rear+(front-rear)*u};
    }

    private static void shellQuad(EaVfxMesh.Sink out,float a,float b,float u,float v,float offset,boolean inner){
        shellVertex(out,a,u,offset,inner,-1);shellVertex(out,b,u,offset,inner,-1);
        shellVertex(out,b,v,offset,inner,-1);shellVertex(out,a,v,offset,inner,-1);
    }

    // Called only while baking the immutable hilt. Finite differences keep
    // highlights continuous across the twisted shell, including the lip's horns.
    private static void shellVertex(EaVfxMesh.Sink out,float a,float u,float offset,boolean inner,int color){
        float[] p=shell(a,u,offset);
        if(color<0){
            float[] left=shell(a-.002F,u,offset),right=shell(a+.002F,u,offset);
            float[] down=shell(a,u-.002F,offset),up=shell(a,u+.002F,offset);
            float ax=right[0]-left[0],ay=right[1]-left[1],az=right[2]-left[2];
            float bx=up[0]-down[0],by=up[1]-down[1],bz=up[2]-down[2];
            color=metal(ay*bz-az*by,az*bx-ax*bz,ax*by-ay*bx);
            if(inner)color=mix(GOLD_SHADOW,color,.36F);
        }
        out.vertex(p[0],p[1],p[2],color,1);
    }

    private static void shellLip(EaVfxMesh.Sink out,float a,float b,float u,int detail){
        int steps=detail==0?4:2;
        float sign=u==0?-1:1;
        for(int i=0;i<steps;i++){
            float p=(float)Math.PI*i/steps,q=(float)Math.PI*(i+1)/steps;
            float r0=-.0115F+.0115F*cos(p),r1=-.0115F+.0115F*cos(q);
            float t0=u+sign*.014F*sin(p),t1=u+sign*.014F*sin(q);
            shellVertex(out,a,t0,r0,false,PALE_GOLD);shellVertex(out,b,t0,r0,false,PALE_GOLD);
            shellVertex(out,b,t1,r1,false,GOLD);shellVertex(out,a,t1,r1,false,GOLD);
        }
    }

    private static void flame(EaVfxMesh.Sink out,float angle,float start,float end,
                              float width,float sweep,int sides,int rows){
        int steps=rows;
        for(int i=0;i<steps;i++){
            float u=(float)i/steps,v=(float)(i+1)/steps;
            float a=angle+sweep*u*u,b=angle+sweep*v*v;
            float w=width*(float)Math.pow(Math.max(0,sin((float)Math.PI*u)),.8F)*(1-u*.55F);
            float ww=width*(float)Math.pow(Math.max(0,sin((float)Math.PI*v)),.8F)*(1-v*.55F);
            float z=start+(end-start)*u,zz=start+(end-start)*v;
            shellInlay(out,new float[][]{{a-w,z},{a+w,z},{b+ww,zz},{b-ww,zz}},sides,rows);
        }
    }

    private static void shellMark(EaVfxMesh.Sink out,float a,float b,float u,float v,int sides,int rows){
        shellInlay(out,new float[][]{{a,u},{b,u},{b,v},{a,v}},sides,rows);
    }

    /** Clip decals to the actual LOD triangles; analytic decals can cut through
     * a coarse shell, leaving ragged holes during rotation. This is bake-time only. */
    private static void shellInlay(EaVfxMesh.Sink out,float[][] polygon,int sides,int rows){
        float minA=Float.POSITIVE_INFINITY,maxA=Float.NEGATIVE_INFINITY;
        float minU=1,maxU=0;
        for(float[] p:polygon){minA=Math.min(minA,p[0]);maxA=Math.max(maxA,p[0]);minU=Math.min(minU,p[1]);maxU=Math.max(maxU,p[1]);}
        int first=(int)Math.floor(minA*sides/TAU),last=(int)Math.floor(maxA*sides/TAU);
        int bottom=Math.max(0,(int)Math.floor(minU*rows)),top=Math.min(rows-1,(int)Math.floor(maxU*rows));
        for(int i=first;i<=last;i++)for(int j=bottom;j<=top;j++){
            float a=TAU*i/sides,b=TAU*(i+1)/sides,u=(float)j/rows,v=(float)(j+1)/rows;
            clippedInlay(out,polygon,new float[][]{{a,u},{b,u},{b,v}});
            clippedInlay(out,polygon,new float[][]{{a,u},{b,v},{a,v}});
        }
    }

    private static void clippedInlay(EaVfxMesh.Sink out,float[][] polygon,float[][] triangle){
        float[][] clipped=polygon;
        for(int edge=0;edge<3;edge++){
            float[] a=triangle[edge],b=triangle[(edge+1)%3];
            java.util.ArrayList<float[]> next=new java.util.ArrayList<>();
            for(int i=0;i<clipped.length;i++){
                float[] p=clipped[i],q=clipped[(i+1)%clipped.length];
                float d=cross(a,b,p),dd=cross(a,b,q);
                if(d>=0)next.add(p);
                if((d>=0)!=(dd>=0)){
                    float t=d/(d-dd);
                    next.add(new float[]{p[0]+(q[0]-p[0])*t,p[1]+(q[1]-p[1])*t});
                }
            }
            if(next.size()<3)return;
            clipped=next.toArray(new float[0][]);
        }
        float[][] points={shell(triangle[0][0],triangle[0][1],0),
                shell(triangle[1][0],triangle[1][1],0),shell(triangle[2][0],triangle[2][1],0)};
        float area=cross(triangle[0],triangle[1],triangle[2]);
        // Every clipped patch is convex and planar: pack adjacent triangles
        // into quads to avoid doubling the cached decal vertex budget.
        for(int i=1;i<clipped.length-1;i+=2){
            int last=Math.min(i+2,clipped.length-1);
            decalVertex(out,clipped[0],triangle,points,area);
            decalVertex(out,clipped[i],triangle,points,area);
            decalVertex(out,clipped[i+1],triangle,points,area);
            decalVertex(out,clipped[last],triangle,points,area);
        }
    }

    private static void decalVertex(EaVfxMesh.Sink out,float[] p,float[][] triangle,float[][] points,float area){
        float u=cross(triangle[1],triangle[2],p)/area,v=cross(triangle[2],triangle[0],p)/area,w=1-u-v;
        float x=u*points[0][0]+v*points[1][0]+w*points[2][0];
        float y=u*points[0][1]+v*points[1][1]+w*points[2][1];
        float z=u*points[0][2]+v*points[1][2]+w*points[2][2];
        out.vertex(x+cos(p[0])*.0012F,y+sin(p[0])*.0012F,z,PURPLE,1);
    }

    /** Rounded metal fittings, with smooth normal gradients at every profile ring. */
    private static void lathe(EaVfxMesh.Sink out,float[][] profile,int sides){
        for(int j=0;j<profile.length-1;j++)for(int i=0;i<sides;i++){
            float a=TAU*i/sides,b=TAU*(i+1)/sides;
            latheVertex(out,profile,j,a);latheVertex(out,profile,j,b);
            latheVertex(out,profile,j+1,b);latheVertex(out,profile,j+1,a);
        }
    }

    private static void latheVertex(EaVfxMesh.Sink out,float[][] profile,int ring,float a){
        float[] p=profile[ring],prev=profile[Math.max(0,ring-1)],next=profile[Math.min(profile.length-1,ring+1)];
        float dz=next[0]-prev[0],dr=next[1]-prev[1],dx=next[2]-prev[2];
        float c=cos(a),s=sin(a);
        out.vertex(p[2]+c*p[1],s*p[1],p[0],metal(c*dz,s*dz,-dr-c*dx),1);
    }

    private static void spiralCap(EaVfxMesh.Sink out,int steps,int sides){
        for(int i=0;i<steps;i++)for(int j=0;j<sides;j++){
            float u=(float)i/steps,v=(float)(i+1)/steps,a=TAU*j/sides,b=TAU*(j+1)/sides;
            capVertex(out,u,a);capVertex(out,v,a);capVertex(out,v,b);capVertex(out,u,b);
        }
    }

    private static void capVertex(EaVfxMesh.Sink out,float u,float a){
        float turn=u*TAU*.93F+.35F,ca=cos(turn),sa=sin(turn);
        float thickness=.023F*(.35F+.65F*sin((float)Math.PI*u));
        float radius=.158F-.042F*u+thickness*cos(a);
        out.vertex(ca*radius,sa*radius,2.791F+.130F*u+thickness*sin(a),
                metal(ca*cos(a),sa*cos(a),sin(a)),1);
    }

    private static int metal(float x,float y,float z){
        float length=(float)Math.sqrt(x*x+y*y+z*z);
        if(length<.0000001F)return GOLD;
        x/=length;y/=length;z/=length;
        float diffuse=clamp((-.40F*x-.72F*y+.55F*z)*.5F+.5F);
        float highlight=positivePower(-.28F*x-.84F*y+.46F*z,12);
        return mix(shade(GOLD,.48F+.48F*diffuse),GOLD_EDGE,highlight*.88F);
    }

    private static int steel(int color,float angle){
        float light=(cos(angle+.72F)+1)*.5F;
        return mix(shade(color,.50F+.45F*light),0x6C6670,positivePower(cos(angle+1.13F),18)*.27F);
    }

    private static float positivePower(float value,int exponent){
        return (float)Math.pow(Math.max(0,value),exponent);
    }
    private static float cross(float[] a,float[] b,float[] p){return (b[0]-a[0])*(p[1]-a[1])-(b[1]-a[1])*(p[0]-a[0]);}
    private static void triangle(EaVfxMesh.Sink out,float x,float y,float z,float xx,float yy,float zz,
                                 float xxx,float yyy,float zzz,int color,float alpha){
        quad(out,x,y,z,xx,yy,zz,xxx,yyy,zzz,xxx,yyy,zzz,color,alpha);
    }
    private static void quad(EaVfxMesh.Sink out,float x,float y,float z,float xx,float yy,float zz,
                            float xxx,float yyy,float zzz,float xxxx,float yyyy,float zzzz,int color,float alpha){
        out.vertex(x,y,z,color,alpha);out.vertex(xx,yy,zz,color,alpha);
        out.vertex(xxx,yyy,zzz,color,alpha);out.vertex(xxxx,yyyy,zzzz,color,alpha);
    }
    private static int mix(int a,int b,float t){return ((int)((a>>16&255)+((b>>16&255)-(a>>16&255))*t)<<16)
            |((int)((a>>8&255)+((b>>8&255)-(a>>8&255))*t)<<8)|(int)((a&255)+((b&255)-(a&255))*t);}
    private static int shade(int color,float factor){return ((int)((color>>16&255)*factor)<<16)
            |((int)((color>>8&255)*factor)<<8)|(int)((color&255)*factor);}
    private static float clamp(float f){return Math.max(0,Math.min(1,f));}
    private static float sin(float f){return (float)Math.sin(f);}
    private static float cos(float f){return (float)Math.cos(f);}
}
