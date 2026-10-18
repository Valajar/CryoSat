"""Connected textures, the "compact" method, shared by the generators and connected_preview.py.

A connected face is drawn as four 8x8 quarters. Each quarter picks one of five pieces from the two
edge neighbours next to it in the face's plane and the diagonal neighbour between them:

    piece        when                                       file suffix
    corner       neither edge neighbour connects            ""            (the isolated block)
    horizontal   only the left or right neighbour connects  "_horizontal" (border runs along top/bottom)
    vertical     only the up or down neighbour connects     "_vertical"   (border runs along left/right)
    inner        both connect, the diagonal does not        "_inner"      (just the inner corner notch)
    center       all three connect                          "_center"     (no border)

Each piece is a full 16x16 texture whose four quarters are all in that state, so any mix of quarters
from the five textures lines up. The Java side (io.github.valajar.cryosat.connected.ConnectedTextures)
uses exactly the same rule; keep `piece` and `FACES` in step with it.

A generator describes its block with a feature function: feature(piece, x, y) names what pixel (x, y)
shows when that pixel's quarter is in that piece state (for example "dark", "plate", "bracket"). Pixels
whose feature matches the isolated block reuse the isolated texture, so the corner piece is the
existing texture byte for byte; other pixels come from a synth(feature, x, y) colour function, which
must be deterministic per pixel.
"""

from __future__ import annotations

from PIL import Image

PIECES = ("corner", "horizontal", "vertical", "inner", "center")
SUFFIX = {"corner": "", "horizontal": "_horizontal", "vertical": "_vertical", "inner": "_inner", "center": "_center"}


def piece(horizontal: bool, vertical: bool, diagonal: bool) -> str:
    """Quarter choice from its neighbours. Mirrors ConnectedTextures.piece in Java."""
    if horizontal and vertical:
        return "center" if diagonal else "inner"
    if horizontal:
        return "horizontal"
    if vertical:
        return "vertical"
    return "corner"


def quarter(x: int, y: int) -> tuple[bool, bool]:
    """(right half, bottom half) of a 16x16 pixel."""
    return x >= 8, y >= 8


def build_pieces(iso: Image.Image, feature, synth) -> dict[str, Image.Image]:
    out = {}
    for p in PIECES:
        img = iso.copy()
        for y in range(16):
            for x in range(16):
                f = feature(p, x, y)
                if f != feature("corner", x, y):
                    img.putpixel((x, y), (*synth(f, x, y), 255))
        out[p] = img
    return out


# --- the in world rule -------------------------------------------------------------------------------
# For each face: the world direction the texture's u axis (right) and v axis (down) run along, from
# Minecraft's default face UVs (BlockElement.uvsByFace). Directions are (dx, dy, dz).
EAST, WEST, UP, DOWN, SOUTH, NORTH = (1, 0, 0), (-1, 0, 0), (0, 1, 0), (0, -1, 0), (0, 0, 1), (0, 0, -1)
FACES = {
    "down": (DOWN, EAST, NORTH),     # normal, right, down
    "up": (UP, EAST, SOUTH),
    "north": (NORTH, WEST, DOWN),
    "south": (SOUTH, EAST, DOWN),
    "west": (WEST, SOUTH, DOWN),
    "east": (EAST, NORTH, DOWN),
}


def add(*vs):
    return tuple(sum(c) for c in zip(*vs))


def neg(v):
    return tuple(-c for c in v)


def face_pieces(is_part, pos, face: str) -> list[str]:
    """Pieces of the four quarters (top left, top right, bottom left, bottom right) of one face of the
    block at pos. is_part(pos) says whether the block there is something this block connects to. A
    neighbour connects when it is a part and the block in front of it (along the face normal) is not."""
    normal, right, down = FACES[face]

    def connects(p):
        return is_part(p) and not is_part(add(p, normal))

    out = []
    for qy in (0, 1):
        for qx in (0, 1):
            h = right if qx else neg(right)
            v = down if qy else neg(down)
            out.append(piece(connects(add(pos, h)), connects(add(pos, v)), connects(add(pos, h, v))))
    return out


def compose(pieces: dict[str, Image.Image], quarters: list[str]) -> Image.Image:
    """The face texture the model draws for these four quarter pieces."""
    img = Image.new("RGBA", (16, 16))
    for i, p in enumerate(quarters):
        box = ((i % 2) * 8, (i // 2) * 8, (i % 2) * 8 + 8, (i // 2) * 8 + 8)
        img.paste(pieces[p].crop(box), box[:2])
    return img
