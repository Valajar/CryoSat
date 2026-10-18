# Milestone 2 build plan: superconductors

Status: built and merged. Separator and connected casing seen in a headless client; the coil ring
only in game tests (a real reactor, ignited by setting its state). Playtest: section 7 of
`docs/testing-guide.md`.

How Milestone 2 of `space-age.md` is split for parallel agents. The foundation is done and merged;
workstreams build against it and must not change its public contracts without saying so.

## Foundation (done)

| Piece | What it is |
| --- | --- |
| `CryoChemicals.NITROGEN` | Nitrogen gas, the boil off of liquid nitrogen coolant. Liquefies back in the plant (`cryosat:liquefaction/nitrogen`, 77 K) |
| `superconductor.Cryostat` | The cooling of one superconducting block: a liquid nitrogen tank, a nitrogen boil off tank (90 percent of what boils off comes back), a steady trickle per tick, `use(amount)` for work, quench when dry and a 30 second cool down after refilling. The owning tile registers both tanks, ticks it, saves it and syncs its state |
| `superconductor.CryostatState`, `SuperconductorLang` | No coolant (quenched), cooling down, superconducting; shared GUI text |
| `CryoItems.SUPERCONDUCTING_COIL` | The crafted component of every superconducting block. Registered, no texture, model or recipe yet (workstream A) |
| `space.ICargoProcessor`, `ICargoReceiver.processBatch` | A batch is offered once, when a miner produces it, to the processors next to its loaded receiver. The Cargo Receiver asks its six neighbours; a Space Hub dock will do the same later |
| `space.SeparatorBonus` | +50 percent on a batch plus the asteroid class's `bonus_yields`, rolled per batch |
| `AsteroidClass.bonus_yields` | Separator extras with a chance per batch. Chondrite: diamonds (30 percent, 2 to 6), Mekanism fluorite (40 percent, 8 to 24) |

Tests: `gametest/SuperconductorTests`.

## Workstreams

Same rules as Milestone 1: own worktree, one commented section per shared class, own lang enum and
datagen helper, build, runData and every game test passing, commit on the branch.

### A. Superconducting Separator and the coil

* `superconducting_separator`: a Mekanism machine block (`TileEntityConfigurableMachine`, like the
  compressor) placed next to a Cargo Receiver. Implements `ICargoProcessor`: while its `Cryostat`
  is superconducting and it has the per batch coolant, it returns `SeparatorBonus.apply(...)`.
  Coolant: trickle 1 mB/t, plus 250 mB per batch (never per item). Chemical side config: liquid
  nitrogen in, nitrogen out (auto eject). Redstone control. No energy, or a small idle draw if that
  reads better as a Mekanism machine.
* GUI: liquid nitrogen gauge, nitrogen gauge, state, cool down left, coolant use, batches
  processed, a hint when no Cargo Receiver is adjacent.
* `superconducting_coil` item: texture, item model, recipe (mid game: copper, osmium or Mekanism
  alloys plus an insulator such as Mekanism HDPE; it should feel like a step up from Basic rocket
  parts). Separator recipe uses coils, a steel casing and a Cryo Casing.
* Textures for the separator (magnet coils visible through a window, cryo cobalt accents, an
  active frost variant).
* Game tests: a real receiver plus separator gets +50 percent and extras on a full mission; a dry
  separator gives nothing extra and quenches; boil off appears; cargo already waiting in orbit
  gets no bonus.

### B. Connected textures for the Liquefaction Plant

* The user wants the plant casing to tile seamlessly like connected texture packs do, so a big
  plant reads as one surface with a border only around its outline.
* Build a small connected texture model of our own (NeoForge custom geometry loader and a baked
  model that picks per face quarters from neighbour connections, the "compact" four texture
  method), no external mod dependency. Casing connects to every plant part; valves, coolers, ports
  and the controller keep their own faces. If it is cheap, apply it to Launch Pad Plating too.
* Generate the needed texture pieces with the existing casing script; preview a 5x5x5 plant and a
  3x3x3 plant rendered with the model before committing (extend `tools/textures/model_preview.py`
  or render a mock).

### C. Fusion Field Coils (Mekanism Generators)

* A ring multiblock around a Mekanism fusion reactor: a flat square loop of Field Coil blocks one
  or two blocks out from the reactor, at its middle layer, with one Field Coil Controller in the
  loop holding a `Cryostat` (coolant in and boil off out through tubes on the controller) and a
  GUI. Up to three stacked loops (middle layer and the layers above and below), each +25 percent.
* While superconducting, each tick: read the reactor's current passive generation from
  `FusionReactorMultiblockData` (public getters) and insert the bonus into its public
  `energyContainer` (internal automation). Coolant: trickle per loop plus an amount scaling with the
  bonus energy. No mixins. Water cooled reactors produce steam instead of energy; the bonus covers
  the energy output only for now (say so in the GUI).
* Generators is optional: add it as `compileOnly`, mark it optional in mods.toml, and keep every
  reference to Generators classes in classes only loaded when `ModList` has `mekanismgenerators`.
* Textures for coil and controller. Game tests with a real reactor if the game test server can
  form one (Generators is on the runtime classpath), otherwise test ring validation and the bonus
  maths against the public reactor data directly.

## After the workstreams

Merge, check the Separator and a coil ring in the client, update the playtest guide with a
superconductor section.
