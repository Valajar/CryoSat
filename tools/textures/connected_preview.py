"""Preview connected textures on whole multiblocks, without a game client.

    python3 tools/textures/connected_preview.py OUT.png [--scene plant|pad] [--connect SET ...]

Builds the scene block by block, picks every face's four quarter pieces with the same rule the Java
model uses (connected.face_pieces, mirroring io.github.valajar.cryosat.connected.ConnectedTextures),
composes the face textures from the generated pieces and draws each scene with model_preview's
isometric renderer (SE and NW views), plus a flat front view of one wall at a larger scale.

--connect picks what the connected block treats as a connection, to compare choices side by side:
    self       only blocks of its own kind
    parts      every part of its multiblock (valves, coolers, ports, controller)
    glass      parts plus Mekanism structural glass (plant only)
Several sets render one row each.
"""

from __future__ import annotations

import argparse
import sys
from pathlib import Path

from PIL import Image, ImageDraw

sys.path.insert(0, str(Path(__file__).resolve().parent))
from connected import FACES, PIECES, SUFFIX, compose, face_pieces  # noqa: E402
from model_preview import BG, REPO, render  # noqa: E402

TEX = REPO / "src" / "main" / "resources" / "assets" / "cryosat" / "textures" / "block"
MEK = REPO / ".mekanism-refs" / "src" / "main" / "resources" / "assets" / "mekanism" / "textures" / "block"
OUT_DIR = REPO / "build" / "connected_preview"
LABEL = (220, 220, 220, 255)


def load(path: Path) -> Image.Image:
    img = Image.open(path).convert("RGBA")
    return img.crop((0, 0, img.width, img.width))  # first animation frame


def pieces(base: Path) -> dict[str, Image.Image]:
    return {p: load(base.with_name(base.stem + SUFFIX[p] + ".png")) for p in PIECES}


def glass_over_interior(path: Path) -> Image.Image:
    """Cutout glass drawn over the dark inside of the multiblock."""
    glass = load(path)
    inside = Image.new("RGBA", glass.size, (24, 26, 30, 255))
    inside.alpha_composite(glass)
    return inside


# --- scenes ----------------------------------------------------------------------------------------
# A scene is {pos: block}, and blocks map to {face: texture} or to a connected piece set.
def plant(n: int) -> dict:
    m = n - 1
    blocks = {}
    for x in range(n):
        for y in range(n):
            for z in range(n):
                if 0 < x < m and 0 < y < m and 0 < z < m:
                    continue
                blocks[(x, y, z)] = "casing"
    c = n // 2
    blocks[(c, c, m)] = "controller"          # south wall
    if n >= 5:
        blocks[(1, 1, m)] = "valve"
        blocks[(3, 1, m)] = "valve"
        for z in (1, 2, 3):                   # east wall: a glass window strip
            blocks[(m, 2, z)] = "glass"
        blocks[(m, 1, 1)] = "valve"
        blocks[(1, m, 1)] = "cryocooler"      # top
        blocks[(3, m, 3)] = "cryocooler"
        blocks[(3, m, 1)] = "heat_rejection_port"
        blocks[(m, 3, 3)] = "heat_rejection_port"
    else:
        blocks[(m, c, c)] = "valve"
        blocks[(c, m, c)] = "cryocooler"
    return blocks


def plant_textures() -> dict:
    lq = TEX / "liquefaction"
    casing = load(lq / "casing.png")

    def cube(img):
        return {f: img for f in FACES}

    return {
        "casing": ("connected", pieces(lq / "casing.png")),
        "valve": cube(load(lq / "valve.png")),
        "cryocooler": cube(load(lq / "cryocooler.png")),
        "heat_rejection_port": cube(load(lq / "heat_rejection_port.png")),
        "controller": {**cube(casing), "south": load(lq / "controller_front.png")},
        "glass": cube(glass_over_interior(MEK / "structural_glass.png")),
    }


def pad(n: int = 5) -> dict:
    blocks = {(x, 0, z): "plating" for x in range(n) for z in range(n)}
    m, c = n - 1, n // 2
    blocks[(c, 0, m)] = "controller"
    blocks[(0, 0, c)] = "fuel_port"
    blocks[(m, 0, c)] = "fuel_port"
    return blocks


def pad_textures() -> dict:
    lp = TEX / "launch_pad"
    side, bottom = load(lp / "plating_side.png"), load(lp / "plating_bottom.png")
    sides = {f: side for f in ("north", "south", "east", "west")}
    port_side = load(lp / "fuel_port_side.png")
    return {
        "plating": ("connected", pieces(lp / "plating_top.png"), {**sides, "down": bottom}),
        "fuel_port": {**{f: port_side for f in ("north", "south", "east", "west")}, "up": load(lp / "fuel_port_top.png"), "down": bottom},
        "controller": {**sides, "south": load(lp / "controller_front.png"), "up": load(lp / "controller_top.png"), "down": bottom},
    }


CONNECT = {
    "self": lambda kind: {kind},
    "parts": lambda kind: {"casing", "valve", "cryocooler", "heat_rejection_port", "controller", "plating", "fuel_port"},
    "glass": lambda kind: {"casing", "valve", "cryocooler", "heat_rejection_port", "controller", "glass"},
}


# --- rendering -------------------------------------------------------------------------------------
def face_texture(blocks, textures, connect, pos, face, cache) -> Image.Image | None:
    tex = textures[blocks[pos]]
    if isinstance(tex, tuple):
        if len(tex) > 2 and face in tex[2]:
            return tex[2][face]
        parts = connect(blocks[pos])
        quarters = face_pieces(lambda p: blocks.get(p) in parts, pos, face)
        key = (blocks[pos], tuple(quarters))
        if key not in cache:
            cache[key] = compose(tex[1], quarters)
        return cache[key]
    return tex.get(face)


def scene_model(blocks, textures, connect, tag: str) -> tuple[dict, dict]:
    """A model_preview model with one element per block and only its outer faces."""
    OUT_DIR.mkdir(parents=True, exist_ok=True)
    cache, saved, elements = {}, {}, []
    flats = {}
    for pos in sorted(blocks):
        faces = {}
        for face, (normal, _, _) in FACES.items():
            if tuple(a + b for a, b in zip(pos, normal)) in blocks:
                continue
            img = face_texture(blocks, textures, connect, pos, face, cache)
            if img is None:
                continue
            key = id(img)
            if key not in saved:
                path = OUT_DIR / f"{tag}_{len(saved)}.png"
                img.save(path)
                saved[key] = str(path)
            faces[face] = {"texture": saved[key], "uv": [0, 0, 16, 16]}
            flats[(pos, face)] = img
        x, y, z = (16 * v for v in pos)
        elements.append({"from": [x, y, z], "to": [x + 16, y + 16, z + 16], "faces": faces})
    return {"elements": elements, "textures": {}}, flats


def flat_view(blocks, flats, face: str, scale: int) -> Image.Image:
    """Straight on view of every block face pointing along `face`, as the player sees that wall."""
    normal, right, down = FACES[face]
    pts = [(p, f) for p, f in flats if f == face]

    def coord(p):
        return sum(a * b for a, b in zip(p, right)), sum(a * b for a, b in zip(p, down))

    # Only the outermost layer along the normal
    depth = max(sum(a * b for a, b in zip(p, normal)) for p, _ in pts)
    pts = [(p, f) for p, f in pts if sum(a * b for a, b in zip(p, normal)) == depth]
    us, vs = zip(*(coord(p) for p, _ in pts))
    img = Image.new("RGBA", ((max(us) - min(us) + 1) * 16, (max(vs) - min(vs) + 1) * 16), BG)
    for p, f in pts:
        u, v = coord(p)
        img.paste(flats[(p, f)], ((u - min(us)) * 16, (v - min(vs)) * 16))
    return img.resize((img.width * scale, img.height * scale), Image.NEAREST)


def main() -> None:
    ap = argparse.ArgumentParser()
    ap.add_argument("out", type=Path)
    ap.add_argument("--scene", choices=("plant", "pad"), default="plant")
    ap.add_argument("--connect", nargs="*", default=["parts"], choices=sorted(CONNECT))
    ap.add_argument("--scale", type=int, default=4)
    a = ap.parse_args()

    if a.scene == "plant":
        scenes = [("5x5x5", plant(5), "south"), ("3x3x3", plant(3), "south")]
        textures = plant_textures()
    else:
        scenes = [("5x5 pad", pad(5), "up")]
        textures = pad_textures()

    rows = []
    for c in a.connect:
        row = []
        for name, blocks, wall in scenes:
            model, flats = scene_model(blocks, textures, CONNECT[c], f"{a.scene}_{c}_{name}")
            se, nw = render(model, {}, a.scale, False), render(model, {}, a.scale, True)
            fv = flat_view(blocks, flats, wall, 5 if len(blocks) > 30 else 6)
            row.append((f"{name}, connect={c}: SE | NW | {wall} wall", [se, nw, fv]))
        rows.append(row)

    cells = [(label, imgs) for row in rows for label, imgs in row]
    widths = [sum(i.width for i in imgs) + 8 * len(imgs) for _, imgs in cells]
    heights = [max(i.height for i in imgs) + 20 for _, imgs in cells]
    per_row = len(scenes)
    sheet_w = max(sum(widths[r * per_row:(r + 1) * per_row]) for r in range(len(rows)))
    sheet_h = sum(max(heights[r * per_row:(r + 1) * per_row]) for r in range(len(rows)))
    sheet = Image.new("RGBA", (sheet_w, sheet_h), BG)
    d = ImageDraw.Draw(sheet)
    y = 0
    for r in range(len(rows)):
        x = 0
        for i in range(per_row):
            label, imgs = cells[r * per_row + i]
            d.text((x + 6, y + 4), label, fill=LABEL)
            cx = x
            for img in imgs:
                sheet.paste(img, (cx, y + 20))
                cx += img.width + 8
            x += widths[r * per_row + i]
        y += max(heights[r * per_row:(r + 1) * per_row])
    a.out.parent.mkdir(parents=True, exist_ok=True)
    sheet.save(a.out)
    print(a.out)


if __name__ == "__main__":
    main()
