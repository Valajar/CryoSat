# CryoSat playtest guide

What to try in game, what you should see, and what to report back. Everything here was checked by
automated game tests and a quick look in a headless client, but nobody has *played* it yet, so the
interesting results are the ones that feel wrong.

## Setup

* Latest build: <https://github.com/Valajar/CryoSat/releases/tag/dev> (the jar name contains the commit).
* Needs NeoForge 1.21.1 and **Mekanism 10.7.19 or any later 10.7.x**. Mekanism Generators is
  recommended (Heat Generators, Thermoelectric Boiler). JEI is optional but makes this much easier.
* A creative world is fastest. Useful items: `mekanism:creative_energy_cube`, Mekanism cables,
  `mekanism:basic_pressurized_tube`, `mekanism:basic_chemical_tank`, `mekanism:basic_thermodynamic_conductor`,
  `mekanism:configurator`, `mekanism:speed_upgrade`, `mekanism:energy_upgrade`.
* Everything CryoSat adds is in the **CryoSat** creative tab.

## 1. Atmospheric Compressor (about 10 minutes)

1. Place it outdoors on the ground. Power it from the back or bottom (cable from a creative energy
   cube). Put a pressurized tube on top leading into a chemical tank.
2. Open it. Expect: **Status: Running**, **Output about 10 mB/t** near sea level, **Energy 10 J/mB** (shown as 4 FE/mB
   if your energy unit is FE). The tank fills with **Compressed Air**.
3. Cover all six faces (the tube on top counts as covered): status changes to **Needs open air
   around it**. Only the six faces count, edges and corners do not. One open face gives half
   output, two or more give full output.
4. Move it high up (y 250 or so): output drops towards 40 percent of the sea level rate.
5. Upgrades: add Speed upgrades through the **upgrade tab** (output should rise, 4 upgrades about
   3.16x), then Energy upgrades (energy per mB should fall). Also try **sneak right clicking the
   block with an upgrade in hand**: this is the one thing not confirmed yet, please tell me if it works.
6. Side config tab: move the chemical output to another face and check the tube on top stops
   receiving. Use the Configurator to do the same in world.
7. Break it and place it again: energy, contents, upgrades and side config should all be kept.
8. Optional: in the **Nether** it should produce Mekanism **Sulfur Dioxide** at half rate; in the
   **End** it shows **No atmosphere here**.

## 2. Liquefaction Plant: forming (about 5 minutes)

1. Build a hollow **3x3x3** box of **Cryo Casing**. The 12 edges and 8 corners must stay casing.
2. Swap the centres of four faces for: the **Liquefaction Controller**, a **Cryo Valve**, a
   **Cryocooler** and a **Heat Rejection Port** (the top centre is fine for one of them). You can also
   use **Mekanism Structural Glass** in face centres.
3. Right click any part: the **Liquefaction Plant** GUI should open showing **Idle, Temp ~300 K,
   Coolers: 1**. If it does not open, the structure did not form: check the edges.
4. Try a bigger box (5x5x5 up to 9x9x9) with several cryocoolers. Bigger plants hold more
   and process faster, but leak more heat.

## 3. Liquefaction Plant: cooling and liquefying (about 15 minutes)

1. Power the plant with a cable into any Cryocooler or Cryo Valve (any side): all coolers share one
   energy buffer, shown as the bar on the right of the GUI and in the energy tab. Watch **Temp** fall
   and **Hot side** rise.
2. Feed **Compressed Air** into a Cryo Valve (valves start in **Input** mode). Sneak right click
   other valves to cycle them to **Output 1 / 2 / 3** and pipe each into its own chemical tank.
3. Once **Temp** reaches **77 K**, status becomes **Liquefying**: Output 1 fills with **Liquid
   Nitrogen**, Output 2 with **Liquid Oxygen**, Output 3 with **Liquid Argon** (78 / 21 / 1).
4. **Heat test:** leave the Heat Rejection Port with nothing attached. Hot side keeps climbing;
   at **1200 K** the status becomes **Hot side overheated!** and cooling stops. Then attach
   Thermodynamic Conductors to the port (a short line into the air, or into a Thermoelectric
   Boiler from Generators): hot side should drop and cooling should resume.
5. **Oxygen and hydrogen:** feed Mekanism Oxygen (90 K) or Mekanism Hydrogen (20 K) from an
   Electrolytic Separator instead of air. The **Needs** line shows the temperature the input needs;
   until the plant gets there the status stays **Cooling down** and the gas waits in the input
   tank. Each product goes to the output tank already holding it or the first empty one, so
   leftovers from an air run do not block hydrogen as long as one tank is free; with none free
   the status says **No free output tank, drain one**. Hydrogen is meant to be hard: note how big a plant and how many coolers you needed, how
   much power it took, and how you kept the hot side from overheating.

## 4. JEI (2 minutes)

* Hover the Liquefaction Controller and press **U**, or a Liquid Nitrogen tank and press **R**:
  a **Liquefaction Plant** page shows each recipe with its temperature limit and heat released,
  and all plant parts as the machines that make it.
* Known gap: atmospheres (what the compressor produces where) are not in JEI yet.

## 5. Optional: datapack recipes

Recipes are plain JSON now. Drop a datapack into the world with, for example,
`data/test/recipe/liquefaction/nitrogen_gas.json` and run `/reload`; it should appear in JEI and work
in the plant. The built in ones are a good template:
`cryosat:liquefaction/compressed_air`, `.../oxygen`, `.../hydrogen`.

## 6. Space: asteroid mining (about 20 minutes)

Commands that help: `/cryosat orbit skip` (operator) makes every miner reach its next arrival or
batch right away; `/cryosat rocket preview <tier>` and `/cryosat rocket preview launch` (creative,
client only) show a rocket on the block you look at, for screenshots.

1. **Cargo Receiver and Uplink Card.** Place a Cargo Receiver. Right click it with an Uplink Card:
   the action bar confirms and the card lights up blue. Sneak right click in the air clears it.
2. **Launch Pad.** Build a flat 5x5 of Launch Pad Plating with one Launch Controller and at least
   one Launch Pad Fuel Port on the outer ring. The 3x3 column above the centre must be clear for 6
   blocks. Right click the controller (or a fuel port): the Rocket Assembly screen opens.
3. **Assemble.** Shift click a Basic Rocket Engine, Basic Fuel Tank, Guidance Computer and Basic
   Asteroid Miner into the four part slots, the Uplink Card into its slot and 16 Solid Propellant
   Grains into the fuel slot (or pipe them into a fuel port). Press **Assemble**: the rocket appears
   on the pad in Basic green. **Dismantle** gives the parts back.
4. **Launch.** Press **Launch** (or give the controller a redstone pulse). Expect about 2 seconds of
   engines on the pad, then a climb out of sight over the next 8 seconds, smoke that lingers, and a
   chat line "<you> launched Basic Miner #1". The fuel is used up and the pad is empty afterwards.
5. **Mission Control.** Place one and open it: your miner is listed as Travelling (about 2 minutes),
   then Mining. Select it to see the asteroid's class, size and hardness, how much the miner can
   extract, batches delivered and time to the next batch. Try renaming it, Unlink and Relink (card in
   the slot), and Decommission on a second miner.
6. **Deliveries.** Batches arrive every few minutes (or use `/cryosat orbit skip` repeatedly). A
   small asteroid gives 6 to 10 stacks of raw iron, 4 to 6 stacks of Enriched Carbon and 2 stacks
   each of raw gold and raw osmium over 3 batches. Fill the receiver up: the rest waits in orbit
   (Mission Control shows it) and arrives once there is room. A redstone signal pauses the receiver.
7. **Advanced and Elite.** Advanced rockets burn Liquid Oxygen plus Mekanism Ethene, Elite rockets
   Liquid Oxygen plus Liquid Hydrogen (4000 mB each, 8000 mB hydrogen), piped into a fuel port.
   Asteroid size odds: Basic rockets 70% small, 30% medium; Advanced 25% small, 50% medium, 25%
   large; Elite 15% medium, 55% large, 30% huge. A miner of the same tier as the rocket always
   extracts the whole asteroid; a smaller miner only extracts part of a big one.

## What to report

Copy this and fill in what applies:

```
Mekanism version / other mods:
Compressor: works? numbers as expected? upgrade by sneak right click works?
Plant formed at sizes:
Time to reach 77 K, plant size, number of coolers, power source:
Hydrogen (20 K) reached? plant size, coolers, power:
Overheating: happened? how long without heat rejection? conductors fixed it?
Anything confusing in the GUIs or tooltips:
Balance feel (too fast / too slow / too expensive / too cheap):
Space: pad formed? launch looked right? deliveries arrived? Mission Control readable?
Bugs (attach logs/latest.log for crashes or errors):
```

Numbers worth commenting on, all easy to change: 10 J per mB of compressed air; cooler 200 heat/t
max; 100 kJ of plant energy buffer per cooler; hot side 50000 heat per K per Heat Rejection Port;
plant interior 1000 heat per K per interior block; heat leak 0.02 per casing and 0.08 per glass
per Kelvin; overheat at 1200 K; liquefying up to 20 mB/t per interior block. Space: 16 grains per
Basic launch, travel 2 minutes, a batch every 5 minutes at hardness 1, small asteroid yields above
(data file `data/cryosat/cryosat/asteroid/chondrite.json`).
