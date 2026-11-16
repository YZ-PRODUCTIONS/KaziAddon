import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.FileImageOutputStream;
import net.kazi.kazimod.abilities.YagiAwakening.ChaosBusterPattern;
import net.kazi.kazimod.abilities.YagiAwakening.ShockerBreakerDimensions;
import net.kazi.kazimod.abilities.YagiAwakening.ShockerBreakerExSequence;
import net.kazi.kazimod.abilities.YagiAwakening.ShockerBreakerVisualTiming;
import net.kazi.kazimod.renderers.entities.asriel.AsrielVisualGeometry;
import net.kazi.kazimod.renderers.entities.asriel.ChaosBeamVisuals;
import net.kazi.kazimod.renderers.entities.asriel.ShockerBreakerVisuals;
import net.kazi.kazimod.renderers.entities.asriel.ShockerBreakerWarningVisuals;

/** Exact native meshes with additive blending. Previews only; never writes to game assets. */
public final class ExplosiveBeamsPreview {
    private static final int BACKGROUND = 0xFF0A101A;
    private static final Color TEXT = new Color(230, 238, 252), MUTED = new Color(147, 168, 194);
    private static final Camera SHOCKER = new Camera(.55, .16, 1);
    private static final Camera IMPACT = new Camera(.55, .45, 1);
    private static final Camera BEAM = new Camera(1, .32, .24);
    private static final Camera CANNON = new Camera(.90, .28, .60);
    private static final Camera CANNON_FRONT = new Camera(.15, .16, 1);
    private static final Camera BARRAGE = new Camera(.55, .35, 1);
    private static final Camera TRAIL = new Camera(.28, .24, 1);
    private static final double VISUAL_GROWTH = ShockerBreakerDimensions.VISUAL_SCALE / 3.0;
    private static final int MARK_INTERVAL = ShockerBreakerExSequence.MARK_INTERVAL_TICKS;
    private static final int LAST_MARK_TICK = (ShockerBreakerExSequence.MARK_ROUNDS - 1) * MARK_INTERVAL;
    private static final double[] TRAIL_BOUNDS = {-90,-40,90,78 * VISUAL_GROWTH};
    private static final double[][] TRAIL_MARKS = {{-32,12},{-16,-12},{0,12},{16,-12},{32,12}};
    private static final double[] SHOCK_BOUNDS = scaledBounds(-20, -6, 20, 76);
    private static final double[] IMPACT_BOUNDS = scaledBounds(-20, -7, 20, 25);
    private static final double[] BEAM_BOUNDS = beamBounds();
    private static final double[] MUZZLE_BOUNDS = closeBounds(2);
    private static final double[] ENDPOINT_BOUNDS = closeBounds(ChaosBusterPattern.BEAM_RANGE,
            ChaosBeamVisuals.cullingRadius(Math.max(3.2, ChaosBusterPattern.BEAM_RADIUS)) + 1);
    private static final double[] CANNON_BOUNDS = {-3.7,-3.0,5.0,3.4};
    private static final double[] CANNON_FRONT_BOUNDS = {-3.5,-3.1,3.5,3.4};
    private ExplosiveBeamsPreview() { }

    public static void main(String[] args) throws Exception {
        File output = new File(args.length == 0 ? "." : args[0], "deliverables/asriel_vfx/explosive_beams");
        if (!output.isDirectory() && !output.mkdirs()) throw new IllegalStateException("Cannot create preview folder");
        if (args.length > 1 && "--chaos-only".equals(args[1])) {
            cannonComparison(output);
            cannonAnimation(output);
            chaosFrames(output);
            animation(new File(output, "chaos_buster_and_blaster.gif"), false);
            ImageIO.write(render(beam(4.5F, true), BEAM, BEAM_BOUNDS, 1500, 650), "png", new File(output, "chaos_blaster_detail.png"));
            System.out.println("Chaos cannon and beam previews exported to " + output.getAbsolutePath());
            return;
        }
        if (args.length > 1 && "--shocker-only".equals(args[1])) {
            shockerFrames(output);
            animation(new File(output, "shocker_breaker_and_ex.gif"), true);
            System.out.println("Shocker warning, sustain and fade previews exported to " + output.getAbsolutePath());
            return;
        }
        contactSheet(output);
        shockerFrames(output);
        cannonComparison(output);
        cannonAnimation(output);
        chaosFrames(output);
        barrage(output);
        movingTarget(output);
        animation(new File(output, "shocker_breaker_and_ex.gif"), true);
        animation(new File(output, "chaos_buster_and_blaster.gif"), false);
        System.out.println("Explosive beam production-mesh previews exported to " + output.getAbsolutePath());
    }

    private static Mesh barrageMesh(float castAge, int detail) {
        Mesh mesh = new Mesh();
        // Stationary enemies deliberately stress overlapping columns from all five marking rounds.
        double[][] targets = {{-20,0},{20,0},{0,30}};
        for (double[] target : targets) for (int round = 0; round < 5; round++) {
            float age = castAge - round * MARK_INTERVAL;
            appendExMarker(mesh,age,target,BARRAGE,detail);
        }
        mesh.finish();
        return mesh;
    }

    private static void barrage(File output) throws Exception {
        BufferedImage image = blank(1300,1000);
        Graphics2D g = graphics(image);
        label(g,"Shocker Breaker EX - three stationary targets",24,35,24,TEXT);
        int shownTick = ShockerBreakerVisualTiming.CHARGE_TICKS + 2 * MARK_INTERVAL + 2;
        label(g,String.format("Five marking rounds; one brief pillar per warning | shown %.1f seconds into the cast",shownTick/20.0),24,63,16,MUTED);
        g.drawImage(render(barrageMesh(shownTick,0),BARRAGE,new double[]{-90,-50,90,78 * VISUAL_GROWTH},1280,900),10,83,null);
        g.dispose();
        ImageIO.write(image,"png",new File(output,"shocker_ex_barrage_overlap.png"));
        for (int detail=0;detail<3;detail++) {
            int peak=0,peakTick=0;
            for (int tick=0;tick<=LAST_MARK_TICK+ShockerBreakerVisualTiming.lifetime(true);tick++) {
                int quads=barrageMesh(tick,detail).faces.size();
                if (quads>peak) { peak=quads;peakTick=tick; }
            }
            System.out.println("EX 15-pillar schedule, detail "+detail+": peak "+peak+" quads at cast tick "+peakTick+" (includes red warnings).");
        }
    }

    private static void appendExMarker(Mesh mesh,float age,double[] target,Camera camera,int detail) {
        if (age<0 || age>=ShockerBreakerVisualTiming.lifetime(true)) return;
        AsrielVisualGeometry.Sink offset = (x,y,z,r,g,b,a) -> mesh.vertex(x+target[0],y,z+target[1],r,g,b,a);
        if (age<ShockerBreakerVisualTiming.CHARGE_TICKS) {
            ShockerBreakerWarningVisuals.ground(offset,age,12);
            AsrielVisualGeometry.Sink billboard = (x,y,z,r,g,b,a) -> {
                double s=12/4.5;
                offset.vertex(s*(camera.right[0]*x+camera.up[0]*y),
                        .25+s*(camera.right[1]*x+camera.up[1]*y),
                        s*(camera.right[2]*x+camera.up[2]*y),r,g,b,a);
            };
            ShockerBreakerWarningVisuals.icon(billboard,age);
        } else {
            double scale = ShockerBreakerDimensions.VISUAL_SCALE;
            AsrielVisualGeometry.Sink scaled=(x,y,z,r,g,b,a)->offset.vertex(x*scale,y*scale,z*scale,r,g,b,a);
            ShockerBreakerVisuals.render(scaled,age,4,true,detail);
        }
    }

    private static BufferedImage trailFrame(float tick,int width,int height) {
        Mesh mesh=new Mesh();
        for(int round=0;round<5;round++) appendExMarker(mesh,tick-round*MARK_INTERVAL,TRAIL_MARKS[round],TRAIL,0);
        mesh.finish();
        BufferedImage image=render(mesh,TRAIL,TRAIL_BOUNDS,width,height);
        Graphics2D g=graphics(image);
        for(int round=0;round<5 && tick>=round*MARK_INTERVAL;round++) {
            double[] p=project(TRAIL_MARKS[round][0],0,TRAIL_MARKS[round][1],TRAIL,TRAIL_BOUNDS,width,height);
            float age=tick-round*MARK_INTERVAL;
            Color color=age<ShockerBreakerVisualTiming.CHARGE_TICKS?new Color(255,91,96)
                    :age<ShockerBreakerVisualTiming.lifetime(true)?TEXT:new Color(81,100,125);
            g.setColor(color);g.fillOval((int)p[0]-3,(int)p[1]-3,6,6);
            label(g,Integer.toString(round+1),(int)p[0]-5,(int)p[1]+25,16,color);
        }
        float progress=Math.max(0,Math.min(4,tick/MARK_INTERVAL));
        int from=Math.min(3,(int)progress);double f=progress-from;
        double x=TRAIL_MARKS[from][0]*(1-f)+TRAIL_MARKS[from+1][0]*f;
        double z=TRAIL_MARKS[from][1]*(1-f)+TRAIL_MARKS[from+1][1]*f;
        double[] current=project(x,0,z,TRAIL,TRAIL_BOUNDS,width,height);
        g.setColor(new Color(75,211,255));g.drawOval((int)current[0]-8,(int)current[1]-8,16,16);
        g.drawLine((int)current[0]-11,(int)current[1],(int)current[0]+11,(int)current[1]);
        g.dispose();return image;
    }

    private static double[] project(double x,double y,double z,Camera camera,double[] bounds,int width,int height) {
        double scale=Math.min((width-20)/(bounds[2]-bounds[0]),(height-20)/(bounds[3]-bounds[1]));
        double px=x*camera.right[0]+y*camera.right[1]+z*camera.right[2];
        double py=x*camera.up[0]+y*camera.up[1]+z*camera.up[2];
        return new double[]{(px-(bounds[0]+bounds[2])/2)*scale+width*.5,
                height*.5-(py-(bounds[1]+bounds[3])/2)*scale};
    }

    private static void movingTarget(File output)throws Exception {
        BufferedImage sheet=blank(1920,850);Graphics2D g=graphics(sheet);
        label(g,"Shocker Breaker EX - each pillar records a fresh location",24,36,27,TEXT);
        label(g,"Native warning boxes and single-strike effects | blue circle: moving target | gray dots: expired marks",24,65,16,MUTED);
        int warning=ShockerBreakerVisualTiming.CHARGE_TICKS;
        int[] ticks={MARK_INTERVAL+2,warning+2,warning+2*MARK_INTERVAL+2,warning+LAST_MARK_TICK+2};
        String[] phases={"Two independent warnings","First pillar strikes behind the target","Third pillar / fourth and fifth warnings","Fifth and final pillar"};
        for(int i=0;i<4;i++) {
            int x=i*480;
            label(g,String.format("%.1f seconds",ticks[i]/20.0),x+18,112,21,TEXT);
            label(g,phases[i],x+18,139,14,MUTED);
            g.drawImage(trailFrame(ticks[i],480,560),x,150,null);
        }
        label(g,"Mark times: "+roundTimes(0),24,761,19,TEXT);
        label(g,"Strike times: "+roundTimes(warning),24,794,19,TEXT);
        label(g,"Every red box stays at its recorded position; each column fires once, then fades.",650,778,17,MUTED);
        g.dispose();ImageIO.write(sheet,"png",new File(output,"shocker_ex_moving_target.png"));
        ImageWriter writer=ImageIO.getImageWritersBySuffix("gif").next();
        try(FileImageOutputStream stream=new FileImageOutputStream(new File(output,"shocker_ex_moving_target.gif"))) {
            writer.setOutput(stream);writer.prepareWriteSequence(null);ImageWriteParam params=writer.getDefaultWriteParam();
            for(int frame=0;frame*2<=LAST_MARK_TICK+ShockerBreakerVisualTiming.lifetime(true)+4;frame++) {
                int tick=frame*2;BufferedImage image=blank(1000,780);g=graphics(image);
                label(g,"Shocker Breaker EX - five separately marked locations",20,34,24,TEXT);
                label(g,String.format("%.1fs | one new mark every %.1fs | each pillar fires once",tick/20.0,MARK_INTERVAL/20.0),20,63,17,MUTED);
                g.drawImage(trailFrame(tick,1000,620),0,85,null);
                label(g,"Blue circle: current target location   |   red marks: waiting to strike",20,732,16,MUTED);
                label(g,"Numbers / gray dots show recorded locations for this preview.",20,757,15,MUTED);g.dispose();
                writeGifFrame(writer,params,image,frame,10);
            }
            writer.endWriteSequence();
        } finally {writer.dispose();}
    }

    private static String roundTimes(int delay) {
        StringBuilder text = new StringBuilder();
        for (int round = 0; round < ShockerBreakerExSequence.MARK_ROUNDS; round++) {
            if (round > 0) text.append(" / ");
            text.append(String.format("%.1fs", (delay + round * MARK_INTERVAL) / 20.0));
        }
        return text.toString();
    }

    private static Mesh shocker(float age, boolean enhanced) {
        if (age < ShockerBreakerVisualTiming.CHARGE_TICKS) return warning(age, enhanced, SHOCKER);
        Mesh mesh = new Mesh();
        double scale = ShockerBreakerDimensions.VISUAL_SCALE;
        AsrielVisualGeometry.Sink world = (x,y,z,r,g,b,a) -> mesh.vertex(x*scale,y*scale,z*scale,r,g,b,a);
        ShockerBreakerVisuals.render(world, age, ShockerBreakerDimensions.originalRadius(enhanced), enhanced, 0);
        mesh.finish();
        return mesh;
    }

    private static Mesh warning(float age, boolean enhanced, Camera camera) {
        Mesh mesh = new Mesh();
        double radius = enhanced ? 12 : 9;
        ShockerBreakerWarningVisuals.ground(mesh, age, (float)radius);
        AsrielVisualGeometry.Sink billboard = (x,y,z,r,g,b,a) -> {
            double scale = radius / 4.5;
            mesh.vertex(scale * (camera.right[0]*x + camera.up[0]*y),
                    .25 + scale * (camera.right[1]*x + camera.up[1]*y),
                    scale * (camera.right[2]*x + camera.up[2]*y),r,g,b,a);
        };
        ShockerBreakerWarningVisuals.icon(billboard,age);
        mesh.finish();
        return mesh;
    }

    private static Mesh beam(float age, boolean enhanced) {
        Mesh mesh = new Mesh();
        double baseRadius = enhanced ? 3.2 : ChaosBusterPattern.BEAM_RADIUS;
        float radius = (float)(baseRadius * ChaosBusterPattern.beamRadiusScale(age, ChaosBusterPattern.BEAM_LIFETIME));
        AsrielVisualGeometry.rainbowBeam(mesh, age, ChaosBusterPattern.BEAM_LIFETIME,
                (float)ChaosBusterPattern.BEAM_RANGE, radius, enhanced, 0);
        mesh.finish();
        return mesh;
    }

    private static Mesh chargingCannon(float age, boolean enhanced) {
        Mesh mesh = new Mesh();
        double spin = Math.toRadians(enhanced ? 0 : ChaosBusterPattern.cannonSpinDegrees(age));
        AsrielVisualGeometry.Sink rotating = (x,y,z,r,g,b,a) -> mesh.vertex(
                x * Math.cos(spin) - y * Math.sin(spin), x * Math.sin(spin) + y * Math.cos(spin), z,r,g,b,a);
        AsrielVisualGeometry.busterCharge(rotating, age, ChaosBusterPattern.chargeFraction(age, enhanced),
                (float)(enhanced ? 3.2 : ChaosBusterPattern.BEAM_RADIUS), enhanced, 0);
        mesh.finish();
        return mesh;
    }

    private static void cannonComparison(File output) throws Exception {
        BufferedImage sheet = blank(1920,1100);
        Graphics2D g = graphics(sheet);
        label(g,"Chaos Buster / Chaos Blaster - rebuilt cannon",24,36,27,TEXT);
        label(g,"Native meshes with additive light | identical framing across phases | actual charge timing",24,66,16,MUTED);
        String[] phases = {"Empty reactor", "Energy gathering", "Fully charged", "Firing"};
        for (int variant = 0; variant < 2; variant++) {
            boolean enhanced = variant == 1;
            float[] ages = enhanced ? new float[]{0,10,19.75F,2.0F}
                    : new float[]{140,153.3F,184,2.0F};
            for (int phase = 0; phase < 4; phase++) {
                int x = phase * 480, y = 94 + variant * 495;
                label(g,(enhanced ? "Chaos Blaster" : "Chaos Buster") + " | " + phases[phase], x+18,y+24,19,TEXT);
                Mesh mesh = phase == 3 ? beam(ages[phase],enhanced) : chargingCannon(ages[phase],enhanced);
                g.drawImage(render(mesh,CANNON,CANNON_BOUNDS,480,315),x,y+34,null);
                g.drawImage(render(mesh,CANNON_FRONT,CANNON_FRONT_BOUNDS,230,140),x+125,y+345,null);
                label(g,"Front view",x+18,y+431,13,MUTED);
            }
        }
        g.dispose();
        ImageIO.write(sheet,"png",new File(output,"chaos_cannon_comparison.png"));
        BufferedImage close = blank(1600,900);
        g = graphics(close);
        label(g,"Full-charge cannon silhouettes",24,36,26,TEXT);
        for (int variant=0;variant<2;variant++) {
            boolean enhanced=variant==1;int x=variant*800;
            Mesh mesh=chargingCannon(enhanced?19.75F:184,enhanced);
            label(g,enhanced?"Chaos Blaster":"Chaos Buster",x+24,80,24,TEXT);
            g.drawImage(render(mesh,CANNON,CANNON_BOUNDS,800,570),x,95,null);
            g.drawImage(render(mesh,CANNON_FRONT,CANNON_FRONT_BOUNDS,420,230),x+190,660,null);
        }
        g.dispose();
        ImageIO.write(close,"png",new File(output,"chaos_cannon_closeup.png"));
    }

    private static void cannonAnimation(File output) throws Exception {
        ImageWriter writer=ImageIO.getImageWritersBySuffix("gif").next();
        try(FileImageOutputStream stream=new FileImageOutputStream(new File(output,"chaos_cannon_charge_fire.gif"))) {
            writer.setOutput(stream);writer.prepareWriteSequence(null);
            ImageWriteParam params=writer.getDefaultWriteParam();
            int normalCharge=ChaosBusterPattern.BEAM_START_TICK-ChaosBusterPattern.CHARGE_START_TICK;
            for(int tick=0;tick<normalCharge+ChaosBusterPattern.BEAM_LIFETIME+8;tick++) {
                BufferedImage image=blank(1200,760);Graphics2D g=graphics(image);
                for(int variant=0;variant<2;variant++) {
                    boolean enhanced=variant==1;int x=variant*600;
                    int chargeEnd=enhanced?20:normalCharge;
                    boolean firing=tick>=chargeEnd;
                    Mesh mesh=firing?beam(tick-chargeEnd,enhanced)
                            :chargingCannon(enhanced?tick:ChaosBusterPattern.CHARGE_START_TICK+tick,enhanced);
                    label(g,enhanced?"Chaos Blaster":"Chaos Buster",x+22,34,24,TEXT);
                    label(g,String.format("%.2fs | %s",tick/20.0,firing?"Release and fade":"Charging"),x+22,63,16,MUTED);
                    g.drawImage(render(mesh,CANNON,CANNON_BOUNDS,600,420),x,75,null);
                    g.drawImage(render(mesh,BEAM,BEAM_BOUNDS,600,235),x,500,null);
                    label(g,"Cannon close view / full attack",x+22,744,14,MUTED);
                }
                g.dispose();writeGifFrame(writer,params,image,tick,5);
            }
            writer.endWriteSequence();
        } finally {writer.dispose();}
    }

    private static void contactSheet(File output) throws Exception {
        BufferedImage sheet = blank(1680, 1170);
        Graphics2D g = graphics(sheet);
        label(g, "Explosive beam effects", 24, 36, 27, TEXT);
        label(g, "Native game meshes | additive light | fixed camera framing", 24, 64, 15, MUTED);
        for (int variant = 0; variant < 2; variant++) {
            int x = variant * 840;
            boolean enhanced = variant == 1;
            Mesh shock = shocker(enhanced ? 42F : 41.5F, enhanced);
            label(g, enhanced ? "Shocker Breaker EX" : "Shocker Breaker", x + 22, 105, 23, TEXT);
            g.drawImage(render(shock, SHOCKER, SHOCK_BOUNDS, 330, 440), x + 8, 120, null);
            g.drawImage(render(shock, IMPACT, IMPACT_BOUNDS, 490, 410), x + 342, 145, null);
            label(g, "216-block pillar | unchanged hitbox", x + 24, 580, 15, MUTED);
            label(g, "Ground detonation / close view", x + 355, 580, 15, MUTED);

            Mesh chaos = beam(enhanced ? 4.5F : 3.5F, enhanced);
            label(g, enhanced ? "Chaos Blaster" : "Chaos Buster", x + 22, 640, 23, TEXT);
            g.drawImage(render(chaos, BEAM, BEAM_BOUNDS, 820, 260), x + 10, 654, null);
            label(g, "Muzzle detonation", x + 30, 946, 16, MUTED);
            label(g, "Endpoint detonation", x + 440, 946, 16, MUTED);
            g.drawImage(render(chaos, BEAM, MUZZLE_BOUNDS, 400, 195), x + 15, 954, null);
            g.drawImage(render(chaos, BEAM, ENDPOINT_BOUNDS, 400, 195), x + 425, 954, null);
        }
        g.setColor(new Color(37, 52, 73)); g.drawLine(840, 90, 840, 1140); g.drawLine(20, 604, 1660, 604);
        g.dispose();
        ImageIO.write(sheet, "png", new File(output, "explosive_beams_contact_sheet.png"));
        // Large single scenes retain enough detail to inspect silhouettes and the axial core.
        ImageIO.write(render(shocker(41.5F, false), IMPACT, IMPACT_BOUNDS, 1000, 800), "png", new File(output, "shocker_impact_detail.png"));
        ImageIO.write(render(beam(4.5F, true), BEAM, BEAM_BOUNDS, 1500, 650), "png", new File(output, "chaos_blaster_detail.png"));
    }

    private static double[] scaledBounds(double x0, double y0, double x1, double y1) {
        return new double[]{x0 * VISUAL_GROWTH, y0 * VISUAL_GROWTH, x1 * VISUAL_GROWTH, y1 * VISUAL_GROWTH};
    }

    private static double[] closeBounds(double z) {
        double x = BEAM.right[2] * z, y = BEAM.up[2] * z;
        return new double[]{x - 10.5, y - 9.5, x + 10.5, y + 9.5};
    }

    private static double[] closeBounds(double z, double padding) {
        double x = BEAM.right[2] * z, y = BEAM.up[2] * z;
        return new double[]{x - padding, y - padding, x + padding, y + padding};
    }

    private static double[] beamBounds() {
        double x = BEAM.right[2] * ChaosBusterPattern.BEAM_RANGE;
        double y = BEAM.up[2] * ChaosBusterPattern.BEAM_RANGE;
        double padding = ChaosBeamVisuals.cullingRadius(Math.max(3.2, ChaosBusterPattern.BEAM_RADIUS)) + 1;
        return new double[]{Math.min(0,x)-padding, Math.min(0,y)-padding,
                Math.max(0,x)+padding, Math.max(0,y)+padding};
    }

    private static void chaosFrames(File output) throws Exception {
        BufferedImage sheet = blank(1800,1300);
        Graphics2D g = graphics(sheet);
        label(g,String.format("Chaos beam motion - %.0f-block range | 2x beam width | %.1f-second firing duration",
                ChaosBusterPattern.BEAM_RANGE, ChaosBusterPattern.BEAM_LIFETIME / 20.0),22,34,25,TEXT);
        float[] ages = {0,3,26,ChaosBusterPattern.BEAM_LIFETIME-ChaosBusterPattern.BEAM_SHRINK_TICKS-1,
                ChaosBusterPattern.BEAM_LIFETIME-8,ChaosBusterPattern.BEAM_LIFETIME-1};
        for (int enhanced = 0; enhanced < 2; enhanced++) for (int frame = 0; frame < ages.length; frame++) {
            int x = frame % 3 * 600, y = 65 + enhanced * 610 + frame / 3 * 300;
            label(g,(enhanced == 0 ? "Chaos Buster" : "Chaos Blaster")+" | tick "+(int)ages[frame],x+18,y+25,18,TEXT);
            g.drawImage(render(beam(ages[frame],enhanced==1),BEAM,BEAM_BOUNDS,600,260),x,y+32,null);
        }
        g.dispose();
        ImageIO.write(sheet,"png",new File(output,"chaos_beam_motion_frames.png"));
    }

    private static void shockerFrames(File output) throws Exception {
        BufferedImage sheet = blank(1920, 1220);
        Graphics2D g = graphics(sheet);
        label(g,"Shocker Breaker - explosive rainbow sustain",22,35,27,TEXT);
        label(g,"Native meshes | original warnings and hitboxes | 216-block visual pillar",22,62,16,MUTED);
        int charge = ShockerBreakerVisualTiming.CHARGE_TICKS;
        int endOfSustain = charge + ShockerBreakerVisualTiming.STRIKE_TICKS;
        int lifetime = ShockerBreakerVisualTiming.LIFETIME;
        float[] ages = {charge-10,charge+2,charge+10,lifetime-20,endOfSustain-1,lifetime-3};
        String[] phases = {"Warning", "Initial impact", "Early sustain", "Late sustain", "End of sustain", "Fading"};
        for (int variant=0;variant<2;variant++) for (int frame=0;frame<ages.length;frame++) {
            boolean enhanced = variant == 1;
            int x=frame*320, y=90+variant*550;
            label(g,(enhanced ? "EX" : "Regular")+" | age "+(int)ages[frame],x+16,y+22,18,TEXT);
            label(g,phases[frame],x+16,y+45,15,MUTED);
            Mesh mesh = frame == 0 ? warning(ages[frame],enhanced,IMPACT) : shocker(ages[frame],enhanced);
            Camera camera = frame == 0 ? IMPACT : SHOCKER;
            double[] bounds = frame == 0 ? new double[]{-18,-12,18,25} : SHOCK_BOUNDS;
            g.drawImage(render(mesh,camera,bounds,320,480),x,y+55,null);
        }
        g.dispose();
        ImageIO.write(sheet,"png",new File(output,"shocker_rainbow_frames.png"));

        BufferedImage details = blank(1600,1080);
        g = graphics(details);
        label(g,"Shocker Breaker - ground impact and sustained discharge",22,35,26,TEXT);
        float[] detailAges = {charge+1.5F,lifetime-20,lifetime-3};
        for (int variant=0;variant<2;variant++) for (int frame=0;frame<detailAges.length;frame++) {
            int x=frame*530,y=65+variant*500;
            label(g,(variant==0 ? "Regular" : "EX")+" | age "+detailAges[frame],x+16,y+24,20,TEXT);
            g.drawImage(render(shocker(detailAges[frame],variant==1),IMPACT,IMPACT_BOUNDS,530,455),x,y+36,null);
        }
        g.dispose();
        ImageIO.write(details,"png",new File(output,"shocker_rainbow_detail.png"));
    }

    private static void animation(File output, boolean shock) throws Exception {
        ImageWriter writer = ImageIO.getImageWritersBySuffix("gif").next();
        try (FileImageOutputStream stream = new FileImageOutputStream(output)) {
            writer.setOutput(stream); writer.prepareWriteSequence(null);
            ImageWriteParam params = writer.getDefaultWriteParam();
            int count = shock ? ShockerBreakerVisualTiming.LIFETIME - 28 + 6 : ChaosBusterPattern.BEAM_LIFETIME + 8;
            for (int frame = 0; frame < count; frame++) {
                BufferedImage image = blank(1000, 580);
                Graphics2D g = graphics(image);
                for (int variant = 0; variant < 2; variant++) {
                    boolean enhanced = variant == 1;
                    int x = variant * 500;
                    float age = shock ? 28 + frame : frame;
                    Mesh mesh = shock ? shocker(age, enhanced) : beam(age, enhanced);
                    label(g, shock ? (enhanced ? "Shocker Breaker EX: one strike" : "Shocker Breaker")
                            : (enhanced ? "Chaos Blaster" : "Chaos Buster"), x + 17, 31, 21, TEXT);
                    if (shock) {
                        g.drawImage(render(mesh, SHOCKER, SHOCK_BOUNDS, 220, 480), x, 55, null);
                        g.drawImage(render(mesh, IMPACT, IMPACT_BOUNDS, 280, 380), x + 220, 135, null);
                        label(g, "Full pillar", x + 16, 556, 14, MUTED);
                        label(g, "Impact close view", x + 239, 556, 14, MUTED);
                    } else {
                        g.drawImage(render(mesh, BEAM, BEAM_BOUNDS, 500, 280), x, 70, null);
                        g.drawImage(render(mesh, BEAM, MUZZLE_BOUNDS, 245, 210), x + 3, 351, null);
                        g.drawImage(render(mesh, BEAM, ENDPOINT_BOUNDS, 245, 210), x + 252, 351, null);
                        label(g, "Muzzle", x + 17, 558, 14, MUTED);
                        label(g, "Endpoint", x + 267, 558, 14, MUTED);
                    }
                }
                g.dispose();
                writeGifFrame(writer,params,image,frame,5);
            }
            writer.endWriteSequence();
        } finally { writer.dispose(); }
    }

    private static void writeGifFrame(ImageWriter writer,ImageWriteParam params,BufferedImage image,int frame,int delay)throws Exception {
        IIOMetadata metadata=writer.getDefaultImageMetadata(ImageTypeSpecifier.createFromRenderedImage(image),params);
        String format=metadata.getNativeMetadataFormatName();
        IIOMetadataNode tree=new IIOMetadataNode(format),control=new IIOMetadataNode("GraphicControlExtension");
        control.setAttribute("disposalMethod","none");control.setAttribute("userInputFlag","FALSE");
        control.setAttribute("transparentColorFlag","FALSE");control.setAttribute("delayTime",Integer.toString(delay));
        control.setAttribute("transparentColorIndex","0");tree.appendChild(control);
        if(frame==0) {
            IIOMetadataNode extensions=new IIOMetadataNode("ApplicationExtensions"),loop=new IIOMetadataNode("ApplicationExtension");
            loop.setAttribute("applicationID","NETSCAPE");loop.setAttribute("authenticationCode","2.0");
            loop.setUserObject(new byte[]{1,0,0});extensions.appendChild(loop);tree.appendChild(extensions);
        }
        metadata.setFromTree(format,tree);writer.writeToSequence(new IIOImage(image,null,metadata),params);
    }

    private static BufferedImage render(Mesh mesh, Camera camera, double[] bounds, int width, int height) {
        BufferedImage image = blank(width, height);
        double scale = Math.min((width - 20) / (bounds[2] - bounds[0]), (height - 20) / (bounds[3] - bounds[1]));
        double cx = (bounds[0] + bounds[2]) / 2, cy = (bounds[1] + bounds[3]) / 2;
        for (Vertex[] face : mesh.faces) {
            double[][] p = new double[4][2];
            for (int i = 0; i < 4; i++) {
                p[i][0] = (dot(face[i], camera.right) - cx) * scale + width * .5;
                p[i][1] = height * .5 - (dot(face[i], camera.up) - cy) * scale;
            }
            triangle(image, p[0], p[1], p[2], face[0], face[1], face[2]);
            triangle(image, p[0], p[2], p[3], face[0], face[2], face[3]);
        }
        return image;
    }

    private static void triangle(BufferedImage image, double[] a, double[] b, double[] c, Vertex va, Vertex vb, Vertex vc) {
        double den = (b[1] - c[1]) * (a[0] - c[0]) + (c[0] - b[0]) * (a[1] - c[1]);
        if (Math.abs(den) < 1e-10) return;
        int x0 = Math.max(0, (int)Math.floor(Math.min(a[0], Math.min(b[0], c[0]))));
        int x1 = Math.min(image.getWidth() - 1, (int)Math.ceil(Math.max(a[0], Math.max(b[0], c[0]))));
        int y0 = Math.max(0, (int)Math.floor(Math.min(a[1], Math.min(b[1], c[1]))));
        int y1 = Math.min(image.getHeight() - 1, (int)Math.ceil(Math.max(a[1], Math.max(b[1], c[1]))));
        for (int y = y0; y <= y1; y++) for (int x = x0; x <= x1; x++) {
            double u = ((b[1] - c[1]) * (x + .5 - c[0]) + (c[0] - b[0]) * (y + .5 - c[1])) / den;
            double v = ((c[1] - a[1]) * (x + .5 - c[0]) + (a[0] - c[0]) * (y + .5 - c[1])) / den;
            if (u < 0 || v < 0 || u + v > 1) continue;
            double w = 1 - u - v, alpha = va.a * u + vb.a * v + vc.a * w;
            int prior = image.getRGB(x, y);
            int r = Math.min(255, (prior >> 16 & 255) + (int)((va.r * u + vb.r * v + vc.r * w) * alpha * 255));
            int g = Math.min(255, (prior >> 8 & 255) + (int)((va.g * u + vb.g * v + vc.g * w) * alpha * 255));
            int bl = Math.min(255, (prior & 255) + (int)((va.b * u + vb.b * v + vc.b * w) * alpha * 255));
            image.setRGB(x, y, 0xFF000000 | r << 16 | g << 8 | bl);
        }
    }

    private static BufferedImage blank(int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics(); g.setColor(new Color(BACKGROUND)); g.fillRect(0, 0, width, height); g.dispose();
        return image;
    }
    private static Graphics2D graphics(BufferedImage image) {
        Graphics2D g = image.createGraphics(); g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON); return g;
    }
    private static void label(Graphics2D g, String text, int x, int y, int size, Color color) {
        g.setFont(new Font("SansSerif", size >= 20 ? Font.BOLD : Font.PLAIN, size)); g.setColor(color); g.drawString(text, x, y);
    }
    private static double[] unit(double x, double y, double z) {
        double length = Math.sqrt(x*x + y*y + z*z); return new double[]{x/length, y/length, z/length};
    }
    private static double dot(Vertex v, double[] axis) { return v.x*axis[0] + v.y*axis[1] + v.z*axis[2]; }
    private static final class Camera {
        final double[] right, up;
        Camera(double x, double y, double z) {
            double[] f = unit(x,y,z); right = unit(f[2],0,-f[0]);
            up = new double[]{f[1]*right[2] - f[2]*right[1], f[2]*right[0] - f[0]*right[2], f[0]*right[1] - f[1]*right[0]};
        }
    }
    private static final class Vertex {
        final double x,y,z; final float r,g,b,a;
        Vertex(double x,double y,double z,float r,float g,float b,float a) { this.x=x;this.y=y;this.z=z;this.r=r;this.g=g;this.b=b;this.a=a; }
    }
    private static final class Mesh implements AsrielVisualGeometry.Sink {
        final List<Vertex[]> faces = new ArrayList<>(); final Vertex[] pending = new Vertex[4]; int index;
        public void vertex(double x,double y,double z,float r,float g,float b,float a) {
            if (!Double.isFinite(x+y+z) || !Float.isFinite(r+g+b+a) || a<0 || a>1.001F) throw new AssertionError("Invalid visual vertex");
            pending[index++] = new Vertex(x,y,z,r,g,b,a);
            if (index==4) { faces.add(pending.clone()); index=0; }
        }
        void finish() { if (index!=0) throw new AssertionError("Unfinished quad"); }
    }
}
