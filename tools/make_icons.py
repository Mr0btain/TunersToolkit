#!/usr/bin/env python3
"""
Draws the app icon (a tuning curve over a map's cells, in the UI kit's "bench" palette) for every platform, and the
tt_ line icons that aren't from an icon set, from one set of shapes. Needs Pillow. Run from the repository root:

    python3 tools/make_icons.py

It writes:
- androidApp/src/main/res: the adaptive icon's layers as vectors, and PNGs for launchers without adaptive icons
- composeApp/icons: icon.png, icon.ico and icon.icns for the desktop installers
- composeApp/src/commonMain/composeResources/drawable/tt_app_icon.png: the window icon and the app's own logo
- composeApp/src/wasmJsMain/resources: favicon.ico, favicon.png and apple-touch-icon.png
- tt_settings.png and tt_gallery.png in the drawable folder
"""
import math
import os

from PIL import Image, ImageDraw

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))

# The "bench" palette from Tm-UI-Kit's TmPalettes
TOP = (0x25, 0x2B, 0x33)
BOTTOM = (0x14, 0x17, 0x1B)
CELL_LOW = (0x25, 0x2B, 0x33)
ACCENT = (0x7C, 0xC4, 0xFF)
INK = (0x14, 0x17, 0x1B)
CURVE = (0xFF, 0xFF, 0xFF)

# Everything is placed on Android's 108 unit adaptive icon canvas; the middle 72 are what's seen
CELLS = 4
GRID_FROM, GRID_TO, GAP, CELL_RADIUS = 31.0, 77.0, 2.0, 2.2
CURVE_WIDTH, UNDER_WIDTH, DOT, DOT_RING = 4.6, 8.6, 4.4, 2.2


def mix(a, b, t):
    return tuple(round(a[i] + (b[i] - a[i]) * t) for i in range(3))


def cells():
    """(x, y, size, color) for each cell, low at the bottom left and high at the top right, like a fuel map"""
    size = (GRID_TO - GRID_FROM - GAP * (CELLS - 1)) / CELLS
    out = []
    for row in range(CELLS):
        for col in range(CELLS):
            level = (col + (CELLS - 1 - row)) / (2 * (CELLS - 1))
            x = GRID_FROM + col * (size + GAP)
            y = GRID_FROM + row * (size + GAP)
            out.append((x, y, size, mix(CELL_LOW, ACCENT, 0.18 + 0.72 * level)))
    return out


def curve_points(steps=28):
    """A rising curve across the cells, flat at first and steep near the end, ending in a dot"""
    x0, x1, y0, y1 = 27.0, 79.0, 73.0, 37.0
    pts = []
    for i in range(steps + 1):
        t = i / steps
        ease = t * t * (3 - 2 * t) * 0.55 + t * t * 0.45
        pts.append((x0 + (x1 - x0) * t, y0 + (y1 - y0) * ease))
    return pts


# --- Raster -----------------------------------------------------------------------------------------------------

def stroke(draw, pts, width, color, scale):
    scaled = [(x * scale, y * scale) for x, y in pts]
    w = width * scale
    draw.line(scaled, fill=color, width=round(w), joint="curve")
    for x, y in (scaled[0], scaled[-1]):
        draw.ellipse((x - w / 2, y - w / 2, x + w / 2, y + w / 2), fill=color)


def draw_canvas(px, with_background=True):
    """The 108 unit canvas at px pixels, drawn 4 times bigger and scaled down to smooth the edges"""
    big = px * 4
    s = big / 108.0
    im = Image.new("RGBA", (big, big), (0, 0, 0, 0))
    if with_background:
        grad = Image.new("RGBA", (1, big))
        for y in range(big):
            grad.putpixel((0, y), mix(TOP, BOTTOM, y / (big - 1)) + (255,))
        im.paste(grad.resize((big, big)), (0, 0))
    d = ImageDraw.Draw(im)
    for x, y, size, color in cells():
        d.rounded_rectangle((x * s, y * s, (x + size) * s, (y + size) * s), radius=CELL_RADIUS * s, fill=color + (255,))
    pts = curve_points()
    stroke(d, pts, UNDER_WIDTH, INK + (255,), s)
    stroke(d, pts, CURVE_WIDTH, CURVE + (255,), s)
    ex, ey = pts[-1]
    r = (DOT + DOT_RING) * s
    d.ellipse((ex * s - r, ey * s - r, ex * s + r, ey * s + r), fill=INK + (255,))
    r = DOT * s
    d.ellipse((ex * s - r, ey * s - r, ex * s + r, ey * s + r), fill=ACCENT + (255,))
    return im.resize((px, px), Image.LANCZOS)


def tile(px, shape="square"):
    """The icon on its own: the visible middle of the canvas, as a rounded square, a circle, or the full square"""
    canvas = draw_canvas(round(px * 108 / 76))
    inset = (canvas.width - px) // 2
    im = canvas.crop((inset, inset, inset + px, inset + px))
    if shape == "full":
        return im
    mask = Image.new("L", (px * 4, px * 4), 0)
    md = ImageDraw.Draw(mask)
    if shape == "circle":
        md.ellipse((0, 0, px * 4 - 1, px * 4 - 1), fill=255)
    else:
        md.rounded_rectangle((0, 0, px * 4 - 1, px * 4 - 1), radius=px * 4 * 0.22, fill=255)
    mask = mask.resize((px, px), Image.LANCZOS)
    out = Image.new("RGBA", (px, px), (0, 0, 0, 0))
    out.paste(im, (0, 0), mask)
    return out


# --- Android vectors --------------------------------------------------------------------------------------------

def hexcolor(c):
    return "#FF%02X%02X%02X" % c


def f(v):
    return ("%.2f" % v).rstrip("0").rstrip(".")


def rounded_rect_path(x, y, w, h, r):
    return (f"M{f(x + r)},{f(y)}h{f(w - 2 * r)}a{f(r)},{f(r)} 0,0 1,{f(r)},{f(r)}v{f(h - 2 * r)}"
            f"a{f(r)},{f(r)} 0,0 1,{f(-r)},{f(r)}h{f(-(w - 2 * r))}a{f(r)},{f(r)} 0,0 1,{f(-r)},{f(-r)}"
            f"v{f(-(h - 2 * r))}a{f(r)},{f(r)} 0,0 1,{f(r)},{f(-r)}z")


def circle_path(cx, cy, r):
    return f"M{f(cx - r)},{f(cy)}a{f(r)},{f(r)} 0,1 1,{f(2 * r)},0a{f(r)},{f(r)} 0,1 1,{f(-2 * r)},0z"


def android_foreground():
    paths = []
    for x, y, size, color in cells():
        paths.append(f'    <path android:fillColor="{hexcolor(color)}" android:pathData="{rounded_rect_path(x, y, size, size, CELL_RADIUS)}" />')
    pts = curve_points()
    line = "M" + " L".join(f"{f(x)},{f(y)}" for x, y in pts)
    for width, color in ((UNDER_WIDTH, INK), (CURVE_WIDTH, CURVE)):
        paths.append(f'    <path android:strokeColor="{hexcolor(color)}" android:strokeWidth="{f(width)}" android:strokeLineCap="round" '
                     f'android:strokeLineJoin="round" android:pathData="{line}" />')
    ex, ey = pts[-1]
    paths.append(f'    <path android:fillColor="{hexcolor(INK)}" android:pathData="{circle_path(ex, ey, DOT + DOT_RING)}" />')
    paths.append(f'    <path android:fillColor="{hexcolor(ACCENT)}" android:pathData="{circle_path(ex, ey, DOT)}" />')
    return ('<?xml version="1.0" encoding="utf-8"?>\n'
            '<!-- Made by tools/make_icons.py: change the shapes there, not here -->\n'
            '<vector xmlns:android="http://schemas.android.com/apk/res/android"\n'
            '    android:width="108dp"\n    android:height="108dp"\n'
            '    android:viewportWidth="108"\n    android:viewportHeight="108">\n'
            + "\n".join(paths) + "\n</vector>\n")


def android_background():
    return ('<?xml version="1.0" encoding="utf-8"?>\n'
            '<!-- Made by tools/make_icons.py -->\n'
            '<vector xmlns:android="http://schemas.android.com/apk/res/android"\n'
            '    xmlns:aapt="http://schemas.android.com/aapt"\n'
            '    android:width="108dp"\n    android:height="108dp"\n'
            '    android:viewportWidth="108"\n    android:viewportHeight="108">\n'
            '    <path android:pathData="M0,0h108v108h-108z">\n'
            '        <aapt:attr name="android:fillColor">\n'
            '            <gradient android:type="linear" android:startX="54" android:startY="0" android:endX="54" android:endY="108">\n'
            f'                <item android:offset="0" android:color="{hexcolor(TOP)}" />\n'
            f'                <item android:offset="1" android:color="{hexcolor(BOTTOM)}" />\n'
            '            </gradient>\n'
            '        </aapt:attr>\n'
            '    </path>\n'
            '</vector>\n')


# --- Line icons (Feather style: a 24 unit grid, 2 unit strokes, round ends) -------------------------------------

def line_icon(draw_fn, px=64):
    big = px * 8
    s = big / 24.0
    im = Image.new("RGBA", (big, big), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    draw_fn(d, s)
    return im.resize((px, px), Image.LANCZOS)


BLACK = (0, 0, 0, 255)


def poly(d, s, pts, closed=False):
    pts = [(x * s, y * s) for x, y in pts]
    if closed:
        pts = pts + pts[:2]
    w = 2 * s
    d.line(pts, fill=BLACK, width=round(w), joint="curve")
    for x, y in pts:
        d.ellipse((x - w / 2, y - w / 2, x + w / 2, y + w / 2), fill=BLACK)


def ring(d, s, cx, cy, r):
    d.ellipse(((cx - r - 1) * s, (cy - r - 1) * s, (cx + r + 1) * s, (cy + r + 1) * s), fill=BLACK)
    d.ellipse(((cx - r + 1) * s, (cy - r + 1) * s, (cx + r - 1) * s, (cy + r - 1) * s), fill=(0, 0, 0, 0))


def settings_icon(d, s):
    """A gear: eight teeth round a ring"""
    cx = cy = 12.0
    outer, inner, teeth = 10.0, 7.6, 8
    pts = []
    for i in range(teeth):
        a = 2 * math.pi * i / teeth
        half = math.pi / teeth
        for da, r in ((-half * 0.95, inner), (-half * 0.45, outer), (half * 0.45, outer), (half * 0.95, inner)):
            pts.append((cx + r * math.cos(a + da), cy + r * math.sin(a + da)))
    poly(d, s, pts, closed=True)
    ring(d, s, cx, cy, 3.0)


def gallery_icon(d, s):
    """A picture: a frame, a sun and a hill"""
    x0, y0, x1, y1, r = 3, 3, 21, 21, 2
    d.rounded_rectangle(((x0 - 1) * s, (y0 - 1) * s, (x1 + 1) * s, (y1 + 1) * s), radius=(r + 1) * s, fill=BLACK)
    d.rounded_rectangle(((x0 + 1) * s, (y0 + 1) * s, (x1 - 1) * s, (y1 - 1) * s), radius=max(r - 1, 0.5) * s, fill=(0, 0, 0, 0))
    ring(d, s, 8.5, 8.5, 1.5)
    poly(d, s, [(21, 15), (16, 10), (5, 21)])


# --- Writing ----------------------------------------------------------------------------------------------------

def write(path, data):
    full = os.path.join(ROOT, path)
    os.makedirs(os.path.dirname(full), exist_ok=True)
    mode = "wb" if isinstance(data, bytes) else "w"
    with open(full, mode) as out:
        out.write(data)


def save(im, path, **kwargs):
    full = os.path.join(ROOT, path)
    os.makedirs(os.path.dirname(full), exist_ok=True)
    im.save(full, **kwargs)


def main():
    res = "androidApp/src/main/res"
    write(f"{res}/drawable-v24/ic_launcher_foreground.xml", android_foreground())
    write(f"{res}/drawable/ic_launcher_background.xml", android_background())
    for density, px in (("mdpi", 48), ("hdpi", 72), ("xhdpi", 96), ("xxhdpi", 144), ("xxxhdpi", 192)):
        save(tile(px), f"{res}/mipmap-{density}/ic_launcher.png")
        save(tile(px, "circle"), f"{res}/mipmap-{density}/ic_launcher_round.png")

    big = tile(1024)
    save(big.resize((512, 512), Image.LANCZOS), "composeApp/icons/icon.png")
    save(big, "composeApp/icons/icon.ico", sizes=[(16, 16), (24, 24), (32, 32), (48, 48), (64, 64), (128, 128), (256, 256)])
    save(big, "composeApp/icons/icon.icns")
    save(tile(256), "composeApp/src/commonMain/composeResources/drawable/tt_app_icon.png")

    web = "composeApp/src/wasmJsMain/resources"
    save(big, f"{web}/favicon.ico", sizes=[(16, 16), (32, 32), (48, 48)])
    save(tile(192), f"{web}/favicon.png")
    # iOS draws its own rounded corners, so this one is the full square
    save(tile(180, "full"), f"{web}/apple-touch-icon.png")

    drawable = "composeApp/src/commonMain/composeResources/drawable"
    save(line_icon(settings_icon), f"{drawable}/tt_settings.png")
    save(line_icon(gallery_icon), f"{drawable}/tt_gallery.png")
    print("Icons written")


if __name__ == "__main__":
    main()
