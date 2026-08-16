#!/usr/bin/env python3
"""Draw the heap block texture.

This script is the source of the sprite; the PNG under src/main/resources is its
output and is not edited by hand.

The block has to be seen into, so the middle of the face is translucent and only
the frame is solid. Shading is derived rather than drawn: a pixel with nothing
above-left of it catches the light, a pixel with nothing below-right of it falls
into shadow, and everything else is body colour. The frame is a ring, so that rule
lights its outer edge and shades its inner one without either being described.

No third-party libraries: the PNG is assembled from zlib and struct.

    python tools/make_textures.py
"""

from __future__ import annotations

import pathlib
import struct
import zlib

SIZE = 16
FRAME = 2  # how many pixels deep the metal border runs

ASSETS = pathlib.Path(__file__).resolve().parents[1] / "src/main/resources/assets/acervus/textures"
OUT = ASSETS / "block/heap.png"
FLUID_OUT = ASSETS / "block/fluid_heap.png"
GUI_OUT = ASSETS / "gui/heap.png"

ALL = {(x, y) for x in range(SIZE) for y in range(SIZE)}
METAL = {(x, y) for (x, y) in ALL
         if x < FRAME or y < FRAME or x >= SIZE - FRAME or y >= SIZE - FRAME}
GLASS = ALL - METAL

# A few brighter cells, as the sheen on a pane. Placed rather than computed: a
# highlight is about where the light happens to be, not about the shape.
SHEEN = {(4, 5), (5, 4), (5, 5), (6, 4), (9, 10), (10, 9), (10, 10)}

# (light, body, shadow)
METAL_TONES = ("#8A8F9C", "#5C6270", "#3A3F4A")

GLASS_BODY = "#A8D4E0"
GLASS_SHEEN = "#DFF2F7"
GLASS_ALPHA = 90  # out of 255; enough to read as glass, clear enough to see through


def _rgb(colour: str) -> tuple[int, int, int]:
    value = colour.lstrip("#")
    return (int(value[0:2], 16), int(value[2:4], 16), int(value[4:6], 16))


def _tone(pixel: tuple[int, int], region: set[tuple[int, int]], tones: tuple[str, str, str]) -> str:
    x, y = pixel
    light, body, shadow = tones
    if (x - 1, y - 1) not in region:
        return light
    if (x + 1, y + 1) not in region:
        return shadow
    return body


def _png(pixels: dict[tuple[int, int], tuple[int, int, int, int]]) -> bytes:
    raw = bytearray()
    for y in range(SIZE):
        raw.append(0)  # filter type 0 for the row
        for x in range(SIZE):
            raw.extend(pixels.get((x, y), (0, 0, 0, 0)))

    def chunk(kind: bytes, data: bytes) -> bytes:
        body = kind + data
        return struct.pack(">I", len(data)) + body + struct.pack(">I", zlib.crc32(body))

    header = struct.pack(">IIBBBBB", SIZE, SIZE, 8, 6, 0, 0, 0)  # 8-bit RGBA
    return (
        b"\x89PNG\r\n\x1a\n"
        + chunk(b"IHDR", header)
        + chunk(b"IDAT", zlib.compress(bytes(raw), 9))
        + chunk(b"IEND", b"")
    )


def draw(glass: str = GLASS_BODY, sheen: str = GLASS_SHEEN) -> bytes:
    """The same block in a different glass. The frame is shared on purpose: a fluid
    heap and an item heap are the same machine holding different things, and should
    look like two of a set rather than two inventions."""
    pixels: dict[tuple[int, int], tuple[int, int, int, int]] = {}
    for pixel in METAL:
        pixels[pixel] = _rgb(_tone(pixel, METAL, METAL_TONES)) + (255,)
    for pixel in GLASS:
        pixels[pixel] = _rgb(sheen if pixel in SHEEN else glass) + (GLASS_ALPHA,)
    return _png(pixels)


# --- the screen ------------------------------------------------------------
# A panel in the game's own idiom: flat fill, a light bevel on the top and left,
# a dark one on the bottom and right. Every measurement below is also a constant in
# HeapScreen, and the two have to agree; they are named the same on both sides.

SHEET = 256
PANEL_W, PANEL_H = 176, 166

PANEL_FILL = "#C6C6C6"
PANEL_LIGHT = "#FFFFFF"
PANEL_DARK = "#555555"
BEVEL = 3

WELL = (7, 18, 162, 44)          # x, y, w, h - where the contents are described
WELL_FILL = "#8B8B8B"
WELL_DARK = "#373737"
WELL_LIGHT = "#FFFFFF"

SLOT = 18
HEAP_SLOT = (16, 30)                              # inner top-left of the heap's own slot
INVENTORY_ROWS = ((8, 84), (8, 102), (8, 120))    # inner top-left of each row
HOTBAR = (8, 142)


def _panel(pixels: dict, ox: int, oy: int) -> None:
    for x in range(PANEL_W):
        for y in range(PANEL_H):
            if x < BEVEL or y < BEVEL:
                colour = PANEL_LIGHT
            elif x >= PANEL_W - BEVEL or y >= PANEL_H - BEVEL:
                colour = PANEL_DARK
            else:
                colour = PANEL_FILL
            pixels[(ox + x, oy + y)] = _rgb(colour) + (255,)


def _recess(pixels: dict, x: int, y: int, w: int, h: int) -> None:
    """A sunken area: dark along the top and left, light along the bottom and right."""
    for dx in range(w):
        for dy in range(h):
            if dx == 0 or dy == 0:
                colour = WELL_DARK
            elif dx == w - 1 or dy == h - 1:
                colour = WELL_LIGHT
            else:
                colour = WELL_FILL
            pixels[(x + dx, y + dy)] = _rgb(colour) + (255,)


def draw_screen() -> bytes:
    pixels: dict[tuple[int, int], tuple[int, int, int, int]] = {}
    _panel(pixels, 0, 0)
    _recess(pixels, *WELL)

    # Slot frames are drawn one pixel out from the sixteen the item occupies. The
    # heap's own slot sits inside the well, so it is drawn after it.
    _recess(pixels, HEAP_SLOT[0] - 1, HEAP_SLOT[1] - 1, SLOT, SLOT)
    for row in INVENTORY_ROWS + (HOTBAR,):
        for column in range(9):
            _recess(pixels, row[0] - 1 + column * SLOT, row[1] - 1, SLOT, SLOT)

    raw = bytearray()
    for y in range(SHEET):
        raw.append(0)
        for x in range(SHEET):
            raw.extend(pixels.get((x, y), (0, 0, 0, 0)))

    def chunk(kind: bytes, data: bytes) -> bytes:
        body = kind + data
        return struct.pack(">I", len(data)) + body + struct.pack(">I", zlib.crc32(body))

    header = struct.pack(">IIBBBBB", SHEET, SHEET, 8, 6, 0, 0, 0)
    return (
        b"\x89PNG\r\n\x1a\n"
        + chunk(b"IHDR", header)
        + chunk(b"IDAT", zlib.compress(bytes(raw), 9))
        + chunk(b"IEND", b"")
    )


FLUID_GLASS = "#7FB8C8"
FLUID_SHEEN = "#C8E8F0"


def main() -> None:
    OUT.parent.mkdir(parents=True, exist_ok=True)
    OUT.write_bytes(draw())
    print(f"wrote {OUT}")

    FLUID_OUT.write_bytes(draw(FLUID_GLASS, FLUID_SHEEN))
    print(f"wrote {FLUID_OUT}")

    GUI_OUT.parent.mkdir(parents=True, exist_ok=True)
    GUI_OUT.write_bytes(draw_screen())
    print(f"wrote {GUI_OUT}")


if __name__ == "__main__":
    main()
