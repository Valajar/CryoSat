"""Prints a /tp command that stands the player at a position and aims the crosshair at a target point.

Usage: python3 aimat.py TX TY TZ [PX PY PZ]   (player defaults to 0.5 100 0.5; eye height 1.62)
`/tp ... facing` aims from the feet and the position gets centred, so compute yaw and pitch instead.
"""
import math
import sys

tx, ty, tz = map(float, sys.argv[1:4])
px, py, pz = map(float, sys.argv[4:7]) if len(sys.argv) >= 7 else (0.5, 100.0, 0.5)
dx, dy, dz = tx - px, ty - (py + 1.62), tz - pz
yaw = math.degrees(math.atan2(-dx, dz))
pitch = -math.degrees(math.atan2(dy, math.hypot(dx, dz)))
print(f"/tp @s {px} {py} {pz} {yaw:.1f} {pitch:.1f}")
