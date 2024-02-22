grammar Javamm;

@header {
    package pt.up.fe.comp2024;
}

EQUALS : '=';
SEMI : ';' ;
COMMA : ',' ;
STOP : '.' ;
LCURLY : '{' ;
RCURLY : '}' ;
LPAREN : '(' ;
RPAREN : ')' ;
LSQUARE: '[' ;
RSQUARE: ']' ;
MUL : '*' ;
DIV : '/' ;
ADD : '+' ;
ELLIPSIS : '...';
SUB : '-' ;
AND : '&&' ;
OR : '||' ;
NEGATION : '!' ;
LESS : '<' ;
LESSEQ : '<=' ;
GREATER : '>' ;
GREATEREQ : '>=';
FALSE : 'false' ;
TRUE : 'true' ;
THIS : 'this' ;
NEW : 'new' ;

CLASS : 'class' ;
INT : 'int' ;
BOOL : 'boolean' ;
PUBLIC : 'public' ;
RETURN : 'return' ;
IMPORT : 'import' ;
IF : 'if' ;
ELSE : 'else' ;
WHILE: 'while' ;

STATIC: 'static';
MAIN: 'main';
VOID: 'void';
STRING: 'String';
EXTENDS : 'extends' ;

ID : (LETTER)(LETTER | INTEGER)*;
INTEGER : [1-9][0-9]* ;
LETTER: [a-zA-Z_$];

MULTI_LINE_COMMENT_START_MARKER: '/*' ;
MULTI_LINE_COMMENT_END_MARKER: '*/' ;
SINGLE_LINE_COMMENT_MARKER: '//';

SINGLE_LINE_COMMENT : SINGLE_LINE_COMMENT_MARKER ~[\n]* -> skip;
MULTI_LINE_COMMENT: (MULTI_LINE_COMMENT_START_MARKER .*? MULTI_LINE_COMMENT_END_MARKER) -> skip ;

WS : [ \t\n\r\f]+ -> skip ;

program
    :  classDecl EOF
    ;

importDecl
    : IMPORT ID ('.' ID)* SEMI
    ;


classDecl
    : importDecl*
        CLASS name=ID
        (EXTENDS ID)?
        LCURLY
        varDecl* methodDecl*
        RCURLY
    ;

varDecl
    : type name=ID SEMI
    ;



methodDecl locals[boolean isPublic=false]
    : (PUBLIC {$isPublic=true;})?
        type name=ID
        LPAREN (params+=param (',' params+=param)*)? RPAREN
        LCURLY varDecl* stmt* RCURLY # NormalMethod
    | mainMethodDecl # MainMethod
    ;

mainMethodDecl
    : (PUBLIC)? STATIC VOID name=MAIN LPAREN STRING '[]' name_of_param RPAREN
              LCURLY varDecl* stmt* RCURLY
    ;

// É preciso ter atenção de que depois a visitar os nós temos de ver se quando encontrarmos um VarArgType, eles está no fim
type
    : name=INT '[]' #ArrayType
    | name=INT ELLIPSIS #VarArgType
    | name= INT #IntegerType
    | name= BOOL #BoolType
    | name= STRING #StringType
    | name= ID #AbstractDataType
    ;

name_of_param
    : name=ID
    ;

param
    : type name=ID
    ;

stmt
    : expr EQUALS expr SEMI #AssignStmt //
    | RETURN expr SEMI #ReturnStmt
    | LCURLY stmt* RCURLY #ScopeStmt
    | IF LPAREN expr RPAREN stmt ELSE stmt #IfElseStmt
    | WHILE LPAREN expr RPAREN stmt #WhileStmt
    | expr SEMI #ExprStmt
    | ID EQUALS expr SEMI #VarAssignStmt
    | ID LSQUARE expr RSQUARE EQUALS expr SEMI #VarListAssignStmt
    ;

expr
    : op=LPAREN expr op=RPAREN #Parenthesis                                     // ()
    | expr op=LSQUARE expr op=RSQUARE #AccessArray                              // aceder a um array, i.e., a[2]
    | expr op=STOP expr LPAREN (expr (COMMA expr)* )? RPAREN #VarMethod         // foo.bar(), foo.bar(...)
    | expr op=STOP expr #VarVar                                                 // foo.bar
    | op=LSQUARE expr (op=COMMA expr)* op=RSQUARE #InitArray                    // inicializar array, i.e., [1,2,3]
    | op=NEGATION expr #Unary                                                   // !
    // necessita de ('[]') senao nao reconhece "new int[]" (n sei pq)
    | op=NEW INT ('[]' | LSQUARE expr? RSQUARE)   #NewInt                       // new int
    | op=NEW expr LPAREN (expr (COMMA expr)* )? RPAREN  #NewClass               // new class
    | expr (op=MUL | op=DIV) expr #BinaryExpr                                   // * ; /
    | expr (op=ADD | op=SUB) expr #BinaryExpr                                   // + ; -
    | expr (op=LESS | op=GREATER | op=LESSEQ | op=GREATEREQ) expr #Relational   // < ; > ; <= ; >=
    | expr op=AND expr #BoolOperator                                            // &&
    | expr op=OR expr #BoolOperator                                             // ||
    | value=INTEGER #IntegerLiteral                                             // numeros
    | value=THIS #This                                                          // keyword: "this"
    | (value=TRUE | value=FALSE) #Bool                                          // keywords: "true" ; "false"
    | name=ID #VarRefExpr                                                       // vars
    ;