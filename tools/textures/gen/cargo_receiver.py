"""Cargo Receiver and Uplink Card.

Cargo Receiver: 1x1 Mekanism steel machine that catches cargo from orbit. No active state, so there is
no window and no animation.

    front         roller cargo hatch on a raised plate, a thin Cryo Cobalt trim, checkered strip
    top           receiving dish seen from above: light rim, concave bowl, three struts and a
                  feed horn with a Cryo Cobalt ring
    left, right   Mekanism style plate over a darker lower section with a vent, checkered strip
    back          item output port (a recessed square hatch with a grate) on a bolted plate
    bottom        plain inset square

Uplink Card: a Mekanism style card (same outline as the Configuration Card) in light steel, with a
chip on the left and an antenna radiating signal arcs on the right. The linked variant lights the chip
and the arcs in Cryo Cobalt, like the encoded Configuration Card lights its chip.

Run from the repo root:  python3 tools/textures/gen/cargo_receiver.py
"""
import math
import sys
import zlib
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from texlib import *  # noqa: E402,F403

NAME = "cargo_receiver"
ITEM_ROOT = ASSETS / "item"


def hrand(x: int, y: int, salt: str) -> int:
    return zlib.crc32(f"{NAME}:{salt}:{x}:{y}".encode())


def hpick(ramp, lo: int, hi: int, x: int, y: int, salt: str, jitter: int = 2):
    h = hrand(x, y, salt)
    c = ramp[lo + h % (hi - lo + 1)]
    return shade(c, (h >> 8) % (2 * jitter + 1) - jitter)


def coverage(inside, x: int, y: int, n: int = 4) -> float:
    hits = sum(inside(x + (i + 0.5) / n, y + (j + 0.5) / n) for i in range(n) for j in range(n))
    return hits / (n * n)


def ring_pixels(c, r_out: float, r_in: float = 0.0):
    out = []
    for y in range(16):
        for x in range(16):
            cov = coverage(lambda px, py: r_in <= math.hypot(px - c[0], py - c[1]) < r_out, x, y)
            if cov >= 0.5:
                out.append((x, y))
    return out


def toward_light(c, x: int, y: int) -> float:
    """+1 on the top left of a circle around c, -1 on the bottom right."""
    a = math.atan2(y + 0.5 - c[1], x + 0.5 - c[0])
    return -math.cos(a - math.radians(45))


# --- Cargo Receiver --------------------------------------------------------------------------------
def front() -> Tex:
    t = Tex(seed=f"{NAME}_front")
    t.machine_shell()
    t.plate((1, 1, 14, 10))
    # Roller hatch: a recessed opening holding horizontal slats, each lit on top and shadowed below.
    t.inset((3, 2, 12, 9), light=PLATE[3], dark=FRAME[0])
    for y in range(3, 9):
        for x in range(4, 12):
            row = (y - 3) % 2
            t.px(x, y, hpick(PANEL, 2, 4, x, y, "slat") if row == 0 else hpick(FRAME, 2, 4, x, y, "gap", 1))
    # Handle on the bottom slat
    for x in (7, 8):
        t.px(x, 8, GLINT[0])
    t.cobalt_trim((1, 11, 14, 12))
    t.checker_strip(13)
    return t


def top() -> Tex:
    t = Tex(seed=f"{NAME}_top")
    t.machine_shell()
    for x, y in ((2, 2), (13, 2), (2, 13), (13, 13)):
        t.px(x, y, GLINT[0])
    c = (8.0, 8.0)
    for y in range(16):
        for x in range(16):
            r = math.hypot(x + 0.5 - c[0], y + 0.5 - c[1])
            lit = toward_light(c, x, y)
            if 6.0 <= r < 6.9:
                # Seam around the dish mount
                t.px(x, y, hpick(FRAME, 0, 1, x, y, "seam", 1))
            elif 5.0 <= r < 6.0:
                # Rim: raised, lit on the top left
                t.px(x, y, hpick(GLINT, 0, 1, x, y, "rim", 1) if lit > 0.4 else
                     hpick(PLATE, 2, 4, x, y, "rim", 1) if lit > -0.4 else hpick(PANEL, 2, 3, x, y, "rim", 1))
            elif 1.9 <= r < 5.0:
                # Bowl: concave, so the far (bottom right) wall catches the light, the near wall is shaded
                if lit < -0.4:
                    t.px(x, y, hpick(PLATE, 1, 3, x, y, "bowl", 1))
                elif lit < 0.4:
                    t.px(x, y, hpick(PANEL, 3, 4, x, y, "bowl", 1))
                else:
                    t.px(x, y, hpick(PANEL, 0, 1, x, y, "bowl", 1))
            elif 1.2 <= r < 1.9:
                # Dark collar of the feed horn
                t.px(x, y, hpick(FRAME, 0, 1, x, y, "collar", 1))
    # Three struts holding the feed horn
    for angle in (-90, 30, 150):
        a = math.radians(angle)
        for step in (2.4, 3.4, 4.4):
            x = math.floor(c[0] + math.cos(a) * step)
            y = math.floor(c[1] + math.sin(a) * step)
            t.px(x, y, hpick(FRAME, 2, 3, x, y, "strut", 1))
    # Feed horn: Cryo Cobalt, lit on its top left pixel
    t.px(7, 7, COBALT[4])
    t.px(8, 7, COBALT[3])
    t.px(7, 8, COBALT[3])
    t.px(8, 8, COBALT[2])
    return t


def side(seed: str) -> Tex:
    t = Tex(seed=f"{NAME}_{seed}")
    t.machine_shell()
    t.plate((1, 1, 14, 7))
    t.line(1, 8, 14, 8, FRAME[1])
    t.inset((3, 9, 12, 12), light=PANEL[4], dark=FRAME[0])
    t.vent((4, 10, 11, 11))
    t.checker_strip(13)
    return t


def back() -> Tex:
    t = Tex(seed=f"{NAME}_back")
    t.machine_shell()
    t.plate((1, 1, 14, 11))
    t.bolts(2)
    # Item output hatch: recessed square with a grate
    t.inset((4, 3, 11, 9), light=PLATE[4], dark=FRAME[0])
    t.fill((5, 4, 10, 8), VOID, 1, 3, jitter=1)
    for x in (6, 8, 10):
        for y in range(4, 9):
            t.px(x, y, hpick(FRAME, 3, 4, x, y, "grate", 1))
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


# --- Uplink Card -----------------------------------------------------------------------------------
CARD = (1, 3, 14, 12)
CHIP = (3, 5, 7, 9)
TIP = (11, 5)
INNER_ARC = ((10, 4), (12, 4), (10, 6), (12, 6))
OUTER_ARC = ((9, 5), (13, 5))
MAST = ((11, 6), (11, 7), (11, 8), (10, 9), (11, 9), (12, 9))
CONTACTS = range(9, 13)


def card(linked: bool) -> Tex:
    t = Tex(seed="uplink_card", transparent=True)
    x0, y0, x1, y1 = CARD
    # Light steel body, one step darker on the lower third, like the Mekanism card's printed band
    t.fill((x0, y0, x1, y1), CASING, 4, 5, jitter=2)
    t.fill((x0 + 1, 10, x1 - 1, y1 - 1), CASING, 3, 4, jitter=2)
    # Edges: lit top and left, dark right and bottom
    for x in range(x0, x1):
        t.px(x, y0, hpick(CASING, 3, 4, x, y0, "top", 1))
    for y in range(y0, y1):
        t.px(x0, y, hpick(CASING, 2, 3, x0, y, "left", 1))
    for y in range(y0, y1 + 1):
        t.px(x1, y, hpick(PLATE, 1, 2, x1, y, "right", 1))
    for x in range(x0, x1 + 1):
        t.px(x, y1, hpick(PLATE, 0, 1, x, y1, "bottom", 1))
    # Chip
    cx0, cy0, cx1, cy1 = CHIP
    if linked:
        t.fill(CHIP, COBALT, 1, 1, jitter=1)
        t.fill((cx0 + 1, cy0 + 1, cx1 - 1, cy1 - 1), CRYO_GLOW, 0, 1, jitter=2)
        t.px(cx0 + 2, cy0 + 2, CRYO_GLOW[3])
        t.px(cx0 + 1, cy0 + 1, CRYO_GLOW[2])
    else:
        t.fill(CHIP, PANEL, 0, 1, jitter=1)
        t.fill((cx0 + 1, cy0 + 1, cx1 - 1, cy1 - 1), PANEL, 2, 3, jitter=1)
    # Antenna: dark mast, a tip and two signal arcs either side
    for x, y in MAST:
        t.px(x, y, hpick(FRAME, 2, 4, x, y, "mast", 1))
    if linked:
        t.px(*TIP, CRYO_GLOW[3])
        for x, y in INNER_ARC:
            t.px(x, y, CRYO_GLOW[1])
        for x, y in OUTER_ARC:
            t.px(x, y, COBALT[3])
    else:
        t.px(*TIP, PANEL[3])
        for x, y in INNER_ARC:
            t.px(x, y, FRAME[4])
        for x, y in OUTER_ARC:
            t.px(x, y, PANEL[1])
    # Contacts along the bottom edge
    for x in CONTACTS:
        t.px(x, y1, COBALT[3] if x % 2 else COBALT[2])
    return t


def main() -> None:
    out = [
        front().save(f"{NAME}/front.png"),
        side("left").save(f"{NAME}/left.png"),
        side("right").save(f"{NAME}/right.png"),
        top().save(f"{NAME}/top.png"),
        back().save(f"{NAME}/back.png"),
        bottom().save(f"{NAME}/bottom.png"),
        card(False).save("uplink_card.png", root=ITEM_ROOT),
        card(True).save("uplink_card_linked.png", root=ITEM_ROOT),
    ]
    for p in out:
        print(p.relative_to(REPO))


if __name__ == "__main__":
    main()
