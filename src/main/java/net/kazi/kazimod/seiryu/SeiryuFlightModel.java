package net.kazi.kazimod.seiryu;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import java.lang.reflect.Field;
import java.util.*;
import net.MrMagicalCart.cartaddon.models.morphs.SeiryuFlyPoint2Model;
import net.kazi.kazimod.mixin.client.ZoanTransitionRenderAccessors;
import net.minecraft.client.renderer.model.ModelRenderer;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.*;

/** Original Cart geometry and UVs, skinned through a continuous, rotation-minimizing spine. */
public final class SeiryuFlightModel<T extends LivingEntity> extends SeiryuFlyPoint2Model<T> {
    private final ModelRenderer dragonHead, dragonBody, neck;
    private final ModelRenderer[] spine = new ModelRenderer[SeiryuSpine.SEGMENTS];
    private final List<Rest> rest = new ArrayList<>();
    private SeiryuCurve curve;
    private Vector3d origin = Vector3d.ZERO;
    private float bodyYaw;
    private double modelScale=SeiryuSpine.MODEL_SCALE;

    public SeiryuFlightModel() {
        dragonHead=part("Head");dragonBody=part("Body");neck=part("segment0");
        for(int i=0;i<spine.length;i++)spine[i]=part("segment"+(i+1));
        Set<ModelRenderer> sections=Collections.newSetFromMap(new IdentityHashMap<>());
        Collections.addAll(sections,spine);
        for(ModelRenderer segment:spine)children(segment).removeIf(sections::contains);
        children(neck).clear();spine[0].addChild(neck);
        children(dragonBody).clear();
        for(ModelRenderer segment:spine)dragonBody.addChild(segment);
        straighten();
        capture(dragonHead);capture(dragonBody);
    }
    private void capture(ModelRenderer part) {
        rest.add(new Rest(part));for(ModelRenderer child:children(part))capture(child);
    }
    private ModelRenderer part(String name) {
        try {Field field=SeiryuFlyPoint2Model.class.getDeclaredField(name);field.setAccessible(true);return (ModelRenderer)field.get(this);}
        catch(ReflectiveOperationException e){throw new IllegalStateException("Seiryu rig changed: "+name,e);}
    }
    private static List<ModelRenderer> children(ModelRenderer part) {
        return ((ZoanTransitionRenderAccessors.Children)(Object)part).kazimod$children();
    }
    private static void place(ModelRenderer part,float x,float y,float z) {
        part.setPos(x,y,z);part.xRot=part.yRot=part.zRot=0;
    }
    private void straighten() {
        place(dragonBody,0,0,0);place(neck,0,0,0);
        place(dragonHead,0,.3659F,-17.6791F);
        double distance=0;
        for(int i=0;i<spine.length;i++) {
            place(spine[i],0,0,(float)(distance/SeiryuSpine.UNIT));
            distance+=SeiryuSpine.length(i);
        }
    }
    @Override public void setupAnim(T owner,float limbSwing,float limbAmount,float age,float yaw,float pitch) {
        for(Rest pose:rest)pose.restore();
        super.setupAnim(owner,limbSwing,limbAmount,age,yaw,pitch);
        straighten(); // Native jaw/limb animation remains, but cannot accumulate body rotations.
        float partial=MathHelper.clamp(age-owner.tickCount,0,1);
        bodyYaw=MathHelper.rotLerp(partial,owner.yBodyRotO,owner.yBodyRot);
        origin=SeiryuClient.position(owner,partial);
        modelScale=SeiryuSpine.MODEL_SCALE*SeiryuFlight.scale(owner);
        curve=SeiryuClient.curve(owner,partial);
    }
    @Override public void renderToBuffer(MatrixStack stack,IVertexBuilder vertices,int light,int overlay,float r,float g,float b,float a) {
        if(curve==null){super.renderToBuffer(stack,vertices,light,overlay,r,g,b,a);return;}
        MatrixStack neutral=new MatrixStack();
        dragonHead.render(neutral,new Skin(vertices,stack.last(),true),light,overlay,r,g,b,a);
        dragonBody.render(neutral,new Skin(vertices,stack.last(),false),light,overlay,r,g,b,a);
    }
    public static Vector3d local(Vector3d world,float yaw) {
        double a=Math.toRadians(yaw),c=Math.cos(a),s=Math.sin(a);
        return new Vector3d(world.x*c+world.z*s,world.y,world.x*s-world.z*c);
    }
    private final class Skin implements IVertexBuilder {
        final IVertexBuilder out;
        final MatrixStack.Entry matrix;
        final boolean head;
        SeiryuCurve.Frame frame;
        Skin(IVertexBuilder out,MatrixStack.Entry matrix,boolean head){this.out=out;this.matrix=matrix;this.head=head;}
        public IVertexBuilder vertex(double x,double y,double z) {
            frame=head?curve.head(z*modelScale):curve.sample(z*modelScale);
            Vector3d p=local(frame.position(x*modelScale,y*modelScale).subtract(origin),bodyYaw);
            out.vertex(matrix.pose(),(float)(p.x/modelScale),
                    (float)(1.501-p.y/modelScale),(float)(p.z/modelScale));
            return this;
        }
        public IVertexBuilder normal(float x,float y,float z) {
            Vector3d n=local(frame.normal(x,y,z),bodyYaw);
            out.normal(matrix.normal(),(float)n.x,(float)-n.y,(float)n.z);return this;
        }
        public IVertexBuilder color(int r,int g,int b,int a){out.color(r,g,b,a);return this;}
        public IVertexBuilder uv(float u,float v){out.uv(u,v);return this;}
        public IVertexBuilder overlayCoords(int u,int v){out.overlayCoords(u,v);return this;}
        public IVertexBuilder uv2(int u,int v){out.uv2(u,v);return this;}
        public void endVertex(){out.endVertex();}
    }
    private static final class Rest {
        final ModelRenderer part;final float x,y,z,rx,ry,rz;
        Rest(ModelRenderer p){part=p;x=p.x;y=p.y;z=p.z;rx=p.xRot;ry=p.yRot;rz=p.zRot;}
        void restore(){part.setPos(x,y,z);part.xRot=rx;part.yRot=ry;part.zRot=rz;}
    }
}
