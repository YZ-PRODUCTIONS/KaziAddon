package net.kazi.kazimod.abilities.Nusu;

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.nbt.StringNBT;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.storage.WorldSavedData;
import xyz.pixelatedw.mineminenomi.api.ModRegistries;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;

import java.util.ArrayList;
import java.util.List;

/**
 * Server-side world saved data that tracks which abilities are globally stolen
 * by any Nusu user. Keyed by ability ResourceLocation, independent of which
 * player originally had the ability. This survives victim death and fruit respawn.
 */
public class NusuWorldData extends WorldSavedData {

    private static final String DATA_NAME   = "nusu_stolen_global";
    private static final String KEY_STOLEN  = "globally_stolen";

    private final List<ResourceLocation> globallyStolen = new ArrayList<>();

    public NusuWorldData() {
        super(DATA_NAME);
    }

    public static NusuWorldData get(ServerWorld world) {
        return world.getDataStorage().computeIfAbsent(NusuWorldData::new, DATA_NAME);
    }

    public void markStolen(AbilityCore<?> core) {
        ResourceLocation rl = ModRegistries.ABILITIES.getKey(core);
        if (rl == null) return;
        if (!globallyStolen.contains(rl)) {
            globallyStolen.add(rl);
            setDirty();
        }
    }

    public void unmarkStolen(AbilityCore<?> core) {
        ResourceLocation rl = ModRegistries.ABILITIES.getKey(core);
        if (rl == null) return;
        if (globallyStolen.remove(rl)) {
            setDirty();
        }
    }

    public boolean isStolen(AbilityCore<?> core) {
        ResourceLocation rl = ModRegistries.ABILITIES.getKey(core);
        return rl != null && globallyStolen.contains(rl);
    }

    @Override
    public void load(CompoundNBT nbt) {
        globallyStolen.clear();
        ListNBT list = nbt.getList(KEY_STOLEN, 8); // 8 = StringNBT
        for (int i = 0; i < list.size(); i++) {
            try {
                globallyStolen.add(new ResourceLocation(list.getString(i)));
            } catch (Exception ignored) {}
        }
    }

    @Override
    public CompoundNBT save(CompoundNBT nbt) {
        ListNBT list = new ListNBT();
        for (ResourceLocation rl : globallyStolen) {
            list.add(StringNBT.valueOf(rl.toString()));
        }
        nbt.put(KEY_STOLEN, list);
        return nbt;
    }
}