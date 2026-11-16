import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.kazi.kazimod.events.RetiredNagiAbilities;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.RegistryEvent.MissingMappings;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;

/** Exercises the real retirement handler; Forge's registry plumbing is the only fixture. */
public final class NagiLegacyMappingsTest {
    private static int checks;
    public static void main(String[] args) {
        String[] retired = {"silent_stride", "silent_slice", "silent_step", "silent_box", "silent_death", "imagine_breaker"};
        List<MissingMappings.Mapping<AbilityCore<?>>> mappings = new ArrayList<>();
        for (String path : retired) {
            ResourceLocation id = new ResourceLocation("kazimod", path);
            check(RetiredNagiAbilities.isRetired(id), "Original ID must retire: " + path);
            mappings.add(new MissingMappings.Mapping<>(id));
        }
        String[][] keep = {
                {"mineminenomi", "silent"}, {"mineminenomi", "silent_box"}, {"cartaddon", "silent_stride"},
                {"cartaddon", "imagine_breaker"}, {"kazimod", "imagine_breaker_extra"}, {"kazimod", "illusion_shatter"},
                {"kazimod", "combat_premonition"}, {"kazimod", "dragon_strike"},
                {"kazimod", "silent_step_extra"}, {"kazimod", "sun_jin_woo_silent_stride"},
                {"kazimod", "sun_jin_woo_silent_slice"}, {"kazimod", "sun_jin_woo_silent_step"},
                {"kazimod", "sun_jin_woo_silent_box"}, {"kazimod", "sun_jin_woo_silent_death"}
        };
        for (String[] parts : keep) {
            ResourceLocation id = new ResourceLocation(parts[0], parts[1]);
            check(!RetiredNagiAbilities.isRetired(id), "Unrelated/current/boss ID must survive: " + parts[0] + ":" + parts[1]);
            mappings.add(new MissingMappings.Mapping<>(id));
        }
        check(!RetiredNagiAbilities.isRetired(null), "Null ID is safe");
        RetiredNagiAbilities.onMissingMappings(new MissingMappings<>(mappings));
        for (int i = 0; i < mappings.size(); i++) {
            check(mappings.get(i).ignores == (i < retired.length ? 1 : 0), "Mixed saved registry must ignore only old Nagi entry " + i);
        }
        RetiredNagiAbilities.onMissingMappings(new MissingMappings<>(Collections.emptyList()));
        check(true, "Empty registry is safe");
        System.out.println("Nagi retirement mapping behavior: " + checks + " checks passed.");
    }
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
        checks++;
    }
}
