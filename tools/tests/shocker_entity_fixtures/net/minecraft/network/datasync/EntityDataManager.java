package net.minecraft.network.datasync;
import java.util.HashMap;
import java.util.Map;
public final class EntityDataManager {
    private final Map<DataParameter<?>, Object> values = new HashMap<>();
    public static <T> DataParameter<T> defineId(Class<?> type, DataSerializer<T> serializer) { return new DataParameter<>(); }
    public <T> void define(DataParameter<T> key, T value) { values.put(key, value); }
    public <T> void set(DataParameter<T> key, T value) { values.put(key, value); }
    @SuppressWarnings("unchecked") public <T> T get(DataParameter<T> key) { return (T) values.get(key); }
}
