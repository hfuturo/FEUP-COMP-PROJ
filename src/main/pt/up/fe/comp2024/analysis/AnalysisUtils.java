package pt.up.fe.comp2024.analysis;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;

import java.util.Optional;

public class AnalysisUtils {
    public static Optional<Symbol> validateSymbolFromSymbolTable(String currentMethod, SymbolTable table, String symbolName) {
        for (Symbol symbol: table.getFields()) {
            if (symbol.getName().equals(symbolName)) {
                return Optional.of(symbol);
            }
        }

        for (Symbol symbol: table.getParameters(currentMethod)) {
            if (symbol.getName().equals(symbolName)) {
                return Optional.of(symbol);
            }
        }

        for (Symbol symbol: table.getLocalVariables(currentMethod)) {
            if (symbol.getName().equals(symbolName)) {
                return Optional.of(symbol);
            }
        }

        return Optional.empty();
    }
}
