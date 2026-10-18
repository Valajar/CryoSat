"""Mission Control: 1x1 steel console that tracks the player's missions in orbit.

Faces:
    front         Mekanism machine front: raised plate with a dark screen showing a faint orbit trace (screen off),
                  a row of console keys under it and the checkered strip
    front_active  8 frame strip: the screen lights up in coolant blue, a planet in the middle and a satellite pip
                  running along the elliptical orbit, with a radar sweep trailing behind it
    left, right   upper plate with a diagonal cut over a darker lower section with a vent
    top           plate with a small recessed antenna mount
    back          data port on a bolted plate
    bottom        plain inset square

Run from the repo root:  python3 tools/textures/gen/mission_control.py
"""
import math
import sys
import zlib
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from texlib import *  # noqa: E402,F403

NAME = "mission_control"

# Screen interior (inclusive box) and the orbit drawn on it, in pixel centre coordinates.
SCREEN = (3, 3, 12, 9)
ORBIT_C = (7.5, 6.0)
ORBIT_A = 4.4   # horizontal semi axis
ORBIT_B = 2.6   # vertical semi axis
FRAMES = 8


def hrand(x: int, y: int, salt: str) -> int:
    """Stable per pixel random number, so animated pixels do not disturb the shared rng and static steel stays
    identical across frames."""
    return zlib.crc32(f"{NAME}:{salt}:{x}:{y}".encode())


def hpick(ramp, lo: int, hi: int, x: int, y: int, salt: str, jitter: int = 1):
    h = hrand(x, y, salt)
    return shade(ramp[lo + h % (hi - lo + 1)], (h >> 8) % (2 * jitter + 1) - jitter)


def orbit_pixels():
    """Pixels on the ellipse, with the angle of each, ordered around the orbit."""
    seen = {}
    for i in range(720):
        a = 2 * math.pi * i / 720
        x = int(math.floor(ORBIT_C[0] + ORBIT_A * math.cos(a) + 0.5))
        y = int(math.floor(ORBIT_C[1] + ORBIT_B * math.sin(a) + 0.5))
        seen.setdefault((x, y), a)
    x0, y0, x1, y1 = SCREEN
    return [(p, a) for p, a in sorted(seen.items(), key=lambda kv: kv[1]) if x0 <= p[0] <= x1 and y0 <= p[1] <= y1]


PLANET = [(7, 5), (8, 5), (7, 6), (8, 6)]


def angle_gap(a: float, b: float) -> float:
    """How far angle a trails behind angle b, in 0..2pi."""
    return (b - a) % (2 * math.pi)


def front(frame: int | None = None) -> Tex:
    """frame None is the idle face with the screen off."""
    t = Tex(seed=f"{NAME}_front")
    t.machine_shell()
    t.plate((1, 1, 14, 11))
    t.window(SCREEN)
    x0, y0, x1, y1 = SCREEN
    orbit = orbit_pixels()

    if frame is None:
        # Screen off: the orbit trace and planet barely show through the dark glass
        for (x, y), _ in orbit:
            if t.get(x, y) not in (PLATE[2],):
                t.px(x, y, hpick(FRAME, 0, 1, x, y, "trace"))
        for x, y in PLANET:
            t.px(x, y, hpick(FRAME, 1, 2, x, y, "planet"))
    else:
        # Screen on: deep cobalt glass, a lit orbit, the planet, and a satellite with a fading sweep behind it
        for y in range(y0, y1 + 1):
            for x in range(x0, x1 + 1):
                t.px(x, y, hpick(COBALT, 0, 0, x, y, "glass", 2))
        sat = 2 * math.pi * frame / FRAMES - math.pi / 2
        for (x, y), a in orbit:
            gap = angle_gap(a, sat)
            if gap < 0.6:
                c = CRYO_GLOW[2]
            elif gap < 1.5:
                c = CRYO_GLOW[1]
            elif gap < 2.6:
                c = COBALT[3]
            else:
                c = COBALT[1]
            t.px(x, y, c)
        planet = [CRYO_GLOW[1], CRYO_GLOW[0], CRYO_GLOW[0], COBALT[3]]
        for (x, y), c in zip(PLANET, planet):
            t.px(x, y, c)
        sx = int(math.floor(ORBIT_C[0] + ORBIT_A * math.cos(sat) + 0.5))
        sy = int(math.floor(ORBIT_C[1] + ORBIT_B * math.sin(sat) + 0.5))
        t.px(sx, sy, CRYO_GLOW[3])

    # Console keys under the screen: small raised steel caps, one cobalt key for the CryoSat accent
    for i, kx in enumerate(range(3, 13, 2)):
        cap = COBALT[3] if i == 2 else PLATE[4]
        t.px(kx, 11, cap)
        t.px(kx + 1, 11, FRAME[3])
    t.line(1, 12, 14, 12, FRAME[1])
    t.checker_strip(13)
    return t


def side(seed: str) -> Tex:
    t = Tex(seed=f"{NAME}_{seed}")
    t.machine_shell()
    t.plate((1, 1, 14, 8))
    t.line(1, 9, 14, 9, FRAME[1])
    t.inset((3, 10, 12, 12), light=PANEL[4], dark=FRAME[0])
    t.vent((4, 11, 11, 11))
    t.checker_strip(13)
    return t


def top() -> Tex:
    t = Tex(seed=f"{NAME}_top")
    t.machine_shell()
    t.plate((2, 2, 13, 13))
    for x, y in ((3, 3), (12, 3), (3, 12), (12, 12)):
        t.px(x, y, GLINT[0])
    # Antenna mount: recessed square socket with a cobalt ringed mast stub in the middle
    t.inset((5, 5, 10, 10), light=PLATE[4], dark=FRAME[0])
    t.fill((6, 6, 9, 9), FRAME, 1, 3)
    t.px(7, 7, COBALT[3]); t.px(8, 7, COBALT[2])
    t.px(7, 8, COBALT[2]); t.px(8, 8, COBALT[1])
    t.px(6, 6, PANEL[1])
    return t


def back() -> Tex:
    t = Tex(seed=f"{NAME}_back")
    t.machine_shell()
    t.plate((1, 1, 14, 9))
    t.bolts(2)
    # Data port: a wide socket with a row of pins
    t.inset((4, 4, 11, 7), light=PLATE[4], dark=FRAME[0])
    t.fill((5, 5, 10, 6), VOID, 1, 3, jitter=1)
    for x in range(5, 11, 2):
        t.px(x, 5, GLINT[0])
    t.line(1, 10, 14, 10, FRAME[0])
    t.vent((3, 11, 12, 12))
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


def main() -> None:
    out = [
        front().save(f"{NAME}/front.png"),
        save_animation([front(f) for f in range(FRAMES)], f"{NAME}/front_active.png", frametime=3),
        side("left").save(f"{NAME}/left.png"),
        side("right").save(f"{NAME}/right.png"),
        top().save(f"{NAME}/top.png"),
        back().save(f"{NAME}/back.png"),
        bottom().save(f"{NAME}/bottom.png"),
    ]
    for p in out:
        print(p.relative_to(REPO))


if __name__ == "__main__":
    main()
