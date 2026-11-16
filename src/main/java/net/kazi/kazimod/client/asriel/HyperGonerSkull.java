package net.kazi.kazimod.client.asriel;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

/** The supplied Hyper Goner model, including its original neutral and laughing shapes. */
public final class HyperGonerSkull {
    private static final String RESOURCE="/assets/kazimod/models/cinematics/hyper_goner.hgm.gz";
    private static final Mesh MODEL=load();
    private HyperGonerSkull() { }

    public static void render(HyperGonerMesh.Sink out,float open,int detail) {
        if(!Float.isFinite(open))return;
        open=Math.max(0,Math.min(1,open));
        MODEL.draw(out,open);
        HyperGonerMouth.render(out,open);
    }

    private static Mesh load() {
        InputStream resource=HyperGonerSkull.class.getResourceAsStream(RESOURCE);
        if(resource==null)throw new IllegalStateException("Missing Hyper Goner model: "+RESOURCE);
        try(DataInputStream in=new DataInputStream(new BufferedInputStream(new GZIPInputStream(resource)))) {
            if(in.readInt()!=0x48474D31)throw new IOException("Unsupported Hyper Goner mesh format");
            int vertices=in.readInt(),indices=in.readInt();
            if(vertices<=0||vertices>100000||indices<=0||indices>300000||indices%3!=0)
                throw new IOException("Invalid Hyper Goner mesh counts");
            Mesh mesh=new Mesh(vertices,indices);
            for(int i=0;i<vertices;i++)for(int pose=0;pose<2;pose++) {
                for(int axis=0;axis<3;axis++) {
                    float value=in.readFloat();
                    if(!Float.isFinite(value)||Math.abs(value)>16)throw new IOException("Invalid model vertex");
                    mesh.positions[pose][i*3+axis]=value;
                }
                mesh.colors[pose][i]=in.readInt()&0xFFFFFF;
            }
            for(int i=0;i<indices;i++) {
                int index=in.readInt();
                if(index<0||index>=vertices)throw new IOException("Invalid model index");
                mesh.indices[i]=index;
            }
            if(in.read()!=-1)throw new IOException("Trailing Hyper Goner model data");
            return mesh;
        } catch(IOException error) {
            throw new IllegalStateException("Cannot read Hyper Goner model",error);
        }
    }

    private static final class Mesh {
        final float[][] positions;
        final int[][] colors;
        final int[] indices,posedColors;
        final float[] posed;
        float lastOpen=Float.NaN;

        Mesh(int vertices,int indices) {
            positions=new float[2][vertices*3];colors=new int[2][vertices];
            posed=new float[vertices*3];posedColors=new int[vertices];this.indices=new int[indices];
        }

        void draw(HyperGonerMesh.Sink out,float open) {
            // Morph each indexed vertex once, with no model parsing or allocation per frame.
            if(open!=lastOpen) {
                for(int i=0;i<posed.length;i++)posed[i]=positions[0][i]+(positions[1][i]-positions[0][i])*open;
                for(int i=0;i<posedColors.length;i++) {
                    int a=colors[0][i],b=colors[1][i];
                    int r=(int)((a>>16&255)+((b>>16&255)-(a>>16&255))*open);
                    int g=(int)((a>>8&255)+((b>>8&255)-(a>>8&255))*open);
                    int blue=(int)((a&255)+((b&255)-(a&255))*open);
                    posedColors[i]=r<<16|g<<8|blue;
                }
                lastOpen=open;
            }
            // The cinematic batch uses quads. A repeated last corner preserves each
            // original triangle exactly, including the model's holes and material edges.
            for(int i=0;i<indices.length;i+=3) {
                emit(out,indices[i]);emit(out,indices[i+1]);emit(out,indices[i+2]);emit(out,indices[i+2]);
            }
        }

        private void emit(HyperGonerMesh.Sink out,int index) {
            int p=index*3;
            out.vertex(posed[p],posed[p+1],posed[p+2],posedColors[index],1);
        }
    }
}
