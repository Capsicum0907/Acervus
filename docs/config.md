# Config

English | [日本語](config.ja.md)

## Server (`acervus-server.toml`)

Applies to the world. On a server, the server's values are used.

| Setting | Default | Range | |
|---|---|---|---|
| `item.capacity` | 2,000,000,000 | 64 – 9,223,372,036,854,775,807 | Items in one Item Heap |
| `item.absorbsWhenCarried` | `true` | | A carried Item Heap picks up the item it holds |
| `fluid.capacity` | 1,000,000,000,000 | 1,000 – 9,223,372,036,854,775,807 | Millibuckets in one Fluid Heap |
| `energy.capacity` | 1,000,000,000,000 | 1,000 – 9,223,372,036,854,775,807 | FE in one Energy Heap |
| `energy.pushes` | `true` | | An Energy Heap gives energy to the blocks touching it |
| `energy.pushRate` | 1,000,000,000 | 1 – 9,223,372,036,854,775,807 | FE given to each neighbour per tick, at most 2,147,483,647 |
| `chemical.capacity` | 1,000,000,000,000 | 1,000 – 9,223,372,036,854,775,807 | Millibuckets in one Gas Heap |
| `display.showsContents` | `true` | | Draw what a heap holds, and how much, on the block |

## Client (`acervus-client.toml`)

| Setting | Default | |
|---|---|---|
| `screen.barScale` | `LINEAR` | How the bar is drawn: `LINEAR`, `LOG` or `DECADE`. Clicking the bar changes it |
