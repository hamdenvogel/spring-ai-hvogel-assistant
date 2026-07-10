"""Gera logo_hvogel_brand.png a partir de logo.bmp com texto profissional."""

from __future__ import annotations

from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

DOCS = Path(__file__).parent
SOURCE_LOGO = DOCS / "logo.bmp"
OUTPUT_LOGO = DOCS / "logo_hvogel_brand.png"
OUTPUT_LOGO_SMALL = DOCS / "logo_hvogel_brand_small.png"

BRAND_DARK = (0, 70, 130)      # #004682
BRAND_MID = (0, 119, 182)      # #0077b6
BRAND_LIGHT = (72, 169, 230)   # #48a9e6
SUBTITLE_COLOR = (90, 100, 110)

TITLE_GAP = 18   # espaco entre "hvogel" e subtitulo (evita sobreposicao do descender do g)
SUB_TAG_GAP = 12 # espaco entre subtitulo e tagline


def _load_font(size: int, bold: bool = False) -> ImageFont.FreeTypeFont | ImageFont.ImageFont:
    # Bahnschrift/Corbel: descenders mais contidos que Segoe UI em tamanhos grandes
    candidates = [
        Path(r"C:\Windows\Fonts\Bahnschrift.ttf"),
        Path(r"C:\Windows\Fonts\corbelb.ttf") if bold else Path(r"C:\Windows\Fonts\corbel.ttf"),
        Path(r"C:\Windows\Fonts\calibrib.ttf") if bold else Path(r"C:\Windows\Fonts\calibri.ttf"),
        Path(r"C:\Windows\Fonts\segoeuib.ttf") if bold else Path(r"C:\Windows\Fonts\segoeui.ttf"),
        Path(r"C:\Windows\Fonts\arialbd.ttf") if bold else Path(r"C:\Windows\Fonts\arial.ttf"),
    ]
    for path in candidates:
        if path.exists():
            return ImageFont.truetype(str(path), size)
    return ImageFont.load_default()


def _rounded_rectangle(draw: ImageDraw.ImageDraw, xy, radius: int, fill):
    draw.rounded_rectangle(xy, radius=radius, fill=fill)


def _text_size(draw: ImageDraw.ImageDraw, text: str, font) -> tuple[int, int]:
    bbox = draw.textbbox((0, 0), text, font=font)
    return bbox[2] - bbox[0], bbox[3] - bbox[1]


def build_brand_logo(height: int = 240) -> Image.Image:
    icon = Image.open(SOURCE_LOGO).convert("RGBA")
    icon_h = height - 48
    icon_w = icon_h
    icon = icon.resize((icon_w, icon_h), Image.Resampling.LANCZOS)

    mask = Image.new("L", (icon_w, icon_h), 0)
    mask_draw = ImageDraw.Draw(mask)
    mask_draw.rounded_rectangle((0, 0, icon_w, icon_h), radius=28, fill=255)
    icon.putalpha(mask)

    font_title = _load_font(50, bold=True)
    font_sub = _load_font(21, bold=False)
    font_tag = _load_font(15, bold=False)

    title = "hvogel"
    subtitle = "Fábrica de Software"
    tagline = "Excelência em Engenharia de Software"

    measure = ImageDraw.Draw(Image.new("RGBA", (1, 1)))
    title_w, title_h = _text_size(measure, title, font_title)
    sub_w, sub_h = _text_size(measure, subtitle, font_sub)
    tag_w, tag_h = _text_size(measure, tagline, font_tag)

    text_block_w = max(title_w, sub_w, tag_w)
    text_block_h = title_h + TITLE_GAP + sub_h + SUB_TAG_GAP + tag_h

    padding_x = 36
    gap = 28
    canvas_w = padding_x + icon_w + gap + text_block_w + padding_x
    canvas_h = height

    canvas = Image.new("RGBA", (canvas_w, canvas_h), (255, 255, 255, 0))

    bg = Image.new("RGBA", (canvas_w, canvas_h), (255, 255, 255, 255))
    bg_draw = ImageDraw.Draw(bg)
    for y in range(canvas_h):
        ratio = y / max(canvas_h - 1, 1)
        r = int(248 - ratio * 8)
        g = int(251 - ratio * 6)
        b = int(255 - ratio * 4)
        bg_draw.line([(0, y), (canvas_w, y)], fill=(r, g, b, 255))
    _rounded_rectangle(bg_draw, (4, 4, canvas_w - 4, canvas_h - 4), 18, (245, 248, 252, 255))
    canvas = Image.alpha_composite(canvas, bg)

    accent = Image.new("RGBA", (canvas_w, canvas_h), (0, 0, 0, 0))
    accent_draw = ImageDraw.Draw(accent)
    accent_draw.rounded_rectangle((4, 4, 10, canvas_h - 4), radius=4, fill=(*BRAND_MID, 255))
    canvas = Image.alpha_composite(canvas, accent)

    draw = ImageDraw.Draw(canvas)

    icon_x = padding_x
    icon_y = (canvas_h - icon_h) // 2
    canvas.paste(icon, (icon_x, icon_y), icon)

    text_x = icon_x + icon_w + gap
    text_block_y = icon_y + (icon_h - text_block_h) // 2

    title_y = text_block_y
    sub_y = title_y + title_h + TITLE_GAP
    tag_y = sub_y + sub_h + SUB_TAG_GAP

    # Sombra sutil (sem segunda camada que aumenta a area visual do titulo)
    draw.text((text_x + 1, title_y + 1), title, font=font_title, fill=(0, 0, 0, 45))
    draw.text((text_x, title_y), title, font=font_title, fill=BRAND_DARK)

    draw.text((text_x, sub_y), subtitle, font=font_sub, fill=BRAND_MID)

    highlight = "Excelência"
    tag_rest = " em Engenharia de Software"
    draw.text((text_x, tag_y), highlight, font=font_tag, fill=SUBTITLE_COLOR)
    exc_w, exc_h = _text_size(draw, highlight, font_tag)
    draw.text((text_x + exc_w, tag_y), tag_rest, font=font_tag, fill=SUBTITLE_COLOR)

    # Sublinhado somente em "Excelência"
    underline_y = tag_y + exc_h + 2
    draw.line([(text_x, underline_y), (text_x + exc_w, underline_y)], fill=BRAND_LIGHT, width=2)

    return canvas


def main():
    if not SOURCE_LOGO.exists():
        raise FileNotFoundError(f"Logo nao encontrada: {SOURCE_LOGO}")

    logo = build_brand_logo(height=240)
    logo.save(OUTPUT_LOGO, format="PNG")

    small = logo.resize((int(logo.width * 0.45), int(logo.height * 0.45)), Image.Resampling.LANCZOS)
    small.save(OUTPUT_LOGO_SMALL, format="PNG")

    print(f"Logo gerada: {OUTPUT_LOGO} ({logo.size})")
    print(f"Logo pequena: {OUTPUT_LOGO_SMALL} ({small.size})")


if __name__ == "__main__":
    main()
