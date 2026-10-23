package dev.oskar.soloutilitymenu;

import java.util.ArrayList;
import java.util.Locale;
import java.util.UUID;
import java.util.List;
import java.util.function.BiConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.PermissionSet;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.level.GameType;

public class SoloUtilityScreen extends Screen {
	private static final int CONTENT_TOP = 176;
	private static final int ROW_HEIGHT = 24;
	private static final int PANEL_COLOR = 0xF0141D20;
	private static final int ACCENT_COLOR = 0xFF75D69B;
	private static final int CONTENT_COLOR = 0xFFCEDBD6;
	private static final int WARNING_COLOR = 0xFFFFC26F;

	private enum Page {
		PLAYER,
		EFFECTS,
		ITEMS,
		WORLD
	}

	private record IconPlacement(String icon, int x, int y, int size) {
	}

	private Page page = Page.PLAYER;
	private final List<IconPlacement> buttonIcons = new ArrayList<>();
	private EditBox itemIdField;
	private EditBox itemCountField;
	private EditBox effectIdField;
	private EditBox effectLevelField;
	private String rememberedItemId = "minecraft:diamond";
	private String rememberedItemCount = "64";
	private String rememberedEffectId = "minecraft:speed";
	private String rememberedEffectLevel = "1";
	private int panelLeft;
	private int panelWidth;
	private int scrollRow;
	private int contentRows;

	public SoloUtilityScreen() {
		super(Component.literal("Solo Utility Menu"));
	}

	@Override
	protected void init() {
		buttonIcons.clear();
		panelWidth = Math.min(520, this.width - 24);
		panelLeft = (this.width - panelWidth) / 2;
		contentRows = 0;
		int tabGap = 6;
		int tabWidth = (panelWidth - tabGap * 3) / 4;
		addTab("Player", Page.PLAYER, panelLeft, tabWidth);
		addTab("Effects", Page.EFFECTS, panelLeft + tabWidth + tabGap, tabWidth);
		addTab("Items", Page.ITEMS, panelLeft + (tabWidth + tabGap) * 2, tabWidth);
		addTab("World", Page.WORLD, panelLeft + (tabWidth + tabGap) * 3, tabWidth);

		switch (page) {
			case PLAYER -> initPlayerPage();
			case EFFECTS -> initEffectsPage();
			case ITEMS -> initItemsPage();
			case WORLD -> initWorldPage();
		}
		addButton("Close", panelLeft, this.height - 28, panelWidth, this::onClose);
	}

	@Override
	public void extractBackground(net.minecraft.client.gui.GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		super.extractBackground(graphics, mouseX, mouseY, partialTick);
		graphics.fill(0, 0, width, height, 0xA6091012);
		graphics.fill(panelLeft - 10, 28, panelLeft + panelWidth + 10, height - 18, PANEL_COLOR);
		graphics.fill(panelLeft - 10, 28, panelLeft + panelWidth + 10, 31, ACCENT_COLOR);
		graphics.outline(panelLeft - 10, 28, panelWidth + 20, height - 46, 0xFF344A43);
		graphics.fill(panelLeft, 84, panelLeft + panelWidth, 85, 0xFF344A43);
	}

	@Override
	public void extractRenderState(net.minecraft.client.gui.GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		super.extractRenderState(graphics, mouseX, mouseY, partialTick);
		graphics.text(font, "SOLO UTILITY", panelLeft, 36, 0xFFFFFFFF);
		String status = Minecraft.getInstance().getSingleplayerServer() == null ? "LOCAL WORLD ONLY" : "LOCAL WORLD ACTIVE";
		int statusColor = Minecraft.getInstance().getSingleplayerServer() == null ? WARNING_COLOR : ACCENT_COLOR;
		graphics.text(font, status, panelLeft + panelWidth - font.width(status), 37, statusColor);
		graphics.text(font, pageTitle(), panelLeft + 34, 92, ACCENT_COLOR);
		graphics.text(font, pageSubtitle(), panelLeft + 34, 107, CONTENT_COLOR);
		drawIcon(graphics, pageIcon(), panelLeft, 90, 26);

		int tabGap = 6;
		int tabWidth = (panelWidth - tabGap * 3) / 4;
		drawIcon(graphics, "player", panelLeft + 8, 57, 18);
		drawIcon(graphics, "effects", panelLeft + tabWidth + tabGap + 8, 57, 18);
		drawIcon(graphics, "items", panelLeft + (tabWidth + tabGap) * 2 + 8, 57, 18);
		drawIcon(graphics, "world", panelLeft + (tabWidth + tabGap) * 3 + 8, 57, 18);
		for (IconPlacement placement : buttonIcons) {
			drawIcon(graphics, placement.icon(), placement.x(), placement.y(), placement.size());
		}

		int maxScroll = Math.max(0, contentRows - visibleRows());
		if (maxScroll > 0) {
			int trackX = panelLeft + panelWidth + 3;
			int trackTop = CONTENT_TOP;
			int trackHeight = Math.max(24, height - CONTENT_TOP - 57);
			int thumbHeight = Math.max(20, trackHeight * visibleRows() / contentRows);
			int thumbTop = trackTop + (trackHeight - thumbHeight) * scrollRow / maxScroll;
			graphics.fill(trackX, trackTop, trackX + 3, trackTop + trackHeight, 0xFF34423E);
			graphics.fill(trackX, thumbTop, trackX + 3, thumbTop + thumbHeight, ACCENT_COLOR);
		}
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
		int maxScroll = Math.max(0, contentRows - visibleRows());
		if (maxScroll > 0 && mouseX >= panelLeft && mouseX <= panelLeft + panelWidth + 8 && mouseY >= CONTENT_TOP - 8 && mouseY < height - 40) {
			rememberInputValues();
			scrollRow = Math.max(0, Math.min(maxScroll, scrollRow - (int) Math.signum(verticalAmount)));
			init(width, height);
			restoreInputValues();
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
	}

	private void addTab(String label, Page targetPage, int x, int tabWidth) {
		Button tab = Button.builder(Component.literal(label), button -> selectPage(targetPage))
				.bounds(x, 50, tabWidth, 32)
				.build();
		tab.active = page != targetPage;
		addRenderableWidget(tab);
	}

	private int visibleRows() {
		return Math.max(3, (height - CONTENT_TOP - 48) / ROW_HEIGHT);
	}

	private int gridColumns() {
		return panelWidth >= 460 ? 5 : panelWidth >= 360 ? 4 : 3;
	}

	private String pageTitle() {
		return switch (page) {
			case PLAYER -> "PLAYER TOOLS";
			case EFFECTS -> "INFINITE EFFECTS";
			case ITEMS -> "ITEMS AND LOADOUTS";
			case WORLD -> "WORLD CONTROLS";
		};
	}

	private String pageSubtitle() {
		return switch (page) {
			case PLAYER -> "Health, abilities, game mode, and inventory";
			case EFFECTS -> "Select any preset or enter a namespaced effect ID";
			case ITEMS -> "Quick supplies, themed kits, and custom item IDs";
			case WORLD -> "Time, weather, difficulty, and gamerules";
		};
	}

	private String pageIcon() {
		return switch (page) {
			case PLAYER -> "player";
			case EFFECTS -> "effects";
			case ITEMS -> "items";
			case WORLD -> "world";
		};
	}

	private void drawIcon(net.minecraft.client.gui.GuiGraphicsExtractor graphics, String name, int x, int y, int size) {
		Identifier texture = Identifier.fromNamespaceAndPath(SoloUtilityMenu.MOD_ID, "textures/gui/" + name + ".png");
		graphics.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, 0.0F, 0.0F, size, size, size, size);
	}

	private void rememberInputValues() {
		if (itemIdField != null) rememberedItemId = itemIdField.getValue();
		if (itemCountField != null) rememberedItemCount = itemCountField.getValue();
		if (effectIdField != null) rememberedEffectId = effectIdField.getValue();
		if (effectLevelField != null) rememberedEffectLevel = effectLevelField.getValue();
	}

	private void restoreInputValues() {
		if (itemIdField != null) itemIdField.setValue(rememberedItemId);
		if (itemCountField != null) itemCountField.setValue(rememberedItemCount);
		if (effectIdField != null) effectIdField.setValue(rememberedEffectId);
		if (effectLevelField != null) effectLevelField.setValue(rememberedEffectLevel);
	}

	private void initPlayerPage() {
		int columns = gridColumns();
		String[] labels = {
				"God Mode", "Survival Flight", "Heal", "Feed", "Reset Fall",
				"Survival", "Creative", "Adventure", "Spectator", "Clear Effects",
				"+100 XP", "+1000 XP", "+10 Levels", "+100 Levels", "Set My Spawn",
				"Set World Spawn", "Repair Held", "Repair All Gear", "Clear Inventory", "Clear Drops"
		};
		Runnable[] actions = {
				this::toggleInvulnerability, this::toggleFlight, this::heal, this::feed, this::resetFall,
				() -> runCommand("gamemode survival"), () -> runCommand("gamemode creative"),
				() -> runCommand("gamemode adventure"), () -> runCommand("gamemode spectator"),
				() -> runCommand("effect clear @s"),
				() -> runOnIntegratedServer((server, player) -> player.giveExperiencePoints(100)),
				() -> runOnIntegratedServer((server, player) -> player.giveExperiencePoints(1000)),
				() -> runOnIntegratedServer((server, player) -> player.giveExperienceLevels(10)),
				() -> runOnIntegratedServer((server, player) -> player.giveExperienceLevels(100)),
				() -> runCommand("spawnpoint @s ~ ~ ~"), () -> runCommand("setworldspawn ~ ~ ~"),
				this::repairHeldItem, this::repairAllGear, () -> runCommand("clear @s"), () -> runCommand("kill @e[type=item,distance=..64]")
		};
		contentRows = (labels.length + columns - 1) / columns;
		for (int index = 0; index < labels.length; index++) {
			addScrollableGridButton(labels[index], index, columns, CONTENT_TOP, actions[index]);
		}
	}

	private void initEffectsPage() {
		int levelWidth = 58;
		int fieldGap = 6;
		int effectWidth = panelWidth - levelWidth - fieldGap;
		effectIdField = addRenderableWidget(new EditBox(font, panelLeft, 126, effectWidth, 20, Component.literal("Effect ID")));
		effectIdField.setMaxLength(128);
		effectIdField.setValue(rememberedEffectId);
		effectIdField.setHint(Component.literal("minecraft:speed"));
		effectLevelField = addRenderableWidget(new EditBox(font, panelLeft + effectWidth + fieldGap, 126, levelWidth, 20, Component.literal("Level")));
		effectLevelField.setMaxLength(3);
		effectLevelField.setValue(rememberedEffectLevel);

		int halfWidth = (panelWidth - fieldGap) / 2;
		addButton("Apply Forever", panelLeft, 151, halfWidth, this::applyInfiniteEffect);
		addButton("Clear All Effects", panelLeft + halfWidth + fieldGap, 151, halfWidth, () -> runCommand("effect clear @s"));

		String[] effects = {
				"Speed", "Slowness", "Haste", "Mining Fatigue", "Strength",
				"Instant Health", "Instant Damage", "Jump Boost", "Nausea", "Regeneration",
				"Resistance", "Fire Resistance", "Water Breathing", "Invisibility", "Blindness",
				"Night Vision", "Hunger", "Weakness", "Poison", "Wither",
				"Health Boost", "Absorption", "Saturation", "Glowing", "Levitation",
				"Luck", "Bad Omen", "Slow Falling", "Conduit Power", "Dolphin's Grace",
				"Hero of the Village", "Darkness"
		};
		String[] effectIds = {
				"speed", "slowness", "haste", "mining_fatigue", "strength",
				"instant_health", "instant_damage", "jump_boost", "nausea", "regeneration",
				"resistance", "fire_resistance", "water_breathing", "invisibility", "blindness",
				"night_vision", "hunger", "weakness", "poison", "wither",
				"health_boost", "absorption", "saturation", "glowing", "levitation",
				"luck", "bad_omen", "slow_falling", "conduit_power", "dolphins_grace",
				"hero_of_the_village", "darkness"
		};
		int columns = gridColumns();
		contentRows = (effects.length + columns - 1) / columns;
		for (int index = 0; index < effects.length; index++) {
			String id = effectIds[index];
			addScrollableGridButton(effects[index], index, columns, CONTENT_TOP, () -> runCommand("effect give @s minecraft:" + id + " infinite 0 true"));
		}
	}

	private void initItemsPage() {
		int countWidth = 58;
		int fieldGap = 6;
		int itemWidth = panelWidth - countWidth - fieldGap;
		itemIdField = addRenderableWidget(new EditBox(font, panelLeft, 126, itemWidth, 20, Component.literal("Item ID")));
		itemIdField.setMaxLength(128);
		itemIdField.setValue(rememberedItemId);
		itemCountField = addRenderableWidget(new EditBox(font, panelLeft + itemWidth + fieldGap, 126, countWidth, 20, Component.literal("Count")));
		itemCountField.setMaxLength(3);
		itemCountField.setValue(rememberedItemCount);
		addButton("Give Item", panelLeft, 151, panelWidth, this::giveItem);

		String[] labels = {
				"Diamonds", "Netherite", "Golden Apples", "Iron Blocks", "Obsidian",
				"Rockets", "Steak", "Ender Pearls", "Arrows", "XP Bottles",
				"Torches", "Oak Planks", "Emeralds", "Ender Chest", "Shield",
				"Golden Carrots", "Water Bucket", "Cobblestone", "Anvils", "Totem"
		};
		String[] itemIds = {
				"diamond", "netherite_ingot", "enchanted_golden_apple", "iron_block", "obsidian",
				"firework_rocket", "cooked_beef", "ender_pearl", "arrow", "experience_bottle",
				"torch", "oak_planks", "emerald", "ender_chest", "shield",
				"golden_carrot", "water_bucket", "cobblestone", "anvil", "totem_of_undying"
		};
		int columns = gridColumns();
		contentRows = (labels.length + 6 + columns - 1) / columns;
		for (int index = 0; index < labels.length; index++) {
			String id = itemIds[index];
			addScrollableGridButton(labels[index], index, columns, CONTENT_TOP, () -> runCommand("give @s minecraft:" + id + " 64"));
		}
		int kitIndex = labels.length;
		addScrollableGridButton("Starter Kit", kitIndex, columns, CONTENT_TOP, () -> runCommands(
				"give @s minecraft:iron_sword", "give @s minecraft:iron_pickaxe", "give @s minecraft:cooked_beef 32", "give @s minecraft:torch 32"));
		addScrollableGridButton("Builder Kit", kitIndex + 1, columns, CONTENT_TOP, () -> runCommands(
				"give @s minecraft:stone 64", "give @s minecraft:oak_planks 64", "give @s minecraft:glass 64", "give @s minecraft:torch 64"));
		addScrollableGridButton("Mining Kit", kitIndex + 2, columns, CONTENT_TOP, () -> runCommands(
				"give @s minecraft:diamond_pickaxe", "give @s minecraft:torch 64", "give @s minecraft:cooked_beef 64", "give @s minecraft:water_bucket"));
		addScrollableGridButton("Travel Kit", kitIndex + 3, columns, CONTENT_TOP, () -> runCommands(
				"give @s minecraft:elytra", "give @s minecraft:firework_rocket 64", "give @s minecraft:ender_pearl 16"));
		addScrollableGridButton("Nether Kit", kitIndex + 4, columns, CONTENT_TOP, () -> runCommands(
				"give @s minecraft:obsidian 64", "give @s minecraft:flint_and_steel", "give @s minecraft:golden_apple 16", "effect give @s minecraft:fire_resistance 600 0 true"));
		addScrollableGridButton("Combat Kit", kitIndex + 5, columns, CONTENT_TOP, () -> runCommands(
				"give @s minecraft:diamond_sword", "give @s minecraft:shield", "give @s minecraft:golden_apple 16", "give @s minecraft:arrow 64"));
	}

	private void initWorldPage() {
		String[] labels = {
				"Day", "Noon", "Sunset", "Midnight",
				"Night", "Dawn", "Freeze Time", "Resume Time",
				"Clear Weather", "Rain", "Thunder", "Peaceful",
				"Easy", "Normal", "Hard", "Keep Inventory On",
				"Keep Inventory Off", "Mob Spawning Off", "Mob Spawning On", "Mob Griefing Off",
				"Mob Griefing On", "Fire Spread Off", "Fire Spread On", "Weather Cycle Off",
				"Weather Cycle On", "Day Cycle Off", "Day Cycle On", "Random Ticks 0",
				"Random Ticks 3", "Set World Spawn", "Set My Spawn"
		};
		String[] commands = {
				"time set day", "time set noon", "time set 12000", "time set midnight",
				"time set night", "time set sunrise", "gamerule doDaylightCycle false", "gamerule doDaylightCycle true",
				"weather clear", "weather rain", "weather thunder", "difficulty peaceful",
				"difficulty easy", "difficulty normal", "difficulty hard", "gamerule keepInventory true",
				"gamerule keepInventory false", "gamerule doMobSpawning false", "gamerule doMobSpawning true", "gamerule mobGriefing false",
				"gamerule mobGriefing true", "gamerule doFireTick false", "gamerule doFireTick true", "gamerule doWeatherCycle false",
				"gamerule doWeatherCycle true", "gamerule doDaylightCycle false", "gamerule doDaylightCycle true", "gamerule randomTickSpeed 0",
				"gamerule randomTickSpeed 3", "setworldspawn ~ ~ ~", "spawnpoint @s ~ ~ ~"
		};
		int columns = gridColumns();
		contentRows = (labels.length + columns - 1) / columns;
		for (int index = 0; index < labels.length; index++) {
			String command = commands[index];
			addScrollableGridButton(labels[index], index, columns, CONTENT_TOP, () -> runCommand(command));
		}
	}

	private void addButton(String label, int x, int y, int buttonWidth, Runnable action) {
		addRenderableWidget(Button.builder(Component.literal(label), button -> action.run())
				.bounds(x, y, buttonWidth, 20)
				.build());
		String icon = buttonIcon(label);
		if (icon != null && font.width(label) + 24 <= buttonWidth) {
			buttonIcons.add(new IconPlacement(icon, x + buttonWidth - 20, y + 2, 16));
		}
	}

	private String buttonIcon(String label) {
		if (label.equals("God Mode") || label.equals("Shield")) return "shield";
		if (label.equals("Heal") || label.equals("Feed") || label.equals("Totem") || label.equals("Golden Apples")) return "heart";
		if (label.contains("XP") || label.contains("Level")) return "lightning";
		if (label.startsWith("Repair") || label.equals("Reset Fall")) return "tools";
		if (label.contains("Spawn")) return "compass";
		if (label.equals("Survival Flight")) return "player";
		if (label.equals("Day") || label.equals("Noon") || label.equals("Sunset") || label.equals("Midnight") || label.equals("Night") || label.equals("Dawn")) return "clock";
		if (label.contains("Weather") || label.equals("Rain") || label.equals("Thunder")) return "world";
		if (label.endsWith("Kit") || label.equals("Diamonds") || label.equals("Netherite") || label.equals("Iron Blocks") || label.equals("Obsidian")) return "items";
		return null;
	}

	private void addGridButton(String label, int index, int columns, int startY, Runnable action) {
		int gap = 5;
		int buttonWidth = (panelWidth - gap * (columns - 1)) / columns;
		int x = panelLeft + (index % columns) * (buttonWidth + gap);
		int y = startY + (index / columns) * 24;
		addButton(label, x, y, buttonWidth, action);
	}

	private void addScrollableGridButton(String label, int index, int columns, int startY, Runnable action) {
		int row = index / columns;
		if (row >= scrollRow && row < scrollRow + visibleRows()) {
			addGridButton(label, index - scrollRow * columns, columns, startY, action);
		}
	}

	private void selectPage(Page selectedPage) {
		rememberInputValues();
		page = selectedPage;
		scrollRow = 0;
		init(width, height);
		restoreInputValues();
	}

	private void runCommand(String command) {
		runOnIntegratedServer((server, player) -> server.getCommands().performPrefixedCommand(
				player.createCommandSourceStack()
						.withMaximumPermission(PermissionSet.ALL_PERMISSIONS)
						.withPermission(PermissionSet.ALL_PERMISSIONS),
				command
		));
	}

	private void runCommands(String... commands) {
		runOnIntegratedServer((server, player) -> {
			var source = player.createCommandSourceStack()
					.withMaximumPermission(PermissionSet.ALL_PERMISSIONS)
					.withPermission(PermissionSet.ALL_PERMISSIONS);
			for (String command : commands) {
				server.getCommands().performPrefixedCommand(source, command);
			}
		});
	}

	private void heal() {
		runOnIntegratedServer((server, player) -> player.setHealth(player.getMaxHealth()));
	}

	private void feed() {
		runOnIntegratedServer((server, player) -> {
			player.getFoodData().setFoodLevel(20);
			player.getFoodData().setSaturation(20);
		});
	}

	private void resetFall() {
		runOnIntegratedServer((server, player) -> {
			player.resetFallDistance();
			player.clearFire();
		});
	}

	private void toggleInvulnerability() {
		runOnIntegratedServer((server, player) -> {
			Abilities abilities = player.getAbilities();
			abilities.invulnerable = !abilities.invulnerable;
			player.onUpdateAbilities();
		});
	}

	private void toggleFlight() {
		runOnIntegratedServer((server, player) -> {
			if (player.gameMode() != GameType.SURVIVAL) {
				return;
			}
			Abilities abilities = player.getAbilities();
			abilities.mayfly = !abilities.mayfly;
			abilities.flying = abilities.mayfly;
			player.onUpdateAbilities();
		});
	}

	private void giveItem() {
		String itemId = itemIdField.getValue().trim().toLowerCase(Locale.ROOT);
		String count = itemCountField.getValue().trim();
		if (!itemId.matches("[a-z0-9_.-]+:[a-z0-9_./-]+")) {
			itemId = "minecraft:" + itemId;
		}
		if (!itemId.matches("[a-z0-9_.-]+:[a-z0-9_./-]+") || !count.matches("[1-9][0-9]?") || Integer.parseInt(count) > 64) {
			return;
		}
		runCommand("give @s " + itemId + " " + count);
	}

	private void applyInfiniteEffect() {
		String effectId = effectIdField.getValue().trim().toLowerCase(Locale.ROOT);
		String level = effectLevelField.getValue().trim();
		if (!effectId.matches("[a-z0-9_.-]+:[a-z0-9_./-]+")) {
			effectId = "minecraft:" + effectId;
		}
		if (!effectId.matches("[a-z0-9_.-]+:[a-z0-9_./-]+") || !level.matches("[1-9][0-9]{0,2}") || Integer.parseInt(level) > 256) {
			return;
		}
		int amplifier = Integer.parseInt(level) - 1;
		runCommand("effect give @s " + effectId + " infinite " + amplifier + " true");
	}

	private void repairHeldItem() {
		runOnIntegratedServer((server, player) -> {
			if (!player.getMainHandItem().isEmpty()) {
				player.getMainHandItem().setDamageValue(0);
				player.inventoryMenu.broadcastChanges();
			}
		});
	}

	private void repairAllGear() {
		runOnIntegratedServer((server, player) -> {
			for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
				ItemStack stack = player.getInventory().getItem(slot);
				if (stack.isDamageableItem()) {
					stack.setDamageValue(0);
				}
			}
			player.inventoryMenu.broadcastChanges();
		});
	}

	private void runOnIntegratedServer(BiConsumer<MinecraftServer, ServerPlayer> action) {
		Minecraft client = Minecraft.getInstance();
		MinecraftServer server = client.getSingleplayerServer();
		if (server == null || client.player == null) {
			return;
		}

		UUID playerId = client.player.getUUID();
		server.execute(() -> {
			ServerPlayer serverPlayer = server.getPlayerList().getPlayer(playerId);
			if (serverPlayer != null) {
				action.accept(server, serverPlayer);
			}
		});
	}
}