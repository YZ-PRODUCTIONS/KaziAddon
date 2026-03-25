package net.kazi.kazimod.mixin;

import net.minecraftforge.fml.loading.FMLPaths;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

public class KaziMixinPlugin implements IMixinConfigPlugin {

    private static Boolean cachedDisabled = null;

    private static boolean isFruitChangesDisabled() {
        if (cachedDisabled != null) return cachedDisabled;
        try {
            Path configPath = FMLPaths.CONFIGDIR.get().resolve("kazimod.toml");
            if (!Files.exists(configPath)) {
                cachedDisabled = false;
                return false;
            }
            for (String line : Files.readAllLines(configPath)) {
                if (line.trim().startsWith("disableFruitChanges")) {
                    cachedDisabled = line.contains("true");
                    return cachedDisabled;
                }
            }
        } catch (Exception e) {
            // Can't read file, default to enabled
        }
        cachedDisabled = false;
        return false;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (mixinClassName.endsWith("AkumaNoMiBoxMixin")) {
            return !isFruitChangesDisabled();
        }
        return true;
    }

    @Override public void onLoad(String mixinPackage) {}
    @Override public String getRefMapperConfig() { return null; }
    @Override public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}
    @Override public List<String> getMixins() { return null; }
    @Override public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}
    @Override public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}
}