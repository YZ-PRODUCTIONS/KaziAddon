package net.kazi.kazimod.models.abilities;

/** Deterministic domain scenery; no particles, world queries, or client-only dependencies. */
public final class InfiniteVoidMesh {
    public static final int STREAK_TICKS = 100;
    public static final int TRANSITION_TICKS = 16;
    private static final float PI = (float) Math.PI;
    private static final float TAU = PI * 2;
    private static final int[] OPENING_COLORS = {0xFF3EA5, 0xFF8CCF, 0xB489FF, 0xFFF4FF};

    private InfiniteVoidMesh() { }

    public static float expansionRadius(float maximum, float age) {
        return maximum * smooth(age / 40);
    }

    public static float voidPhase(float age) {
        return smooth((age - STREAK_TICKS) / TRANSITION_TICKS);
    }

    public static void shell(KokuVfxMesh.Sink out, float radius) {
        if (radius <= 0) return;
        for (int y = 0; y < 24; y++) {
            float a = -PI / 2 + PI * y / 24;
            float b = -PI / 2 + PI * (y + 1) / 24;
            for (int x = 0; x < 48; x++) {
                float c = TAU * x / 48;
                float d = TAU * (x + 1) / 48;
                spherePoint(out, radius, a, c);
                spherePoint(out, radius, b, c);
                spherePoint(out, radius, b, d);
                spherePoint(out, radius, a, d);
            }
        }
        for (int i = 0; i < 64; i++) {
            float a = TAU * i / 64, b = TAU * (i + 1) / 64;
            out.vertex(0, .015F, 0, 0x030308, 1);
            out.vertex(cos(a) * radius, .015F, sin(a) * radius, 0x030308, 1);
            out.vertex(cos(b) * radius, .015F, sin(b) * radius, 0x030308, 1);
            out.vertex(0, .015F, 0, 0x030308, 1);
        }
    }

    private static void spherePoint(KokuVfxMesh.Sink out, float r, float lat, float lon) {
        out.vertex(cos(lat) * cos(lon) * r, sin(lat) * r, cos(lat) * sin(lon) * r, 0x020208, 1);
    }

    /** Separate non-additive pass: light must never fill the event horizon. */
    public static void horizon(KokuVfxMesh.Sink out, float radius, float age,
                               float eyeX, float eyeY, float eyeZ) {
        float phase = voidPhase(age);
        if (radius < .1F || phase <= 0) return;
        Frame f = new Frame(radius, eyeX, eyeY, eyeZ);
        int segments = count(out, 96, 72, 48);
        float r = radius * .128F;
        for (int i = 0; i < segments; i++) {
            float a = TAU * i / segments, b = TAU * (i + 1) / segments;
            f.vertex(out, 0, 0, .012F, 0x000002, phase);
            f.vertex(out, cos(a) * r, sin(a) * r, .012F, 0x000002, phase);
            f.vertex(out, cos(b) * r, sin(b) * r, .012F, 0x000002, phase);
            f.vertex(out, 0, 0, .012F, 0x000002, phase);
        }
    }

    public static void energy(KokuVfxMesh.Sink out, float radius, float age,
                              float eyeX, float eyeY, float eyeZ) {
        if (radius < .1F) return;
        float phase = voidPhase(age);
        float appear = smooth(age / 10);
        View view = new View(out, eyeX, eyeY, eyeZ);
        if (phase < 1) opening(view, radius, age, appear * (1 - phase));
        if (phase > 0) {
            Frame frame = new Frame(radius, eyeX, eyeY, eyeZ);
            blackHole(view, frame, radius, age, phase);
            whiteStreams(view, radius, age, phase);
            fragments(view, radius, age, phase);
        }
        stars(view, radius, age, appear * (.12F + phase * .88F));
    }

    private static void opening(View v, float r, float age, float opacity) {
        v.flowOrigin(r * .08F, r * .74F);
        int strands = count(v.out, 120, 84, 56);
        for (int i = 0; i < strands; i++) {
            float angle = i * 2.399963F;
            float speed = .013F + hash(i, 1) * .012F;
            float travel = fract(hash(i, 2) + age * speed);
            float length = .16F + hash(i, 3) * .28F;
            float start = Math.max(.025F, travel - length), end = Math.max(.026F, travel);
            float life = smooth(travel / .12F) * smooth((1 - travel) / .12F);
            float reach = .7F + .22F * hash(i, 4);
            float width = r * (.0008F + .0028F * hash(i, 5));
            int color = OPENING_COLORS[i % OPENING_COLORS.length];
            for (int j = 0; j < 3; j++) {
                float t0 = j / 3F, t1 = (j + 1) / 3F;
                float p = lerp(start, end, t0), q = lerp(start, end, t1);
                float a = angle + .025F * sin(age * .018F + p * 3 + i);
                float b = angle + .025F * sin(age * .018F + q * 3 + i);
                float x = cos(a) * r * reach * p, y = r * (.08F + sin(a) * reach * p);
                float xx = cos(b) * r * reach * q, yy = r * (.08F + sin(b) * reach * q);
                float a0 = opacity * life * sin(PI * t0), a1 = opacity * life * sin(PI * t1);
                v.ribbon(x, y, r * (.74F - 1.3F * p), xx, yy, r * (.74F - 1.3F * q),
                        width * 4, width * 4, color, a0 * .14F, a1 * .14F);
                v.ribbon(x, y, r * (.74F - 1.3F * p), xx, yy, r * (.74F - 1.3F * q),
                        width, width, color, a0 * .85F, a1 * .85F);
                if (i % 3 == 0) v.ribbon(x, y, r * (.74F - 1.3F * p), xx, yy, r * (.74F - 1.3F * q),
                        width * .28F, width * .28F, 0xFFFFFF, a0, a1);
            }
        }
    }

    private static void blackHole(View v, Frame f, float r, float age, float opacity) {
        // The outer lens and inner photon ring frame a hazy, moving accretion field.
        accretion(v.out, f, r, age, opacity);
        ring(v.out, f, r * .302F, r * .060F, age, .008F * r, 0xA6B0F8, opacity * .18F);
        ring(v.out, f, r * .302F, r * .026F, age, .006F * r, 0xFFDC89, opacity * .7F);
        ring(v.out, f, r * .302F, r * .015F, age, .004F * r, 0xFFFFF2, opacity);
        ring(v.out, f, r * .132F, r * .022F, -age * .6F, .0008F * r, 0xFFE4AE, opacity * .5F);
        ring(v.out, f, r * .132F, r * .005F, age * .4F, .0004F * r, 0xFFFFFF, opacity);

        int wisps = count(v.out, 22, 16, 10);
        int steps = count(v.out, 18, 14, 10);
        for (int i = 0; i < wisps; i++) {
            float initial = hash(i, 10) * TAU + age * (.004F + .004F * hash(i, 11));
            float radius = r * (.154F + .12F * hash(i, 12));
            float length = .45F + 1.6F * hash(i, 13);
            for (int j = 0; j < steps; j++) {
                float t = j / (float) steps, u = (j + 1F) / steps;
                float a = initial + t * length, b = initial + u * length;
                float rr = radius + r * .009F * sin(a * 6 + i);
                float ss = radius + r * .009F * sin(b * 6 + i);
                float w = r * (.006F + .01F * hash(i, 14));
                f.arc(v.out, a, rr, b, ss, w,
                        i % 3 == 0 ? 0xE9E9FF : 0xA6AEE9,
                        opacity * .28F * sin(PI * t), opacity * .28F * sin(PI * u));
            }
        }

        // Broken white flows sweep around the center rather than drawing a solid cross over it.
        int arcs = count(v.out, 16, 12, 8);
        for (int i = 0; i < arcs; i++) {
            float angle = i * 2.399963F + age * .005F;
            float travel = fract(age * (.003F + hash(i, 20) * .002F) + hash(i, 21));
            float fade = opacity * sin(PI * travel);
            for (int j = 0; j < 8; j++) {
                float t = j / 8F, u = (j + 1) / 8F;
                float a = angle + t * .65F, b = angle + u * .65F;
                float rr = r * (.15F + .22F * t + .035F * travel);
                float ss = r * (.15F + .22F * u + .035F * travel);
                float width = r * (.001F + .008F * hash(i, 22));
                f.arc(v.out, a, rr, b, ss, width,
                        0xFFFDF6, fade * sin(PI * t), fade * sin(PI * u));
            }
        }
    }

    private static void accretion(KokuVfxMesh.Sink out, Frame f, float r, float age, float opacity) {
        int segments = count(out, 96, 72, 48), bands = count(out, 16, 12, 8);
        for (int band = 0; band < bands; band++) for (int i = 0; i < segments; i++) {
            float a = TAU * i / segments, b = TAU * (i + 1) / segments;
            float q = band / (float) bands, s = (band + 1F) / bands;
            cloudPoint(out,f,r,a,q,age,opacity);
            cloudPoint(out,f,r,b,q,age,opacity);
            cloudPoint(out,f,r,b,s,age,opacity);
            cloudPoint(out,f,r,a,s,age,opacity);
        }
    }

    private static void cloudPoint(KokuVfxMesh.Sink out, Frame f, float r, float a, float q, float age, float opacity) {
        float curl = a + q * 2.8F - age * .006F;
        float noise = .5F + .26F * sin(curl * 5 + sin(q * 12 - a * 3))
                + .16F * sin(curl * 11 - q * 23 + age * .007F) + .08F * sin(a * 19 + q * 27 - age * .014F);
        float density = smooth(q / .16F) * smooth((1 - q) / .2F) * (.1F + .72F * noise * noise);
        float radius = r * (.134F + q * .17F);
        int blue = (int) (174 + noise * 81), red = (int) (124 + noise * 118), green = (int) (132 + noise * 114);
        f.vertex(out, cos(a) * radius, sin(a) * radius, -.01F, red << 16 | green << 8 | blue, density * opacity);
    }

    private static void fragments(View v, float r, float age, float opacity) {
        int count = count(v.out, 40, 28, 16);
        for (int i = 0; i < count; i++) {
            float a = i * 2.399963F + age * .0018F;
            float h = .08F + .84F * hash(i, 50);
            float radial = (float) Math.sqrt(1 - h * h) * r * .93F;
            float x = cos(a) * radial, y = h * r * .93F, z = sin(a) * radial;
            float size = r * (.008F + .033F * hash(i, 51));
            float alpha = opacity * (.65F + .2F * sin(age * .025F + i));
            v.fragment(x,y,z,size,age*.012F+i,i,alpha);
        }
    }

    private static void ring(KokuVfxMesh.Sink out, Frame f, float radius, float width,
                             float age, float distortion, int color, float opacity) {
        int segments = count(out, 96, 72, 48);
        for (int i = 0; i < segments; i++) {
            float a = TAU * i / segments, b = TAU * (i + 1) / segments;
            float ra = radius + distortion * (sin(a * 7 + age * .03F) * .6F + sin(a * 13 - age * .021F) * .4F);
            float rb = radius + distortion * (sin(b * 7 + age * .03F) * .6F + sin(b * 13 - age * .021F) * .4F);
            float ca = opacity * (.82F + .18F * sin(a * 3 - age * .04F));
            float cb = opacity * (.82F + .18F * sin(b * 3 - age * .04F));
            for (int side = -1; side <= 1; side += 2) {
                f.vertex(out, cos(a) * ra, sin(a) * ra, .02F, color, ca);
                f.vertex(out, cos(b) * rb, sin(b) * rb, .02F, color, cb);
                f.vertex(out, cos(b) * (rb + side * width), sin(b) * (rb + side * width), .02F, color, 0);
                f.vertex(out, cos(a) * (ra + side * width), sin(a) * (ra + side * width), .02F, color, 0);
            }
        }
    }

    private static void whiteStreams(View v, float r, float age, float opacity) {
        int tracks = count(v.out, 6, 4, 3);
        StreamPoint start = new StreamPoint(), end = new StreamPoint();
        // Evenly spaced arcs keep every direction populated, including on low detail.
        for (int track = 0; track < tracks; track++) {
            float height = track / (float) (tracks - 1);
            float radius = r * (.48F + .36F * hash(track, 31));
            float latitude = .14F + height * 1.12F;
            float wave = .025F + height * .075F;
            float phase = hash(track, 34) * TAU;
            float speed = (.018F + .012F * hash(track, 35)) * (track % 2 == 0 ? 1 : -1);
            for (int lane = 0; lane < 12; lane++) {
                int seed = track * 12 + lane;
                float angle = TAU * lane / 12 + track * 2.399963F + age * speed;
                float length = .28F + .42F * hash(seed, 32);
                float width = r * (.0025F + .012F * hash(seed, 33));
                float fade = opacity * (.8F + .2F * sin(age * .025F + seed));
                start.sample(v, radius, angle, latitude, wave, phase);
                for (int j = 0; j < 6; j++) {
                    float t = j / 6F, u = (j + 1) / 6F;
                    end.sample(v, radius, angle + length * u, latitude, wave, phase);
                    float rough = .65F + .35F * sin(seed * 2 + t * 10 + age * .02F);
                    float roughNext = .65F + .35F * sin(seed * 2 + u * 10 + age * .02F);
                    v.streamSegment(start, end, width * rough, width * roughNext,
                            fade * Math.max(0, sin(PI * t)), fade * Math.max(0, sin(PI * u)));
                    StreamPoint swap = start; start = end; end = swap;
                }
            }
        }
    }

    private static final class StreamPoint {
        float x, y, z, nx, ny, nz;
        boolean visible;

        void sample(View view, float radius, float angle, float latitude, float wave, float phase) {
            float lat = latitude + wave * sin(angle * 2 + phase);
            float slope = wave * 2 * cos(angle * 2 + phase);
            float ca = cos(angle), sa = sin(angle), cl = cos(lat), sl = sin(lat);
            x = radius * cl * ca;
            y = radius * sl;
            z = radius * cl * sa;
            float tx = -sl * slope * ca - cl * sa;
            float ty = cl * slope;
            float tz = -sl * slope * sa + cl * ca;
            float ex = view.ex - x, ey = view.ey - y, ez = view.ez - z;
            nx = ty * ez - tz * ey;
            ny = tz * ex - tx * ez;
            nz = tx * ey - ty * ex;
            float length = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
            visible = length > .00001F;
            if (visible) { nx /= length; ny /= length; nz /= length; }
        }
    }

    private static void stars(View v, float r, float age, float opacity) {
        v.flowing = false;
        int stars = count(v.out, 140, 96, 64);
        for (int i = 0; i < stars; i++) {
            float a = i * 2.399963F, h = 1 - 2 * (i + .5F) / stars;
            float radial = (float) Math.sqrt(1 - h * h) * r * .96F;
            float x = cos(a) * radial, y = h * r * .96F, z = sin(a) * radial;
            float size = r * (.0007F + .0015F * hash(i, 40));
            float alpha = opacity * (.25F + .25F * sin(age * .026F + i));
            v.ribbon(x - size, y, z, x + size, y, z, size * .6F, size * .6F, 0xD9DCFF, alpha, alpha);
        }
    }

    private static int count(KokuVfxMesh.Sink out, int high, int medium, int low) {
        return out.detail() == 0 ? high : out.detail() == 1 ? medium : low;
    }

    private static float hash(int i, int salt) {
        int value = i * 374761393 + salt * 668265263;
        value = (value ^ (value >>> 13)) * 1274126177;
        return ((value ^ (value >>> 16)) & 0xFFFFFF) / 16777216F;
    }

    private static float smooth(float p) {
        p = Math.max(0, Math.min(1, p));
        return p * p * (3 - 2 * p);
    }

    private static float lerp(float a, float b, float p) { return a + (b - a) * p; }
    private static float sin(float v) { return (float) Math.sin(v); }
    private static float cos(float v) { return (float) Math.cos(v); }
    private static float fract(float v) { return v - (float) Math.floor(v); }

    /** Fixed world-space center, with the lens facing the viewer as a gravitational image. */
    private static final class Frame {
        final float cy, cz, ux, uy, uz, vx, vy, vz, nx, ny, nz;
        Frame(float radius, float ex, float ey, float ez) {
            cy = radius * .37F;
            cz = radius * .68F;
            float x = ex, y = ey - cy, z = ez - cz;
            float length = (float) Math.sqrt(x * x + y * y + z * z);
            if (length < .001F) { x = 0; y = 0; z = -1; length = 1; }
            nx = x / length; ny = y / length; nz = z / length;
            float horizontal = (float) Math.sqrt(nx * nx + nz * nz);
            ux = horizontal > .001F ? -nz / horizontal : 1;
            uy = 0;
            uz = horizontal > .001F ? nx / horizontal : 0;
            vx = ny * uz;
            vy = nz * ux - nx * uz;
            vz = -ny * ux;
        }
        void vertex(KokuVfxMesh.Sink out, float x, float y, float z, int color, float alpha) {
            out.vertex(ux * x + vx * y + nx * z, cy + uy * x + vy * y + ny * z,
                    cz + uz * x + vz * y + nz * z, color, Math.max(0, alpha));
        }
        void arc(KokuVfxMesh.Sink out, float a, float r, float b, float s,
                 float width, int color, float alpha, float endAlpha) {
            for (int side=-1;side<=1;side+=2) {
                vertex(out,cos(a)*r,sin(a)*r,.025F,color,alpha);
                vertex(out,cos(b)*s,sin(b)*s,.025F,color,endAlpha);
                vertex(out,cos(b)*(s+side*width),sin(b)*(s+side*width),.025F,color,0);
                vertex(out,cos(a)*(r+side*width),sin(a)*(r+side*width),.025F,color,0);
            }
        }
    }

    private static final class View {
        final KokuVfxMesh.Sink out;
        final float ex, ey, ez;
        boolean flowing;
        float originY, originZ;
        View(KokuVfxMesh.Sink out, float x, float y, float z) { this.out = out; ex = x; ey = y; ez = z; }

        void flowOrigin(float y, float z) { flowing = true; originY = y; originZ = z; }

        void streamSegment(StreamPoint a, StreamPoint b, float width, float endWidth, float alpha, float endAlpha) {
            if (!a.visible || !b.visible || alpha + endAlpha <= .001F) return;
            for (int layer = 0; layer < 2; layer++) {
                float scale = layer == 0 ? 3 : 1, opacity = layer == 0 ? .11F : .9F;
                int color = layer == 0 ? 0xBBC5FF : 0xFFFFFF;
                for (int side = -1; side <= 1; side += 2) {
                    out.vertex(a.x, a.y, a.z, color, alpha * opacity);
                    out.vertex(b.x, b.y, b.z, color, endAlpha * opacity);
                    out.vertex(b.x + b.nx * endWidth * scale * side, b.y + b.ny * endWidth * scale * side,
                            b.z + b.nz * endWidth * scale * side, color, 0);
                    out.vertex(a.x + a.nx * width * scale * side, a.y + a.ny * width * scale * side,
                            a.z + a.nz * width * scale * side, color, 0);
                }
            }
        }

        void fragment(float x, float y, float z, float size, float spin, int seed, float alpha) {
            float nx=ex-x, ny=ey-y, nz=ez-z;
            float length=(float)Math.sqrt(nx*nx+ny*ny+nz*nz);
            if(length<.001F)return;
            nx/=length;ny/=length;nz/=length;
            float horizontal=(float)Math.sqrt(nx*nx+nz*nz);
            float ux=horizontal>.001F?-nz/horizontal:1, uz=horizontal>.001F?nx/horizontal:0;
            float vx=ny*uz, vy=nz*ux-nx*uz, vz=-ny*ux;
            for(int i=0;i<10;i++) {
                float a=TAU*i/10+spin, b=TAU*(i+1)/10+spin;
                float ra=size*(.55F+.45F*hash(seed,i+70)), rb=size*(.55F+.45F*hash(seed,(i+1)%10+70));
                float ax=cos(a)*ra, ay=sin(a)*ra*.6F, bx=cos(b)*rb, by=sin(b)*rb*.6F;
                for(int layer=0;layer<2;layer++) {
                    float scale=layer==0?2.2F:1, edge=layer==0?0:alpha*.8F;
                    int color=layer==0?0xBCC9FF:0xFFFFFF;
                    float center=layer==0?alpha*.12F:alpha;
                    out.vertex(x,y,z,color,center);
                    out.vertex(x+(ux*ax+vx*ay)*scale,y+vy*ay*scale,z+(uz*ax+vz*ay)*scale,color,edge);
                    out.vertex(x+(ux*bx+vx*by)*scale,y+vy*by*scale,z+(uz*bx+vz*by)*scale,color,edge);
                    out.vertex(x,y,z,color,center);
                }
            }
        }

        void ribbon(float x, float y, float z, float xx, float yy, float zz,
                    float width, float endWidth, int color, float alpha, float endAlpha) {
            alpha = Math.max(0, alpha); endAlpha = Math.max(0, endAlpha);
            if (alpha + endAlpha <= .001F) return;
            float dx = xx - x, dy = yy - y, dz = zz - z;
            float cx = ex - (x + xx) * .5F, cy = ey - (y + yy) * .5F, cz = ez - (z + zz) * .5F;
            float nx = dy * cz - dz * cy, ny = dz * cx - dx * cz, nz = dx * cy - dy * cx;
            float endNx=nx, endNy=ny, endNz=nz;
            if (flowing) {
                // Adjacent segments share endpoint widths, avoiding cracks in the flowing ribbons.
                nx=(y-originY)*(ez-z)-(z-originZ)*(ey-y);
                ny=(z-originZ)*(ex-x)-x*(ez-z);
                nz=x*(ey-y)-(y-originY)*(ex-x);
                endNx=(yy-originY)*(ez-zz)-(zz-originZ)*(ey-yy);
                endNy=(zz-originZ)*(ex-xx)-xx*(ez-zz);
                endNz=xx*(ey-yy)-(yy-originY)*(ex-xx);
            }
            float length = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
            float endLength=(float)Math.sqrt(endNx*endNx+endNy*endNy+endNz*endNz);
            if (length < .00001F || endLength < .00001F) return;
            nx /= length; ny /= length; nz /= length;
            endNx/=endLength; endNy/=endLength; endNz/=endLength;
            for (int side = -1; side <= 1; side += 2) {
                out.vertex(x, y, z, color, alpha);
                out.vertex(xx, yy, zz, color, endAlpha);
                out.vertex(xx + endNx * endWidth * side, yy + endNy * endWidth * side, zz + endNz * endWidth * side, color, 0);
                out.vertex(x + nx * width * side, y + ny * width * side, z + nz * width * side, color, 0);
            }
        }
    }
}
