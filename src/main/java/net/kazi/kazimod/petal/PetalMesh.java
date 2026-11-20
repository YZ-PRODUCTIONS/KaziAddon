package net.kazi.kazimod.petal;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.math.vector.*;
public final class PetalMesh extends RenderState {
 private PetalMesh(){super(null,null,null);}
 public static final RenderType TYPE=RenderType.create("senbonzakura_petals",DefaultVertexFormats.POSITION_COLOR,7,262144,false,true,RenderType.State.builder().setTextureState(NO_TEXTURE).setAlphaState(NO_ALPHA).setTransparencyState(TRANSLUCENT_TRANSPARENCY).setShadeModelState(SMOOTH_SHADE).setCullState(NO_CULL).setLightmapState(NO_LIGHTMAP).setDiffuseLightingState(NO_DIFFUSE_LIGHTING).setWriteMaskState(COLOR_WRITE).setDepthTestState(LEQUAL_DEPTH_TEST).createCompositeState(false));
 private static void vertex(IVertexBuilder b,Matrix4f m,double x,double y,double z,float r,float g,float blue,float alpha){b.vertex(m,(float)x,(float)y,(float)z).color(r,g,blue,alpha).endVertex();}
 private static void ribbon(IVertexBuilder b,Matrix4f m,Vector3d a,Vector3d c,Vector3d width,float alpha,boolean white){
  ribbon(b,m,a,c,width,width,alpha,white);
 }
 private static void ribbon(IVertexBuilder b,Matrix4f m,Vector3d a,Vector3d c,Vector3d width,Vector3d endWidth,float alpha,boolean white){
  for(int edge:new int[]{-1,1}){Vector3d outer=width.scale(edge),outerEnd=endWidth.scale(edge);vertex(b,m,a.x,a.y,a.z,1,white?1:.72F,white?1:.87F,alpha);vertex(b,m,c.x,c.y,c.z,1,white?1:.72F,white?1:.87F,alpha);vertex(b,m,c.x+outerEnd.x,c.y+outerEnd.y,c.z+outerEnd.z,white?.85F:1,white?.94F:.4F,white?1:.7F,0);vertex(b,m,a.x+outer.x,a.y+outer.y,a.z+outer.z,white?.85F:1,white?.94F:.4F,white?1:.7F,0);}
 }
 private static Vector3d streamPoint(Vector3d end,Vector3d u,Vector3d w,double t,double phase){double wave=Math.sin(t*Math.PI);return end.scale(t).add(u.scale(Math.sin(t*13+phase)*wave*.32)).add(w.scale(Math.cos(t*10+phase)*wave*.32));}
 private static void sphereBands(IVertexBuilder v,Matrix4f m,double r,double clock,float alpha,int bands){for(int j=0;j<bands;j++){double lat=-1.25+j*2.5/(bands-1),rad=Math.cos(lat)*r;for(int i=0;i<72;i++){double a=i*Math.PI/36+clock+j*.7,c=a+Math.PI/36;double y=1.2+Math.sin(lat)*r*.75;
   Vector3d p=new Vector3d(Math.cos(a)*rad,y+Math.sin(a*3+clock)*.18,Math.sin(a)*rad),q=new Vector3d(Math.cos(c)*rad,y+Math.sin(c*3+clock)*.18,Math.sin(c)*rad);float pulse=(float)(.35+.65*Math.pow(.5+.5*Math.sin(a*2-clock+j),2));ribbon(v,m,p,q,new Vector3d(0,.20+r*.025,0),alpha*pulse,false);
  }}}
 public static void petal(IVertexBuilder b,Matrix4f m,double x,double y,double z,double size,double angle,float alpha,boolean white){
  double ux=Math.cos(angle)*size,uy=Math.sin(angle)*size,uz=Math.sin(angle*.7)*size*.35,vx=-Math.sin(angle)*size*.45,vy=Math.cos(angle)*size*.45,vz=Math.cos(angle*.7)*size*.5;
  float r=white?.9F:1,g=white?.96F:.58F,blue=white?1:.79F;
  vertex(b,m,x-ux,y-uy,z-uz,r,g,blue,alpha);vertex(b,m,x+vx,y+vy,z+vz,1,.88F,.96F,alpha);vertex(b,m,x+ux*1.4,y+uy*1.4,z+uz*1.4,1,1,1,alpha);vertex(b,m,x-vx,y-vy,z-vz,r,g*.8F,blue,alpha);
 }
 public static void ring(MatrixStack s,IRenderTypeBuffer b,double radius,float alpha,float clock){IVertexBuilder v=b.getBuffer(TYPE);for(int i=0;i<72;i++){double a=i*Math.PI/36;petal(v,s.last().pose(),Math.cos(a)*radius,.05,Math.sin(a)*radius,.08,a+clock*.02,alpha,false);}}
 public static void orbit(MatrixStack s,IRenderTypeBuffer b,float time,float strength,boolean bankai,float alpha){IVertexBuilder v=b.getBuffer(TYPE);int count=bankai?130:65;for(int i=0;i<count;i++){double t=i/(double)count,a=t*Math.PI*8+time*.035,r=1.05+Math.sin(t*19+time*.018)*.24;petal(v,s.last().pose(),Math.cos(a)*r,.3+t*1.8,Math.sin(a)*r,.035+strength*.025,a+time*.07,alpha*(.4F+strength*.5F),false);}}
 public static void field(int kind,float age,float life,float size,Vector3d end,float yaw,MatrixStack s,IRenderTypeBuffer b,float alpha,int quality){
  float fade=Math.min(1,Math.min(age/5,(life-age)/8));if(fade<=0)return;alpha*=fade;double time=age*.09;IVertexBuilder v=b.getBuffer(TYPE);Matrix4f m=s.last().pose();int count=Math.max(64,quality);
  if(kind==0){for(int i=0;i<count/2;i++){double t=i/(double)(count/2),a=i*2.399+time,r=Math.max(0,age-4)*.035;petal(v,m,.35+Math.cos(a)*r,1.1+t*1.5,Math.sin(a)*r-.5,.045,a,alpha,false);}}
  else if(kind==1){Vector3d dir=end.normalize(),u=dir.cross(new Vector3d(0,1,0)).normalize();if(u.lengthSqr()<.1)u=new Vector3d(1,0,0);Vector3d w=dir.cross(u).normalize();
   for(int strand=0;strand<4;strand++)for(int j=0;j<64;j++){double t=j/64D,phase=time+strand*Math.PI*.5;Vector3d a=streamPoint(end,u,w,t,phase),c=streamPoint(end,u,w,(j+1)/64D,phase);double width=(.06+Math.sin(t*Math.PI)*.3)*size;ribbon(v,m,a,c,(strand%2==0?u:w).scale(width),alpha*.65F,false);}
   for(int i=0;i<count;i++){double t=((i/(double)count+age*.035)%1),a=i*2.399+time,spread=(.12+Math.sin(t*Math.PI)*.42)*size,rad=spread*(.3+(i%7)/10D);double x=end.x*t+u.x*Math.cos(a)*rad+w.x*Math.sin(a)*rad,y=end.y*t+u.y*Math.cos(a)*rad+w.y*Math.sin(a)*rad,z=end.z*t+u.z*Math.cos(a)*rad+w.z*Math.sin(a)*rad;petal(v,m,x,y,z,.05+(i%3)*.018,a,alpha,false);}
  }else if(kind==2){for(int band=0;band<4;band++)for(int i=0;i<64;i++){double a=i*Math.PI/32+time,r=.25+band*.27,c=a+Math.PI/32;ribbon(v,m,new Vector3d(Math.cos(a)*r,1.25+Math.sin(a)*r,-.9),new Vector3d(Math.cos(c)*r,1.25+Math.sin(c)*r,-.9),new Vector3d(Math.cos(a)*.16,Math.sin(a)*.16,0),new Vector3d(Math.cos(c)*.16,Math.sin(c)*.16,0),alpha*.8F,false);}for(int i=0;i<count/2;i++){double t=i/(double)(count/2),r=Math.sqrt(t)*1.15,a=i*2.399+time*.8;petal(v,m,Math.cos(a)*r,1.25+Math.sin(a)*r,-.9+.18*t,.08,a+time,alpha,false);}}
  else if(kind==3){for(int i=0;i<70;i++){double a=i*2.399,r=.2+age*.045;petal(v,m,Math.cos(a)*r,.5+(i%13)*.08-age*.015,Math.sin(a)*r,.055,a+time,alpha,false);}}
  else if(kind==4||kind==9){double close=Math.min(1,age/(kind==4?6:20)),r=kind==9?Math.max(1.5,size*(1-Math.max(0,age-20)/110)):size;
   sphereBands(v,m,r+(1-close)*2,time,alpha*(float)close*(kind==9?.85F:.5F),kind==9?11:7);
   for(int i=0;i<count;i++){double t=(i+.5)/count,y=1-2*t,a=i*2.399+time*(kind==9?1.8:1),belt=Math.sqrt(1-y*y);double radius=r+(1-close)*2;petal(v,m,Math.cos(a)*belt*radius,1.2+y*radius*.75,Math.sin(a)*belt*radius,.07+(kind==9?.025:0),a+time,alpha*(float)(.5+close*.5),false);}ring(s,b,r,alpha*.7F,age);
  }else if(kind==5){
   ring(s,b,size,alpha,age);
   for(int i=0;i<96;i++){
    double a=i*Math.PI/48,next=(i+1)*Math.PI/48;
    ribbon(v,m,new Vector3d(Math.cos(a)*size,.06,Math.sin(a)*size),new Vector3d(Math.cos(next)*size,.06,Math.sin(next)*size),
      new Vector3d(Math.cos(a)*.16,0,Math.sin(a)*.16),new Vector3d(Math.cos(next)*.16,0,Math.sin(next)*.16),alpha*.8F,false);
   }
   double descend=Math.max(0,Math.min(1,(age-20)/8D));int petals=Math.min(960,count*3);
   for(int i=0;i<petals;i++){
    double t=(i+.5)/petals,a=i*2.399+time*1.8,r=Math.sqrt(t)*size,y=age<28?(5+(i%23)/23D*5)*(1-descend):.1+(age-28)*.025;
    petal(v,m,Math.cos(a)*r,y,Math.sin(a)*r,.12,a+time,alpha*(age<20?.5F:1),false);
   }
   if(age>=20&&age<=30)for(int j=0;j<36;j++){
    double a=j*2.399+time,r=Math.sqrt((j+.5)/36)*size;Vector3d at=new Vector3d(Math.cos(a)*r,0,Math.sin(a)*r);
    ribbon(v,m,at.add(0,Math.max(.1,8*(1-descend)),0),at,new Vector3d(.3,0,.3),alpha*.6F,false);
   }
  }
  else if(kind==10){double phase=Math.min(1,age%PetalField.SENKEI_SWORD_INTERVAL/12);for(int i=0;i<40;i++){double t=i/40D;petal(v,m,end.x+Math.cos(i*2.4)*.3,end.y+8*(1-phase)+t,end.z+Math.sin(i*2.4)*.3,.07,time+i,alpha,false);} }
  
 }
}
