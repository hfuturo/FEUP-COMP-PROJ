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
VOID: 'void';
EXTENDS : 'extends' ;

MULTI_LINE_COMMENT_START_MARKER: '/*' ;
MULTI_LINE_COMMENT_END_MARKER: '*/' ;
SINGLE_LINE_COMMENT_MARKER: '//';

SINGLE_LINE_COMMENT : SINGLE_LINE_COMMENT_MARKER ~[\n]* -> skip;
MULTI_LINE_COMMENT: (MULTI_LINE_COMMENT_START_MARKER .*? MULTI_LINE_COMMENT_END_MARKER) -> skip ;

ID : (LETTER)(LETTER | INTEGER)*;
INTEGER : ([0] | [1-9][0-9]*) ;
LETTER: [a-zA-Z_$];

WS : [ \t\n\r\f]+ -> skip ;

program
    :  importDecl* classDeclRule EOF
    ;

importDecl
    : IMPORT names+=('main' | 'length' | ID) ('.' names+=('main' | 'length' | ID))* SEMI # Import
    ;


classDeclRule
    :   CLASS name=('main' | LENGTH | ID)
        (EXTENDS ultraSuper=ID)?
        LCURLY
        varDeclRule* methodDeclRule*
        RCURLY # ClassDecl
    ;

varDeclRule
    : type name=('main' | LENGTH | ID) SEMI # VarDecl
    ;

methodDeclRule locals[boolean isPublic=false]
    : (PUBLIC {$isPublic=true;})?
        type name=('main' | LENGTH | ID)
        LPAREN (param (',' param)*)? RPAREN
        LCURLY varDeclRule* stmt* RCURLY # MethodDecl
    | mainMethodDecl # MainMethod
    ;

mainMethodDecl
    : (PUBLIC)? STATIC VOID 'main' LPAREN 'String' ('[' ']'  | '[]') name=ID RPAREN
              LCURLY varDeclRule* stmt* RCURLY #InnerMainMethod
    ;

// É preciso ter atenção de que depois a visitar os nós temos de ver se quando encontrarmos um VarArgType, eles está no fim
type
    : name= ('length' | 'main' | ID) #AbstractDataType
    | name=INT ('[]' | '[' ']') #ArrayType
    | name=INT ELLIPSIS #VarArgType
    | name= INT #IntegerType
    | name= BOOL #BoolType
    | name= 'String' #StringType
    ;

param
    : type name=('main' | LENGTH | ID)
    ;

stmt
    : RETURN expr? SEMI #ReturnStmt
    | LCURLY stmt* RCURLY #ScopeStmt
    | IF LPAREN expr RPAREN stmt ELSE stmt #IfElseStmt
    | WHILE LPAREN expr RPAREN stmt #WhileStmt
    | expr SEMI #ExprStmt
    | left=expr EQUALS value=expr SEMI #AssignStmt
    | ID LSQUARE left=expr RSQUARE EQUALS right=expr SEMI #VarListAssignStmt
    ;

expr
    : op=NEGATION expr #Unary                                                   // !
    | openingParentheses=LPAREN expr closingParentheses=RPAREN #Parenthesis                                     // ()
    | expr op=STOP 'length' #Length                                               // foo.length
    | expr op=STOP name=ID LPAREN (expr (COMMA expr)* )? RPAREN #VarMethod         // foo.bar(), foo.bar(...)
    | var=expr op=LSQUARE index=expr op=RSQUARE #AccessArray                              // aceder a um array, i.e., a[2]
    | op=NEW INT LSQUARE size=expr RSQUARE   #NewInt                                 // new int
    | op=NEW name=ID LPAREN RPAREN  #NewClass                                   // new class
    | op=LSQUARE (expr (op=COMMA expr)* )? op=RSQUARE #InitArray                // inicializar array, i.e., [1,2,3]
    | left=expr (op=MUL | op=DIV) right=expr #BinaryExpr                                   // * ; /
    | left=expr (op=ADD | op=SUB) right=expr #BinaryExpr                                   // + ; -
    | left=expr op=LESS right=expr #BinaryExpr                                             // <
    | left=expr op=AND right=expr #BinaryExpr                                            // &&
    | value=INTEGER #IntegerLiteral                                             // numeros
    | value=THIS #This                                                          // keyword: "this"
    | (value=TRUE | value=FALSE) #Bool                                          // keywords: "true" ; "false"
    | name=('main' | 'length' | ID) #VarRefExpr                                                       // vars
    ;
