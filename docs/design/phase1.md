# CryoSat Design: Phase 1, "First Light"

Status: draft for review. Numbers are starting points and all of them become config values.

## Goal of phase 1

One complete loop through both halves of the mod:

> Pull air out of the sky, liquefy it, liquefy hydrogen, fuel a rocket, put a power satellite in
> orbit, and receive its power on the ground.

Everything in this phase exists to make that loop work and feel good. Asteroid miners, survey and
comms satellites, superconductors and WarSats come in later phases, but phase 1 builds the systems
they plug into (the cooling model, the orbital registry, the launch sequence).

## Design pillars

1. **Cold is the resource.** Mekanism players spend the game making things hot. CryoSat makes them
   manage cold, and the cold has a real cost: the colder you go, the more energy every unit of
   cooling takes, and all of that energy comes back out as heat you must dump somewhere.
2. **CryoSat feeds Mekanism and Mekanism feeds CryoSat.** Mekanism's hydrogen, oxygen, ethene and
   heat network are inputs. Our waste heat is useful to Mekanism's boilers. Our chemicals flow
   through Mekanism tubes and tanks.
3. **Orbit is a place you manage, not a place you walk.** No dimension. Satellites are records in a
   world wide orbital registry, managed from Mission Control.
4. **Big moments, cheap upkeep.** Launches are a spectacle. Running a satellite afterwards should not
   need babysitting beyond slow wear.

## New chemicals

All registered in Mekanism's chemical registry, so Mekanism pipes, tanks and machines carry them.

| Id | Name | State | Tint | Source |
| --- | --- | --- | --- | --- |
| `compressed_air` | Compressed Air | gas | `#c9d3dc` | Atmospheric Compressor |
| `liquid_nitrogen` | Liquid Nitrogen | liquid | `#d8e4f5` | Liquefaction Plant (exists already) |
| `liquid_oxygen` | Liquid Oxygen | liquid | `#8fb4ff` | Liquefaction Plant |
| `liquid_argon` | Liquid Argon | liquid | `#c9b8ff` | Liquefaction Plant (byproduct, used in phase 2) |
| `liquid_hydrogen` | Liquid Hydrogen | liquid | `#e8f1ff` | Liquefaction Plant from Mekanism hydrogen |

Liquid nitrogen also gets Mekanism's cooled coolant data map entry, so it works as a coolant where
Mekanism accepts one. Boil off (cryogenic liquids slowly warming in ordinary tanks) is a phase 2
idea and is deliberately left out here.

## Machines

### 1. Atmospheric Compressor (1x1 machine)

Pulls air from the environment and outputs Compressed Air.

* Needs at least 4 open air blocks adjacent to it (any side), otherwise it stalls with a tooltip
  explaining why.
* Output scales with dimension and altitude: Overworld 100 percent at sea level, falling linearly to
  40 percent at build height (thin air). The Nether yields Mekanism's sulfur dioxide instead of air,
  a small nod that rewards curiosity. The End and other dimensions without an atmosphere yield
  nothing.
* Base: 200 J per mB, 10 mB per tick. Accepts Mekanism speed and energy upgrades if we can do so
  through the API (see "Integration notes"), otherwise our own upgrade slots.
* Ports: energy on the back and bottom, chemical output on top. Fixed sides in phase 1, Mekanism
  style side config later.

### 2. Cryogenic Liquefaction Plant (multiblock)

The centrepiece of the cryogenics half. It inverts Mekanism's Thermal Evaporation Plant: instead of
collecting heat, it fights it.

**Structure.** A hollow box from 3x3x3 up to 5x5x7 (width, depth, height), built from:

| Block | Role |
| --- | --- |
| Cryo Casing | Frame and walls |
| Insulated Glass | Optional wall windows, shows the coolant level inside |
| Cryo Valve | Chemical input and output (mode set with a configurator) |
| Cryocooler | Wall component. Each one removes heat from the plant while it has power |
| Heat Rejection Port | Wall component. Pushes the plant's removed heat into any Mekanism heat handler |
| Liquefaction Controller | One per plant, holds the GUI |

Volume decides tank sizes and how much heat leaks in from outside. Cryocoolers decide cooling power.

**The thermal model.** The plant has one internal temperature `T`, starting at ambient (300 K).

* *Heat leak in:* every tick, heat flows in proportional to `(300 - T)` times the plant's surface
  area times an insulation factor. Insulated Glass leaks more than casing; MLI wrapped casings
  (crafted upgrade, phase 1 recipe) leak less.
* *Cooling:* each powered Cryocooler removes up to `Q` heat per tick. The energy it costs follows a
  Carnot style curve: `energy = Q * (300 - T) / T * inefficiency`. Cooling at 77 K costs about 3x the
  heat moved; at 20 K it costs about 14x. Liquid hydrogen is expensive on purpose.
* *Rejection:* the removed heat plus the energy spent (energy becomes heat) must leave through Heat
  Rejection Ports into a Mekanism heat handler: a Thermodynamic Conductor, a Thermoelectric Boiler,
  a Resistive Heater's neighbour, anything that takes heat. If the ports cannot dump heat, the
  cryocoolers stall and `T` climbs. This is the core puzzle: a big cryo plant is also a big heat
  source, and a smart player pipes it into a Mekanism boiler and gets steam out of it.
* *Process heat:* liquefying chemicals releases latent heat into the plant, so throughput is limited
  by cooling capacity too.

**Recipes** are gated by temperature, which is what turns cooling into progression:

| Input | Output (per 100 mB) | Needs T at or below |
| --- | --- | --- |
| Compressed Air | 78 mB Liquid Nitrogen, 21 mB Liquid Oxygen, 1 mB Liquid Argon | 77 K |
| Mekanism Oxygen | 100 mB Liquid Oxygen | 90 K |
| Mekanism Hydrogen | 100 mB Liquid Hydrogen | 20 K |

A small plant with a few cryocoolers reaches 77 K comfortably. Reaching 20 K needs a well insulated,
larger plant with many cryocoolers and serious heat rejection, which lands liquid hydrogen around
the point where Mekanism players have fission or a big solar array.

**GUI.** Temperature gauge with the three recipe thresholds marked, a heat flow readout (leak in,
cooling, rejected), input and output tanks, energy bar.

### 3. Launch Complex (multiblock)

**Structure.**

* *Launch Pad:* a flat 5x5 of Launch Pad Plating with a Launch Controller anywhere along its edge.
* *Umbilical Tower:* a column of 3 to 6 Umbilical Tower segments standing next to the pad, topped
  with an Umbilical Arm. The tower carries Fuel Ports (chemical input) at its base.

**Rockets** are crafted items, not built in world:

| Tier | Fuel | Reach | Payload slots |
| --- | --- | --- | --- |
| Tier 1 Light Rocket | Mekanism Ethene + Liquid Oxygen | LEO only | 1 |
| Tier 2 Heavy Rocket | Liquid Hydrogen + Liquid Oxygen | LEO or GEO | 2 |

Tier 1 needs the plant only for liquid oxygen (77 K), so a player can launch early. Tier 2 needs
liquid hydrogen (20 K) and is where the cryogenics investment pays off. A tier 3 nuclear thermal
rocket using Mekanism fissile fuel or antimatter is pencilled in for phase 3.

**Launch sequence.**

1. Insert a rocket and its payloads into the Launch Controller.
2. Fuel through the tower's Fuel Ports. The rocket item stores its fuel.
3. Press launch in the GUI, or send a redstone pulse.
4. A 10 second countdown with alarm and venting particles, then the rocket entity spawns on the pad
   and climbs with exhaust particles and camera shake for nearby players. Above build height the
   entity despawns and its payloads are added to the orbital registry.
5. Every player on the server gets a chat announcement ("Valajar launched a Heavy Rocket carrying
   an Orbital Solar Array") and the rocket's smoke trail is visible from far away.
6. The pad plating scorches (cosmetic block state) and cools back over a minute.

Exhaust never damages or burns blocks. It is particles, sound and a short knockback push on
entities standing in the flame, nothing more.

### 4. Orbital registry (system, no block)

World saved data on the Overworld holding every orbital asset:

* id, owner (player or team), custom name, payload type
* orbit: `LEO` (cheap, passes overhead, works part of the day) or `GEO` (expensive, stationary,
  works all the time) plus a ground position for its footprint
* health (0 to 100 percent), launch time, and type specific data (stored energy for a power
  satellite, and so on)

Health decays slowly from micrometeoroids and radiation, about 1 percent per in game day for LEO
and 0.5 percent for GEO. A satellite at 0 percent stops working and stays listed as debris until it
is deorbited. This creates a gentle, long term demand for launches without punishing anyone.

### 5. Mission Control (1 block, 3D model)

A console desk with screens. Its GUI lists the player's orbital assets: name, orbit, health, status
(for example "in sunlight, beaming 4 kJ/t"). Actions in phase 1: rename, deorbit, and link a
satellite to a ground receiver using an Uplink Card (copy from satellite, paste on receiver, the
same gesture as Mekanism's configuration card). Mission Control is only needed to manage assets;
satellites keep working without it.

### 6. Orbital Solar Array and Rectenna (the first payload)

* **Orbital Solar Array** (payload item): gathers solar power in orbit. GEO arrays produce
  constantly; LEO arrays produce during the day only but are cheaper to launch.
* **Rectenna** (multiblock): a flat 3x3 of Rectenna Panels plus a Rectenna Controller, linked to one
  or more arrays with the Uplink Card. Outputs energy to Mekanism cables. Rain cuts reception by 30
  percent and thunderstorms by 60 percent. Must have open sky above it.
* Output target: a GEO array should roughly match a few Mekanism Advanced Solar Generators per
  launch, because the launch is expensive. Tuned in playtesting.

### 7. Orbital Display (holographic globe)

A block whose screen is not an inventory GUI but a full custom view: a slowly rotating globe in the
middle of the screen with every orbital asset drawn on it.

* Orbits are traced as rings around the globe. LEO rings sit close and the dots move along them
  visibly; GEO assets hang still above their ground position.
* Color coded by category: **energy** (cryo cobalt), **resource** (MLI gold), **utility** (white),
  **offensive** (ordnance red). Dead satellites show as grey debris.
* Drag to rotate, scroll to zoom, hover a satellite for its name, health and status, click to
  select it (and open the same actions as Mission Control).
* The globe itself is a stylised sphere using our palette. A nice stretch goal is to texture it
  from the world's actual biome map around spawn, so the planet is recognisably your world.
* The block itself has a 3D model with a holographic projector on top, so the globe can also float
  above the block in the world when someone is using it.

Phase 1 ships the globe with the energy category only (that is all that exists yet), but the
renderer is built for all four categories from the start.

## Phase 2 direction

Phase 2 focuses on **resources**: asteroid mining and other space sourced materials that are hard or
impossible to get in a normal world, including the new elements needed for superconducting power
transfer. It gets its own brainstorm once phase 1 is playable, so phase 1 only needs to leave room
for it (resource category in the registry and the globe, payload slots that can hold a miner).

## New items and components

| Item | Made from (rough) | Used in |
| --- | --- | --- |
| MLI Foil | Gold ingot plus plastic (Mekanism HDPE sheet) | Insulated casings, satellites |
| Cryocooler Unit | Steel, osmium, Advanced Control Circuit | Cryocooler block |
| Insulated Panel | Steel casing plus MLI Foil | Cryo Casing |
| Rocket Engine (T1, T2) | Steel, refined obsidian, alloys (T2: atomic alloy) | Rockets |
| Fuselage Section | Steel sheet style recipe, Mekanism steel | Rockets |
| Guidance Computer | Elite Control Circuit, HDPE | Rockets, satellites |
| Payload Fairing | Steel, MLI Foil | Rockets |
| Solar Array Panel | Mekanism Solar Panel, MLI Foil | Orbital Solar Array |
| Uplink Card | Basic Control Circuit, HDPE | Linking |

## Integration notes (for whoever writes the Java)

* **Chemicals:** `mekanism.api.chemical` (`BasicChemicalTank`, `IChemicalHandler`). Done for liquid
  nitrogen already.
* **Capabilities:** Mekanism defines its block capabilities in non API code, but NeoForge identifies
  capabilities by name and type. A small `MekCapabilities` class in our code that calls
  `BlockCapability.createSided(ResourceLocation.fromNamespaceAndPath("mekanism", "chemical_handler"),
  IChemicalHandler.class)` (likewise `heat_handler` with `IHeatHandler`, `strict_energy_handler` with
  `IStrictEnergyHandler`) gets the same capability objects without touching Mekanism internals.
* **Energy:** expose both Mekanism's strict energy handler (Joules) and NeoForge's FE capability.
* **Heat:** Heat Rejection Ports find neighbouring `IHeatHandler`s and push heat into them with
  `handleHeat`. Ambient temperature constant: `mekanism.api.heat.HeatAPI.AMBIENT_TEMP`.
* **Upgrades:** if Mekanism's upgrade system turns out to be internal only, we use our own upgrade
  slot accepting Mekanism's upgrade items through their item tags or ids.
* **Rocket rendering:** the rocket entity renders baked JSON models (registered as standalone
  models), so rockets are made in Blockbench like the other 3D assets. Each rocket is split into a
  lower stage and an upper stage model, because one block model cannot exceed 3 blocks.

## Asset list for texture and model work

Follow `docs/texture-style-guide.md`. "1x1 faces" means front, front_active (if animated), back,
left, right, top, bottom, reusing faces where they are identical.

| Asset | Kind | Notes |
| --- | --- | --- |
| Atmospheric Compressor | 1x1 faces | Intake grille on top, cobalt trim, light frost. Active: spinning fan (animated) |
| Cryo Casing | Casing, tiling | Lighter casing steel, cobalt seams, frost at the top edge. Must tile cleanly |
| Insulated Glass | Casing, tiling | Like Mekanism structural glass with a faint cobalt frame |
| Cryo Valve | Casing variant | Casing with a valve ring in cobalt, plus an input and output LED overlay |
| Cryocooler | Casing variant | Fins and a cold head; active state with coolant glow |
| Heat Rejection Port | Casing variant | Warm colored grille (Mekanism heat orange is fine here, it is heat) |
| Liquefaction Controller | Casing variant | Screen with temperature readout, animated when running |
| Launch Pad Plating | 1x1 top plus side | Hazard markings on top, scorched state variant |
| Launch Controller | 1x1 faces | Console face with a big launch button, ordnance red trim |
| Umbilical Tower and Arm | 3D model | Steel lattice column, MLI gold wrapped arm, small tier free |
| Mission Control | 3D model | Desk with three screens (screens on an `_led` sheet) |
| Orbital Display | 3D model | Pedestal with a holo projector lens on top (lens glow on an `_led` sheet) |
| Rectenna Panel and Controller | 3D model (flat) | Low mesh grid on struts; controller with a status LED |
| Rocket T1 and T2 | 3D models, 2 stages each | Steel body, MLI gold interstage, tier accent ring (basic for T1, advanced for T2) |
| Orbital Solar Array | Item, plus a 3D model for Mission Control and the launch | Solar cells and gold foil |
| Items in the table above | 16x16 items | Match Mekanism's item style (circuits, alloys) |

## Build order

Each step is one pull request with game tests where behaviour can be tested.

1. **Machine foundation:** base block entity with energy, chemical tanks, a simple GUI, capability
   mirroring, data generation setup (blockstates, models, loot, lang, recipes).
2. **Atmospheric Compressor** plus the remaining chemicals.
3. **Liquefaction Plant:** multiblock validation, thermal model, recipes, heat rejection into
   Mekanism heat handlers. Game tests for structure formation and for equilibrium temperatures.
4. **Orbital registry and Mission Control:** data and GUI, with a debug command to add assets.
5. **Orbital Display:** the custom globe screen and the block's hologram.
6. **Launch Complex and rockets:** structure, fuelling, launch sequence, rocket entity and rendering.
7. **Orbital Solar Array and Rectenna:** first payload, closing the loop.
8. **Balance and polish:** recipes, JEI categories, configs, sounds.

Texture and model work runs in parallel from step 1, driven by the asset list.

## Decisions log

* Rocket names: plain descriptive names (Light Rocket, Heavy Rocket).
* Exhaust: cosmetic only, never touches blocks.
* Launches: announced to the whole server.
* Satellite viewing: custom globe screen (Orbital Display), color coded by category.
* Phase 2: resources first, brainstormed separately once phase 1 runs.

## Open questions

1. Atmospheric Compressor in the Nether: sulfur dioxide output, or simply refuse to run?
