package pt.up.fe.comp2024.optimization;

import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.ollir.JmmOptimization;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.comp2024.optimization.registers.RegisterAllocationOptimizer;
import pt.up.fe.comp2024.utils.graph.algorithms.GreedyGraphColoringAlgorithm;
import pt.up.fe.comp2024.utils.graph.algorithms.KColorsAlgorithm;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public class JmmOptimizationImpl implements JmmOptimization {

    @Override
    public OllirResult toOllir(JmmSemanticsResult semanticsResult) {

        var visitor = new OllirGeneratorVisitor(semanticsResult.getSymbolTable());
        var ollirCode = visitor.visit(semanticsResult.getRootNode());

        return new OllirResult(semanticsResult, ollirCode, Collections.emptyList());
    }

    @Override
    public OllirResult optimize(OllirResult ollirResult) {
        this.registerAllocation(ollirResult);

        return ollirResult;
    }

    private void registerAllocation(OllirResult ollirResult) {
        if(this.isNoneRegisterAllocation(ollirResult)) return;

        List<Function<OllirResult, Optional<RegisterAllocationOptimizer>>> optimizationFinders = List.of(
                this::definedLimitRegisterAllocation, this::fewAsPossibleRegisterAllocation
        );

        for(var finder: optimizationFinders) {
            Optional<RegisterAllocationOptimizer> possibleOptimizer = finder.apply(ollirResult);
            if(possibleOptimizer.isPresent()) {
                possibleOptimizer.get().optimize();
            }
            break;
        }
    }

    private boolean isNoneRegisterAllocation(OllirResult ollirResult) {
        return ollirResult.getConfig().get("registerAllocation").equals("-1");
    }

    private Optional<RegisterAllocationOptimizer> definedLimitRegisterAllocation(OllirResult ollirResult) {
        int maximumNumberOfRegisters = Integer.valueOf(ollirResult.getConfig().get("registerAllocation"));
        if(maximumNumberOfRegisters >= 1) {
            return Optional.of(new RegisterAllocationOptimizer(ollirResult, new KColorsAlgorithm<>(maximumNumberOfRegisters)));
        }

        return Optional.empty();
    }

    private Optional<RegisterAllocationOptimizer> fewAsPossibleRegisterAllocation(OllirResult ollirResult) {
        if(ollirResult.getConfig().get("registerAllocation").equals("0")) {
            return Optional.of(new RegisterAllocationOptimizer(ollirResult, new GreedyGraphColoringAlgorithm<String>()));
        }

        return Optional.empty();
    }
}
