# Milestone 3 build plan: satellite imagery

Status: built and merged; the terminal screen seen in a headless client, the rest in game tests. Playtest: section 8 of
`docs/testing-guide.md`. Builds on `space-age.md` and the finished Milestones 1 and 2. Guiding
rule from the user: do not overengineer.

## Gameplay

1. Launch **Survey Satellites** to **Earth orbit** (Basic rocket and up). Every satellite you own
   joins your **constellation**; nothing to link or configure.
2. The constellation's **coverage** grows with every satellite, more for better tiers, and never
   reaches 100 percent. Coverage decides how **precise** a result is, how **far** the search reaches,
   and how many requests run **at once** (one per satellite).
3. At an **Imaging Terminal** (powered ground block), pick a structure type and request a survey. After
   the imaging pass the terminal logs the result: an estimated location with an uncertainty radius
   that shrinks as coverage grows. From **95 percent coverage** results are exact coordinates.
4. **The log remembers what was found.** A new request for the same type finds the next nearest
   unclaimed structure instead of the one you already looted. Old entries stay in the log until
   dismissed.
5. Put a **Survey Locator** (a tech compass, no power needed) into the terminal to load log entries
   onto it. Held in hand, its needle points to the selected target and a waypoint beam marks the
   spot in the world. Sneak use cycles between stored targets.
6. Underground structures (ancient cities, strongholds, trial chambers, mineshafts) need a **SQUID
   Array** next to the terminal: a superconducting ground module cooled with liquid nitrogen, like
   the Separator; without it they are not searchable at all. Dungeons (the small spawner rooms) are
   world features, not structures, so nothing can locate them.
7. A constellation is shared with your team: your satellites plus those of players who trust you
   in Mekanism's Security Desk.

There is no orbit over the Nether (a bedrock ceiling, and hell) or the End (already an asteroid field
with nothing to orbit). Satellites do not wear out in this milestone.

## Coverage model

Each Survey Satellite adds coverage value by tier: Basic 1, Advanced 2, Elite 4, Ultimate 8 (open
question 1). With total value `V`:

```
coverage  = 1 - e^(-V / 8)                      asymptotic, never 100 percent
precision = 512 blocks * (1 - coverage)         uncertainty radius; exact from 95 percent coverage
range     = 1000 + 15000 * coverage blocks      how far around the terminal it searches
parallel  = number of satellites                requests in progress at once
```

| Constellation | Value | Coverage | Uncertainty | Range |
| --- | --- | --- | --- | --- |
| 1 Basic | 1 | 12% | 452 blocks | 2800 |
| 4 Basic | 4 | 39% | 311 | 6900 |
| 3 Elite | 12 | 78% | 114 | 12700 |
| 6 Elite | 24 | 95% | 25 | 15300 |
| 4 Ultimate | 32 | 98% | 9 | 15700 |

Below 95 percent the reported location is the true one shifted by a random offset inside the
uncertainty radius,
fixed per result, so the Locator leads into the right area and the last stretch is exploring.
Launching more or better satellites later does not sharpen old results; a new request does.

## Strategic decisions

Each says what, why, and what it keeps open.

### 1. Orbital assets get a typed payload state

`OrbitalAsset` today mixes the generic part (id, owner, name, design, destination, receiver, state,
timers) with miner fields. It splits into the generic core plus a `PayloadState`, a sealed interface
with one record per payload kind (`MinerState`, `SatelliteState`; later scoops, Dyson segments,
WarSats), saved through a codec that dispatches on `PayloadType`. Ticking moves into the state, so
the registry stops growing a branch per payload. The registry gets a `data_version`; today's saves
load as `MinerState`, so existing miners survive. Doing this at the second payload is cheap; at the
fifth it is a rewrite.

### 2. Coverage is a constellation property, computed not stored

Coverage is derived on demand from the owner's active satellites over a dimension, never saved.
Adding, losing or decommissioning a satellite changes it immediately with nothing to migrate, and a
future change to the formula or tier values (or a datapack override) applies to existing worlds.
The same shape fits later constellations: a Dyson swarm's output is "segments built", computed the
same way.

### 3. Orbits only over dimensions that allow them

Destinations gain a `launch_from` list of dimensions. `cryosat:earth_orbit` allows only the
Overworld; Nether and End never get orbits. Modpacks with other planet mods can add orbits over their
dimensions with a datapack file. Satellites remember the dimension they orbit.

### 4. Requests are tasks on the asset, results are delivered like cargo

A survey request is an `OrbitalTask` (requester, requesting terminal, target type, completion time,
result) held by the satellite working on it. When it completes, the result goes to the terminal while
its chunk is loaded, otherwise it waits and is retried, never lost: the same rule as cargo. The queue
is generic so later orbital work (Dyson beam requests, Space Station operations) reuses it.

### 5. Survey targets are datapack driven

`data/<ns>/cryosat/survey_target/*.json` with the structures (a tag or id, what `/locate` takes),
whether it needs a deep scan (SQUID Array), a display name and an icon item. CryoSat ships the
vanilla set; modded structures are a file each.

### 6. Vanilla's structure search, and vanilla's "already found" mark

Results come from the lookup behind `/locate` and explorer maps, run once when a pass completes,
bounded by the constellation's range and limited to a few per tick server wide. With "skip known
structures" on, vanilla marks each found structure as claimed (one claim per structure), which is how
two explorer maps never lead to the same place. That gives the log's "find the next unclaimed one"
for free. The mark is world wide and shared with explorer maps, which is accepted.

### 7. A locator item instead of maps

The Survey Locator stores a small list of targets (dimension, position, uncertainty radius, type)
as an item data component. The needle uses vanilla's compass logic aimed at a stored position and a
client side waypoint beam renders at the target. No energy. Data on the item, not the player, so locators can be handed to
other players or kept for different targets. Map mod waypoint integration (JourneyMap, Xaero) can be
added later as optional compat on top of the same data.

### 8. Ground modules by adjacency, kept simple

Anything that needs coolant stays on the ground next to the block that receives results: the SQUID
Array next to the terminal, as the Separator sits next to the Cargo Receiver. Each receiving block
asks its six neighbours for the one small interface it cares about (`ICargoProcessor`,
`IDeepScanModule`); no shared module framework until a third case needs one.

### 9. Networking follows the Mekanism container pattern

The terminal syncs its target list, queue, coverage figures and log through Mekanism container sync,
and sends actions through one payload checked against the open container and ownership, like Mission
Control. A client side picture of orbital assets for the globe view is a separate, later sync.

### Not in this milestone

**WarSats** move to the Space Station update: they need targeting, damage rules and server
configuration that fit better once players have a station to operate them from. The task queue and
orbit data built here are what they will need.

## Build steps

### Foundation (done)

| Piece | What it is |
| --- | --- |
| `PayloadState` (`MinerState`, `SatelliteState`) | Per payload state with its own tick, saved by a codec dispatching on `PayloadType`. `OrbitalAsset.miner()` and `.satellite()` give typed access. Old saves (miner fields on the asset) load as `MinerState` |
| `SatelliteState` | Orbit dimension, cosmetic `Orbit`, at most one `OrbitalTask`. Travelling, then `ORBITING`. `passTicks(tier)` 4 to 1 minutes, `coverageValue(tier)` 1, 2, 4, 8 |
| `OrbitalTask`, `ISurveyTerminal` | A pass for a terminal; when done it is offered to the `ISurveyTerminal` at the terminal position while loaded, kept until accepted, dropped if the terminal is gone |
| `Constellation` | `of(server, viewer, dimension)`: own satellites plus those of owners who trust the viewer in Mekanism's Security Desk. `coverage()`, `uncertainty()` (0 from 95 percent), `range()`, `free()` |
| `OrbitalRegistry.requestSurvey` | Queues a pass on a free satellite of the requester's constellation |
| `Destination.launch_from`, `cryosat:earth_orbit` | Earth orbit only from the Overworld; `MissionProblem.WRONG_DIMENSION`; the Launch Pad passes its dimension |
| `SurveyTarget`, `SpaceData.SURVEY_TARGETS` | Datapack type in `data/<ns>/cryosat/survey_target/` (structures tag or id, `deep_scan`, `icon`), resolved against the world's structure registry |
| `IDeepScanModule` | What the SQUID Array implements for the terminal |
| `LocatorData`, `CryoDataComponents.LOCATOR_DATA` | Up to 8 results (position, uncertainty, target id) and the selected one, on the Survey Locator item |
| `CryoItems.SURVEY_SATELLITES` | The four tiered Survey Satellite payload items (no textures or recipes yet) |

Tests: `gametest/SatelliteTests`.

### Parallel workstreams

* **A. Imaging Terminal**: block, GUI (targets, coverage, queue, log with dismiss), requests, energy
  cost, implements `ISurveyTerminal` and runs the structure search when a pass arrives (range, the
  per tick limit, the uncertainty offset), deep targets only with an adjacent `IDeepScanModule`,
  loading log entries onto a Locator slot, redstone repeat. Game tests: search finds a placed structure, the second request skips
  it, results wait while the terminal is unloaded.
* **B. Survey Locator**: item with stored targets, compass needle, sneak cycling,
  client waypoint beam, textures.
* **C. Survey targets and SQUID Array**: the vanilla target set as data files with lang names, and
  the SQUID Array ground module (`IDeepScanModule`) with a `Cryostat`.
* **D. Satellite visuals**: Survey Satellite item textures; a satellite fairing on the rocket model
  when it carries one.

## Decisions from the review

1. Tier values 1, 2, 4, 8; coverage drives precision, range and parallel requests.
2. Exact coordinates from 95 percent coverage; below that a search circle.
3. Vanilla's world wide "already found" mark; keep it simple.
4. Locator: needle plus waypoint beam, no energy.
5. Ancient cities, strongholds, trial chambers and mineshafts need the SQUID Array and are not
   searchable without it.
6. Constellations are shared through Mekanism's trusted players.
