package pt.up.fe.comp2024.optimization.registers;

import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.comp2024.utils.graph.Graph;

public class DefinedLimitRegisterAllocationOptimization extends RegisterAllocationOptimizer {
    int limit;
    public DefinedLimitRegisterAllocationOptimization(OllirResult ollirResult, int limit) {
        super(ollirResult);
        this.limit = limit;
    }

    @Override
    public void optimize() {

    }
}
