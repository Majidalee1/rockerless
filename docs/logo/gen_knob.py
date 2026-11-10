"""Rockerless logo generator.

The mark: a wavy "cookie" knob (the same shape as the app's floating bubble)
inside a 270-degree level ring. The knob's pointer sits exactly where the
ring's fill ends, so the knob and the level always agree.

Usage:
    python3 gen_knob.py <out_dir> [palette]

Writes into <out_dir>:
    rockerless-logo.svg      full-bleed square logo (README, socials)
    rockerless-icon.svg      same mark with launcher-safe padding (Play Store icon source)
    ic_launcher_foreground.xml, ic_launcher_monochrome.xml   Android vector drawables
    background_color.txt     the palette's background colour, for colors.xml
"""
import math
import os
import sys

PALETTES = {
    # bg, track, fill, knob
    "midnight": ("#1F2B4D", "#34436B", "#E9DDFF", "#AFC6FF"),  # the approved original
    "indigo":   ("#1A1B4B", "#33357A", "#FFD9F0", "#B7B9FF"),
    "pixel":    ("#0F2A2E", "#1F4A50", "#C6F0A8", "#9FE3E8"),
    "sunset":   ("#2B1631", "#4D2B54", "#FFD8B0", "#F3B2D4"),
    "ocean":    ("#0B2545", "#1B3F6B", "#8EE3EF", "#5FA8F5"),
    "forest":   ("#1B2A1F", "#2F4A35", "#E6F4C8", "#9ED9A8"),
    "graphite": ("#1C1C1F", "#38383D", "#FFFFFF", "#C9C9D1"),
    "ember":    ("#2A1410", "#4E2A22", "#FFC48A", "#FF8A6B"),
    "lilac":    ("#ECE6FF", "#D3C8F5", "#5B3FD6", "#9A86F0"),
    "daylight": ("#E3EEFF", "#C4D6F5", "#1F2B4D", "#6E93E6"),
    "mint":     ("#E4F7EC", "#C5E9D3", "#1E5A3D", "#5EC08E"),
    "rose":     ("#FFE6EC", "#F5C9D5", "#8A1F45", "#F07A9C"),
}

S, C = 1024, 512
# Ring geometry, SVG degrees (y down, clockwise). The gap is centred on the
# bottom. A smaller gap makes the mark closer to a full circle, so it balances
# top-to-bottom and the knob sits at (almost) the true centre.
GAP = float(os.environ.get("KNOB_GAP", "40"))
SWEEP = 360 - GAP
START = 90 + GAP / 2
RR, SW = 300, 60                      # ring radius and stroke width
R, A, N = 194, 8, 10                  # knob base radius, lobe depth, lobe count
# Symmetry: one lobe points straight up (270 deg), so with an even lobe count the
# knob mirrors cleanly left/right and top/bottom. The level is chosen so the
# fill ends exactly on a lobe, which puts the pointer on a lobe's centre line.
LOBE_STEP = 360 / N
# STYLE picks where the level ends. "level" = 63% (pointer one lobe right of top),
# "half" = 50% (pointer straight up), "full" = whole ring one tone, pointer up.
STYLE = os.environ.get("KNOB_STYLE", "full")
END = 270 + (LOBE_STEP if STYLE == "level" else 0)
LEVEL = (END - START) / SWEEP         # 0.633
DOT_R, DOT_IN = 30, 66                # pointer radius, distance in from knob edge
ICON_SCALE = 0.84                     # keeps the ring inside Android's 66dp safe zone
MONO_SCALE = 0.66                     # themed icons: ~46dp glyph, in line with Google's own
# Centre the mark's bounding box on the canvas. The ring is open at the bottom,
# so its box sits higher than its centre; shift down by exactly that amount.
_top = RR + SW / 2
_bottom = RR * math.sin(math.radians(START + SWEEP - 360)) + SW / 2
NUDGE = (_top - _bottom) / 2


def f(v):
    return f"{v:.1f}".rstrip("0").rstrip(".")


def pt(r, deg):
    a = math.radians(deg)
    return C + r * math.cos(a), C + r * math.sin(a)


def arc(r, a0, a1):
    x0, y0 = pt(r, a0)
    x1, y1 = pt(r, a1)
    large = 1 if (a1 - a0) > 180 else 0
    return f"M{f(x0)},{f(y0)} A{r},{r} 0 {large} 1 {f(x1)},{f(y1)}"


TRACK_D = arc(RR, START, START + SWEEP)
FILL_D = TRACK_D if STYLE == "full" else arc(RR, START, END)


def knob_d():
    """Knob outline plus the pointer as a hole (even-odd), so the pointer is a
    true cut-out: it works in themed (monochrome) icons too."""
    pts = []
    for i in range(360):
        t = 2 * math.pi * i / 360
        r = R + A * math.cos(N * (t - math.radians(270)))
        pts.append((C + r * math.cos(t), C + r * math.sin(t)))
    outline = "M" + " L".join(f"{f(x)},{f(y)}" for x, y in pts) + " Z"
    dx, dy = pt(R - DOT_IN, END)
    hole = (f"M{f(dx - DOT_R)},{f(dy)} a{DOT_R},{DOT_R} 0 1 0 {DOT_R * 2},0 "
            f"a{DOT_R},{DOT_R} 0 1 0 {-DOT_R * 2},0 Z")
    return outline + " " + hole


KNOB_D = knob_d()


def svg(palette, scale, size=S):
    bg, track, fill, knob = palette
    return f'''<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {S} {S}" width="{size}" height="{size}">
  <title>Rockerless</title>
  <rect width="{S}" height="{S}" fill="{bg}"/>
  <g transform="translate({C} {f(C + NUDGE * scale)}) scale({scale}) translate({-C} {-C})">
    <path d="{TRACK_D}" fill="none" stroke="{track}" stroke-width="{SW}" stroke-linecap="round"/>
    <path d="{FILL_D}" fill="none" stroke="{fill}" stroke-width="{SW}" stroke-linecap="round"/>
    <path d="{KNOB_D}" fill="{knob}" fill-rule="evenodd"/>
  </g>
</svg>
'''


def vector(palette, mono=False):
    _, track, fill, knob = palette
    scale = MONO_SCALE if mono else ICON_SCALE
    if mono:  # themed icons: Android tints by alpha only
        track, fill, knob = "#FFFFFFFF", "#FFFFFFFF", "#FFFFFFFF"
    track_alpha = ' android:strokeAlpha="0.35"' if mono else ""
    return f'''<?xml version="1.0" encoding="utf-8"?>
<!-- Generated by docs/logo/gen_knob.py. Edit the script, not this file. -->
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="{S}"
    android:viewportHeight="{S}">
    <group
        android:pivotX="{C}"
        android:pivotY="{C}"
        android:scaleX="{scale}"
        android:scaleY="{scale}"
        android:translateY="{f(NUDGE * scale)}">
        <path
            android:pathData="{TRACK_D}"
            android:strokeColor="{track}"{track_alpha}
            android:strokeWidth="{SW}"
            android:strokeLineCap="round" />
        <path
            android:pathData="{FILL_D}"
            android:strokeColor="{fill}"
            android:strokeWidth="{SW}"
            android:strokeLineCap="round" />
        <path
            android:pathData="{KNOB_D}"
            android:fillColor="{knob}"
            android:fillType="evenOdd" />
    </group>
</vector>
'''


def main():
    out = sys.argv[1]
    name = sys.argv[2] if len(sys.argv) > 2 else "midnight"
    p = PALETTES[name]
    os.makedirs(out, exist_ok=True)
    files = {
        "rockerless-logo.svg": svg(p, 1.0),
        "rockerless-icon.svg": svg(p, ICON_SCALE),
        "ic_launcher_foreground.xml": vector(p),
        "ic_launcher_monochrome.xml": vector(p, mono=True),
        "background_color.txt": p[0] + "\n",
    }
    for fn, body in files.items():
        with open(os.path.join(out, fn), "w") as fh:
            fh.write(body)


if __name__ == "__main__":
    main()
