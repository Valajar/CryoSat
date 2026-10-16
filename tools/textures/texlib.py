"""Small pixel art toolkit for CryoSat textures.

Every generator script builds textures out of these primitives so the whole mod shares one look.
Coordinates are inclusive pixel boxes: (x0, y0, x1, y1). All randomness is seeded, so rerunning a
script produces identical PNGs.

Typical use:
    from texlib import *
    t = Tex(seed="cryo_condenser_front")
    t.machine_shell()
    t.window((3, 3, 12, 8))
    t.save("cryo_condenser/front.png")
"""

from __future__ import annotations

import json
import random
import zlib
from pathlib import Path

from PIL import Image, ImageDraw

from palette import *  # noqa: F401,F403  (re-exported for generator scripts)
from palette import COBALT, CRYO_GLOW, FRAME, FROST, GLINT, GOLD, HAZARD, PANEL, PLATE, VOID

RGB = tuple[int, int, int]
REPO = Path(__file__).resolve().parents[2]
ASSETS = REPO / "src" / "main" / "resources" / "assets" / "cryosat" / "textures"


def _clamp(v: int) -> int:
    return max(0, min(255, v))


def shade(c: RGB, d: int) -> RGB:
    """Shift all channels of a color by d (positive = lighter)."""
    return _clamp(c[0] + d), _clamp(c[1] + d), _clamp(c[2] + d)


class Tex:
    def __init__(self, w: int = 16, h: int = 16, seed: str = "cryosat", transparent: bool = False):
        self.w, self.h = w, h
        self.img = Image.new("RGBA", (w, h), (0, 0, 0, 0) if transparent else (*PANEL[2], 255))
        self.rng = random.Random(zlib.crc32(seed.encode()))

    # --- pixels ---------------------------------------------------------------------------------
    def px(self, x: int, y: int, c: RGB, a: int = 255) -> None:
        if 0 <= x < self.w and 0 <= y < self.h:
            self.img.putpixel((x, y), (*c, a))

    def get(self, x: int, y: int) -> RGB:
        return self.img.getpixel((x, y))[:3]

    def pick(self, ramp: list[RGB], lo: int = 0, hi: int | None = None) -> RGB:
        hi = len(ramp) - 1 if hi is None else hi
        return ramp[self.rng.randint(lo, hi)]

    def fill(self, box, ramp: list[RGB], lo: int = 0, hi: int | None = None, jitter: int = 2) -> None:
        """Fill a box with a noisy mix of ramp[lo..hi]. Mekanism steel is never a flat color."""
        x0, y0, x1, y1 = box
        for y in range(y0, y1 + 1):
            for x in range(x0, x1 + 1):
                self.px(x, y, shade(self.pick(ramp, lo, hi), self.rng.randint(-jitter, jitter)))

    def line(self, x0: int, y0: int, x1: int, y1: int, c: RGB) -> None:
        ImageDraw.Draw(self.img).line((x0, y0, x1, y1), fill=(*c, 255))

    def outline(self, box, c: RGB) -> None:
        x0, y0, x1, y1 = box
        ImageDraw.Draw(self.img).rectangle((x0, y0, x1, y1), outline=(*c, 255))

    # --- Mekanism building blocks ---------------------------------------------------------------
    def bevel(self, box, light: RGB = PLATE[3], dark: RGB = FRAME[1]) -> None:
        """Raised edge: light on top and left, dark on bottom and right (light comes from top left)."""
        x0, y0, x1, y1 = box
        self.line(x0, y0, x1, y0, light)
        self.line(x0, y0, x0, y1, light)
        self.line(x0, y1, x1, y1, dark)
        self.line(x1, y0, x1, y1, dark)

    def inset(self, box, light: RGB = PLATE[2], dark: RGB = FRAME[0]) -> None:
        """Recessed edge: the bevel inverted."""
        self.bevel(box, light=dark, dark=light)

    def machine_shell(self, border: int = 1) -> None:
        """Standard machine face: noisy panel body inside a dark frame with a slight bevel."""
        self.fill((0, 0, self.w - 1, self.h - 1), PANEL, 1, 4)
        for i in range(border):
            self.outline((i, i, self.w - 1 - i, self.h - 1 - i), FRAME[1 + i % 2])
        b = border
        self.bevel((b, b, self.w - 1 - b, self.h - 1 - b), light=PANEL[3], dark=FRAME[4])

    def plate(self, box) -> None:
        """Raised lighter plate, like the top half of Mekanism machine sides."""
        self.fill(box, PLATE, 0, 4)
        self.bevel(box, light=PLATE[4], dark=PANEL[0])

    def bolt(self, x: int, y: int) -> None:
        self.px(x, y, GLINT[1])
        self.px(x + 1, y + 1, FRAME[0]) if x + 1 < self.w and y + 1 < self.h else None

    def bolts(self, inset: int = 2) -> None:
        for x, y in ((inset, inset), (self.w - 1 - inset, inset), (inset, self.h - 1 - inset), (self.w - 1 - inset, self.h - 1 - inset)):
            self.px(x, y, GLINT[0])

    def window(self, box, glare: bool = True) -> None:
        """Dark machine window with a framed rim and diagonal glare specks."""
        x0, y0, x1, y1 = box
        self.outline((x0 - 1, y0 - 1, x1 + 1, y1 + 1), FRAME[0])
        self.fill(box, VOID, 0, 3, jitter=1)
        if glare:
            for i in range(0, min(x1 - x0, y1 - y0) + 1, 3):
                self.px(x0 + 1 + i, y0 + 1 + (i % 2), PLATE[2])

    def vent(self, box, horizontal: bool = True) -> None:
        """Slotted grille: alternating void and frame rows (or columns)."""
        x0, y0, x1, y1 = box
        for y in range(y0, y1 + 1):
            for x in range(x0, x1 + 1):
                on = (y - y0) % 2 == 0 if horizontal else (x - x0) % 2 == 0
                self.px(x, y, VOID[2] if on else FRAME[3])

    def checker_strip(self, y: int, a: RGB = FRAME[0], b: RGB = PLATE[0]) -> None:
        """The checkered strip along the bottom of Mekanism machine fronts."""
        for x in range(1, self.w - 1):
            self.px(x, y, a if x % 2 == 0 else b)
            self.px(x, y + 1, b if x % 2 == 0 else a)

    def hazard(self, y: int, h: int = 2) -> None:
        """Yellow and black hazard band, as on factories and chemical tanks."""
        for dy in range(h):
            for x in range(self.w):
                self.px(x, y + dy, HAZARD[0] if (x + dy) % 4 < 2 else FRAME[0])

    def tier_pip(self, box, tier_ramp: list[RGB]) -> None:
        """Small tier colored inlay. Keep it subtle: a few pixels, not a whole face."""
        self.fill(box, tier_ramp, 1, len(tier_ramp) - 1, jitter=1)
        x0, y0, x1, y1 = box
        self.line(x0, y1, x1, y1, tier_ramp[0])

    # --- CryoSat signatures ---------------------------------------------------------------------
    def cobalt_trim(self, box) -> None:
        """Cryo Cobalt accent band, the signature of cryogenic machinery. Use at least 2 pixels tall."""
        x0, y0, x1, y1 = box
        self.fill(box, COBALT, 2, 3, jitter=1)
        if y1 > y0:
            self.line(x0, y0, x1, y0, COBALT[4])
            self.line(x0, y1, x1, y1, COBALT[1])

    def coolant_window(self, box) -> None:
        """Window showing cryogenic liquid: glowing blue fill with a lighter meniscus line."""
        x0, y0, x1, y1 = box
        self.outline((x0 - 1, y0 - 1, x1 + 1, y1 + 1), FRAME[0])
        self.fill(box, CRYO_GLOW, 0, 1, jitter=3)
        self.line(x0, y0, x1, y0, CRYO_GLOW[3])
        for x in range(x0, x1 + 1, 3):
            self.px(x, y0 + 1, CRYO_GLOW[2])

    def frost(self, depth: int = 3, from_top: bool = True, coverage: float = 0.6) -> None:
        """Rime crust along an edge with short drips hanging off it.

        The edge row is mostly covered; each drip only continues where the pixel above it is frosted,
        so the crust stays connected instead of reading as random snow.
        """
        cols = [x for x in range(1, self.w - 1) if self.rng.random() < coverage]
        for x in cols:
            length = 1 + (self.rng.random() < 0.5) + (self.rng.random() < 0.25) * (depth - 2)
            for r in range(min(length, depth)):
                y = r if from_top else self.h - 1 - r
                self.px(x, y, FROST[3 - min(r, 2)] if self.rng.random() < 0.7 else FROST[2 - min(r, 2)])

    def frost_corner(self, size: int = 4, corner: str = "tl") -> None:
        """Triangular frost patch growing out of one corner."""
        for y in range(size):
            for x in range(size - y):
                if self.rng.random() < 0.85 - 0.15 * (x + y):
                    xx = x if "l" in corner else self.w - 1 - x
                    yy = y if "t" in corner else self.h - 1 - y
                    self.px(xx, yy, FROST[3 - min(2, (x + y) // 2)])

    def gold_foil(self, box, cell: int = 3) -> None:
        """Crinkled multilayer insulation: small flat facets with dark creases between them.

        Worley style: each pixel takes the tone of its nearest facet seed, and pixels almost equally
        close to two seeds become creases.
        """
        x0, y0, x1, y1 = box
        seeds = []
        for gy in range(y0, y1 + 1, cell):
            for gx in range(x0, x1 + 1, cell):
                seeds.append((gx + self.rng.uniform(0, cell), gy + self.rng.uniform(0, cell), self.pick(GOLD, 2, 4)))
        for y in range(y0, y1 + 1):
            for x in range(x0, x1 + 1):
                d = sorted(((sx - x) ** 2 + (sy - y) ** 2, c) for sx, sy, c in seeds)
                (d1, c1), (d2, _) = d[0], d[1]
                if d2 ** 0.5 - d1 ** 0.5 < 0.45:
                    self.px(x, y, GOLD[1] if self.rng.random() < 0.6 else GOLD[2])
                else:
                    self.px(x, y, c1)
        for _ in range(max(1, (x1 - x0) // 4)):
            self.px(self.rng.randint(x0, x1), self.rng.randint(y0, y1), GOLD[5])

    # --- output ---------------------------------------------------------------------------------
    def save(self, rel_path: str, root: Path = ASSETS / "block") -> Path:
        out = root / rel_path
        out.parent.mkdir(parents=True, exist_ok=True)
        self.img.save(out)
        return out


def save_animation(frames: list[Tex], rel_path: str, frametime: int = 2, interpolate: bool = False,
                   root: Path = ASSETS / "block") -> Path:
    """Stack frames vertically and write the matching .mcmeta, the format Mekanism uses for screens."""
    w, h = frames[0].w, frames[0].h
    strip = Image.new("RGBA", (w, h * len(frames)))
    for i, f in enumerate(frames):
        strip.paste(f.img, (0, i * h))
    out = root / rel_path
    out.parent.mkdir(parents=True, exist_ok=True)
    strip.save(out)
    meta = {"animation": {"frametime": frametime}}
    if interpolate:
        meta["animation"]["interpolate"] = True
    out.with_name(out.name + ".mcmeta").write_text(json.dumps(meta, indent=2) + "\n")
    return out
