# `build_publish_preview.sh` 説明書

`publish/*.tex` の変更を著者側で素早く確認するためのPDFプレビューです。編集側の本番組版を再現するものではなく、TeXがコンパイルできることと、数式、コード、図、表、章構造が大きく壊れていないことを確認するためだけに使います。

`publish/*.tex`自体は変更しないのでご安心ください。

## 使い方

Dockerのある環境でリポジトリルートで次を実行します。

```bash
./build_publish_preview.sh docker
```

生成物だけを削除するには次を実行します。

```bash
./build_publish_preview.sh clean
```

## 出力

プレビューPDFは次の固定パスへ生成されます。

```text
build/publish-preview/parser_book-preview.pdf
```

## 検証できること

- `publish/main.tex` と各章がLuaLaTeXでコンパイルできること
- 数式、コードブロック、TikZ図、表、箇条書き、章構造の大まかな表示
- `lstlisting` や `codescreen` などの環境が閉じていること
- ローカルな`\input`、`\include`、bibliography、直接の画像参照が解決できること
- 索引生成、参照、目次を含む複数パスがエラーなしで完了すること
- 生成PDFが空でないこと
