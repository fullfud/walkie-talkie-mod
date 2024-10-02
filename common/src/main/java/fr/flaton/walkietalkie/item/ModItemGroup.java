package fr.flaton.walkietalkie.item;

import dev.architectury.registry.CreativeTabRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

public class ModItemGroup {

    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create("walkietalkie", Registries.CREATIVE_MODE_TAB);

    public static final RegistrySupplier<CreativeModeTab> WALKIETALKIE = TABS.register(
            "walkietalkie",
            () -> CreativeTabRegistry.create(
                    Component.translatable("itemGroup.walkietalkie.walkietalkie"),
                    () -> new ItemStack(ModItems.WOODEN_WALKIETALKIE.get())
            )
    );

    public static void register() {
        TABS.register();
    }
}
