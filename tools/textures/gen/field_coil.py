"""Fusion Field Coils: a flat ring of superconducting magnet blocks around a Mekanism fusion reactor.

The ring is many blocks long and up to three loops tall, so the coil side has to tile seamlessly
sideways (the ring reads as one continuous band) and stack cleanly (each loop keeps a thin seam):

    field_coil/side(_active)     cryostat jacket: a steel rim on top, the superconducting tape winding
                                 (silver lines on dark gaps) held by two clamp straps, a Cryo Cobalt
                                 trim along the bottom. Active: a rime crust grows along the top edge,
                                 and a cryo glow pulse runs sideways along the winding and the trim
                                 (8 frames). Its period is the block width, so on a ring the pulse
                                 flows from block to block all the way round
    field_coil/top(_active)      bolted cryostat cover with cobalt corner marks; symmetric, because a
                                 ring block can face either way. Active: frost in two corners
    field_coil_controller/front(_active)
                                 control panel: framed window over the winding, a cobalt trim and the
                                 checkered strip of Mekanism machines. Active: the window shows coolant
                                 glow, the panel frosts at the top and the trim carries the ring's pulse
    field_coil_controller/top(_active)
                                 the chemical port tubes connect to: steel flange, cobalt ring, dark
                                 bore. Active: a frost patch on the flange

The controller's other faces use the coil side, since it is one block of a loop.

Run from the repo root:  python3 tools/textures/gen/field_coil.py
"""
import sys
import zlib
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from texlib import *  # noqa: E402,F403

NAME = "field_coil"
C = (8.0, 8.0)
WIND = (4, 11)          # winding rows
CLAMPS = (2, 10)        # left column of each 2 pixel clamp strap; period 8 so the side tiles sideways
FRAMES = 8              # glow pulse animation frames; the pulse moves 2 pixels per frame


def hrand(x: int, y: int, salt: str) -> int:
    """Stable per pixel random number, so the active variant keeps the idle steel byte identical."""
    return zlib.crc32(f"{NAME}:{salt}:{x}:{y}".encode())


def hpick(ramp, lo: int, hi: int, x: int, y: int, salt: str, jitter: int = 2):
    h = hrand(x, y, salt)
    c = ramp[lo + h % (hi - lo + 1)]
    return shade(c, (h >> 8) % (2 * jitter + 1) - jitter)


def rime(t: Tex, salt: str, depth: int = 3, coverage: float = 0.8, cols=range(16)) -> None:
    """A connected frost crust along the top edge, over every column (texlib's frost leaves the outer
    columns bare, which would show as a gap every block along a ring). Drips only hang where the pixel
    above is frosted."""
    for x in cols:
        h = hrand(x, 0, salt)
        if (h % 1000) / 1000 >= coverage:
            continue
        length = 1 + ((h >> 10) % 2) + ((h >> 11) % 4 == 0) * (depth - 2)
        for r in range(min(length, depth)):
            pick = (hrand(x, r, salt + "c") % 10) < 7
            t.px(x, r, FROST[3 - min(r, 2)] if pick else FROST[2 - min(r, 2)])


def corner_frost(t: Tex, corner: str, size: int, salt: str) -> None:
    for y in range(size):
        for x in range(size - y):
            if (hrand(x, y, salt + corner) % 100) / 100 < 0.85 - 0.15 * (x + y):
                xx = x if "l" in corner else 15 - x
                yy = y if "t" in corner else 15 - y
                t.px(xx, yy, FROST[3 - min(2, (x + y) // 2)])


# --- coil side --------------------------------------------------------------------------------------
def pulse(x: int, frame: int) -> float:
    """Brightness of the travelling glow at column x: a narrow peak that wraps every 16 pixels."""
    import math
    phase = (x - frame * 16 / FRAMES) / 16 * 2 * math.pi
    return max(0.0, math.cos(phase)) ** 6


def glow_rows(t: Tex, frame: int, tape_rows, trim_rows, cols=range(16)) -> None:
    """A soft glow where the pulse passes: tape lines and the cobalt trim light up, the gaps stay dark.
    Palette colours only."""
    for x in cols:
        w = pulse(x, frame)
        if w < 0.2:
            continue
        for y in tape_rows:
            t.px(x, y, CRYO_GLOW[2] if w > 0.8 else CRYO_GLOW[1] if w > 0.45 else COBALT[4])
        for y in trim_rows:
            t.px(x, y, CRYO_GLOW[1] if w > 0.6 else COBALT[4])


def coil_side(active: bool = False, frame: int = 0) -> Tex:
    t = Tex(seed=f"{NAME}_side")
    for x in range(16):
        # Seams top and bottom only: neighbours along the ring join without a line
        t.px(x, 0, hpick(FRAME, 0, 1, x, 0, "seam", 1))
        t.px(x, 15, hpick(FRAME, 0, 1, x, 15, "seam", 1))
        # Raised jacket rim: light top row, plate below, shadow under it
        t.px(x, 1, hpick(PLATE, 3, 4, x, 1, "rim", 2))
        t.px(x, 2, hpick(PLATE, 1, 2, x, 2, "rim", 2))
        t.px(x, 3, hpick(FRAME, 2, 3, x, 3, "rim", 1))
        # Lower jacket under the cobalt trim
        t.px(x, 14, hpick(PANEL, 1, 3, x, 14, "low", 2))
    # Superconducting tape winding: silver tape lines on dark interlayer gaps. Each tape line is lit a
    # little more toward the top (light from above) and gets an occasional brighter glint.
    for y in range(WIND[0], WIND[1] + 1):
        tape = (y - WIND[0]) % 2 == 0
        for x in range(16):
            if tape:
                lo = 2 if y < 8 else 1
                c = hpick(PLATE, lo, lo + 2, x, y, "tape", 2)
                if hrand(x, y, "glint") % 23 == 0:
                    c = GLINT[1]
            else:
                c = hpick(FRAME, 0, 2, x, y, "gap", 1)
            t.px(x, y, c)
    # Clamp straps across the winding, lit on the left, shadowed on the right, with a bolt head
    for cx in CLAMPS:
        for y in range(WIND[0] - 1, WIND[1] + 2):
            t.px(cx, y, hpick(PLATE, 0, 1, cx, y, "clamp", 1))
            t.px(cx + 1, y, hpick(FRAME, 3, 4, cx + 1, y, "clamp", 1))
        t.px(cx, WIND[0] + 1, GLINT[0])
        t.px(cx + 1, WIND[0] + 2, FRAME[0])
        t.px(cx, WIND[1] - 1, GLINT[0])
    # Cryo Cobalt trim along the bottom of the jacket, 2 pixels (12 percent of the face)
    for x in range(16):
        t.px(x, 12, hpick(COBALT, 2, 3, x, 12, "trim", 1))
        t.px(x, 13, hpick(COBALT, 1, 1, x, 13, "trim", 1))
    if active:
        glow_rows(t, frame, [y for y in range(WIND[0], WIND[1] + 1) if (y - WIND[0]) % 2 == 0], (12,))
        # Clamp straps stay steel: restore them over the glow
        for cx in CLAMPS:
            for y in range(WIND[0] - 1, WIND[1] + 2):
                t.px(cx, y, hpick(PLATE, 0, 1, cx, y, "clamp", 1))
                t.px(cx + 1, y, hpick(FRAME, 3, 4, cx + 1, y, "clamp", 1))
        rime(t, "side")
    return t


# --- coil top ---------------------------------------------------------------------------------------
def coil_top(active: bool = False) -> Tex:
    t = Tex(seed=f"{NAME}_top")
    for y in range(16):
        for x in range(16):
            t.px(x, y, hpick(PANEL, 1, 3, x, y, "body", 2))
    t.outline((0, 0, 15, 15), FRAME[1])
    # Bolted cover plate
    for y in range(3, 13):
        for x in range(3, 13):
            t.px(x, y, hpick(PLATE, 0, 3, x, y, "cover", 2))
    t.bevel((3, 3, 12, 12), light=PLATE[4], dark=PANEL[0])
    t.outline((2, 2, 13, 13), FRAME[2])
    for x, y in ((4, 4), (11, 4), (4, 11), (11, 11)):
        t.px(x, y, GLINT[1])
        t.px(x + 1, y + 1, FRAME[1])
    # Cobalt corner marks, the same at every corner
    for cx, cy, sx, sy in ((1, 1, 1, 1), (14, 1, -1, 1), (1, 14, 1, -1), (14, 14, -1, -1)):
        t.px(cx, cy, COBALT[3])
        t.px(cx + sx, cy, COBALT[2])
        t.px(cx, cy + sy, COBALT[2])
    # Seam across the cover, where the two winding halves meet
    for x in range(5, 11):
        t.px(x, 7, hpick(PANEL, 0, 1, x, 7, "split", 1))
        t.px(x, 8, hpick(PLATE, 3, 4, x, 8, "split", 1))
    if active:
        corner_frost(t, "tl", 5, "top")
        corner_frost(t, "br", 4, "top")
    return t


# --- controller -------------------------------------------------------------------------------------
def controller_front(active: bool = False, frame: int = 0) -> Tex:
    t = Tex(seed=f"{NAME}_controller_front")
    t.machine_shell()
    t.plate((2, 2, 13, 9))
    if active:
        t.coolant_window((4, 4, 11, 7))
    else:
        t.window((4, 4, 11, 7))
    # Status LEDs beside the window: cobalt when cold, dark when idle
    t.px(12, 4, COBALT[4] if active else FRAME[2])
    t.px(12, 6, COBALT[3] if active else FRAME[2])
    # Cobalt trim and Mekanism's checkered strip below the panel
    for x in range(2, 14):
        t.px(x, 10, hpick(COBALT, 2, 3, x, 10, "trim", 1))
        t.px(x, 11, hpick(COBALT, 1, 1, x, 11, "trim", 1))
    t.checker_strip(13)
    if active:
        glow_rows(t, frame, (), (10,), cols=range(2, 14))
        rime(t, "front", depth=2, coverage=0.6, cols=range(1, 15))
    return t


def octagon(half: float, cut: float):
    out = []
    for y in range(16):
        for x in range(16):
            dx, dy = abs(x + 0.5 - C[0]), abs(y + 0.5 - C[1])
            if max(dx, dy) < half and dx + dy < cut:
                out.append((x, y))
    return out


def controller_top(active: bool = False) -> Tex:
    t = Tex(seed=f"{NAME}_controller_top")
    t.machine_shell()
    seam, flange, ring, bore = octagon(5, 8), octagon(4, 6.5), octagon(3, 4.5), octagon(2, 3)
    for x, y in seam:
        if (x, y) not in flange:
            t.px(x, y, hpick(FRAME, 0, 1, x, y, "seam", 1) if x + y > 15 else hpick(FRAME, 2, 3, x, y, "seam", 1))
    for x, y in flange:
        if (x, y) not in ring:
            lit = (x + y) - 15
            i = 4 if lit < -3 else 3 if lit < 0 else 1 if lit < 4 else 0
            t.px(x, y, hpick(PLATE, i, i, x, y, "flange", 1))
    for x, y in ring:
        if (x, y) not in bore:
            lit = (x + y) - 15
            i = 3 if lit < -2 else 2 if lit < 2 else 1
            t.px(x, y, hpick(COBALT, i, i, x, y, "ring", 1))
    for x, y in bore:
        lit = (x + y) - 15
        t.px(x, y, hpick(VOID, 0, 0, x, y, "bore", 1) if lit < 0 else hpick(VOID, 2, 3, x, y, "bore", 1))
    t.px(6, 5, COBALT[4])
    t.bolts(2)
    if active:
        corner_frost(t, "tl", 5, "ctop")
        # Rime on the upper left of the cold flange
        for x, y in flange:
            if (x, y) not in ring and x + y < 12 and hrand(x, y, "flangefrost") % 3:
                t.px(x, y, FROST[2] if (x + y) % 2 else FROST[1])
    return t


def main() -> None:
    out = []
    for active in (False, True):
        s = "_active" if active else ""
        out.append(coil_top(active).save(f"field_coil/top{s}.png"))
        out.append(controller_top(active).save(f"field_coil_controller/top{s}.png"))
    out.append(coil_side().save("field_coil/side.png"))
    out.append(controller_front().save("field_coil_controller/front.png"))
    out.append(save_animation([coil_side(True, f) for f in range(FRAMES)], "field_coil/side_active.png", frametime=2))
    out.append(save_animation([controller_front(True, f) for f in range(FRAMES)], "field_coil_controller/front_active.png", frametime=2))
    for p in out:
        print(p)


if __name__ == "__main__":
    main()
