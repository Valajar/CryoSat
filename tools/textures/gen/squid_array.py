"""SQUID Array: 1x1 Mekanism steel machine, a superconducting magnetometer cooled by liquid nitrogen.
While superconducting it is active, its sensor head glows and frost grows on it.

    front          round sensor head in a recessed plate: two Cryo Cobalt rings (the pickup loops) round a
                   dark core, Cryo Cobalt trim and the checkered strip below
    front_active   4 frame strip: the core and the gap between the rings glow with coolant, pulsing slowly,
                   frost crust along the top edge
    side           Mekanism style plate holding a row of three small cobalt sensor pads (the array), over a
                   darker lower section crossed by a coolant line with Cryo Cobalt collars; the active
                   variant adds a frost crust and frosted collars
    top            dewar cap: a round lid with a Cryo Cobalt ring and the nitrogen boil off bore in the middle;
                   the active variant frosts the lid
    back           liquid nitrogen inlet: round port with a Cryo Cobalt ring on a bolted plate
    bottom         plain inset square

Run from the repo root:  python3 tools/textures/gen/squid_array.py
"""
import math
import sys
import zlib
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from texlib import *  # noqa: E402,F403

NAME = "squid_array"
HEAD = (8.0, 6.0)                # centre of the sensor head on the front (between pixels)


def hrand(x: int, y: int, salt: str) -> int:
    """Stable per pixel random number, so animated parts never disturb the shared rng."""
    return zlib.crc32(f"{NAME}:{salt}:{x}:{y}".encode())


def hpick(ramp, lo: int, hi: int, x: int, y: int, salt: str, jitter: int = 2):
    h = hrand(x, y, salt)
    c = ramp[lo + h % (hi - lo + 1)]
    return shade(c, (h >> 8) % (2 * jitter + 1) - jitter)


def ring_pixels(c, r_max: float, r_min: float = 0.0):
    """Pixels whose area lies mostly between r_min and r_max around c."""
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


def lit_ring(t: Tex, c, r_max: float, r_min: float, ramp, salt: str, raised: bool = True) -> None:
    """A ring shaded as a raised (or recessed) band lit from the top left."""
    for x, y in ring_pixels(c, r_max, r_min):
        lit = toward_light(c, x, y) * (1 if raised else -1)
        i = 3 if lit > 0.35 else 2 if lit > -0.35 else 1
        t.px(x, y, hpick(ramp, i, i, x, y, salt, 1))


def frost_px(t: Tex, x: int, y: int, level: int, salt: str) -> None:
    """One rime pixel; level 0 is the thick crust, higher levels thinner and darker."""
    t.px(x, y, hpick(FROST, max(0, 1 - level), max(0, 3 - level), x, y, salt, 1))


def frost_crust(t: Tex, salt: str, depth: int = 2) -> None:
    """Connected crust along the top edge with short drips."""
    for x in range(1, 15):
        if hrand(x, 0, salt) % 100 < 88:
            frost_px(t, x, 0, 0, salt)
            length = 1 + hrand(x, 1, salt) % depth
            for r in range(1, length):
                frost_px(t, x, r, r, salt)


# --- faces -----------------------------------------------------------------------------------------
PULSE = (0, 1, 2, 1)             # coolant glow level of the core per animation frame


def head_ring(x: int, y: int) -> int:
    """Ring index of a front pixel around the sensor head: 0 the 2x2 core, 1 inner loop, 2 gap, 3 outer loop,
    4 the seam of the recess, higher outside. Octagonal so each ring is a clean one pixel band."""
    ax, ay = abs(x + 0.5 - HEAD[0]), abs(y + 0.5 - HEAD[1])
    return round(max(ax, ay, (ax + ay) * 0.72) - 0.5)


def sensor_head(t: Tex, glow_frame: int | None) -> None:
    c = HEAD
    for y in range(16):
        for x in range(16):
            k = head_ring(x, y)
            lit = toward_light(c, x, y)
            if k == 4:
                t.px(x, y, hpick(FRAME, 0, 1, x, y, "seam", 1) if lit > -0.2 else hpick(PLATE, 3, 4, x, y, "seam", 1))
            elif k in (1, 3):
                # Pickup loops: raised cobalt, lit from the top left
                i = 4 if lit > 0.6 else 3 if lit > -0.1 else 2 if lit > -0.6 else 1
                if glow_frame is not None:
                    i -= 1  # backlit by the glow: the loops read darker
                t.px(x, y, hpick(COBALT, i, i, x, y, f"loop{k}", 1))
            elif k == 2:
                if glow_frame is None:
                    t.px(x, y, hpick(VOID, 1, 3, x, y, "gap", 1))
                else:
                    # Brighter than the loops, so they stay readable as dark bands in the glow
                    lvl = PULSE[glow_frame] + 1
                    t.px(x, y, hpick(CRYO_GLOW, lvl, lvl, x, y, f"gap{glow_frame}", 2))
            elif k == 0:
                # The SQUID itself behind glass
                if glow_frame is None:
                    t.px(x, y, VOID[0] if lit < 0.5 else PLATE[1])
                else:
                    t.px(x, y, hpick(CRYO_GLOW, 2 + PULSE[glow_frame] // 2, 3, x, y, f"core{glow_frame}", 2))


def front(glow_frame: int | None = None) -> Tex:
    t = Tex(seed=f"{NAME}_front")
    t.machine_shell()
    t.plate((1, 1, 14, 10))
    sensor_head(t, glow_frame)
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


def side(active: bool = False) -> Tex:
    t = Tex(seed=f"{NAME}_side")
    t.machine_shell()
    t.plate((1, 1, 14, 6))
    # Three sensor pads in a row: recessed sockets with a cobalt pad lit from the top left
    for px0 in (3, 7, 11):
        t.px(px0 - 1, 3, FRAME[0])
        t.px(px0 - 1, 4, FRAME[0])
        t.px(px0, 2, FRAME[0])
        t.px(px0 + 1, 2, FRAME[0])
        t.px(px0, 3, hpick(COBALT, 3, 4, px0, 3, "pad", 1))
        t.px(px0 + 1, 3, hpick(COBALT, 2, 3, px0 + 1, 3, "pad", 1))
        t.px(px0, 4, hpick(COBALT, 2, 2, px0, 4, "pad", 1))
        t.px(px0 + 1, 4, hpick(COBALT, 1, 1, px0 + 1, 4, "pad", 1))
        t.px(px0 + 2, 3, PLATE[4])
        t.px(px0 + 2, 4, PLATE[4])
        t.px(px0, 5, PLATE[4])
        t.px(px0 + 1, 5, PLATE[4])
    t.line(1, 7, 14, 7, FRAME[1])
    coolant_line(t, 9, active, "side")
    t.checker_strip(13)
    if active:
        frost_crust(t, "side_frost", depth=3)
    return t


def top(active: bool = False) -> Tex:
    t = Tex(seed=f"{NAME}_top")
    t.machine_shell()
    t.plate((2, 2, 13, 13))
    t.bolts(3)
    c = (8.0, 8.0)
    # Dewar lid: a raised steel disc with a cobalt ring and the boil off bore in the middle
    for x, y in ring_pixels(c, 5.3, 4.6):
        t.px(x, y, hpick(FRAME, 0, 2, x, y, "lidseam", 1))
    for x, y in ring_pixels(c, 4.6, 3.0):
        lit = toward_light(c, x, y)
        t.px(x, y, hpick(PLATE, 3, 4, x, y, "lid", 1) if lit > 0.35 else hpick(PLATE, 1, 2, x, y, "lid", 1) if lit > -0.35
             else hpick(PANEL, 3, 4, x, y, "lid", 1))
    lit_ring(t, c, 3.0, 2.0, COBALT, "lidring")
    for x, y in ring_pixels(c, 2.0):
        t.px(x, y, hpick(VOID, 0, 1, x, y, "bore", 1) if toward_light(c, x, y) > 0 else hpick(VOID, 2, 3, x, y, "bore", 1))
    if active:
        # Cold vapour freezes on the lid: rime on the bottom right of the disc, never the whole face
        for x, y in ring_pixels(c, 4.6, 3.0):
            a = math.degrees(math.atan2(y + 0.5 - c[1], x + 0.5 - c[0]))
            if -30 <= a <= 150 and hrand(x, y, "lidfrost") % 100 < 85:
                frost_px(t, x, y, 0 if hrand(x, y, "lidlvl") % 2 else 1, "lidfrost")
    return t


def back() -> Tex:
    t = Tex(seed=f"{NAME}_back")
    t.machine_shell()
    t.plate((1, 1, 14, 11))
    t.bolts(2)
    c = (8.0, 6.5)
    for x, y in ring_pixels(c, 4.4, 3.5):
        t.px(x, y, hpick(FRAME, 1, 3, x, y, "seam", 1))
    lit_ring(t, c, 3.5, 2.6, COBALT, "ring")
    for x, y in ring_pixels(c, 2.6):
        t.px(x, y, hpick(VOID, 0, 1, x, y, "bore", 1) if toward_light(c, x, y) > 0 else hpick(VOID, 2, 3, x, y, "bore", 1))
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


def main() -> None:
    out = [
        front().save(f"{NAME}/front.png"),
        save_animation([front(f) for f in range(4)], f"{NAME}/front_active.png", frametime=8, interpolate=True),
        side().save(f"{NAME}/side.png"),
        side(active=True).save(f"{NAME}/side_active.png"),
        top().save(f"{NAME}/top.png"),
        top(active=True).save(f"{NAME}/top_active.png"),
        back().save(f"{NAME}/back.png"),
        bottom().save(f"{NAME}/bottom.png"),
    ]
    for p in out:
        print(p.relative_to(REPO))


if __name__ == "__main__":
    main()
