package net.minecraft.util.math;
public final class MathHelper { public static float clamp(float value, float low, float high) { return Math.max(low, Math.min(high, value)); } }
