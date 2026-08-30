all: parser

parser: y.tab.c lex.yy.c
	gcc -o parser y.tab.c lex.yy.c

lex.yy.c: token.l y.tab.h
	flex token.l

y.tab.c y.tab.h: calculator.y
	yacc -d calculator.y

clean:
	rm -f parser y.tab.c y.tab.h lex.yy.c
