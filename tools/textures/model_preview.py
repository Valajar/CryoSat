"""Isometric preview of a Java block model (the JSON Blockbench exports), for checking 3D models
without a game client.

    python3 tools/textures/model_preview.py MODEL.json OUT.png [--tex KEY=PNG ...] [--scale N]

Draws the model from the south east and from the north west, side by side, with Minecraft style
face shading. Texture references like "cryosat:block/models/foo" resolve against this repo's assets
and against .mekanism-refs (see fetch_mekanism_refs.sh). Use --tex to override a texture variable,
for example --tex valve=build/valve_elite.png to preview one tier.

Supported: elements with from/to, per face uv, face rotation, element rotation (any axis, any
angle). Not supported: parent model elements (only textures are inherited), rescale, tint.
"""

from __future__ import annotations

import argparse
import json
import math
from pathlib import Path

from PIL import Image, ImageDraw

REPO = Path(__file__).resolve().parents[2]
ROOTS = [REPO / "src" / "main" / "resources" / "assets", REPO / "src" / "generated" / "resources" / "assets",
         REPO / ".mekanism-refs" / "src" / "main" / "resources" / "assets"]
SHADE = {"up": 1.0, "down": 0.5, "north": 0.8, "south": 0.8, "east": 0.6, "west": 0.6}
BG = (40, 40, 48, 255)


def load_model(path: Path) -> dict:
    model = json.loads(path.read_text())
    textures = dict(model.get("textures", {}))
    parent = model.get("parent")
    while parent and ":" in parent and not parent.startswith("minecraft:") and not parent.startswith("block/"):
        ns, p = parent.split(":", 1)
        found = next((r / ns / "models" / f"{p}.json" for r in ROOTS if (r / ns / "models" / f"{p}.json").exists()), None)
        if not found:
            break
        pm = json.loads(found.read_text())
        textures = {**pm.get("textures", {}), **textures}
        model.setdefault("elements", pm.get("elements"))
        parent = pm.get("parent")
    model["textures"] = textures
    return model


def resolve(ref: str, textures: dict, overrides: dict, cache: dict) -> Image.Image | None:
    seen = 0
    while ref.startswith("#") and seen < 10:
        key = ref[1:]
        if key in overrides:
            ref = overrides[key]
            break
        ref = textures.get(key, "")
        seen += 1
    if ref in cache:
        return cache[ref]
    img = None
    p = Path(ref)
    if ref.endswith(".png") and p.exists():
        img = Image.open(p).convert("RGBA")
    elif ref:
        ns, path = ref.split(":", 1) if ":" in ref else ("minecraft", ref)
        for r in ROOTS:
            f = r / ns / "textures" / f"{path}.png"
            if f.exists():
                img = Image.open(f).convert("RGBA")
                img = img.crop((0, 0, img.width, min(img.height, img.width)))  # first animation frame
                break
    if img is None and ref:
        print(f"warning: texture not found: {ref}")
    cache[ref] = img
    return img


def face_frame(direction: str, f, t):
    """Corner, u axis and v axis (world vectors) of a face, matching Minecraft's UV orientation."""
    (x0, y0, z0), (x1, y1, z1) = f, t
    return {
        "up": ((x0, y1, z0), (x1 - x0, 0, 0), (0, 0, z1 - z0)),
        "down": ((x0, y0, z1), (x1 - x0, 0, 0), (0, 0, z0 - z1)),
        "north": ((x1, y1, z0), (x0 - x1, 0, 0), (0, y0 - y1, 0)),
        "south": ((x0, y1, z1), (x1 - x0, 0, 0), (0, y0 - y1, 0)),
        "east": ((x1, y1, z1), (0, 0, z0 - z1), (0, y0 - y1, 0)),
        "west": ((x0, y1, z0), (0, 0, z1 - z0), (0, y0 - y1, 0)),
    }[direction]


def default_uv(direction: str, f, t):
    (x0, y0, z0), (x1, y1, z1) = f, t
    return {
        "up": (x0, z0, x1, z1), "down": (x0, 16 - z1, x1, 16 - z0),
        "north": (16 - x1, 16 - y1, 16 - x0, 16 - y0), "south": (x0, 16 - y1, x1, 16 - y0),
        "east": (16 - z1, 16 - y1, 16 - z0, 16 - y0), "west": (z0, 16 - y1, z1, 16 - y0),
    }[direction]


def rotate_point(p, rot):
    if not rot:
        return p
    ox, oy, oz = rot.get("origin", (8, 8, 8))
    a = math.radians(rot.get("angle", 0))
    c, s = math.cos(a), math.sin(a)
    x, y, z = p[0] - ox, p[1] - oy, p[2] - oz
    axis = rot.get("axis", "y")
    if axis == "x":
        y, z = y * c - z * s, y * s + z * c
    elif axis == "y":
        x, z = x * c + z * s, -x * s + z * c
    else:
        x, y = x * c - y * s, x * s + y * c
    return x + ox, y + oy, z + oz


NORMALS = {"up": (0, 1, 0), "down": (0, -1, 0), "north": (0, 0, -1), "south": (0, 0, 1), "east": (1, 0, 0), "west": (-1, 0, 0)}


def render(model: dict, overrides: dict, scale: int, flip: bool) -> Image.Image:
    cache: dict = {}
    polys = []
    view = (1, 1, 1)
    for el in model.get("elements", []):
        f, t, rot = el["from"], el["to"], el.get("rotation")
        for direction, face in el.get("faces", {}).items():
            n = rotate_point(tuple(8 + v for v in NORMALS[direction]), rot and {**rot, "origin": (8, 8, 8)})
            n = tuple(a - 8 for a in n)
            if flip:
                n = (-n[0], n[1], -n[2])
            if sum(a * b for a, b in zip(n, view)) <= 1e-6:
                continue
            tex = resolve(face.get("texture", ""), model["textures"], overrides, cache)
            if tex is None:
                continue
            u0, v0, u1, v1 = face.get("uv") or default_uv(direction, f, t)
            corner, uvec, vvec = face_frame(direction, f, t)
            k = tex.width / 16
            nu = max(1, round(abs(u1 - u0) * k))
            nv = max(1, round(abs(v1 - v0) * k))
            frot = face.get("rotation", 0)
            for i in range(nu):
                for j in range(nv):
                    s, r = (i + 0.5) / nu, (j + 0.5) / nv
                    # Face rotation turns the texture on the face; sample with rotated coordinates.
                    ss, rr = {0: (s, r), 90: (r, 1 - s), 180: (1 - s, 1 - r), 270: (1 - r, s)}[frot]
                    tx = min(tex.width - 1, int((u0 + (u1 - u0) * ss) * k))
                    ty = min(tex.height - 1, int((v0 + (v1 - v0) * rr) * k))
                    col = tex.getpixel((tx, ty))
                    if col[3] < 128:
                        continue
                    pts = []
                    for a, b in ((i, j), (i + 1, j), (i + 1, j + 1), (i, j + 1)):
                        p = tuple(corner[m] + uvec[m] * a / nu + vvec[m] * b / nv for m in range(3))
                        p = rotate_point(p, rot)
                        if flip:
                            p = (16 - p[0], p[1], 16 - p[2])
                        pts.append(p)
                    cx, cy, cz = (sum(p[m] for p in pts) / 4 for m in range(3))
                    sh = SHADE[direction]
                    polys.append((cx + cy + cz, pts, tuple(int(c * sh) for c in col[:3])))
    polys.sort(key=lambda q: q[0])
    c30, s30 = math.cos(math.radians(30)), math.sin(math.radians(30))

    def proj(p):
        x, y, z = p
        return (x - z) * c30 * scale, -y * scale + (x + z) * s30 * scale

    projected = [([proj(p) for p in pts], col) for _, pts, col in polys]
    xs = [x for q, _ in projected for x, _ in q] or [0]
    ys = [y for q, _ in projected for _, y in q] or [0]
    pad = 2 * scale
    ox, oy = min(xs) - pad, min(ys) - pad
    img = Image.new("RGBA", (int(max(xs) - ox + pad), int(max(ys) - oy + pad)), BG)
    d = ImageDraw.Draw(img)
    for q, col in projected:
        d.polygon([(x - ox, y - oy) for x, y in q], fill=(*col, 255), outline=(*col, 255))
    return img


def main() -> None:
    ap = argparse.ArgumentParser()
    ap.add_argument("model", type=Path)
    ap.add_argument("out", type=Path)
    ap.add_argument("--tex", action="append", default=[], help="KEY=path.png or KEY=namespace:path")
    ap.add_argument("--scale", type=int, default=10)
    a = ap.parse_args()
    overrides = dict(x.split("=", 1) for x in a.tex)
    model = load_model(a.model)
    se, nw = render(model, overrides, a.scale, False), render(model, overrides, a.scale, True)
    out = Image.new("RGBA", (se.width + nw.width, max(se.height, nw.height) + 20), BG)
    out.paste(se, (0, 20))
    out.paste(nw, (se.width, 20))
    ImageDraw.Draw(out).text((6, 6), f"{a.model.name}   SE view | NW view", fill=(220, 220, 220, 255))
    out.save(a.out)
    print(a.out)


if __name__ == "__main__":
    main()
