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
- **Creative heaps** of all four kinds, with no recipe, give what they hold without end. They are in the Acervus tab of the creative inventory.

## Horreum

A rack for nine heaps of any kind, mixed. Pipes and cables connected to it reach every heap inside. Its front lights a lamp for each heap, in that heap's colour.

## Worth knowing

- A dropped heap or Horreum survives fire, lava, explosions and cacti. Like any item, it still disappears after five minutes on the ground.
- An Item Heap takes no heaps, Horreums, shulker boxes, bundles, or anything carrying contents of its own.
- A heap's tooltip shows what it holds and how much. A Horreum's tooltip shows every heap in it.
- Jade and The One Probe show what a heap holds and its real amount.
- If the mod that adds what a heap holds is removed, the heap keeps it until the mod is back.
- With Refined Storage, an External Storage reads the real amount, past 2,147,483,647.
- The proper tool is a diamond pickaxe or better, but any tool breaks one and it always drops.
- Mekanism is needed only for the Gas Heap.
- Every capacity can be changed in `config/acervus-server.toml`.

## Requirements

NeoForge 21.1.x on Minecraft 1.21.1. MIT licensed.

---

# 日本語

Acervus は1種類の物をチェストとは桁違いの量で貯められる収納ブロックを追加します。

## 超容量ストレージ

| ブロック | 入れる物 | 既定の容量 |
|---|---|---|
| 超容量チェスト | 1種類のアイテム | 2,000,000,000 個 |
| 超容量タンク | 1種類の液体 | 1,000,000,000 バケツ |
| 超容量蓄電器 | Forge Energy | 1,000,000,000,000 FE |
| 超容量ガスタンク | Mekanism の化学物質1種類 | 1,000,000,000,000 mB |

- **超容量ストレージは壊しても中身を保ちます。** 拾って別の場所に置けます。
- **画面**には「入」と「出」の2つのスロットがあります。超容量タンク・超容量蓄電器・超容量ガスタンクでは、バケツ・タンク・バッテリーを置いて出し入れします。
- **超容量チェストは同じ物の別の形も受け付けます。** 金属の塊・インゴット・ブロックは1つの超容量チェストに入ります。ほかの Mod の物も対象です。
- **インベントリに入れて持ち歩くと**、超容量チェストは入っている物を拾い集めます。インベントリが満杯でも拾います。
- **錠前のボタン**で今の種類に固定できます。空になっても種類を覚えています。
- **クリエイティブ用**の超容量ストレージが4種類あります。レシピは無く、入っている物をいくらでも出します。クリエイティブのインベントリの Acervus のタブにあります。

## 超容量ラック

どの種類の超容量ストレージでも混ぜて9個まで入れられる棚です。つないだパイプやケーブルは中のすべてに届きます。正面のランプは入っているものごとにその色で点きます。

## 知っておくとよいこと

- 落とした超容量ストレージと超容量ラックは、火・溶岩・爆発・サボテンで消えません。ほかのアイテムと同じく、地面に5分置いたままにすると消えます。
- 超容量チェストには超容量ストレージ・超容量ラック・シュルカーボックス・バンドルと、中身を持っている物は入りません。
- 超容量ストレージのツールチップには入っている物と量が出ます。超容量ラックのツールチップには中のものがすべて出ます。
- Jade と The One Probe で、入っている物と本当の量が見えます。
- 中身を足していた Mod を外しても、超容量ストレージは中身を保ちます。入れ直せば元どおりです。
- Refined Storage の外部ストレージは 2,147,483,647 を超える量もそのまま読めます。
- 向いた道具はダイヤのつるはし以上です。ただしどの道具でも壊せて、必ず落ちます。
- Mekanism が必要なのは超容量ガスタンクだけです。
- 容量はすべて `config/acervus-server.toml` で変えられます。

## 動作環境

Minecraft 1.21.1 の NeoForge 21.1.x 向けです。MIT ライセンスです。
