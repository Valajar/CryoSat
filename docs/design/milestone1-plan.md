# Milestone 1 build plan: asteroid mining

How Milestone 1 of `space-age.md` is split so several agents can build it in parallel. The
foundation is done and merged; every workstream builds against it and must not change its public
contracts without saying so in its report.

## Foundation (done)

Package `io.github.valajar.cryosat.space`:

| Class | What it is |
| --- | --- |
| `RocketTier` | Basic, Advanced, Elite, Ultimate. Mekanism tier colour (`color()`), miner capacity, fuel per launch (`fuel()`, empty for Ultimate until the gas giants) |
| `PayloadType` | `ASTEROID_MINER` for now |
| `RocketPartItem` | Engine, fuel tank, guidance computer, payload items. Registered in `CryoItems` (`ROCKET_ENGINES`, `FUEL_TANKS`, `GUIDANCE_COMPUTER`, `ASTEROID_MINERS`, plus `PROPELLANT_MIX`, `SOLID_PROPELLANT_GRAIN`) |
| `RocketDesign` | An assembled rocket (tier, payload, payload tier). `assemble(engine, tank, guidance, payload)` returns the design or a `MissionProblem`. Codec and stream codec for saving and syncing |
| `Destination`, `AsteroidClass`, `SpaceData` | Datapack types in `data/<ns>/cryosat/destination` and `.../asteroid`. Default: `cryosat:asteroid_belt` and `cryosat:chondrite` |
| `OrbitalRegistry` | World saved data of every `OrbitalAsset`. `check(design, destination)`, `launch(server, owner, design, destination, receiver)`, `ownedBy(uuid)`, `rename`, `relink`, `decommission`. Ticks every second: arrival, batches, delivery |
| `ICargoReceiver` | Implemented by the Cargo Receiver's tile; the registry calls `receiveCargo(stack, asset)` while the chunk is loaded and keeps what does not fit |
| `SpaceLang` | Shared lang keys (parts, problems, asset states, default names, launch announcement); text in `datagen/space/SpaceLangData` |

Also: `registries/CryoDataComponents.UPLINK_TARGET` (a `GlobalPos` on the Uplink Card) and
`client/rocket/RocketRenderer` (stub with `render(...)`, `exhaust(...)`, `HEIGHT`, `RADIUS`).
Tests: `gametest/OrbitalTests`.

## Workstreams

Each runs in its own worktree and ends with `./gradlew build`, `runData` and `runGameTestServer`
passing, committed on its branch. Shared registry and datagen classes (`CryoBlocks`,
`CryoBlockTypes`, `CryoBlockEntities`, `CryoMenus`, `CryoSatClient`, `CryoDataGen`,
`CryoLanguageProvider`, `CryoBlockLoot`, `CryoRecipeProvider`, ...) get one clearly commented section
per workstream appended at the end, so merges stay mechanical. Lang keys go into a lang enum owned by
the workstream (like `SpaceLang`), with English text in a `datagen/<feature>` helper.

### A. Cargo Receiver and Uplink Card

* Cargo Receiver: Mekanism machine (`TileEntityConfigurableMachine`, like the compressor) with a
  grid of output slots, item side config and auto eject, no energy. Implements `ICargoReceiver`.
  GUI shows the slots and the missions linked to it (from the registry, synced).
* Uplink Card: right click a Cargo Receiver to store `UPLINK_TARGET`, sneak right click air to
  clear, tooltip shows the target.
* Textures for the receiver; item texture for the card. Game tests: a mission linked to a real
  receiver fills it, a full receiver keeps the rest in orbit.

### B. Launch Pad and Rocket Assembly

* Launch Pad multiblock: a flat platform with a Launch Controller, fuel ports (chemical and item)
  and plating, built on Mekanism's multiblock framework if it handles a flat structure, otherwise a
  controller side scan. Needs a clear column above the centre (`RocketRenderer.RADIUS`,
  `HEIGHT`).
* Rocket Assembly GUI on the controller: engine, fuel tank, guidance computer and payload slots,
  an uplink slot for the Uplink Card, destination selector (only the asteroid belt for now), the
  `MissionProblem` or fuel status, and a Launch button. Redstone launch too.
* Assembling consumes the parts into a stored `RocketDesign`, synced to clients. Fuel per
  `RocketTier.fuel()`. Launching burns the fuel, calls `OrbitalRegistry.launch` with the tile
  owner (Mekanism security owner), plays a launch sequence of about 5 seconds (rocket rises,
  `RocketRenderer.exhaust` each client tick, sound), then clears the pad, and announces
  `SpaceLang.LAUNCH_ANNOUNCEMENT` server wide. Exhaust never touches blocks.
* Block entity renderer that calls `RocketRenderer.render` at the pad centre, including during the
  rise; render bounding box covers the rocket.
* Textures for the pad blocks. Game tests: a structure forms, assembly and problems, fuel is
  consumed, launch creates an asset with the uplink's receiver.

### C. Rocket model and launch effects

* Replace the `RocketRenderer` stub: a Blockbench style JSON model (or several, one per part)
  loaded as an additional model and drawn with the tier colour as a tint on accent faces, within
  `HEIGHT` and `RADIUS`. Steel body per the style guide, tier coloured stripes and fins; the
  payload tier can tint the nose.
* `exhaust`: flame and smoke particles scaling with progress, client only, no block interaction.
* Optional: a debug command or creative only item that shows a rocket in world, for screenshots.
  No game tests needed beyond the build; check the model by rendering previews.

### D. Mission Control

* A block with a custom Mekanism GUI listing the player's assets from `OrbitalRegistry.ownedBy`:
  name, state, destination, asteroid class and size once arrived, extracted share, batches
  delivered of total, time to next batch, pending cargo, linked receiver.
* Actions: rename, relink to the Uplink Card in a slot, decommission (with confirm). Server
  validates ownership.
* Sync through a payload or Mekanism container sync; refresh while open. Textures. Game tests for
  the server side actions.

### E. Items, recipes and the propellant chain

* Item textures and generated item models for every item in `CryoItems` (tier accent from the
  Mekanism tier colours, per the style guide).
* Recipes: Propellant Mix by crafting from `c:dusts/sulfur`, Mekanism Enriched Carbon and
  `c:slime_balls`; Solid Propellant Grain from the mix in Mekanism's Enrichment Chamber (or the
  machine that fits best); rocket parts per tier from Mekanism alloys, circuits and steel, Basic
  parts reachable in the early game; Ultimate parts left without recipes for now.
* Game tests that every recipe loads.

## After the workstreams

Merge in order E, A, C, B, D; wire anything that crosses workstreams; update the playtest guide
with a space section; check the whole loop in the client.
