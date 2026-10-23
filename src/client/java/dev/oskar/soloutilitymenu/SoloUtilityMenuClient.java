package dev.oskar.soloutilitymenu;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public class SoloUtilityMenuClient implements ClientModInitializer {
	private static KeyMapping openMenuKey;

	@Override
	public void onInitializeClient() {
		KeyMapping.Category category = KeyMapping.Category.register(
				SoloUtilityMenu.id("controls")
		);
		openMenuKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
				"key.solo_utility_menu.open_menu",
				InputConstants.KEY_RSHIFT,
				category
		));

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (openMenuKey.consumeClick()) {
				if (client.player == null) {
					continue;
				}
				client.setScreenAndShow(new SoloUtilityScreen());
			}
		});
	}
}