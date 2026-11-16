package net.kazi.kazimod.ope;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.minecraft.client.renderer.IRenderTypeBuffer;

public final class WilleMesh {
    public static void render(MatrixStack stack, IRenderTypeBuffer buffer, float length, float age, float progress, float after, boolean puncture, boolean first) {
        IVertexBuilder out=buffer.getBuffer(OpeMesh.GLOW);
        float opacity=first?.45F:1;
        ribbon(out,stack,-.025F,0,0,.025F,0,length,.025F,0xB8F9FF,.75F*opacity);
        ribbon(out,stack,0,-.015F,0,0,.015F,length,.1F,0x4BCFFF,.2F*opacity);
        int rings=puncture?9:5;
        for(int i=0;i<rings;i++) {
            float t=(age*.018F+i/(float)rings)%1;
            ring(out,stack,.22F+(float)Math.sin(t*Math.PI)*.12F,length*t,.025F,0x77DCFF,.45F*opacity);
        }
        if(after<0) {
            ring(out,stack,.32F+progress*.2F,.45F,.035F,0xD6FCFF,.6F*opacity);
            return;
        }
        float fade=Math.max(0,1-after/14)*opacity;
        int sections=puncture?12:7;
        for(int i=0;i<sections;i++) {
            float z=length*(i+.5F)/sections;
            float radius=.35F+after*(puncture?.5F:.24F);
            ring(out,stack,radius,z,.1F,0x66DEFF,fade*.6F);
            ring(out,stack,radius*.93F,z+.05F,.026F,0xF0FFFF,fade);
            for(int arm=0;arm<3;arm++) {
                float angle=(float)(arm*Math.PI*2/3+i*1.7);
                float px=0,py=0,pz=z;
                for(int s=1;s<=5;s++) {
                    float r=s*radius/5, a=angle+(float)Math.sin(s*7+i+Math.floor(age/2))*.28F;
                    float x=(float)Math.cos(a)*r,y=(float)Math.sin(a)*r,nz=z+(float)Math.sin(s*2+i)*.6F;
                    ribbon(out,stack,px,py,pz,x,y,nz,.04F,0xBAF5FF,fade);
                    px=x;py=y;pz=nz;
                }
            }
        }
    }
    private static void ring(IVertexBuilder out,MatrixStack stack,float radius,float z,float width,int color,float alpha) {
        for(int i=0;i<48;i++) for(int c=0;c<4;c++) {
            float a=(float)(Math.PI*2*(i+(c==1||c==2?1:0))/48), r=radius+(c>=2?-width:width);
            v(out,stack,(float)Math.cos(a)*r,(float)Math.sin(a)*r,z,color,alpha);
        }
    }
    private static void ribbon(IVertexBuilder out,MatrixStack stack,float x,float y,float z,float tx,float ty,float tz,float width,int color,float alpha) {
        v(out,stack,x-width,y,z,color,alpha);v(out,stack,tx-width,ty,tz,color,alpha);
        v(out,stack,tx+width,ty,tz,color,alpha);v(out,stack,x+width,y,z,color,alpha);
        v(out,stack,x,y-width,z,color,alpha);v(out,stack,tx,ty-width,tz,color,alpha);
        v(out,stack,tx,ty+width,tz,color,alpha);v(out,stack,x,y+width,z,color,alpha);
    }
    private static void v(IVertexBuilder out,MatrixStack stack,float x,float y,float z,int color,float alpha) {
        out.vertex(stack.last().pose(),x,y,z).color((color>>16&255)/255F,(color>>8&255)/255F,(color&255)/255F,alpha).endVertex();
    }
    private WilleMesh() {}
}
