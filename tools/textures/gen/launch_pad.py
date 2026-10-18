"""Launch Pad: a flat 5x5 multiblock of deck plating with a controller and fuel ports on the outer ring.

The formed pad is a floor seen mostly from above, with its one block high edge seen from the side.
Every part shares the same two faces so the pad reads as one platform, tier neutral:

    plating_top          steel deck plate: casing coloured rim (so neighbouring tiles meet in a seam,
                         like Mekanism casings) around a diamond tread field
    plating_side         the pad edge: a light lip on top, a 2 pixel hazard band, darker skirt below
    plating_bottom       plain inset square
    fuel_port_top        deck plate with a filler cap: steel flange, Cryo Cobalt ring (the pad takes
                         cryogenic oxidiser), dark bore
    fuel_port_side       pad edge with the same port, for tubes and pipes on the outside of the pad
    controller_top       deck plate with a recessed console grille
    plating_top_<piece>  connected texture pieces of plating_top (see tools/textures/connected.py)
    controller_front     pad edge with a console screen. Idle: dark glass. Active (a rocket stands on
    (_active)            the pad): a small rocket glyph and a countdown bar filling up, 8 frames

Run from the repo root:  python3 tools/textures/gen/launch_pad.py
"""
import sys
import zlib
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from texlib import *  # noqa: E402,F403
from connected import SUFFIX, build_pieces, quarter  # noqa: E402

NAME = "launch_pad"
C = (8.0, 8.0)


def hrand(x: int, y: int, salt: str) -> int:
    """Stable per pixel random number, so animated frames keep byte identical steel."""
    return zlib.crc32(f"{NAME}:{salt}:{x}:{y}".encode())


def hpick(ramp, lo: int, hi: int, x: int, y: int, salt: str, jitter: int = 2):
    h = hrand(x, y, salt)
    c = ramp[lo + h % (hi - lo + 1)]
    return shade(c, (h >> 8) % (2 * jitter + 1) - jitter)


# --- deck --------------------------------------------------------------------------------------
def deck(seed: str) -> Tex:
    """Deck plate. A 1 pixel rim lit on the top and left and shadowed on the bottom and right, so a
    floor of these shows a clean 2 pixel seam between tiles, around a field of raised diamond tread.
    """
    t = Tex(seed=f"{NAME}_{seed}")
    t.fill((0, 0, 15, 15), PANEL, 3, 4, jitter=2)
    for i in range(16):
        t.px(i, 0, hpick(PLATE, 3, 4, i, 0, "rim"))
        t.px(0, i, hpick(PLATE, 3, 4, 0, i, "rim"))
        t.px(i, 15, hpick(FRAME, 2, 3, i, 15, "rim"))
        t.px(15, i, hpick(FRAME, 2, 3, 15, i, "rim"))
    # Tread: short diagonal bars in alternating directions on a 4 pixel grid, lit along the bar and
    # shadowed on its lower right, like the raised pattern of a steel deck plate.
    for gy in range(2, 14, 4):
        for gx in range(2, 14, 4):
            flip = ((gx + gy) // 4) % 2
            bar = ((gx, gy + 1), (gx + 1, gy)) if flip else ((gx, gy), (gx + 1, gy + 1))
            for i, (x, y) in enumerate(bar):
                t.px(x, y, hpick(PLATE, 3, 4, x, y, "tread", 1) if i == 0 else hpick(PLATE, 1, 2, x, y, "tread", 1))
                if (x + 1, y + 1) not in bar:
                    t.px(x + 1, y + 1, hpick(FRAME, 3, 4, x + 1, y + 1, "tread_shadow", 1))
    for x, y in ((1, 1), (14, 1), (1, 14), (14, 14)):
        t.px(x, y, GLINT[0])
    return t


def plating_top() -> Tex:
    return deck("plating_top")


# --- connected deck (see tools/textures/connected.py) ------------------------------------------
# The plating top as five quarter pieces, so the 5x5 pad reads as one deck with the rim only around
# its outline and around the controller and fuel ports. Where a side connects, its rim goes and the
# tread lattice carries on across the joint (cells every 4 pixels, so a cell sits on pixels 14..15
# and casts its shadow into the next block). The rim is lit on the top and left, dark on the bottom
# and right, lit wins where they meet (as in deck); the glints sit on outer corners only.
TREAD = (2, 6, 10)


def tread_cell(gx: int, gy: int) -> dict:
    """Pixels of one tread cell: the lit and dim halves of the bar and its shadow (as in deck)."""
    flip = ((gx + gy) // 4) % 2
    bar = ((gx, gy + 1), (gx + 1, gy)) if flip else ((gx, gy), (gx + 1, gy + 1))
    out = {bar[0]: "tread_hi", bar[1]: "tread_lo"}
    for x, y in bar:
        if (x + 1, y + 1) not in bar:
            out[(x + 1, y + 1)] = "tread_shadow"
    return out


def deck_feature(piece: str, x: int, y: int) -> str:
    right, bottom = quarter(x, y)
    ex, ey = (15 if right else 0), (15 if bottom else 0)
    h = piece in ("horizontal", "inner", "center")  # the side neighbour connects
    v = piece in ("vertical", "inner", "center")    # the neighbour above or below connects
    hb, vb = not v, not h                           # rim along the top/bottom, along the left/right
    if (hb and y == ey) or (vb and x == ex) or (piece == "inner" and (x, y) == (ex, ey)):
        # Lit wins where rims cross, so only the bottom right corner is dark when both meet there.
        if (hb and y == ey and not bottom) or (vb and x == ex and not right) or (piece == "inner" and not (right and bottom)):
            return "rim_lit"
        return "rim_dark"
    if piece == "corner" and (x, y) in ((1, 1), (14, 1), (1, 14), (14, 14)):
        return "glint"
    # Cells on this block's own lattice and on the neighbours' (offset by 16), where they exist.
    xs = [g for g in TREAD] + ([14 if right else -2] if h else [])
    ys = [g for g in TREAD] + ([14 if bottom else -2] if v else [])
    for gx in xs:
        for gy in ys:
            if gx not in TREAD and gy not in TREAD and piece != "center":
                continue
            f = tread_cell(gx, gy).get((x, y))
            if f:
                return f
    return "plate"


def deck_pieces(seed: str = "plating_top") -> dict:
    base = Tex(seed=f"{NAME}_{seed}")
    base.fill((0, 0, 15, 15), PANEL, 3, 4, jitter=2)  # the plate under everything, same rng as deck

    def synth(feature: str, x: int, y: int):
        return {
            "plate": lambda: base.get(x, y),
            "rim_lit": lambda: hpick(PLATE, 3, 4, x, y, "rim"),
            "rim_dark": lambda: hpick(FRAME, 2, 3, x, y, "rim"),
            "tread_hi": lambda: hpick(PLATE, 3, 4, x, y, "tread", 1),
            "tread_lo": lambda: hpick(PLATE, 1, 2, x, y, "tread", 1),
            "tread_shadow": lambda: hpick(FRAME, 3, 4, x, y, "tread_shadow", 1),
            "glint": lambda: GLINT[0],
        }[feature]()

    iso = deck(seed).img
    # Every pixel of the plain deck must follow from the feature map, or the pieces would not line up.
    for y in range(16):
        for x in range(16):
            assert iso.getpixel((x, y))[:3] == synth(deck_feature("corner", x, y), x, y), (x, y)
    return build_pieces(iso, deck_feature, synth)


# --- edge --------------------------------------------------------------------------------------
LIP = 2          # rows 0..1: the deck's edge, light like the deck rim
HAZARD_Y = 3     # rows 3..4: hazard band


def edge(seed: str) -> Tex:
    """The pad's 1 block high edge: light lip, dark seam, hazard band, then a darker steel skirt with a
    seam a few pixels up from the ground and bolts."""
    t = Tex(seed=f"{NAME}_{seed}")
    t.fill((0, 0, 15, LIP - 1), CASING, 1, 3, jitter=2)
    for x in range(16):
        t.px(x, 0, hpick(CASING, 3, 4, x, 0, "lip"))
        t.px(x, LIP, hpick(FRAME, 1, 2, x, LIP, "lipseam", 1))
    # Hazard band checkered like Mekanism's factories
    for dy in range(2):
        for x in range(16):
            t.px(x, HAZARD_Y + dy, HAZARD[0] if (x + dy) % 2 == 0 else FRAME[0])
    t.fill((0, HAZARD_Y + 2, 15, 15), PANEL, 1, 3, jitter=2)
    for x in range(16):
        t.px(x, HAZARD_Y + 2, hpick(FRAME, 0, 1, x, HAZARD_Y + 2, "hazshadow", 1))
        t.px(x, 13, hpick(FRAME, 2, 3, x, 13, "skirtseam", 1))
        t.px(x, 14, hpick(PANEL, 3, 4, x, 14, "skirtlit", 1))
        t.px(x, 15, hpick(FRAME, 1, 2, x, 15, "foot", 1))
    for x in (2, 13):
        t.px(x, 7, GLINT[0])
        t.px(x + 1, 8, FRAME[0])
    return t


def plating_side() -> Tex:
    return edge("plating_side")


def plating_bottom() -> Tex:
    t = Tex(seed=f"{NAME}_plating_bottom")
    t.fill((0, 0, 15, 15), PANEL, 0, 2, jitter=2)
    t.outline((0, 0, 15, 15), FRAME[2])
    t.inset((3, 3, 12, 12), light=PANEL[4], dark=FRAME[1])
    return t


# --- fuel port ---------------------------------------------------------------------------------
def octagon(cx: float, cy: float, half: float, cut: float):
    out = []
    for y in range(16):
        for x in range(16):
            dx, dy = abs(x + 0.5 - cx), abs(y + 0.5 - cy)
            if max(dx, dy) < half and dx + dy < cut:
                out.append((x, y))
    return out


def port(t: Tex, cx: float, cy: float, size: float) -> None:
    """Round fuel connection: shadowed seam, raised steel flange, Cryo Cobalt ring, dark bore."""
    seam = octagon(cx, cy, size + 1, (size + 1) * 1.6)
    flange = octagon(cx, cy, size, size * 1.6)
    ring = octagon(cx, cy, size - 1, (size - 1) * 1.5)
    bore = octagon(cx, cy, size - 2, (size - 2) * 1.5)
    diag = cx + cy - 1
    for x, y in seam:
        if (x, y) not in flange:
            t.px(x, y, hpick(FRAME, 0, 1, x, y, "seam", 1) if x + y > diag else hpick(FRAME, 2, 3, x, y, "seam", 1))
    for x, y in flange:
        if (x, y) not in ring:
            lit = (x + y) - diag
            i = 5 if lit < -3 else 4 if lit < 0 else 2 if lit < 4 else 1
            t.px(x, y, hpick(CASING, i, i, x, y, "flange", 1))
    for x, y in ring:
        if (x, y) not in bore:
            lit = (x + y) - diag
            i = 3 if lit < -1 else 2 if lit < 2 else 1
            t.px(x, y, hpick(COBALT, i, i, x, y, "ring", 1))
    for x, y in bore:
        lit = (x + y) - diag
        t.px(x, y, hpick(VOID, 0, 0, x, y, "bore", 1) if lit < 0 else hpick(VOID, 2, 3, x, y, "bore", 1))


def fuel_port_top() -> Tex:
    t = deck("fuel_port_top")
    port(t, *C, 5)
    t.px(5, 5, COBALT[4])
    return t


def fuel_port_side() -> Tex:
    t = edge("fuel_port_side")
    port(t, 8.0, 9.5, 4)
    return t


# --- controller --------------------------------------------------------------------------------
def controller_top() -> Tex:
    t = deck("controller_top")
    # Recessed console grille in the middle of the deck plate
    box = (4, 5, 11, 10)
    t.fill(box, FRAME, 0, 1, jitter=1)
    t.vent((5, 6, 10, 9))
    t.inset((4, 5, 11, 10), light=PLATE[4], dark=FRAME[0])
    return t


SCREEN = (3, 6, 12, 12)          # glass, inside a 1 pixel rim
ROCKET = [(5, 7), (5, 8), (4, 9), (5, 9), (6, 9), (5, 10), (4, 11), (6, 11)]  # nose, body, fins, nozzle legs
BAR_Y = 11
BAR_X = (8, 11)
FRAMES = 8


def controller_front(frame: int = 0, active: bool = False) -> Tex:
    t = edge("controller_front")
    x0, y0, x1, y1 = SCREEN
    t.outline((x0 - 1, y0 - 1, x1 + 1, y1 + 1), FRAME[0])
    t.inset((x0 - 1, y0 - 1, x1 + 1, y1 + 1), light=PANEL[4], dark=FRAME[0])
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            t.px(x, y, hpick(VOID, 2, 3, x, y, "screen", 1) if (y - y0) % 2 else hpick(VOID, 0, 1, x, y, "screen", 1))
    if not active:
        for gx, gy, c in ((4, 7, PANEL[0]), (5, 8, FRAME[4]), (9, 7, FRAME[4])):
            t.px(gx, gy, c)
        return t
    # A rocket stands on the pad: its glyph on the left, a countdown bar filling on the right, then
    # the whole bar blinks once before it starts again.
    for x, y in ROCKET:
        t.px(x, y, CRYO_GLOW[3] if y < 9 else CRYO_GLOW[2])
    t.px(5, 7, CRYO_GLOW[3])
    for x in range(BAR_X[0], BAR_X[1] + 1):
        t.px(x, BAR_Y, COBALT[0])
    filled = min(frame, BAR_X[1] - BAR_X[0] + 1)
    blink = frame >= FRAMES - 2 and frame % 2 == 1
    for i in range(filled):
        t.px(BAR_X[0] + i, BAR_Y, CRYO_GLOW[0] if blink else CRYO_GLOW[2])
    # Status ticks above the bar: one lit per filled quarter
    for i in range(4):
        t.px(BAR_X[0] + i, 8, CRYO_GLOW[1] if i < filled else COBALT[0])
    return t


def main() -> None:
    d = NAME
    out = [
        plating_top().save(f"{d}/plating_top.png"),
        plating_side().save(f"{d}/plating_side.png"),
        plating_bottom().save(f"{d}/plating_bottom.png"),
        fuel_port_top().save(f"{d}/fuel_port_top.png"),
        fuel_port_side().save(f"{d}/fuel_port_side.png"),
        controller_top().save(f"{d}/controller_top.png"),
        controller_front().save(f"{d}/controller_front.png"),
        save_animation([controller_front(f, True) for f in range(FRAMES)], f"{d}/controller_front_active.png", frametime=5),
    ]
    # Connected deck pieces; the corner piece is plating_top.png itself.
    for name, img in deck_pieces().items():
        if SUFFIX[name]:
            path = ASSETS / "block" / d / f"plating_top{SUFFIX[name]}.png"
            img.save(path)
            out.append(path)
    for p in out:
        print(p.relative_to(REPO))


if __name__ == "__main__":
    main()
