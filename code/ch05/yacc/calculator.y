%{
#include <stdio.h>
int yylex(void);
void yyerror(char const *s);
int yywrap(void) {return 1;}
extern int yylval;
%}

%token NUM
%token EOL
%left '+' '-'
%left '*' '/'
%nonassoc UMINUS

%%

input :
      | input line
      ;

line : expr EOL
      {
        printf("Result: %d\n", $1);
      }
      | EOL
      | error EOL
      {
        yyerrok;
      }
      ;

expr : NUM
      {
        $$ = $1;
      }
      | expr '+' expr
      {
        $$ = $1 + $3;
      }
      | expr '-' expr
      {
        $$ = $1 - $3;
      }
      | expr '*' expr
      {
        $$ = $1 * $3;
      }
      | expr '/' expr
      {
        if ($3 == 0)
        {
          yyerror("Cannot divide by zero.");
          YYERROR;
        }
        else
        {
          $$ = $1 / $3;
        }
      }
      | '(' expr ')'
      {
        $$ = $2;
      }
      | '-' expr %prec UMINUS
      {
        $$ = -$2;
      }
      ;

%%

void yyerror(char const *s)
{
  fprintf(stderr, "Parse error: %s\n", s);
}

int main()
{
  yyparse();
}
