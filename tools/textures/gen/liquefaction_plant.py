"""Cryogenic Liquefaction Plant: Mekanism style multiblock, a hollow box of casings, ports and glass.

There is no 3D model. The formed plant has to read as one machine purely from these faces, so every
block shares the same casing base (see docs/design/phase1.md, "2. Cryogenic Liquefaction Plant"):

    casing                      light multiblock steel plate with Cryo Cobalt corner brackets. Tiles
                                in any arrangement; no frost, because it repeats over whole walls
    valve                       casing with a round chemical port: steel collar, cobalt ring, dark bore
    cryocooler(_active)         casing with a recessed finned cold finger. Active: 4 frame strip, the
                                cold head pulses with coolant glow
    heat_rejection_port(_active) casing with a vertical heat exchanger grille. Active: 4 frame strip,
                                a dull ember glow flickers behind the slats (the only warm colors)
    controller_front(_active)   casing with a screen. Idle screen is dark; active is a 6 frame strip:
                                a read head runs down a cooldown curve, the gauge follows it

The brackets frame every insert (inserts occupy the 3..12 square), which is what ties the set
together next to Mekanism structural glass.

Run from the repo root:  python3 tools/textures/gen/liquefaction_plant.py
"""
import math
import sys
import zlib
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from texlib import *  # noqa: E402,F403

NAME = "liquefaction"
C = (8.0, 8.0)  # centre of a face, in pixel coordinates (pixel x spans x..x+1)
INSERT = (3, 3, 12, 12)  # every functional insert sits in this square, inside the brackets


# --- helpers local to this family ------------------------------------------------------------------
def hrand(x: int, y: int, salt: str) -> int:
    """Stable per pixel random number, so animated parts never disturb the shared rng and the static
    steel stays byte identical across frames."""
    return zlib.crc32(f"{NAME}:{salt}:{x}:{y}".encode())


def hpick(ramp, lo: int, hi: int, x: int, y: int, salt: str, jitter: int = 2):
    h = hrand(x, y, salt)
    c = ramp[lo + h % (hi - lo + 1)]
    return shade(c, (h >> 8) % (2 * jitter + 1) - jitter)


# --- shared casing ---------------------------------------------------------------------------------
BRACKETS = ((2, 2, 1, 1), (13, 2, -1, 1), (2, 13, 1, -1), (13, 13, -1, -1))


def casing_base(seed: str) -> Tex:
    """Multiblock casing plate: a 1 pixel CASING[0] border on every side (so any two blocks meet in the
    same 2 pixel seam, like Mekanism casings), a noisy plate, a gentle top left bevel, and a Cryo
    Cobalt L bracket inlaid in each corner."""
    t = Tex(seed=f"{NAME}_{seed}")
    t.fill((0, 0, 15, 15), CASING, 0, 0, jitter=2)
    t.fill((1, 1, 14, 14), CASING, 3, 4, jitter=2)
    for i in range(1, 15):
        t.px(i, 1, shade(t.pick(CASING, 4, 5), t.rng.randint(-2, 1)))
        t.px(1, i, shade(t.pick(CASING, 4, 5), t.rng.randint(-2, 1)))
    for i in range(2, 15):
        t.px(i, 14, shade(CASING[2], t.rng.randint(-2, 2)))
        t.px(14, i, shade(CASING[2], t.rng.randint(-2, 2)))
    for cx, cy, sx, sy in BRACKETS:
        # Inlay: the corner pixel catches the light, the arms sit one step darker. Kept dark on
        # purpose: brighter or longer brackets turn a big wall into a busy blue grid.
        t.px(cx, cy, COBALT[2])
        for k in (1, 2):
            t.px(cx + sx * k, cy, COBALT[1])
            t.px(cx, cy + sy * k, COBALT[1])
    return t


def recess(t: Tex, box, floor=FRAME, lo: int = 0, hi: int = 2) -> None:
    """Square pocket in the plate: shadowed top and left lip, lit bottom and right lip, dark floor."""
    x0, y0, x1, y1 = box
    t.fill((x0 + 1, y0 + 1, x1 - 1, y1 - 1), floor, lo, hi, jitter=1)
    t.inset(box, light=CASING[2], dark=FRAME[2])


# --- faces -----------------------------------------------------------------------------------------
def casing() -> Tex:
    return casing_base("casing")


def octagon(half: float, cut: float):
    """Pixels of a square of the given half size around C with its corners cut off (a pixel clean
    round port; true circles this small turn into lumpy crosses)."""
    out = []
    for y in range(16):
        for x in range(16):
            dx, dy = abs(x + 0.5 - C[0]), abs(y + 0.5 - C[1])
            if max(dx, dy) < half and dx + dy < cut:
                out.append((x, y))
    return out


def valve() -> Tex:
    t = casing_base("valve")
    # Raised steel flange set into the plate by a dark seam, Cryo Cobalt ring, dark bore. The seam
    # spans the same 3..12 square as the other inserts.
    seam = octagon(5, 8)
    flange = octagon(4, 6.5)
    ring = octagon(3, 4.5)
    bore = octagon(2, 3)
    for x, y in seam:
        if (x, y) not in flange:
            # The flange stands proud, so its shadow is deepest on the bottom right.
            t.px(x, y, hpick(FRAME, 0, 1, x, y, "seam", 1) if x + y > 15 else hpick(FRAME, 2, 3, x, y, "seam", 1))
    for x, y in flange:
        if (x, y) not in ring:
            lit = (x + y) - 15  # negative toward the top left
            i = 5 if lit < -3 else 4 if lit < 0 else 2 if lit < 4 else 1
            t.px(x, y, hpick(CASING, i, i, x, y, "flange", 1))
    for x, y in ring:
        if (x, y) not in bore:
            lit = (x + y) - 15
            i = 3 if lit < -2 else 2 if lit < 2 else 1
            t.px(x, y, hpick(COBALT, i, i, x, y, "ring", 1))
    for x, y in bore:
        # Recessed, so the shadow falls on its top left.
        lit = (x + y) - 15
        t.px(x, y, hpick(VOID, 0, 0, x, y, "bore", 1) if lit < 0 else hpick(VOID, 2, 3, x, y, "bore", 1))
    t.px(6, 5, COBALT[4])  # glint on the ring
    return t


# Cryocooler: a cold finger in a pocket. Heat sink fins on top, a flange, and the cold head below.
COOLER_FINS = (4, 6)
COOLER_FLANGE = 8
COOLER_HEAD = (5, 9, 10, 11)   # cold head box, bottom corners rounded off
COOLER_PULSE = (0, 1, 2, 1)    # glow level per frame


def cryocooler(frame: int = 0, active: bool = False) -> Tex:
    t = casing_base("cryocooler")
    recess(t, INSERT, VOID, 1, 3)
    # Finger shaft, only visible between the fins.
    for y in (5, 7):
        t.px(7, y, hpick(PANEL, 2, 3, 7, y, "shaft", 1))
        t.px(8, y, hpick(FRAME, 3, 4, 8, y, "shaft", 1))
    # Fins: thin plate steel, lit toward the left end.
    for fy in COOLER_FINS:
        for x in range(4, 12):
            lo, hi = (3, 4) if x < 7 else (1, 3) if x < 10 else (0, 1)
            t.px(x, fy, hpick(PLATE, lo, hi, x, fy, "fin", 1))
    # Flange: a wider casing steel collar the cold head hangs from.
    for x in range(4, 12):
        i = 5 if x < 6 else 4 if x < 10 else 2
        t.px(x, COOLER_FLANGE, hpick(CASING, i, i, x, COOLER_FLANGE, "flange", 1))
    x0, y0, x1, y1 = COOLER_HEAD
    head = [(x, y) for y in range(y0, y1 + 1) for x in range(x0, x1 + 1) if not (y == y1 and x in (x0, x1))]
    if not active:
        for x, y in head:
            i = 3 if y == y0 or x == x0 else 1 if y == y1 or x == x1 else 2
            t.px(x, y, hpick(COBALT, i, i, x, y, "head", 1))
    else:
        level = COOLER_PULSE[frame]
        core = (CRYO_GLOW[1], CRYO_GLOW[2], CRYO_GLOW[3])[level]
        rim = (COBALT[3], CRYO_GLOW[0], CRYO_GLOW[1])[level]
        for x, y in head:
            edge = y in (y0, y1) or x in (x0, x1)
            t.px(x, y, shade(rim if edge else core, hrand(x, y, "headglow") % 3 - 1))
        # Cold light on the pocket floor beside the head, strongest at the peak of the pulse.
        if level:
            for y in range(y0, y1 + 1):
                for x in (x0 - 1, x1 + 1):
                    t.px(x, y, COBALT[level - 1] if y > y0 else COBALT[0])
    # Rime: a thin connected crust on the top lip of the pocket, with one short drip onto the fin.
    for x in range(5, 11):
        t.px(x, 3, hpick(FROST, 0, 1, x, 3, "frost", 1))
    t.px(4, 3, hpick(FROST, 0, 0, 4, 3, "frost", 1))
    t.px(9, 4, FROST[0])
    return t


# Heat rejection port: vertical exchanger slats in a pocket, with heat glowing between them.
SLATS = (4, 6, 8, 10)
HEAT = [ORDNANCE[1], ORDNANCE[2], GOLD[1], GOLD[2], GOLD[3]]  # dull ember, dark to bright


def heat_rejection_port(frame: int = 0, active: bool = False) -> Tex:
    t = casing_base("heat_rejection_port")
    recess(t, INSERT, VOID, 0, 2)
    for x in range(4, 12):
        for y in range(4, 12):
            if x in SLATS:
                # Slat: lit edge on top, body plate steel, shadow at the foot.
                ramp, lo, hi = (PLATE, 3, 4) if y == 4 else (PANEL, 1, 2) if y == 11 else (PLATE, 0, 2)
                t.px(x, y, hpick(ramp, lo, hi, x, y, "slat", 1))
            elif active:
                # Glow is hottest in the middle of the pocket and flickers a little per frame.
                r = math.hypot(x + 0.5 - 8.0, (y + 0.5 - 8.0) * 1.2)
                heat = 4.2 - r * 1.05 + (hrand(x, y, f"flicker{frame}") % 3 - 1) * 0.6
                i = max(0, min(len(HEAT) - 1, round(heat)))
                t.px(x, y, shade(HEAT[i], hrand(x, y, "heat") % 3 - 1))
            else:
                # Cold port: dark gaps, the deepest centre faintly scorched.
                deep = abs(x + 0.5 - 8.0) < 2.5 and abs(y + 0.5 - 8.0) < 2.5
                t.px(x, y, hpick(ORDNANCE, 0, 0, x, y, "soot", 1) if deep and (hrand(x, y, "soot") % 4 == 0)
                     else hpick(VOID, 1, 3, x, y, "gap", 1))
    # A cross brace holding the slats, in casing steel.
    for x in range(4, 12):
        t.px(x, 9, hpick(CASING, 2, 2, x, 9, "brace", 1) if x in SLATS else hpick(FRAME, 1, 2, x, 9, "brace", 1))
        t.px(x, 10, hpick(FRAME, 0, 1, x, 10, "brace", 1) if x not in SLATS else t.get(x, 10))
    return t


# Controller screen: interior 4..11 square. Temperature gauge on the left, cooldown curve on the right.
SCREEN = (4, 4, 11, 11)
GAUGE_X = (4, 5)
GAUGE_Y = (5, 10)
THRESHOLDS = (7, 9)                  # faint guide lines across the plot (the recipe temperatures)
CURVE = ((7, 5), (8, 6), (8, 7), (9, 8), (10, 9), (11, 9))  # temperature falling over time
FRAMES = len(CURVE)


def controller_front(frame: int = 0, active: bool = False) -> Tex:
    t = casing_base("controller")
    x0, y0, x1, y1 = SCREEN
    t.inset(INSERT, light=CASING[2], dark=FRAME[2])
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            # Faint scan rows, like Mekanism's evaporation controller screen.
            t.px(x, y, hpick(VOID, 2, 3, x, y, "screen", 1) if (y - y0) % 2 else hpick(VOID, 0, 1, x, y, "screen", 1))
    if not active:
        # Dead screen: a few diagonal glare specks.
        for gx, gy, c in ((5, 5, PANEL[0]), (6, 6, FRAME[4]), (9, 5, FRAME[4])):
            t.px(gx, gy, c)
        return t

    # The cooldown curve is always on screen, dim. A bright read head runs along it, one point per
    # frame, and the gauge shows the temperature at the head (colder = fuller).
    gy0, gy1 = GAUGE_Y
    hx, hy = CURVE[frame]
    fill = 1 + (hy - CURVE[0][1]) * (gy1 - gy0) // (CURVE[-1][1] - CURVE[0][1])  # filled rows from the bottom
    for y in range(gy0, gy1 + 1):
        for x in GAUGE_X:
            if y <= gy1 - fill:
                t.px(x, y, COBALT[0])
            elif y == gy1 - fill + 1:
                t.px(x, y, CRYO_GLOW[3] if x == GAUGE_X[0] else CRYO_GLOW[2])
            else:
                t.px(x, y, CRYO_GLOW[1] if x == GAUGE_X[0] else CRYO_GLOW[0])
    for x in range(7, 12):
        for ty in THRESHOLDS:
            t.px(x, ty, COBALT[0])
        t.px(x, 11, COBALT[1])  # time axis
    for x, y in CURVE:
        t.px(x, y, COBALT[3])
    t.px(hx, hy, CRYO_GLOW[3])
    t.px(hx, 11, CRYO_GLOW[1])  # cursor on the time axis
    return t


def main() -> None:
    d = NAME
    out = [
        casing().save(f"{d}/casing.png"),
        valve().save(f"{d}/valve.png"),
        cryocooler().save(f"{d}/cryocooler.png"),
        save_animation([cryocooler(f, True) for f in range(len(COOLER_PULSE))], f"{d}/cryocooler_active.png", frametime=3),
        heat_rejection_port().save(f"{d}/heat_rejection_port.png"),
        save_animation([heat_rejection_port(f, True) for f in range(4)], f"{d}/heat_rejection_port_active.png", frametime=4),
        controller_front().save(f"{d}/controller_front.png"),
        save_animation([controller_front(f, True) for f in range(FRAMES)], f"{d}/controller_front_active.png", frametime=4),
    ]
    for p in out:
        print(p.relative_to(REPO))


if __name__ == "__main__":
    main()
