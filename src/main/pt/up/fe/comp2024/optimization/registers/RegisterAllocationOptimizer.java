package pt.up.fe.comp2024.optimization.registers;

import org.specs.comp.ollir.Descriptor;
import org.specs.comp.ollir.Instruction;
import org.specs.comp.ollir.Method;
import org.specs.comp.ollir.Node;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.comp2024.utils.graph.Graph;
import pt.up.fe.comp2024.utils.graph.GraphColoringNode;
import pt.up.fe.comp2024.utils.graph.GraphNode;
import pt.up.fe.comp2024.utils.graph.algorithms.GraphColoringAlgorithm;

import java.util.*;

public class RegisterAllocationOptimizer {
    OllirResult ollirResult;
    GraphColoringAlgorithm<String> graphColoringAlgorithm;

    public RegisterAllocationOptimizer(OllirResult ollirResult, GraphColoringAlgorithm<String> graphColoringAlgorithm) {
        this.ollirResult = ollirResult;
        this.graphColoringAlgorithm = graphColoringAlgorithm;

    }

    public void optimize() {
        for(Method method: this.ollirResult.getOllirClass().getMethods()) {
            method.buildCFG();
            CfgMetadata cfgMetadata = new CfgMetadata(method.getBeginNode());
            Graph<GraphColoringNode<String>> currentMethodGraph = new Graph<GraphColoringNode<String>>();
            HashMap<String, GraphNode<String>> variableNodeMap = new HashMap<>();
            List<String> variables = new ArrayList<>();

            // 1. Create nodes for each variable and store it inside an hash map mapping variable names to nodes
            for(Map.Entry<String, Descriptor> entry: method.getVarTable().entrySet()) {
                String varName = entry.getKey();
                if(varName.equals("this")) continue;
                GraphColoringNode<String> graphNode = new GraphColoringNode<>(varName);
                variableNodeMap.put(varName, graphNode);
                currentMethodGraph.addNode(graphNode);
                variables.add(varName);
            }

            // 2. For each variable, see which other ones interfere with it and add it as an edge on the graph
            for(var variable1: variables) {
                for(var variable2: variables) {
                    if(variable1.equals("this") || variable2.equals("this")) continue;
                    if(variable2.equals(variable1)) continue;

                    if(cfgMetadata.interfere(variable1, variable2)) {
                        var variable1Node = variableNodeMap.get(variable1);
                        var variable2Node = variableNodeMap.get(variable2);

                        variable1Node.addEdge(variable2Node);
                    }
                }
            }

            this.graphColoringAlgorithm.execute(currentMethodGraph, method.getParams().size() + 1);
            for(var node: currentMethodGraph.getNodes()) {
                Optional<Integer> possibleColor = node.getColor();

                if(possibleColor.isPresent()) {
                    method.getVarTable().get(node.getValue()).setVirtualReg(possibleColor.get());
                }
            }
        }
    }
}
