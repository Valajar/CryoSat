"""Reference implementation: a full face set for a 1x1 cryogenic machine plus a space hardware panel.

Generator scripts for real blocks should follow this structure. Output goes to build/texture-examples
so the examples never end up in the mod jar.
"""
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from texlib import *  # noqa: E402,F403

OUT = REPO / "build" / "texture-examples"


def front(active: bool) -> Tex:
    t = Tex(seed="example_front")
    t.machine_shell()
    t.plate((2, 2, 13, 10))
    if active:
        t.coolant_window((4, 4, 11, 8))
    else:
        t.window((4, 4, 11, 8))
    t.cobalt_trim((1, 11, 14, 12))
    t.checker_strip(13)
    t.frost(depth=2)
    return t


def side() -> Tex:
    t = Tex(seed="example_side")
    t.machine_shell()
    t.plate((2, 2, 13, 7))
    t.vent((4, 9, 11, 12))
    t.cobalt_trim((1, 13, 14, 14))
    t.frost(depth=3)
    return t


def top() -> Tex:
    t = Tex(seed="example_top")
    t.machine_shell()
    t.inset((3, 3, 12, 12))
    t.fill((4, 4, 11, 11), FRAME, 1, 3)
    t.vent((5, 5, 10, 10), horizontal=False)
    t.bolts()
    t.frost_corner(5, "tl")
    t.frost_corner(3, "br")
    return t


def foil_panel() -> Tex:
    t = Tex(seed="example_foil")
    t.machine_shell()
    t.gold_foil((2, 2, 13, 13))
    t.outline((1, 1, 14, 14), FRAME[0])
    t.bolts(1)
    return t


def solar_panel() -> Tex:
    t = Tex(seed="example_solar")
    t.fill((0, 0, 15, 15), SOLAR, 1, 3, jitter=1)
    for i in range(0, 16, 4):
        t.line(i, 0, i, 15, SOLAR[0])
        t.line(0, i, 15, i, SOLAR[0])
    for i in range(1, 16, 4):
        t.px(i, i, SOLAR[4])
    return t


if __name__ == "__main__":
    for name, tex in [("front", front(False)), ("front_active", front(True)), ("side", side()), ("top", top()),
                      ("foil_panel", foil_panel()), ("solar_panel", solar_panel())]:
        print(tex.save(f"{name}.png", root=OUT))
    for tier, r in TIER.items():
        t = Tex(seed="example_valve_" + tier)
        t.machine_shell()
        t.inset((4, 4, 11, 11))
        t.tier_pip((5, 5, 10, 10), r)
        t.px(7, 7, FRAME[0]); t.px(8, 8, FRAME[0])
        print(t.save(f"valve_{tier}.png", root=OUT))
