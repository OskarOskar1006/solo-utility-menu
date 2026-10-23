package dev.oskar.soloutilitymenu;

import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.Identifier;

public class SoloUtilityMenu implements ModInitializer {
	public static final String MOD_ID = "solo_utility_menu";

	@Override
	public void onInitialize() {
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}