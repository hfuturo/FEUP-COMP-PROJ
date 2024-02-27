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
NEGATION : '!' ;
LESS : '<' ;
FALSE : 'false' ;
TRUE : 'true' ;
THIS : 'this' ;
NEW : 'new' ;
LENGTH : 'length' ;

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
EXTENDS : 'extends' ;

MULTI_LINE_COMMENT_START_MARKER: '/*' ;
MULTI_LINE_COMMENT_END_MARKER: '*/' ;
SINGLE_LINE_COMMENT_MARKER: '//';

SINGLE_LINE_COMMENT : SINGLE_LINE_COMMENT_MARKER ~[\n]* -> skip;
MULTI_LINE_COMMENT: (MULTI_LINE_COMMENT_START_MARKER .*? MULTI_LINE_COMMENT_END_MARKER) -> skip ;

ID : (LETTER)(LETTER | INTEGER)*;
INTEGER : [0-9][0-9]* ;
LETTER: [a-zA-Z_$];

WS : [ \t\n\r\f]+ -> skip ;

program
    :  classDeclRule EOF
    ;

importDecl
    : IMPORT names+=ID ('.' names+=ID)* SEMI # Import
    ;


classDeclRule
    : importDecl*
        CLASS name=ID
        (EXTENDS ultraSuper=ID)?
        LCURLY
        varDecl* methodDeclRule*
        RCURLY # ClassDecl
    ;

varDecl
    : type name=ID SEMI
    ;

methodDeclRule locals[boolean isPublic=false]
    : (PUBLIC {$isPublic=true;})?
        type name=ID
        LPAREN (param (',' param)*)? RPAREN
        LCURLY varDecl* stmt* RCURLY # MethodDecl
    | mainMethodDecl # MainMethod
    ;

mainMethodDecl
    : (PUBLIC)? STATIC VOID MAIN LPAREN 'String' '[]' name=ID RPAREN
              LCURLY varDecl* stmt* RCURLY #MainMethodTest
    ;

// É preciso ter atenção de que depois a visitar os nós temos de ver se quando encontrarmos um VarArgType, eles está no fim
type
    : name= ID #AbstractDataType
    | name=INT '[]' #ArrayType
    | name=INT ELLIPSIS #VarArgType
    | name= INT #IntegerType
    | name= BOOL #BoolType
    | name= 'String' #StringType
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
    | expr op=STOP name=ID LPAREN (expr (COMMA expr)* )? RPAREN #VarMethod         // foo.bar(), foo.bar(...)
    | expr op=STOP LENGTH #VarVar                                               // foo.length
    | op=LSQUARE (expr (op=COMMA expr)* )? op=RSQUARE #InitArray                // inicializar array, i.e., [1,2,3]
    | op=NEGATION expr #Unary                                                   // !
    | op=NEW INT LSQUARE expr RSQUARE   #NewInt                                 // new int
    | op=NEW name=ID LPAREN RPAREN  #NewClass                                   // new class
    | expr (op=MUL | op=DIV) expr #BinaryExpr                                   // * ; /
    | expr (op=ADD | op=SUB) expr #BinaryExpr                                   // + ; -
    | expr op=LESS expr #Relational                                             // <
    | expr op=AND expr #BoolOperator                                            // &&
    | value=INTEGER #IntegerLiteral                                             // numeros
    | value=THIS #This                                                          // keyword: "this"
    | (value=TRUE | value=FALSE) #Bool                                          // keywords: "true" ; "false"
    | name=ID #VarRefExpr                                                       // vars
    ;