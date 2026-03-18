package net.kazi.kazimod.abilities.Nusu;

import net.minecraft.entity.LivingEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.ResourceLocation;
import xyz.pixelatedw.mineminenomi.api.ModRegistries;
import xyz.pixelatedw.mineminenomi.api.abilities.Ability;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
import xyz.pixelatedw.mineminenomi.data.entity.ability.AbilityDataCapability;
import xyz.pixelatedw.mineminenomi.data.entity.ability.IAbilityData;

import javax.annotation.Nullable;

/**
 * Shared base for the three stolen-ability storage slots.
 *
 * When a steal succeeds, {@link #setStolenAbility(AbilityCore)} records the stolen core.
 * On use, the actual stolen ability's own use-logic fires exactly as if the user owned it.
 * The stolen core is persisted via IAbility.save / IAbility.load (CompoundNBT).
 */
public abstract class StolenAbilityBase extends Ability {

    private static final String NBT_STOLEN_KEY = "nusu_stolen_ability";

    /** The core of the stolen ability; null when nothing has been stolen yet. */
    @Nullable
    private AbilityCore<?> stolenCore = null;

    // ── constructor ──────────────────────────────────────────────────────────
    protected StolenAbilityBase(AbilityCore<? extends StolenAbilityBase> core) {
        super(core);
        this.isNew = true;
        // IOnUse2Event: onUse(LivingEntity, IAbility) -> void
        this.addUseEvent(this::onUseStolenAbility);
    }

    // ── public API ───────────────────────────────────────────────────────────
    public void setStolenAbility(AbilityCore<?> stolen) {
        this.stolenCore = stolen;
    }

    @Nullable
    public AbilityCore<?> getStolenCore() {
        return this.stolenCore;
    }

    public boolean hasStolen() {
        return this.stolenCore != null;
    }

    // ── use delegation ───────────────────────────────────────────────────────
    private void onUseStolenAbility(LivingEntity user, IAbility thisAbility) {
        if (this.stolenCore == null) return;

        IAbilityData data = AbilityDataCapability.get(user);
        if (data == null) return;

        // Get or create a live instance of the stolen ability kept as a passive
        IAbility stolenInstance = data.getPassiveAbility(this.stolenCore);
        if (stolenInstance == null) {
            // IFactory.create(AbilityCore) -> IAbility; cast to concrete type via raw call
            stolenInstance = createInstance(this.stolenCore);
            data.addPassiveAbility(stolenInstance);
        }

        // Delegate – call IAbility.use(LivingEntity) which triggers all internal use-events
        stolenInstance.use(user);
    }

    /**
     * Wraps the raw IFactory call to avoid the unchecked generic mismatch.
     * IFactory<A> create(AbilityCore<A>) → A; we call it via the raw interface.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static IAbility createInstance(AbilityCore<?> core) {
        AbilityCore.IFactory factory = core.getFactory();
        return (IAbility) factory.create(core);
    }

    // ── NBT persistence (IAbility.save / IAbility.load) ──────────────────────
    @Override
    public CompoundNBT save(CompoundNBT nbt) {
        nbt = super.save(nbt);
        if (this.stolenCore != null) {
            ResourceLocation key = ModRegistries.ABILITIES.getKey(this.stolenCore);
            if (key != null) {
                nbt.putString(NBT_STOLEN_KEY, key.toString());
            }
        }
        return nbt;
    }

    @Override
    public void load(CompoundNBT nbt) {
        super.load(nbt);
        if (nbt.contains(NBT_STOLEN_KEY)) {
            ResourceLocation loc = new ResourceLocation(nbt.getString(NBT_STOLEN_KEY));
            this.stolenCore = ModRegistries.ABILITIES.getValue(loc);
        }
    }
}