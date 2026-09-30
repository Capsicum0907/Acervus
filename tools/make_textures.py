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
OUT = ASSETS / "block/item_heap.png"
FLUID_OUT = ASSETS / "block/fluid_heap.png"
ENERGY_SIDE_OUT = ASSETS / "block/energy_heap_side.png"
ENERGY_FRONT_OUT = ASSETS / "block/energy_heap_front_{}.png"
CHEMICAL_OUT = ASSETS / "block/chemical_heap.png"
READOUT_OUT = ASSETS / "gui/readout.png"
RACK_OUT = ASSETS / "block/horreum.png"
RACK_GUI_OUT = ASSETS / "gui/horreum.png"
BOLT_OUT = ASSETS / "gui/energy_icon.png"

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



def draw_casing(lit: int, show_lamps: bool = True) -> bytes:
    pixels: dict[tuple[int, int], tuple[int, int, int, int]] = {}
    for pixel in METAL:
        pixels[pixel] = _rgb(_tone(pixel, METAL, METAL_TONES)) + (255,)
    for pixel in GLASS:
        pixels[pixel] = _rgb(CASING) + (255,)
    if show_lamps:
        inner = SIZE - 2 * FRAME
        used = LAMPS * 2 - 1
        first = FRAME + (inner - used + 1) // 2
        lane = range(SIZE // 2 - LAMP_WIDTH // 2, SIZE // 2 - LAMP_WIDTH // 2 + LAMP_WIDTH)
        for lamp in range(LAMPS):
            y = SIZE - 1 - (first + lamp * 2)
            for x in lane:
                edge = x in (lane[0], lane[-1])
                colour = (LAMP_ON_EDGE if edge else LAMP_ON) if lamp < lit else LAMP_OFF
                pixels[(x, y)] = _rgb(colour) + (255,)
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

WELL_FILL = "#8B8B8B"
WELL_DARK = "#373737"
WELL_LIGHT = "#FFFFFF"

SLOT = 18
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


def _sheet(pixels: dict) -> bytes:
    """A 256x256 sheet, transparent wherever nothing was drawn. The three screens
    share this rather than each carrying its own copy of the PNG writing."""
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


# One frame, three glasses. The colour is the only thing that says which resource a
# heap is for, which is the intent: they are the same machine.
FLUID_GLASS = "#7FB8C8"
FLUID_SHEEN = "#C8E8F0"
CASING = "#4A4F5C"
LAMP_ON = "#F5C85A"
LAMP_ON_EDGE = "#D8A24A"
LAMP_OFF = "#2A2D35"
LAMPS = 6
LAMP_WIDTH = 4
CHEMICAL_GLASS = "#8FCF8A"
CHEMICAL_SHEEN = "#D6F2D2"


# Energy has no item and no sprite of its own, so its tooltip row had a hole where
# every other row has a picture. Drawn rather than described, because the shape is the
# point: nobody needs to be told what a lightning bolt means.
BOLT = """
................
.......###......
......###.......
.....###........
....######......
...######.......
......###.......
.....###........
....###.........
...###..........
..###...........
................
................
................
................
................
"""

BOLT_BODY = "#F5DC9A"
BOLT_EDGE = "#D8A24A"


def draw_bolt() -> bytes:
    """Sixteen by sixteen, transparent but for the bolt. Blitted straight rather than
    stitched onto the block atlas: it belongs to a tooltip, not to a block.

    The drawing is centred by measuring it, not by counting dots in the art above.
    Written out by hand it sat against the top-left corner of its frame while every
    other icon in the row was centred, and the fix for that is not to move the dots -
    it is to stop the position being something anyone has to get right."""
    rows = [line for line in BOLT.strip("\n").split("\n")]
    drawn = {(x, y) for y, line in enumerate(rows) for x, cell in enumerate(line) if cell == "#"}

    left = min(x for (x, _) in drawn)
    right = max(x for (x, _) in drawn)
    top = min(y for (_, y) in drawn)
    bottom = max(y for (_, y) in drawn)
    dx = (SIZE - (right - left + 1)) // 2 - left
    dy = (SIZE - (bottom - top + 1)) // 2 - top
    lit = {(x + dx, y + dy) for (x, y) in drawn}
    pixels: dict[tuple[int, int], tuple[int, int, int, int]] = {}
    for (x, y) in lit:
        # An edge is any lit pixel with an unlit neighbour above or to the left, which
        # gives the bolt a rim without anyone placing one.
        edge = (x - 1, y) not in lit or (x, y - 1) not in lit
        pixels[(x, y)] = _rgb(BOLT_EDGE if edge else BOLT_BODY) + (255,)
    return _png(pixels)


def draw_rack() -> bytes:
    """The rack block: the same frame as a heap, filled in rather than glazed, with a
    grid of niches for the heaps it holds. It is the one block of the set you cannot
    see into, because what is inside it is heaps and not contents."""
    pixels: dict[tuple[int, int], tuple[int, int, int, int]] = {}
    for pixel in ALL:
        pixels[pixel] = _rgb(_tone(pixel, METAL, METAL_TONES)) + (255,)
    # Four niches, two by two, standing for the slots inside. Dark on the top and left
    # so they read as holes rather than as studs.
    for ox in (3, 9):
        for oy in (3, 9):
            for dx in range(4):
                for dy in range(4):
                    dark = dx == 0 or dy == 0
                    colour = RACK_NICHE_DARK if dark else RACK_NICHE
                    pixels[(ox + dx, oy + dy)] = _rgb(colour) + (255,)
    return _png(pixels)


RACK_NICHE = "#3A3F4A"
RACK_NICHE_DARK = "#23262E"

# Where the twelve heap slots sit in the rack screen: six across, two down.
RACK_SLOTS = (26, 22)
RACK_COLUMNS = 6
RACK_ROWS = 2


def draw_rack_screen() -> bytes:
    """Twelve slots and the player's inventory, and nothing else. Each heap says what
    it holds on its own tooltip, so a readout here would be a second copy of it."""
    pixels: dict[tuple[int, int], tuple[int, int, int, int]] = {}
    _panel(pixels, 0, 0)
    for row in range(RACK_ROWS):
        for column in range(RACK_COLUMNS):
            _recess(pixels,
                    RACK_SLOTS[0] - 1 + column * SLOT,
                    RACK_SLOTS[1] - 1 + row * SLOT,
                    SLOT, SLOT)
    for row in INVENTORY_ROWS + (HOTBAR,):
        for column in range(9):
            _recess(pixels, row[0] - 1 + column * SLOT, row[1] - 1, SLOT, SLOT)
    return _sheet(pixels)


IN_SLOT = (8, 38)
OUT_SLOT = (150, 38)
BAR = (52, 41, 94, 6)


def draw_readout() -> bytes:
    pixels: dict[tuple[int, int], tuple[int, int, int, int]] = {}
    _panel(pixels, 0, 0)
    _recess(pixels, *BAR)
    for box in (IN_SLOT, OUT_SLOT):
        _recess(pixels, box[0] - 1, box[1] - 1, SLOT, SLOT)
    for row in INVENTORY_ROWS + (HOTBAR,):
        for column in range(9):
            _recess(pixels, row[0] - 1 + column * SLOT, row[1] - 1, SLOT, SLOT)

    return _sheet(pixels)


def main() -> None:
    OUT.parent.mkdir(parents=True, exist_ok=True)
    OUT.write_bytes(draw())
    print(f"wrote {OUT}")

    FLUID_OUT.write_bytes(draw(FLUID_GLASS, FLUID_SHEEN))
    print(f"wrote {FLUID_OUT}")

    ENERGY_SIDE_OUT.write_bytes(draw_casing(0, show_lamps=False))
    print(f"wrote {ENERGY_SIDE_OUT}")
    for lit in range(LAMPS + 1):
        out = pathlib.Path(str(ENERGY_FRONT_OUT).format(lit))
        out.write_bytes(draw_casing(lit))
        print(f"wrote {out}")

    CHEMICAL_OUT.write_bytes(draw(CHEMICAL_GLASS, CHEMICAL_SHEEN))
    print(f"wrote {CHEMICAL_OUT}")


    READOUT_OUT.write_bytes(draw_readout())
    print(f"wrote {READOUT_OUT}")

    RACK_OUT.write_bytes(draw_rack())
    print(f"wrote {RACK_OUT}")

    RACK_GUI_OUT.write_bytes(draw_rack_screen())
    print(f"wrote {RACK_GUI_OUT}")

    BOLT_OUT.write_bytes(draw_bolt())
    print(f"wrote {BOLT_OUT}")


if __name__ == "__main__":
    main()
