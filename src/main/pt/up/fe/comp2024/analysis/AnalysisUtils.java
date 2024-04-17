package pt.up.fe.comp2024.analysis;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2024.ast.TypeUtils;

import java.util.List;
import java.util.Optional;

public class AnalysisUtils {
    public static Optional<Symbol> validateSymbolFromSymbolTable(String currentMethod, SymbolTable table, String symbolName) {
        Optional<Symbol> methodSymbol = AnalysisUtils.tryToGetSymbolFromMethod(symbolName, currentMethod, table) ;
        if(methodSymbol.isPresent()) return methodSymbol;

        return AnalysisUtils.tryToGetSymbolFromClassFields(symbolName, table);
    }

    public static Optional<Symbol> validateSymbolFromSymbolTable(SymbolTable table, String symbolName) {
        List<String> methods = table.getMethods();

        if (methods != null) {
            for (String method : methods) {

                Optional<Symbol> symbol = tryToGetSymbolFromMethod(symbolName, method, table);

                if (symbol.isPresent()) return symbol;
            }
        }

        return AnalysisUtils.tryToGetSymbolFromClassFields(symbolName, table);
    }

    private static Optional<Symbol> tryToGetSymbolFromClassFields(String symbolName, SymbolTable table) {
        List<Symbol> fields = table.getFields();

        if (fields != null) {
            for (Symbol symbol : fields) {
                if (symbol.getName().equals(symbolName)) {
                    return Optional.of(symbol);
                }
            }
        }
        return Optional.empty();
    }

    private static Optional<Symbol> tryToGetSymbolFromMethod(String symbolName, String method, SymbolTable table) {
        List<Symbol> parameters = table.getParameters(method);
        List<Symbol> localVars = table.getLocalVariables(method);

        if (parameters != null) {
            for (Symbol symbol : parameters) {
                if (symbol.getName().equals(symbolName)) {
                    return Optional.of(symbol);
                }
            }
        }

        if (localVars != null) {
            for (Symbol symbol : localVars) {
                if (symbol.getName().equals(symbolName)) {
                    return Optional.of(symbol);
                }
            }
        }

        return Optional.empty();
    }

    public static boolean validateIsImported(String name, SymbolTable table) {
        List<String> imports = table.getImports();
        if (imports == null)
            return false;
        return imports.stream().anyMatch(currImport -> currImport.equals(name));
    }

    public static boolean validateIsField(String name, String currentMethod, SymbolTable table) {
        Optional<Symbol> methodSymbol = AnalysisUtils.tryToGetSymbolFromMethod(name, currentMethod, table) ;
        if(methodSymbol.isPresent()) return false;

        Optional<Symbol> classField = AnalysisUtils.tryToGetSymbolFromClassFields(name, table);

        return classField.isPresent();
    }

    public static boolean allElementsOfArrayAreOfType(Type type, JmmNode arrayNode, SymbolTable table) {
        for(JmmNode arrayElement: arrayNode.getChildren()) {
            if(!TypeUtils.getExprType(arrayElement, table).getName().equals(type.getName())) {
                return false;
            }
        }

        return true;
    }
}
