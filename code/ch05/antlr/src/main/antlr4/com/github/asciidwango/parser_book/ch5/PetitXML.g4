grammar PetitXML;
@parser::header {
    import static com.github.asciidwango.parser_book.ch5.PetitXML.*;
    import java.util.*;
}

root returns [Element e]
    : v=element {$e = $v.e;}
    ;

element returns [Element e]
    : ('<' begin=NAME '>' es=elements '</' end=NAME '>' {$begin.text.equals($end.text)}?
      {$e = new Element($begin.text, $es.es);})
    | ('<' name=NAME '/>' {$e = new Element($name.text);})
    ;

elements returns [List<Element> es]
    : { $es = new ArrayList<>();} (element {$es.add($element.e);})*
    ;

LT: '<';
GT: '>';
SLASH: '/';
NAME:  [a-zA-Z_][a-zA-Z0-9]* ;

WS  :   [ \t\n\r]+ -> skip ;
