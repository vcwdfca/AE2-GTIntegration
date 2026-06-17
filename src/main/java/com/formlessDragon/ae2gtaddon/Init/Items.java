package com.formlessDragon.ae2gtaddon.Init;

import ae2.api.upgrades.Upgrades;
import ae2.core.definitions.ItemDefinition;
import com.formlessDragon.ae2gtaddon.Tags;
import com.formlessDragon.ae2gtaddon.ids.ItemIds;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@Mod.EventBusSubscriber(modid = Tags.MOD_ID)
public final class Items {

    public static final CreativeTabs TABS = new CreativeTabs(Tags.MOD_ID) {

        @Override
        public ItemStack createIcon() {
            return CIRCUIT_CARD.stack(1);
        }
    };

    public static final ItemDefinition<Item> CIRCUIT_CARD = new ItemDefinition<>(ItemIds.CIRCUIT_CARD,
            Upgrades.createUpgradeCardItem(), TABS);

    private static final ItemDefinition<?>[] ITEMS = {
        CIRCUIT_CARD
    };

    private Items() {

    }

    @SubscribeEvent
    public static void register(RegistryEvent.Register<Item> event) {
        for(ItemDefinition<?> definition : ITEMS) {
            event.getRegistry().register(definition.item());
        }
    }

    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public static void registerModels(ModelRegistryEvent event) {
        for(ItemDefinition<?> definition : ITEMS) {
            Item item = definition.item();
            if(item != null) {
                ModelLoader.setCustomModelResourceLocation(item, 0, new ModelResourceLocation(definition.id(), "inventory"));
            }
        }
    }
}
