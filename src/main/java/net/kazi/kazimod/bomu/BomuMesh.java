package net.kazi.kazimod.bomu;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.kazi.kazimod.kake.KakeMesh;
import net.kazi.kazimod.kake.KakeMesh.Sink;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.math.vector.Vector4f;

/** Short, deterministic mesh animations; no individual spark or smoke entities. */
public final class BomuMesh {
    private static final int WHITE = 0xfffbd7, YELLOW = 0xffe82b, GOLD = 0xffb51c;
    private static final int ORANGE = 0xff6a0d, RED = 0xc82a09, SMOKE = 0x534448;
    private static final double TAU = Math.PI * 2;
    private static final double[][] NEAR_SPHERE = sphere(16, 8), FAR_SPHERE = sphere(10, 5);

    private static final class State extends RenderState {
        State() { super(null, null, null); }
        static RenderType type(boolean glow) {
            return RenderType.create(glow ? "bomu_glow" : "bomu_flame", DefaultVertexFormats.POSITION_COLOR,
                    7, 65536, false, true, RenderType.State.builder()
                            .setTransparencyState(glow ? LIGHTNING_TRANSPARENCY : TRANSLUCENT_TRANSPARENCY)
                            .setCullState(NO_CULL).setDepthTestState(LEQUAL_DEPTH_TEST)
                            .setWriteMaskState(COLOR_WRITE).createCompositeState(false));
        }
    }
    private static final RenderType FLAME = State.type(false), GLOW = State.type(true);

    private BomuMesh() {}

    private static Sink sink(MatrixStack stack, IRenderTypeBuffer buffers, boolean glow) {
        IVertexBuilder b = buffers.getBuffer(glow ? GLOW : FLAME);
        Vector4f v = new Vector4f(0, 0, 0, 1);
        return (x, y, z, color, alpha) -> {
            v.set((float) x, (float) y, (float) z, 1);
            v.transform(stack.last().pose());
            b.vertex(v.x(), v.y(), v.z()).color(color >> 16 & 255, color >> 8 & 255, color & 255,
                    (int) (clamp(alpha) * 255)).endVertex();
        };
    }

    private static float clamp(double x) { return (float) Math.max(0, Math.min(1, x)); }
    private static double noise(int i, int seed) {
        double n = Math.sin(i * 127.1 + seed * 31.73) * 43758.5453;
        return n - Math.floor(n);
    }

    private static void triangle(Sink s, double[] a, double[] b, double[] c, int color, float alpha) {
        s.vertex(a[0], a[1], a[2], color, alpha); s.vertex(b[0], b[1], b[2], color, alpha);
        s.vertex(c[0], c[1], c[2], color, alpha); s.vertex(c[0], c[1], c[2], color, alpha);
    }

    private static double[][] sphere(int segments, int rings) {
        double[][] out=new double[segments*rings*4][6];
        int index=0;
        for(int y=0;y<rings;y++) for(int x=0;x<segments;x++) for(int k=0;k<4;k++) {
            double latitude=Math.PI*(y+(k==1||k==2?1:0))/rings;
            double longitude=TAU*(x+(k>=2?1:0))/segments;
            out[index++]=new double[]{Math.cos(longitude)*Math.sin(latitude),Math.cos(latitude),
                    Math.sin(longitude)*Math.sin(latitude),longitude*5,latitude*4,x+y*3};
        }
        return out;
    }

    private static void cloud(Sink s, double r, float age, int seed, int color, float alpha, boolean far) {
        double phase = noise(3, seed) * TAU;
        for (double[] p : far ? FAR_SPHERE : NEAR_SPHERE) {
            double wave = r*(1+.085*Math.sin(p[3]+phase+age*.12)*Math.sin(p[4]+phase));
            // Broad cel-shaded patches keep the flame readable against bright terrain.
            int tint = ((int)p[5]+seed)%7==0 ? mix(color,GOLD,.18F) : color;
            s.vertex(p[0]*wave,p[1]*wave,p[2]*wave,tint,alpha);
        }
    }

    private static int mix(int a, int b, float t) {
        return ((int) ((a >> 16 & 255) * (1-t) + (b >> 16 & 255) * t) << 16)
                | ((int) ((a >> 8 & 255) * (1-t) + (b >> 8 & 255) * t) << 8)
                | (int) ((a & 255) * (1-t) + (b & 255) * t);
    }

    private static void star(Sink s, double size, int color, float alpha) {
        for (int plane = 0; plane < 2; plane++) {
            Sink p = KakeMesh.rotate(s, plane * Math.PI / 2, 1);
            for (int i = 0; i < 4; i++) {
                Sink arm = KakeMesh.rotate(p, i * Math.PI / 2, 2);
                triangle(arm, new double[]{-.11*size, 0, 0}, new double[]{0, size, 0},
                        new double[]{.11*size, 0, 0}, color, alpha);
            }
        }
    }

    private static void spike(Sink s, double radius, double length, int color, float alpha) {
        for (int i = 0; i < 4; i++) {
            double a = i * Math.PI / 2, b = (i+1) * Math.PI / 2;
            triangle(s, new double[]{Math.cos(a)*radius, Math.sin(a)*radius, 0},
                    new double[]{Math.cos(b)*radius, Math.sin(b)*radius, 0}, new double[]{0, 0, length}, color, alpha);
        }
    }

    private static Sink ray(Sink s, double yaw, double pitch, double distance) {
        return KakeMesh.offset(KakeMesh.rotate(KakeMesh.rotate(s, yaw, 1), pitch, 0), 0, 0, distance);
    }

    public static void render(MatrixStack stack, IRenderTypeBuffer buffers, int kind, float age,
                              float life, float radius, int seed, boolean far) {
        if (age < 0 || age >= life || radius <= 0) return;
        // Immediate buffers share a builder: finish each render type before requesting the next.
        for (boolean glowing : new boolean[]{false,true}) {
            Sink s=KakeMesh.scale(sink(stack,buffers,glowing),radius);
            if(kind==BomuVfxEntity.HANDS) hands(s,age,seed,far,glowing);
            else if(kind==BomuVfxEntity.JET) {
                Sink jet=(x,y,z,c,a)->s.vertex(x*.55,y*.55,z*1.6+.8,c,a);
                burst(jet,age,life,seed,far,false,glowing);
            } else burst(s,age,life,seed,far,kind==BomuVfxEntity.FLASH,glowing);
        }
    }

    private static void burst(Sink s, float age, float life, int seed, boolean far, boolean flash, boolean glowing) {
        float t = age / life;
        double expand = .12 + .88 * (1-Math.pow(1-clamp(age/8), 3));
        float fire = clamp((.66-t)/.24), smoke = clamp((t-.25)/.18) * clamp((1-t)/.28);
        Sink body = KakeMesh.scale(s, expand);

        // Lobed outer fire transitions to rising smoke after the short white-hot flash.
        if (!glowing && !flash) for (int i = 0, n = far ? 6 : 10; i < n; i++) {
            double a = TAU * i / n + noise(1, seed), h = .25 + noise(i+7, seed)*.65;
            double spread = .60 + t*.32;
            Sink lobe = KakeMesh.offset(body, Math.cos(a)*spread, h+t*.7, Math.sin(a)*spread);
            cloud(lobe, .42+noise(i+19, seed)*.18, age, seed+i, mix(ORANGE, SMOKE, clamp((t-.25)*2)),
                    Math.max(fire*.88F, smoke*.72F), far);
        }
        if (!glowing && fire > 0) {
            cloud(body, 1, age, seed, RED, fire*.72F, far);
            cloud(body, .90, age, seed+1, ORANGE, fire*.85F, far);
            cloud(body, .75, age, seed+2, YELLOW, fire*.96F, far);
            int rays = far ? 12 : 22;
            for (int i = 0; i < rays; i++) {
                double yaw = TAU*noise(i, seed), pitch = Math.asin(noise(i+30, seed)*1.8-.9);
                double length = (.45+noise(i+60, seed)*.8) * (1-t*.7);
                Sink outer = ray(body, yaw, pitch, .62);
                spike(outer, .19, length, ORANGE, fire*.8F);
                spike(outer, .09, length*.88, YELLOW, fire);
            }
        }

        if (!glowing) return;
        if(fire>0) {
            cloud(body,.82,age,seed+2,YELLOW,fire*.44F,far);
            cloud(body,.48,age,seed+3,WHITE,fire*.82F,far);
            cloud(body,1.06,age,seed,GOLD,fire*.06F,far);
        }

        float wave = clamp((.65-t)/.5);
        double ring = .2 + 2*t;
        KakeMesh.ring(s, ring, .028+.04*(1-t), .08, WHITE, wave*.8F, far?28:48);
        KakeMesh.ring(s, ring*.91, .025, .11, GOLD, wave*.5F, far?28:48);
        if (flash) {
            KakeMesh.ring(KakeMesh.rotate(s, Math.PI/2, 0), ring, .025, 0, YELLOW, wave*.6F, 48);
        }
        for (int i = 0, n = far ? 8 : (flash?32:18); i < n; i++) {
            double yaw = noise(i+100, seed)*TAU, pitch = Math.asin(noise(i+160, seed)*1.8-.9);
            Sink spark = ray(s, yaw, pitch, .7+t*(1.4+noise(i+200, seed)));
            float alpha = clamp((1-t)*1.5) * (.65F+.35F*(float)Math.sin(age*.45+i));
            star(spark, (.05+noise(i+230, seed)*.1)*(1-t*.6)*(flash?1.5:1), WHITE, alpha);
            if (!flash) spike(spark, .013, -.22, GOLD, alpha*.8F);
        }
    }

    private static void hands(Sink s, float age, int seed, boolean far, boolean glowing) {
        double pulse = .7+.08*Math.sin(age*.5), ramp = Math.min(1, age/7);
        if(!glowing) {
            cloud(s, pulse*ramp*.65, age, seed, ORANGE, .65F, true);
            cloud(s, pulse*ramp*.44, age, seed+1, YELLOW, .9F, true);
            for (int i=0;i<8;i++) {
                Sink point=ray(s, i*TAU/8+age*.015, .25*Math.sin(i+age*.08), .22);
                spike(point,.10,(.5+.15*Math.sin(age*.4+i))*ramp, YELLOW,.9F);
            }
            return;
        }
        cloud(s, pulse*ramp, age, seed, GOLD, .12F, true);
        for (int i = 0, n = far ? 7 : 13; i < n; i++) {
            double cycle = ((age+i*5)%20)/20;
            double yaw = noise(i,seed)*TAU, pitch = Math.asin(noise(i+20,seed)*1.8-.9);
            Sink spark = ray(s, yaw, pitch, .45+cycle*1.2);
            star(spark, .12*Math.sin(cycle*Math.PI), GOLD, .95F);
        }
        star(s, .5*ramp, WHITE, .65F);
    }

    public static void projectile(MatrixStack stack, IRenderTypeBuffer buffers, float age, int seed, float size) {
        for(boolean glowing:new boolean[]{false,true}) {
            Sink s=KakeMesh.scale(sink(stack,buffers,glowing),size);
            hands(s,age+8,seed,true,glowing);
            if(!glowing) for (int i=0;i<3;i++) {
                Sink tail=KakeMesh.rotate(KakeMesh.offset(s,0,0,-i*.35),Math.PI,1);
                spike(tail,.26-i*.055,1.7-i*.25,i==0?ORANGE:YELLOW,.75F-i*.15F);
            }
        }
    }
}
