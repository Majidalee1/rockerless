"""Generate the Play Store feature graphic (1024x500) from the existing logo."""
from PIL import Image, ImageDraw, ImageFont
import os

BG = (31, 43, 77)        # #1F2B4D midnight
FILL = (233, 221, 255)   # #E9DDFF
ACCENT = (175, 198, 255) # #AFC6FF
W, H = 1024, 500

FONT_CANDIDATES_BOLD = [
    "/System/Library/Fonts/HelveticaNeue.ttc",
    "/System/Library/Fonts/Helvetica.ttc",
    "/System/Library/Fonts/Supplemental/Arial Bold.ttf",
    "/Library/Fonts/Arial Bold.ttf",
]
FONT_CANDIDATES_REG = [
    "/System/Library/Fonts/HelveticaNeue.ttc",
    "/System/Library/Fonts/Helvetica.ttc",
    "/System/Library/Fonts/Supplemental/Arial.ttf",
]

def load(paths, size, index=0):
    for p in paths:
        if os.path.exists(p):
            try:
                return ImageFont.truetype(p, size, index=index)
            except Exception:
                continue
    return ImageFont.load_default()

img = Image.new("RGB", (W, H), BG)
d = ImageDraw.Draw(img)

# soft radial glow behind the mark
glow = Image.new("L", (W, H), 0)
gd = ImageDraw.Draw(glow)
for i in range(60, 0, -1):
    r = int(150 + i * 2.2)
    gd.ellipse([200 - r, H // 2 - r, 200 + r, H // 2 + r], fill=int(4 + (60 - i) * 0.6))
img.paste(Image.new("RGB", (W, H), (52, 67, 107)), (0, 0), glow)

logo_path = os.path.join(os.path.dirname(__file__), "rockerless-logo.png")
logo = Image.open(logo_path).convert("RGBA")
size = 330
logo = logo.resize((size, size), Image.LANCZOS)
img.paste(logo, (60, (H - size) // 2), logo)

title_font = load(FONT_CANDIDATES_BOLD, 84)
tag_font = load(FONT_CANDIDATES_REG, 28)
sub_font = load(FONT_CANDIDATES_REG, 24)

x = 440
d.text((x, 126), "Rockerless", font=title_font, fill=FILL)
d.text((x, 252), "The volume button your phone forgot.", font=tag_font, fill=ACCENT)
d.text((x, 300), "Floating bubble for broken volume buttons", font=sub_font, fill=(200, 210, 235))

# thin accent rule under the title
d.rounded_rectangle([x, 231, x + 84, 236], radius=2, fill=ACCENT)

out_dir = os.path.join(os.path.dirname(__file__), "..", "play-assets")
os.makedirs(out_dir, exist_ok=True)
out = os.path.join(out_dir, "feature-graphic-1024x500.png")
img.save(out, "PNG", optimize=True)
print("wrote", os.path.normpath(out), img.size)
