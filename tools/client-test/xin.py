"""Tiny X input driver: xin.py key <keysym> | type <text> | click <button> | chat <command>"""
import sys, time
from Xlib import X, XK, display
from Xlib.ext import xtest
d = display.Display(':99')
def key(sym, shift=False):
    code = d.keysym_to_keycode(XK.string_to_keysym(sym))
    if shift: xtest.fake_input(d, X.KeyPress, d.keysym_to_keycode(XK.XK_Shift_L))
    xtest.fake_input(d, X.KeyPress, code); d.sync(); time.sleep(0.03)
    xtest.fake_input(d, X.KeyRelease, code)
    if shift: xtest.fake_input(d, X.KeyRelease, d.keysym_to_keycode(XK.XK_Shift_L))
    d.sync(); time.sleep(0.03)
NAMES = {' ': 'space', '/': 'slash', '~': 'asciitilde', ':': 'colon', '_': 'underscore', '[': 'bracketleft', ']': 'bracketright',
         '=': 'equal', '{': 'braceleft', '}': 'braceright', '.': 'period', ',': 'comma', '-': 'minus', '@': 'at'}
SHIFTED = set('~:_{}@') | set('ABCDEFGHIJKLMNOPQRSTUVWXYZ')
def type_text(s):
    for ch in s:
        key(NAMES.get(ch, ch.lower() if ch.isalpha() else ch), shift=ch in SHIFTED)
cmd = sys.argv[1]
if cmd == 'key': key(sys.argv[2])
elif cmd == 'type': type_text(sys.argv[2])
elif cmd == 'click':
    b = int(sys.argv[2]); xtest.fake_input(d, X.ButtonPress, b); d.sync(); time.sleep(0.05); xtest.fake_input(d, X.ButtonRelease, b); d.sync()
elif cmd == 'chat':
    key('t'); time.sleep(0.5); type_text(sys.argv[2]); time.sleep(0.2); key('Return'); time.sleep(0.5)
elif cmd == 'move':
    xtest.fake_input(d, X.MotionNotify, x=int(sys.argv[2]), y=int(sys.argv[3])); d.sync()
elif cmd == 'clickat':
    xtest.fake_input(d, X.MotionNotify, x=int(sys.argv[2]), y=int(sys.argv[3])); d.sync(); time.sleep(0.15)
    b = int(sys.argv[4]) if len(sys.argv) > 4 else 1
    xtest.fake_input(d, X.ButtonPress, b); d.sync(); time.sleep(0.05); xtest.fake_input(d, X.ButtonRelease, b); d.sync()
elif cmd == 'shiftclickat':
    xtest.fake_input(d, X.MotionNotify, x=int(sys.argv[2]), y=int(sys.argv[3])); d.sync(); time.sleep(0.15)
    sh = d.keysym_to_keycode(XK.XK_Shift_L)
    xtest.fake_input(d, X.KeyPress, sh); d.sync(); time.sleep(0.05)
    xtest.fake_input(d, X.ButtonPress, 1); d.sync(); time.sleep(0.05); xtest.fake_input(d, X.ButtonRelease, 1); d.sync(); time.sleep(0.05)
    xtest.fake_input(d, X.KeyRelease, sh); d.sync()
