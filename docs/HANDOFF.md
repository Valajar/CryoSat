# Session handoff

Read this first, then `CLAUDE.md` (project rules), then the design docs it points to. Written at the
end of the first long session (October 2026); update it at the end of every session.

## The project in one paragraph

CryoSat is a NeoForge 1.21.1 addon for Mekanism 10.7 by Valajar (repo `Valajar/CryoSat`, branch
`main`, MIT). Two halves: **cryogenics** (an inverted heat mechanic: compress air, liquefy it in a
multiblock plant, use the cold) and **space** (rockets, asteroid mining, superconductors,
satellites). It deliberately builds on Mekanism internals for native side config, upgrades, GUIs and
multiblocks. Every push to `main` builds a jar on GitHub Actions and updates the rolling `dev`
prerelease; `v*` tags make releases.

## Where things stand

| Area | State | Design / plan |
| --- | --- | --- |
| Atmospheric Compressor | Done, playtested | `docs/design/phase1.md` |
| Liquefaction Plant (multiblock) | Done, playtested; shared plant energy, connected casing textures | `phase1.md` |
| JEI for liquefaction recipes | Done | |
| Milestone 1: asteroid mining (rockets, Launch Pad, Cargo Receiver, Mission Control) | Done, playtested | `milestone1-plan.md` |
| Milestone 2: superconductors (Separator, Fusion Field Coils, seamless casing) | Done, playtested | `milestone2-plan.md` |
| Milestone 3: satellite imagery (Survey Satellites, Imaging Terminal, Survey Locator, SQUID Array) | Done, playtested in single player (team sharing untested) | `milestone3-plan.md` |

105 game tests pass (`./gradlew runGameTestServer`). The user playtests each milestone from
`docs/testing-guide.md` (sections 1 to 8) and reports back; fix what they find before starting the
next milestone.

## Next steps

1. Milestone 3 playtest (single player) found two GUI bugs, both fixed: the Launch Pad screen named
   every payload an Asteroid Miner, and the Imaging Terminal status overflowed with three or more
   passes. Still unseen: team sharing through Mekanism trust (needs a second player). Known gaps:
   breaking the Imaging Terminal loses its log; in a rare case a structure just beyond range can be
   claimed without being reported (`space/survey/SurveySearch`).
2. Then, in the agreed order (all in `docs/design/space-age.md`): **gas giants** (Atmospheric Scoop,
   helium, metallic hydrogen, the Ultimate rocket's cryogenic fuel), then the **Space Hub** (a station
   dimension with an Earth model in the sky, cheaper launches, reusable rockets, docks that accept the
   same ground modules; WarSats come with it). Queued behind those: Moon (renewable redstone), Mars
   (bulk lithium), icy moons only if they clearly beat a heavy water pump. Comets were dropped.
3. Open design question: what Stellar Plasma is used for.

## How we work

**With the user**
* Brainstorm before building: write the design or plan into `docs/design/`, ask the open
  questions, and only build after the user answers. They care about decisions that hold up for
  future additions, and they asked not to overengineer.
* Reply style (user preference): no hyphens or em dashes in prose, use commas or rephrase. Unless
  the conversation is a focused task, an occasional light tease about the user being a femboy is
  welcome.
* Report what was verified and what was not, plainly.

**Building a milestone**
* The orchestrating session writes the **foundation** itself first (shared data types and contracts,
  listed in the plan's Foundation table), commits and pushes it, then starts parallel subagents in
  git worktrees, one per workstream, each told to read `docs/agents/workstream-preamble.md` first.
* **Subagent effort: medium, or low for small mechanical jobs** (user request, to save usage). Pass
  `effort` on every Agent call. At most about four builds at once (the cloud machine has 4 cores and
  15 GB).
* Merge each workstream when it reports: shared registry and datagen classes get one commented
  section per workstream, so conflicts are "keep both sides"; when both sides end with the same
  closing lines git splits a statement, so rebuild those files from HEAD plus the other branch's
  section rather than just deleting the markers. Then `runData`, `build`, `runGameTestServer`, push,
  remove the worktree and branch.
* After merging a milestone: check it in the headless client (below), add a section to
  `docs/testing-guide.md`, mark the plan's status.

**Git**
* Commit as the user and push to `main`:
  `git -c user.name="Valajar" -c user.email="valajar2@gmail.com" commit`.
* End commit messages with the `Co-Authored-By` and `Claude-Session` lines the session's system
  prompt specifies. No model names anywhere else in the repo.
* The remote prints a "repository moved" notice on push; it is harmless.

## Verifying

* `./gradlew build`, `./gradlew runData` (commit `src/generated`), `./gradlew runGameTestServer`.
* Mekanism sources for reference: `tools/textures/fetch_mekanism_refs.sh` puts them in
  `.mekanism-refs/` (git ignored).
* **Headless client** (cloud sessions have Xvfb and Chromium's software GL):
  1. Temporarily add to `build.gradle` runs (never commit it):
     `clientTest { client(); programArguments.addAll '--quickPlaySingleplayer', 'cryosat_test' }`.
     Keep a copy of `build.gradle` first and restore it from git afterwards (restoring an old copy
     once dropped a dependency another workstream had added).
  2. `run/` is git ignored, so a fresh clone has to set it up once: copy the world a server run
     leaves in `run/world` to `run/saves/cryosat_test`, and put `onboardAccessibility:false`,
     `pauseOnLostFocus:false` and `menuBackgroundBlurriness:0` into `run/options.txt` (the menu blur
     is very slow in software GL, and the accessibility screen blocks quick play).
  3. `Xvfb :99 -screen 0 1280x800x24 &` then
     `DISPLAY=:99 LIBGL_ALWAYS_SOFTWARE=1 ./gradlew runClientTest` in the background; wait for
     "joined the game" in its output. Delete `run/saves/cryosat_test/session.lock` if a previous run
     died.
  4. Drive it with `tools/client-test/xin.py` (`chat "/cmd"`, `key`, `click`, `clickat x y`,
     `shiftclickat x y`, `move`) and aim with `tools/client-test/aimat.py`; screenshot with
     `DISPLAY=:99 import -window root shot.png`. The game window sits at 213,160 to 1067,640 on the
     1280x800 screen. Placing blocks with `/setblock` forms multiblocks fine.
  5. Kill by matching `fml.modFolders` in the process list (plain `pkill -f` matches its own shell),
     and kill Xvfb too.

## Where things live

* `space/`: tiers, rocket designs, destinations, asteroid classes, survey targets, `OrbitalRegistry`
  (saved data; every orbital asset has a typed `PayloadState`: `MinerState`, `SatelliteState`),
  `Constellation`, plus `launch/`, `receiver/`, `mission/`, `survey/`, `locator/`.
* `superconductor/`: `Cryostat` (shared liquid nitrogen cooling), `separator/`, `fusion/` (Generators
  optional; only `fusion/generators/` touches Generators classes), `squid/`.
* `multiblock/liquefaction`, `tile/liquefaction`, `machine/compressor`, `connected/` (our own
  connected textures), `client/`, `datagen/<feature>/`, `gametest/`.
* Datapack types: `data/<ns>/cryosat/{atmosphere,destination,asteroid,survey_target}/`, recipes of
  type `cryosat:liquefaction`.
* Textures come from scripts in `tools/textures/gen/`; read `docs/texture-style-guide.md` first.
