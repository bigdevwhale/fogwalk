"""
Builds every marketing picture of Fogwalk from raw phone screenshots.

Take the screenshots on the demo build (`./gradlew installDebug -Pdemo`, old Prague) on a 1080x2340 phone, in both
languages, and name them `<lang>_<screen>.png` (lang: en, ru; screen: story1, story3, map, quests, shop, chronicle,
relics, deeds). Then:

    python tools/store_shots.py <folder with the screenshots> [--site <stillcraft repo>]

It writes:
  docs/screenshots/<name>[.ru].png                     the README's gallery (540 px wide, status bar cut off)
  docs/screenshots/overview[.ru].png                   three phones for the top of the README
  docs/google-play/graphics/screenshots-<en|ru>/N.png  1080x1920 store screenshots with a caption
  docs/google-play/graphics/feature-graphic-<en|ru>.png  the 1024x500 store banner
  <site>/assets/fw-<name>[.ru].png                     the website's gallery, the same as the README's
"""
import os
import shutil
import sys

from PIL import Image, ImageDraw, ImageFilter, ImageFont

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RES = os.path.join(ROOT, "app", "src", "main", "res")
FONT = os.path.join(RES, "font")
ART = os.path.join(RES, "drawable-nodpi")

# The phone's status bar, cut off every screenshot: the clock and icons say nothing about the game.
STATUS = 96

INK = (43, 29, 16)
GOLD = (174, 122, 28)
GILT = (201, 154, 46)
CREAM = (255, 246, 224)
PAPER = (239, 227, 200)

# README and website names, by the screen each shows.
GALLERY = {
    "map": "map", "quests": "quests", "onboarding": "story3", "hoard": "relics",
    "merchant": "shop", "chronicle": "chronicle", "deeds": "deeds",
}

CAPTIONS = {
    "en": [
        ("map", "Your city, hidden in mist.", "Walk to bring it back."),
        ("story3", "A story in every step.", "Free the realm from the Mist of Forgetting."),
        ("quests", "Quests every dawn.", "Keep the streak alive."),
        ("shop", "Companions walk with you.", "A fox, an owl, a hedgehog and more."),
        ("relics", "60 relics to find.", "Ten sets, four of them seasonal."),
        ("chronicle", "Your own atlas.", "Every road you have walked."),
        ("deeds", "209 deeds.", "From Wanderer to Legend of the Realm."),
    ],
    "ru": [
        ("map", "Ваш город скрыт туманом.", "Гуляйте, чтобы вернуть его."),
        ("story3", "История в каждом шаге.", "Освободите королевство от Тумана Забвения."),
        ("quests", "Квесты каждый рассвет.", "Не дайте серии прерваться."),
        ("shop", "Спутники идут с вами.", "Лис, сова, ёжик и другие."),
        ("relics", "60 реликвий.", "Десять наборов, четыре из них сезонные."),
        ("chronicle", "Ваш собственный атлас.", "Каждая пройденная дорога."),
        ("deeds", "209 деяний.", "От Странника до Легенды королевства."),
    ],
}

FEATURE = {
    "en": ("Your city, hidden in mist.", "Walk to bring it back."),
    "ru": ("Ваш город скрыт туманом.", "Гуляйте, чтобы вернуть его."),
}


def font(name, size):
    return ImageFont.truetype(os.path.join(FONT, name), size)


def raw(shots, lang, screen):
    return Image.open(os.path.join(shots, f"{lang}_{screen}.png")).convert("RGB")


def screen(shots, lang, name):
    """The screenshot without the status bar."""
    im = raw(shots, lang, name)
    return im.crop((0, STATUS, im.width, im.height))


def paper(w, h):
    """Warm paper: the game's parchment tiled faintly over cream, darker toward the edges."""
    bg = Image.new("RGB", (w, h), PAPER)
    tex = Image.open(os.path.join(ART, "parchment.jpg")).convert("RGB")
    tile = Image.new("RGB", (w, h))
    for x in range(0, w, tex.width):
        for y in range(0, h, tex.height):
            tile.paste(tex, (x, y))
    bg = Image.blend(bg, tile, 0.35)
    # A soft vignette, as on the map.
    mask = Image.new("L", (w, h), 0)
    d = ImageDraw.Draw(mask)
    d.ellipse((-w * 0.2, -h * 0.2, w * 1.2, h * 1.2), fill=255)
    mask = mask.filter(ImageFilter.GaussianBlur(min(w, h) // 6))
    dark = Image.new("RGB", (w, h), (205, 182, 140))
    return Image.composite(bg, dark, mask)


def phone(shot, width):
    """A screenshot in a gilt phone frame with rounded corners, on transparency, with a soft shadow."""
    k = width / shot.width
    inner = shot.resize((width, round(shot.height * k)), Image.LANCZOS)
    pad = max(6, width // 60)
    r = width // 11
    w, h = inner.width + 2 * pad, inner.height + 2 * pad
    out = Image.new("RGBA", (w + 60, h + 60), (0, 0, 0, 0))
    # Shadow.
    sh = Image.new("L", out.size, 0)
    ImageDraw.Draw(sh).rounded_rectangle((30, 40, 30 + w, 40 + h), r + pad, fill=110)
    sh = sh.filter(ImageFilter.GaussianBlur(18))
    out.paste((60, 40, 20, 255), (0, 0), sh)
    # Gilt body.
    body = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    bd = ImageDraw.Draw(body)
    bd.rounded_rectangle((0, 0, w - 1, h - 1), r + pad, fill=GILT + (255,))
    bd.rounded_rectangle((2, 2, w - 3, h - 3), r + pad - 2, outline=(246, 220, 140, 255), width=2)
    # The screen with its corners rounded.
    m = Image.new("L", inner.size, 0)
    ImageDraw.Draw(m).rounded_rectangle((0, 0, inner.width - 1, inner.height - 1), r, fill=255)
    body.paste(inner, (pad, pad), m)
    out.alpha_composite(body, (30, 30))
    return out


def fit(text, name, size, width):
    """The largest font up to [size] that sets [text] within [width]."""
    while size > 20:
        f = font(name, size)
        if f.getlength(text) <= width:
            return f
        size -= 2
    return font(name, size)


def centred(d, text, f, y, w, fill):
    d.text(((w - f.getlength(text)) / 2, y), text, font=f, fill=fill)


def store_shot(shots, lang, name, head, sub):
    w, h = 1080, 1920
    im = paper(w, h).convert("RGBA")
    d = ImageDraw.Draw(im)
    centred(d, head, fit(head, "sc_bold.ttf", 70, w - 120), 120, w, INK)
    centred(d, sub, fit(sub, "sans_medium.ttf", 44, w - 140), 215, w, GOLD)
    ph = phone(screen(shots, lang, name), 700)
    im.alpha_composite(ph, ((w - ph.width) // 2, 320))
    return im.convert("RGB")


def overview(shots, lang):
    w, h = 1800, 1100
    im = paper(w, h).convert("RGBA")
    names = ["story3", "map", "shop"]
    phones = [phone(screen(shots, lang, n), 470) for n in names]
    gap = (w - sum(p.width for p in phones)) // (len(phones) + 1)
    x = gap
    for i, p in enumerate(phones):
        # The middle phone a little higher, the way a hand fans cards.
        im.alpha_composite(p, (x, 30 if i == 1 else 70))
        x += p.width + gap
    return im.convert("RGB")


def feature(shots, lang):
    w, h = 1024, 500
    bg = Image.open(os.path.join(ART, "banner_chronicle.jpg")).convert("RGB")
    k = max(w / bg.width, h / bg.height)
    bg = bg.resize((round(bg.width * k), round(bg.height * k)), Image.LANCZOS)
    bg = bg.crop(((bg.width - w) // 2, (bg.height - h) // 2, (bg.width - w) // 2 + w, (bg.height - h) // 2 + h)).convert("RGBA")
    # Darken from the left so the words read, as the app's banners do.
    shade = Image.new("RGBA", (w, h))
    sd = ImageDraw.Draw(shade)
    for x in range(w):
        a = int(225 * max(0.0, 1 - x / (w * 0.72)))
        sd.line((x, 0, x, h), fill=(43, 29, 16, a))
    im = Image.alpha_composite(bg, shade)
    d = ImageDraw.Draw(im)
    icon = Image.open(os.path.join(ROOT, "docs", "google-play", "graphics", "icon-512.png")).convert("RGBA").resize((96, 96), Image.LANCZOS)
    im.alpha_composite(icon, (48, 64))
    d.text((162, 72), "Fogwalk", font=font("sc_bold.ttf", 76), fill=CREAM)
    head, sub = FEATURE[lang]
    d.text((50, 230), head, font=fit(head, "sc_bold.ttf", 46, 520), fill=CREAM)
    d.text((50, 296), sub, font=fit(sub, "sans_medium.ttf", 34, 520), fill=(240, 200, 110))
    # The map on a phone, tilted at the right.
    ph = phone(screen(shots, lang, "map"), 300).rotate(-7, resample=Image.BICUBIC, expand=True)
    im.alpha_composite(ph, (w - ph.width + 20, 40))
    return im.convert("RGB")


def light(im, path):
    """Saves a picture for the web: 256 colours with dithering, a third of the size and no visible loss."""
    im.convert("RGB").quantize(256, method=Image.Quantize.MEDIANCUT, dither=Image.Dither.FLOYDSTEINBERG).save(path, optimize=True)


def main():
    shots = sys.argv[1]
    site = sys.argv[sys.argv.index("--site") + 1] if "--site" in sys.argv else None
    gallery_dir = os.path.join(ROOT, "docs", "screenshots")
    play = os.path.join(ROOT, "docs", "google-play", "graphics")
    for lang in ("en", "ru"):
        dot = "" if lang == "en" else ".ru"
        for out, name in GALLERY.items():
            im = screen(shots, lang, name)
            im = im.resize((540, round(im.height * 540 / im.width)), Image.LANCZOS)
            path = os.path.join(gallery_dir, f"{out}{dot}.png")
            light(im, path)
            if site:
                shutil.copyfile(path, os.path.join(site, "assets", f"fw-{out}{dot}.png"))
        light(overview(shots, lang), os.path.join(gallery_dir, f"overview{dot}.png"))
        folder = os.path.join(play, f"screenshots-{lang}")
        for f in os.listdir(folder):
            if f.endswith(".png"):
                os.remove(os.path.join(folder, f))
        for i, (name, head, sub) in enumerate(CAPTIONS[lang], 1):
            store_shot(shots, lang, name, head, sub).save(os.path.join(folder, f"{i}.png"), optimize=True)
        feature(shots, lang).save(os.path.join(play, f"feature-graphic-{lang}.png"), optimize=True)
    print("done")


if __name__ == "__main__":
    main()
