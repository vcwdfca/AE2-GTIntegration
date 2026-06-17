package com.formlessDragon.ae2gtaddon.proxy;

import ae2.api.upgrades.Upgrades;
import ae2.core.definitions.AEBlocks;
import ae2.core.definitions.AEParts;
import ae2.core.localization.GuiText;
import com.formlessDragon.ae2gtaddon.Init.Items;

public class CommonProxy implements IProxy {

    @Override
    public void init() {
        String patternProviderGroup = GuiText.CraftingInterface.getTranslationKey();
        Upgrades.add(Items.CIRCUIT_CARD.item(), AEBlocks.PATTERN_PROVIDER.item(), 1, patternProviderGroup);
        Upgrades.add(Items.CIRCUIT_CARD.item(), AEParts.PATTERN_PROVIDER.item(), 1, patternProviderGroup);
    }
}
