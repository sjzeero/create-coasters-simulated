# Create: Coasters Simulated (CCS fix)

An unofficial fork of [Create: Coasters Simulated](https://www.curseforge.com/minecraft/mc-mods/create-coasters-simulated) 0.1.5 by SilverGold. It fixes severe server TPS lag caused by constant sublevel and rivet physics scans.

- Minecraft: 1.21.1
- Loader: NeoForge
- Mod version: 0.1.5 (unchanged on purpose)

## What is fixed

- **Cart bearing checks** (`CoasterCartBearingTickCache`) are cached with a TTL instead of scanning blocks on every physics tick.
- **Rivet disassembly checks** (`RivetDisassemblyEnforcer`: `enforceSingleRivet`, `popIfSelfHosted`) are throttled instead of running every tick.
- **Track snap removal** (`CoasterCartTrackSnap`) is guarded by `KNOWN_CARTS`, so non-cart sublevels no longer broadcast removal packets to clients every tick.
- **Persisted train links** (`CoasterCartTrainLinkConstraint.maybeRestorePersistedLinks`) are restored every 3 seconds instead of every tick.

## Compatibility

The mod version and network protocol are the same as the original 0.1.5, so the original client can connect to a server running this build. Install this build on the server; clients can keep the original mod.

Required dependencies are the same as the original: Create, Sable, Ponder, Flywheel, Veil, Catnip and Registrate (see `libs/`).

## Building

Requirements: JDK 21 and Gradle 8.12 or newer.

The bundled `gradle-wrapper.jar` is broken, so use a system Gradle instead of `./gradlew`:

    gradle build -x test

The jar is written to `build/libs/`.

## Source notes

The sources in this repository come from a decompiled build of the original mod (cleaned up with Vineflower), so formatting and variable names differ from the upstream code.

## Credits and license

All credit for the original mod goes to SilverGold. This fork only adds the performance fixes listed above. It is not affiliated with or endorsed by the original author. Please check the original project's page for its license terms before redistributing.

- Original on CurseForge: https://www.curseforge.com/minecraft/mc-mods/create-coasters-simulated
- Original on Modrinth: https://modrinth.com/mod/create-coasters-simulated
