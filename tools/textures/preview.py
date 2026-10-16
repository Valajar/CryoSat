"""Render upscaled preview sheets so textures can be judged by eye.

    python3 tools/textures/preview.py OUT.png [--ref MEKANISM_PNG ...] [--tile] CRYOSAT_PNG ...

Each texture is drawn at 16x scale with its name underneath. Reference textures (from a Mekanism
checkout, see fetch_mekanism_refs.sh) get their own row on top so style drift is obvious. With
--tile, each texture is also shown as a 3x3 tiling at 4x scale, which is how a wall of the block
reads in game and exposes seams. Animated strips show their first frame.
"""

import argparse
from pathlib import Path

from PIL import Image, ImageDraw

BG = (40, 40, 48, 255)
LABEL = (220, 220, 220, 255)
SCALE = 16
COLS = 6


def first_frame(path: Path) -> Image.Image:
    im = Image.open(path).convert("RGBA")
    return im.crop((0, 0, im.width, min(im.height, im.width)))


def cell(path: Path, tile: bool) -> Image.Image:
    im = first_frame(path)
    big = im.resize((16 * SCALE, 16 * SCALE), Image.NEAREST)
    if not tile:
        return big
    tiled = Image.new("RGBA", (im.width * 3, im.height * 3))
    for i in range(9):
        tiled.paste(im, ((i % 3) * im.width, (i // 3) * im.height))
    tiled = tiled.resize((16 * SCALE, 16 * SCALE), Image.NEAREST)
    both = Image.new("RGBA", (big.width * 2 + 8, big.height), BG)
    both.paste(big, (0, 0))
    both.paste(tiled, (big.width + 8, 0))
    return both


def row(paths: list[Path], tile: bool) -> list[tuple[str, Image.Image]]:
    return [(p.name if p.parent.name in ("block", "item", "models") else f"{p.parent.name}/{p.name}", cell(p, tile)) for p in paths]


def main() -> None:
    ap = argparse.ArgumentParser()
    ap.add_argument("out")
    ap.add_argument("files", nargs="*", type=Path)
    ap.add_argument("--ref", nargs="*", type=Path, default=[])
    ap.add_argument("--tile", action="store_true")
    a = ap.parse_args()

    sections = [s for s in (row(a.ref, a.tile), row(a.files, a.tile)) if s]
    cw = max(im.width for s in sections for _, im in s) + 8
    ch = 16 * SCALE + 22
    cols = min(COLS if not a.tile else 3, max(len(s) for s in sections))
    rows = sum((len(s) + cols - 1) // cols for s in sections)
    sheet = Image.new("RGBA", (cols * cw, rows * ch + 8 * (len(sections) - 1)), BG)
    d = ImageDraw.Draw(sheet)
    y = 0
    for si, s in enumerate(sections):
        for i, (name, im) in enumerate(s):
            x = (i % cols) * cw + 4
            yy = y + (i // cols) * ch + 2
            sheet.paste(im, (x, yy), im)
            d.text((x, yy + 16 * SCALE + 3), ("REF " if si == 0 and a.ref else "") + name, fill=LABEL)
        y += ((len(s) + cols - 1) // cols) * ch + 8
    sheet.save(a.out)
    print(a.out)


if __name__ == "__main__":
    main()
