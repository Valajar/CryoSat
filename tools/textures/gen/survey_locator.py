"""Survey Locator: a tech compass, one texture per needle frame.

A round housing in Mekanism item steel (lit top left, dark bottom right, its side wall showing below)
with a stub antenna at the top right whose tip is a Cryo Cobalt LED. Inside, a dark bezel with Cryo
Cobalt ticks at the four cardinal points, the brightest at the top, holds a black screen. The needle is
drawn in coolant glow on the screen, with a plain steel tail and pivot.

Every frame shares the same housing pixel for pixel; only the needle turns. Frame 0 points straight up
(target ahead) and the frames turn clockwise, matching the item model overrides in
datagen/locator/LocatorModelProvider (FRAMES there must equal FRAMES here).

    survey_locator_00.png .. survey_locator_15.png

Run from the repo root:  python3 tools/textures/gen/survey_locator.py
"""
import math
import sys
import zlib
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from texlib import *  # noqa: E402,F403

NAME = "survey_locator"
ITEMS = ASSETS / "item"
FRAMES = 16

# Pixel (CX, CY) is the centre of the dial; distances are measured between pixel centres. The housing
# is a disc seen from above, with its side wall showing one pixel lower at the bottom for depth.
CX, CY = 7, 7
HOUSING = 6.4
BEZEL = 5.0
SCREEN = 4.2
NEEDLE = 3.4
TAIL = 2.0
ANTENNA = ((12, 2), (13, 1))
ANTENNA_TIP = (14, 0)


def hrand(x: int, y: int, salt: str) -> int:
    return zlib.crc32(f"{NAME}:{salt}:{x}:{y}".encode())


def hpick(ramp, lo: int, hi: int, x: int, y: int, salt: str, jitter: int = 2):
    h = hrand(x, y, salt)
    lo, hi = max(0, lo), min(len(ramp) - 1, hi)
    c = ramp[lo + h % (hi - lo + 1)] if hi >= lo else ramp[hi]
    return shade(c, (h >> 8) % (2 * jitter + 1) - jitter)


def lit(x: int, y: int) -> float:
    """How much a rim pixel faces the top left light, -1 to 1."""
    dx, dy = x - CX, y - CY
    d = math.hypot(dx, dy) or 1
    return (-dx - dy) / (d * math.sqrt(2))


def housing() -> Tex:
    t = Tex(seed=NAME, transparent=True)
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - CX, y - CY)
            wall = math.hypot(x - CX, y - CY - 1.4)
            if d > HOUSING:
                if wall <= HOUSING:
                    # Side wall below the rim: dark steel, the item's bottom outline
                    t.px(x, y, hpick(ITEM_STEEL, 0, 1, x, y, "wall", 2))
                continue
            light = lit(x, y)
            if d > BEZEL:
                # Rim: lit steel, near white top left, dark bottom right
                i = round(5 + 3.5 * light)
                t.px(x, y, hpick(ITEM_STEEL, i, i, x, y, "rim", 3))
            elif d > SCREEN:
                # Bezel: a recessed dark ring, lighter on the bottom right where it faces the light
                t.px(x, y, hpick(FRAME, round(1 - 1.5 * light), round(2 - 1.5 * light), x, y, "bezel", 1))
            else:
                # Screen: a black window like Mekanism's
                t.px(x, y, hpick(VOID, 0, 2, x, y, "screen", 1))
    # Cardinal ticks on the bezel, the top one brightest
    for (x, y), c in (((CX, CY - 5), COBALT[4]), ((CX + 5, CY), COBALT[2]), ((CX, CY + 5), COBALT[1]), ((CX - 5, CY), COBALT[2])):
        t.px(x, y, c)
    # Glare specks on the glass, top left
    t.px(CX - 3, CY - 2, FRAME[3])
    t.px(CX - 2, CY - 3, FRAME[3])
    t.px(CX - 3, CY - 1, FRAME[1])
    # Antenna stub from the top right of the rim, LED tip
    for x, y in ANTENNA:
        t.px(x, y, hpick(ITEM_STEEL, 2, 3, x, y, "antenna", 1))
    t.px(*ANTENNA_TIP, COBALT[4])
    return t


def endpoint(angle: float, length: float) -> tuple[int, int]:
    """The pixel a ray of about this length from the dial centre ends on, kept inside the screen."""
    while True:
        x, y = round(length * math.sin(angle)), round(-length * math.cos(angle))
        if math.hypot(x, y) <= SCREEN or length < 1:
            return CX + x, CY + y
        length -= 0.1


def line(x1: int, y1: int) -> list[tuple[int, int]]:
    """Bresenham line from the dial centre (excluded) to (x1, y1), so every angle gives a clean stroke."""
    x0, y0 = CX, CY
    dx, dy = abs(x1 - x0), -abs(y1 - y0)
    sx, sy = (1 if x1 > x0 else -1), (1 if y1 > y0 else -1)
    err = dx + dy
    out = []
    while (x0, y0) != (x1, y1):
        e2 = 2 * err
        if e2 >= dy:
            err += dy
            x0 += sx
        if e2 <= dx:
            err += dx
            y0 += sy
        out.append((x0, y0))
    return out


def frame(base: Tex, i: int) -> Tex:
    t = Tex(seed=NAME, transparent=True)
    t.img = base.img.copy()
    angle = 2 * math.pi * i / FRAMES
    # Tail in plain steel, like the grey half of a vanilla needle, so the glowing end is clearly the pointer
    for n, (x, y) in enumerate(line(*endpoint(angle + math.pi, TAIL))):
        t.px(x, y, ITEM_STEEL[2] if n == 0 else ITEM_STEEL[1])
    head = line(*endpoint(angle, NEEDLE))
    for n, (x, y) in enumerate(head):
        t.px(x, y, CRYO_GLOW[3] if n == len(head) - 1 else CRYO_GLOW[2] if n >= len(head) - 2 else CRYO_GLOW[1])
    t.px(CX, CY, ITEM_STEEL[7])
    return t


def main() -> None:
    base = housing()
    for i in range(FRAMES):
        print(frame(base, i).save(f"{NAME}_{i:02d}.png", root=ITEMS).relative_to(REPO))


if __name__ == "__main__":
    main()
