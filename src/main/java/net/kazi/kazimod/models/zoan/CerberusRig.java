package net.kazi.kazimod.models.zoan;

import com.google.gson.Gson;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

/** Baked per-face UV geometry and animation data exported from the user's Blockbench file. */
public final class CerberusRig {
    public String sourceHash;
    public Node[] nodes;
    public Clip[] animations;
    public static final class Node {
        public String name;
        public float[] origin, rotation;
        public boolean visible;
        public Node[] children;
        public Quad[] quads;
    }
    public static final class Quad { public float[][] vertices; public float[] normal; }
    public static final class Clip { public String name; public float length; public boolean loop; public Track[] tracks; }
    public static final class Track { public String bone; public Key[] keys; }
    public static final class Key { public String channel; public float time; public float[] value; }
    public static final class Pose {
        public final float[] rotation = new float[3], position = new float[3];
    }
    public static CerberusRig read(Reader reader) { return new Gson().fromJson(reader, CerberusRig.class); }
    public Clip clip(String name) {
        for (Clip clip : animations) if (clip.name.equals(name)) return clip;
        throw new IllegalArgumentException("Unknown Cerberus animation: " + name);
    }
    public Map<String, Pose> pose() { return new HashMap<>(); }
    public List<Node> path(String bone) {
        List<Node> result = new ArrayList<>();
        if (!path(nodes, bone, result)) throw new IllegalArgumentException("Unknown Cerberus bone: " + bone);
        return result;
    }
    private static boolean path(Node[] nodes, String bone, List<Node> result) {
        for (Node node : nodes) if (node.children != null) {
            result.add(node);
            if (node.name.equals(bone) || path(node.children, bone, result)) return true;
            result.remove(result.size()-1);
        }
        return false;
    }
    public void apply(Map<String, Pose> pose, String name, float time, float weight) {
        if (weight <= 0) return;
        Clip clip = clip(name);
        time = clip.loop ? ((time % clip.length) + clip.length) % clip.length : Math.max(0, Math.min(time, clip.length));
        for (Track track : clip.tracks) {
            Pose p = pose.computeIfAbsent(track.bone, k -> new Pose());
            sample(track, "rotation", time, weight, p.rotation);
            sample(track, "position", time, weight, p.position);
        }
    }
    private static void sample(Track track, String channel, float time, float weight, float[] target) {
        Key first = null, last = null, before = null, after = null;
        for (Key key : track.keys) if (key.channel.equals(channel)) {
            if (first == null || key.time < first.time) first = key;
            if (last == null || key.time > last.time) last = key;
            if (key.time <= time && (before == null || key.time > before.time)) before = key;
            if (key.time >= time && (after == null || key.time < after.time)) after = key;
        }
        if (first == null) return;
        if (before == null) before = first;
        if (after == null) after = last;
        float t = after.time == before.time ? 0 : (time - before.time) / (after.time - before.time);
        for (int i = 0; i < 3; i++) target[i] += weight * (before.value[i] + (after.value[i] - before.value[i]) * t);
    }
}
