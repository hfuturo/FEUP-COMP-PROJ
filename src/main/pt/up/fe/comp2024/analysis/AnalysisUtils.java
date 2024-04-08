package pt.up.fe.comp2024.analysis;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;

import javax.swing.text.html.Option;
import java.util.List;
import java.util.Optional;

public class AnalysisUtils {
    public static Optional<Symbol> validateSymbolFromSymbolTable(String currentMethod, SymbolTable table, String symbolName) {
        Optional<Symbol> methodSymbol = AnalysisUtils.tryToGetSymbolFromMethod(symbolName, currentMethod, table) ;
        if(methodSymbol.isPresent()) return methodSymbol;

        return AnalysisUtils.tryToGetSymbolFromClassFields(symbolName, table);
    }

    public static Optional<Symbol> validateSymbolFromSymbolTable(SymbolTable table, String symbolName) {
        for (String method: table.getMethods()) {
            Optional<Symbol> symbol = tryToGetSymbolFromMethod(symbolName, method, table);

            if(symbol.isPresent()) return symbol;
        }

        return AnalysisUtils.tryToGetSymbolFromClassFields(symbolName, table);
    }

    private static Optional<Symbol> tryToGetSymbolFromClassFields(String symbolName, SymbolTable table) {
        for (Symbol symbol: table.getFields()) {
            if (symbol.getName().equals(symbolName)) {
                return Optional.of(symbol);
            }
        }

        return Optional.empty();
    }

    private static Optional<Symbol> tryToGetSymbolFromMethod(String symbolName, String method, SymbolTable table) {
        for (Symbol symbol: table.getParameters(method)) {
            if (symbol.getName().equals(symbolName)) {
                return Optional.of(symbol);
            }
        }

        for (Symbol symbol: table.getLocalVariables(method)) {
            if (symbol.getName().equals(symbolName)) {
                return Optional.of(symbol);
            }
        }

        return Optional.empty();
    }

    public static boolean validateIsImported(String name, SymbolTable table) {
        List<String> imports = table.getImports();
        return imports.stream().anyMatch(currImport -> currImport.equals(name));
    }
}
