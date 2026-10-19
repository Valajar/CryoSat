"""Imaging Terminal: 1x1 steel machine that requests imaging passes from Survey Satellites and logs what they find.

Faces:
    front         Mekanism machine front: raised plate with a dark screen showing a faint survey grid and a satellite
                  silhouette (screen off), a card reader slot with a cobalt LED under it and the checkered strip
    front_active  8 frame strip: the grid lights up in cobalt, a scan line sweeps left to right under the satellite and a
                  target blip flares where it passes, then fades
    left, right   upper plate over a darker lower section with a vent
    top           plate with a recessed imaging lens (dark glass in a cobalt ring)
    back          energy socket on a bolted plate
    bottom        plain inset square

Run from the repo root:  python3 tools/textures/gen/imaging_terminal.py
"""
import sys
import zlib
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from texlib import *  # noqa: E402,F403

NAME = "imaging_terminal"

# Screen interior (inclusive box)
SCREEN = (3, 3, 12, 9)
FRAMES = 8
# Satellite on the screen: solar panel, body, solar panel
SATELLITE = [(4, 4), (5, 4), (6, 4)]
# Target blip the scan finds
BLIP = (10, 7)


def hrand(x: int, y: int, salt: str) -> int:
    """Stable per pixel random number, so animated pixels do not disturb the shared rng and static steel stays
    identical across frames."""
    return zlib.crc32(f"{NAME}:{salt}:{x}:{y}".encode())


def hpick(ramp, lo: int, hi: int, x: int, y: int, salt: str, jitter: int = 1):
    h = hrand(x, y, salt)
    return shade(ramp[lo + h % (hi - lo + 1)], (h >> 8) % (2 * jitter + 1) - jitter)


def on_grid(x: int, y: int) -> bool:
    x0, y0, _, _ = SCREEN
    return (x - x0) % 3 == 2 or (y - y0) % 3 == 2


def front(frame: int | None = None) -> Tex:
    """frame None is the idle face with the screen off."""
    t = Tex(seed=f"{NAME}_front")
    t.machine_shell()
    t.plate((1, 1, 14, 11))
    t.window(SCREEN)
    x0, y0, x1, y1 = SCREEN

    if frame is None:
        # Screen off: the grid and the satellite barely show through the dark glass
        for y in range(y0, y1 + 1):
            for x in range(x0, x1 + 1):
                if on_grid(x, y) and t.get(x, y) != PLATE[2]:
                    t.px(x, y, hpick(VOID, 3, 3, x, y, "grid"))
        for x, y in SATELLITE:
            t.px(x, y, hpick(FRAME, 1, 2, x, y, "sat"))
    else:
        # Screen on: deep cobalt glass, lit grid, a scan line sweeping across with a short trail behind it
        scan = x0 + frame * (x1 - x0 + 1) // FRAMES
        for y in range(y0, y1 + 1):
            for x in range(x0, x1 + 1):
                c = hpick(COBALT, 1, 1, x, y, "grid", 2) if on_grid(x, y) else hpick(COBALT, 0, 0, x, y, "glass", 2)
                behind = scan - x
                if behind == 0:
                    c = CRYO_GLOW[1]
                elif behind == 1:
                    c = COBALT[3]
                t.px(x, y, c)
        # Satellite: solar panels either side of a gold body, steady
        t.px(*SATELLITE[0], SOLAR[4])
        t.px(*SATELLITE[1], GOLD[4])
        t.px(*SATELLITE[2], SOLAR[4])
        # The blip flares as the scan passes it, then fades over the following frames
        flare = min(f for f in range(FRAMES) if x0 + f * (x1 - x0 + 1) // FRAMES >= BLIP[0])
        since = (frame - flare) % FRAMES
        blip = [CRYO_GLOW[3], CRYO_GLOW[2], CRYO_GLOW[1], CRYO_GLOW[1], COBALT[4], COBALT[3], COBALT[3], COBALT[2]][since]
        t.px(*BLIP, blip)

    # Card reader under the screen, with a cobalt status LED to its right (the CryoSat accent)
    t.line(4, 11, 9, 11, VOID[1])
    t.px(12, 11, COBALT[3])
    t.px(11, 11, FRAME[3])
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
    # Imaging lens: a recessed socket holding dark glass in a cobalt ring, glare at the top left
    t.inset((4, 4, 11, 11), light=PLATE[4], dark=FRAME[0])
    t.fill((5, 5, 10, 10), FRAME, 0, 2)
    for x in range(5, 11):
        for y in range(5, 11):
            d = ((x - 7.5) ** 2 + (y - 7.5) ** 2) ** 0.5
            if d <= 1.9:
                t.px(x, y, hpick(VOID, 0, 2, x, y, "lens"))
            elif d <= 3.1:
                t.px(x, y, COBALT[3] if x + y < 15 else COBALT[1])
    t.px(7, 6, SOLAR[4])
    return t


def back() -> Tex:
    t = Tex(seed=f"{NAME}_back")
    t.machine_shell()
    t.plate((1, 1, 14, 9))
    t.bolts(2)
    # Energy socket: a square port with two contacts
    t.inset((5, 3, 10, 8), light=PLATE[4], dark=FRAME[0])
    t.fill((6, 4, 9, 7), VOID, 1, 3, jitter=1)
    t.px(6, 5, GLINT[0])
    t.px(9, 6, GLINT[0])
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
