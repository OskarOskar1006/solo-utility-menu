# Solo Utility Menu

A Java Fabric mod for Minecraft 26.3. The menu opens with Right Shift by default; rebind it under Options > Controls > Key Binds.

## Features

- Browse player, effects, items, and world tools in a scrollable tabbed menu.
- Use the supplied PNG icons for tab navigation, page headings, and compact action shortcuts.
- Apply 32 infinite-duration effect presets, including negative effects, or enter a custom effect ID and level.
- Toggle invulnerability and survival flight; heal, feed, extinguish, reset fall distance, and grant experience.
- Change game mode, set spawn points, repair held items or all gear, clear inventory, or remove nearby item drops.
- Give items by namespaced ID, use 20 quick-item presets, or grant one of six themed kits.
- Change time, weather, difficulty, and multiple world gamerules.

Every action requires an active integrated single-player server. The menu does not modify multiplayer clients or send actions to remote servers.

## Requirements

- JDK 25
- Minecraft 26.3

## Build and run

From PowerShell in this project directory, run `.\gradlew.bat runClient` to launch the Fabric development client, or `.\gradlew.bat build` to create the mod jar in `build/libs`. The wrapper downloads and uses Gradle 9.8.2.

Install the built jar and Fabric API in the `mods` folder of a Minecraft 26.3 Fabric profile to try it in a normal single-player world.