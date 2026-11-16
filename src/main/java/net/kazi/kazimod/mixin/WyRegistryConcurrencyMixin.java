package net.kazi.kazimod.mixin;

import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import xyz.pixelatedw.mineminenomi.wypi.WyRegistry;

import java.util.function.Supplier;

/** Protect MMNM's shared registration queues when addons initialize in parallel. */
@Mixin(value = WyRegistry.class, remap = false)
public abstract class WyRegistryConcurrencyMixin {
    // Forge 1.16 stores pending entries in an unsynchronized LinkedHashMap.
    // Cart and MMNM both enqueue fruit items through this class during mod
    // construction. Concurrent insertion can silently lose an entry (e.g. Moku).
    // Cover every enqueue here, including block items, not only registerItem.
    // Lock only the enqueue: suppliers must remain deferred, and class
    // initialization outside it must not run under a shared registry lock.
    @Redirect(method = "*", at = @At(value = "INVOKE", target =
            "Lnet/minecraftforge/registries/DeferredRegister;register(Ljava/lang/String;Ljava/util/function/Supplier;)Lnet/minecraftforge/fml/RegistryObject;"),
            remap = false, require = 1)
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static RegistryObject kazimod$enqueueSafely(DeferredRegister registry,
                                                       String name, Supplier supplier) {
        synchronized (registry) {
            return registry.register(name, supplier);
        }
    }
}
