"""Atmospheric Compressor: 1x1 cryogenic machine that pulls in air and outputs Compressed Air on top.

Faces (see docs/design/phase1.md, "1. Atmospheric Compressor"):
    front         recessed fan behind a grille, Cryo Cobalt trim, checkered strip
    front_active  4 frame strip: the fan turns 30 degrees per frame with a faint coolant glow behind it
    left, right   intake vents with a frost crust along the top (cold air rushes in)
    top           chemical output nozzle with a Cryo Cobalt ring
    back          energy input socket on a bolted plate
    bottom        plain inset square

Run from the repo root:  python3 tools/textures/gen/atmospheric_compressor.py
"""
import math
import sys
import zlib
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from texlib import *  # noqa: E402,F403

NAME = "atmospheric_compressor"

# Fan geometry, in pixel coordinates (pixel x spans x..x+1). The shroud is centered on the plate.
FAN_C = (8.0, 7.0)
FAN_R_IN = 5.1      # open housing radius
FAN_R_OUT = 6.0     # outer edge of the dark shroud rim
HUB_R = 1.3
BLADES = 3
BLADE_HALF = math.radians(27)    # angular half width of a blade
BLADE_SWEEP = math.radians(16)   # extra twist per pixel of radius, so blades read as curved
STEP = 2 * math.pi / BLADES / 4  # 4 frames cover one blade period, so the loop is seamless
GRILLE_ROWS = (3, 10)  # guard slats near the rim, symmetric about the hub (rows 6 and 7); closer in they chop the blades
GLOW_R = (1.8, 2.8, 3.8)  # radii of the coolant glow bands: bright core, cobalt, dark cobalt


# --- helpers local to this machine -----------------------------------------------------------------
def hrand(x: int, y: int, salt: str) -> int:
    """Stable per pixel random number. Moving parts use this instead of Tex.rng, so every frame
    consumes the shared rng identically and the static steel stays byte identical across frames."""
    return zlib.crc32(f"{NAME}:{salt}:{x}:{y}".encode())


def hpick(ramp, lo: int, hi: int, x: int, y: int, salt: str, jitter: int = 2):
    h = hrand(x, y, salt)
    c = ramp[lo + h % (hi - lo + 1)]
    return shade(c, (h >> 8) % (2 * jitter + 1) - jitter)


def coverage(inside, x: int, y: int, n: int = 4) -> float:
    """Fraction of a pixel covered by a shape, sampled on an n x n grid."""
    hits = sum(inside(x + (i + 0.5) / n, y + (j + 0.5) / n) for i in range(n) for j in range(n))
    return hits / (n * n)


def polar(px: float, py: float, c=FAN_C):
    dx, dy = px - c[0], py - c[1]
    return math.hypot(dx, dy), math.atan2(dy, dx)


def in_blade(px: float, py: float, angle: float) -> bool:
    r, a = polar(px, py)
    if r < HUB_R * 0.8 or r > FAN_R_IN - 0.35:
        return False
    for i in range(BLADES):
        centre = angle + i * 2 * math.pi / BLADES + BLADE_SWEEP * r
        d = (a - centre + math.pi) % (2 * math.pi) - math.pi
        if abs(d) < BLADE_HALF:
            return True
    return False


def disc_pixels(c, r_max: float, r_min: float = 0.0, n: int = 4):
    """Pixels whose covered fraction of the ring r_min..r_max is at least half."""
    out = []
    for y in range(16):
        for x in range(16):
            cov = coverage(lambda px, py: r_min <= polar(px, py, c)[0] < r_max, x, y, n)
            if cov >= 0.5:
                out.append((x, y))
    return out


def recess_lip(t: Tex, c, r_in: float, r_out: float) -> None:
    """Dark round rim around a recess, shadowed on the top left and a touch lighter on the bottom right."""
    for x, y in disc_pixels(c, r_out, r_in):
        _, a = polar(x + 0.5, y + 0.5, c)
        lit = math.cos(a - math.radians(45))  # +1 toward bottom right
        if lit > 0.35:
            t.px(x, y, hpick(FRAME, 4, 4, x, y, "lip", 1))
        elif lit > -0.35:
            t.px(x, y, hpick(FRAME, 2, 3, x, y, "lip", 1))
        else:
            t.px(x, y, hpick(FRAME, 0, 1, x, y, "lip", 1))


# --- faces -----------------------------------------------------------------------------------------
def front(frame: int = 0, glow: bool = False) -> Tex:
    t = Tex(seed=f"{NAME}_front")
    t.machine_shell()
    t.plate((1, 1, 14, 10))
    t.cobalt_trim((1, 11, 14, 12))
    t.checker_strip(13)

    # Housing background: void, drawn with the shared rng so every frame matches the idle face.
    housing = disc_pixels(FAN_C, FAN_R_IN)
    lip = disc_pixels(FAN_C, FAN_R_OUT, FAN_R_IN)
    for x, y in housing:
        t.px(x, y, shade(t.pick(VOID, 0, 2), t.rng.randint(-1, 1)))
    recess_lip(t, FAN_C, FAN_R_IN, FAN_R_OUT)

    if glow:
        # Faint cold light deep in the housing, brightest at the hub. Stays behind the blades.
        for x, y in housing:
            r, _ = polar(x + 0.5, y + 0.5)
            if r < GLOW_R[0]:
                t.px(x, y, hpick(CRYO_GLOW, 0, 0, x, y, "glow", 2))
            elif r < GLOW_R[1]:
                t.px(x, y, hpick(COBALT, 1, 1, x, y, "glow", 2))
            elif r < GLOW_R[2]:
                t.px(x, y, hpick(COBALT, 0, 0, x, y, "glow", 1))

    # Blades: plate steel, framed by the dark rim. Faces turned toward the top left catch a little
    # more light; partly covered pixels fall back to panel steel.
    angle = math.radians(-90) + frame * STEP
    for x, y in housing:
        cov = coverage(lambda px, py: in_blade(px, py, angle), x, y)
        if cov >= 0.6:
            lit = (x + y) < (FAN_C[0] + FAN_C[1] - 1)
            t.px(x, y, hpick(PLATE, 2, 4, x, y, "blade") if lit else hpick(PLATE, 0, 2, x, y, "blade"))
        elif cov >= 0.3:
            t.px(x, y, hpick(PANEL, 1, 2, x, y, "blade_edge", 1))

    # Hub: a darker spinner cap with one glint on its top left, so it reads apart from the blades.
    hx, hy = int(FAN_C[0]) - 1, int(FAN_C[1]) - 1
    t.px(hx, hy, GLINT[0])
    t.px(hx + 1, hy, PANEL[2])
    t.px(hx, hy + 1, PANEL[1])
    t.px(hx + 1, hy + 1, FRAME[3])

    # Grille: thin guard slats across the opening, in front of the blades and clear of the hub.
    for gy in GRILLE_ROWS:
        for x in range(16):
            if (x, gy) in housing:
                t.px(x, gy, hpick(FRAME, 2, 3, x, gy, "grille", 1))
            elif (x, gy) in lip:
                t.px(x, gy, hpick(FRAME, 1, 2, x, gy, "grille", 1))
    return t


def louvers(t: Tex, box, salt: str) -> None:
    """Intake louvers: lit slat lips over dark gaps, two rows per slat."""
    x0, y0, x1, y1 = box
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            if (y - y0) % 2 == 0:
                t.px(x, y, hpick(PANEL, 2, 4, x, y, salt) if x > x0 else hpick(PANEL, 1, 2, x, y, salt))
            else:
                t.px(x, y, hpick(VOID, 1, 3, x, y, salt, 1))


def side(seed: str) -> Tex:
    t = Tex(seed=f"{NAME}_{seed}")
    t.machine_shell()
    t.plate((1, 1, 14, 5))
    t.line(1, 6, 14, 6, FRAME[1])
    # Two intake grilles in recessed frames, split by a mullion.
    for x0, x1 in ((2, 7), (8, 13)):
        t.inset((x0, 7, x1, 12), light=PANEL[4], dark=FRAME[0])
        louvers(t, (x0 + 1, 8, x1 - 1, 11), seed)
    t.checker_strip(13)
    t.frost(depth=2, coverage=0.85)
    return t


def top() -> Tex:
    t = Tex(seed=f"{NAME}_top")
    t.machine_shell()
    t.plate((2, 2, 13, 13))
    for x, y in ((3, 3), (12, 3), (3, 12), (12, 12)):
        t.px(x, y, GLINT[0])
    c = (8.0, 8.0)

    def toward_light(x, y):  # +1 on the top left of the circle, -1 on the bottom right
        _, a = polar(x + 0.5, y + 0.5, c)
        return -math.cos(a - math.radians(45))

    # Seam around the nozzle, raised steel collar, Cryo Cobalt ring, dark bore.
    for x, y in disc_pixels(c, 5.0, 4.2):
        t.px(x, y, hpick(FRAME, 1, 3, x, y, "seam", 1))
    for x, y in disc_pixels(c, 4.2, 3.2):
        lit = toward_light(x, y)
        t.px(x, y, hpick(PLATE, 3, 4, x, y, "collar") if lit > 0.35 else
             hpick(PLATE, 0, 2, x, y, "collar") if lit > -0.35 else hpick(PANEL, 1, 2, x, y, "collar"))
    for x, y in disc_pixels(c, 3.2, 2.0):
        lit = toward_light(x, y)
        t.px(x, y, hpick(COBALT, 3, 3, x, y, "ring", 1) if lit > 0.35 else
             hpick(COBALT, 2, 2, x, y, "ring", 1) if lit > -0.35 else hpick(COBALT, 1, 1, x, y, "ring", 1))
    for x, y in disc_pixels(c, 2.0):
        lit = toward_light(x, y)
        t.px(x, y, hpick(VOID, 0, 1, x, y, "bore", 1) if lit > 0 else hpick(VOID, 2, 3, x, y, "bore", 1))
    return t


def back() -> Tex:
    t = Tex(seed=f"{NAME}_back")
    t.machine_shell()
    t.plate((1, 1, 14, 9))
    t.bolts(2)
    # Energy socket: recessed square with two contact slots.
    t.inset((5, 3, 10, 8), light=PLATE[4], dark=FRAME[0])
    t.fill((6, 4, 9, 7), FRAME, 0, 2)
    for x in (6, 9):
        t.px(x, 5, VOID[0])
        t.px(x, 6, VOID[1])
    t.px(7, 4, PANEL[1]); t.px(8, 4, PANEL[1])
    t.line(1, 10, 14, 10, FRAME[0])
    t.checker_strip(13)
    return t


def bottom() -> Tex:
    t = Tex(seed=f"{NAME}_bottom")
    t.machine_shell()
    # A shallow dish, like Mekanism machine bottoms: dark corners on the outer ring, a shadowed
    # inner ring on the top left, and a floor that lightens toward the middle.
    for x, y in ((2, 2), (13, 2), (2, 13), (13, 13)):
        t.px(x, y, FRAME[2])
    t.inset((3, 3, 12, 12), light=PANEL[1], dark=FRAME[4])
    t.fill((4, 4, 11, 11), PANEL, 0, 1)
    t.fill((5, 5, 10, 10), PANEL, 1, 2)
    return t


def main() -> None:
    out = [
        front().save(f"{NAME}/front.png"),
        save_animation([front(f, glow=True) for f in range(4)], f"{NAME}/front_active.png", frametime=2),
        side("left").save(f"{NAME}/left.png"),
        side("right").save(f"{NAME}/right.png"),
        top().save(f"{NAME}/top.png"),
        back().save(f"{NAME}/back.png"),
        bottom().save(f"{NAME}/bottom.png"),
    ]
    for p in out:
        print(p.relative_to(REPO))


if __name__ == "__main__":
    main()
