grammar Javamm;

@header {
    package pt.up.fe.comp2024;
}

EQUALS : '=';
SEMI : ';' ;
LCURLY : '{' ;
RCURLY : '}' ;
LPAREN : '(' ;
RPAREN : ')' ;
LSQUARE: '[' ;
RSQUARE: ']' ;
MUL : '*' ;
ADD : '+' ;
ELLIPSIS : '...';

COMMA: ',' ;

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
INTEGER : [0-9] ;
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
    : expr op= MUL expr #BinaryExpr //
    | expr op= ADD expr #BinaryExpr //
    | value=INTEGER #IntegerLiteral //
    | name=ID #VarRefExpr //
    ;