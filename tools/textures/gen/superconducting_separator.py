"""Superconducting Separator and the Superconducting Coil item.

Superconducting Separator: 1x1 Mekanism steel machine with a big magnet coil inside, cooled by liquid
nitrogen. While superconducting it is active, and frost grows on it.

    front          a window onto the magnet: copper windings between two steel flanges, a Cryo Cobalt
                   clamp band round the middle, Cryo Cobalt trim and the checkered strip below
    front_active   4 frame strip: the window floods with coolant glow behind the coil, a brighter flux
                   band sweeps down through it, frost crust along the top edge
    left, right    Mekanism style plate over a darker lower section crossed by a coolant line with
                   Cryo Cobalt collars; the active variants add a frost crust and frosted collars
    top            nitrogen boil off vent: slotted grille in a recessed square with cobalt brackets;
                   the active variant frosts the vent corners
    back           liquid nitrogen inlet: round port with a Cryo Cobalt ring on a bolted plate
    bottom         plain inset square

Superconducting Coil (item): an upright spool in Mekanism item steel, copper windings with a Cryo Cobalt
tape band round the middle and a lead sticking out to the right.

Run from the repo root:  python3 tools/textures/gen/superconducting_separator.py
"""
import math
import sys
import zlib
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from texlib import *  # noqa: E402,F403

NAME = "superconducting_separator"
ITEMS = ASSETS / "item"

WINDOW = (3, 3, 12, 8)          # inside of the front window
COIL_X = (5, 10)                # copper winding columns inside the window
FLANGES = (4, 11)               # steel flange columns either side of the winding
CLAMP_ROWS = (5, 6)             # Cryo Cobalt clamp band across the winding


def hrand(x: int, y: int, salt: str) -> int:
    """Stable per pixel random number, so animated parts never disturb the shared rng."""
    return zlib.crc32(f"{NAME}:{salt}:{x}:{y}".encode())


def hpick(ramp, lo: int, hi: int, x: int, y: int, salt: str, jitter: int = 2):
    h = hrand(x, y, salt)
    c = ramp[lo + h % (hi - lo + 1)]
    return shade(c, (h >> 8) % (2 * jitter + 1) - jitter)


def disc_pixels(c, r_max: float, r_min: float = 0.0):
    out = []
    for y in range(16):
        for x in range(16):
            hits = sum(r_min <= math.hypot(x + (i + 0.5) / 4 - c[0], y + (j + 0.5) / 4 - c[1]) < r_max
                       for i in range(4) for j in range(4))
            if hits >= 8:
                out.append((x, y))
    return out


def toward_light(c, x: int, y: int) -> float:
    """+1 on the top left of a circle around c, -1 on the bottom right."""
    a = math.atan2(y + 0.5 - c[1], x + 0.5 - c[0])
    return -math.cos(a - math.radians(45))


def frost_px(t: Tex, x: int, y: int, level: int, salt: str) -> None:
    """One rime pixel; level 0 is the thick crust, higher levels thinner and darker."""
    t.px(x, y, hpick(FROST, max(0, 1 - level), max(0, 3 - level), x, y, salt, 1))


def frost_crust(t: Tex, salt: str, depth: int = 2) -> None:
    """Connected crust along the top edge with short drips, drawn with hrand so idle and active
    faces share their steel."""
    for x in range(1, 15):
        if hrand(x, 0, salt) % 100 < 88:
            frost_px(t, x, 0, 0, salt)
            length = 1 + hrand(x, 1, salt) % depth
            for r in range(1, length):
                frost_px(t, x, r, r, salt)


# --- separator faces -------------------------------------------------------------------------------
def coil_in_window(t: Tex, glow_frame: int | None) -> None:
    x0, y0, x1, y1 = WINDOW
    t.outline((x0 - 1, y0 - 1, x1 + 1, y1 + 1), FRAME[0])
    # Behind the coil: void, or coolant glow with a flux band sweeping down (4 frame loop)
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            if glow_frame is None:
                t.px(x, y, hpick(VOID, 0, 3, x, y, "void", 1))
            else:
                band = (y - y0 - glow_frame * 2) % 8
                lo = 2 if band in (0, 1) else 0 if band > 4 else 1
                t.px(x, y, hpick(CRYO_GLOW, lo, lo, x, y, f"glow{glow_frame}", 3))
    # Flanges: steel discs seen edge on, lit on the left
    for fx in FLANGES:
        for y in range(y0, y1 + 1):
            t.px(fx, y, hpick(PLATE, 2, 4, fx, y, "flange") if fx == FLANGES[0] else hpick(PANEL, 1, 3, fx, y, "flange"))
        t.px(fx, y0, GLINT[0] if fx == FLANGES[0] else PLATE[1])
    # Windings: alternating copper rows, a cylinder lit from the top left
    cx0, cx1 = COIL_X
    for y in range(y0, y1 + 1):
        for x in range(cx0, cx1 + 1):
            if y in CLAMP_ROWS:
                continue
            across = (x - cx0) / (cx1 - cx0)
            if (y - y0) % 2 == 0:
                # Lit top of a turn, falling off to the right
                base = 4 if across < 0.3 else 3 if across < 0.7 else 2
            else:
                # Groove between turns
                base = 1 if across < 0.7 else 0
            t.px(x, y, hpick(COPPER, max(0, base - 1), base, x, y, "winding", 2))
    # Clamp band
    for x in range(cx0, cx1 + 1):
        t.px(x, CLAMP_ROWS[0], hpick(COBALT, 3, 4 if x < cx0 + 2 else 3, x, CLAMP_ROWS[0], "clamp", 1))
        t.px(x, CLAMP_ROWS[1], hpick(COBALT, 1, 2, x, CLAMP_ROWS[1], "clamp", 1))
    if glow_frame is None:
        # Glare specks on the glass, only over the dark gaps
        t.px(x0, y0 + 1, PLATE[2])
        t.px(x1, y1 - 1, PANEL[4])


def front(glow_frame: int | None = None) -> Tex:
    t = Tex(seed=f"{NAME}_front")
    t.machine_shell()
    t.plate((1, 1, 14, 10))
    coil_in_window(t, glow_frame)
    t.cobalt_trim((1, 11, 14, 12))
    t.checker_strip(13)
    if glow_frame is not None:
        frost_crust(t, "front_frost", depth=2)
    return t


def coolant_line(t: Tex, y: int, frosted: bool, salt: str) -> None:
    """A two pixel coolant pipe across the lower section, with cobalt collars where it enters the casing."""
    for x in range(1, 15):
        t.px(x, y, hpick(PLATE, 1, 3, x, y, salt))
        t.px(x, y + 1, hpick(PANEL, 0, 1, x, y + 1, salt))
    t.line(1, y + 2, 14, y + 2, FRAME[0])
    for cx in (3, 12):
        t.px(cx, y, hpick(COBALT, 3, 4, cx, y, salt, 1))
        t.px(cx, y + 1, hpick(COBALT, 1, 2, cx, y + 1, salt, 1))
        t.px(cx + 1, y, hpick(COBALT, 3, 3, cx + 1, y, salt, 1))
        t.px(cx + 1, y + 1, hpick(COBALT, 1, 1, cx + 1, y + 1, salt, 1))
        if frosted:
            frost_px(t, cx, y - 1, 0, salt)
            frost_px(t, cx + 1, y - 1, 1, salt)
            frost_px(t, cx, y, 1, salt)


def side(seed: str, active: bool = False) -> Tex:
    t = Tex(seed=f"{NAME}_{seed}")
    t.machine_shell()
    t.plate((1, 1, 14, 6))
    t.line(1, 7, 14, 7, FRAME[1])
    coolant_line(t, 9, active, seed)
    t.checker_strip(13)
    if active:
        frost_crust(t, f"{seed}_frost", depth=3)
    return t


def top(active: bool = False) -> Tex:
    t = Tex(seed=f"{NAME}_top")
    t.machine_shell()
    t.plate((2, 2, 13, 13))
    # Recessed boil off vent with cobalt brackets in its corners
    t.inset((4, 4, 11, 11), light=PLATE[4], dark=FRAME[0])
    t.vent((5, 5, 10, 10))
    for (x, y, dx, dy) in ((4, 4, 1, 1), (11, 4, -1, 1), (4, 11, 1, -1), (11, 11, -1, -1)):
        t.px(x, y, hpick(COBALT, 3, 4, x, y, "bracket", 1))
        t.px(x + dx, y, hpick(COBALT, 2, 3, x + dx, y, "bracket", 1))
        t.px(x, y + dy, hpick(COBALT, 2, 2, x, y + dy, "bracket", 1))
    if active:
        # Cold vapour freezes on the grille: frost creeping in from each corner of the vent
        for (x, y, dx, dy) in ((5, 5, 1, 1), (10, 5, -1, 1), (5, 10, 1, -1), (10, 10, -1, -1)):
            frost_px(t, x, y, 0, "top_frost")
            frost_px(t, x + dx, y, 1, "top_frost")
            frost_px(t, x, y + dy, 1, "top_frost")
    return t


def back() -> Tex:
    t = Tex(seed=f"{NAME}_back")
    t.machine_shell()
    t.plate((1, 1, 14, 11))
    t.bolts(2)
    c = (8.0, 6.5)
    for x, y in disc_pixels(c, 4.4, 3.5):
        t.px(x, y, hpick(FRAME, 1, 3, x, y, "seam", 1))
    for x, y in disc_pixels(c, 3.5, 2.6):
        lit = toward_light(c, x, y)
        t.px(x, y, hpick(COBALT, 3, 3, x, y, "ring", 1) if lit > 0.35 else
             hpick(COBALT, 2, 2, x, y, "ring", 1) if lit > -0.35 else hpick(COBALT, 1, 1, x, y, "ring", 1))
    for x, y in disc_pixels(c, 2.6):
        lit = toward_light(c, x, y)
        t.px(x, y, hpick(VOID, 0, 1, x, y, "bore", 1) if lit > 0 else hpick(VOID, 2, 3, x, y, "bore", 1))
    t.line(1, 12, 14, 12, FRAME[0])
    t.checker_strip(13)
    return t


def bottom() -> Tex:
    t = Tex(seed=f"{NAME}_bottom")
    t.machine_shell()
    for x, y in ((2, 2), (13, 2), (2, 13), (13, 13)):
        t.px(x, y, FRAME[2])
    t.inset((3, 3, 12, 12), light=PANEL[1], dark=FRAME[4])
    t.fill((4, 4, 11, 11), PANEL, 0, 1)
    t.fill((5, 5, 10, 10), PANEL, 1, 2)
    return t


# --- coil item -------------------------------------------------------------------------------------
STEEL_PROFILE = (7, 8, 9, 9, 8, 7, 6, 5, 4, 3)
COPPER_PROFILE = (4, 5, 5, 4, 4, 3, 3, 2, 2, 1)
COBALT_PROFILE = (3, 4, 4, 3, 3, 2, 2, 1, 1, 1)


def profile_at(profile, x: int, x0: int, x1: int) -> int:
    f = 0.0 if x1 == x0 else (x - x0) / (x1 - x0)
    return profile[min(len(profile) - 1, int(f * len(profile)))]


def ramp_px(t: Tex, x: int, y: int, ramp, i: int, jitter: int = 2) -> None:
    t.px(x, y, shade(ramp[max(0, min(len(ramp) - 1, i))], t.rng.randint(-jitter, jitter)))


def flange_row(t: Tex, y: int, x0: int, x1: int, shift: int = 0) -> None:
    for x in range(x0, x1 + 1):
        if x == x0:
            ramp_px(t, x, y, ITEM_STEEL, 5 + min(shift, 0))
        elif x == x1:
            ramp_px(t, x, y, ITEM_STEEL, 1)
        else:
            ramp_px(t, x, y, ITEM_STEEL, profile_at(STEEL_PROFILE, x, x0, x1) + shift)


def coil_item() -> Tex:
    t = Tex(seed="superconducting_coil", transparent=True)
    # Top flange: the rim seen from slightly above, with the bore
    flange_row(t, 1, 4, 11, shift=-1)
    flange_row(t, 2, 2, 13, shift=0)
    flange_row(t, 3, 2, 13, shift=-3)
    for x in (7, 8):
        t.px(x, 1, ITEM_STEEL[0])
    # Winding: copper turns, every other row a shade darker, cobalt tape band in the middle
    for y in range(4, 13):
        for x in range(3, 13):
            if x == 12:
                ramp_px(t, x, y, COPPER, 0, 1)
            elif y in (7, 8):
                ramp_px(t, x, y, COBALT, profile_at(COBALT_PROFILE, x, 3, 12) - (y - 7), 1)
            else:
                ramp_px(t, x, y, COPPER, profile_at(COPPER_PROFILE, x, 3, 12) - (2 if y % 2 else 0), 2)
        if y not in (7, 8):
            ramp_px(t, 3, y, COPPER, 3, 1)
    # Bottom flange
    flange_row(t, 13, 2, 13, shift=-2)
    flange_row(t, 14, 3, 12, shift=-4)
    # Lead out to the right
    for x, y, i in ((13, 10, 4), (14, 10, 3), (14, 11, 2), (15, 11, 1)):
        ramp_px(t, x, y, COPPER, i, 1)
    return t


def main() -> None:
    out = [
        front().save(f"{NAME}/front.png"),
        save_animation([front(f) for f in range(4)], f"{NAME}/front_active.png", frametime=4, interpolate=True),
        side("left").save(f"{NAME}/left.png"),
        side("right").save(f"{NAME}/right.png"),
        side("left", active=True).save(f"{NAME}/left_active.png"),
        side("right", active=True).save(f"{NAME}/right_active.png"),
        top().save(f"{NAME}/top.png"),
        top(active=True).save(f"{NAME}/top_active.png"),
        back().save(f"{NAME}/back.png"),
        bottom().save(f"{NAME}/bottom.png"),
        coil_item().save("superconducting_coil.png", root=ITEMS),
    ]
    for p in out:
        print(p.relative_to(REPO))


if __name__ == "__main__":
    main()
