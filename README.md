# Orehall

A modular Vanilla+ enhancement mod for Minecraft 1.21.1 built on NeoForge, focused on balanced gameplay tweaks, quality-of-life mechanics, and modpack-friendly recipe and durability customization.

## What is this?
Orehall provides a collection of independent, toggleable gameplay adjustments and recipe enhancements designed to feel right at home in modern vanilla and modded Minecraft. Every feature is encapsulated in a dedicated module that can be individually enabled, disabled, or fine-tuned.

## Features

### Gameplay Tweaks
- **Mob Milking Cooldowns**: Adds configurable cooldowns to milking cows, mooshrooms, and goats to prevent infinite bucket spam, complete with custom audio cues and angry particle feedback on refusal.
- **Goat Mutton Drops**: Goats drop mutton upon death, matching sheep.
- **Death-Persistent Recovery Compasses**: Recovery compasses stay with the player upon death, safely bypassing gravestone, corpse, and loot-capture mods while still respecting the Curse of Vanishing.
- **Automated Durability Cataloging**: Automatically scans all items across vanilla and loaded mods on startup and catalogs their durability into `config/orehall/durability.toml`, supporting global pattern, tag, and wildcard overrides.

### Recipe Tweaks
- **Reliable Recipe Replacements**: Sensible replacement recipes for items like the spyglass, shears, lever, and recovery compass, safely replacing vanilla recipes at runtime without file conflicts.
- **Utility Recipes**: Convenience crafting options, such as crafting bells from wooden slabs and gold, or sugar from beetroot.
- **Automatic Recipe Replacement System**: A lightweight, high-performance rule engine that rebalances broad categories of crafting recipes across vanilla and modded items:
  - **Redstone-Activated Components**: Buttons and pressure plates require redstone to craft (yielding 4 buttons and 2 pressure plates respectively).
  - **Cheaper Construction**: More sensible recipe yields for doors (yielding 4), trapdoors (yielding 8), fences (yielding 4), fence gates (yielding 2), signs (yielding 2), hanging signs (yielding 2), and chains (yielding 2).
  - Fully extensible via simple key-value rules supporting wildcards, tags, and pattern overrides.

## Installation
1. Ensure you have **Minecraft 1.21.1** installed with **NeoForge** (21.1.250 or newer).
2. Download the latest Orehall `.jar` release.
3. Place the `.jar` file into your Minecraft instance's `mods` directory.

## Configuration
Yes.
