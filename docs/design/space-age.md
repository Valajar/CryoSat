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
| Rockets | Crafted parts placed into an assembly GUI. The assembled rocket shows as a 3D model on the pad, coloured by tier (Mekanism basic, advanced, elite, ultimate) |
| Cargo | A Cargo Receiver block on the ground for now. Later: a Space Hub station (see "Later") |
| Orbits | Abstract. No low or high orbit distinction, no coverage maths |
| Risk | No launch failures, no debris, for now |
| Ownership | Per player, no PvP. WarSats are PvE and spectacle when they come |
| Dimensions | None for the core loop. The Space Hub may become one later |
| Superconductors | Not cables. Superconducting magnets that boost miners (see below) |

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
| Basic | Solid propellant from early Mekanism materials | Small miners. Early game, no cryo needed |
| Advanced | Liquid oxygen plus Mekanism ethene | Bigger miners, more cargo |
| Elite | Liquid oxygen plus liquid hydrogen | Large miners, rare asteroid classes |
| Ultimate | To be decided (antimatter or nuclear) | Endgame classes, Dyson swarm segments |

This makes the first launch possible before the Liquefaction Plant, and the plant becomes the gate
to better launches rather than to space itself.

**Asteroid Miner (payload).** Once launched it stays in orbit and sends batches of cargo to a linked
Cargo Receiver every so often, until it wears out (slow decay, then replace it). Bigger miners from
higher tier rockets send bigger batches and can reach rarer asteroid classes.

**Asteroid classes are datapack driven**, like atmospheres
(`data/<ns>/cryosat/asteroid/*.json`): minimum rocket tier, weight, and what they yield. Yields
reference item tags (for example `c:raw_materials/iron`), so modpacks can add other mods' materials
without code. Defaults:

* Metallic: raw iron, raw copper, raw gold, plus Mekanism raw osmium, tin and lead.
* Carbonaceous: carbon directly (Mekanism enriched carbon or our own carbon item), some coal, some
  diamond at higher tiers.
* Later classes: uranium, fluorite, and the rare materials from the resource brainstorm.

**Cargo Receiver.** A block that catches the batches and exposes them as an inventory for pipes.
Linked to miners with an Uplink Card, the same gesture as Mekanism's configuration card.

**Mission Control.** A simple list of your miners: status, health, next delivery, rename,
decommission. The globe view comes later.

**Orbital registry.** World saved data holding every orbital asset: owner, type, tier, health,
linked receiver, timers. Everything in orbit lives here, not in loaded chunks.

## Superconductors: what they are for

Mekanism cables are already lossless and the top tier carries an enormous amount of power, so
superconducting *cables* would add nothing. Superconductors in CryoSat are **magnets**, and their
defining trait is that they **only work while kept cold**:

* **Superconducting Separator** (Cargo Receiver upgrade): magnetically sorts incoming cargo for
  better yields and a chance at rarer materials, but needs a steady feed of liquid nitrogen. That
  gives the Liquefaction Plant a permanent job after the early launches, and matches the idea that
  cryo improves how well miners work rather than what they find.
* A higher tier magnet cooled by liquid helium could later boost Elite and Ultimate miners further.
* Other places magnets could matter later: an electromagnetic launch assist that cuts fuel use, or
  the receiver for a Dyson swarm's power beam.

If the cooling feed stops, the magnet quenches: the bonus stops (and maybe a short cooldown), never
anything destructive.

## Milestone 2: Satellite imagery

A **Survey Satellite** plus an imaging terminal (part of Mission Control or its own block). Pick a
structure type (village, woodland mansion, ocean monument, desert or jungle temple, pillager
outpost, trail ruins, modded structures by tag), wait for the imaging pass, and receive the
coordinates or a map item with the location marked, like an explorer map.

Balance ideas: each request takes time and power, range is limited (a few thousand blocks around
the terminal), and only surface structures are visible. Underground ones (ancient cities,
strongholds) need a ground penetrating radar upgrade, or stay out of reach.

This uses the same lookup as `/locate` and explorer maps, so it is cheap to build and immediately
useful in survival.

## Later (recorded, not scheduled)

* **Space Hub.** An orbital station where cargo arrives instead of a ground receiver, possibly its
  own dimension with a custom sky that players can visit and expand. Needs a way for players to get
  there and back.
* **Dyson swarm.** Launch segments over time; power scales with how much of the swarm is built and
  can exceed fusion when large. Received on the ground by a beam receiver.
* **Orbital Display globe.** The custom globe screen from `phase1.md`, colour coded by category.
  Worth it once there are several kinds of satellites.
* **WarSats.** Orbital strikes as PvE and spectacle, with server config for block damage.
* **Risk.** Launch failures, debris. Explicitly out for now.

## Open questions

1. Basic tier solid propellant: what should it be made from? (Proposal: Mekanism sulfur dust plus
   charcoal plus gunpowder, made in a Mekanism machine, so it is early but not free.)
2. Miner delivery rhythm: a batch every few in game hours for a fixed number of batches, or a
   continuous trickle while healthy?
3. Carbon: give out Mekanism's enriched carbon item directly, or add our own raw carbon item?
4. Should Milestone 1 already include the Superconducting Separator, or come right after?
