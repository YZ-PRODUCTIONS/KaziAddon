package net.kazi.kazimod.api;

import com.mojang.datafixers.types.Type;
import java.util.HashMap;
import java.util.function.Function;
import java.util.function.Supplier;
import javax.annotation.Nullable;

import net.minecraft.block.Block;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityClassification;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.attributes.Attribute;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.ContainerType;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.particles.ParticleType;
import net.minecraft.potion.Effect;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.tileentity.TileEntityType.Builder;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.feature.Feature;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.common.extensions.IForgeContainerType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.fml.network.IContainerFactory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import xyz.pixelatedw.mineminenomi.api.ModRegistries;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
import xyz.pixelatedw.mineminenomi.api.challenges.ChallengeCore;
import xyz.pixelatedw.mineminenomi.api.morph.MorphInfo;
import xyz.pixelatedw.mineminenomi.api.quests.Quest;
import xyz.pixelatedw.mineminenomi.api.quests.QuestId;
import xyz.pixelatedw.mineminenomi.particles.effects.ParticleEffect;
import xyz.pixelatedw.mineminenomi.wypi.WyHelper;

public class KaziRegistry {
    private static HashMap<String, String> langMap = new HashMap();
    public static final DeferredRegister<Item> ITEMS;
    public static final DeferredRegister<Block> BLOCKS;
    public static final DeferredRegister<ContainerType<?>> CONTAINER_TYPES;
    public static final DeferredRegister<AbilityCore<?>> ABILITIES;
    public static final DeferredRegister<Effect> EFFECTS;
    public static final DeferredRegister<Enchantment> ENCHANTMENTS;
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES;
    public static final DeferredRegister<TileEntityType<?>> TILE_ENTITIES;
    public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES;
    public static final DeferredRegister<QuestId<?>> QUESTS;
    public static final DeferredRegister<Feature<?>> FEATURES;
    public static final DeferredRegister<SoundEvent> SOUNDS;
    public static final DeferredRegister<Attribute> ATTRIBUTES;
    public static final DeferredRegister<ChallengeCore<?>> CHALLENGES;
    public static final DeferredRegister<ParticleEffect<?>> PARTICLE_EFFECTS;
    public static final DeferredRegister<MorphInfo> MORPHS;
    public static final DeferredRegister<Biome> BIOMES;

    public static HashMap<String, String> getLangMap() {
        return langMap;
    }

    public static String registerName(String key, String localizedName) {
        getLangMap().put(key, localizedName);
        return key;
    }

    public static TranslationTextComponent registerTextComponent(String key, String localizedName) {
        return new TranslationTextComponent(registerName(key, localizedName));
    }

    public static <T extends MorphInfo> RegistryObject<T> registerMorph(String resourceName, Supplier<T> morph) {
        RegistryObject<T> reg = MORPHS.register(resourceName, morph);
        return reg;
    }

    public static <T extends Biome> RegistryObject<T> registerBiome(String localizedName, Supplier<T> biome) {
        String resourceName = WyHelper.getResourceName(localizedName);
        getLangMap().put("biome.kazimod." + resourceName, localizedName);
        RegistryObject<T> reg = BIOMES.register(resourceName, biome);
        return reg;
    }

    public static <T extends ParticleEffect<?>> RegistryObject<T> registerParticleEffect(String localizedName, Supplier<T> supplier) {
        String resourceName = WyHelper.getResourceName(localizedName);
        getLangMap().put("particle_effect.kazimod." + resourceName, localizedName);
        RegistryObject<T> reg = PARTICLE_EFFECTS.register(resourceName, supplier);
        return reg;
    }



    public static RegistryObject<Attribute> registerAttribute(String localizedName, Supplier<Attribute> attr) {
        String resourceName = WyHelper.getResourceName(localizedName);
        getLangMap().put("attribute.name.generic.kazimod." + resourceName, localizedName);
        RegistryObject<Attribute> reg = ATTRIBUTES.register("generic." + resourceName, attr);
        return reg;
    }

    public static <T extends ParticleType<?>> RegistryObject<T> registerParticleType(String localizedName, Supplier<T> type) {
        String resourceName = WyHelper.getResourceName(localizedName);
        RegistryObject<T> reg = PARTICLE_TYPES.register(resourceName, type);
        return reg;
    }

    public static <T extends Feature<?>> RegistryObject<T> registerFeature(String localizedName, Supplier<T> feature) {
        String resourceName = WyHelper.getResourceName(localizedName);
        RegistryObject<T> reg = FEATURES.register(resourceName, feature);
        return reg;
    }

    public static <T extends Effect> RegistryObject<T> registerEffect(String localizedName, Supplier<T> effect) {
        String resourceName = WyHelper.getResourceName(localizedName);
        return registerEffect(localizedName, resourceName, effect);
    }

    public static <T extends Effect> RegistryObject<T> registerEffect(String localizedName, String resourceKey, Supplier<T> effect) {
        String resourceName = WyHelper.getResourceName(resourceKey);
        getLangMap().put("effect.kazimod." + resourceName, localizedName);
        RegistryObject<T> reg = EFFECTS.register(resourceName, effect);
        return reg;
    }

    public static <T extends Enchantment> RegistryObject<T> registerEnchantment(String localizedName, Supplier<T> enchantment) {
        String resourceName = WyHelper.getResourceName(localizedName);
        getLangMap().put("enchantment.kazimod." + resourceName, localizedName);
        RegistryObject<T> reg = ENCHANTMENTS.register(resourceName, enchantment);
        return reg;
    }

    public static <T extends Quest> QuestId<T> registerQuest(QuestId<T> quest) {
        String resourceName = WyHelper.getResourceName(quest.getName());
        getLangMap().put("quest.kazimod." + resourceName, quest.getName());
        ResourceLocation key = new ResourceLocation("kazimod", resourceName);
        RegistryObject<QuestId<?>> ret = RegistryObject.of(key, ModRegistries.QUESTS);
        if (!QUESTS.getEntries().contains(ret)) {
            QUESTS.register(resourceName, () -> quest);
        }

        return quest;
    }

    public static <T extends IAbility> AbilityCore<T> registerAbility(AbilityCore<T> core) {
        String resourceName = WyHelper.getResourceName(core.getId());
        ResourceLocation key = new ResourceLocation("kazimod", resourceName);
        getLangMap().put("ability.kazimod." + resourceName, core.getUnlocalizedName());
        RegistryObject<AbilityCore<?>> ret = RegistryObject.of(key, KaziRegistries.ABILITIES);
        if (!ABILITIES.getEntries().contains(ret)) {
            ABILITIES.register(resourceName, () -> core);
            if (core.getIcon() == null) {
                core.setIcon(new ResourceLocation(key.getNamespace(), "textures/abilities/" + key.getPath() + ".png"));
            }
        }

        return core;
    }

    public static RegistryObject<SoundEvent> registerSound(String localizedName) {
        String resourceName = WyHelper.getResourceName(localizedName);
        getLangMap().put("kazimod.subtitle." + resourceName, localizedName);
        SoundEvent sound = new SoundEvent(new ResourceLocation("kazimod", resourceName));
        RegistryObject<SoundEvent> reg = SOUNDS.register(resourceName, () -> sound);
        return reg;
    }

    public static <T extends Item> RegistryObject<T> registerItem(String localizedName, Supplier<T> item) {
        String resourceName = WyHelper.getResourceName(localizedName);
        getLangMap().put("item.kazimod." + resourceName, localizedName);
        RegistryObject<T> reg = ITEMS.register(resourceName, item);
        return reg;
    }

    public static RegistryObject<ForgeSpawnEggItem> registerSpawnEggItem(String localizedEntityName, Supplier<ForgeSpawnEggItem> supp) {
        String entityResName = WyHelper.getResourceName(localizedEntityName);
        String resourceName = entityResName + "_spawn_egg";
        String localizedName = "Spawn " + localizedEntityName;
        getLangMap().put("item.kazimod." + resourceName, localizedName);
        RegistryObject<ForgeSpawnEggItem> reg = ITEMS.register(resourceName, supp);
        return reg;
    }

    public static <T extends Block> RegistryObject<T> registerBlock(String localizedName, Supplier<T> block) {
        return registerBlock(localizedName, block, (ItemGroup)null);
    }

    public static <T extends Block> RegistryObject<T> registerBlock(String localizedName, Supplier<T> block, @Nullable ItemGroup tab) {
        Item.Properties blockItemProps = new Item.Properties();
        if (tab != null) {
            blockItemProps.tab(tab);
        }

        return registerBlock(localizedName, block, blockItemProps);
    }

    public static <T extends Block> RegistryObject<T> registerBlock(String localizedName, Supplier<T> block, Item.Properties props) {
        String resourceName = WyHelper.getResourceName(localizedName);
        getLangMap().put("block.kazimod." + resourceName, localizedName);
        RegistryObject<T> reg = BLOCKS.register(resourceName, block);
        registerItem(localizedName, () -> new BlockItem((Block)reg.get(), props));
        return reg;
    }

    public static <T extends Block> RegistryObject<T> registerBlock(String localizedName, Supplier<T> block, Function<T, BlockItem> blockItemFunc) {
        String resourceName = WyHelper.getResourceName(localizedName);
        getLangMap().put("block.kazimod." + resourceName, localizedName);
        RegistryObject<T> reg = BLOCKS.register(resourceName, block);
        registerItem(localizedName, () -> (BlockItem)blockItemFunc.apply(reg.get()));
        return reg;
    }

    public static <T extends Block> RegistryObject<T> registerBlockOnly(String localizedName, Supplier<T> block) {
        String resourceName = WyHelper.getResourceName(localizedName);
        getLangMap().put("block.kazimod." + resourceName, localizedName);
        RegistryObject<T> reg = BLOCKS.register(resourceName, block);
        return reg;
    }

    public static <C extends Container> RegistryObject<ContainerType<C>> registerContainer(String localizedName, IContainerFactory<C> containerFactory) {
        String resourceName = WyHelper.getResourceName(localizedName);
        getLangMap().put("container.kazimod." + resourceName, localizedName);
        RegistryObject<ContainerType<C>> reg = CONTAINER_TYPES.register(resourceName, () -> IForgeContainerType.create(containerFactory));
        return reg;
    }

    public static RegistryObject<TileEntityType<?>> registerTileEntity(String localizedName, Supplier<TileEntity> factory, Block... blocks) {
        String resourceName = WyHelper.getResourceName(localizedName);
        TileEntityType<?> type = Builder.of(factory, blocks).build((Type)null);
        RegistryObject<TileEntityType<?>> reg = TILE_ENTITIES.register(resourceName, () -> type);
        return reg;
    }

    public static <T extends Entity> EntityType.Builder<T> createFastEntityType(EntityType.IFactory<T> factory) {
        EntityType.Builder<T> builder = net.minecraft.entity.EntityType.Builder.of(factory, EntityClassification.MISC);
        builder.setTrackingRange(128).setShouldReceiveVelocityUpdates(true).setUpdateInterval(3).sized(0.6F, 1.8F);
        return builder;
    }

    public static <T extends Entity> EntityType.Builder createEntityType(EntityType.IFactory<T> factory) {
        return createEntityType(factory, EntityClassification.MISC);
    }

    public static <T extends Entity> EntityType.Builder createEntityType(EntityType.IFactory<T> factory, EntityClassification classification) {
        EntityType.Builder<T> builder = net.minecraft.entity.EntityType.Builder.of(factory, classification);
        builder.setTrackingRange(10).setShouldReceiveVelocityUpdates(true).setUpdateInterval(1).sized(0.6F, 1.8F);
        return builder;
    }

    public static <T extends Entity> RegistryObject<EntityType<T>> registerEntityType(String localizedName, Supplier<EntityType<T>> supp) {
        String resourceName = WyHelper.getResourceName(localizedName);
        getLangMap().put("entity.kazimod." + resourceName, localizedName);
        RegistryObject<EntityType<T>> reg = ENTITY_TYPES.register(resourceName, supp);
        return reg;
    }

    public static <T extends Entity> RegistryObject<EntityType<T>> registerEntityType(String localizedName, String resourceName, Supplier<EntityType<T>> supp) {
        getLangMap().put("entity.kazimod." + resourceName, localizedName);
        RegistryObject<EntityType<T>> reg = ENTITY_TYPES.register(resourceName, supp);
        return reg;
    }

    public static <T extends Entity> void registerEntityType(EntityType<T> type, String localizedName) {
        String resourceName = WyHelper.getResourceName(localizedName);
        getLangMap().put("entity.kazimod." + resourceName, localizedName);
    }

    static {
        ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, "kazimod");
        BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, "kazimod");
        CONTAINER_TYPES = DeferredRegister.create(ForgeRegistries.CONTAINERS, "kazimod");
        ABILITIES = DeferredRegister.create(ModRegistries.ABILITIES, "kazimod");
        EFFECTS = DeferredRegister.create(ForgeRegistries.POTIONS, "kazimod");
        ENCHANTMENTS = DeferredRegister.create(ForgeRegistries.ENCHANTMENTS, "kazimod");
        ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.ENTITIES, "kazimod");
        TILE_ENTITIES = DeferredRegister.create(ForgeRegistries.TILE_ENTITIES, "kazimod");
        PARTICLE_TYPES = DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, "kazimod");
        QUESTS = DeferredRegister.create(ModRegistries.QUESTS, "kazimod");
        FEATURES = DeferredRegister.create(ForgeRegistries.FEATURES, "kazimod");
        SOUNDS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, "kazimod");
        ATTRIBUTES = DeferredRegister.create(ForgeRegistries.ATTRIBUTES, "kazimod");
        CHALLENGES = DeferredRegister.create(ModRegistries.CHALLENGES, "kazimod");
        PARTICLE_EFFECTS = DeferredRegister.create(ModRegistries.PARTICLE_EFFECTS, "kazimod");
        MORPHS = DeferredRegister.create(ModRegistries.MORPHS, "kazimod");
        BIOMES = DeferredRegister.create(ForgeRegistries.BIOMES, "kazimod");
    }
}
