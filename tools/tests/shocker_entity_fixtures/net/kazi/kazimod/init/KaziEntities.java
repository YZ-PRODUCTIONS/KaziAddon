package net.kazi.kazimod.init;
import net.kazi.kazimod.entities.asriel.AsrielLightningEntity;
import net.minecraft.entity.EntityType;
public final class KaziEntities {
    public static final LightningType ASRIEL_LIGHTNING = new LightningType();
    public static final class LightningType {
        public EntityType<AsrielLightningEntity> get() { return new EntityType<>(); }
    }
}
