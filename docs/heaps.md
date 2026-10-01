# Heaps and the Horreum

English | [日本語](heaps.ja.md)

## Recipes

| Block | Ingredients |
|---|---|
| Item Heap | 4 iron blocks, 2 nether stars, 2 netherite ingots, 1 chest |
| Fluid Heap | 4 iron blocks, 2 nether stars, 2 netherite ingots, 1 cauldron |
| Energy Heap | 4 iron blocks, 2 nether stars, 2 netherite ingots, 1 redstone block |
| Gas Heap | 4 iron blocks, 2 nether stars, 2 netherite ingots, 1 block of osmium (Mekanism only) |
| Horreum | 4 netherite ingots, 4 nether stars, 1 empty heap of any kind |

Only an empty, unlocked heap can be used to make a Horreum.

## The screen

Right-click a heap to open it. The screen shows what the heap holds, a bar, the amount
against the capacity, and two slots: **In** on the left and **Out** on the right.

| | Item Heap | Fluid, Energy and Gas Heap |
|---|---|---|
| **In** | put items in | put a container in to empty it into the heap |
| **Out** | take items out | put a container in to fill it from the heap |

In a Fluid, Energy or Gas Heap, a container stays in its slot until it is taken out,
and shift-clicking a container from the inventory puts an empty one in Out and any
other in In.

Point at a shortened number to see all of it.

**Click the bar** to change how it is drawn. The choice is kept per player.

| Scale | The bar is full at |
|---|---|
| Linear (default) | the capacity |
| Logarithmic | the capacity, with one tick for every power of ten |
| Within the decade | the next power of ten |

**The lock** at the top right keeps a heap to the kind it holds. Red is Locked, grey is
Free. A locked heap keeps its kind when it runs empty and takes nothing else. Only a
heap with something in it can be locked. Energy Heaps have no lock.

## Item Heap

Out in the world, without opening the screen:

| | |
|---|---|
| Right-click holding what the heap holds | puts that stack in |
| Sneak + right-click holding what the heap holds | puts in all of it from the inventory |

**Other forms of the same item.** Where 9 (or 4) of one item craft into 1 of another
and back again — gold nuggets, ingots and blocks, slime balls and slime blocks — a heap
takes every form. The amount is shown in the form that went in first, and that is the
form pipes and hoppers take out. Mods' forms are included.

Click the word **Out** to switch the Out slot to the smallest form. Anything less than
one of the first form, such as 4 nuggets in a heap of ingots, comes out this way.

**Carrying one.** An Item Heap in the inventory picks up what it holds:

- what is walked over
- what arrives in the inventory any other way, a moment later

Nothing comes out of a carried heap. What is in the hand and worn armour are left
alone, and nothing is picked up while a container screen is open. Several heaps in one
stack pick up nothing.

Sneak + right-click the air to open the heap in your hand. An empty one can be given
its kind there. Turn picking up off with `absorbsWhenCarried`.

## Fluid, Energy and Gas Heap

The glass of a Fluid or Gas Heap is filled with what it holds.

An Energy Heap faces whoever placed it. Its front has 12 lamps, which share out the
powers of ten up to the capacity. With the default capacity, one more lights for every
power of ten: 10 FE lights the first, 1,000,000 FE lights 6.
An Energy Heap also gives its energy to the blocks touching it, up to 1,000,000,000 FE
per tick to each.

Sneak + right-click the air to open a heap in your hand. Its In slot still empties a
container into it. Its Out slot takes nothing while it is carried.

## Horreum

A rack with 9 slots for heaps of any kind, mixed. Pipes and cables connected to it
reach every heap inside: items, fluids, energy and chemicals.

A placed Horreum faces whoever placed it. Its front has a lamp for each slot, in the
same 3 by 3 order as its screen, lit in the colour of the heap in it.

| Heap | Colour |
|---|---|
| Item | grey |
| Fluid | blue |
| Energy | amber |
| Gas | green |

Breaking a Horreum keeps its heaps inside it. Sneak + right-click the air to open one
you are carrying; its heaps can be moved, but nothing can be taken out of them.

## Tooltips

A heap's tooltip shows what it holds and how much. A Horreum's tooltip shows a row for
each heap in it.

## When a mod is removed

If the mod that adds what a heap holds is removed, the heap keeps it. It shows the
missing-texture checkerboard and the id it held, and nothing goes in or out until the
mod is back.

Without Mekanism a Gas Heap stays in the world the same way, and cannot be crafted.

## Refined Storage

Point an External Storage at a heap or a Horreum. The network sees the real amount,
past 2,147,483,647 items or millibuckets.
