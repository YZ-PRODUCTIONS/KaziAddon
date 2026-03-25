package net.kazi.kazimod.entities;

import net.kazi.kazimod.init.KaziLootTables;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ILivingEntityData;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.ai.attributes.AttributeModifierMap;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.IServerWorld;
import net.minecraft.world.World;
import net.minecraftforge.fml.loading.FMLPaths;
import xyz.pixelatedw.mineminenomi.api.entities.TraderEntity;
import xyz.pixelatedw.mineminenomi.api.enums.Currency;
import xyz.pixelatedw.mineminenomi.entities.mobs.OPEntity;
import xyz.pixelatedw.mineminenomi.init.ModAttributes;

public class VegapunkTraderEntity extends TraderEntity {

    public static final ResourceLocation[] TEXTURES = {
            new ResourceLocation("kazimod", "textures/entities/vegapunk_trader.png")
    };

    private static final int DESPAWN_TIME = 36000;
    private int spawnTimer = 0;
    private boolean timerStarted = false;

    public VegapunkTraderEntity(EntityType<? extends VegapunkTraderEntity> type, World world) {
        super(type, world, TEXTURES);
    }

    /**
     * 100 HP + hidden armor equivalent to full Prot 4 netherite:
     *
     *   ARMOR           = 20  — full netherite armour value (4+7+6+3 = 20)
     *   ARMOR_TOUGHNESS = 12  — full netherite toughness (3 per piece x 4 = 12)
     *   FAUX_PROTECTION = 20  — mineminenomi extra damage reduction;
     *                           mirrors EPF cap of Prot 4 on all 4 pieces
     *                           (EPF 5 x 4 = 20, capped at 20 = ~80% reduction)
     */
    public static AttributeModifierMap.MutableAttribute createAttributes() {
        return OPEntity.createAttributes()
                .add(Attributes.MAX_HEALTH,           300.0)
                .add(Attributes.MOVEMENT_SPEED,       0.2)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.ATTACK_DAMAGE,        1.0)
                .add(Attributes.ARMOR,                20.0)
                .add(Attributes.ARMOR_TOUGHNESS,      12.0)
                .add(ModAttributes.FAUX_PROTECTION.get(), 20.0);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
    }

    @Override
    public ILivingEntityData finalizeSpawn(IServerWorld world, DifficultyInstance difficulty,
                                           SpawnReason reason, ILivingEntityData spawnData,
                                           CompoundNBT dataTag) {
        if (isFruitChangesDisabled()) {
            this.remove();
            return null;
        }
        ILivingEntityData data = super.finalizeSpawn(world, difficulty, reason, spawnData, dataTag);
        if (reason == SpawnReason.COMMAND) {
            timerStarted = true;
            spawnTimer   = 0;
        }
        return data;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!level.isClientSide && isFruitChangesDisabled()) {
            this.remove();
            return;
        }
        if (!level.isClientSide && timerStarted) {
            spawnTimer++;
            if (spawnTimer >= DESPAWN_TIME) this.remove();
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundNBT nbt) {
        super.addAdditionalSaveData(nbt);
        nbt.putInt("SpawnTimer",       spawnTimer);
        nbt.putBoolean("TimerStarted", timerStarted);
    }

    @Override
    public void readAdditionalSaveData(CompoundNBT nbt) {
        super.readAdditionalSaveData(nbt);
        if (nbt.contains("SpawnTimer"))   spawnTimer   = nbt.getInt("SpawnTimer");
        if (nbt.contains("TimerStarted")) timerStarted = nbt.getBoolean("TimerStarted");
    }

    @Override public boolean canTrade(PlayerEntity player) { return true; }
    @Override public String  getTradeFailMessage()         { return "My research is not for everyone!"; }
    @Override public ResourceLocation getTradeTable()      { return KaziLootTables.VEGAPUNK_TRADER; }
    @Override public Currency getCurrency()                { return Currency.BELLY; }
    @Override public boolean removeWhenFarAway(double d)   { return false; }

    // ── Config cache ──────────────────────────────────────────────────────────

    private static Boolean cachedDisabled = null;

    private static boolean isFruitChangesDisabled() {
        if (cachedDisabled != null) return cachedDisabled;
        try {
            java.nio.file.Path configPath = FMLPaths.CONFIGDIR.get().resolve("kazimod.toml");
            if (!java.nio.file.Files.exists(configPath)) { cachedDisabled = false; return false; }
            for (String line : java.nio.file.Files.readAllLines(configPath)) {
                if (line.trim().startsWith("disableFruitChanges")) {
                    cachedDisabled = line.contains("true");
                    return cachedDisabled;
                }
            }
        } catch (Exception ignored) {}
        cachedDisabled = false;
        return false;
    }
}