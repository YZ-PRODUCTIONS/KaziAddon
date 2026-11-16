package net.kazi.kazimod.ope;

import java.util.*;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.model.*;
import net.minecraft.client.renderer.texture.*;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Direction;

/** Split textured item faces at the guard, preserving the real sword's hilt and blade texture. */
public final class WilleSwordGeometry {
    private static final float GUARD = .38F;
    private static class Vertex {
        float x,y,z,u,v;
        Vertex(float x,float y,float z,float u,float v) { this.x=x;this.y=y;this.z=z;this.u=u;this.v=v; }
        float axis() { return (x+y)*.5F; }
        Vertex lerp(Vertex other,float t) { return new Vertex(x+(other.x-x)*t,y+(other.y-y)*t,z+(other.z-z)*t,u+(other.u-u)*t,v+(other.v-v)*t); }
    }
    public static void render(LivingEntity user, MatrixStack stack, IRenderTypeBuffer buffer, float length) {
        ItemStack item = user.getMainHandItem(); if (item.isEmpty()) return;
        IBakedModel model = Minecraft.getInstance().getItemRenderer().getModel(item, user.level, user);
        List<BakedQuad> quads = new ArrayList<>();
        Random random = new Random(42);
        for (Direction direction : Direction.values()) { random.setSeed(42); quads.addAll(model.getQuads(null, direction, random)); }
        random.setSeed(42); quads.addAll(model.getQuads(null, null, random));
        IVertexBuilder out = buffer.getBuffer(RenderType.entityTranslucent(AtlasTexture.LOCATION_BLOCKS));
        for (BakedQuad quad : quads) {
            int[] data = quad.getVertices(); int stride = data.length/4;
            List<Vertex> face = new ArrayList<>();
            for(int i=0;i<4;i++) { int p=i*stride; face.add(new Vertex(Float.intBitsToFloat(data[p]),Float.intBitsToFloat(data[p+1]),Float.intBitsToFloat(data[p+2]),Float.intBitsToFloat(data[p+4]),Float.intBitsToFloat(data[p+5]))); }
            emit(clip(face,false),out,stack,length);
            emit(clip(face,true),out,stack,length);
        }
    }
    private static List<Vertex> clip(List<Vertex> input, boolean blade) {
        List<Vertex> output = new ArrayList<>();
        for(int i=0;i<input.size();i++) {
            Vertex a=input.get(i),b=input.get((i+1)%input.size());
            boolean ia=blade?a.axis()>=GUARD:a.axis()<=GUARD, ib=blade?b.axis()>=GUARD:b.axis()<=GUARD;
            if(ia) output.add(a);
            if(ia!=ib) output.add(a.lerp(b,(GUARD-a.axis())/(b.axis()-a.axis())));
        }
        return output;
    }
    private static void emit(List<Vertex> polygon, IVertexBuilder out, MatrixStack stack, float length) {
        for(int i=1;i+1<polygon.size();i++) {
            vertex(polygon.get(0),out,stack,length);vertex(polygon.get(i),out,stack,length);
            vertex(polygon.get(i+1),out,stack,length);vertex(polygon.get(i+1),out,stack,length);
        }
    }
    private static void vertex(Vertex v,IVertexBuilder out,MatrixStack stack,float length) {
        float s=v.axis(), forward=s<=GUARD?(s-GUARD)*1.5F:(s-GUARD)/(1-GUARD)*length;
        out.vertex(stack.last().pose(),(v.x-v.y)*.65F,(v.z-.5F)*.65F,forward)
                .color(.8F,.96F,1F,1F).uv(v.u,v.v).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(15728880)
                .normal(stack.last().normal(),0,1,0).endVertex();
    }
    private WilleSwordGeometry() {}
}
