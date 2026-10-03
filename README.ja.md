# Acervus

[English](README.md) | 日本語

1種類の物をチェストとは桁違いの量で貯められる収納ブロックです。

## ブロック

| ブロック | |
|---|---|
| 超容量チェスト | 1種類のアイテムを入れます。既定で 2,000,000,000 個 |
| 超容量タンク | 1種類の液体を入れます。既定で 1,000,000,000 バケツ |
| 超容量蓄電器 | Forge Energy（FE）を入れます。既定で 1,000,000,000,000 FE |
| 超容量ガスタンク | Mekanism の化学物質を1種類入れます。既定で 1,000,000,000,000 mB |
| 超容量ラック | どの種類の超容量ストレージでも9個入る棚です。パイプから中のすべてに届きます |

超容量ストレージは壊しても中身を保ちます。超容量チェストをインベントリに入れて持ち歩くと、入っているのと同じアイテムを拾い集めます。

## 資料

| | |
|---|---|
| [超容量ストレージと超容量ラック](docs/heaps.ja.md) | 各ブロックの働きと使い方 |
| [設定](docs/config.ja.md) | すべての設定項目 |
| [設計メモ](docs/design.ja.md) | ソースについてのメモ（ファイルごと） |

## スクリーンショット

![4種類の超容量ストレージ](branding/gallery/01-card.png)

ほかの画像は [branding/gallery](branding/gallery) にあります。

## 動作環境

| | |
|---|---|
| Minecraft | 1.21.1 |
| NeoForge | 21.1.248 |
| Java | 21 |

任意：[Mekanism](https://www.curseforge.com/minecraft/mc-mods/mekanism) を入れると超容量ガスタンクが使えます。[Refined Storage](https://www.curseforge.com/minecraft/mc-mods/refined-storage) を入れると 2,147,483,647 を超える量もそのまま読めます。

## ビルド

```
run.bat                   # コンパイルして開発用クライアントを起動
gradlew build             # jar を作る
gradlew runGameTestServer # ゲームテストを実行
gradlew runData           # モデル・レシピ・言語ファイルを作り直す
```

`JAVA_HOME` が JDK 21 を指しているか、`java` に `PATH` が通っている必要があります。

## ライセンス

MIT です。[LICENSE](LICENSE) を見てください。
