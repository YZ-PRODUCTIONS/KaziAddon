package net.minecraft.util;

public final class ResourceLocation {
    private final String namespace;
    private final String path;
    public ResourceLocation(String namespace, String path) {
        this.namespace = namespace;
        this.path = path;
    }
    public String getNamespace() { return namespace; }
    public String getPath() { return path; }
}
