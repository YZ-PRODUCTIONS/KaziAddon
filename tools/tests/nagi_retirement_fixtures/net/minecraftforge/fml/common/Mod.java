package net.minecraftforge.fml.common;
public @interface Mod {
    @interface EventBusSubscriber {
        String modid();
        Bus bus();
        enum Bus { FORGE, MOD }
    }
}
