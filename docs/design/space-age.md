# CryoSat Design: The Space Age

Status: agreed direction, details open. Supersedes the space half of `phase1.md` (Launch Complex,
power satellite, Rectenna). Nothing here is built yet.

## Why players go to space

Space has to give players something the ground cannot, or it is an expensive detour. Three reasons,
in priority order:

1. **Resources, early.** Asteroid mining returns common materials in bulk (iron, gold, copper,
   carbon directly instead of via coal, and the materials Mekanism and other mods need), so a first
   launch is worth it in the early to mid game. Later tiers add better yields and rare materials.
2. **Utility the ground cannot offer.** Satellite imagery that finds structures: a survival
   version of `/locate`.
3. **Spectacle.** Launches, a rocket that visibly reflects its tier, and later a globe view of your
   satellites and orbital strikes as a PvE feature.

Power from space (a Dyson swarm) is wanted but low priority.

## Decisions so far

| Topic | Decision |
| --- | --- |
| First payload | Asteroid Miner |
| Destinations | Abstract, datapack driven: asteroid belt first, gas giants and stars later. Never dimensions you visit |
| Rockets | Crafted parts placed into an assembly GUI. The assembled rocket shows as a 3D model on the pad, coloured by tier (Mekanism basic, advanced, elite, ultimate) |
| Cargo | A Cargo Receiver block on the ground: the early game answer. Later: the Space Hub, a station dimension (see "Space Hub") |
| Orbits | Abstract. No low or high orbit distinction, no coverage maths |
| Risk | No launch failures, no debris, for now |
| Ownership | Per player, no PvP. WarSats are PvE and spectacle when they come |
| Dimensions | None for the core loop. The Space Hub becomes one later |
| Superconductors | Not cables. Magnets and coils that only work while kept cold. Milestone 2, right after asteroid mining |

## Milestone 1: Asteroid mining

The smallest loop that proves space is fun.

**Launch Pad (multiblock)** with a **Rocket Assembly** GUI: put in an engine, fuel tanks, a guidance
computer and a payload, and the rocket appears on the pad as a 3D model. Fuel through the pad's
ports, then launch from the GUI or with redstone. Launch is particles, sound and a server wide chat
announcement; exhaust never touches blocks.

**Rocket tiers** follow Mekanism's four tiers and colours, so a player can read a rocket's tier at a
glance:

| Tier | Fuel | Unlocks |
| --- | --- | --- |
| Basic | Solid propellant (see below) | Small miners. Early game, no cryo needed |
| Advanced | Liquid oxygen plus Mekanism ethene | Bigger miners, more cargo |
| Elite | Liquid oxygen plus liquid hydrogen | Large miners, rare asteroid classes |
| Ultimate | Cryogenic solid grain: frozen oxygen plus metallic hydrogen (see below) | Endgame classes, stars, Dyson swarm segments |

This makes the first launch possible before the Liquefaction Plant, and the plant becomes the gate
to better launches rather than to space itself.

**Solid propellant** is sulfur, carbon and a binder, like a real composite rocket motor: Mekanism
sulfur dust (`c:dusts/sulfur`), Mekanism Enriched Carbon, and slime balls (`c:slime_balls`) as the
rubbery binder that makes it a solid, mixed by crafting into a Propellant Mix and pressed into a
Solid Propellant Grain in a Mekanism machine. Everything in it is available in the early game,
none of it is free.

**Ultimate propellant** closes the loop back to cryo and space: a fictional cryogenic solid grain of
oxygen frozen solid (below 54 K) around metallic hydrogen, a late material only found deep in gas
giants. It is cast in the Liquefaction Plant and has to stay cold on the pad: an Ultimate rocket
left without coolant warms up and stops being launchable until it is chilled again (never
explodes). Metallic hydrogen is a real theoretical super propellant, which keeps the fiction
plausible.

**Missions are rocket plus payload plus destination.** Destinations are datapack files
(`data/<ns>/cryosat/destination/*.json`) with a minimum rocket tier, a travel time and the payloads
that work there. Milestone 1 ships only the asteroid belt, but the data model is built so later
destinations plug in without new systems:

| Destination | Minimum tier | Payload | Gives |
| --- | --- | --- | --- |
| Asteroid belt | Basic | Asteroid Miner | Ores, carbon (Milestone 1) |
| Gas giant | Elite | Atmospheric Scoop | Helium; metallic hydrogen from deep dives |
| Exoplanets | Elite or Ultimate | Scoop or miner | Datapack defined gases and materials, room for modpacks |
| Star | Ultimate | Dyson segment, Stellar Scoop | Dyson swarm power; Stellar Plasma (use to be decided), the one thing only Ultimate rockets can get |

**Asteroid Miner (payload).** Once launched it locks onto one asteroid rolled from the classes its
tier can reach, and sends a few batches of cargo to a linked Cargo Receiver over its lifetime, then
retires. Each asteroid rolls a **size** and a **hardness**:

* Size sets the total yield, so a lucky large asteroid is worth noticeably more than a small one.
* Hardness sets how long each batch takes to mine.
* Miner tier sets how many batches it can deliver before wearing out and how much of a large
  asteroid it can actually extract.

Mission Control shows the asteroid's class, size and hardness once the miner has surveyed it, so the
variable outcome is visible rather than a hidden roll.

**Asteroid classes are datapack driven**, like atmospheres
(`data/<ns>/cryosat/asteroid/*.json`): minimum rocket tier, weight, size and hardness ranges, and
what they yield. Yields reference item tags (for example `c:raw_materials/iron`) or item ids, so
modpacks can add other mods' materials without code. Defaults:

* Chondrite (the Milestone 1 default), per small asteroid: 6 to 10 stacks of raw iron, 4 to 6
  stacks of Mekanism Enriched Carbon (`mekanism:enriched_carbon`), 2 stacks each of raw gold and raw
  osmium. Bigger sizes multiply this (medium 2x, large 4x, huge 8x), split into 3, 4, 6 or 8 batches.
* Size odds per rocket tier: Basic 70% small, 30% medium; Advanced 25% small, 50% medium, 25% large;
  Elite 15% medium, 55% large, 30% huge; Ultimate 40% large, 60% huge. Miner capacity matches the
  biggest size its tier's rocket finds.
* Later classes: metallic (copper, tin, lead), carbonaceous (coal, diamond), uranium, fluorite, and
  the rare materials from the resource brainstorm.

**Cargo Receiver.** A block that catches the batches and exposes them as an inventory for pipes.
Linked to miners with an Uplink Card, the same gesture as Mekanism's configuration card.

**Mission Control.** A simple list of your miners: status, health, next delivery, rename,
decommission. The globe view comes later.

**Orbital registry.** World saved data holding every orbital asset: owner, type, tier, health,
linked receiver, timers. Everything in orbit lives here, not in loaded chunks.

## Milestone 2: Superconductors

Comes right after asteroid mining, starting with the Superconducting Separator.

Mekanism cables are already lossless and the top tier carries an enormous amount of power, so
superconducting *cables* would add nothing. Superconductors in CryoSat are **magnets and coils**,
and their defining trait is that they **only work while kept cold**.

### Shared rules

* **Two grades.** High temperature superconductors are cooled with liquid nitrogen (77 K) and come
  first. Low temperature superconductors need liquid helium (4 K) and are stronger, for the endgame.
  Helium comes from gas giants (see "Gas giants" below), so the stronger grade is gated
  behind Elite rockets.
* **Coolant is consumed.** Every superconducting block takes its coolant through a tube and slowly
  boils it off, faster when working harder. The boil off returns as nitrogen gas, which can be piped
  back into the Liquefaction Plant, so a well built base runs a closed loop and only pays the power
  to reliquefy.
* **Quench.** If the coolant runs out, the block warms past its limit and its effect switches off
  until it is cold again, with a short cooldown. Never destructive.
* **One component.** All of them are built from a crafted Superconducting Coil (HTS or LTS grade),
  so the progression reads as "make coils, then put them to work".

### Applications

In planned order:

1. **Superconducting Separator**, a block placed next to a Cargo Receiver (and later next to a Space
   Hub dock). While cold it magnetically sorts each batch as it arrives: **+50 percent** on everything
   in the batch, plus a chance at rarer extras listed per asteroid class (chondrite: diamonds,
   Mekanism fluorite). Coolant: a small steady trickle of liquid nitrogen while cold, plus a fixed
   amount per batch (never per item, so big asteroids do not make it unaffordable). It only works on
   batches that arrive while it is loaded and cold; cargo already waiting in orbit gets no bonus.
2. **Fusion Field Coils** (Mekanism Generators fusion reactor). A real tokamak confines its plasma
   with superconducting magnets, and a stronger field means better confinement. The coils are their
   own multiblock: a flat square ring of Field Coil blocks around the reactor's middle layer, one or
   two blocks away from it, with a coil controller and a coolant port in the ring. Stacking up to
   three rings raises the field. While cold, the ring adds a flat share of the reactor's current
   output (for example +25 percent per ring) straight into the reactor's own energy buffer, which
   Mekanism exposes publicly, so no mixin is needed and nothing inside Mekanism is patched. Coolant
   use scales with the bonus. The multiplier lives on the reactor, not on a cable: a multiplying
   cable could be chained or looped into infinite power. Only active when Generators is installed.
3. **SQUID survey sensor** (Survey Satellite upgrade, Milestone 3). Real SQUID magnetometers detect
   tiny magnetic fields and are used in mineral exploration. This is the upgrade that lets satellite
   imagery see underground structures (ancient cities, strongholds), instead of a generic radar.
4. **Electromagnetic Launch Rail** (Launch Pad addon). A coil gun track that pushes the rocket off
   the pad, cutting fuel per launch or letting a lower tier rocket carry a bigger payload.
5. **Superconducting Magnetic Energy Storage** (multiblock). Stores energy in a persistent current:
   enormous input and output rate for its size, but it needs constant cooling and slowly bleeds its
   charge while quenched. A buffer for fusion and Dyson power rather than a replacement for the
   Induction Matrix.
6. **Dyson swarm receiver.** The ground station that catches the swarm's power beam.

## Milestone 3: Satellite imagery

A constellation of **Survey Satellites** in Earth orbit plus an **Imaging Terminal** on the ground.
Every satellite adds coverage (more for better tiers, approaching but never reaching 100 percent),
which makes results more precise, reach further and run in parallel. Results are logged in the
terminal, which remembers what was found so the next request finds a new, unlooted structure, and
are loaded onto a **Survey Locator**, a tech compass that points to the target and marks it in the
world. Underground structures need a SQUID Array next to the terminal. No orbits over the Nether or
the End.

Build plan and the decisions underneath it: `milestone3-plan.md`.

## Gas giants: helium and metallic hydrogen

Scheduled after Milestone 3. Real helium comes from natural gas wells, but drilling gas pockets would
be a ground side detour; scooping it from a gas giant's atmosphere is more in theme.

* **Atmospheric Scoop** (payload, Elite rocket and up): skims a gas giant and returns pressurised
  gas canisters to the Cargo Receiver, which empties them into tanks like a chemical output.
  Gas giant and exoplanet atmospheres reuse the atmosphere format the Atmospheric Compressor
  already reads, so they are datapack defined too.
* **Helium** is liquefied in the plant at 4 K, the hardest recipe it has, and feeds the low
  temperature superconductors.
* **Deep dives** with a better scoop return a little **metallic hydrogen**, the late material for
  the Ultimate propellant.
* Possible later bonus: helium 3 as an alternative fusion fuel, if it fits alongside Mekanism's
  D T fuel.

## Space Hub (planned after the early game)

The Cargo Receiver is the early game answer. The Space Hub replaces it as the place cargo arrives:

* **A station dimension** with a custom sky: Earth as a large rotating 3D model below the station,
  an atmosphere glow on its rim, and the station's orientation to Earth adjustable by the player.
* **Cheaper launches from orbit**, and **reusable rockets**: rockets return to the Hub intact instead
  of being spent.
* **Docks** take over from Cargo Receivers. Anything built for receivers, like the Superconducting
  Separator, attaches to a dock the same way, so nothing from the early game becomes useless.
* Needs a way for players to travel up and back (an early crew capsule, later a shuttle).

## Destinations and rocket roles

Decided: planned work (superconductors, satellite imagery, gas giants, Space Hub) comes first; these
destinations are queued behind it. Planets keep their real Solar System names.

| Destination | Tier | Payload | Gives | Decision |
| --- | --- | --- | --- | --- |
| Moon | Basic | Regolith Miner | Redstone above all, plus quartz and glowstone; later helium 3 | **Yes**, the second early target. Mekanism has no renewable redstone, so the Moon fills that gap |
| Mars | Advanced | Surface Lander | Lithium in bulk, Mekanism salt, redstone, iron | **Yes**. Lithium en masse is an alternative to the Thermal Evaporation Plant |
| Icy moons | Elite | Ice Harvester | Heavy water ice (deuterium) | **Maybe**. Only worth it if it beats pumping heavy water with a filter upgrade by a wide margin |
| Near Earth comets | Advanced | Ice Harvester | Water and nitrogen ice | **No**. The Liquefaction Plant already makes plenty of coolant, and Quantum Entangloporters carry it wherever it is needed |
| Metallic asteroid classes | Advanced | Asteroid Miner | Copper, tin, lead, uranium | Widens the belt, cheap to add as data |
| Gas giant | Elite | Atmospheric Scoop | Helium, metallic hydrogen | Already planned |
| Exoplanets | Elite or Ultimate | Datapack defined | Modpack materials | Already planned |
| Star | Ultimate | Stellar Scoop, Dyson segment | Stellar plasma, Dyson power | Already planned |

Rocket roles, as payload types rather than new rockets (a rocket is tier plus payload, so new
roles cost no new rocket models):

* **Miner** (asteroids, Moon): batches of solids, built.
* **Harvester or Scoop** (comets, icy moons, gas giants, stars): canisters of chemicals, delivered
  into a receiver's tanks.
* **Lander** (planets): one large delivery after a long trip instead of batches.
* **Satellite** (Earth orbit): survey imagery, later WarSats; never returns cargo.
* **Crew capsule and shuttle** (Space Hub era): carry players and return reusable rockets.

## Later (recorded, not scheduled)

* **Dyson swarm.** Launch segments over time; power scales with how much of the swarm is built and
  can exceed fusion when large. Received on the ground by a beam receiver.
* **Orbital Display globe.** The custom globe screen from `phase1.md`, colour coded by category.
  Worth it once there are several kinds of satellites.
* **WarSats.** Orbital strikes as PvE and spectacle, with server config for block damage. Planned
  with the Space Station update.
* **Risk.** Launch failures, debris. Explicitly out for now.

## Open questions

1. What Stellar Plasma is used for.
2. Icy moons: what deuterium rate would clearly beat a heavy water pump, and is that worth a trip.
2. Asteroid numbers beyond the chondrite starting point, settled by playtesting.
