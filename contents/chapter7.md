
# 現実の構文解析

ここまでで、LL法やLR法、Packrat Parsingといった、これまでに知られているメジャーな構文解析アルゴリズムを一通り取り上げてきました。これらの構文解析アルゴリズムは概ね文脈自由言語あるいはそのサブセットを取り扱うことができ、一般的なプログラミング言語の構文解析を行うのに必要十分な能力を持っているように思えます。

しかし、構文解析を専門としている人や実用的な構文解析器を書いている人は直感的に理解していることなのですが、実のところ、既存の構文解析アルゴリズムだけではうまく取り扱えない類の構文があります。一言でいうと、それらの構文は文脈自由言語から逸脱しているために、文脈自由言語を取り扱う既存の手法だけではうまくいかないのです。

このような、既存の構文解析アルゴリズムだけでは扱えない要素は多数あります。たとえば、Cのtypedefはその典型ですし、RubyやPerlのヒアドキュメントと呼ばれる構文もそうです。他には、Scalaのプレースホルダ構文やC++のテンプレート、Pythonのインデント文法など、文脈自由言語を逸脱しているがゆえに人間が特別に配慮しなければいけない構文は多く見かけられます。

また、これまでの章では、主に構文解析を行う手法を取り扱っていましたが、現実問題としては抽象構文木をうまく作る方法やエラーメッセージを適切に出す方法も重要になってきます。

この章では、巷の書籍ではあまり扱われない、しかし現実の構文解析では対処しなくてはならない構文や問題について取り上げます。皆さんが何かしらの構文解析器を作るとき、やはり理想どおりにはいかないことが多いと思います。そのような現実の構文解析の**泥臭さ**に遭遇する読者の方々の助けになれば幸いです。

## トークンが「文脈自由」なケース

最近の多くの言語は文字列補間(String Interpolation)と呼ばれる機能を持っています。

たとえば、Rubyでは以下の文字列を評価すると、`"x + y = x + y"`ではなく`"x + y = 3"`になります。

```ruby
x = 1
y = 2
"x + y = #{x + y}" # "x + y = 3"
```

`#{`と`}`で囲まれた範囲をRubyの式として評価した結果を文字列として埋め込んでくれるわけです。

Scalaでも同じことを次のように書くことができます。

```scala
val x = 1; val y = 2
s"x + y = ${x + y}" // "x + y = 3"
```

Swiftだと次のようになります。

```swift
let x = 1
let y = 2
"x + y = \(x + y)"
```

同様の機能はKotlin、Python、JavaScript、TypeScriptなど様々な言語に採用されています。比較的新しい言語や、既存言語の新機能として採用するのが普通になった機能と言えるでしょう。

文字列補間は便利な機能ですが、構文解析という観点からは少々やっかいな存在です。文字列リテラルは従来、字句解析で扱うトークンでした。そして、字句解析では処理を単純化するためにトークンを正規言語の範囲に収まるようにするのがベストプラクティスでした。字句解析と構文解析を分離し、かつ、字句解析を可能な限り単純化するという観点で言えばある意味当然とも言えますが、文字列補間は正規表現で表現できたもの（正規言語に収まるもの）を文脈自由文法で取り扱わなければいけない存在にしてしまいました。

たとえば、Rubyでは以下のように`#{}`の中にさらに文字列リテラルを書くことができ、その中には`#{}`を……といった具合に無限にネストできるのです。これまでの章を振り返ればわかるように、これはDyck言語的な構造があり、明らかに正規言語では扱えず、文脈自由言語の能力が必要なものです。

```ruby
x = 1
y = 2
"expr1 (#{"expr2 (#{x + y})"})" # "expr1 (expr2 (3))"
```

しかし、従来の手法では文字列リテラルはトークンとして扱わなければいけないため、各言語処理系の実装者はad hocな形で構文解析器に手を加えています。たとえば、Rubyの構文解析器はBisonを使って書かれていますが、字句解析器に状態を持たせることでこの問題に対処しています。文字列リテラル内に`#{`が出現したら状態を式モードに切り替えて、その中で文字列リテラルがあらわれたら文字列モードに切り替えるといった具合です。

一方、PEGでは字句解析と構文解析が分離されていないため、特別な工夫をすることなく文字列補間を実装することができます。以下はRubyの文字列補間と同じようなものをPEGで記述する例です。

```text
string <- "\"" ("#{" expression "}" / !"\"" .)* "\""
expression <- 式の定義
```

文字列補間を含む文字列リテラルは分解可能という意味で厳密な意味ではトークンと言えないわけですが、PEGは字句解析を分離しないおかげで文字列リテラルをことさら特別扱いする必要がないわけです。

PEGの利用例が近年増えてきているのは、言語に対してこのようにアドホックに構文を追加したいというニーズがあるためではないかと筆者は考えています。

```{=latex}
\begin{center}
\begin{tikzpicture}[
  node distance=3cm,
  every node/.style={font=\small},
  % 状態ノードのスタイル
  state/.style={
    circle,
    draw,
    minimum size=1.8cm,
    font=\footnotesize
  },
  % 初期状態のスタイル
  initial state/.style={
    state,
    fill=green!10
  },
  % 遷移ラベルのスタイル
  transition/.style={
    font=\footnotesize\ttfamily,
    auto
  }
]
  
  % 状態ノード
  \node[initial state] (string) {文字列\textbackslash モード};
  \node[state, right=of string, fill=orange!10] (expr) {式\textbackslash モード};
  
  % 初期状態への矢印
  \draw[->, thick] ([xshift=-1cm]string.west) -- (string);
  
  % 状態遷移
  \draw[->, thick, bend left=25] (string) to node[transition, above] {\#\{} (expr);
  \draw[->, thick, bend left=25] (expr) to node[transition, below] {\}} (string);
  
  % 自己ループ
  \draw[->, thick] (string) to[loop above] node[transition] {通常文字} (string);
  \draw[->, thick] (expr) to[loop above] node[transition] {式トークン} (expr);
  
  % 例の説明
  \node[below=2.5cm of string, text width=10cm, align=center] {
    \footnotesize 例: \texttt{"abc\#\{1+2\}def"} の解析\\[0.2em]
    \begin{tabular}{cl}
      1. & 文字列モード: \texttt{"abc"} を消費 \\
      2. & \texttt{\#\{} を認識 → 式モードへ遷移 \\
      3. & 式モード: \texttt{1+2} を式として解析 \\
      4. & \texttt{\}} を認識 → 文字列モードへ復帰 \\
      5. & 文字列モード: \texttt{def"} を消費
    \end{tabular}
  };
  
  % 図のラベル
  \node[below=5cm of string, font=\footnotesize] {図7.1: 文字列補間の解析状態遷移};
\end{tikzpicture}
\end{center}
```

## インデント文法

Pythonではインデントによってプログラムの構造を表現します。たとえば、次のPythonプログラムを考えます。

```python
class Point:
  def __init__(self, x, y):
    self.x = x
    self.y = y
```

このPythonプログラムは次のような抽象構文木に変換されると考えられます。

```{=latex}
\begin{center}
\begin{tikzpicture}[
  node distance=1.2cm and 1.5cm,
  every node/.style={font=\small},
  % ノードスタイル定義
  astnode/.style={
    rectangle, 
    draw, 
    rounded corners=3pt, 
    minimum width=2.2cm, 
    minimum height=0.7cm,
    font=\ttfamily
  },
  classnode/.style={astnode, fill=blue!15},
  defnode/.style={astnode, fill=orange!15},
  namenode/.style={astnode, fill=green!15, minimum width=3cm},
  argsnode/.style={astnode, fill=purple!15},
  bodynode/.style={astnode, fill=red!15},
  paramnode/.style={astnode, fill=gray!10, minimum width=1.2cm, minimum height=0.5cm},
  stmtnode/.style={astnode, fill=yellow!10, minimum width=2.8cm, minimum height=0.5cm},
  edge/.style={->, >=stealth, thick}
]
  
  % ルートノード
  \node[classnode] (class) {class};
  
  % 第2レベル
  \node[namenode, below left=of class] (classname) {name: Point};
  \node[defnode, below right=of class] (def) {def};
  
  % 第3レベル（def の子ノード）
  \node[namenode, below=of def] (funcname) {name: \_\_init\_\_};
  \node[argsnode, left=of funcname] (args) {arguments};
  \node[bodynode, right=of funcname] (body) {body};
  
  % 第4レベル（パラメータ）
  \node[paramnode, below=0.8cm of args, xshift=-1.2cm] (self) {self};
  \node[paramnode, below=0.8cm of args] (x) {x};
  \node[paramnode, below=0.8cm of args, xshift=1.2cm] (y) {y};
  
  % 第4レベル（ステートメント）
  \node[stmtnode, below=0.8cm of body, xshift=-1.5cm] (stmt1) {self.x = x};
  \node[stmtnode, below=0.8cm of body, xshift=1.5cm] (stmt2) {self.y = y};
  
  % エッジの描画
  \draw[edge] (class) -- (classname);
  \draw[edge] (class) -- (def);
  \draw[edge] (def) -- (funcname);
  \draw[edge] (def) -- (args);
  \draw[edge] (def) -- (body);
  \draw[edge] (args) -- (self);
  \draw[edge] (args) -- (x);
  \draw[edge] (args) -- (y);
  \draw[edge] (body) -- (stmt1);
  \draw[edge] (body) -- (stmt2);
\end{tikzpicture}
\end{center}
```

インデントによってプログラムの構造を表現するというアイデアは秀逸ですが、インデントによる構造の表現は明らかに文脈自由言語を超えます。インデントによってブロックや文法の構造を表現することは、HaskellやScala 3、YAMLなどでも採用されており、広く使われているアイデアです。

Pythonでは字句解析のときにインデントを`<INDENT>`（インデントレベル増加）、インデントを「外す」のを`<DEDENT>`（インデントレベル減少）という特別なトークンに変換します。これにより、構文解析器自体はこれらのトークンをブロックの開始と終了のように扱うことができ、文脈自由文法の範囲で処理しやすくなります。

例えば、先の `Point` クラスの定義は、字句解析後には（簡略化すると）以下のようなトークン列として扱われるイメージです。

```
<CLASS> <NAME:Point> <COLON> <NEWLINE> 
  <INDENT>
    <DEF> <NAME:__init__> 
    <LPAREN> <NAME:self> <COMMA> <NAME:x> <COMMA> <NAME:y> <RPAREN> 
    <COLON> <NEWLINE>
    <INDENT>
      <NAME:self> <DOT> <NAME:x> <ASSIGN> <NAME:x> <NEWLINE>
      <NAME:self> <DOT> <NAME:y> <ASSIGN> <NAME:y> <NEWLINE>
    <DEDENT>
<DEDENT>
```

（`<NAME:A>`は識別子`A`、`<COLON>`はコロン、`<NEWLINE>`は改行、`<ASSIGN>`は代入演算子を表すトークンとします。実際にはさらに詳細なトークン分割が行われます）。このように、インデント/デデントトークンがブロック構造を示すため、構文解析器は括弧の対応付けと同様の形で処理できます。

しかし、よくよく考えればわかるのですが、`<INDENT>`トークンと`<DEDENT>`トークンを切り出す時点で、文脈自由ではありません。つまり、字句解析時に`<INDENT>`と`<DEDENT>`トークンを切り出すために特殊な処理をしていることになります。`<DEDENT>`トークンは`<INDENT>`トークンとスペースの数が対応していなければいけないため、切り出すためには正規表現でも文脈自由文法でも手に余ります。

```{=latex}
\begin{center}
\begin{tikzpicture}[
  node distance=0.3cm,
  every node/.style={font=\footnotesize},
  % コードラインのスタイル
  codeline/.style={
    rectangle,
    anchor=west,
    font=\ttfamily\footnotesize
  },
  % スタック表示のスタイル
  stacknode/.style={
    rectangle,
    draw,
    minimum width=0.8cm,
    minimum height=0.5cm,
    font=\footnotesize
  }
]
  
  % 左側: ソースコード表示
  \node[codeline] (line1) at (0,0) {class Point:};
  \node[codeline] (line2) at (0.5,-0.6) {def \_\_init\_\_(self, x, y):};
  \node[codeline] (line3) at (1,-1.2) {self.x = x};
  \node[codeline] (line4) at (1,-1.8) {self.y = y};
  
  % 右側: インデントスタックの状態
  \node[right=5.5cm of line1, anchor=west] (stack_label) {\textbf{インデントスタック}};
  
  % スタック状態1: [0]
  \node[stacknode, below=0.3cm of stack_label] (s1_0) {0};
  \node[right=0.2cm of s1_0] {← 初期状態};
  
  % スタック状態2: [0, 2]
  \node[stacknode, below=0.7cm of s1_0] (s2_0) {0};
  \node[stacknode, right=0 of s2_0] (s2_2) {2};
  \node[right=0.2cm of s2_2] {← class定義後};
  
  % スタック状態3: [0, 2, 4]
  \node[stacknode, below=0.7cm of s2_0] (s3_0) {0};
  \node[stacknode, right=0 of s3_0] (s3_2) {2};
  \node[stacknode, right=0 of s3_2] (s3_4) {4};
  \node[right=0.2cm of s3_4] {← def定義後};
  
  % スタック状態4: [0, 2]
  \node[stacknode, below=0.7cm of s3_0] (s4_0) {0};
  \node[stacknode, right=0 of s4_0] (s4_2) {2};
  \node[right=0.2cm of s4_2] {← defブロック終了};
  
  % スタック状態5: [0]
  \node[stacknode, below=0.7cm of s4_0] (s5_0) {0};
  \node[right=0.2cm of s5_0] {← classブロック終了};
\end{tikzpicture}
\end{center}
```

スタックを使っていることから一見文脈自由文法で処理できるように思えるかもしれませんが、スタックの各要素が数字であり、この数字が対応してない時点でエラーになるので、文脈自由文法ではありません。Pythonの字句解析器はこのようなインデント/デデントトークンをプログラムで処理していますが、その字句解析器は明らかに文脈自由でない言語を処理しています。

いずれにせよ、インデント文法が文脈自由言語に収まらないために、字句解析器あるいは構文解析器のどちらかにしわ寄せがきます。インデント文法を処理できるもっと強力な形式文法はありますが、プログラムでad hocに対応する方が早いため、そのような方式が一般的です。

## ヒアドキュメント

ヒアドキュメントは複数行に渡る文字列を記述するための文法で、従来はbashなどのシェル言語で採用されていましたが、PerlやRubyもヒアドキュメントを採用しました。たとえば、RubyでHTMLの文字列をヒアドキュメントで以下のように書くことができます。

```ruby
html = <<HTML
<html>
  <head><title>Title</title></head>
  <body><p>Hello</p></body>
</html>
HTML
```

特筆すべきは、`<<HTML`と`HTML`のように対応している間だけが文字列として解釈されることです。これだけなら文脈自由言語の範囲内です。実際には問題はもっと複雑です。ヒアドキュメントは**ネストが可能**なのです。たとえば、以下のようなヒアドキュメントは正しいRubyプログラムです。

```ruby
here = <<E1 + <<E2
ここはE1です
E1
ここはE2です
E2
```

これは以下の内容の文字列として解釈されます。

```ruby
ここはE1です
ここはE2です
```

ヒアドキュメント内では文字列補間が使えるのでさらに複雑です。以下のようなヒアドキュメントもOKなのです。

```ruby
a = 100
b = 200
here = <<A + <<B
aは#{a}です
A
bは#{b}です
B
```

これは次の文字列として解釈されます。

```
aは100です
bは200です
```

読者の方々はおそらく「確かに凄いけど、普通はこのような書き方をすることはほぼないのでは」と思われたのではないでしょうか。実際問題そうなのですが、Rubyはこのような複雑怪奇なプログラムもうまく構文解析できなければいけないのも事実です。

Rubyのヒアドキュメントを適切に構文解析するにはHTMLやXMLにおけるタグ名の対応付けと同じ処理が必要になりますが、これは文脈自由言語の範囲を超えています。Rubyのヒアドキュメントがこのような振る舞いをすることを初めて知ったのは筆者が大学院生の頃ですが、あまりに予想外の振る舞いに目眩がする思いだったのを覚えています。

Rubyのヒアドキュメントが実際にどのように実装されているかはさておき、筆者はかつて中田育男先生が実装されたISO RubyのScalaによる試験的な構文解析器の実装のお手伝いをした際に、ヒアドキュメントの扱いに非常に苦労しました。

実装の詳細は、中田先生の[ruby_scalaリポジトリ](https://github.com/inakata/ruby_scala/blob/3f54cc6f80678e30a211fb1374280246f08182ed/src/main/scala/com/github/inakata/ruby_scala/Ruby.scala#L1383)で確認できますが、ヒアドキュメントの開始デリミタを記憶し、対応する終了デリミタが現れるまでを特別に処理する、といった複雑なロジックが必要でした。

このときはScalaのパーサーコンビネータを使ってヒアドキュメントを再現したのですが、引数を取ってコンビネータを返すメソッドを定義することで問題を解決しました。形式言語の文脈でいうのなら、PEGの規則が引数を持てるように拡張することでヒアドキュメントを解釈できるようになったと言うことができます。

PEGを拡張して規則が引数を持てるようにするという試みは複数ありますが、筆者もMacro PEGというPEGを拡張したものを提案しました[^macropeg]。ヒアドキュメントという当たり前に使われている言語機能ですら、構文解析を正しく行うためには厄介な処理をする必要があるのです。

[^macropeg]: 実装と仕様は[https://github.com/kmizu/macro_peg](https://github.com/kmizu/macro_peg) を参照してください。

```{=latex}
\usetikzlibrary{decorations.pathreplacing}
\begin{center}
\begin{tikzpicture}[
  node distance=0.4cm,
  every node/.style={font=\footnotesize},
  % ヒアドキュメントのスタイル
  heredoc/.style={
    rectangle,
    anchor=west,
    font=\ttfamily\footnotesize
  },
  % デリミタのスタイル
  delimiter/.style={
    rectangle,
    draw,
    rounded corners=2pt,
    fill=yellow!20,
    font=\ttfamily\footnotesize,
    minimum height=0.5cm,
    minimum width=1cm
  },
  % スタック要素のスタイル
  stackitem/.style={
    rectangle,
    draw,
    fill=orange!10,
    minimum width=1cm,
    minimum height=0.5cm,
    font=\footnotesize\ttfamily
  },
  % ブレースのスタイル
  brace/.style={
    decorate,
    decoration={brace, amplitude=5pt},
    thick
  }
]
  
  % 左側: ヒアドキュメントの構造
  \node[heredoc] (line1) at (0,0) {<<E1};
  \node[heredoc] (line2) at (0.3,-0.5) {...};
  \node[heredoc] (line3) at (0.3,-1) {<<E2};
  \node[heredoc] (line4) at (0.6,-1.5) {...};
  \node[heredoc] (line5) at (0.3,-2) {E2};
  \node[heredoc] (line6) at (0.3,-2.5) {...};
  \node[heredoc] (line7) at (0,-3) {E1};
  
  % デリミタのハイライト
  \node[delimiter, right=2cm of line1] (delim1_start) {E1開始};
  \node[delimiter, right=2cm of line3] (delim2_start) {E2開始};
  \node[delimiter, right=2cm of line5] (delim2_end) {E2終了};
  \node[delimiter, right=2cm of line7] (delim1_end) {E1終了};
  
  % 矢印
  \draw[->, gray] (line1.east) -- (delim1_start.west);
  \draw[->, gray] (line3.east) -- (delim2_start.west);
  \draw[->, gray] (line5.east) -- (delim2_end.west);
  \draw[->, gray] (line7.east) -- (delim1_end.west);
  
  % ブレースで範囲を示す
  \draw[brace] ([xshift=-3mm]line1.west) -- ([xshift=-3mm]line7.west) 
    node[midway, left=5mm, text width=1cm, align=center] {E1の\\範囲};
  \draw[brace] ([xshift=-1mm]line3.west) -- ([xshift=-1mm]line5.west) 
    node[midway, left=3mm, text width=1cm, align=center] {E2の\\範囲};
\end{tikzpicture}
\end{center}
```

## 改行終端可能文法

C、C++、Java、C#などの言語では、解釈・実行の基本単位は**文**(Statement)と呼ばれるものになります。また、文はセミコロンなどの終端子と呼ばれるもので終わるか、区切り文字で区切られるのが一般的です。セミコロンが終端子でなく文の区切りになるのがPascalなどの言語です。厳密には違いますが、関数型プログラミング言語Standard MLのセミコロンも似たような扱いです。

Javaでは次のように書くことで、A、B、Cを順に出力することができます。

```java
System.out.println("A");
System.out.println("B");
System.out.println("C");
```

このように文が終端子（Terminator)で終わる文法には、文の途中に改行が挟まっても単なるスペースと同様に取り扱えるという利点があります。先程のプログラムを次のように書き換えても意味は変わりません。

```java
System.out.println(
  "A");
System.out.println(
  "B");
System.out.println(
  "C");
```

大抵の場合、文は一行で終わるのですから、毎回セミコロンをつけなければいけないのも面倒くさいものです。そういったニーズを反映してか、Scala、Kotlin、Swift、Goなどの比較的新しい言語では（Scalaの初期バージョン
が2003ですから、そこまで新しいのかという話もありますが）、文はセミコロンで終わることもできるが、改行でも終わることができます。より古い言語でもPython、Ruby、JavaScriptも改行で文が終わることができます。

たとえば、先程のJavaプログラムに相当するScalaプログラムは次のようになります。

```scala
println("A")
println("B")
println("C")
```

見た目にもすっきりしますし、改行を併用するコーディングスタイルが大半であることを考えても、無駄なタイピングが減るしでいいことずくめです。Scalaではそれでいて、次のように文の途中で改行が入っても問題なく解釈・実行することができます。

```scala
println(
  "A")
println(
  "B")
println(
  "C")
```

Scalaでも一行の文字数が増えれば分割したくなりますから、このような機能があるのは自然でしょう。

ここで一つの疑問が湧きます。「文は改行で終わる」という規則なら改行が来たときに「文の終わり」とみなせばよいですし、「文はセミコロンで終わる」という規則なら、セミコロンが来たときに「文の終わり」とみなせば問題ありません。しかしながら、このような文法を実現するためには「セミコロンが来れば文が終わるが、改行で文が終わることもある」というややこしい規則に基づいて構文解析をしなければいけません。

このような文法を実現するのは案外ややこしいものです。Javaの`System.out.println("A");`という文は正確には「式文」と呼ばれますが、この式文は次のように定義されます。

```text
expression_statement ::= expression <SEMICOLON>
```

`<SEMICOLON>`はセミコロンを表すトークンです。では、Scala式の文法を「改行でもセミコロンでも終わることができる」と考えて次のように記述しても大丈夫でしょうか。

```text
expression_statement = expression (<SEMICOLON> | <LINE_TERMINATOR>)
```


`<LINE_TERMINATOR>`は改行を表すトークンです。プラットフォームによって改行コードは異なるので、このように定義しておくと楽でしょう。このような規則でうまく先程の例全てをうまく取り扱えるかといえば、端的に言って無理です。たとえば、次のようなコードを考えてみましょう。

```scala
println(
  "A")
```

`(`の後に改行が来ていますが、ここでは単なる空白文字として扱って無視して欲しいわけです。しかし、上記の規則では改行が来た時点で文が終わってしまいます。つまり、上のような式文は「式文が改行で終わる」という規則に従っていないため、構文解析エラーになってしまいます。

C系の言語では原則的には、字句解析時に改行もスペースも同じ扱いで処理しているので、このような「式の途中で改行が来る」ケースも特に工夫する必要がありませんでした。しかし、Scalaなどの言語における「改行」は式の途中では無視されるが文末では終端子にもなり得るという複雑な存在です。

言い換えると「文脈」を考慮して改行を取り扱う必要がでてきたのです。これ自体は文脈自由文法の範囲で取り扱う事が可能ですが、字句解析器が素朴に空白文字を無視するだけでは対応できません。なぜなら、通常の字句解析器は文脈を持たないからです。字句解析器は入力をトークンに分解するだけのもので、文脈情報を持たないため、改行が文の終わりかどうかを判断できません。

このような文法はどのようにすれば取り扱えるでしょうか。なかなか難しい問題ですが、大きく分けて二つの戦略があります。

一つ目は字句解析器に文脈情報を持たせる方法です。たとえば、「式」モードでは改行は無視されるが、「文」モードだと無視されないという風にした上で、式が終わったら「文」モードに切り替えを行い、式が開始したら「式」モードに切り替えを行います。この方式を採用している典型的な言語がRubyで、Cで書かれた字句解析器には実に多数の文脈情報を持たせています。

```c
// https://github.com/ruby/ruby/blob/v3_2_0/parse.y#L161-L181
/* examine combinations */
enum lex_state_e {
#define DEF_EXPR(n) EXPR_##n = (1 << EXPR_##n##_bit)
    DEF_EXPR(BEG),
    DEF_EXPR(END),
    DEF_EXPR(ENDARG),
    DEF_EXPR(ENDFN),
    DEF_EXPR(ARG),
    DEF_EXPR(CMDARG),
    DEF_EXPR(MID),
    DEF_EXPR(FNAME),
    DEF_EXPR(DOT),
    DEF_EXPR(CLASS),
    DEF_EXPR(LABEL),
    DEF_EXPR(LABELED),
    DEF_EXPR(FITEM),
    EXPR_VALUE = EXPR_BEG,
    EXPR_BEG_ANY  =  (EXPR_BEG | EXPR_MID | EXPR_CLASS),
    EXPR_ARG_ANY  =  (EXPR_ARG | EXPR_CMDARG),
    EXPR_END_ANY  =  (EXPR_END | EXPR_ENDARG | EXPR_ENDFN),
    EXPR_NONE = 0
};
```

「改行で文が終わる」以外にもRubyはかなり複雑な構文解析を行っているため、このように多数の状態を字句解析器に持たせる必要があります。Rubyの文法は私が知る限り**もっとも複雑なものの一つ**なのでややこれは極端ですが、字句解析器に状態を持たせるアプローチは他の言語も採用していることが多いようです。

別のアプローチとしてPEGを使うという方法があります。PEGでは字句解析という概念自体がありませんから、式の途中に改行が入るというのも構文解析レベルで処理できます。Pythonでは3.9からPEGベースのパーサーが既定となり、3.10では旧来のLL(1)パーサーが削除されました。これにより、Pythonのパーサーは改行の扱いなど、柔軟な構文規則を以前よりも直接的に記述しやすくなりました。

また、拙作のプログラミング言語Klassicでは次のようにして式の合間に改行を挟むことができるようにしています。

```scala
//add ::= term {"+" term | "-" term}
lazy val add: Parser[AST] = rule{
  chainl(term)(
    (%% << CL(PLUS)) ^^ { location => (left: AST, right: AST) => 
        BinaryExpression(location, Operator.ADD, left, right) 
    } 
  | (%% << CL(MINUS)) ^^ { location => (left: AST, right: AST) => 
        BinaryExpression(location, Operator.SUBTRACT, left, right) 
    }
  )
}
```

関数`CL()`は次のように定義されます。

```scala
 def CL[T](parser: Parser[T]): Parser[T] = parser << SPACING
```

Klassicの構文解析器は自作のパーサーコンビネータライブラリで構築されているので少々ややこしく見えますが、要約すると、`CL()`は引数に与えたものの後に任意個のスペース（改行）が来るという意味で、キーワードである`PLUS`や`MINUS`の後にこのような規則を差し込むことで「式の途中での改行は無視」が実現できています。

現在ある言語で採用されているかはわかりませんが、GLR法のようにスキャナレス構文解析と呼ばれる他の手法を使う方法もあります。スキャナレスということは字句解析が無いということですが、字句解析器を別に必要としない構文解析法の総称を指します。PEGも字句解析器を必要としませんから、スキャナレス構文解析の一種と言えます。

ともあれ、私達が普通に使っている「改行で文が終わる」ようにできる処理一つとっても厄介な問題だということがポイントです。

## Cのtypedef

C言語の`typedef`文は既存の型に別名をつける機能です。C言語やC++言語のプログラムをバリバリ書いているプログラマならお馴染みの機能です。`typedef`は

- 移植性を高める
- 関数ポインタを使ったよみづらい宣言を読みやすくする

といった目的で使われますが、この`typedef`文が意外に曲者だったりします。以下は`i`をint型の別名として定義するものですが、同時にローカル変数`i`を`i`型として定義しています。

```c
typedef int i;
int main(void) {
        i i = 100; // OK
        int x = (i)'a'; // ERROR
        return 0;
}
```

現実にこのようなコードの書き方をするかはともかく、

```c
i i = 100;
```

は明らかにOKな表現として解析してあげなければいけません。一方で、

```c
int x = (i)'a';
```

は構文解析エラーになります。`i i = 100;`という宣言がなければこの文も通るのですが、合わせて書けば構文解析エラーです。それ以前の文脈である識別子がtypedefされたかどうかで構文解析の結果が変わるのですからとてもややこしいです。

C言語ではこのようなややこしい構文を解析するために、構文解析器がtypedefした識別子のテーブルを持っておいて、それを使って構文解析を行うという手法を採用しています。

## Scalaでの「文頭に演算子が来る場合の処理」

7.4 で改行で文を終端する文法について説明しましたが、Scalaはさらにややこしい入力を処理できなければいけません。たとえば、以下のような文を解釈できる必要があります。

```scala
val x = 1
      + 2
println(x) // 3
```

ここで、xを3とちゃんと解釈するには`val x = 1`が改行で終わったから「文が終わった」と解釈せず、次のトークンである`+`まで見てから文が終わるか判定する必要があります。これまで試した限り、同じことができるのはJavaScriptくらいで、Ruby、Python、Kotlin、Go、Swiftなどの言語ではエラーになるか、`x = 1`で文が終わったと解釈され、`+ 2`は別の文として解釈されるケースばかりでした。

この処理について、Scala言語仕様内の[1.2 Newline Characters](https://scala-lang.org/files/archive/spec/2.13/01-lexical-syntax.html)に関連する記述があります。

> Scala is a line-oriented language where statements may be terminated by semi-colons or newlines. A newline in a Scala source text is treated as the special token “nl” if the three following criteria are satisfied:
>   1. The token immediately preceding the newline can terminate a statement.
>   2. The token immediately following the newline can begin a statement.
>   3. The token appears in a region where newlines are enabled.
> The tokens that can terminate a statement are: literals, identifiers and the following delimiters and reserved words:

これを意訳すると、通常の場合はScalaの文はセミコロンまたは改行で終わることができるが、次の三つの条件**全て**を満たしたときのみ、特別なトークン`nl`として扱われることになる、ということになります。

1. 改行の直前のトークンが「文を終わらせられる」ものである場合
2. 改行の直後のトークンが「文を始められる」ものである場合
3. 改行が「利用可能」になっている箇所にあらわれたものである場合

たとえば、以下のScalaプログラムについていうと、最初の改行は`nl`トークンになりませんが、何故かというと条件1が満たされても条件2が満たされないからです。

```scala
val x = 1
      + 2
```

ちなみに、調査を開始する時点ではScalaの文法の基本文法を継承したKotlinでも同じようになっていると思っていたのですが、一行目で文が終わると解釈されてしまいました。

```kotlin
val x = 1
      + 2 // Kotlinでは+ 2は単独の式として解釈されてしまう
println(x) // 1
```

これらに対する体系的な調査は行っていませんが、Scalaは文法上、ところどころにトリッキーな機能があり、改行の扱いも特殊なものになっています。

## C++のテンプレート構文

C++のテンプレートは型パラメータを使ったジェネリックプログラミングを可能にする強力な機能ですが、その構文は構文解析の観点から見ると興味深い問題を含んでいます。特に有名なのが「`>>`の曖昧性」です。

Javaのジェネリクスでいうと `List<List<String>>` のような書き方に相当しますが、C++では歴史的な経緯から特別な問題がありました。

C++03までは、次のようなネストしたテンプレートの宣言はコンパイルエラーになりました：

```cpp
std::vector<std::vector<int>> matrix;  // C++03ではエラー
```

何故かというと、`>>`が右シフト演算子として解釈されてしまうからです。そのため、C++03では次のようにスペースを入れる必要がありました：

```cpp
std::vector<std::vector<int> > matrix;  // C++03ではOK
```

この問題は単純な字句解析の問題のように見えますが、実はそうではありません。次の例を考えてみましょう：

```cpp
template<int N>
struct A {
    static const int value = N;
};

// これは A<1>::value >> 2 （右シフト）
int x = A<1>::value >> 2;

// これは vector<vector<int>> （テンプレートの終端）
std::vector<std::vector<int>> v;
```

同じ`>>`という文字列が、文脈によって右シフト演算子として解釈されたり、テンプレート引数リストの終端記号2つとして解釈されたりする必要があるのです。これは明らかに文脈自由文法の範囲を超えており、構文解析器は現在の文脈（テンプレート引数リストの中にいるかどうか）を追跡する必要があります。

C++11以降では、この問題に対処するため、構文解析器がテンプレート引数リストの文脈では`>>`を`> >`として解釈するように標準が変更されました。しかし、これは構文解析器の実装をより複雑にします。例えば、次のような入れ子になったケースも正しく処理する必要があります：

```cpp
std::map<int, std::vector<std::vector<int>>> nested;  // C++11以降はOK
```

さらに複雑なのは、`>>=`（右シフト代入演算子）や`>>>`（一部の拡張で使われる）なども考慮する必要があることです。構文解析器は、テンプレート引数リストの文脈を正確に追跡し、適切にトークンを分割する必要があります。

## 正規表現リテラルの曖昧性

JavaScriptには正規表現リテラルという便利な記法があります。Javaでは `Pattern.compile("[a-z]+", Pattern.CASE_INSENSITIVE)` のように書くところを、JavaScriptでは以下のように簡潔に書けます：

```javascript
const pattern = /[a-z]+/i;  // 大文字小文字を区別しない英字のパターン
```

しかし、この`/`記号は除算演算子としても使われるため、構文解析上の曖昧性が生じます：

```javascript
// これは正規表現リテラル
const regex = /ab+c/;

// これは除算演算
const result = a / b + c / d;
```

さらに厄介なのは、次のようなケースです：

```javascript
// これは何でしょう？
return /regex/i.test(str);  // 正規表現リテラル
return a / b / c;            // 除算演算
```

この曖昧性を解決するには、構文解析器は現在の文脈を理解する必要があります。JavaScriptの仕様では、正規表現リテラルが現れることができる位置と、除算演算子が現れることができる位置を厳密に定義しています。

一般的なルールとしては以下のようになります：

- 式の開始位置では`/`は正規表現リテラルの開始
- 式の途中では`/`は除算演算子

しかし、実際にはもっと複雑で、直前の構文要素が式を完結しているかどうか（識別子・リテラル・後置インクリメント・括弧で囲まれた式など）で判断する必要があります：

```javascript
// 関数呼び出し ( 式を完結する ) の直後 → 除算
func() / 2

// 代入演算子の直後 ( 式の開始位置 ) → 正規表現
const x = /pattern/

// return の直後 → 正規表現
return /pattern/g
```

ややこしいのは、同じ`)`でも`if (cond)`のような制御構文の閉じ括弧の直後では`/`が正規表現として解釈される点です（例: `if (a) /pat/.test(s)`）。つまり、字句解析器は単に前のトークンの種類だけでなく、その`)`が式の終端か制御構文の構成要素かまで把握する必要があり、字句解析と構文解析が密接に連携しなければなりません。

## エラーリカバリ

構文解析の途中でエラーが起きることは普通にあります。構文解析中のエラーリカバリについては多くの研究があるものの、コンパイラの教科書で構文解析アルゴリズムでのエラーリカバリについて言及されることは稀です。推測ですが、構文解析において２つ目以降のエラーは大抵最初のエラーに誘発されて起こるということや、どうしても経験則に頼った記述になりがちなため、教科書で言及されることは少ないのでしょう。また、大抵の言語処理系で構文解析中のエラーリカバリについては大したことをしていなかったという歴史的事情もあるかもしれません。

しかし、現在は別の観点から構文解析中のエラーリカバリが重要性を増してきています。それは、IDEあるいはテキストエディタの拡張としてのIDE類似の機能を提供することが一般的になったため、「構文解析エラーになるが、それっぽくなんとか構文解析をして欲しい」という強いニーズがあるからです。

ユーザーがコードを書いている最中は、一時的に文法的に正しくない状態になることが頻繁にあります。IDEがそのような状況でも構文ハイライト、コード補完、リアルタイムのエラー表示などの機能を提供し続けるためには、エラーが発生しても即座に解析を中断するのではなく、可能な限り解析を継続し、後続のエラーも検出できるような仕組み、すなわちエラーリカバリ機構が不可欠です。

エラーリカバリにはいくつかの代表的な手法があります。

- **パニックモード (Panic Mode):** 最も単純な手法の一つです。エラーを検出したら、セミコロン（`;`）や閉じ波括弧（`}`）のような、文やブロックの区切りとなる「同期トークン」が見つかるまで、入力トークンを読み飛ばします。同期トークンが見つかったら、そこから解析を再開します。

   C言語風のコード `x = a + * b; y = c;` で、`*` が予期しないトークンとしてエラーになった場合、パニックモードでは `*` と `b` を読み飛ばし、次の同期トークンである `;` を見つけて解析を再開します。これにより、`y = c;` の解析は行われますが、`* b` に関するエラーの詳細は失われる可能性があります。

- **フレーズレベルリカバリ (Phrase-Level Recovery):** エラー箇所の周辺で、局所的な修正を試みる手法です。例えば、不足しているセミコロンを補ったり、予期しないトークンを削除したり、期待されるトークンに置き換えたりします。

 ```javascript
 x = a + b y = c;
 ```

というコードで、`b` と `y` の間にセミコロンが欠落している場合、パーサは `y` が予期しないトークンであると判断します。フレーズレベルリカバリでは「文の終わりにはセミコロンが期待される」という知識に基づき、`b` の後にセミコロンを挿入して `x = a + b; y = c;` として解析を試みるかもしれません。
 
あるいは、Javaのメソッド呼び出しで

```java
myObject.method(arg1 arg2)
```

のようにカンマが抜けている場合、`arg1` と `arg2` の間にカンマを補って `myObject.method(arg1, arg2)` として解釈を試みる、といった具合です。

JSONの配列 `[1, , 2]` で余分なカンマがある場合、削除して `[1, 2]` として解析を続けるかもしれません。パニックモードよりは洗練されていますが、どのような修正を行うかの判断が難しく、実装が複雑になりがちです。

- **エラー生成規則 (Error Productions):** 文法にあらかじめよくあるエラーパターンに対応する生成規則を追加しておく手法です。例えば、「`if (condition) statement`」という正しい規則に加えて、「`if condition) statement`」（開き括弧が欠落）のようなエラー用の規則を定義しておきます。これにより、特定のエラーを「受理」し、解析を継続できます。

  Yaccのようなツールでは、`error` トークンを使ってエラー規則を定義できます。

```text
statement: IF '(' expr ')' statement
         | IF error ')' statement { yyerror("Missing opening parenthesis in if statement"); }
         | /* ... other rules ... */
         ;
```

この例では、`if` の後に開き括弧 `(` がない場合に `error` トークンがマッチし、エラーメッセージを出力しつつ、`)` 以降の解析を継続しようとします。多くのエラーパターンを網羅しようとすると文法が複雑になりますが、特定のエラーに対しては効果的です。

- **グローバル訂正 (Global Correction):** 理論的には最も強力な手法で、入力文字列全体に対して、最小限の修正（挿入、削除、置換）で文法的に正しい文字列に変換する方法を探します。

  `if x > 0) { ... }` という入力に対し、グローバル訂正は開き括弧 `(` を挿入するのが最小の修正であると判断するかもしれません。しかし、入力全体を考慮して最適な修正を見つけるのは計算量的に非常に困難であり、実用的なコンパイラやIDEでこの手法が全面的に採用されることは稀です。

IDEのような環境では、これらの手法を組み合わせたり、部分的な構文木（エラー箇所を含むかもしれないが、解析できた部分）を構築したり、インクリメンタルな解析（変更箇所だけを再解析する）を行ったりすることで、ユーザーが編集中でも可能な限り正確な情報を提供しようと試みています。

例えば、Javaのクラスを書いている途中で

```java
class MyClass { 
  public void myMethod() { 
    ... 
  }
// ここで閉じ括弧 `}` を入力し忘れた場合
```

と入力し、最後の閉じ括弧 `}` を入力し忘れている場合でも、IDEは `myMethod` の本体部分についてはある程度解析を試み、メソッド内の変数に対するコード補完や型チェックを行おうとします。これは、エラーリカバリ機構が、エラー箇所を特定しつつも、それ以外の部分については解析を継続し、部分的な構文情報を抽出しているためです。

エラーリカバリは、単にエラーを見つけるだけでなく、その後の解析をどう継続し、ユーザーにどのようなフィードバックを与えるかという、実践的で奥深い問題領域なのです。

## まとめ

7章では現実の構文解析で遭遇する問題について、いくつかの例を挙げて説明しました。筆者が大学院博士後期課程に進学した頃「構文解析は終わった問題」と言われたのを覚えていますが、実際にはその後もANTLRの`LL(*)`、さらにそれに続く`ALL(*)`アルゴリズムのような革新が起きていますし、細かいところでは今回の例のように従来の構文解析法単体では取り扱えない部分をアドホックに各プログラミング言語が補っている部分があります。

このような問題が起きるのは結局のところ、当初の想定と違って「プログラミング言語は文脈自由言語では表せなかった」ということです。もちろん、文脈自由言語の範囲に納めるように文法を制限することは可能ですが、便利な表記を許していくとどうしても文脈自由言語から「はみ出て」しまうということでしょう。このような「現実のプログラミング言語の文脈依存性」については専門の研究者以外には案外知られていなかったりしますが、ともあれこのような問題があることを知っておくのは、既存言語の表記法を取り入れた新しい言語を設計するときにも有益でしょう。
