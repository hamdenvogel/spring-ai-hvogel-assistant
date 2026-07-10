"""Gera hvogel_politicas_rh.pdf a partir do markdown com layout ReportLab."""

from __future__ import annotations

import re
from pathlib import Path

from reportlab.lib import colors
from reportlab.lib.enums import TA_CENTER, TA_JUSTIFY
from reportlab.lib.pagesizes import A4
from reportlab.lib.styles import ParagraphStyle, getSampleStyleSheet
from reportlab.lib.units import cm
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
from reportlab.platypus import BaseDocTemplate, Frame, Image, PageTemplate, Paragraph, Spacer

from build_logo_hvogel import OUTPUT_LOGO, OUTPUT_LOGO_SMALL, build_brand_logo

md_path = Path(__file__).with_name("hvogel_politicas_rh.md")
pdf_path = md_path.with_suffix(".pdf")
text = md_path.read_text(encoding="utf-8")

FONT_REGULAR = "Arial"
FONT_BOLD = "Arial-Bold"
ARIAL = Path(r"C:\Windows\Fonts\arial.ttf")
ARIAL_BOLD = Path(r"C:\Windows\Fonts\arialbd.ttf")

MARGIN = 2.5 * cm
PAGE_WIDTH, PAGE_HEIGHT = A4
FRAME_WIDTH = PAGE_WIDTH - 2 * MARGIN

# Zonas reservadas para cabecalho/rodape (conteudo nao pode invadir estas areas)
HEADER_ZONE = 2.9 * cm
FOOTER_ZONE = 1.8 * cm
HEADER_Y = PAGE_HEIGHT - 1.35 * cm
FOOTER_Y = 0.9 * cm
FIRST_PAGE_HEADER_ZONE = 1.6 * cm
LOGO_HEADER_HEIGHT = 1.15 * cm


def ensure_brand_logo() -> tuple[Path, Path]:
    from PIL import Image as PILImage

    if not OUTPUT_LOGO.exists() or not OUTPUT_LOGO_SMALL.exists():
        logo = build_brand_logo(height=240)
        logo.save(OUTPUT_LOGO, format="PNG")
        small = logo.resize(
            (int(logo.width * 0.45), int(logo.height * 0.45)),
            PILImage.Resampling.LANCZOS,
        )
        small.save(OUTPUT_LOGO_SMALL, format="PNG")
    return OUTPUT_LOGO, OUTPUT_LOGO_SMALL


def logo_dimensions(path: Path, target_width: float) -> tuple[float, float]:
    from PIL import Image as PILImage

    with PILImage.open(path) as img:
        w, h = img.size
    ratio = h / w
    return target_width, target_width * ratio


def logo_dimensions_by_height(path: Path, target_height: float) -> tuple[float, float]:
    from PIL import Image as PILImage

    with PILImage.open(path) as img:
        w, h = img.size
    width = target_height * (w / h)
    return width, target_height


def make_cover_logo(path: Path) -> Image:
  width, height = logo_dimensions(path, 14 * cm)
  logo = Image(str(path), width=width, height=height)
  logo.hAlign = "CENTER"
  return logo


def draw_header_logo(canvas) -> None:
    _, small_path = ensure_brand_logo()
    width, height = logo_dimensions_by_height(small_path, LOGO_HEADER_HEIGHT)
    y = PAGE_HEIGHT - HEADER_ZONE + 0.25 * cm
    canvas.drawImage(
        str(small_path),
        MARGIN,
        y,
        width=width,
        height=height,
        mask="auto",
        preserveAspectRatio=True,
        anchor="sw",
    )


def register_fonts() -> None:
    pdfmetrics.registerFont(TTFont(FONT_REGULAR, str(ARIAL)))
    pdfmetrics.registerFont(TTFont(FONT_BOLD, str(ARIAL_BOLD)))
    pdfmetrics.registerFontFamily(
        FONT_REGULAR,
        normal=FONT_REGULAR,
        bold=FONT_BOLD,
        italic=FONT_REGULAR,
        boldItalic=FONT_BOLD,
    )


def escape_xml(value: str) -> str:
    return (
        value.replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace('"', "&quot;")
    )


def inline_md_to_reportlab(value: str) -> str:
    value = escape_xml(value)
    value = (
        value.replace("—", "-")
        .replace("–", "-")
        .replace("≥", "&gt;=")
        .replace("•", "-")
    )
    return re.sub(r"\*\*(.+?)\*\*", r"<b>\1</b>", value)


def build_styles():
    base = getSampleStyleSheet()
    return {
        "title": ParagraphStyle(
            "TitleHvogel",
            parent=base["Heading1"],
            fontName=FONT_BOLD,
            fontSize=16,
            leading=20,
            textColor=colors.HexColor("#004682"),
            spaceBefore=8,
            spaceAfter=10,
            alignment=TA_CENTER,
        ),
        "h1": ParagraphStyle(
            "H1Hvogel",
            parent=base["Heading1"],
            fontName=FONT_BOLD,
            fontSize=13,
            leading=17,
            textColor=colors.HexColor("#004682"),
            spaceBefore=16,
            spaceAfter=8,
        ),
        "h2": ParagraphStyle(
            "H2Hvogel",
            parent=base["Heading2"],
            fontName=FONT_BOLD,
            fontSize=11,
            leading=14,
            textColor=colors.HexColor("#004682"),
            spaceBefore=12,
            spaceAfter=6,
        ),
        "body": ParagraphStyle(
            "BodyHvogel",
            parent=base["BodyText"],
            fontName=FONT_REGULAR,
            fontSize=10,
            leading=14,
            alignment=TA_JUSTIFY,
            spaceBefore=0,
            spaceAfter=6,
        ),
        "bullet": ParagraphStyle(
            "BulletHvogel",
            parent=base["BodyText"],
            fontName=FONT_REGULAR,
            fontSize=10,
            leading=14,
            leftIndent=18,
            firstLineIndent=-10,
            spaceBefore=0,
            spaceAfter=4,
            alignment=TA_JUSTIFY,
        ),
    }


def header_footer(canvas, doc):
    canvas.saveState()
    canvas.setFont(FONT_REGULAR, 8)
    canvas.setFillColor(colors.grey)
    canvas.drawCentredString(PAGE_WIDTH / 2, FOOTER_Y, f"Pagina {doc.page}")
    canvas.restoreState()


def header_footer_later(canvas, doc):
    canvas.saveState()

    line_y = PAGE_HEIGHT - HEADER_ZONE + 0.15 * cm
    canvas.setStrokeColor(colors.HexColor("#CCCCCC"))
    canvas.setLineWidth(0.5)
    canvas.line(MARGIN, line_y, PAGE_WIDTH - MARGIN, line_y)

    draw_header_logo(canvas)

    canvas.setFont(FONT_REGULAR, 8)
    canvas.setFillColor(colors.grey)
    canvas.drawRightString(
        PAGE_WIDTH - MARGIN,
        HEADER_Y,
        "Manual de Politicas de RH  |  v1.1  |  CONFIDENCIAL",
    )
    canvas.drawCentredString(PAGE_WIDTH / 2, FOOTER_Y, f"Pagina {doc.page}")
    canvas.restoreState()


def make_frame(header_zone: float) -> Frame:
    frame_height = PAGE_HEIGHT - header_zone - FOOTER_ZONE
    return Frame(
        MARGIN,
        FOOTER_ZONE,
        FRAME_WIDTH,
        frame_height,
        id="normal",
        showBoundary=0,
        leftPadding=0,
        rightPadding=0,
        topPadding=0,
        bottomPadding=0,
    )


def parse_markdown(content: str, styles, skip_cover_branding: bool = False) -> list:
    story = []
    lines = content.splitlines()
    i = 0
    skipped_branding = False

    while i < len(lines):
        raw = lines[i].rstrip()
        line = raw.strip()

        if skip_cover_branding and not skipped_branding:
            if line.startswith("# HVOGEL") or line.startswith("**Excelência em Engenharia"):
                i += 1
                continue
            if line == "---" and not story:
                i += 1
                continue
            if line.startswith("# MANUAL DE POLÍTICAS"):
                skipped_branding = True

        if not line:
            story.append(Spacer(1, 4))
            i += 1
            continue

        if line == "---":
            story.append(Spacer(1, 10))
            i += 1
            continue

        if line.startswith("# "):
            story.append(Paragraph(inline_md_to_reportlab(line[2:]), styles["title"]))
            i += 1
            continue

        if line.startswith("## "):
            story.append(Paragraph(inline_md_to_reportlab(line[3:]), styles["h1"]))
            i += 1
            continue

        if line.startswith("### "):
            story.append(Paragraph(inline_md_to_reportlab(line[4:]), styles["h2"]))
            i += 1
            continue

        if line.startswith("- ") or line.startswith("* "):
            story.append(
                Paragraph(
                    f"- {inline_md_to_reportlab(line[2:])}",
                    styles["bullet"],
                )
            )
            i += 1
            continue

        if re.match(r"^\d+\.", line):
            story.append(Paragraph(inline_md_to_reportlab(line), styles["bullet"]))
            i += 1
            continue

        paragraph_lines = [line]
        j = i + 1
        while j < len(lines):
            nxt = lines[j].strip()
            if (
                not nxt
                or nxt == "---"
                or nxt.startswith("#")
                or nxt.startswith("- ")
                or nxt.startswith("* ")
                or re.match(r"^\d+\.", nxt)
            ):
                break
            paragraph_lines.append(nxt)
            j += 1

        paragraph = " ".join(paragraph_lines)
        story.append(Paragraph(inline_md_to_reportlab(paragraph), styles["body"]))
        i = j

    return story


def main():
    register_fonts()
    styles = build_styles()
    logo_path, _ = ensure_brand_logo()
    story = [
        make_cover_logo(logo_path),
        Spacer(1, 14),
    ]
    story.extend(parse_markdown(text, styles, skip_cover_branding=True))

    doc = BaseDocTemplate(
        str(pdf_path),
        pagesize=A4,
        leftMargin=MARGIN,
        rightMargin=MARGIN,
        topMargin=HEADER_ZONE,
        bottomMargin=FOOTER_ZONE,
        title="Manual de Politicas de RH - Hvogel",
        author="Hvogel Tecnologia Ltda.",
    )

    doc.addPageTemplates(
        [
            PageTemplate(
                id="first",
                frames=[make_frame(FIRST_PAGE_HEADER_ZONE)],
                onPage=header_footer,
                autoNextPageTemplate="later",
            ),
            PageTemplate(
                id="later",
                frames=[make_frame(HEADER_ZONE)],
                onPage=header_footer_later,
            ),
        ]
    )
    block_count = len(story)
    doc.build(story)
    print(f"PDF gerado: {pdf_path} ({pdf_path.stat().st_size} bytes, {block_count} blocos)")


if __name__ == "__main__":
    main()
