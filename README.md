# FantaLory

FantaLory is a fantasy-themed Mindustry Java mod focused on magical combat, custom resource systems, and a complete custom progression branch.

## Highlights

- A custom planet progression path centered on Talory content.
- New magical resources and production chains, including mana and magic-liquid mechanics.
- New items, floors, blocks, and unit content with fantasy-style visuals.
- Extended combat APIs with magic damage behavior and custom damage interactions.
- In-game FantaLory menu pages (encyclopedia, codex-style sections, credits, settings, and music room).
- Built-in custom music playlist support in the mod UI.

## Supported Build

- Designed for Mindustry Java mod loading on desktop and Android packaging workflow.
- `minGameVersion` is defined in `mod.hjson`.

## Install

1. Build or download the mod `.jar`.
2. Import the `.jar` in Mindustry Mods menu.
3. Enable the mod and restart the game if required.

## Local Build

### Desktop test build

```bash
gradlew jar
```

### Cross-platform package (Desktop + Android)

```bash
gradlew deploy
```

Notes:
- JDK 17 is recommended for this project.
- Android packaging requires a valid Android SDK path in `local.properties` (`sdk.dir=...`) or environment variables.

## Repository Structure

- `src/` Java source code for gameplay systems and UI.
- `assets/` sprites, bundles, and visual assets.
- `music/` in-mod music files used by the music room and menu music logic.
- `maps/` map-related content placeholders/assets.

## License

This repository currently does not define a separate license file.
