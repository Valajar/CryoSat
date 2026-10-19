# Milestone 3 build plan: satellite imagery

Status: plan, revised after the user's first review; waiting for answers to the questions at the
end. Builds on `space-age.md` and the finished Milestones 1 and 2.

## Gameplay

1. Launch **Survey Satellites** to **Earth orbit** (Basic rocket and up). Every satellite you own
   joins your **constellation**; nothing to link or configure.
2. The constellation's **coverage** grows with every satellite, more for better tiers, and never
   reaches 100 percent. Coverage decides how **precise** a result is, how **far** the search reaches,
   and how many requests run **at once** (one per satellite).
3. At an **Imaging Terminal** (powered ground block), pick a structure type and request a survey. After
   the imaging pass the terminal logs the result: an estimated location with an uncertainty radius
   that shrinks as coverage grows.
4. **The log remembers what was found.** A new request for the same type finds the next nearest
   unclaimed structure instead of the one you already looted. Old entries stay in the log until
   dismissed.
5. Put a **Survey Locator** (a tech compass) into the terminal to load log entries onto it. Held in
   hand, it points to the selected target, shows the distance and the search radius, and marks the
   spot in the world with a waypoint beam. Sneak use cycles between stored targets.
6. Underground structures (ancient cities, strongholds and so on) need a **SQUID Array** next to the
   terminal: a superconducting ground module cooled with liquid nitrogen, like the Separator.

There is no orbit over the Nether (a bedrock ceiling, and hell) or the End (already an asteroid field
with nothing to orbit). Satellites do not wear out in this milestone.

## Coverage model

Each Survey Satellite adds coverage value by tier: Basic 1, Advanced 2, Elite 4, Ultimate 8 (open
question 1). With total value `V`:

```
coverage  = 1 - e^(-V / 8)                      asymptotic, never 100 percent
precision = 512 blocks * (1 - coverage)         uncertainty radius of a result
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

The reported location is the true one shifted by a random offset inside the uncertainty radius,
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
for free. The catch: the mark is world wide and shared with explorer maps (open question 3).

### 7. A locator item instead of maps

The Survey Locator stores a small list of targets (dimension, position, uncertainty radius, type)
as an item data component. The needle uses vanilla's compass logic aimed at a stored position, the
action bar shows distance and radius while held, and a client side waypoint beam renders at the
target when within render distance. Data on the item, not the player, so locators can be handed to
other players or kept for different targets. Map mod waypoint integration (JourneyMap, Xaero) can be
added later as optional compat on top of the same data.

### 8. Ground modules by adjacency

Anything that needs coolant stays on the ground next to the block that receives results: the SQUID
Array next to the terminal, as the Separator sits next to the Cargo Receiver. A shared
`IGroundModule` lookup (the receiving block asks its six neighbours) replaces the receiver's private
loop, and Space Station docks will accept the same modules.

### 9. Networking follows the Mekanism container pattern

The terminal syncs its target list, queue, coverage figures and log through Mekanism container sync,
and sends actions through one payload checked against the open container and ownership, like Mission
Control. A client side picture of orbital assets for the globe view is a separate, later sync.

### Not in this milestone

**WarSats** move to the Space Station update: they need targeting, damage rules and server
configuration that fit better once players have a station to operate them from. The task queue and
orbit data built here are what they will need.

## Build steps

### Foundation (me, before the parallel work)

* Decisions 1 to 4: payload state split with migration, `data_version`, `launch_from` on
  destinations, `cryosat:earth_orbit`, cosmetic orbit parameters rolled at launch (for the future
  globe), `OrbitalTask` and result delivery, `PayloadType.SURVEY_SATELLITE`, the four Survey
  Satellite items, the coverage calculation.
* Mission Control and the Cargo Receiver adapted with no visible change; `IGroundModule` with the
  Separator moved onto it.
* Game tests: an old save loads into `MinerState`, every existing test passes unchanged, coverage
  maths, a satellite reaches Earth orbit and accepts a task, no launch from the Nether.

### Parallel workstreams

* **A. Imaging Terminal**: block, GUI (targets, coverage, queue, log with dismiss), requests, energy
  cost, structure search with range and the per tick limit, uncertainty offset, loading results onto
  a Locator, redstone repeat. Game tests: search finds a placed structure, the second request skips
  it, results wait while the terminal is unloaded.
* **B. Survey Locator**: item with stored targets, compass needle, action bar readout, sneak cycling,
  client waypoint beam, textures.
* **C. Survey targets and SQUID Array**: the datapack type, vanilla target set and lang, and the
  SQUID Array ground module with a `Cryostat` gating deep scans.
* **D. Satellite visuals**: Survey Satellite item textures; a satellite fairing on the rocket model
  when it carries one.

## Open questions

1. **Coverage numbers:** tier values 1, 2, 4, 8 and the curve above (8 value for 63 percent)? Should
   coverage drive all three of precision, range and parallel requests, or only some?
2. **What precision means in play:** a search circle the Locator leads you into (as above), or exact
   coordinates once coverage passes a threshold, say 95 percent?
3. **"Already found" scope:** use vanilla's world wide claim (simple; also hides our finds from
   cartographer maps and the other way round), or a per player log (each player can find the same
   structure; more work, our own search loop)?
4. **The Locator:** compass needle plus distance readout plus waypoint beam, or a subset? Should it
   run on Mekanism energy like other tech tools?
5. **Underground targets:** still behind the SQUID Array, and which ones: ancient city, stronghold,
   trial chambers, mineshaft?
6. **Sharing:** is a constellation per player, or shared with a team (for example everyone on the
   same Mekanism security frequency)?
