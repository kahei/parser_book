
# JSONの構文解析

2章で構文解析に必要な基本概念について学ぶことができました。この章ではJSONという実際に使われている言語を題材に、より実践的な構文解析のやり方を学んでいきます。

## JSONの概要

JSON（JavaScript Object Notation）は、WebサービスにアクセスするためのAPIで非常に一般的に使われているデータフォーマットです。また、企業内サービス間で連携するときにも非常によく使われます。皆さんは何らかの形でJSONに触れたことがあるのではないかと思います。

JSONは元々はJavaScriptのオブジェクトリテラル構文を参考にして設計されたデータ形式ですが、現在はECMA-404[^1]およびIETFの[RFC 8259](https://datatracker.ietf.org/doc/html/rfc8259)で標準化されており、色々な言語でJSONを扱うライブラリがあります。なお、JSONはJavaScriptの厳密な部分集合ではなく、両者の仕様は別々に管理されている独立した規格です（U+2028/U+2029の扱いなど、歴史的に細かな差異がありました）。また、JSONはデータ交換用フォーマットの中でも非常にシンプルであるという特徴があり、そのシンプルさ故か、同じ言語でもJSONを扱うライブラリが乱立する程です。今のWebアプリケーション開発に携わる開発者にとってJSONは避けて通れないといってよいでしょう。
 
以降では簡単なJSONのサンプルを通してJSONの概要を説明します。

### オブジェクト

以下は、二つの名前/値のペアからなる**オブジェクト**です。

```js
{
  "name": "Kota Mizushima",
  "age":  41
}
```

このJSONは、`name`と`"Kota Mizushima"`という文字列の**ペア**と、`age`と`41`という数値の**ペア**からなる**オブジェクト**であることを示しています。

なお、用語については、ECMA-404の仕様書に記載されているものに準拠しています。名前/値のペアは、属性やプロパティと呼ばれることもあるので、適宜読み替えてください。
日本語で表現すると、このオブジェクトは、名前が`Kota Mizushima`、年齢が`41`という人物一人分のデータを表していると考えることができます。オブジェクトは、`{}`で囲まれた、`"name":value`の対が`,`を区切り文字として続く形になります。後述しますが、`name`の部分は**文字列**である必要があります。

### 配列

別の例として、以下のJSONを見てみます。

```js
{
  "kind":"Rectangle",
  "points": [
    {"x":0,   "y":0  },
    {"x":0,   "y":100},
    {"x":100, "y":100},
    {"x":100, "y":0  },
  ]
}
```

このJSONは、
 
- `"kind"`と`"Rectangle"`のペア
- `"points"`と`[...]`のペア

からなるオブジェクトです。さらに、`"points"`に対応する値が**配列**になっていて、その中に以下の4つの要素が含まれています。
 
- オブジェクト： `{"x":0,   "y":0}`
- オブジェクト： `{"x":0,   "y":100}`
- オブジェクト： `{"x":100, "y":100}`
- オブジェクト： `{"x":100, "y":0}`

配列は、`[]`で囲まれた要素の並びで、区切り文字は`,`です。

このオブジェクトは、種類が四角形で、それを構成する点が`(0, 0), (0, 100), (100, 100), (100, 0)`からなっているデータを表現しているとみることができます。
 
### 数値

これまで見てきたオブジェクトと配列は複合的なデータでしたが、既に出てきているように、JSONにはこれ以上分解できないデータもあります。先ほどから出てきている数値もそうです。数値は、

```js
1
10
100
1000
1.0
1.5
```

のような形になっており、整数または小数です。JSONでの数値の解釈は特に規定されていない点に注意してください。たとえば、`0.1`は2進法での小数だと解釈しても良いですし、10進法での小数と解釈しても構いません。つまり、特に、IEEEの浮動小数点数である、といった規定はありません。

### 文字列

JSONのデータには文字列もあります。

```js
"Hello, World"
"Kota Mizushima"
"hogehoge"
```

のように、`""`で囲まれたものが文字列となります。JSONの仕様では、オブジェクトのキーは必ずダブルクォーテーションで囲まれた文字列でなければなりません。たとえば、以下は**JavaScriptの**オブジェクトリテラルとしては許容されますが（`name` が識別子の命名規則に合致するため）、**JSON**の定義には従っていません。

```js
{
  name: "Kota Mizushima",
  age:  41
}
```

#### コラム：JSONとJavaScriptオブジェクトの違い

JSONとJavaScriptのオブジェクトリテラルは似ていますが、いくつかの重要な違いがあります。以下の点に注意してください。

- キーはJSONでは必ずダブルクォーテーションで囲まれた文字列です（例：`"name"`）。JavaScriptでは識別子であればダブルクオート省略可能です。
- コメントはJSONには存在しません（`//` や `/* ... */` は不可）。
- 末尾カンマはJSONでは不可です（JavaScriptの一部構文では許容される場合あり）。
- 値の種類も異なります。`undefined`/`NaN`/`Infinity` などはJSONの値としては不正です。

### 真偽値

JSONには、多くのプログラミング言語にある真偽値もあります。JSONの真偽値は以下のように、`true`または`false`の二通りです。

```js
true
false
```

真偽値も解釈方法は定められていませんが、ほとんどのプログラミング言語で、該当するリテラル表現があるので、おおむねそのような真偽値リテラルにマッピングされます。

### null

多くのプログラミング言語にある要素ですが、JSONには`null`もあります。多くのプログラミング言語のJSONライブラリでは、無効値に相当する値にマッピングされますが、JSONの仕様では`null`の解釈は定められていません。`null`に相当するリテラルがあればそれにマッピングされる事も多いですが、`Option`や`Maybe`といったデータ型によって`null`を表現する言語では、そのようなデータ型にマッピングされる事が多いようです。

### JSONの全体像

ここまでで、JSONで現れる6つの要素について説明しましたが、JSONで使える要素は**これだけ**です。このシンプルさが、多くのプログラミング言語でJSONが使われる要因でもあるのでしょう。JSONで使える要素について改めて並べてみます。

- オブジェクト
- 配列
- 数値
- 文字列
- 真偽値
- `null`

次の節では、このJSONの**文法**が、どのような形で表現できるかについて見ていきます。

## JSONのBNF

前の節でJSONの概要について説明し終わったところで、いよいよJSONの文法について見ていきます。JSONの文法はECMA-404の仕様書に記載されていますが、ここでは、それを若干変形したBNFで表現されたJSONの文法を見ていきます。

JSONのBNFによる定義を簡略化したものは以下で全てです。特に小数点以下の部分は煩雑になる割に本質的でないので削除しました。また、文字列のエスケープシーケンス（`\n`、`\t`、`\"`など）も本章では扱いません。これらは構文解析の本質を理解する上では必須ではないためです。

```text
json = ws value;
value = true | false | null | number | string | object | array;
object = LBRACE RBRACE | LBRACE pair {COMMA pair} RBRACE;
pair = string COLON value;
array = LBRACKET RBRACKET | LBRACKET value {COMMA value} RBRACKET ;
string = "\"" {CHAR} "\"" ws;
number = INT ws;
true = "true" ws;
false = "false" ws;
null = "null" ws;

COMMA = "," ws;
COLON = ":" ws;
LBRACE = "{" ws;
RBRACE = "}" ws;
LBRACKET = "[" ws;
RBRACKET = "]" ws;

ws = {" " | "\t" | "\n" | "\r"} ;
CHAR = "a" | "b" | "c" | "d" | "e" | "f" | "g" | "h" | "i" | "j" | "k" | "l" | "m" |
       "n" | "o" | "p" | "q" | "r" | "s" | "t" | "u" | "v" | "w" | "x" | "y" | "z" |
       "A" | "B" | "C" | "D" | "E" | "F" | "G" | "H" | "I" | "J" | "K" | "L" | "M" |
       "N" | "O" | "P" | "Q" | "R" | "S" | "T" | "U" | "V" | "W" | "X" | "Y" | "Z" |
       "0" | "1" | "2" | "3" | "4" | "5" | "6" | "7" | "8" | "9" |
       " " | "!" | "#" | "$" | "%" | "&" | "'" | "(" | ")" | "*" | "+" | "," |
       "-" | "." | "/" | ":" | ";" | "<" | "=" | ">" | "?" | "@" | "[" | "]" |
       "^" | "_" | "`" | "{" | "|" | "}" | "~" ;
INT = ["-"] ("0" | (NONZERO {DIGIT})) ;
DIGIT = "0" | "1" | "2" | "3" | "4" | "5" | "6" | "7" | "8" | "9" ;
NONZERO = "1" | "2" | "3" | "4" | "5" | "6" | "7" | "8" | "9" ;
```

これまで説明したJSONの要素と比較して見慣れない記号が出てきましたが、一つ一つ見て行きましょう。

なお、規則名の大文字・小文字の使い分けについてですが、`COMMA`、`LBRACE`のような大文字で書かれた規則は、後の字句解析の節で出てくる「トークン」に対応します。これらは単一の記号や固定の文字列を表すもので、構文解析の際には一つの単位として扱われます。一方、`json`、`value`のような小文字の規則は、より複雑な構造を表します。補助規則として、`ws`（空白）、`CHAR`（印字可能文字の集合）、`INT`（整数）、`DIGIT`（数字1桁）、`NONZERO`（非ゼロの数字）を定義しています。

### json

一番上から読んでいきます。2章の復習になりますが、BNFでは、

 ```
json = ws value;
 ```
 
のような**規則**の集まりによって、文法を表現します。各規則はセミコロン（`;`）で終わります。`=`の左側である`json`が**左辺**で、右側（ここでは `ws value`）が**本体**になります。さらに、本体の中に出てくる、他の規則を参照する部分（ここでは`value`や`ws`)を非終端記号と呼びます。非終端記号は同じBNFで定義されている規則の左辺と一致する必要があります。
  
この規則を日本語で表現すると、「`json`という名前の規則は、`ws`の後に`value`が続く」と読むことができます。`value`は、JSONの値を表しているので、jsonという規則は`ws`（空白文字）の後にJSONの値が続くものを表しています。

### object

`object`はJSONのオブジェクトを表す規則で、定義は以下のようになっています。

```
object = LBRACE RBRACE | LBRACE pair {COMMA pair} RBRACE;
```

`pair`の定義はのちほど出てきますので心配しないでください。

この規則によって`object`は

- ブレースで囲まれたもの（`LBRACE RBRACE`)である
  - `LBRACE`はLeft-Brace（開き波カッコ）の略で`{`を示しています
  - `RBRACE`はRight-Brace（閉じ波カッコ）の略で`}`を示しています
- `LBRACE`が来た後に、`pair`が1回出現して、さらにその後に、「`COMMA`（カンマ）とそれに続く`pair`」というペアが0回以上繰り返して出現した後、`RBRACE`が来る

のどちらかであることを表しています。ここで `{ ... }` は「0回以上の繰り返し」を表します（2章のBNFの説明を参照）。

具体的なJSONを当てはめてみましょう。以下のJSONは`LBRACE RBRACE`にマッチします。

```js
{}
```

以下のJSONは`LBRACE pair {COMMA pair} RBRACE`にマッチします。

```js
{"x":1}
{"x":1,"y":2}
{"x":1,"y":2,"z":3}
```

しかし、以下のテキストは、`object`に当てはまらず、エラーになります。`{COMMA pair}`とあるように、カンマは後ろにペアを必要とするからです。

```js
{"x":1,} // ,で終わっている
```

### pair

`pair`（ペア）は、JSONのオブジェクト内での`"x":1`に当たる部分を表現する規則です。`value`の定義については後述します。

```text
pair = string COLON value;
```

これによってペアは`:`（`COLON`）の前に文字列リテラル（`string`)が来て、その後にJSONの値（`value`）が来ることを表しています。`pair`にマッチするテキストとしては、

```
"x":1
"y":true
```

などがあります。一方で、以下のテキストは`pair`にマッチしません（JavaScriptのオブジェクトとJSONの違いについては後述のコラムを参照）。

```
x:1 // 文字列リテラルでないといけない
```

### COMMA

`COMMA`は、カンマを表す規則です。カンマそのものを表すには、単に`","`と書けばいいのですが、任意個の空白文字が続くことを表現したいため、規則`ws`（後述）を参照しています。

```text
COMMA = "," ws;
```

### array

`array`は、JSONの値の配列を表す規則です。

```text
array = LBRACKET RBRACKET | LBRACKET value {COMMA value} RBRACKET ;
```

`LBRACKET`は開き大カッコ（`[`）を、`RBRACKET`は閉じ大カッコ（`]`)を表しています。`value`の定義については後述します。

これによって`array`は、

- 大カッコで囲まれたもの（`LBRACKET RBRACKET`)である
- 開き大カッコ（`LBRACKET`）が来た後に、`value`が1回あらわれ、さらにその後に、「`COMMA`（カンマ）とそれに続く`value`」というペアが0回以上繰り返してあらわれた後、閉じ大カッコが来る（`RBRACKET`)

のどちらかであることを表しています。よく見ると、先程の`object`と同様の構造を持っていることがわかります。

`array`についても具体的なJSONを当てはめてみましょう。以下のJSONは`LBRACKET RBRACKET`にマッチします。

```js
[]
```

また、以下のJSONは`LBRACKET value {COMMA value} RBRACKET`にマッチします。

```js
[1]
[1, 2]
[1, 2, 3]
["foo"]
```

しかし、以下のテキストは、`array`に当てはまらず、エラーになります。`{COMMA value}`とあるように、カンマは必ず後ろに`value`を必要とするからです。

```js
[1,] // ,で終わっている
```

### value

```text
value = true | false | null | number | string | object | array;
```

`value`はJSONの値を表現する規則です。これは、JSONの値は、

- 真（`true`）
- 偽（`false`）
- ヌル（`null`）
- 数値（`number`)
- 文字列（`string`)
- オブジェクト（`object`）
- 配列（`array`）

のいずれかでなければいけない事を示しています。JSONを普段使っている皆さんにはお馴染みでしょう。

### true

`true`は、真を表すリテラルを表す規則です。

```text
true = "true" ws;
```

文字列 `true` が真を表すということでそのままですね。

### false

`false`は、偽を表すリテラルを表す規則です。構造的には、`true`と同じです。

```text
false = "false" ws;
```

### null

`null`は、ヌルリテラルを表す規則です。構造的には、`true`や`false`と同じです。

```text
null = "null" ws;
```

JSON仕様は`null`の意味付けを規定しません。多くの実装ではホスト言語の`null`相当の値に対応づけますが、`null`を表現するためのリテラルがない言語では、`Option`や`Maybe`といったデータ型に対応づけることもあります。

### number

`number`は、数値リテラルを表す規則です。

```text
number = INT ws;
```

整数（`INT`）に続いて、`ws`が来るのが`number`であるということを表現しています。簡単のため、本章では数値を整数に限定し、小数部や指数部は扱いません（実装拡張の話題は章末の演習を参照）。

### string

`string`は文字列リテラルを表す規則です。

```text
string = "\"" {CHAR} "\"" ws;
```

`"`で始まって、`CHAR`で定義される文字が0個以上続いて、 `"` で終わります。`CHAR`の定義はBNF中に含まれており、ダブルクォーテーションとバックスラッシュを除く印字可能文字を表しています。なお、簡単のため本章ではエスケープシーケンスは扱いません。

### JSONのBNFまとめ

JSONのBNFは、非常に少数の規則だけで表現することができます。読者の中には、あまりにも簡潔過ぎて驚かれた方もいるのではないでしょうか。しかし、これだけ単純であるにも関わらず、JSONのBNFは**再帰的に定義されている**ため、非常に複雑な構造も表現することができます。たとえば、

- 一要素の配列があり、その要素はオブジェクトであり、キー`"a"`に対応する要素の中に配列があって、その配列は空配列である

といったことも、JSONでは以下のように表現することができます。

```json
[{"a":[]}]
```

再帰的な規則は、構文解析において非常に重要な要素なので、これから本書を読み進める上でも念頭に置いてください。

## JSONの抽象構文木

JSONの定義と、文法について見てきました。構文解析器を実装する前に、まずJSONの抽象構文木（AST: Abstract Syntax Tree）をJavaでどのように表現するかを定義しましょう。

抽象構文木は、2章でも説明したとおり、プログラムの構造を表現するためのデータ構造です。重要な点は、抽象構文木では元の文字列に含まれていた「構文上のノイズ」が取り除かれることです。例えば：

- 空白文字（`ws`）は抽象構文木には含まれません
- カンマ（`,`）やコロン（`:`）などの区切り文字も、構造として表現されるため個別のノードにはなりません
- 括弧（`{}`、`[]`）も同様に、オブジェクトや配列という構造で表現されます

以下の`JsonAst`の定義を見ると、JSONの各要素をJavaのレコード・クラス（record）やインタフェースとして表現していることがわかります。これは、JSONという「文字列」を、Javaのオブジェクトという「構造化されたデータ」に変換するための定義です。

```java
public interface JsonAst {
    // value
    sealed interface JsonValue permits 
        JsonNull, JsonTrue, JsonFalse, 
        JsonNumber, JsonString, 
        JsonObject, JsonArray {}
    
    // NULL
    record JsonNull() implements JsonValue {
        @Override
        public String toString() {
            return "null";
        }
    }

    // TRUE
    record JsonTrue() implements JsonValue {
        @Override
        public String toString() {
            return "true";
        }
    }
    
    // FALSE
    record JsonFalse() implements JsonValue {
        @Override
        public String toString() {
            return "false";
        }
    }
    
    // NUMBER
    record JsonNumber(double value) implements JsonValue {
        @Override
        public String toString() {
            return "JsonNumber(" + value + ')';
        }
    }
    
    // STRING
    record JsonString(String value) implements JsonValue {
        @Override
        public String toString() {
            return "JsonString(\"" + value + "\")";
        }
    }
    
    // object
    record JsonObject(List<Pair<JsonString, JsonValue>> properties) 
        implements JsonValue {
        @Override
        public String toString() {
            return "JsonObject{" + properties + '}';
        }
    }
    
    // array
    record JsonArray(List<JsonValue> elements) 
        implements JsonValue {
        @Override
        public String toString() {
            return "JsonArray[" + elements + ']';
        }
    }
}
```

各クラスがBNFの左辺に対応しているのがわかるでしょうか。ただし、前述のとおり、`ws`や区切り文字（`,`、`:`）などは抽象構文木には現れません。これらは構文解析の過程で消費されますが、最終的なデータ構造には含まれないのです。

## JSONの構文解析器

この節では、BNFを元に、JSONのデータを**構文解析**するプログラムを実装していきます。以下のようなインタフェース`JsonParser`インタフェースを実装したクラスを「JSONの構文解析器」と考えることにします。

```java
package parser;
interface JsonParser {
    public ParseResult<JsonAst.JsonValue> parse(String input);
}
```

クラス`ParseResult<T>`は以下のようなジェネリックなクラスになっています。`value`は解析結果の値です。これは任意の型をとり得るので、`T`としています。また、`input`は「構文解析の対象となる文字列」を表します。

```java
public record ParseResult<T>(T value, String input) {}
```

インタフェース`JsonParser`は`parse()`メソッドだけを持ちます。`parse()`メソッドは、文字列`input`を受け取り、`ParseResult<JsonAst.JsonValue>`型を返します。

### 構文解析の戦略

実装に入る前に、これから作るJSONパーサーの解析戦略について説明しておきます。この章の実装は、PEG（Parsing Expression Grammar）に基づくトップダウン解析を採用します。

1. トップダウン解析: BNFの規則を上から順番に適用していく方式を採用します。例えば、`value`規則から始めて、それぞれの選択肢を試していきます。

2. バックトラック: 解析に失敗した場合、前の状態に戻って別の選択肢を試します。これにより、複数の可能性がある場合でも正しい解析結果を見つけることができます。

3. 順序付き選択（Ordered Choice）: BNFの `|` で区切られた選択肢を、左から右へ順番に試します。例えば、`value = true | false | null | number | string | object | array` の場合、まず`true`を試し、失敗したら`false`を試す、という具合です。この順序は重要で、より具体的なパターンを先に配置することで、正しい解析を保証します。実装の`parseValue()`メソッドでも、BNFの定義順序と同じ順番で各選択肢を試すようにしています。

4. 例外による失敗の表現: 構文解析の失敗は、`ParseException`という例外で表現します。これにより、深くネストした解析処理から簡潔に失敗を伝播させることができます。

本章の実装はPEGの順序付き選択とバックトラックという特徴を持っています。そのため、クラス名は`PegJsonParser`としています（PEGの詳細は第5章で解説します）。

### 構文解析器の全体像

それでは、JSONの構文解析器の実装を見ていきましょう。構文解析を行う各メソッドについては省略して、後ほど解説することにします。以下が`PegJsonParser`クラスの全体像です。

```java
package parser;

import java.util.ArrayList;
import java.util.List;

public class PegJsonParser implements JsonParser {
    private int cursor;
    private String input;

    private static class ParseException extends RuntimeException {
        public ParseException(String message) {
            super(message);
        }
    }

    public ParseResult<JsonAst.JsonValue> parse(String input) {
        this.input = input;
        this.cursor = 0;
        var value = parseValue();
        return new ParseResult<>(value, input.substring(this.cursor));
    }

    /**
     * 指定された文字列リテラルが現在のカーソル位置にあることを確認し、
     * マッチした場合はカーソルを進めます。
     * @param literal 認識すべき文字列リテラル
     * @throws ParseException リテラルがマッチしない場合
     */
    private void recognize(String literal) {
        if(input.substring(cursor).startsWith(literal)) {
            cursor += literal.length();
        } else {
            String substring = input.substring(cursor);
            int endIndex = cursor + 
              (literal.length() > substring.length() ? 
                substring.length() : literal.length());
            throw new ParseException(
                "expected: " + literal + 
                ", actual: " + input.substring(cursor, endIndex)
            );
        }
    }


    /**
     * 空白文字（スペース、タブ、改行、キャリッジリターン）を
     * スキップし、カーソルを次の非空白文字まで進めます。
     */
    private void skipWhitespace() {
        OUTER:
        while(cursor < input.length()) {
            char currentCharacter = input.charAt(cursor);
            switch (currentCharacter) {
                // 空白文字
                case ' ':       // Space
                case '\t':      // Horizontal Tab
                case '\n':      // Line Feed
                case '\r':      // Carriage Return
                    cursor++;
                    continue OUTER;
                default:
                    break OUTER;
            }
        }
    }

    /**
     * JSONの値（value）を解析します。
     * BNF: value = true | false | null | number | string | object | array
     * @return 解析されたJSON値
     * @throws ParseException 有効なJSON値が見つからない場合
     */
    private JsonAst.JsonValue parseValue() {
        // 後で解説
    }

  
    /**
     * true値を解析します。
     * BNF: true = "true" ws
     * @return JSONのtrue値
     * @throws ParseException "true"がマッチしない場合
     */
    private JsonAst.JsonTrue parseTrue() {
        // 後で解説
    }

    /** BNF: false = "false" ws */
    private JsonAst.JsonFalse parseFalse() {
        // 後で解説
    }

    /** BNF: null = "null" ws */
    private JsonAst.JsonNull parseNull() {
        // 後で解説
    }

    /** BNF: LBRACE = '{' ws */
    private void parseLBrace() {
        // 後で解説
    }

    /** BNF: RBRACE = '}' ws */
    private void parseRBrace() {
        // 後で解説
    }

    /** BNF: LBRACKET = '[' ws */
    private void parseLBracket() {
        // 後で解説
    }

    /** BNF: RBRACKET = ']' ws */
    private void parseRBracket() {
        // 後で解説
    }

    /** BNF: COMMA = ',' ws */
    private void parseComma() {
        // 後で解説
    }

    /** BNF: COLON = ':' ws */
    private void parseColon() {
        // 後で解説
    }

    /** BNF: string = ('""' | '"' {CHAR} '"') ws */
    private JsonAst.JsonString parseString() {
        // 後で解説
    }

    /** BNF: number = INT ws */
    private JsonAst.JsonNumber parseNumber() {
        // 後で解説
    }

    /** BNF: pair = string COLON value */
    private Pair<JsonAst.JsonString, JsonAst.JsonValue> parsePair() {
        // 後で解説
    }

    /** BNF: object = LBRACE RBRACE | LBRACE pair {COMMA pair} RBRACE */
    private JsonAst.JsonObject parseObject() {
        // 後で解説
    }

    /** BNF: array = LBRACKET RBRACKET | LBRACKET value {COMMA value} RBRACKET */
    public JsonAst.JsonArray parseArray() {
        // 後で解説
    }
}
```

このクラス`PegJsonParser`で重要なことは、クラスがフィールドとして以下を保持していることです。

```java
public class PegJsonParser implements JsonParser {
    private int cursor;
    private String input;
    // ...
}
```

構文解析器の実装スタイルには、大きく分けて「関数型（不変データ・純粋関数を重視）」と「手続き型（内部状態と更新を用いる）」があります。どちらも正しく実装すれば、同じ入力に対して同じ解析結果を返しますが、表現と状態管理の方法が異なります。本書では説明のしやすさから手続き型の実装を採用します。

また、`skipWhitespace()`メソッドと`recognize()`メソッドを定義して、空白文字の読み飛ばしと、特定の文字列の認識を行います。さらに、構文解析中にエラーが発生した場合は`ParseException`を投げることで、どこでどのような問題が発生したかを呼び出し元に伝えます。

`cursor`フィールドは現在の読み取り位置を、`input`フィールドは解析対象の文字列を保持します。エラーが発生した場合は`ParseException`を投げることで、どこでどのような問題が発生したかを呼び出し元に伝えます。

次からはいよいよ構文解析の各メソッドを見ていきます。

### valueの構文解析メソッド

まずは、`parseValue()`メソッドから始めましょう。


```java
    private JsonAst.JsonValue parseValue() {
        int backup = cursor;
        try {
            return parseString();
        } catch (ParseException e) {
            cursor = backup;
        }

        try {
            return parseNumber();
        } catch (ParseException e) {
            cursor = backup;
        }
        // parseObject(), parseArray(),
        // parseTrue(), parseFalse(), parseNull() と続く...
    }
```

`parseValue()`の実装は、BNFの`value = true | false | null | number | string | object | array`という定義に対応しています。

`|`は「または」を意味するので、「valueは、trueまたはfalseまたはnullまたは...」という意味になります。このコードでは、それぞれの可能性を順番に試していきます。

1. まず`parseString()`を試し、失敗したらカーソルを元に戻す
2. 次に`parseNumber()`を試し、失敗したらカーソルを元に戻す
3. 以下同様に続く...

このように、失敗したら元の位置に戻って別の可能性を試すことを「バックトラック」と呼びます。

なお、この順番自体は本実装では結果に影響しません。JSONの`value`では候補同士が先頭文字で互いに区別できる（`"`なら`string`、数字なら`number`、`{`なら`object`……）ため、どの順番で試しても同じ結果になります。ただし、後で出てくるPEGの`|`（順序付き選択）一般では「先に書いた候補が優先される」ので、順番を変えると結果が変わることがある、という点は覚えておくとよいでしょう。

### trueの構文解析メソッド

`true`の構文解析は、次のような `parseTrue()` メソッドとして定義します。

```java
private JsonAst.JsonTrue parseTrue() {
    recognize("true");
    skipWhitespace();
    return new JsonAst.JsonTrue();
}
```

このメソッドで行っていることを見ていきましょう。このメソッドでは、入力である`input`の現在位置が`"true"`という文字列で始まっているかをチェックします。もしそうなら、**JSONのtrue**をあらわす`JsonAst.JsonTrue`のインスタンスを返します。もし、先頭が`"true"`でなければ、構文解析は失敗なので例外を発生させますが、これは`recognize()`メソッドの中で行われています。`recognize()`の内部では、入力の現在位置と与えられた文字列を照合して、マッチしない場合例外を投げます。

次に、`skipWhitespace()`メソッドを呼び出して、「空白の読み飛ばし」を行っています。

### falseの構文解析メソッド

`false`の構文解析は、次のような `parseFalse()` メソッドとして定義します。

```java
private JsonAst.JsonFalse parseFalse() {
    recognize("false");
    skipWhitespace();
    return new JsonAst.JsonFalse();
}
```

これも、`parseTrue()`とほぼ同じですので、特に説明の必要はないでしょう。

### nullの構文解析メソッド

`null`の構文解析は、次のような `parseNull()` メソッドとして定義します。

```java
private JsonAst.JsonNull parseNull() {
    recognize("null");
    skipWhitespace();
    return new JsonAst.JsonNull();
}
```

これも、`parseTrue()`や`parseFalse()`とほぼ同じです。固定の文字列を解析するという点で、これらのメソッドは共通の処理になっています。

### 数値の構文解析メソッド

数値の構文解析は、次のような `parseNumber()` メソッドとして定義します。

```java
    private JsonAst.JsonNumber parseNumber() {
        // 本書の実装では、JSONの数値型を整数のみに限定して扱います。
        int start = cursor;
        char ch = 0;
        // オプショナルなマイナス記号
        if (cursor < input.length() && input.charAt(cursor) == '-') {
            cursor++;
        }

        // 数字の連続 (0, または 1-9 で始まり数字が続く)
        int digitsStart = cursor;
        if (cursor < input.length()) {
            ch = input.charAt(cursor);
            if (ch == '0') {
                cursor++;
            } else if ('1' <= ch && ch <= '9') {
                cursor++;
                while (cursor < input.length()) {
                    ch = input.charAt(cursor);
                    if (!('0' <= ch && ch <= '9')) break;
                    cursor++;
                }
            } else {
                // 数字で始まらない場合はエラー
                throw new ParseException(
                    "expected: digit actual: " + 
                    (cursor < input.length() ? input.charAt(cursor) : "EOF")
                );
            }
        } else {
             throw new ParseException("expected: digit actual: EOF");
        }

        if (digitsStart == cursor) {
             // 数字が一つも読まれなかった場合（"-"のみなど）
             throw new ParseException(
                "expected: digit after '-' actual: " + 
                (cursor < input.length() ? input.charAt(cursor) : "EOF")
             );
        }
        
        String numberStr = input.substring(start, cursor);
        try {
            double value = Double.parseDouble(numberStr);
            skipWhitespace();
            return new JsonAst.JsonNumber(value);
        } catch (NumberFormatException e) {
            throw new ParseException("invalid number format: " + numberStr);
        }
    }
```

`parseNumber()` メソッド（PegJsonParser内）では、入力文字列から数値部分を読み取り、`Double.parseDouble` を用いて数値に変換しています。この実装は、ECMA-404で定義されるJSONの数値型の完全な仕様（小数部、指数部を含む）には対応しておらず、整数のみを扱えるように単純化されています。

### 文字列の構文解析メソッド

文字列の構文解析は、次のような `parseString()` メソッドとして定義します。

```java
    private JsonAst.JsonString parseString() {
        if(cursor >= input.length()) {
            throw new ParseException("expected: \"" + " actual: EOF");
        }
        char ch = input.charAt(cursor);
        if(ch != '"') {
            throw new ParseException("expected: \"" + "actual: " + ch);
        }
        cursor++;
        var builder = new StringBuilder();
        OUTER:
        while(cursor < input.length()) {
            ch = input.charAt(cursor);
            switch(ch) {
                case '\\':
                    throw new ParseException(
                        "escape sequences are not supported in this parser"
                    );
                case '"':
                    cursor++;
                    break OUTER;
                default:
                    builder.append(ch);
                    cursor++;
                    break;
            }
        }

        if(ch != '"') {
            throw new ParseException("expected: " + "\"" + " actual: " + ch);
        } else {
            skipWhitespace();
            return new JsonAst.JsonString(builder.toString());
        }
    }
```

`while`文の中が若干複雑になっていますが、一つ一つ見ていきます。

まず、最初の部分では、

```java
        if(cursor >= input.length()) {
            throw new ParseException("expected: \"" + " actual: EOF");
        }
        char ch = input.charAt(cursor);
        if(ch != '"') {
            throw new ParseException("expected: \"" + "actual: " + ch);
        }
        cursor++;
```

- 入力が終端に達していないこと
- 入力の最初が`"`であること

をチェックしています。文字列は当然ながら、ダブルクォートで始まりますし、入力の終端にも達していないはずですから、それらの条件が満たされなければ例外が投げられるわけです。

`while`文の中では、各文字を読み込んで文字列を構築していきます。switch文の中で最も重要なのは`default`ケースで、ここで通常の文字を`StringBuilder`に追加して文字列を構築しています。ダブルクォート（`"`）が現れたら文字列の終端として処理し、バックスラッシュ（`\`）が現れた場合はエラーとして処理します。実際のJSONパーサーではエスケープシーケンスの処理が必要ですが、構文解析の本質を理解する上では必須ではないため、本書では省略しています。

`while`文が終わったあとで、

```java
        if(ch != '"') {
            throw new ParseException("expected: " + "\"" + " actual: " + ch);
        } else {
            skipWhitespace();
            return new JsonAst.JsonString(builder.toString());
        }
```

というチェックを入れることによって、ダブルクォートで文字列が終端している事を確認した後、空白を読み飛ばしています。


### 配列の構文解析メソッド

配列の構文解析は、次のような `parseArray()` メソッドとして定義します。

```java
    public JsonAst.JsonArray parseArray() {
        int backup = cursor;
        try {
            parseLBracket();
            parseRBracket();
            return new JsonAst.JsonArray(new ArrayList<>());
        } catch (ParseException e) {
            cursor = backup;
        }

        parseLBracket();
        List<JsonAst.JsonValue> values = new ArrayList<>();
        var value = parseValue();
        values.add(value);
        try {
            while (true) {
                backup = cursor;
                parseComma();
                value = parseValue();
                values.add(value);
            }
        } catch (ParseException e) {
            cursor = backup;
            parseRBracket();
            return new JsonAst.JsonArray(values);
        }
    }
```

この`parseArray()`は多少複雑になります。まず、先頭に`"["`が来るかチェックする必要があります。これをコードにすると、以下のようになります。

```java
parseLBracket();
```

`parseLBracket()`は以下のように定義されています。

```java
 private void parseLBracket() {
    recognize("[");
    skipWhitespace();
}
```

`recognize()`で、現在の入力位置が`[`と一致しているかチェックをした後、空白を読み飛ばしています。

`[`の次には任意の`JsonValue`または`"]"`が来る可能性があります。この時、まず最初に、`]`が来ると**仮定**するのがポイントです。

```java
        int backup = cursor;
        try {
            parseLBracket();
            parseRBracket();
            return new JsonAst.JsonArray(new ArrayList<>());
        } catch (ParseException e) {
            cursor = backup;
        }
```

仮定が成り立たなかった場合、`ParseException`がthrowされるはずですから、それをcatchして、バックアップした位置に巻き戻します。

`]`が来るという仮定が成り立たなかった場合、再び最初に`[`が出現して、その次に来るのは任意の`JsonValue`ですから、以下のようなコードになります。

```java
        parseLBracket();
        List<JsonAst.JsonValue> values = new ArrayList<>();
        var value = parseValue();
        values.add(value);
```

ローカル変数`values`は、配列の要素を格納するためのものです。

配列の中で、最初の要素が読み込まれた後、次に来るのは、`,`か`]`のどちらかですが、ひとまず、`,`が来ると仮定して`while`ループで

```java
parseComma();
value = parseValue();
values.add(value);
```

を繰り返します。この繰り返しは、1回ごとに必ず入力を1以上進めます。失敗した時は、テキストが正しいJSONなら、`]`が来るはずなので、

```java
parseRBracket();
return new JsonAst.JsonArray(values);
```

とします。もし、テキストが正しいJSONでない場合、`parseRBracket()`から例外が投げられるはずですが、その例外は**より上位の層が適切にリカバーしてくれると期待して**放置します。JSONのような再帰的な構造を解析する時、このような、「自分の呼び出し元が適切にやってくれるはず」（何故なら、自分はその呼び出し元で適切にcatchしているのだから）という考え方が重要になります。

多少複雑になりましたが、`parseArray()`の定義が、EBNFにおける表記

```text
array = LBRACKET RBRACKET | LBRACKET {value {COMMA value}} RBRACKET ;
```

に対応していることがわかるでしょうか 。読み方のポイントは、`|`の後を、例外をキャッチした後の処理ととらえることです。

### オブジェクトの構文解析メソッド

オブジェクトの構文解析は、次のような `parseObject()` メソッドとして定義します。

```java
    /** BNF: object = LBRACE RBRACE | LBRACE pair {COMMA pair} RBRACE */
    private JsonAst.JsonObject parseObject() {
        int backup = cursor;
        try {
            parseLBrace();
            parseRBrace();
            return new JsonAst.JsonObject(new ArrayList<>());
        } catch (ParseException e) {
            cursor = backup;
        }

        parseLBrace();
        List<Pair<JsonAst.JsonString, JsonAst.JsonValue>> members =
            new ArrayList<>();
        var member = parsePair();
        members.add(member);
        try {
            while (true) {
                backup = cursor;
                parseComma();
                member = parsePair();
                members.add(member);
            }
        } catch (ParseException e) {
            cursor = backup;
            parseRBrace();
            return new JsonAst.JsonObject(members);
        }
    }
```

この定義を見て、ひょっとしたら、

「あれ？これ、`parseArray()`とほとんど同じでは」

と気づかれた読者の方が居るかも知れません。実際、`parseObject()`がやっていることは`parseArray()`と非常に類似しています。

最初に、

```java
parseLBrace();
parseRBrace();
return new JsonAst.JsonObject(new ArrayList<>());
```

としている箇所は、`{}`という形の空オブジェクトを読み取ろうとしていますが、これは、空配列`[]`を読み取るコードとほぼ同じです。

続くコードも、対応する記号が`{}`か`[]`の違いこそあるものの、基本的に同じです。唯一の違いは、オブジェクトの各要素は、`name:value`というペアなため、`parseValue()`の代わりに`parsePair()`を呼び出しているところくらいです。

そして、`parsePair()`は以下のように定義されています。

```java
    private Pair<JsonAst.JsonString, JsonAst.JsonValue> parsePair() {
        var key = parseString();
        parseColon();
        var value = parseValue();
        return new Pair<>(key, value);
    }
```

これはEBNFにおける以下の定義にそのまま対応しているのがわかるでしょう。

```
pair = string COLON value;
```

### 構文解析における再帰

配列やオブジェクトの構文解析メソッドを見るとわかりますが、

- `parseArray() -> parseValue() -> parseArray()`
- `parseArray() -> parseValue() -> parseObject()`
- `parseObject() -> parseValue() -> parseObject()`
- `parseObject() -> parseValue() -> parseArray()`

のような再帰呼び出しが起こり得ることがわかります。このような再帰呼び出しでは、各ステップで必ず構文解析が1文字以上進むため、JSONがどれだけ深くなっても（スタックが溢れない限り）うまく構文解析ができるのです。

### 構文解析とPEG

このようにしてJSONの構文解析器を実装することができました。

実は、ここで作った構文解析器には、以下のような特徴があります：

1. **バックトラック**：失敗したら元の位置に戻って別の可能性を試す
2. **順序付き選択**：`|`で区切られた選択肢を左から順番に試す
3. **文字列を直接解析**：特別な前処理なしに、入力文字列をそのまま解析する

このような構文解析の手法を**PEG（Parsing Expression Grammar、解析表現文法）**と呼びます。PEGは2004年に提案された比較的新しい手法で、プログラミング言語のような「曖昧さがない」言語の解析に適しています。最近ではPython（バージョン3.9以降）もPEGベースの構文解析器を使っています[^pep617]。

[^pep617]: PythonにおけるPEGパーサ採用の経緯と理由は [PEP 617 -- New PEG parser for CPython](https://peps.python.org/pep-0617/) で詳しく説明されています。

厳密にはPEG自体はBNFと異なる文法の定義方法（形式文法）ですが、PEG自体が「どのように解析されるか」を定義するため、ここではPEG自体を構文解析の手法として扱います。この点については第5章で詳しく解説します。

PEGは直感的でシンプルなので、最初に学ぶのに適しています。ただし、従来から使われている別の構文解析手法も重要です。次の節では、その伝統的な手法について解説します。

## 古典的な構文解析器

前節では、PEGという手法を使って構文解析器を作りました。しかし、LL/LR系（例：LL(1)、LR(1)）に代表される伝統的な構文解析の手法では、少し違ったアプローチをとります。

伝統的な手法では、構文解析を以下の2つのステップに分けます：

1. **字句解析（トークナイザー）**：文字列を「トークン」という単位に分割する
2. **構文解析（パーサー）**：トークンの列から構造を組み立てる

たとえば、以下の英文があったとします。

```
We are parsers.
```

我々は構文解析器であるというジョーク的な文ですが、それはさておき、この文は

```
[We, are, parsers]
```

という三つのトークン（単語）に分解すると考えるのが字句解析の発想法です。

古典的な構文解析の世界では、字句解析が必須とされていましたが、それは後の章で説明される構文解析アルゴリズムの都合に加えて、空白のスキップという処理を字句解析で行えるからでもあります。

前節で出てきたJSONの構文解析器では`skipWhitespace()`の呼び出しが頻出していましたが、字句解析器を使う場合、空白を読み飛ばす処理を先に行うことで、構文解析器では空白の読み飛ばしという作業をしなくてよくなります。

この点はトレードオフがあって、たとえば、空白に関する規則がある言語（Pythonのオフサイドルール＝インデントでブロックを表す規則など）では、字句解析という前処理を安易に入れると逆に扱いづらくなることすらあります。ともあれ、字句解析という前処理を通すことには一定のメリットがあるのは確かです。

以下では字句解析器を使った構文解析器の全体像を示します。ここでは、コアとなるアイデアに絞って説明します。完全なソースコードは巻末の付録を参照してください。

### JSONの字句解析器

JSONの字句解析器（トークナイザー）の基本構造は次のようになります。

```java
public class SimpleJsonTokenizer implements JsonTokenizer {
    private final String input;
    private int index;

    // すべての入力を一度にトークン化
    public List<Token> tokenizeAll() {
        List<Token> tokens = new ArrayList<>();
        while (moveNext()) {
            tokens.add(current());
        }
        tokens.add(new Token(Token.Type.EOF, null));
        return tokens;
    }

    // 次のトークンを読み取る
    private boolean moveNext() {
        skipWhitespace();
        
        if (index >= input.length()) {
            return false;
        }
        
        char ch = input.charAt(index);
        switch (ch) {
            case '"':
                return tokenizeStringLiteral();
            case '{':
                accept("{", Token.Type.LBRACE, "{");
                return true;
            case '[':
                accept("[", Token.Type.LBRACKET, "[");
                return true;
            // 他のトークンも同様に処理...
        }
    }
    
    // 文字列トークンの読み取り（エスケープシーケンス非対応）
    private boolean tokenizeStringLiteral() {
        if(input.charAt(index) != '"') return false;
        index++;
        var builder = new StringBuilder();
        while(index < input.length()) {
            char ch = input.charAt(index);
            if(ch == '"') {
                fetched = new Token(Token.Type.STRING, builder.toString());
                index++;
                return true;
            }
            builder.append(ch);
            index++;
        }
        return false;
    }
}
```

このコードは`SimpleJsonTokenizer`クラスの基本構造を示しています。主な特徴は以下の通りです：

- `tokenizeAll()`：
    - 入力文字列全体を一度に処理してトークン列を生成
    - `moveNext()`を繰り返し呼び出してトークンを収集
    - 最後に`EOF`（End Of File）トークンを追加

- `moveNext()`：
    - まず空白文字をスキップ
    - 現在の文字を見て、トークンの種類を判定
    - switch文で各文字に応じた処理を実行
        - `"` → 文字列リテラルの処理
        - `{`、`[` → 括弧トークンの生成
        - その他の文字も同様にパターンマッチング

- `tokenizeStringLiteral()`メソッド：
    - ダブルクォートで囲まれた文字列を読み取る
    - 閉じクォートが見つかるまで文字を収集
    - エスケープシーケンス非対応

この設計により、字句解析の責任が明確になり、構文解析器は純粋にトークンの構造解析に専念できます。

### JSONの構文解析器

この字句解析器を使った構文解析器の基本構造を示します。トークナイザーが一度にすべてのトークンを生成し、パーサーはそのトークン列を処理します。

```java
public class SimpleJsonParser implements JsonParser {
    private List<Token> tokens;
    private int index;

    public ParseResult<JsonAst.JsonValue> parse(String input) {
        // 入力を一度に完全にトークン化
        var tokenizer = new SimpleJsonTokenizer(input);
        this.tokens = tokenizer.tokenizeAll();
        this.index = 0;
        var value = parseValue();
        return new ParseResult<>(value, "");
    }
    
    private Token current() {
        if (index < tokens.size()) {
            return tokens.get(index);
        }
        return new Token(Token.Type.EOF, null);
    }
    
    private boolean moveNext() {
        if (index < tokens.size() - 1) {
            index++;
            return true;
        }
        return false;
    }

    // トークン列から抽象構文木を構築
    private JsonAst.JsonValue parseValue() {
        // 中身は後で解説
    }

    // オブジェクトの解析（トークンベース）
    private JsonAst.JsonObject parseObject() {
        // 中身は後で解説
    }
    
    // 配列の解析（トークンベース）
    private JsonAst.JsonArray parseArray() {
        // 中身は後で解説
    }
    // 他のメソッドも同様に実装
}
```

このアプローチの重要な点は、**字句解析と構文解析の責任が明確に分離されている**ことです。`tokenizeAll()`メソッドが最初に入力全体をトークン列に変換し、パーサーはそのトークン列を処理します。これにより：

1. 関心の分離: 字句解析器はトークンの認識に、構文解析器は構造の構築に専念できます
2. デバッグの容易さ: トークン列を事前に確認できるため、問題の切り分けが簡単です
3. 実装の簡潔さ: 各コンポーネントが単一の責任を持つため、コードがシンプルになります

構文解析器の`parseXXX()`メソッドを見ると、文字列の代わりにトークン列を処理していることがわかります。また、この構文解析器には空白の読み飛ばしに関する処理が入っていません。これは、字句解析器が空白を処理済みだからです。

PEG版と異なり、途中で失敗したら後戻り（バックトラック）するという処理も存在しません。トークン列が事前に確定しているため、より決定的な解析が可能になります。

以下では構文解析のための各メソッドの詳細を説明します。残りのコードは、巻末の付録に掲載しています。

### valueの構文解析メソッド（トークンベース）

`parseValue()`メソッドは、JSONの値を解析するためのメソッドです。これは、BNFで定義された`value = true | false | null | number | string | object | array`に対応しています。実装は以下のようになります。

```java
    private Ast.JsonValue parseValue() {
        var token = current();
        switch(token.type) {
            case TRUE:
                return parseTrue();
            case FALSE:
                return parseFalse();
            case NULL:
                return parseNull();
            case INTEGER:
                return parseNumber();
            case STRING:
                return parseString();
            case LBRACE:
                return parseObject();
            case LBRACKET:
                return parseArray();
        }
        throw new RuntimeException("cannot reach here");
    }
```

`current()`メソッドは、現在のトークンを取得するためのメソッドで、トークン列から現在の位置のトークンを返します。`switch`文では、先頭のトークンの種類に応じて適切な解析メソッドを呼び出しています。バックトラックが必要ないため、各トークンの種類に応じて直接対応するメソッドを呼び出す形になっています。

### parseObject

`parseObject()`メソッドは、規則`object`に対応するメソッドで、JSONのオブジェクトリテラルに対応するものを解析するメソッドでもあります。実装を示すと以下のようになります：

```java
    private JsonAst.JsonObject parseObject() {
        if(current().type != Token.Type.LBRACE) {
            throw new ParseException(
                "expected `{`, actual: " + current().value
            );
        }

        moveNext();
        if(current().type == Token.Type.RBRACE) {
            return new JsonAst.JsonObject(new ArrayList<>());
        }

        List<Pair<JsonAst.JsonString, JsonAst.JsonValue>> members = 
            new ArrayList<>();
        var pair= parsePair();
        members.add(pair);

        while(moveNext()) {
            if(current().type == Token.Type.RBRACE) {
                return new JsonAst.JsonObject(members);
            }
            if(current().type != Token.Type.COMMA) {
                throw new ParseException(
                    "expected: `,`, actual: " + current().value
                );
            }
            moveNext();
            pair = parsePair();
            members.add(pair);
        }

        throw new ParseException("unexpected EOF");
    }
```

まず、最初のif文で、次のトークンが`{`であることを確認した後に、

- その次のトークンが`}`であった場合：空オブジェクトを返す
- それ以外の場合： `parsePair()` を呼び出し、 `string:value` のようなペアを解析した後、以下のループに突入：
  - 次のトークンが`}`の場合、集めたペアのリストを引数として、`JsonAst.JsonObject()`オブジェクトを作って返す
  - それ以外で、次のトークンが`,`でない場合、構文エラーを投げて終了
  - それ以外の場合：次のトークンをフェッチして来て、`parsePair()`を呼び出して、ペアを解析した後、リストにペアを追加

のような動作を行います。実際のJSONのオブジェクトと対応付けてみると、より理解が進むでしょう。

### parseArray

`parseArray()`メソッドは、規則`array`に対応するメソッドで、JSONの配列リテラルに対応するものを解析するメソッドでもあります。実装を示すと以下のようになります：

```java
    /** BNF: array = LBRACKET RBRACKET | LBRACKET value {COMMA value} RBRACKET */
    private JsonAst.JsonArray parseArray() {
        if(current().type != Token.Type.LBRACKET) {
            throw new ParseException(
                "expected: `[`, actual: " + current().value
            );
        }

        moveNext();
        if(current().type == Token.Type.RBRACKET) {
            return new JsonAst.JsonArray(new ArrayList<>());
        }

        List<JsonAst.JsonValue> values = new ArrayList<>();
        var value = parseValue();
        values.add(value);

        while(moveNext()) {
            if(current().type == Token.Type.RBRACKET) {
                return new JsonAst.JsonArray(values);
            }
            if(current().type != Token.Type.COMMA) {
                throw new ParseException(
                    "expected: `,`, actual: " + current().value
                );
            }
            moveNext();
            value = parseValue();
            values.add(value);
        }

        throw new ParseException("unexpected EOF");
    }
 ```

まず、最初のif文で、次のトークンが`[`であることを確認した後に、

- その次のトークンが`]`であった場合：空の配列 (`JsonAst.JsonArray`) を返す
- それ以外の場合： `parseValue()` を呼び出し、 `value`を解析した後、以下のループに突入：
  - 次のトークンが`]`の場合、集めた`values`のリストを引数として、`JsonAst.JsonArray()`オブジェクトを作って返す
  - それ以外で、次のトークンが`,`でない場合、構文エラーを投げて終了
  - それ以外の場合：次のトークンをフェッチして来て、`parseValue()`を呼び出して、`value`を解析した後、リストに`value`を追加

のような動作を行います。実際のJSONの配列と対応付けてみると、より理解が進むでしょう。

`parseArray()`のコードを読めばわかるように、ほとんどのコードは、`parseObject()`と共通のものになっています。もしこれが気になるようであれば、共通部分をくくりだすことも出来ます。他の割愛したメソッドも同様に、トークンを読み取って期待される構文を解析するという流れになっています。

### 字句解析器と構文解析器の連携

字句解析器を使った構文解析の流れを、`{"key": "value"}` を例に説明します：

1. **字句解析フェーズ**: `tokenizeAll()`が入力全体を一度にトークン列に変換

```
入力: {"key": "value"}
↓
トークン列: [LBRACE, STRING("key"), COLON, STRING("value"), RBRACE, EOF]
```

2. **構文解析フェーズ**: パーサーがトークン列を走査しながら抽象構文木を構築

- index == 0: `LBRACE`を見て`parseObject()`を呼び出し
- index == 1: `STRING("key")`を読み取り
- index == 2: `COLON`を確認
- index == 3: `STRING("value")`を`parseValue()`で処理
- index == 4: `RBRACE`で終了を確認
- 最終的にオブジェクトの抽象構文木を生成

この方式ではトークン列が事前に確定しているため、以下の利点があります：

- パーサーは純粋に構造の解析に集中できる
- トークン列をログ出力してデバッグが容易

## PEGベースの構文解析器と伝統的な構文解析器の違い

前節のPEGベースの構文解析器と、この節の字句解析器を使った構文解析器の主な違いは以下の通りです：

1. 関心の分離: 字句解析と構文解析が明確に分離されている
2. 空白の処理: 字句解析器が空白を処理するため、構文解析器は空白を意識しない
3. バックトラック: 字句解析器を使う方式では通常バックトラックを行わない
4. 性能: 一般的に字句解析器を使う方式の方が高速

PEGベースの構文解析器にはいいとこなしのように見えますが、PEGべースの構文解析器は、以下のような利点もあります：

1. シンプルな実装: PEGは直感的で、構文解析のロジックが大幅に簡潔に表現できる
2. 再帰的な構造の自然な表現: 再帰的な文法を自然に扱えるため、特にネストされた構造の解析が容易

PEGベースの構文解析器は、トークン列を生成する必要がないため、文字列補間のような、一見トークン化が難しいケースでも、構文解析器の中で直接文字列を解析できるという利点があります。

## まとめ

この章では、JSONの構文解析や字句解析を実際に作ってみることを通して、構文解析の基礎について学んでもらいました。特に、

- JSONの概要
- JSONのBNF
- JSONの構文解析器（PEG版）
- 古典的な構文解析器
- JSONの字句解析器
- JSONの構文解析器

といった順番で、JSONの定義から入って、PEGによるJSONパーサー、字句解析器を使った構文解析器の作り方について学んでもらいました。この書籍中で使ったJSONはECMA-404で定義されている正式なJSONのサブセットになっています。たとえば、浮動小数点数が完全に扱えないという制限がありますが、構文解析器全体から見ればささいなことなので、この章を理解出来れば、JSONの構文解析について理解できたと思って構いません。

次の章では、文脈自由文法（Context-Free Grammar, CFG）の考え方について学んでもらいます。文脈自由文法は、現在使われているほとんどの構文解析アルゴリズムの基盤となっている概念であって、CFGの理解なくしては、その後の構文解析の理解もおぼつかないからです。

逆に、CFGの考え方さえわかってしまえば、個別の構文解析アルゴリズム自体は、それほど難しいとは感じられなくなって来るかもしれません。

---

**演習問題**

1. コメントのサポート:*

- JSONのBNF定義を拡張し、`//` から行末までの単一行コメントと、`/*` から `*/` までの複数行コメントをサポートするようにしてください。
  - `PegJsonParser` と `SimpleJsonTokenizer` の両方を修正し、これらのコメントを正しく無視するように実装してください。
  -  ヒント: `PegJsonParser` では `skipWhitespace` にコメントスキップのロジックを追加するか、各解析メソッドの適切な箇所でコメントを読み飛ばす処理を挟みます。`SimpleJsonTokenizer` では `moveNext` の `switch` 文にケースを追加し、そこからコメントの種別を判定して読み飛ばす処理を実装します。

2. 数値型の拡張:

- `PegJsonParser` の `parseNumber` メソッドと、`SimpleJsonTokenizer` の `tokenizeNumber` メソッドを修正し、ECMA-404仕様に準拠した数値型（小数部、指数部 `e` または `E` を含む）を正しく解析できるようにしてください。
  - `JsonAst.JsonNumber` の `value` フィールドの型を `double` から `java.math.BigDecimal` に変更し、精度が失われないように対応してください。
  - テストケースとして、`123`, `-0.5`, `1.2e3`, `0.4E-1` のような多様な数値表現を試してみてください

[^2]: ECMA-404 The JSON data interchange syntax 2nd edition, December 2017.  https://ecma-international.org/publications-and-standards/standards/ecma-404/
