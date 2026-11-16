package net.minecraftforge.event;

import java.util.List;
import net.minecraft.util.ResourceLocation;

public final class RegistryEvent {
    public static final class MissingMappings<T> {
        private final List<Mapping<T>> mappings;
        public MissingMappings(List<Mapping<T>> mappings) { this.mappings = mappings; }
        public List<Mapping<T>> getAllMappings() { return mappings; }
        public static final class Mapping<T> {
            public final ResourceLocation key;
            public int ignores;
            public Mapping(ResourceLocation key) { this.key = key; }
            public void ignore() { ignores++; }
            public void remap(T target) { throw new AssertionError("Retired player IDs must never remap to boss abilities"); }
        }
    }
}
