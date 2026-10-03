package net.kazi.kazimod.worldturtle;

/** Five low-poly stellar cores and additive corona/beam shells. */
public final class SupernovaMesh {
    public static void star(WorldTurtleMesh.Sink out,float age,int index){
        float charge=Math.min(1,age/40),fade=Math.min(1,Math.max(0,(120-age)/8));
        float r=(1+charge*3.8F)*(1+.025F*(float)Math.sin(age*.22+index));
        WorldTurtleMesh.sphere(out,0,0,0,r,r,r,16,8,0xffad24,.28F*fade);
        WorldTurtleMesh.sphere(out,0,0,0,r*.83F,r*.83F,r*.83F,12,6,0xffedb3,.8F*fade);
        WorldTurtleMesh.sphere(out,0,0,0,r*.6F,r*.6F,r*.6F,12,6,0xffffff,fade);
        WorldTurtleMesh.ring(out,0,0,0,r*1.25F,.2F,0xffbf40,.75F*fade,age*.016F+index);
        WorldTurtleMesh.ring(out,0,0,0,r*1.45F,.08F,0xffec9d,.55F*fade,-age*.012F-index);
    }
    public static void beam(WorldTurtleMesh.Sink out,float length,float age){
        if(age<40||length<.01F)return;
        float fade=Math.min(1,(age-40)/3)*Math.min(1,Math.max(0,(120-age)/8));
        for(int layer=0;layer<3;layer++){
            float radius=layer==0?3.2F:layer==1?2.3F:1.1F;float alpha=(layer==0?.08F:layer==1?.32F:.95F)*fade;
            int color=layer==2?0xfffbed:0xffc454;
            for(int side=0;side<16;side++){
                double a=side*Math.PI/8,b=(side+1)*Math.PI/8;
                out.vertex((float)Math.cos(a)*radius,(float)Math.sin(a)*radius,0,color,alpha);
                out.vertex((float)Math.cos(b)*radius,(float)Math.sin(b)*radius,0,color,alpha);
                out.vertex((float)Math.cos(b)*radius*.55F,(float)Math.sin(b)*radius*.55F,length,color,alpha);
                out.vertex((float)Math.cos(a)*radius*.55F,(float)Math.sin(a)*radius*.55F,length,color,alpha);
            }
        }
        for(int i=0;i<3;i++){
            float z=((age*.035F+i/3F)%1)*length,r=3.5F-i*.4F;
            WorldTurtleMesh.ring((x,y,Z,c,a)->out.vertex(x,Z,z+y,c,a),0,0,0,r,.13F,0xfff1c9,.6F*fade,0);
        }
    }
}
