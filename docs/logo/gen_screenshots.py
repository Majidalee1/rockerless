# ---------------------------------------------------------------- art frames
import math


def cookie_points(cx, cy, R, lobes=9, amp=0.055, steps=720):
    pts = []
    for i in range(steps):
        t = 2 * math.pi * i / steps
        r = R * (1 + amp * math.cos(lobes * t))
        pts.append((cx + r * math.cos(t), cy + r * math.sin(t)))
    return pts


def draw_knob(img, cx, cy, R, ring_w=None, ring_span=270, theme="dark"):
    """The app mark: wavy cookie knob inside a level ring."""
    d = ImageDraw.Draw(img)
    ring_w = ring_w or int(R * 0.085)
    ring_R = R * 1.42
    track = (52, 67, 107) if theme == "dark" else (216, 206, 240)
    fill = (233, 221, 255) if theme == "dark" else (91, 63, 214)
    knob = (175, 198, 255) if theme == "dark" else (122, 104, 226)

    d.ellipse([cx - ring_R, cy - ring_R, cx + ring_R, cy + ring_R], outline=track, width=ring_w)
    start = -90 - ring_span / 2 + 0
    d.arc([cx - ring_R, cy - ring_R, cx + ring_R, cy + ring_R],
          start=start, end=start + ring_span, fill=fill, width=ring_w)
    for a in (start, start + ring_span):
        rad = math.radians(a)
        ex, ey = cx + ring_R * math.cos(rad), cy + ring_R * math.sin(rad)
        d.ellipse([ex - ring_w / 2, ey - ring_w / 2, ex + ring_w / 2, ey + ring_w / 2], fill=fill)

    d.polygon(cookie_points(cx, cy, R), fill=knob)
    d.ellipse([cx - R * 0.085, cy - R * 0.62, cx + R * 0.085, cy - R * 0.40], fill=(24, 30, 50))

    # speaker glyph
    s = R * 0.30
    d.polygon([(cx - s, cy - s * 0.42), (cx - s * 0.42, cy - s * 0.42), (cx + s * 0.05, cy - s),
               (cx + s * 0.05, cy + s), (cx - s * 0.42, cy + s * 0.42), (cx - s, cy + s * 0.42)],
              fill=(24, 30, 50))
    for k, rr in enumerate((0.45, 0.75)):
        d.arc([cx - s * rr, cy - s * rr * 1.1, cx + s * rr * 1.6, cy + s * rr * 1.1],
              start=-55, end=55, fill=(24, 30, 50), width=max(3, int(R * 0.035)))


def header(d, headline, subline, theme):
    P = PALETTE[theme]
    hf, sf = font(FONTS_BOLD, 74), font(FONTS_REG, 36)
    y = 118
    for ln in wrap(d, headline, hf, W - 150):
        d.text((75, y), ln, font=hf, fill=P["head"])
        y += 86
    d.text((75, y + 10), subline, font=sf, fill=P["sub"])


def footer(img, d, theme, logo_path):
    P = PALETTE[theme]
    logo = Image.open(logo_path).convert("RGBA").resize((72, 72), Image.LANCZOS)
    ly = H - 118
    img.paste(logo, (72, ly), logo)
    d.text((72 + 92, ly + 20), "Rockerless", font=font(FONTS_BOLD, 30), fill=P["foot"])


def art_frame(out_name, headline, subline, kind, theme, logo_path):
    P = PALETTE[theme]
    img = gradient(P["bg1"], P["bg2"]).convert("RGBA")
    d = ImageDraw.Draw(img)
    header(d, headline, subline, theme)

    if kind == "knob":
        cx, cy, R = W // 2, 820, 235
        glow = Image.new("RGBA", (W, H), (0, 0, 0, 0))
        ImageDraw.Draw(glow).ellipse([cx - 420, cy - 420, cx + 420, cy + 420], fill=(120, 140, 210, 40))
        img = Image.alpha_composite(img, glow.filter(ImageFilter.GaussianBlur(60)))
        draw_knob(img, cx, cy, R, theme=theme)

    elif kind == "snap":
        cy = 880
        d.rounded_rectangle([W - 34, cy - 210, W, cy + 210], radius=18, fill=P["frame"])
        d.rounded_rectangle([W - 26, cy - 190, W, cy + 190], radius=14, fill=P["sub"])
        draw_knob(img, W - 290, cy, 168, theme=theme)
        d = ImageDraw.Draw(img)
        d.line([(150, cy), (W - 560, cy)], fill=P["sub"], width=7)
        d.polygon([(W - 520, cy), (W - 568, cy - 26), (W - 568, cy + 26)], fill=P["sub"])

    elif kind == "trust":
        rows = ["No ads or trackers", "No internet permission", "No accessibility service"]
        y = 760
        f = font(FONTS_REG, 42)
        for r in rows:
            d.ellipse([80, y + 6, 128, y + 54], fill=P["sub"])
            d.line([(96, y + 32), (108, y + 44)], fill=P["bg1"], width=6)
            d.line([(108, y + 44), (124, y + 18)], fill=P["bg1"], width=6)
            d.text((160, y + 4), r, font=f, fill=P["head"])
            y += 118

    footer(img, ImageDraw.Draw(img), theme, logo_path)
    img.convert("RGB").save(os.path.join(OUT, out_name), "PNG", optimize=True)
    print("wrote", out_name, img.size)


"""Compose Play Store phone screenshots: branded 1080x1920 marketing frames.

Real app captures are placed inside a device frame with a benefit-led headline,
on the Rockerless palette, so the listing reads as designed rather than as raw
device dumps.
"""
from PIL import Image, ImageDraw, ImageFont, ImageFilter
import os

W, H = 1080, 1920
OUT = "/tmp/play-assets"

PALETTE = {
    "dark": {"bg1": (18, 26, 48), "bg2": (44, 60, 100), "head": (233, 221, 255),
             "sub": (175, 198, 255), "frame": (10, 14, 26), "foot": (200, 210, 235)},
    "light": {"bg1": (240, 236, 255), "bg2": (214, 204, 246), "head": (42, 32, 74),
              "sub": (91, 63, 214), "frame": (255, 255, 255), "foot": (90, 80, 120)},
}

FONTS_BOLD = ["/System/Library/Fonts/HelveticaNeue.ttc", "/System/Library/Fonts/Helvetica.ttc",
              "/System/Library/Fonts/Supplemental/Arial Bold.ttf"]
FONTS_REG = ["/System/Library/Fonts/HelveticaNeue.ttc", "/System/Library/Fonts/Helvetica.ttc",
             "/System/Library/Fonts/Supplemental/Arial.ttf"]


def font(paths, size, index=0):
    for p in paths:
        if os.path.exists(p):
            try:
                return ImageFont.truetype(p, size, index=index)
            except Exception:
                continue
    return ImageFont.load_default()


def gradient(c1, c2):
    img = Image.new("RGB", (1, H))
    d = ImageDraw.Draw(img)
    for y in range(H):
        t = y / (H - 1)
        d.point((0, y), tuple(int(a + (b - a) * t) for a, b in zip(c1, c2)))
    return img.resize((W, H))


def wrap(draw, text, fnt, max_w):
    words, lines, cur = text.split(), [], ""
    for w in words:
        trial = (cur + " " + w).strip()
        if draw.textlength(trial, font=fnt) <= max_w:
            cur = trial
        else:
            if cur:
                lines.append(cur)
            cur = w
    if cur:
        lines.append(cur)
    return lines


def rounded_mask(size, radius):
    m = Image.new("L", size, 0)
    ImageDraw.Draw(m).rounded_rectangle([0, 0, size[0] - 1, size[1] - 1], radius=radius, fill=255)
    return m


def compose(screen_path, out_name, headline, subline, theme, logo_path):
    P = PALETTE[theme]
    img = gradient(P["bg1"], P["bg2"])
    d = ImageDraw.Draw(img)

    hf = font(FONTS_BOLD, 74)
    sf = font(FONTS_REG, 36)
    ff = font(FONTS_BOLD, 30)

    lines = wrap(d, headline, hf, W - 150)
    y = 118
    for ln in lines:
        d.text((75, y), ln, font=hf, fill=P["head"])
        y += 86
    d.text((75, y + 10), subline, font=sf, fill=P["sub"])
    head_bottom = y + 70

    # device
    shot = Image.open(screen_path).convert("RGB")
    max_h = 1220
    scale = max_h / shot.height
    sw, sh = int(shot.width * scale), max_h
    shot = shot.resize((sw, sh), Image.LANCZOS)

    pad = 16
    fw, fh = sw + pad * 2, sh + pad * 2
    fx = (W - fw) // 2
    fy = max(head_bottom + 40, 470)

    shadow = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    ImageDraw.Draw(shadow).rounded_rectangle(
        [fx + 6, fy + 18, fx + fw + 6, fy + fh + 18], radius=52, fill=(0, 0, 0, 90))
    img = Image.alpha_composite(img.convert("RGBA"), shadow.filter(ImageFilter.GaussianBlur(22))).convert("RGB")
    d = ImageDraw.Draw(img)

    d.rounded_rectangle([fx, fy, fx + fw, fy + fh], radius=48, fill=P["frame"])
    inner_mask = rounded_mask((sw, sh), 34)
    img.paste(shot, (fx + pad, fy + pad), inner_mask)

    # footer
    logo = Image.open(logo_path).convert("RGBA").resize((72, 72), Image.LANCZOS)
    ly = H - 118
    img.paste(logo, (72, ly), logo)
    d.text((72 + 92, ly + 20), "Rockerless", font=ff, fill=P["foot"])

    img.save(os.path.join(OUT, out_name), "PNG", optimize=True)
    print("wrote", out_name, img.size)


if __name__ == "__main__":
    repo = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
    logo = os.path.join(repo, "docs", "logo", "rockerless-logo.png")
    light = os.path.join(repo, "docs", "screenshots", "settings-light.png")
    dark = os.path.join(repo, "docs", "screenshots", "settings-dark.png")
    os.makedirs(OUT, exist_ok=True)

    compose(light, "01-broken-volume-button.png",
            "Broken volume button? One bubble fixes it.",
            "No hardware. No accessibility service.", "light", logo)
    compose(dark, "02-tap-volume-panel.png",
            "Tap for Android's real volume panel.",
            "Media, call, ring and alarm volume.", "dark", logo)

    art_frame("03-long-press-mute.png", "Long-press to mute.",
              "Long-press again to restore.", "knob", "dark", logo)
    art_frame("04-drag-and-snap.png", "Drag it anywhere.",
              "It snaps to the nearest edge.", "snap", "dark", logo)
    art_frame("05-quiet-by-design.png", "Quiet by design.",
              "What it does not do matters too.", "trust", "dark", logo)
