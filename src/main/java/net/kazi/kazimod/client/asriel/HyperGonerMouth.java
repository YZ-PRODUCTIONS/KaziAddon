package net.kazi.kazimod.client.asriel;

/** An opaque, recessed lining fitted behind the imported skull's animated mouth. */
public final class HyperGonerMouth {
    // Original Collada vertex anchors, in perimeter order:
    // 203,3880,3921,5046,291,175,181,5356,5309,5214,4143,4190,83,77.
    // Keep each anchor's depth: the cheek strips sit behind the upper and lower teeth.
    private static final float[] CLOSED={
        -1.4729088F,-.6462384F,-.8418986F,
        -.8701226F,-.8902583F,.7039024F,
        -.0080586F,-.9446339F,.3888200F,
        .8403750F,-.8898177F,.7039024F,
        1.4500125F,-.5268476F,-.8418986F,
        1.7975052F,-.6483472F,-2.1846048F,
        1.5718846F,-1.2416498F,-1.6044719F,
        1.1505993F,-2.2272624F,-2.2529761F,
        .5656037F,-3.0472463F,.3122309F,
        -.0125627F,-3.2030678F,.6474153F,
        -.5886562F,-3.0476869F,.3122309F,
        -1.1879079F,-2.2277032F,-2.2529761F,
        -1.6455973F,-1.2416498F,-1.6044719F,
        -1.8787497F,-.6483472F,-2.1846048F
    };
    // Only the first five anchors move with the author's laughing expression.
    private static final float[] OPEN_ROOF={
        -1.4729088F,1.0360447F,-.5926668F,
        -.8701226F,1.8165443F,.7637495F,
        -.0080586F,1.5764137F,.5526323F,
        .8403750F,1.8168878F,.7634737F,
        1.4500123F,1.1289772F,-.6676204F
    };
    private static final float[] DEPTH={0,.12F,.30F,.60F,1};
    private static final int[] SHADE={0x191A1E,0x08090B,0x010102,0,0};
    private static final int POINTS=CLOSED.length/3;
    private static final float BACK_Z=-4.45F;
    private HyperGonerMouth() { }

    public static void render(HyperGonerMesh.Sink out,float open) {
        if(!Float.isFinite(open))return;
        open=Math.max(0,Math.min(1,open));
        for(int row=0;row<DEPTH.length-1;row++)for(int i=0;i<POINTS;i++) {
            int next=(i+1)%POINTS;
            vertex(out,i,open,DEPTH[row],SHADE[row]);
            vertex(out,next,open,DEPTH[row],SHADE[row]);
            vertex(out,next,open,DEPTH[row+1],SHADE[row+1]);
            vertex(out,i,open,DEPTH[row+1],SHADE[row+1]);
        }
        // A closed back blocks the world behind the skull, even at oblique angles.
        // It is part of the mouth volume, rather than a separate backdrop plane.
        for(int i=0;i<POINTS;i++) {
            out.vertex(0,centerY(open),BACK_Z,0,1);
            vertex(out,i,open,1,0);
            vertex(out,(i+1)%POINTS,open,1,0);
            vertex(out,(i+1)%POINTS,open,1,0);
        }
    }

    private static float centerY(float open) {return -2+open*1.2F;}

    private static void vertex(HyperGonerMesh.Sink out,int point,float open,float depth,int color) {
        int p=point*3;
        float x=CLOSED[p],y=CLOSED[p+1],z=CLOSED[p+2];
        if(p<OPEN_ROOF.length) {
            x+=(OPEN_ROOF[p]-x)*open;
            y+=(OPEN_ROOF[p+1]-y)*open;
            z+=(OPEN_ROOF[p+2]-z)*open;
        }
        z-=.03F; // Conceal the lining seam inside the existing bone.
        float shrink=1-depth*.72F,center=centerY(open);
        out.vertex(x*shrink,center+(y-center)*shrink,z+(BACK_Z-z)*depth,color,1);
    }
}
