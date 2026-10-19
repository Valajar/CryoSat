# Milestone 3 build plan: satellite imagery

Status: plan, waiting for the user's go ahead. Builds on `space-age.md` (Milestone 3) and the
finished Milestones 1 and 2.

The feature itself is small: launch a Survey Satellite, ask a ground terminal for a structure type,
wait for the imaging pass, get a marked map. Most of this plan is about the decisions underneath it,
because satellites are the first payload that is not a miner, and WarSats, the orbital globe, the
Dyson swarm and the Space Hub all build on the same ground.

## Gameplay

1. Build a Survey Satellite payload and launch it to **Earth orbit**, a new destination (Basic
   rocket and up, short travel).
2. Place an **Imaging Terminal** (ground block, powered). It lists the survey targets your satellites
   can image, with the range of your best satellite.
3. Pick a target (village, ocean monument, woodland mansion and so on), put a blank map in, press
   Request. The request queues on one of your satellites, takes a few minutes, and costs energy.
4. When the pass completes the terminal prints an **explorer style map** marking the nearest match
   within range, and logs its coordinates in a history list. No match in range is a result too ("none
   within 4000 blocks"), so the request is never silently lost.
5. Underground structures (ancient cities, strongholds, trial chambers) need a **SQUID Array** next
   to the terminal: a superconducting block cooled with liquid nitrogen, like the Separator.

| Satellite tier | Range around the terminal | Requests at once | Time per request |
| --- | --- | --- | --- |
| Basic | 2000 blocks | 1 | 4 minutes |
| Advanced | 4000 | 2 | 3 minutes |
| Elite | 8000 | 3 | 2 minutes |
| Ultimate | 16000 | 4 | 1 minute |

Satellites do not wear out in this milestone (risk stays out, per the design decisions). Players
decommission them in Mission Control.

## Strategic decisions

Each decision says what it is, why, and what it keeps open for later.

### 1. Orbital assets get a typed payload state

**Now:** `OrbitalAsset` mixes the generic part (id, owner, name, design, destination, receiver,
state, timers) with miner fields (`asteroid`, `batchesDelivered`, `pending`). A satellite needs none
of the miner fields and has its own (task queue, orbit, coverage).

**Decision:** split it. `OrbitalAsset` keeps the generic part plus a `PayloadState payload`, a sealed
interface with one record per payload kind, saved through a codec that dispatches on
`PayloadType` (`MinerState`, `SurveyState`; later `ScoopState`, `WarSatState`, `DysonSegmentState`).
Ticking moves into the state (`tick(context)`), so `OrbitalRegistry` stops growing a branch per
payload.

**Why now:** every later payload would otherwise add nullable fields to one class and branches to
the registry. Doing it at the second payload is cheap; at the fifth it is a rewrite.

**Save compatibility:** the registry gets a `data_version`. Version 1 saves (today's) are read with
the old codec and turned into `MinerState`, so existing worlds keep their miners.

### 2. A generic task queue on assets

**Decision:** an imaging request is an `OrbitalTask`: who asked, from which terminal (a `GlobalPos`),
what (a task specific payload, here the survey target id), when it completes, and its result.
Tasks live in the asset's state and are saved with it. Results are delivered to the requesting block
the same way cargo is: while its chunk is loaded, otherwise kept and retried, never lost.

**Why:** WarSat strikes are the same shape (target position, charge time, result at a terminal), and
so are future Dyson beam requests. One queue and one delivery path serve all of them.

### 3. Orbits are per dimension, Earth orbit first

**Decision:** a satellite orbits the dimension it was launched from (`orbitDimension` on the asset),
and the Imaging Terminal only uses satellites over its own dimension. The new destination
`cryosat:earth_orbit` is valid from the Overworld. Nether or End orbits are a datapack question
later (a destination can list the dimensions it is reachable from).

**Why:** structure lookups are per level, and it keeps "satellite imagery of the Nether" a deliberate
future addition instead of an accident.

### 4. Cosmetic orbit parameters stored from day one

**Decision:** every asset launched to an orbit gets an `Orbit` record rolled at launch: altitude
band, inclination, phase, period. Nothing in Milestone 3 uses it for gameplay (orbits stay abstract,
per the design decisions).

**Why:** the Orbital Display globe (orbit lines around an Earth model, colour coded by role) needs
stable orbits for assets that already exist. Storing them now costs a few bytes and avoids a
migration when the globe lands. Orbital strikes can later use the phase for "next pass over target"
spectacle without making it mechanics.

### 5. Survey targets are datapack driven

**Decision:** `data/<ns>/cryosat/survey_target/*.json`:

```json
{
  "structures": "#minecraft:village",
  "min_tier": "basic",
  "deep_scan": false,
  "icon": "minecraft:village_plains",
  "name": "Village"
}
```

`structures` is a structure tag or id (what `/locate` and explorer maps take), `deep_scan` marks
underground targets that need the SQUID Array, `icon` is a vanilla map decoration type for the map
marker. CryoSat ships the vanilla set; modpacks add modded structures with a file each.

**Why:** the same reason as atmospheres and asteroid classes: modded structures without code, and
balance changes without a release.

### 6. Use vanilla's structure search, bounded and spread out

**Decision:** results come from `ServerLevel.findNearestMapStructure`, the lookup behind explorer
maps and `/locate`, with the search radius taken from the satellite tier (blocks to chunks). It
runs once, when the imaging pass completes, never while the player waits on a screen, and at most a
few per tick across the server (completed tasks queue up and are resolved in order). The result
(position, structure id) is stored on the task, so reopening a GUI or printing another copy never
searches again.

**Why:** reuses the exact behaviour players know from explorer maps, supports every modded
structure that registers properly, and keeps worst case cost bounded on servers.

### 7. Results are vanilla maps plus a coordinate log

**Decision:** the terminal fills a blank map into a vanilla explorer style map
(`MapItem.create` plus a target decoration, like the cartographer's maps), and keeps a scrollable
history of results with coordinates and distance. No custom map item.

**Why:** vanilla maps work with map mods, item frames, copying in a cartography table, and need no
custom rendering. Coordinates cover players who just want the numbers.

### 8. Ground station modules by adjacency

**Decision:** the SQUID Array is a block placed next to the Imaging Terminal, with its own `Cryostat`
(trickle plus a fixed amount per deep scan), exactly like the Separator next to a Cargo Receiver.
Both follow one rule: *orbital assets stay simple; ground blocks next to the receiving block add
processing*. A shared `IGroundModule` lookup (the terminal or receiver asks its six neighbours)
replaces the receiver's private loop.

**Why:** coolant cannot be refilled in orbit, so anything superconducting belongs on the ground. The
same pattern will carry WarSat targeting computers, a Dyson beam receiver's converters, and Space
Hub docks, which accept the same modules.

### 9. One request path for players and automation

**Decision:** the terminal accepts requests from its GUI and from a redstone pulse (repeat the last
request), and it keeps a small queue. Computer integration is left for later but the request is a
plain record, so a ComputerCraft peripheral can be added without touching the task system.

### 10. Networking follows the Mekanism container pattern

**Decision:** like Mission Control, the terminal syncs its lists through Mekanism container sync
(target list, queue, history), and sends actions through one small payload checked against the
player's open container and ownership. No global client cache of orbital data in this milestone.

**Why:** the globe view will need a client side picture of orbital assets; that is a separate,
read only sync added with the globe, and should not be shaped by a terminal list.

## Build steps

### Foundation (one agent or me, before the parallel work)

* Decisions 1 to 4: `PayloadState` with `MinerState` (moved miner logic, behaviour unchanged) and
  `SurveyState`; `data_version` and the version 1 migration; `OrbitalTask` and result delivery;
  `orbitDimension`; `Orbit` rolled at launch; `cryosat:earth_orbit` destination;
  `PayloadType.SURVEY_SATELLITE` and the four tiered Survey Satellite items.
* Mission Control and the Cargo Receiver adapted to the split, with no visible change.
* `IGroundModule` lookup, with the Separator moved onto it.
* Game tests: an old format save loads into `MinerState`; all existing tests still pass unchanged;
  a satellite launches to Earth orbit and accepts a task.

### Parallel workstreams

* **A. Imaging Terminal**: block, GUI (target list, request queue, history), map filling, energy
  cost, redstone repeat, result delivery, structure search with the tier radius and the per tick
  limit. Game tests with a real village or structure placed by a test template if practical,
  otherwise against a test structure tag.
* **B. Survey targets data**: the datapack type and loader, the vanilla target set with names,
  icons and tiers, lang, and JEI display of what each satellite tier can find (optional).
* **C. SQUID Array**: the ground module block with a `Cryostat`, its GUI, texture, recipe, and the
  deep scan gate.
* **D. Satellite visuals**: item textures for the four satellites; the rocket model's payload
  section showing a satellite fairing when it carries one (the nose already takes the payload tier
  colour).

## Open questions for the user

1. Tier table: are the ranges, request counts and times above in the right ballpark?
2. Energy per request at the terminal: suggestion 50 kJ basic, scaling with range.
3. Which structures count as underground by default: ancient city, stronghold, trial chambers,
   mineshafts? Buried treasure seems too cheap to locate and could stay out entirely.
4. Should Mission Control show satellites the same way as miners (state, tasks waiting), or should
   satellites only appear in the Imaging Terminal?
