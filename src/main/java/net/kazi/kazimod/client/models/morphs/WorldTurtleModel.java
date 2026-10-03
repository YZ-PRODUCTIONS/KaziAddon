package net.kazi.kazimod.client.models.morphs;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.kazi.kazimod.models.zoan.CerberusRig;
import net.kazi.kazimod.models.zoan.WorldTurtleMotion;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.HandSide;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Matrix3f;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.Vector3f;
import net.minecraft.util.math.vector.Vector4f;
import net.kazi.kazimod.abilities.KameRework.DivineShieldAbility;
import net.kazi.kazimod.abilities.KameRework.WorldShakingSpinAbility;
import xyz.pixelatedw.mineminenomi.api.morph.MorphModel;
import java.util.*;

public final class WorldTurtleModel<T extends LivingEntity> extends MorphModel<T> {
    private static final Map<LivingEntity,WorldTurtleMotion> MOTION = new WeakHashMap<>();
    private static final class Data {
        static final CerberusRig RIG = net.kazi.kazimod.worldturtle.WorldTurtleAnatomy.rig();
    }
    private Map<String,CerberusRig.Pose> pose = Collections.emptyMap();
    private final Vector4f vertex = new Vector4f(0,0,0,1);
    private final Vector3f normal = new Vector3f();
    private float shield;
    private boolean filterBackFaces;
    public WorldTurtleModel() { super(0); }
    public static void preload() { Objects.requireNonNull(Data.RIG); }
    @Override public void setupAnim(T entity,float limbSwing,float limbAmount,float age,float yaw,float pitch) {
        float partial=MathHelper.clamp(age-entity.tickCount,0,1);
        WorldTurtleMotion motion=MOTION.computeIfAbsent(entity,e->new WorldTurtleMotion());
        boolean spinning=WorldShakingSpinAbility.active(entity);
        motion.shieldAt(DivineShieldAbility.active(entity)||spinning,age);
        motion.spinAt(spinning,age);
        shield=motion.shield();
        pose=motion.sample(Data.RIG,age,limbAmount,
                !entity.isOnGround(),entity.isSprinting(),yaw,pitch,
                MathHelper.wrapDegrees(entity.yBodyRot-entity.yBodyRotO),entity.getAttackAnim(partial));
    }
    @Override public void renderToBuffer(MatrixStack stack,IVertexBuilder buffer,int light,int overlay,float r,float g,float b,float alpha) {
        // GUI previews use an orthographic camera, so retain their full mesh.
        filterBackFaces=net.minecraft.client.Minecraft.getInstance().screen==null;
        stack.pushPose();
        stack.translate(0,1.5,0);
        stack.scale(1/16F,1/16F,1/16F);
        // Exact reflection avoids negative-scale normal corruption when looking up.
        stack.last().pose().multiply(Matrix4f.createScaleMatrix(1,-1,1));
        stack.last().normal().mul(Matrix3f.createScaleMatrix(1,-1,1));
        stack.translate(0,16,0);
        draw(Data.RIG.nodes,stack,buffer,light,overlay,r,g,b,alpha);
        stack.popPose();
    }
    private void draw(CerberusRig.Node[] nodes,MatrixStack s,IVertexBuilder buffer,int light,int overlay,float r,float g,float b,float alpha){
        for(CerberusRig.Node n:nodes){
            if(!n.visible)continue;
            if(n.children!=null){
                if(shield>.98F&&(n.name.equals("Neck")||n.name.equals("Tail")||n.name.toLowerCase(java.util.Locale.ROOT).contains("flipper")))continue;
                s.pushPose();CerberusRig.Pose p=pose.get(n.name);
                if(p!=null)s.translate(p.position[0],p.position[1],p.position[2]);
                s.translate(n.origin[0],n.origin[1],n.origin[2]);
                s.mulPose(Vector3f.ZP.rotationDegrees(n.rotation[2]+(p==null?0:p.rotation[2])));
                s.mulPose(Vector3f.YP.rotationDegrees(n.rotation[1]+(p==null?0:p.rotation[1])));
                s.mulPose(Vector3f.XP.rotationDegrees(n.rotation[0]+(p==null?0:p.rotation[0])));
                s.translate(-n.origin[0],-n.origin[1],-n.origin[2]);
                draw(n.children,s,buffer,light,overlay,r,g,b,alpha);s.popPose();
            }else if(n.quads!=null)for(CerberusRig.Quad q:n.quads){
                normal.set(q.normal[0],q.normal[1],q.normal[2]);normal.transform(s.last().normal());
                float[] first=q.vertices[3];vertex.set(first[0],first[1],first[2],1);vertex.transform(s.last().pose());
                // Both vectors are in camera space. Skip the entire back-facing quad
                // before transforming or submitting its other three vertices.
                if(filterBackFaces&&normal.x()*vertex.x()+normal.y()*vertex.y()+normal.z()*vertex.z()>0.001F)continue;
                for(int i=3;i>=0;i--){float[]v=q.vertices[i];if(i!=3){vertex.set(v[0],v[1],v[2],1);vertex.transform(s.last().pose());}
                    buffer.vertex(vertex.x(),vertex.y(),vertex.z()).color(r,g,b,alpha).uv(v[3],v[4]).overlayCoords(overlay).uv2(light)
                            .normal(normal.x(),normal.y(),normal.z()).endVertex();
                }
            }
        }
    }
    @Override public boolean renderItemInHand(T entity,HandSide side,MatrixStack stack) { return false; }
    @Override public void renderFirstPersonArm(MatrixStack s,IVertexBuilder b,int l,int o,float r,float g,float blue,float a,HandSide side) {}
    @Override public void renderFirstPersonLeg(MatrixStack s,IVertexBuilder b,int l,int o,float r,float g,float blue,float a,HandSide side) {}
}
