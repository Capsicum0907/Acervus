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

OUT = pathlib.Path(__file__).resolve().parents[1] / "src/main/resources/assets/acervus/textures/block/heap.png"

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


def draw() -> bytes:
    pixels: dict[tuple[int, int], tuple[int, int, int, int]] = {}
    for pixel in METAL:
        pixels[pixel] = _rgb(_tone(pixel, METAL, METAL_TONES)) + (255,)
    for pixel in GLASS:
        colour = GLASS_SHEEN if pixel in SHEEN else GLASS_BODY
        pixels[pixel] = _rgb(colour) + (GLASS_ALPHA,)
    return _png(pixels)


def main() -> None:
    OUT.parent.mkdir(parents=True, exist_ok=True)
    OUT.write_bytes(draw())
    print(f"wrote {OUT}")


if __name__ == "__main__":
    main()
