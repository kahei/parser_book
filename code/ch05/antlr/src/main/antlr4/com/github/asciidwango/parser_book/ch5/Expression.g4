grammar Expression;

expression returns [int e]
    : v=additive {$e = $v.e;}
    ;

additive returns [int e = 0;]
    : l=multitive {$e = $l.e;} (
      '+' r=multitive {$e = $e + $r.e;}
    | '-' r=multitive {$e = $e - $r.e;}
    )*
    ;

multitive returns [int e = 0;]
    : l=primary {$e = $l.e;} (
      '*' r=primary {$e = $e * $r.e;}
    | '/' r=primary {$e = $e / $r.e;}
    )*
    ;

primary returns [int e]
    : n=NUMBER {$e = Integer.parseInt($n.getText());}
    | '(' x=expression ')' {$e = $x.e;}
    ;

LP : '(' ;
RP : ')' ;
NUMBER : INT ;
fragment INT :   '0' | [1-9] [0-9]* ; // no leading zeros
WS  :   [ \t\n\r]+ -> skip ;
