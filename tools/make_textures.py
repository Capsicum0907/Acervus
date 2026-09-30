#!/usr/bin/env python3

from __future__ import annotations

import pathlib
import struct
import zlib

SIZE = 16
FRAME = 2

ASSETS = pathlib.Path(__file__).resolve().parents[1] / "src/main/resources/assets/acervus/textures"
OUT = ASSETS / "block/item_heap.png"
FLUID_OUT = ASSETS / "block/fluid_heap.png"
ENERGY_SIDE_OUT = ASSETS / "block/energy_heap_side.png"
ENERGY_FRONT_OUT = ASSETS / "block/energy_heap_front_{}.png"
CHEMICAL_OUT = ASSETS / "block/chemical_heap.png"
READOUT_OUT = ASSETS / "gui/readout.png"
RACK_GUI_OUT = ASSETS / "gui/horreum.png"
RACK_FRONT_OUT = ASSETS / "block/horreum_front.png"
RACK_LAMP_OUT = ASSETS / "block/horreum_lamp.png"
BOLT_OUT = ASSETS / "gui/energy_icon.png"

ALL = {(x, y) for x in range(SIZE) for y in range(SIZE)}
METAL = {(x, y) for (x, y) in ALL
         if x < FRAME or y < FRAME or x >= SIZE - FRAME or y >= SIZE - FRAME}
GLASS = ALL - METAL

SHEEN = {(4, 5), (5, 4), (5, 5), (6, 4), (9, 10), (10, 9), (10, 10)}

METAL_TONES = ("#8A8F9C", "#5C6270", "#3A3F4A")

GLASS_BODY = "#D4D8DC"
GLASS_SHEEN = "#F2F4F6"
GLASS_ALPHA = 90


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


def _png(pixels: dict[tuple[int, int], tuple[int, int, int, int]], size: int = SIZE) -> bytes:
    raw = bytearray()
    for y in range(size):
        raw.append(0)
        for x in range(size):
            raw.extend(pixels.get((x, y), (0, 0, 0, 0)))

    def chunk(kind: bytes, data: bytes) -> bytes:
        body = kind + data
        return struct.pack(">I", len(data)) + body + struct.pack(">I", zlib.crc32(body))

    header = struct.pack(">IIBBBBB", size, size, 8, 6, 0, 0, 0)
    return (
        b"\x89PNG\r\n\x1a\n"
        + chunk(b"IHDR", header)
        + chunk(b"IDAT", zlib.compress(bytes(raw), 9))
        + chunk(b"IEND", b"")
    )


def draw(glass: str = GLASS_BODY, sheen: str = GLASS_SHEEN) -> bytes:
    pixels: dict[tuple[int, int], tuple[int, int, int, int]] = {}
    for pixel in METAL:
        pixels[pixel] = _rgb(_tone(pixel, METAL, METAL_TONES)) + (255,)
    for pixel in GLASS:
        pixels[pixel] = _rgb(sheen if pixel in SHEEN else glass) + (GLASS_ALPHA,)
    return _png(pixels)


def draw_casing(lit: int, show_lamps: bool = True) -> bytes:
    scale = FRONT_SCALE if show_lamps else 1
    pixels: dict[tuple[int, int], tuple[int, int, int, int]] = {}
    for (x, y) in METAL | GLASS:
        colour = _tone((x, y), METAL, METAL_TONES) if (x, y) in METAL else CASING
        for dx in range(scale):
            for dy in range(scale):
                pixels[(x * scale + dx, y * scale + dy)] = _rgb(colour) + (255,)
    if show_lamps:
        inner = (SIZE - 2 * FRAME) * scale
        height = (inner - (LAMPS - 1) * LAMP_GAP) // LAMPS
        used = LAMPS * height + (LAMPS - 1) * LAMP_GAP
        bottom = FRAME * scale + (inner - used) // 2 + used - 1
        left = (SIZE // 2 - LAMP_WIDTH // 2) * scale
        right = left + LAMP_WIDTH * scale
        for lamp in range(LAMPS):
            for row in range(height):
                y = bottom - lamp * (height + LAMP_GAP) - row
                for x in range(left, right):
                    edge = x < left + scale or x >= right - scale
                    colour = (LAMP_ON_EDGE if edge else LAMP_ON) if lamp < lit else LAMP_OFF
                    pixels[(x, y)] = _rgb(colour) + (255,)
    return _png(pixels, SIZE * scale)


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
INVENTORY_ROWS = ((8, 84), (8, 102), (8, 120))
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


FLUID_GLASS = "#6A9CE0"
FLUID_SHEEN = "#C4D8F4"
CASING = "#4A4F5C"
LAMP_ON = "#F5C85A"
LAMP_ON_EDGE = "#D8A24A"
LAMP_OFF = "#2A2D35"
LAMPS = 12
LAMP_WIDTH = 4
LAMP_GAP = 1
FRONT_SCALE = 4
RACK_LAMPS_ACROSS = 3
RACK_LAMP = 12
RACK_LAMP_GAP = 4
RACK_LAMP_BODY = "#FFFFFF"
RACK_LAMP_EDGE = "#B4B4B4"
CHEMICAL_GLASS = "#8FCF8A"
CHEMICAL_SHEEN = "#D6F2D2"


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
        edge = (x - 1, y) not in lit or (x, y - 1) not in lit
        pixels[(x, y)] = _rgb(BOLT_EDGE if edge else BOLT_BODY) + (255,)
    return _png(pixels)


def rack_lamp_origin(column: int, row: int) -> tuple[int, int]:
    used = RACK_LAMPS_ACROSS * RACK_LAMP + (RACK_LAMPS_ACROSS - 1) * RACK_LAMP_GAP
    start = FRAME * FRONT_SCALE + ((SIZE - 2 * FRAME) * FRONT_SCALE - used) // 2
    step = RACK_LAMP + RACK_LAMP_GAP
    return start + column * step, start + row * step


def draw_rack_front() -> bytes:
    pixels: dict[tuple[int, int], tuple[int, int, int, int]] = {}
    for (x, y) in METAL | GLASS:
        colour = _tone((x, y), METAL, METAL_TONES) if (x, y) in METAL else CASING
        for dx in range(FRONT_SCALE):
            for dy in range(FRONT_SCALE):
                pixels[(x * FRONT_SCALE + dx, y * FRONT_SCALE + dy)] = _rgb(colour) + (255,)
    for row in range(RACK_LAMPS_ACROSS):
        for column in range(RACK_LAMPS_ACROSS):
            left, top = rack_lamp_origin(column, row)
            for dx in range(RACK_LAMP):
                for dy in range(RACK_LAMP):
                    pixels[(left + dx, top + dy)] = _rgb(LAMP_OFF) + (255,)
    return _png(pixels, SIZE * FRONT_SCALE)


def draw_rack_lamp() -> bytes:
    pixels: dict[tuple[int, int], tuple[int, int, int, int]] = {}
    for x in range(SIZE):
        for y in range(SIZE):
            edge = x < FRONT_SCALE // 2 or y < FRONT_SCALE // 2 or x >= SIZE - FRONT_SCALE // 2 or y >= SIZE - FRONT_SCALE // 2
            pixels[(x, y)] = _rgb(RACK_LAMP_EDGE if edge else RACK_LAMP_BODY) + (255,)
    return _png(pixels)

RACK_SLOTS = (62, 18)
RACK_COLUMNS = 3
RACK_ROWS = 3


def draw_rack_screen() -> bytes:
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


    RACK_GUI_OUT.write_bytes(draw_rack_screen())
    print(f"wrote {RACK_GUI_OUT}")

    RACK_FRONT_OUT.write_bytes(draw_rack_front())
    print(f"wrote {RACK_FRONT_OUT}")

    RACK_LAMP_OUT.write_bytes(draw_rack_lamp())
    print(f"wrote {RACK_LAMP_OUT}")

    BOLT_OUT.write_bytes(draw_bolt())
    print(f"wrote {BOLT_OUT}")


if __name__ == "__main__":
    main()
