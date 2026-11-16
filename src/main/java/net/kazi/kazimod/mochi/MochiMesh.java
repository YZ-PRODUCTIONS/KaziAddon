package net.kazi.kazimod.mochi;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.*;

/** Opaque, batched liquid surfaces: no nested translucent shells or particle entities. */
public final class MochiMesh {
    public static final int CREAM=0, HAKI=1, TOASTED=2;
    public static final RenderType SURFACE=Surface.TYPE;
    private static final double[][][][] BLOBS={
            {blobGrid(16,8,1),blobGrid(16,8,.58),blobGrid(16,8,.68)},
            {blobGrid(6,3,1),blobGrid(6,3,.58),blobGrid(6,3,.68)}};
    private static final class Surface extends RenderType {
        private static final RenderType TYPE=create("kazi_liquid_mochi",DefaultVertexFormats.POSITION_COLOR,7,262144,
            false,false,State.builder().setTransparencyState(NO_TRANSPARENCY).setCullState(NO_CULL)
                    .setShadeModelState(SMOOTH_SHADE).setDepthTestState(LEQUAL_DEPTH_TEST)
                    .setWriteMaskState(COLOR_DEPTH_WRITE).createCompositeState(false));
        private Surface(String n,Runnable a,Runnable b){super(n,DefaultVertexFormats.POSITION_COLOR,7,256,false,false,a,b);}
    }
    private final MatrixStack stack;
    private final IVertexBuilder out;
    private final float age, brightness;
    private final boolean far;
    private final int sides;
    public MochiMesh(MatrixStack stack,IRenderTypeBuffer buffers,float age,boolean far,int light) {
        this.stack=stack;this.out=buffers.getBuffer(SURFACE);this.age=age;this.far=far;this.sides=far?6:16;
        this.brightness=.62F+.38F*Math.max((light>>4)&15,(light>>20)&15)/15F;
    }
    private void vertex(Vector3d p,Vector3d normal,int material) {
        vertex(p.x,p.y,p.z,normal.x,normal.y,normal.z,material);
    }
    private void vertex(double x,double y,double z,double nx,double ny,double nz,int material) {
        double length=Math.sqrt(nx*nx+ny*ny+nz*nz);
        if(length>.000001){nx/=length;ny/=length;nz/=length;}
        double diffuse=Math.max(0,nx*-.37+ny*.78+nz*.50);
        double h=Math.max(0,nx*-.32+ny*.48+nz*.817),h2=h*h,h4=h2*h2,h8=h4*h4;
        double gloss=h8*h8*h8*h4;
        float shade=(float)(.69+.29*diffuse),shine=(float)gloss;
        float r,g,b;
        if(material==HAKI){r=.045F+shade*.055F+shine*.35F;g=.028F+shade*.045F+shine*.31F;b=.065F+shade*.075F+shine*.40F;}
        else if(material==TOASTED){r=shade;g=shade*.60F+shine*.14F;b=shade*.28F+shine*.20F;}
        else {r=Math.min(1,shade+.055F*shine);g=Math.min(1,shade*.982F+.065F*shine);b=Math.min(1,shade*.922F+.10F*shine);}
        out.vertex(stack.last().pose(),(float)x,(float)y,(float)z).color(r*brightness,g*brightness,b*brightness,1).endVertex();
    }
    public void blob(Vector3d center,double x,double y,double z,int material) {
        blob(center,x,y,z,material,1);
    }
    private void blob(Vector3d center,double x,double y,double z,int material,double roundness) {
        int rings=far?3:8;
        double[][] grid=BLOBS[far?1:0][roundness==1?0:roundness==.58?1:2];
        double sin=Math.sin(age*.13),cos=Math.cos(age*.13);
        for(int j=0;j<rings;j++)for(int i=0;i<sides;i++) {
            blobVertex(center,x,y,z,grid[j*(sides+1)+i],material,sin,cos);blobVertex(center,x,y,z,grid[(j+1)*(sides+1)+i],material,sin,cos);
            blobVertex(center,x,y,z,grid[(j+1)*(sides+1)+i+1],material,sin,cos);blobVertex(center,x,y,z,grid[j*(sides+1)+i+1],material,sin,cos);
        }
    }
    private static double signedPower(double value,double power){return Math.copySign(Math.pow(Math.abs(value),power),value);}
    private static double[][] blobGrid(int count,int rings,double roundness) {
        double[][] result=new double[(count+1)*(rings+1)][8];
        for(int j=0;j<=rings;j++)for(int i=0;i<=count;i++) {
            double a=i*Math.PI*2/count,b=j*Math.PI/rings;
            double nx=Math.cos(a)*Math.sin(b),ny=Math.cos(b),nz=Math.sin(a)*Math.sin(b);
            result[j*(count+1)+i]=new double[]{signedPower(nx,roundness),signedPower(ny,roundness),signedPower(nz,roundness),
                    signedPower(nx,2-roundness),signedPower(ny,2-roundness),signedPower(nz,2-roundness),
                    .025*Math.sin(a*3)*Math.sin(b)*Math.sin(b),.025*Math.cos(a*3)*Math.sin(b)*Math.sin(b)};
        }
        return result;
    }
    private void blobVertex(Vector3d c,double x,double y,double z,double[] p,int material,double sin,double cos) {
        double ripple=material==HAKI?1:1+p[6]*cos+p[7]*sin;
        vertex(c.x+p[0]*x*ripple,c.y+p[1]*y*ripple,c.z+p[2]*z*ripple,p[3]/x,p[4]/y,p[5]/z,material);
    }
    public static Vector3d bezier(Vector3d a,Vector3d b,Vector3d c,Vector3d d,double t) {
        double u=1-t;return a.scale(u*u*u).add(b.scale(3*u*u*t)).add(c.scale(3*u*t*t)).add(d.scale(t*t*t));
    }
    public void strand(Vector3d start,Vector3d control1,Vector3d control2,Vector3d end,double r0,double r1,int material) {
        int segments=far?5:10;
        Vector3d[] points=new Vector3d[segments+1];double[] radii=new double[segments+1];
        for(int i=0;i<=segments;i++) {
            double t=i/(double)segments;
            points[i]=bezier(start,control1,control2,end,t);
            radii[i]=Math.max(.003,MathHelper.lerp(t,r0,r1)*(1+.035*Math.sin(t*10-age*.18)*Math.sin(Math.PI*t)));
        }
        tube(points,radii,material);
    }
    public void tube(Vector3d[] points,double[] radii,int material) {
        if(points.length<2)return;
        Vector3d[][] vertices=new Vector3d[points.length][sides+1],normals=new Vector3d[points.length][sides+1];
        Vector3d previous=null;
        for(int j=0;j<points.length;j++) {
            Vector3d direction=points[Math.min(j+1,points.length-1)].subtract(points[Math.max(0,j-1)]).normalize();
            if(direction.lengthSqr()<.01)direction=new Vector3d(0,1,0);
            Vector3d side;
            if(previous==null)side=direction.cross(Math.abs(direction.y)<.9?new Vector3d(0,1,0):new Vector3d(1,0,0)).normalize();
            else {side=previous.subtract(direction.scale(previous.dot(direction))).normalize();if(side.lengthSqr()<.01)side=direction.cross(new Vector3d(0,0,1)).normalize();}
            Vector3d up=direction.cross(side).normalize();previous=side;
            for(int i=0;i<=sides;i++) {
                double a=i*Math.PI*2/sides;Vector3d n=side.scale(Math.cos(a)).add(up.scale(Math.sin(a)));
                normals[j][i]=n;vertices[j][i]=points[j].add(n.scale(radii[j]));
            }
        }
        for(int j=0;j<points.length-1;j++)for(int i=0;i<sides;i++) {
            vertex(vertices[j][i],normals[j][i],material);vertex(vertices[j+1][i],normals[j+1][i],material);
            vertex(vertices[j+1][i+1],normals[j+1][i+1],material);vertex(vertices[j][i+1],normals[j][i+1],material);
        }
    }
    public void ring(double radius,double thickness,int material,boolean spikes) {
        int segments=far?24:48,around=far?6:10;
        for(int i=0;i<segments;i++)for(int j=0;j<around;j++) {
            ringVertex(i,j,segments,around,radius,thickness,material);ringVertex(i+1,j,segments,around,radius,thickness,material);
            ringVertex(i+1,j+1,segments,around,radius,thickness,material);ringVertex(i,j+1,segments,around,radius,thickness,material);
        }
        if(spikes)for(int i=0;i<12;i++) {
            double a=i*Math.PI/6+age*.035;Vector3d radial=new Vector3d(Math.cos(a),Math.sin(a),0);
            Vector3d p=radial.scale(radius+thickness*.6),q=radial.scale(radius+thickness*2.8);
            strand(p,p.add(radial.scale(thickness)),q,q,thickness*.32,.005,HAKI);
        }
    }
    private void ringVertex(int i,int j,int count,int around,double radius,double thickness,int material) {
        double a=i*Math.PI*2/count,b=j*Math.PI*2/around;
        double r=radius*(1+.028*Math.sin(a*3-age*.075)+.016*Math.sin(a*5+age*.11));
        double t=thickness*(1+.075*Math.sin(a*4-age*.12));
        vertex(Math.cos(a)*(r+t*Math.cos(b)),Math.sin(a)*(r+t*Math.cos(b)),Math.sin(b)*t,
                Math.cos(a)*Math.cos(b),Math.sin(a)*Math.cos(b),Math.sin(b),material);
    }
    /** Local +Z is the striking direction. Knuckles retain a readable fist silhouette. */
    public void fist(double radius,boolean hardened,boolean open) {
        int material=hardened?HAKI:CREAM;
        blob(new Vector3d(0,0,0),radius,radius*.80,radius*.82,material,.58);
        if(far&&!open)blob(new Vector3d(0,radius*.38,radius*.73),radius*.89,radius*.36,radius*.30,material,.68);
        for(int i=0;i<(far&&!open?0:4);i++) {
            double x=(i-1.5)*radius*.43;
            if(open)strand(new Vector3d(x,radius*.12,radius*.48),new Vector3d(x,radius*.18,radius),
                    new Vector3d(x,radius*.38,radius*1.7),new Vector3d(x,radius*.12,radius*1.8),radius*.17,radius*.10,material);
            else blob(new Vector3d(x,radius*.38,radius*.73),radius*.25,radius*.36,radius*.30,material,.68);
        }
        blob(new Vector3d(radius*.84,-radius*.18,radius*.20),radius*.30,radius*.48,radius*.40,material);
    }
    public void drips(double radius,double y,int count,int seed) {
        for(int i=0;i<(far?Math.min(count,2):count);i++) {
            double a=i*2.399963+seed*.17;
            Vector3d root=new Vector3d(Math.cos(a)*radius,y,Math.sin(a)*radius*.55);
            drip(root,radius,i,seed);
        }
    }
    public void ringDrips(double radius,double thickness,int count,int seed) {
        int n=far?Math.min(count,2):count;
        for(int i=0;i<n;i++) {
            double a=Math.PI*(1.12+.76*(i+.5)/n);
            double r=radius*(1+.028*Math.sin(a*3-age*.075)+.016*Math.sin(a*5+age*.11));
            drip(new Vector3d(Math.cos(a)*r,Math.sin(a)*r-thickness*.65,0),radius*.7,i,seed);
        }
    }
    private void drip(Vector3d root,double radius,int i,int seed) {
            double cycle=(age*.021+i*.271+seed*.013)%1;if(cycle<0)cycle++;
            double grow=Math.min(1,cycle/.72),length=radius*(.22+.8*grow*grow);
            if(cycle<.76) {
                Vector3d tip=root.add(Math.sin(age*.08+i)*radius*.045,-length,0);
                strand(root,root.add(0,-length*.25,0),tip.add(0,length*.25,0),tip,
                        radius*.065*(1-grow*.65),radius*.045,CREAM);
                blob(tip,radius*.075,radius*.11,radius*.075,CREAM);
            } else {
                double fall=(cycle-.76)/.24;
                double size=radius*.075*(1-fall*.75);
                blob(root.add(0,-length-fall*fall*radius*2,0),size,size*(1.5+fall),size,CREAM);
            }
    }
    public void tile(int x,int z) {
        int grid=far?2:4;
        for(int j=0;j<grid;j++)for(int i=0;i<grid;i++) {
            tileVertex(x,z,i/(double)grid,j/(double)grid);tileVertex(x,z,(i+1.0)/grid,j/(double)grid);
            tileVertex(x,z,(i+1.0)/grid,(j+1.0)/grid);tileVertex(x,z,i/(double)grid,(j+1.0)/grid);
        }
    }
    private void tileVertex(int x,int z,double u,double v) {
        double wave=(x+u)*1.8+(z+v)*.7-age*.08;
        double y=.027+.010*Math.sin(wave)+.004*Math.sin((z+v)*3-age*.10);
        vertex(u,y,v,-.018*Math.cos(wave),1,-.007*Math.cos(wave),CREAM);
    }
    public void splash(double radius,double progress,int seed) {
        double r=radius*(.2+.8*Math.min(1,progress*2));
        blob(new Vector3d(0,.025,0),r,.08*(1-progress)+.015,r,CREAM);
        if(progress>.75)return;
        int lobes=far?6:12;
        for(int i=0;i<lobes;i++) {
            double a=i*Math.PI*2/lobes+seed*.1;Vector3d dir=new Vector3d(Math.cos(a),0,Math.sin(a));
            double height=radius*(.24+.18*Math.sin(i*2.7+seed))*Math.sin(Math.PI*Math.min(1,progress*1.4));
            Vector3d start=dir.scale(r*.48),tip=dir.scale(r*1.18).add(0,height,0);
            strand(start,start.add(0,height*.9,0),tip.add(0,height*.5,0),tip,radius*.065,.005,CREAM);
        }
    }
    public void river(double width,double height) {
        int nx=far?12:24,ny=far?8:16;
        for(int shell=0;shell<2;shell++)for(int j=0;j<ny;j++)for(int i=0;i<nx;i++) {
            riverVertex(i,j,nx,ny,width,height,shell);riverVertex(i+1,j,nx,ny,width,height,shell);
            riverVertex(i+1,j+1,nx,ny,width,height,shell);riverVertex(i,j+1,nx,ny,width,height,shell);
        }
        for(int edge=0;edge<3;edge++) {
            int count=edge==0?nx:ny;Vector3d[] points=new Vector3d[count+1];double[] radii=new double[count+1];
            for(int i=0;i<=count;i++) {
                double u=edge==0?i/(double)count:edge==1?0:1,t=edge==0?1:i/(double)count;
                points[i]=riverPoint(u,t,width,height);radii[i]=width*.035*(.8+.2*Math.sin(u*10-age*.1));
            }
            tube(points,radii,CREAM);
        }
    }
    private Vector3d riverPoint(double u,double t,double width,double height) {
        double angle=t*Math.PI*.78,h=height*(.94+.06*Math.sin(u*9+age*.085))*(.87+.13*Math.sin(u*Math.PI));
        return new Vector3d((u-.5)*width,Math.sin(angle)*h,
                -height*.32*(1-Math.cos(angle))+.13*Math.sin(u*14-age*.12+t*8)*Math.sin(Math.PI*t));
    }
    private void riverVertex(int i,int j,int nx,int ny,double width,double height,int shell) {
        double u=i/(double)nx,t=j/(double)ny,angle=t*Math.PI*.78;
        Vector3d p=riverPoint(u,t,width,height);
        double nyc=Math.sin(angle)*.32,nzc=Math.cos(angle),offset=(shell==0?1:-1)*width*.025;
        vertex(p.x,p.y+nyc*offset,p.z+nzc*offset,.1*Math.cos(u*14-age*.12+t*8),nyc*(shell==0?1:-1),nzc*(shell==0?1:-1),CREAM);
    }
    public static void orient(MatrixStack stack,Vector3d direction) {
        Vector3d d=direction.normalize();
        stack.mulPose(Vector3f.YP.rotation((float)Math.atan2(d.x,d.z)));
        stack.mulPose(Vector3f.XP.rotation((float)-Math.asin(MathHelper.clamp(d.y,-1,1))));
    }
}
