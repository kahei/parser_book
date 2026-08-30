grammar LRExpression;

expression returns [int e]
    : v=additive {$e = $v.e;}
    ;

additive returns [int e]
    : l=additive op='+' r=multitive {$e = $l.e + $r.e;}
    | l=additive op='-' r=multitive {$e = $l.e - $r.e;}
    | v=multitive {$e = $v.e;}
    ;

multitive returns [int e]
    : l=multitive op='*' r=primary {$e = $l.e * $r.e;}
    | l=multitive op='/' r=primary {$e = $l.e / $r.e;}
    | v=primary {$e = $v.e;}
    ;

primary returns [int e]
    : n=NUMBER {$e = Integer.parseInt($n.getText());}
    | '(' x=expression ')' {$e = $x.e;}
    ;

LP
   : '('
   ;

RP : ')'
   ;

NUMBER
    : INT
    ;

fragment INT :   '0' | [1-9] [0-9]* ; // no leading zeros

WS  :   [ \t\n\r]+ -> skip ;
