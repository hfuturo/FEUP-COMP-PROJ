package pt.up.fe.comp2024.optimization.registers;

import org.specs.comp.ollir.Descriptor;
import org.specs.comp.ollir.Instruction;
import org.specs.comp.ollir.Method;
import org.specs.comp.ollir.Node;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.comp2024.utils.graph.Graph;
import pt.up.fe.comp2024.utils.graph.GraphColoringNode;
import pt.up.fe.comp2024.utils.graph.GraphNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class RegisterAllocationOptimizer {
    HashMap<Method, Graph<GraphColoringNode<String>>> graphMaps;
    OllirResult ollirResult;

    public RegisterAllocationOptimizer(OllirResult ollirResult) {
        this.ollirResult = ollirResult;
        this.graphMaps = new HashMap<>();

        this.buildInterferenceGraph();
    }

    private void buildInterferenceGraph() {
        HashMap<Method, Graph<GraphColoringNode<String>>> result = new HashMap<>();

        for(Method method: this.ollirResult.getOllirClass().getMethods()) {
            method.buildCFG();
            CfgMetadata cfgMetadata = new CfgMetadata(method.getBeginNode());
            Graph currentMethodGraph = new Graph<String>();


            // Add nodes
            for(Map.Entry<String, Descriptor> entry: method.getVarTable().entrySet()) {
                List<GraphNode<String>> interferenceVars = this.findInterferenceVars(method.getInstructions(), entry.getKey());

                GraphNode<String> graphNode = new GraphNode(entry.getKey());
                graphNode.addEdges(interferenceVars);

                currentMethodGraph.addNode(graphNode);
            }

            this.graphMaps.put(method, currentMethodGraph);
        }

        this.graphMaps = result;
    }

    private List<GraphNode<String>> findInterferenceVars(ArrayList<Instruction> methodInstructions, String var) {
        return new ArrayList<>();
    }

    private void buildNodes(Graph graph) {
        this.buildFieldNodes(graph);
        this.buildMethodNodes(graph);
    }

    private void buildFieldNodes(Graph graph) {

    }

    private void buildMethodNodes(Graph graph) {
        for(Method method: this.ollirResult.getOllirClass().getMethods()) {

        }
    }

    public abstract void optimize();
}
