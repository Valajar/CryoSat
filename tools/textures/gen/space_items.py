"""Space items: rocket parts per tier, the guidance computer and the solid propellant chain.

Items follow Mekanism's item look (tier installers, control circuits, alloys): light item steel with a
darker outline on the bottom and right, and the tier shown by an accent in Mekanism's item tier colours.
Every tier of a part shares one silhouette; only the accent changes.

    <tier>_rocket_engine     combustion chamber over a bell nozzle, tier band around the chamber
    <tier>_fuel_tank         domed pressure tank with a valve, tier band around the middle
    <tier>_asteroid_miner    lander body wrapped in MLI gold, tier collar above a spiral drill
    guidance_computer        steel box with an amber trajectory screen, pins along the bottom
    propellant_mix           yellow grey powder pile, sulfur and carbon specks in it
    solid_propellant_grain   pressed khaki cylinder with a star shaped core

Run from the repo root:  python3 tools/textures/gen/space_items.py
"""
import math
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from texlib import *  # noqa: E402,F403

ITEMS = ASSETS / "item"
TIERS = ("basic", "advanced", "elite", "ultimate")

# Cylinder shading across a row, left to right: lit from the top left, falling off to the right.
STEEL_PROFILE = (7, 8, 9, 9, 8, 7, 6, 5, 4, 3)
TIER_PROFILE = (2, 3, 4, 4, 3, 2, 2, 1, 1, 1)


def clampi(i: int, n: int) -> int:
    return max(0, min(n - 1, i))


def steel(t: Tex, x: int, y: int, i: int, jitter: int = 2) -> None:
    t.px(x, y, shade(ITEM_STEEL[clampi(i, len(ITEM_STEEL))], t.rng.randint(-jitter, jitter)))


def profile_index(profile, x: int, x0: int, x1: int) -> int:
    f = 0.0 if x1 == x0 else (x - x0) / (x1 - x0)
    return profile[min(len(profile) - 1, int(f * len(profile)))]


def cyl_row(t: Tex, y: int, x0: int, x1: int, shift: int = 0, outline: bool = True) -> None:
    """One row of a lit steel cylinder. The outer pixels are the item outline, light left, dark right."""
    for x in range(x0, x1 + 1):
        if outline and x == x0:
            steel(t, x, y, 5 + min(shift, 0))
        elif outline and x == x1:
            steel(t, x, y, 1 + max(min(shift, 1), -1))
        else:
            steel(t, x, y, profile_index(STEEL_PROFILE, x, x0, x1) + shift)


def tier_row(t: Tex, y: int, x0: int, x1: int, ramp, shift: int = 0) -> None:
    """One row of a tier coloured band wrapped round a cylinder."""
    for x in range(x0, x1 + 1):
        if x == x1:
            i = 0
        else:
            i = profile_index(TIER_PROFILE, x, x0, x1) + shift
        t.px(x, y, shade(ramp[clampi(i, len(ramp))], t.rng.randint(-1, 1)))


# --- rocket parts ------------------------------------------------------------------------------------
def rocket_engine(tier: str) -> Tex:
    t = Tex(seed=f"{tier}_rocket_engine", transparent=True)
    ramp = ITEM_TIER[tier]
    # Injector flange
    cyl_row(t, 1, 4, 11, shift=1)
    cyl_row(t, 2, 4, 11, shift=-2)
    # Combustion chamber with the tier band
    cyl_row(t, 3, 5, 10)
    tier_row(t, 4, 5, 10, ramp, shift=1)
    tier_row(t, 5, 5, 10, ramp)
    cyl_row(t, 6, 5, 10, shift=-1)
    # Throat
    cyl_row(t, 7, 6, 9, shift=-2)
    # Bell nozzle, flaring out; every other column a little darker for the cooling tubes
    bell = ((8, 6, 9), (9, 5, 10), (10, 5, 10), (11, 4, 11), (12, 3, 12), (13, 2, 13))
    for y, x0, x1 in bell:
        cyl_row(t, y, x0, x1, shift=-(y - 8) // 3)
        for x in range(x0 + 1, x1):
            if (x - x0) % 2 == 0:
                c = t.get(x, y)
                t.px(x, y, shade(c, -14))
    # Exit lip
    cyl_row(t, 14, 2, 13, shift=-3)
    for x in range(3, 13):
        if x % 2 == 0:
            t.px(x, 14, shade(t.get(x, 14), -8))
    return t


def fuel_tank(tier: str) -> Tex:
    t = Tex(seed=f"{tier}_fuel_tank", transparent=True)
    ramp = ITEM_TIER[tier]
    # Valve and neck
    cyl_row(t, 1, 7, 8, shift=1)
    cyl_row(t, 2, 6, 9, shift=-2)
    # Upper dome
    cyl_row(t, 3, 5, 10, shift=1)
    cyl_row(t, 4, 4, 11, shift=1)
    # Body with weld seams and the tier band
    for y in range(5, 14):
        cyl_row(t, y, 3, 12, shift=-1 if y in (5, 12) else 0)
    tier_row(t, 8, 3, 12, ramp, shift=1)
    tier_row(t, 9, 3, 12, ramp)
    # Lower dome
    cyl_row(t, 14, 4, 11, shift=-3)
    return t


def asteroid_miner(tier: str) -> Tex:
    t = Tex(seed=f"{tier}_asteroid_miner", transparent=True)
    ramp = ITEM_TIER[tier]
    # Antenna and top hatch
    steel(t, 11, 0, 7)
    steel(t, 11, 1, 3)
    for x in range(6, 10):
        steel(t, x, 1, 8 if x < 9 else 4)
    # Body: steel frame with MLI gold foil inside
    for y in range(2, 8):
        for x in range(3, 13):
            if y == 2 or x == 3:
                steel(t, x, y, 8 if (x, y) != (3, 7) else 5)
            elif y == 7 or x == 12:
                steel(t, x, y, 2 if y == 7 else 3)
    # One wrapped foil panel, finely crinkled
    t.gold_foil((4, 3, 11, 6), cell=2)
    t.px(4, 3, GOLD[5])
    # Landing legs off the bottom corners
    steel(t, 2, 8, 6)
    steel(t, 1, 9, 4)
    steel(t, 13, 8, 3)
    steel(t, 14, 9, 1)
    # Drill mount, the tier collar
    cyl_row(t, 8, 4, 11, shift=-2)
    tier_row(t, 9, 5, 10, ramp, shift=1)
    tier_row(t, 10, 5, 10, ramp, shift=-1)
    # Spiral drill
    drill = ((11, 5, 10), (12, 5, 10), (13, 6, 9), (14, 6, 9), (15, 7, 8))
    for y, x0, x1 in drill:
        cyl_row(t, y, x0, x1, shift=-1)
        for x in range(x0 + 1, x1):
            if (x + y) % 3 == 0:
                t.px(x, y, shade(t.get(x, y), -50))
    return t


def guidance_computer() -> Tex:
    t = Tex(seed="guidance_computer", transparent=True)
    # Antenna
    steel(t, 11, 1, 7)
    steel(t, 11, 2, 4)
    # Casing
    for y in range(3, 13):
        for x in range(2, 14):
            if y == 3 or x == 2:
                steel(t, x, y, 8 if (x, y) != (2, 12) else 5)
            elif y == 12 or x == 13:
                steel(t, x, y, 2)
            else:
                steel(t, x, y, 6 if y < 8 else 5)
    # Screen bezel and screen
    t.outline((4, 5, 11, 10), ITEM_STEEL[1])
    t.line(4, 10, 11, 10, ITEM_STEEL[7])
    t.line(11, 5, 11, 10, ITEM_STEEL[7])
    t.fill((5, 6, 10, 9), VOID, 0, 3, jitter=1)
    # Trajectory: an amber arc from the launch site up to orbit, with the target blip
    for x, y in ((5, 9), (6, 8), (7, 7), (8, 7)):
        t.px(x, y, GOLD[3])
    t.px(9, 6, GOLD[5])
    t.px(10, 6, GOLD[2])
    # Glare speck
    t.px(10, 9, PANEL[1])
    # Pins along the bottom, like Mekanism's circuits
    for x in (3, 5, 7, 9, 11):
        steel(t, x, 13, 3)
    return t


# --- propellant chain --------------------------------------------------------------------------------
MOUND = {4: (7, 8), 5: (6, 9), 6: (5, 10), 7: (4, 11), 8: (3, 12), 9: (3, 12), 10: (2, 13), 11: (2, 13), 12: (3, 12)}
LOOSE = ((1, 12), (14, 11), (13, 13), (5, 13), (10, 13))


def propellant_mix() -> Tex:
    t = Tex(seed="propellant_mix", transparent=True)
    for y, (x0, x1) in MOUND.items():
        cx, half = (x0 + x1) / 2, (x1 - x0) / 2 + 0.5
        for x in range(x0, x1 + 1):
            u = (x + 0.5 - cx - 0.5) / half
            light = 4.6 - (y - 4) * 0.28 - u * 1.4
            if x == x1 or y == 12:
                light -= 1.5
            i = clampi(round(light + t.rng.uniform(-0.6, 0.6)), len(PROPELLANT))
            c = PROPELLANT[i]
            r = t.rng.random()
            if r < 0.12:
                c = SULFUR[clampi(i - 1, len(SULFUR))]          # sulfur grains
            elif r < 0.22:
                c = FRAME[1] if i < 3 else PANEL[2]             # carbon grains
            t.px(x, y, shade(c, t.rng.randint(-2, 2)))
    for x, y in LOOSE:
        t.px(x, y, PROPELLANT[1] if x > 8 else PROPELLANT[3])
    t.px(1, 12, SULFUR[2])
    t.px(13, 13, FRAME[2])
    return t


# Five point star core of the grain, as seen on the tilted end face (x, y offsets from its top left)
STAR = (
    "...#...",
    "#######",
    ".#####.",
    ".##.##.",
    ".#...#.",
)
STAR_AT = (4, 3)


def solid_propellant_grain() -> Tex:
    t = Tex(seed="solid_propellant_grain", transparent=True)
    cx, rx = 8.0, 6.5          # columns 1..14
    top_c, ry = 5.0, 4.0       # end face ellipse spans rows 1..8
    bot_c = 10.5               # bottom ellipse centre; the side wall runs down to row 14

    def in_ellipse(x: int, y: int, cy: float) -> bool:
        return ((x + 0.5 - cx) / rx) ** 2 + ((y + 0.5 - cy) / ry) ** 2 <= 1.0

    def in_side(x: int, y: int) -> bool:
        return abs(x + 0.5 - cx) <= rx and (top_c <= y + 0.5 <= bot_c or in_ellipse(x, y, bot_c))

    def in_star(x: int, y: int) -> bool:
        sx, sy = x - STAR_AT[0], y - STAR_AT[1]
        return 0 <= sy < len(STAR) and 0 <= sx < len(STAR[0]) and STAR[sy][sx] == "#"

    for y in range(16):
        for x in range(16):
            if in_ellipse(x, y, top_c):
                if in_star(x, y):
                    # The bore: dark, with the far inner wall lit where the star meets the face above
                    t.px(x, y, FRAME[1] if not in_star(x, y - 1) else VOID[(x + y) % 2])
                    continue
                if not in_ellipse(x, y - 1, top_c) or not in_ellipse(x - 1, y, top_c):
                    i = 5
                elif not in_ellipse(x, y + 1, top_c):
                    i = 3
                else:
                    i = 4
                t.px(x, y, shade(PROPELLANT[i], t.rng.randint(-2, 2)))
            elif in_side(x, y):
                u = (x + 0.5 - cx) / rx
                light = 1.9 - u * 1.4
                if in_ellipse(x, y - 1, top_c):
                    light -= 1          # shadow line under the front rim of the end face
                if not in_side(x, y + 1):
                    light -= 1          # bottom edge
                if not in_side(x + 1, y):
                    light = 0           # outline on the right
                elif not in_side(x - 1, y):
                    light = 3           # lit left edge
                t.px(x, y, shade(PROPELLANT[clampi(round(light), len(PROPELLANT))], t.rng.randint(-2, 2)))
    # Inhibitor liner: a thin darker ring round the wall near the base
    for x in range(16):
        y = 11 if x in (1, 14) else 12
        if in_side(x, y) and not in_ellipse(x, y, top_c):
            t.px(x, y, shade(t.get(x, y), -16))
    return t


def main() -> None:
    for tier in TIERS:
        rocket_engine(tier).save(f"{tier}_rocket_engine.png", root=ITEMS)
        fuel_tank(tier).save(f"{tier}_fuel_tank.png", root=ITEMS)
        asteroid_miner(tier).save(f"{tier}_asteroid_miner.png", root=ITEMS)
    guidance_computer().save("guidance_computer.png", root=ITEMS)
    propellant_mix().save("propellant_mix.png", root=ITEMS)
    solid_propellant_grain().save("solid_propellant_grain.png", root=ITEMS)


if __name__ == "__main__":
    main()
