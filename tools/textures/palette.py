"""CryoSat texture palette.

Steel values were measured from Mekanism 10.7 machine textures (enrichment chamber, casings,
factory, chemical tank valves). Accent ramps are CryoSat's own and were picked to stay clear of
Mekanism's tier hues (basic mint, advanced red, elite teal, ultimate violet).

Every ramp is ordered darkest to lightest.
"""


def hex_rgb(h: str) -> tuple[int, int, int]:
    h = h.lstrip("#")
    return int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16)


def ramp(*hexes: str) -> list[tuple[int, int, int]]:
    return [hex_rgb(h) for h in hexes]


# --- Mekanism steel (measured) ---------------------------------------------------------------
# Window/void blacks behind glass and inside vents.
VOID = ramp("0d0d0d", "111111", "151515", "181818")
# Outer frame and recessed seams.
FRAME = ramp("242424", "282828", "2c2c2c", "303030", "353535")
# Main panel body. Most of a machine face lives here.
PANEL = ramp("404040", "474747", "4c4c4c", "515151", "565656")
# Raised plates and bevel highlights.
PLATE = ramp("5d5d5d", "636363", "686868", "6d6d6d", "737373")
# Top edge glints and bolt heads. Use sparingly, 1 to 4 pixels per face.
GLINT = ramp("7f7f7f", "8a8a8a", "9d9b99")
# Light "casing" steel used by multiblock casings (boiler, chemical tank body).
CASING = ramp("5b5b5b", "676767", "727272", "807c7c", "868282", "9d9b99")

# Mekanism factory hazard stripe (alternate with FRAME[0]).
HAZARD = ramp("caa84e")

# --- Mekanism tier accents (measured from chemical tank valves) -------------------------------
TIER = {
    "basic": ramp("266343", "238051", "219358", "1ca760"),
    "advanced": ramp("662525", "882b28", "902c28", "9f2f2b"),
    "elite": ramp("2d4859", "357785", "308995", "3d98a2"),
    "ultimate": ramp("3d2748", "603366", "69366f", "733b7b"),
    "creative": ramp("111111", "181818", "1a1a1a", "1d1d1d"),
}
# Bright tier text colors (BaseTier), for LEDs and glowing tier pips only.
TIER_GLOW = {
    "basic": hex_rgb("5fffb8"),
    "advanced": hex_rgb("ff806a"),
    "elite": hex_rgb("4bf8ff"),
    "ultimate": hex_rgb("f787ff"),
    "creative": hex_rgb("585858"),
}

# --- CryoSat accents ------------------------------------------------------------------------
# Cryo Cobalt: the cryogenics signature. Hue about 228 degrees, so it reads as deep blue next to
# elite teal (about 187) and ultimate violet (about 290). Keep it saturated; washed out cobalt
# drifts toward elite.
COBALT = ramp("16204f", "22337a", "2f48a8", "4466d4", "7391ee")
# Frost rime: crusted ice on cold surfaces. Only ever on top of steel, never as a base color.
FROST = ramp("aebdd4", "c8d4e6", "e2eaf5", "f4f8fc")
# Coolant glow for windows and gauges showing cryogenic liquid.
CRYO_GLOW = ramp("3a5ad0", "6d8cff", "a9bcff", "dde5ff")

# MLI Gold: crinkled multilayer insulation foil, the space hardware signature. Amber leaning so
# it does not merge with the yellow hazard stripe.
GOLD = ramp("4a3410", "7a5518", "a87822", "d2a03a", "f0cd6e", "fff0b8")
# Solar cell blue black for satellite panels.
SOLAR = ramp("0c1020", "141b33", "1e2a4d", "2d3f70", "5a6fa8")
# Warning red for weapon hardware (WarSats). Darker than advanced tier red on purpose.
ORDNANCE = ramp("3a0e0e", "5c1414", "7d1b1b")

ALL = {
    "VOID": VOID, "FRAME": FRAME, "PANEL": PANEL, "PLATE": PLATE, "GLINT": GLINT, "CASING": CASING,
    "HAZARD": HAZARD, "COBALT": COBALT, "FROST": FROST, "CRYO_GLOW": CRYO_GLOW, "GOLD": GOLD,
    "SOLAR": SOLAR, "ORDNANCE": ORDNANCE,
}
