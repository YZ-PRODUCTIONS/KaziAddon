import net.kazi.kazimod.abilities.cinematics.AwakeningCutsceneType;
import net.kazi.kazimod.abilities.NagiAwakening.InnerDragonsSequence;
import net.kazi.kazimod.abilities.NagiAwakening.InnerDragonsCutsceneCooldown;
import net.kazi.kazimod.client.asriel.HyperGonerMesh;
import net.kazi.kazimod.client.asriel.HyperGonerGravity;
import net.kazi.kazimod.client.asriel.HyperGonerSkull;
import net.kazi.kazimod.client.asriel.HyperGonerMouth;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.io.File;
import javax.imageio.ImageIO;

/** Executes production latch, shared deadlines, replay timer and native mesh outside Minecraft. */
public final class HyperGonerTest {
    private static int checks;
    private static void check(boolean value,String message) { checks++;if(!value)throw new AssertionError(message); }
    public static void main(String[] args) throws Exception {
        InnerDragonsSequence goat=new InnerDragonsSequence(.5,true,260);
        check(!goat.update(50.01,100,0,true),"above threshold");
        check(goat.update(50,100,1,true),"exactly half must trigger");
        check(!goat.update(40,100,2,true),"damage cannot restart the scene");
        check(goat.isPlaying(160)&&goat.isPlaying(260)&&!goat.isPlaying(261),"Goat must not end at Nagi's deadline");
        check(!goat.update(30,100,400,true),"remaining low cannot replay");
        goat.update(70,100,401,true);check(goat.update(49,100,402,true),"new health crossing");
        goat.update(40,100,403,false);check(!goat.isPlaying(403),"suppression stops playback");
        check(!goat.update(40,100,404,true),"suppression cannot reset latch");
        goat.update(0,100,405,true);check(!goat.isPlaying(405),"death stops playback");
        for(double invalid:new double[]{Double.NaN,Double.POSITIVE_INFINITY,0,-1})
            check(!goat.update(invalid,100,500,true),"invalid/dead health");
        long next=InnerDragonsCutsceneCooldown.nextAllowedTick(100);
        check(next==12100&&!InnerDragonsCutsceneCooldown.ready(12099,next)&&InnerDragonsCutsceneCooldown.ready(12100,next),"ten-minute persistent deadline");
        InnerDragonsSequence nagi=new InnerDragonsSequence(),goru=new InnerDragonsSequence(.5);
        check(!nagi.update(30,100,0,true)&&nagi.update(29,100,1,true),"Nagi threshold regression");
        check(!goru.update(50,100,0,true)&&goru.update(49,100,1,true),"Goru threshold regression");
        check(!nagi.isPlaying(161)&&!goru.isPlaying(161),"existing durations regression");
        for(AwakeningCutsceneType type:AwakeningCutsceneType.values()) {
            check(AwakeningCutsceneType.fromWire(type.wireId)==type,"wire type identity");
            check(type.active(0)&&type.active(type.durationTicks-.01F)&&!type.active(type.durationTicks),"client/server exact deadline");
            check(!type.active(Float.NaN)&&!type.active(-1),"invalid packet age");
        }
        try {AwakeningCutsceneType.fromWire(3);throw new AssertionError("unknown wire type accepted");}catch(IllegalArgumentException expected){checks++;}
        check(HyperGonerMesh.suction(30)==0&&HyperGonerMesh.suction(120)>.9F,"vacuum begins after introduction");
        check(HyperGonerMesh.flash(200)==0&&HyperGonerMesh.flash(240)==1&&HyperGonerMesh.flash(259)==0,"flash timing and cleanup");
        int[] peaks=new int[3],budgets={65000,65000,65000};
        for(int lod=0;lod<3;lod++)for(float age=0;age<260;age+=.5F) {
            final int[] count={0};
            HyperGonerMesh.Sink sink=(x,y,z,rgb,alpha)->{
                count[0]++;check(Float.isFinite(x)&&Float.isFinite(y)&&Float.isFinite(z)&&Float.isFinite(alpha),"non-finite vertex");
                check(alpha>=0&&alpha<=1,"alpha range");check(Math.abs(x)<=15&&Math.abs(y)<=16&&Math.abs(z)<=8,"unexpected bounds");
            };
            HyperGonerMesh.Sink body=(x,y,z,rgb,alpha)->{
                sink.vertex(x,y,z,rgb,alpha);
                // Mouth and eye cavities remain dark; a black world/background plane must not return.
                if((rgb>>16&255)<64&&(rgb>>8&255)<64&&(rgb&255)<96)
                    check(Math.abs(x)<4.1F&&y>=-3.61F&&y<=4.51F&&z>=-5.34F&&z<=1.71F,"dark geometry escaped model bounds");
            };
            HyperGonerMesh.render(body,sink,age,lod);
            check(count[0]%4==0,"broken quad stream");check(count[0]<=budgets[lod],"VFX budget at detail "+lod+": "+count[0]);peaks[lod]=Math.max(peaks[lod],count[0]);
        }
        check(peaks[0]>peaks[1]&&peaks[1]>peaks[2],"lower details must reduce the whole-scene vertex budget");
        skull();
        gravity();
        HyperGonerMesh.Sink inactive=(x,y,z,c,a)->{throw new AssertionError("expired mesh");};
        for(float age:new float[]{-1,260,Float.NaN,Float.POSITIVE_INFINITY})HyperGonerMesh.render(inactive,inactive,age,0);
        preview(args[0]);
        System.out.println("Hyper Goner: "+checks+" checks passed; peak vertices high/medium/low "+Arrays.toString(peaks));
    }
    private static void skull() throws Exception {
        int originalVertices;
        float[][] sourcePositions;
        int[][] sourceColors;
        int[] sourceIndices;
        String resource="/assets/kazimod/models/cinematics/hyper_goner.hgm.gz";
        try(java.io.InputStream packed=HyperGonerSkull.class.getResourceAsStream(resource)) {
            check(packed!=null,"the authored model must be packaged on the runtime classpath");
            try(java.io.DataInputStream in=new java.io.DataInputStream(new java.util.zip.GZIPInputStream(packed))) {
                check(in.readInt()==0x48474D31,"packaged model format");
                int indexedVertices=in.readInt(),triangleIndices=in.readInt();
                check(indexedVertices>=5000&&indexedVertices<=50000,"authored mesh must retain its detailed geometry");
                check(triangleIndices==35868,"all 11956 original model triangles must be retained");
                originalVertices=triangleIndices/3*4;
                sourcePositions=new float[2][indexedVertices*3];sourceColors=new int[2][indexedVertices];
                for(int vertex=0;vertex<indexedVertices;vertex++)for(int pose=0;pose<2;pose++) {
                    for(int axis=0;axis<3;axis++)sourcePositions[pose][vertex*3+axis]=in.readFloat();
                    sourceColors[pose][vertex]=in.readInt();
                }
                sourceIndices=new int[triangleIndices];
                for(int i=0;i<triangleIndices;i++) {
                    sourceIndices[i]=in.readInt();
                    check(sourceIndices[i]>=0&&sourceIndices[i]<indexedVertices,"valid authored triangle index");
                }
                check(in.read()==-1,"complete model poses, material colors, indices and gzip checksum");
            }
        }
        int expectedVertices=originalVertices+280;
        int[] counts=new int[3];
        long firstGeometry=0,firstAppearance=0;
        for(int lod=0;lod<3;lod++) {
            SkullSignature closed=new SkullSignature(expectedVertices),open=new SkullSignature(expectedVertices),repeat=new SkullSignature(expectedVertices);
            HyperGonerSkull.render(closed,0,lod);
            HyperGonerSkull.render(open,1,lod);
            HyperGonerSkull.render(repeat,1,lod);
            counts[lod]=open.count;
            check(open.count==expectedVertices&&closed.count==expectedVertices,"render the complete authored model followed by the mouth lining");
            check(open.count<=52000&&open.count%4==0,"bounded complete skull stream");
            check(open.geometryHash!=closed.geometryHash,"the laughing shape must actually move model vertices");
            check(open.hash==repeat.hash&&open.count==repeat.count,"cached model must render deterministically");
            if(lod==0){firstGeometry=open.geometryHash;firstAppearance=open.hash;}
            else check(open.geometryHash==firstGeometry&&open.hash==firstAppearance,"all detail settings preserve the supplied model topology and materials");
            int moved=0;
            for(int vertex=0;vertex<originalVertices;vertex++) {
                int offset=vertex*3,source=sourceIndices[vertex/4*3+Math.min(vertex%4,2)];
                for(int axis=0;axis<3;axis++) {
                    check(Math.abs(closed.xyz[offset+axis]-sourcePositions[0][source*3+axis])<.000002F,
                            "closed model prefix must exactly retain packaged author geometry");
                    check(Math.abs(open.xyz[offset+axis]-sourcePositions[1][source*3+axis])<.000002F,
                            "laughing model prefix must exactly retain packaged author geometry");
                }
                check(closed.colors[vertex]==sourceColors[0][source]&&open.colors[vertex]==sourceColors[1][source],
                        "mouth lining must leave all original model material colors unchanged");
                if(Math.abs(open.xyz[offset]-closed.xyz[offset])+Math.abs(open.xyz[offset+1]-closed.xyz[offset+1])
                        +Math.abs(open.xyz[offset+2]-closed.xyz[offset+2])>.001F)moved++;
                if(vertex%4==3)for(int axis=0;axis<3;axis++)
                    check(open.xyz[offset+axis]==open.xyz[offset-3+axis]&&closed.xyz[offset+axis]==closed.xyz[offset-3+axis],
                            "quad submission must retain original triangles without a fourth corner or a fill across holes");
            }
            check(moved>100,"authored shape keys must produce substantial facial animation");
            for(float pose:new float[]{.25F,.5F,.75F}) {
                SkullSignature between=new SkullSignature(expectedVertices);
                HyperGonerSkull.render(between,pose,lod);
                check(between.count==expectedVertices,"morphing must preserve all model and interior vertices and their order");
                for(int coordinate=0;coordinate<expectedVertices*3;coordinate++) {
                    float expected=closed.xyz[coordinate]+(open.xyz[coordinate]-closed.xyz[coordinate])*pose;
                    check(Math.abs(between.xyz[coordinate]-expected)<.000002F,"model and mouth must interpolate together between authored endpoints");
                }
            }
            SkullSignature below=new SkullSignature(expectedVertices),above=new SkullSignature(expectedVertices),revisited=new SkullSignature(expectedVertices);
            HyperGonerSkull.render(below,-1,lod);HyperGonerSkull.render(above,2,lod);HyperGonerSkull.render(revisited,0,lod);
            check(below.hash==closed.hash&&above.hash==open.hash,"opening must clamp at both model pose limits");
            check(revisited.hash==closed.hash,"revisiting a pose must restore the same model after cached animation");
        }
        HyperGonerMesh.Sink invalid=(x,y,z,c,a)->{throw new AssertionError("invalid skull pose emitted vertices");};
        for(float open:new float[]{Float.NaN,Float.POSITIVE_INFINITY,Float.NEGATIVE_INFINITY})HyperGonerSkull.render(invalid,open,0);
        mouth(originalVertices);
        System.out.println("Authored model + mouth vertices "+Arrays.toString(counts)+"; original prefix, materials and linear shape-key animation verified");
    }
    private static void mouth(int originalVertices) {
        for(float pose:new float[]{0,.25F,.5F,.75F,1}) {
            SkullSignature cavity=new SkullSignature(280),complete=new SkullSignature(originalVertices+280);
            HyperGonerMouth.render(cavity,pose);HyperGonerSkull.render(complete,pose,0);
            check(cavity.count==280,"mouth must remain a complete bounded volume");
            float minZ=Float.POSITIVE_INFINITY,maxZ=Float.NEGATIVE_INFINITY;
            for(int vertex=0;vertex<cavity.count;vertex++) {
                int p=vertex*3,q=(originalVertices+vertex)*3;
                check(cavity.xyz[p]==complete.xyz[q]&&cavity.xyz[p+1]==complete.xyz[q+1]&&cavity.xyz[p+2]==complete.xyz[q+2]
                        &&cavity.colors[vertex]==complete.colors[originalVertices+vertex],"append the actual opaque mouth volume after the untouched model");
                int color=cavity.colors[vertex];
                check((color>>16&255)<=25&&(color>>8&255)<=26&&(color&255)<=30,"mouth lining must remain deep black and charcoal");
                minZ=Math.min(minZ,cavity.xyz[p+2]);maxZ=Math.max(maxZ,cavity.xyz[p+2]);
            }
            check(maxZ-minZ>=2,"the dark mouth must have real recessed walls rather than a flat black decal");
            for(float x:new float[]{-.4F,-.2F,0,.2F,.4F})for(float y:new float[]{-2.6F,-2.4F,-2.2F}) {
                RayHit hit=frontRay(cavity,x,y);
                check(hit!=null&&hit.z<-.5F,"front rays through the opening must end inside the deep closed mouth at pose "+pose+" ("+x+","+y+")");
            }
            for(float x:new float[]{-.15F,.15F}) {
                RayHit visible=frontRay(complete,x,-2.6F);
                check(visible!=null&&visible.dark,"the recessed black interior must be visible through the actual model opening at pose "+pose);
            }
            for(float[] outside:new float[][]{{0,4.4F},{-4,-2.4F},{4,-2.4F},{0,-3.6F}})
                check(frontRay(cavity,outside[0],outside[1])==null,"mouth geometry must not become a world or face background");
        }
        HyperGonerMesh.Sink invalid=(x,y,z,c,a)->{throw new AssertionError("invalid mouth pose emitted vertices");};
        for(float pose:new float[]{Float.NaN,Float.POSITIVE_INFINITY,Float.NEGATIVE_INFINITY})HyperGonerMouth.render(invalid,pose);
        System.out.println("Opaque recessed mouth: front-ray coverage, visible black interior, depth and bounded silhouette verified across five poses");
    }
    /** Orthographic front ray using the same two triangles submitted for each quad. */
    private static RayHit frontRay(SkullSignature mesh,float x,float y) {
        RayHit nearest=null;
        for(int first=0;first<mesh.count;first+=4)for(int triangle=0;triangle<2;triangle++) {
            int a=first,b=first+(triangle==0?1:2),c=first+(triangle==0?2:3);
            float ax=mesh.xyz[a*3],ay=mesh.xyz[a*3+1],bx=mesh.xyz[b*3],by=mesh.xyz[b*3+1],cx=mesh.xyz[c*3],cy=mesh.xyz[c*3+1];
            float divisor=(by-cy)*(ax-cx)+(cx-bx)*(ay-cy);
            if(Math.abs(divisor)<.0000001F)continue;
            float u=((by-cy)*(x-cx)+(cx-bx)*(y-cy))/divisor;
            float v=((cy-ay)*(x-cx)+(ax-cx)*(y-cy))/divisor,w=1-u-v;
            if(u<-.00001F||v<-.00001F||w<-.00001F)continue;
            float z=u*mesh.xyz[a*3+2]+v*mesh.xyz[b*3+2]+w*mesh.xyz[c*3+2];
            if(nearest==null||z>nearest.z) {
                float r=u*(mesh.colors[a]>>16&255)+v*(mesh.colors[b]>>16&255)+w*(mesh.colors[c]>>16&255);
                float g=u*(mesh.colors[a]>>8&255)+v*(mesh.colors[b]>>8&255)+w*(mesh.colors[c]>>8&255);
                float blue=u*(mesh.colors[a]&255)+v*(mesh.colors[b]&255)+w*(mesh.colors[c]&255);
                nearest=new RayHit(z,r<=30&&g<=30&&blue<=35);
            }
        }
        return nearest;
    }
    private static final class RayHit {
        final float z;final boolean dark;
        RayHit(float z,boolean dark){this.z=z;this.dark=dark;}
    }
    private static final class SkullSignature implements HyperGonerMesh.Sink {
        int count;
        long hash=0xcbf29ce484222325L,geometryHash=0xcbf29ce484222325L;
        final float[] xyz;final int[] colors;
        SkullSignature(int expectedVertices){xyz=new float[expectedVertices*3];colors=new int[expectedVertices];}
        public void vertex(float x,float y,float z,int rgb,float alpha) {
            check(Float.isFinite(x)&&Float.isFinite(y)&&Float.isFinite(z)&&Float.isFinite(alpha),"finite model vertex");
            check(alpha==1,"model and recessed mouth must be opaque before the cinematic fade");
            check(Math.abs(x)<4.1F&&y>=-3.61F&&y<=4.51F&&z>=-5.34F&&z<=1.71F,"model and interior must fit the supplied mesh bounds");
            check(count*3+2<xyz.length,"model emitted more vertices than the authored mesh and bounded interior");
            colors[count]=rgb;int offset=count++*3;xyz[offset]=x;xyz[offset+1]=y;xyz[offset+2]=z;
            geometryHash=mix(geometryHash,Float.floatToIntBits(x));geometryHash=mix(geometryHash,Float.floatToIntBits(y));
            geometryHash=mix(geometryHash,Float.floatToIntBits(z));
            hash=mix(hash,Float.floatToIntBits(x));hash=mix(hash,Float.floatToIntBits(y));
            hash=mix(hash,Float.floatToIntBits(z));hash=mix(hash,rgb);hash=mix(hash,Float.floatToIntBits(alpha));
        }
        private static long mix(long hash,int value){return (hash^value)*0x100000001b3L;}
    }
    private static void gravity() {
        int[] rockPeaks=new int[3],vertexPeaks=new int[3],budgets={11000,6000,3200};
        for(int lod=0;lod<3;lod++)for(float age=96;age<=215;age+=.5F) {
            Signature body=new Signature(),glow=new Signature();
            HyperGonerGravity.render(body,glow,age,lod,-1.27F,2.45F,1,1);
            check(body.count%32==0,"debris must retain complete eight-faced rock meshes");
            check(glow.count%4==0,"gravity filaments must remain complete quads");
            check(body.maxZ-body.minZ>3,"debris must occupy a three-dimensional funnel");
            int vertices=body.count+glow.count;
            check(vertices<=budgets[lod],"gravity-only detail budget");
            rockPeaks[lod]=Math.max(rockPeaks[lod],body.count/32);
            vertexPeaks[lod]=Math.max(vertexPeaks[lod],vertices);
        }
        check(rockPeaks[0]>44*3,"high detail must show more than three times the previous 44 debris fragments");
        check(rockPeaks[0]>rockPeaks[1]&&rockPeaks[1]>rockPeaks[2],"fragment counts must respect detail settings");
        check(vertexPeaks[0]>vertexPeaks[1]&&vertexPeaks[1]>vertexPeaks[2],"gravity LOD must reduce total geometry");
        Signature bodyA=new Signature(),glowA=new Signature(),bodyB=new Signature(),glowB=new Signature();
        HyperGonerGravity.render(bodyA,glowA,160,0,-1.27F,2.45F,1,1);
        HyperGonerGravity.render(bodyB,glowB,160,0,-1.27F,2.45F,1,1);
        check(bodyA.hash==bodyB.hash&&bodyA.count==bodyB.count&&glowA.hash==glowB.hash&&glowA.count==glowB.count,
                "same scene time must produce exactly deterministic debris and gravity streams");
        Signature bodyNext=new Signature(),glowNext=new Signature();
        HyperGonerGravity.render(bodyNext,glowNext,160.5F,0,-1.27F,2.45F,1,1);
        check(bodyA.hash!=bodyNext.hash&&glowA.hash!=glowNext.hash,"both debris and gravity must move between partial ticks");
        HyperGonerMesh.Sink inactive=(x,y,z,c,a)->{throw new AssertionError("inactive gravity emitted vertices");};
        HyperGonerGravity.render(inactive,inactive,160,0,-1.27F,2.45F,0,1);
        HyperGonerGravity.render(inactive,inactive,160,0,-1.27F,2.45F,1,0);
        for(float invalid:new float[]{Float.NaN,Float.POSITIVE_INFINITY,Float.NEGATIVE_INFINITY})
            HyperGonerGravity.render(inactive,inactive,invalid,0,-1.27F,2.45F,1,1);
        System.out.println("Gravity peak visible fragments "+Arrays.toString(rockPeaks)+"; vertices "+Arrays.toString(vertexPeaks));
    }
    private static final class Signature implements HyperGonerMesh.Sink {
        int count;
        long hash=0xcbf29ce484222325L;
        float minZ=Float.POSITIVE_INFINITY,maxZ=Float.NEGATIVE_INFINITY;
        public void vertex(float x,float y,float z,int rgb,float alpha) {
            check(Float.isFinite(x)&&Float.isFinite(y)&&Float.isFinite(z)&&Float.isFinite(alpha),"finite gravity vertex");
            check(alpha>=0&&alpha<=1,"gravity alpha range");
            check(Math.abs(x)<=15&&Math.abs(y)<=13&&Math.abs(z)<=8,"gravity bounds");
            count++;minZ=Math.min(minZ,z);maxZ=Math.max(maxZ,z);
            mix(Float.floatToIntBits(x));mix(Float.floatToIntBits(y));mix(Float.floatToIntBits(z));mix(rgb);mix(Float.floatToIntBits(alpha));
        }
        private void mix(int value){hash^=value;hash*=0x100000001b3L;}
    }
    private static void preview(String path)throws Exception {
        BufferedImage image=new BufferedImage(1440,900,BufferedImage.TYPE_INT_RGB);
        Graphics2D g=image.createGraphics();g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(new Color(18,21,30));g.fillRect(0,0,1440,900);
        int[] ages={28,65,105,165,226,240};
        for(int frame=0;frame<ages.length;frame++) {
            java.util.List<float[]> body=new ArrayList<>(),glow=new ArrayList<>();
            HyperGonerMesh.render(collector(body),collector(glow),ages[frame],0);
            body.sort(Comparator.comparingDouble(q->(q[2]+q[7]+q[12]+q[17])/4));
            Graphics2D panel=(Graphics2D)g.create(frame%3*480,frame/3*450,480,450);
            panel.setPaint(new GradientPaint(0,0,new Color(111,154,185),0,450,new Color(42,65,77)));
            panel.fillRect(0,0,480,450);
            // A visible world-like grid makes any accidental full-frame dark backing obvious.
            panel.setColor(new Color(190,214,218,45));
            for(int grid=0;grid<480;grid+=40)panel.drawLine(grid,0,grid,450);
            for(int grid=0;grid<450;grid+=40)panel.drawLine(0,grid,480,grid);
            draw(panel,body);draw(panel,glow);
            float flash=HyperGonerMesh.flash(ages[frame]);
            if(flash>0){panel.setColor(new Color(1F,1F,1F,flash));panel.fillRect(0,0,480,450);}
            panel.setColor(new Color(130,160,190));panel.drawString("Hyper Goner / tick "+ages[frame],12,22);panel.dispose();
        }
        g.dispose();ImageIO.write(image,"png",new File(path));
    }
    private static HyperGonerMesh.Sink collector(java.util.List<float[]> list){
        final float[] quad=new float[20];final int[] index={0};
        return (x,y,z,c,a)->{int i=index[0]*5;quad[i]=x;quad[i+1]=y;quad[i+2]=z;quad[i+3]=c;quad[i+4]=a;if(++index[0]==4){list.add(quad.clone());index[0]=0;}};
    }
    private static void draw(Graphics2D g,java.util.List<float[]> list){
        for(float[]q:list){int[]x=new int[4],y=new int[4];float alpha=0;
            for(int k=0;k<4;k++){x[k]=240+Math.round(q[k*5]*31);y[k]=210-Math.round(q[k*5+1]*31);alpha+=q[k*5+4]*.25F;}
            int rgb=(int)q[3];g.setColor(new Color(rgb>>16&255,rgb>>8&255,rgb&255,Math.round(alpha*255)));g.fillPolygon(x,y,4);
        }
    }
}
