Acervus adds storage blocks that each hold one kind of thing, in amounts far beyond a chest.

## Heaps

| Block | Holds | Default capacity |
|---|---|---|
| Item Heap | one kind of item | 2,000,000,000 |
| Fluid Heap | one fluid | 1,000,000,000 buckets |
| Energy Heap | Forge Energy | 1,000,000,000,000 FE |
| Gas Heap | one Mekanism chemical | 1,000,000,000,000 mB |

- **A heap keeps its contents when it is broken.** Pick it up and place it somewhere else.
- **The screen** has an In slot and an Out slot. In a Fluid, Energy or Gas Heap they take buckets, tanks and batteries.
- **An Item Heap takes every form of what it holds**: nuggets, ingots and blocks of a metal go into one heap. Mods' forms are included.
- **Carried in the inventory**, an Item Heap picks up the item it holds, even when the inventory is full.
- **The lock** keeps a heap to what it holds, even when it runs empty.

## Horreum

A rack for nine heaps of any kind, mixed. Pipes and cables connected to it reach every heap inside. Its front lights a lamp for each heap, in that heap's colour.

## Worth knowing

- A heap's tooltip shows what it holds and how much. A Horreum's tooltip shows every heap in it.
- If the mod that adds what a heap holds is removed, the heap keeps it until the mod is back.
- With Refined Storage, an External Storage reads the real amount, past 2,147,483,647.
- Mekanism is needed only for the Gas Heap.
- Every capacity can be changed in `config/acervus-server.toml`.

## Requirements

NeoForge 21.1.x on Minecraft 1.21.1. MIT licensed.

---

# 日本語

Acervus は1種類の物をチェストとは桁違いの量で貯められる収納ブロックを追加します。

## Heap

| ブロック | 入れる物 | 既定の容量 |
|---|---|---|
| Item Heap | 1種類のアイテム | 2,000,000,000 個 |
| Fluid Heap | 1種類の液体 | 1,000,000,000 バケツ |
| Energy Heap | Forge Energy | 1,000,000,000,000 FE |
| Gas Heap | Mekanism の化学物質1種類 | 1,000,000,000,000 mB |

- **Heap は壊しても中身を保ちます。** 拾って別の場所に置けます。
- **画面**には In と Out の2つのスロットがあります。Fluid・Energy・Gas Heap ではバケツ・タンク・バッテリーを置いて出し入れします。
- **Item Heap は同じ物の別の形も受け付けます。** 金属の塊・インゴット・ブロックは1つの Heap に入ります。ほかの Mod の物も対象です。
- **インベントリに入れて持ち歩くと**、Item Heap は入っている物を拾い集めます。インベントリが満杯でも拾います。
- **錠前のボタン**で、Heap を今の種類に固定できます。空になっても種類を覚えています。

## Horreum

どの種類の Heap でも混ぜて9個まで入れられる棚です。つないだパイプやケーブルは中のすべての Heap に届きます。正面のランプは入っている Heap ごとにその色で点きます。

## 知っておくとよいこと

- Heap のツールチップには入っている物と量が出ます。Horreum のツールチップには中の Heap がすべて出ます。
- Heap の中身を足していた Mod を外しても Heap は中身を保ちます。入れ直せば元どおりです。
- Refined Storage の外部ストレージは 2,147,483,647 を超える量もそのまま読めます。
- Mekanism が必要なのは Gas Heap だけです。
- 容量はすべて `config/acervus-server.toml` で変えられます。

## 動作環境

Minecraft 1.21.1 の NeoForge 21.1.x 向けです。MIT ライセンスです。
