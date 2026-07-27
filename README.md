# 構文解析のしくみ

── 電卓から言語処理系まで自分で書く

ZEN大学出版会で出版予定の構文解析の技術書です。LuaLaTeX形式で執筆・組版します。

# リポジトリの構成

```
./
  README.md         // このファイル
  build_pdf.sh      // PDF生成スクリプト
  tex/              // LuaLaTeX原稿
    main.tex        // 組版用メインファイル
    preamble.tex    // パッケージ・フォント・紙面設定
    references.bib  // 参考文献データ
    contents/       // 個別章のTeXファイル
      chapter1.tex  // 第1章：構文解析の世界へようこそ
      chapter2.tex  // 第2章：構文解析の基礎
      chapter3.tex  // 第3章：JSONの構文解析
      chapter4.tex  // 第4章：文脈自由文法の世界
      chapter5.tex  // 第5章：構文解析アルゴリズム・処理系統
      chapter6.tex  // 第6章：構文解析器生成系の世界
      chapter7.tex  // 第7章：現実の構文解析
      chapter8.tex  // 第8章：おわりに
  docker/luatex/    // Dockerビルド環境
  build/            // ビルド出力ディレクトリ
  code/             // サンプルコード
    chapter3/       // JSON パーサー実装
    chapter5/       // SLR(1) パーサー実装
    chapter6/       // パーサージェネレータの例
  .gitignore        // gitの管理対象から除外するパターン
```

## 必要な環境

- [LuaLaTeX](https://www.luatex.org/) (TeX Live 2022以上推奨)
- BibTeX
- upmendex (日本語索引用)
- 日本語フォント (Noto CJK フォント推奨)

Dockerを使う場合、ローカルにTeX Liveや日本語フォントを直接インストールする必要はありません。

### Ubuntu/Debianでのインストール

[こちら](https://qiita.com/YuH25/items/76f056bf691855e420e0)を参考に、以下のコマンドで必要なパッケージをインストールできます。

```bash
sudo apt update

# TeX Live (LuaLaTeX含む)
sudo apt install -y texlive-lang-japanese
sudo apt install -y texlive-luatex
sudo apt install -y texlive-pictures texlive-latex-extra

# 日本語フォント
sudo apt install -y fonts-noto-cjk

# rsvg-convert  # SVG画像の変換に必要
sudo apt install -y librsvg2-dev
```

### macOSでのインストール

```bash
# Homebrew使用
brew install --cask mactex

# 日本語フォント
brew install --cask font-noto-sans-cjk-jp
```

## 書籍のビルド方法

### PDFビルド

```bash
./build_pdf.sh
# build/parser_book.pdf が生成されます
```

### Dockerビルド

```bash
./build_pdf.sh docker
# Docker内のLuaLaTeXで build/parser_book.pdf が生成されます
```

## 執筆ワークフロー

```bash
# 章を編集（tex/contents/*.tex）
vim tex/contents/chapter1.tex

# PDFを生成
./build_pdf.sh
```

## 索引

索引はLaTeX標準の `makeidx` で `build/parser_book.idx` を出力し、
`upmendex` で日本語対応の `build/parser_book.ind` に変換します。
`./build_pdf.sh` はこの手順を自動で実行します。

本文に索引用語を追加する場合は、該当箇所の近くに以下のように書きます。

```tex
\index{こうぶんかいせき@構文解析}
\index{PEG}
\index{ぱーさーこんびねーた@パーサーコンビネータ}
```

日本語の用語は `読み@表示名` の形にすると、索引の並びが安定します。
