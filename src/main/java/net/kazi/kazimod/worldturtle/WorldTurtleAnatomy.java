package net.kazi.kazimod.worldturtle;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import net.kazi.kazimod.models.zoan.CerberusRig;

/** Bone-local bounds baked once; only eight corners per part are animated at runtime. */
public final class WorldTurtleAnatomy {
    public static final String[] BONES={"Shell and plastron","Neck","Head","Front flipper swimming",
            "Front_flipper_swimming2","Rear flipper left","Rear flipper right","Tail"};
    private static final class Data {
        static final CerberusRig RIG=load();
        static final double[][] LOCAL=bake(RIG);
        static final List<List<CerberusRig.Node>> PATHS=paths();
        static final double[][] REST=rest();
        private static List<List<CerberusRig.Node>> paths(){List<List<CerberusRig.Node>> p=new ArrayList<>();for(String bone:BONES)p.add(RIG.path(bone));return p;}
        private static double[][] rest(){double[][] r=new double[BONES.length][];for(int i=0;i<r.length;i++)r[i]=bounds(i,Collections.emptyMap(),0,1);return r;}
    }
    public static CerberusRig rig(){return Data.RIG;}
    /** Constant box dimensions; only its center follows the owner's facing. */
    public static double[] fixedBounds(int part,float yaw,float scale){
        double[] r=Data.REST[part];double a=Math.toRadians(yaw),c=Math.cos(a),s=Math.sin(a);
        double x=(r[0]+r[3])*.5*scale,z=(r[2]+r[5])*.5*scale;
        double cx=c*x-s*z,cz=s*x+c*z,hx=(r[3]-r[0])*.5*scale,hz=(r[5]-r[2])*.5*scale;
        return new double[]{cx-hx,r[1]*scale,cz-hz,cx+hx,r[4]*scale,cz+hz};
    }
    private static CerberusRig load(){
        try(Reader r=new InputStreamReader(Objects.requireNonNull(WorldTurtleAnatomy.class.getResourceAsStream(
                "/assets/kazimod/models/zoan/world_turtle.json")),StandardCharsets.UTF_8)){return CerberusRig.read(r);}
        catch(IOException e){throw new IllegalStateException("World Turtle anatomy",e);}
    }
    private static double[][] bake(CerberusRig rig){
        double[][] result=new double[BONES.length][];
        for(int i=0;i<BONES.length;i++){
            List<CerberusRig.Node> path=rig.path(BONES[i]);double[] b=empty();
            collect(path.get(path.size()-1).children,b);result[i]=b;
        }
        return result;
    }
    private static void collect(CerberusRig.Node[] nodes,double[] b){
        for(CerberusRig.Node n:nodes)if(n.visible){
            // Head has its own part, so it must not also enlarge the neck's box.
            if(n.children!=null){if(!Arrays.asList(BONES).contains(n.name))collect(n.children,b);}
            else if(n.quads!=null)for(CerberusRig.Quad q:n.quads)for(float[] v:q.vertices)include(b,v[0],v[1],v[2]);
        }
    }
    private static double[] empty(){return new double[]{Double.POSITIVE_INFINITY,Double.POSITIVE_INFINITY,Double.POSITIVE_INFINITY,
            Double.NEGATIVE_INFINITY,Double.NEGATIVE_INFINITY,Double.NEGATIVE_INFINITY};}
    private static void include(double[] b,double x,double y,double z){
        b[0]=Math.min(b[0],x);b[1]=Math.min(b[1],y);b[2]=Math.min(b[2],z);
        b[3]=Math.max(b[3],x);b[4]=Math.max(b[4],y);b[5]=Math.max(b[5],z);
    }
    public static double[] bounds(int part,Map<String,CerberusRig.Pose> pose,float bodyYaw,float scale){
        double[] local=Data.LOCAL[part],b=empty();List<CerberusRig.Node> path=Data.PATHS.get(part);
        double yaw=Math.toRadians(bodyYaw),sin=Math.sin(yaw),cos=Math.cos(yaw);
        for(int corner=0;corner<8;corner++){
            double[] p={local[(corner&1)==0?0:3],local[(corner&2)==0?1:4],local[(corner&4)==0?2:5]};
            for(int j=path.size()-1;j>=0;j--){
                CerberusRig.Node n=path.get(j);CerberusRig.Pose a=pose.get(n.name);
                for(int k=0;k<3;k++)p[k]-=n.origin[k];
                rotate(p,n.rotation[0]+(a==null?0:a.rotation[0]),0);
                rotate(p,n.rotation[1]+(a==null?0:a.rotation[1]),1);
                rotate(p,n.rotation[2]+(a==null?0:a.rotation[2]),2);
                for(int k=0;k<3;k++)p[k]+=n.origin[k]+(a==null?0:a.position[k]);
            }
            // Matches ZoanMorphRenderer's Y rotation, mirrored model and -1.501 offset.
            double x=p[0]*scale/16,z=p[2]*scale/16;
            include(b,cos*x+sin*z,(p[1]/16+1.001)*scale,sin*x-cos*z);
        }
        return b;
    }
    private static void rotate(double[] p,double degrees,int axis){
        if(degrees==0)return;
        int a=(axis+1)%3,b=(axis+2)%3;double c=Math.cos(Math.toRadians(degrees)),s=Math.sin(Math.toRadians(degrees)),v=p[a];
        p[a]=v*c-p[b]*s;p[b]=v*s+p[b]*c;
    }
}
