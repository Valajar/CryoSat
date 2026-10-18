"""Rocket: the model the Launch Pad shows before and during a launch (client/rocket/RocketRenderer).

The rocket is 6 blocks tall and too big for one block model (elements must stay within -16..32), so it is
three stacked segment models, each 2 blocks tall and drawn by RocketRenderer one above the other:

    engine    nozzle bell, darker aft body with engine bay vents and a hazard band, four fins that reach the pad
    tank      propellant tank with a seam, rivet rows and a tier band near the top
    payload   fairing with an MLI gold panel, stepped nose cone

All three share one cross section: two overlapping boxes (20x14 and 14x20 pixels) give a body 1.25 blocks
across with stepped corners, which reads as round from a distance. Fins reach 23 pixels from the axis, inside
RocketRenderer.RADIUS (24).

Tier colour comes from the renderer as a tint, not from textures: faces with tintindex 0 (nozzle lip, fin tips,
tank band) take the rocket tier's colour, faces with tintindex 1 (nose tip) the payload tier's. Those faces map
to a light grey "accent" area of the sheet, so the tint multiplies into a muted tier colour close to Mekanism's
own valve ramps. Everything else is untinted steel.

This script writes the three 64x64 sheets (textures/block/rocket/<segment>.png) and the three models
(models/rocket/<segment>.json), so UVs always match the sheets. Run from the repo root:

    python3 tools/textures/gen/rocket.py              textures and models
    python3 tools/textures/gen/rocket.py --preview    also build/rocket_preview.png: all four tiers, the
                                                      whole rocket assembled, next to Mekanism's digital miner
"""
import json
import re
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from texlib import *  # noqa: E402,F403
from model_preview import face_frame  # noqa: E402

NAME = "rocket"
SHEET = 64
UV = SHEET / 16  # texels per UV unit: one texel per model pixel
MODELS = REPO / "src" / "main" / "resources" / "assets" / "cryosat" / "models" / NAME
SEGMENTS = ("engine", "tank", "payload")
SEGMENT_HEIGHT = 32  # model pixels; RocketRenderer stacks the segments 2 blocks apart
# Mekanism's BaseTier colours, which RocketTier.color() hands to the renderer as the tint
TIER_COLORS = {"basic": "5fffb8", "advanced": "ff806a", "elite": "4bf8ff", "ultimate": "f787ff"}

# Shared regions (x, y, w, h) in texels. Every sheet has these two.
PLAIN = (0, 40, 24, 24)   # small faces: fin edges, ledges
ACCENT = (40, 40, 24, 24)  # light grey, tinted in game


# --- UV mapping ----------------------------------------------------------------------------------------
def dims(d, f, t):
    if d in ("north", "south"):
        return t[0] - f[0], t[1] - f[1]
    if d in ("east", "west"):
        return t[2] - f[2], t[1] - f[1]
    return t[0] - f[0], t[2] - f[2]


class Seen:
    """Maps a face onto a region as seen from outside, centred horizontally. Side faces line up vertically
    with the region's top at model height `top`; with top None (and for up and down faces) they are centred.
    Centring is what lets the two boxes of the cross section share one region: the 14 wide face takes the
    middle columns, the 20 wide face shows only its outer 3 columns on each side (the stepped corners)."""

    def __init__(self, region, top=None):
        self.region, self.top = region, top

    def uv(self, d, f, t):
        rx, ry, rw, rh = self.region
        w, h = dims(d, f, t)
        u0 = rx + (rw - w) / 2
        v0 = ry + (rh - h) / 2 if self.top is None or d in ("up", "down") else ry + self.top - t[1]
        assert rx <= u0 and u0 + w <= rx + rw and ry <= v0 and v0 + h <= ry + rh, (d, f, t, self.region)
        return [u0, v0, u0 + w, v0 + h]


class Profile:
    """Maps the flat sides of a fin onto one fin profile, by distance from the body surface, so every fin
    piece shows its part of one continuous drawing and both sides of a fin match."""

    def __init__(self, region, axis, sign, top):
        self.region, self.axis, self.sign, self.top = region, axis, sign, top

    def uv(self, d, f, t):
        rx, ry, rw, rh = self.region
        corner, uvec, vvec = face_frame(d, f, t)
        tl = corner
        br = tuple(corner[i] + uvec[i] + vvec[i] for i in range(3))
        a = 0 if self.axis == "x" else 2

        def col(p):
            return rx + self.sign * (p[a] - 8) - 10

        uv = [col(tl), ry + self.top - tl[1], col(br), ry + self.top - br[1]]
        assert all(rx <= u <= rx + rw for u in uv[::2]) and all(ry <= v <= ry + rh for v in uv[1::2]), (d, f, t)
        return uv


SIDES = ("north", "south", "east", "west")
ALL = SIDES + ("up", "down")


class Model:
    def __init__(self, segment):
        self.segment = segment
        self.elements, self.groups = [], {}

    def box(self, group, name, f, t, mapping, faces=ALL, tint=None, overrides=None):
        """Adds an element. `mapping` covers `faces`; `overrides` maps a direction to (mapping, tint) for faces
        that differ, or to None to leave that face out (hidden faces)."""
        assert all(-16 <= v <= 32 for v in (*f, *t)), (name, f, t)
        out = {}
        spec = {d: (mapping, tint) for d in faces}
        spec.update(overrides or {})
        for d in ALL:
            if spec.get(d) is None:
                continue
            m, ti = spec[d]
            face = {"uv": [round(v / UV, 4) for v in m.uv(d, f, t)], "texture": "#all"}
            if ti is not None:
                face["tintindex"] = ti
            out[d] = face
        self.groups.setdefault(group, []).append(len(self.elements))
        self.elements.append({"name": name, "from": list(f), "to": list(t), "faces": out})

    def cross(self, group, name, half, y0, y1, mapping, notch=3, **kw):
        """The stepped round cross section: a box `half` wide either way in x and `half - notch` in z, and
        the same turned a quarter."""
        n = half - notch
        self.box(group, f"{name}_x", (8 - half, y0, 8 - n), (8 + half, y1, 8 + n), mapping, **kw)
        self.box(group, f"{name}_z", (8 - n, y0, 8 - half), (8 + n, y1, 8 + half), mapping, **kw)

    def json(self):
        tex = f"cryosat:block/{NAME}/{self.segment}"
        return {
            "credit": "Generated by tools/textures/gen/rocket.py, do not edit by hand",
            "ambientocclusion": False,
            "render_type": "minecraft:cutout",
            "texture_size": [SHEET, SHEET],
            "textures": {"all": tex, "particle": tex},
            "elements": self.elements,
            "groups": [{"name": g, "origin": [8, 8, 8], "color": 0, "children": c} for g, c in self.groups.items()],
        }


def fmt(v):
    return int(v) if float(v).is_integer() else v


def write_model(m: Model) -> Path:
    def clean(o):
        if isinstance(o, dict):
            return {k: clean(v) for k, v in o.items()}
        if isinstance(o, list):
            return [clean(v) for v in o]
        return fmt(o) if isinstance(o, float) else o

    MODELS.mkdir(parents=True, exist_ok=True)
    out = MODELS / f"{m.segment}.json"
    text = json.dumps(clean(m.json()), indent=2)
    # Number lists on one line, as Blockbench writes them
    text = re.sub(r"\[\s*([-\d.,\s]+?)\s*\]", lambda g: "[" + ", ".join(v.strip() for v in g.group(1).split(",")) + "]", text)
    out.write_text(text + "\n")
    return out


# --- painting helpers ----------------------------------------------------------------------------------
def sheet(segment) -> Tex:
    t = Tex(SHEET, SHEET, seed=f"{NAME}_{segment}", transparent=True)
    t.fill(box(PLAIN), PANEL, 1, 4)
    # Tinted faces: light neutral grey (GLINT), so Mekanism's bright tier colours multiply down to about the
    # valve ramp values. Gentle noise only: these faces are small and tinted, structure would read as dirt.
    t.fill(box(ACCENT), GLINT, 0, 2, jitter=3)
    return t


def box(region):
    x, y, w, h = region
    return x, y, x + w - 1, y + h - 1


def body(t: Tex, region, rivet_rows=(), dark=False):
    """Body side region, 20 wide: columns 0..2 and 17..19 are the stepped corners (outer edge lit, inner
    corner in shadow), 3..16 the main panel (lit left edge, darker right edge). Top row catches the light,
    bottom row is the seam to the segment below."""
    x, y, w, h = region
    t.fill((x, y, x + 2, y + h - 1), PLATE, 0, 3)
    t.fill((x + 17, y, x + 19, y + h - 1), PLATE, 0, 3)
    t.fill((x + 3, y, x + 16, y + h - 1), *((PLATE, 0, 3) if dark else (CASING, 1, 3)))
    for r in range(h):
        t.px(x, y + r, shade(PLATE[4], t.rng.randint(-2, 2)))
        t.px(x + 2, y + r, shade(PANEL[1], t.rng.randint(-2, 2)))
        t.px(x + 17, y + r, shade(PANEL[1], t.rng.randint(-2, 2)))
        t.px(x + 19, y + r, shade(PLATE[2], t.rng.randint(-2, 2)))
        t.px(x + 3, y + r, shade(PLATE[4] if dark else CASING[4], t.rng.randint(-2, 2)))
        t.px(x + 16, y + r, shade(PLATE[1], t.rng.randint(-2, 2)))
    for c in range(w):
        t.px(x + c, y, shade(CASING[5] if 3 <= c <= 16 else PLATE[4], t.rng.randint(-3, 0)))
        t.px(x + c, y + h - 1, shade(FRAME[3], t.rng.randint(-2, 2)))
    for r in rivet_rows:
        for c in (5, 9, 10, 14):
            t.px(x + c, y + r, GLINT[2])
            t.px(x + c + 1, y + r + 1, PLATE[0])


def seam(t: Tex, region, row):
    """Horizontal panel seam across the whole body region: dark line, lit row under it."""
    x, y, w, _ = region
    for c in range(w):
        t.px(x + c, y + row, shade(FRAME[3], t.rng.randint(-2, 2)))
        t.px(x + c, y + row + 1, shade(CASING[4] if 3 <= c <= 16 else PLATE[4], t.rng.randint(-2, 0)))


def hazard_band(t: Tex, region, row):
    x, y, w, _ = region
    for dy in range(2):
        for c in range(w):
            t.px(x + c, y + row + dy, HAZARD[0] if (c + dy) % 4 < 2 else FRAME[0])


def cap(t: Tex, region, light=CASING, lo=2, hi=4):
    """Up facing ledge or cap, centred in its region: noisy steel with a lit edge."""
    x, y, w, h = region
    t.fill(box(region), light, lo, hi)
    t.bevel(box(region), light=light[min(len(light) - 1, hi + 1)], dark=PLATE[0])


# --- engine ----------------------------------------------------------------------------------------------
E_BODY = (0, 0, 20, 22)       # body sides, rows are y 32..10
E_UNDER = (20, 0, 20, 20)     # underside of the body around the nozzle
E_FIN = (40, 0, 14, 32)       # fin profile: column = distance from the body surface, rows are y 32..0
E_BELL = (0, 22, 12, 10)      # nozzle sides, rows are y 10..0
E_EXIT = (12, 22, 12, 12)     # nozzle exit, seen from below
E_LEDGE = (24, 22, 12, 12)    # up facing nozzle steps
FIN_PIECES = (  # (name, radius from, radius to, y from, y to): a root fillet up the body, the main plate, and a
    # tinted outer edge that stands on the pad
    ("root", 10, 15, 20, 28), ("plate", 10, 20, 2, 20), ("edge", 20, 23, 0, 16))
FIN_HALF = 1  # fins are 2 pixels thick


def engine():
    t = sheet("engine")
    body(t, E_BODY, rivet_rows=(2,), dark=True)
    x, y, _, _ = E_BODY
    t.vent((x + 6, y + 5, x + 13, y + 10))
    t.outline((x + 5, y + 4, x + 14, y + 11), FRAME[1])
    hazard_band(t, E_BODY, 16)
    t.fill((x + 3, y + 18, x + 16, y + 20), PLATE, 0, 2)
    # Underside: dark plate with a ring where the nozzle mounts
    t.fill(box(E_UNDER), FRAME, 2, 4)
    ux, uy, _, _ = E_UNDER
    t.outline((ux + 6, uy + 6, ux + 13, uy + 13), PANEL[2])
    # Fin profile: one drawing for every fin, the steel root fillet and plate as one swept shape with a lit
    # stepped top edge, a dark lower edge and a faint seam where the pieces meet
    fx, fy, _, _ = E_FIN
    cover = {}
    for name, r0, r1, y0, y1 in FIN_PIECES[:2]:
        for r in range(r0, r1):
            lo, hi = cover.get(r, (y0, y1))
            cover[r] = (min(lo, y0), max(hi, y1))
    for r, (y0, y1) in cover.items():
        c = fx + r - 10
        t.fill((c, fy + 32 - y1, c, fy + 32 - y0 - 1), PANEL, 1, 4)
        t.px(c, fy + 32 - y1, shade(PLATE[3], t.rng.randint(-2, 2)))
        t.px(c, fy + 32 - y0 - 1, shade(FRAME[2], t.rng.randint(-2, 2)))
    root = FIN_PIECES[0]
    for r in range(root[1], root[2]):  # seam where the root fillet sits on the plate
        t.px(fx + r - 10, fy + 32 - root[3], shade(FRAME[4], t.rng.randint(-1, 1)))
    for row in (6, 10, 14, 18, 22, 26):  # rivets along the root, where the fin bolts to the body
        t.px(fx + 1, fy + row, GLINT[1])
    # Nozzle: dark heat stained steel, a lit stripe down the left, darker toward the exit
    bx, by, bw, bh = E_BELL
    t.fill(box(E_BELL), PANEL, 0, 2)
    t.fill((bx, by, bx + bw - 1, by + 1), FRAME, 1, 3)  # throat
    for r in range(2, bh):
        t.px(bx + 2 + (r > 4), by + r, shade(PLATE[2], t.rng.randint(-2, 2)))
        t.px(bx + bw - 3 - (r > 4), by + r, shade(FRAME[2], t.rng.randint(-2, 2)))
    for c in range(bw):
        t.px(bx + c, by + 4, shade(FRAME[1], t.rng.randint(-1, 1)))  # weld seam between neck and bell
    # Exit, seen from below: steel rim, black bore with a faint inner ring
    t.fill(box(E_EXIT), PANEL, 1, 3)
    ex, ey, ew, eh = E_EXIT
    t.fill((ex + 1, ey + 1, ex + ew - 2, ey + eh - 2), VOID, 0, 3, jitter=1)
    t.outline((ex + 3, ey + 3, ex + ew - 4, ey + eh - 4), FRAME[1])
    cap(t, E_LEDGE, light=PANEL, lo=1, hi=3)

    m = Model("engine")
    acc = Seen(ACCENT)
    plain = Seen(PLAIN)
    # Nozzle, from the exit up; the lip is the tinted ring
    m.box("nozzle", "nozzle_lip", (2, 0, 2), (14, 2, 14), acc, tint=0, overrides={"down": (Seen(E_EXIT), None)})
    m.box("nozzle", "nozzle_bell", (2.5, 2, 2.5), (13.5, 5, 13.5), Seen(E_BELL, 10), faces=SIDES,
          overrides={"up": (Seen(E_LEDGE), None)})
    m.box("nozzle", "nozzle_neck", (3.5, 5, 3.5), (12.5, 8, 12.5), Seen(E_BELL, 10), faces=SIDES,
          overrides={"up": (Seen(E_LEDGE), None)})
    m.box("nozzle", "nozzle_throat", (5, 8, 5), (11, 10, 11), Seen(E_BELL, 10), faces=SIDES)
    # Aft body; its top is covered by the tank
    m.cross("body", "aft_body", 10, 10, 32, Seen(E_BODY, 32), faces=SIDES, overrides={"down": (Seen(E_UNDER), None)})
    # Fins on the four sides, reaching down to the pad
    for axis, sign, label in (("x", 1, "east"), ("x", -1, "west"), ("z", 1, "south"), ("z", -1, "north")):
        for name, r0, r1, y0, y1 in FIN_PIECES:
            a, b = sorted((8 + sign * r0, 8 + sign * r1))
            z0, z1 = 8 - FIN_HALF, 8 + FIN_HALF
            f, to = ((a, y0, z0), (b, y1, z1)) if axis == "x" else ((z0, y0, a), (z1, y1, b))
            inner = ("west" if sign > 0 else "east") if axis == "x" else ("north" if sign > 0 else "south")
            if name == "edge":
                m.box("fins", f"fin_{label}_{name}", f, to, acc, tint=0)
                continue
            flat = ("north", "south") if axis == "x" else ("east", "west")
            over = {d: (Profile(E_FIN, axis, sign, 32), None) for d in flat}
            if name == "root":
                over[inner] = None  # against the body
                over["down"] = None  # on the plate
            m.box("fins", f"fin_{label}_{name}", f, to, plain, overrides=over)
    return t, m


# --- tank ------------------------------------------------------------------------------------------------
T_BODY = (0, 0, 20, 32)  # rows are local y 32..0
BAND = (24, 28)          # tier band, local y


def tank():
    t = sheet("tank")
    body(t, T_BODY, rivet_rows=(13, 29))
    seam(t, T_BODY, 15)
    m = Model("tank")
    m.cross("body", "tank_body", 10, 0, 32, Seen(T_BODY, 32), faces=SIDES)
    m.cross("band", "tier_band", 10.5, BAND[0], BAND[1], Seen(ACCENT), notch=3, tint=0)
    return t, m


# --- payload ---------------------------------------------------------------------------------------------
P_BODY = (0, 0, 20, 16)      # fairing sides, rows are local y 16..0
P_SHOULDER = (20, 0, 20, 20)  # fairing top, around the nose
P_NOSE1 = (40, 0, 16, 6)      # nose base sides, rows are local y 22..16
P_NOSE1_TOP = (40, 6, 16, 16)
P_NOSE2 = (0, 18, 10, 5)      # nose middle sides, rows are local y 27..22
P_NOSE2_TOP = (10, 18, 10, 10)


def payload():
    t = sheet("payload")
    body(t, P_BODY, rivet_rows=(1,))
    x, y, _, _ = P_BODY
    # MLI gold panel over the payload bay, the space hardware signature
    t.outline((x + 5, y + 3, x + 14, y + 12), FRAME[1])
    t.gold_foil((x + 6, y + 4, x + 13, y + 11))
    cap(t, P_SHOULDER)
    nx, ny, nw, nh = P_NOSE1
    t.fill(box(P_NOSE1), CASING, 2, 4)
    for c in range(nw):
        t.px(nx + c, ny, shade(CASING[5], t.rng.randint(-3, 0)))
        t.px(nx + c, ny + nh - 1, shade(PLATE[1], t.rng.randint(-2, 2)))
    for r in range(nh):  # stepped corners as on the body, scaled down
        t.px(nx + 2, ny + r, shade(PLATE[0], t.rng.randint(-2, 2)))
        t.px(nx + 13, ny + r, shade(PLATE[0], t.rng.randint(-2, 2)))
    cap(t, P_NOSE1_TOP)
    n2x, n2y, n2w, n2h = P_NOSE2
    t.fill(box(P_NOSE2), CASING, 3, 5)
    for c in range(n2w):
        t.px(n2x + c, n2y + n2h - 1, shade(PLATE[2], t.rng.randint(-2, 2)))
    cap(t, P_NOSE2_TOP, lo=3, hi=4)

    m = Model("payload")
    m.cross("fairing", "fairing", 10, 0, 16, Seen(P_BODY, 16), faces=SIDES, overrides={"up": (Seen(P_SHOULDER), None)})
    m.cross("nose", "nose_base", 8, 16, 22, Seen(P_NOSE1, 22), faces=SIDES, overrides={"up": (Seen(P_NOSE1_TOP), None)})
    m.box("nose", "nose_mid", (3, 22, 3), (13, 27, 13), Seen(P_NOSE2, 27), faces=SIDES,
          overrides={"up": (Seen(P_NOSE2_TOP), None)})
    # Tip in the payload tier's colour (tint index 1)
    m.box("nose", "nose_tip", (5, 27, 5), (11, 30, 11), Seen(ACCENT), faces=SIDES + ("up",), tint=1)
    m.box("nose", "nose_point", (6.5, 30, 6.5), (9.5, 32, 9.5), Seen(ACCENT), faces=SIDES + ("up",), tint=1)
    return t, m


# --- preview ---------------------------------------------------------------------------------------------
def preview(out: Path) -> None:
    from PIL import Image, ImageDraw
    import model_preview as mp

    merged = {"textures": {}, "elements": []}
    for i, seg in enumerate(SEGMENTS):
        model = json.loads((MODELS / f"{seg}.json").read_text())
        merged["textures"][seg] = model["textures"]["all"]
        for el in model["elements"]:
            el = json.loads(json.dumps(el))
            el["from"][1] += i * SEGMENT_HEIGHT
            el["to"][1] += i * SEGMENT_HEIGHT
            for face in el["faces"].values():
                face["texture"] = f"#{seg}"
            merged["elements"].append(el)

    def rgb(h):
        return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4))

    scale = 4
    miner = mp.render(mp.load_model(REPO / ".mekanism-refs/src/main/resources/assets/mekanism/models/block/digital_miner.json"),
                      {}, scale, False) if (REPO / ".mekanism-refs").exists() else None
    shots = [("digital miner", miner)] if miner else []
    for tier, payload_tier in (("basic", "basic"), ("advanced", "advanced"), ("elite", "elite"), ("ultimate", "ultimate"),
                               ("elite", "basic")):
        tints = {0: rgb(TIER_COLORS[tier]), 1: rgb(TIER_COLORS[payload_tier])}
        label = tier if tier == payload_tier else f"{tier}, {payload_tier} payload"
        shots.append((label, mp.render(merged, {}, scale, False, tints)))
    shots.append(("elite NW", mp.render(merged, {}, scale, True, {0: rgb(TIER_COLORS["elite"]), 1: rgb(TIER_COLORS["elite"])})))
    w = sum(s.width for _, s in shots) + 10 * len(shots)
    h = max(s.height for _, s in shots)
    small = [(n, s.resize((s.width // 6, s.height // 6))) for n, s in shots[1:5]]
    sheet_img = Image.new("RGBA", (w, h + 40 + max(s.height for _, s in small)), mp.BG)
    d = ImageDraw.Draw(sheet_img)
    x = 0
    for name, s in shots:
        sheet_img.paste(s, (x, h - s.height + 20))
        d.text((x + 6, 6), name, fill=(220, 220, 220, 255))
        x += s.width + 10
    x = 0
    for name, s in small:  # about how it reads from 20 blocks away
        sheet_img.paste(s, (x + 20, h + 30))
        x += s.width + 30
    d.text((x + 30, h + 40), "distance view", fill=(220, 220, 220, 255))
    out.parent.mkdir(parents=True, exist_ok=True)
    sheet_img.save(out)
    print(out.relative_to(REPO))


def main() -> None:
    for build in (engine, tank, payload):
        t, m = build()
        print(t.save(f"{NAME}/{m.segment}.png").relative_to(REPO))
        print(write_model(m).relative_to(REPO))
    if "--preview" in sys.argv:
        preview(REPO / "build" / "rocket_preview.png")


if __name__ == "__main__":
    main()
