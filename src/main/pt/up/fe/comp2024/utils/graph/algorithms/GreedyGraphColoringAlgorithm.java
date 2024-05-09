package pt.up.fe.comp2024.utils.graph.algorithms;

import pt.up.fe.comp2024.utils.graph.Edge;
import pt.up.fe.comp2024.utils.graph.Graph;
import pt.up.fe.comp2024.utils.graph.GraphColoringNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class GreedyGraphColoringAlgorithm<T> {
    Graph<GraphColoringNode<T>> graph;
    int startingColor;
    public GreedyGraphColoringAlgorithm(Graph<GraphColoringNode<T>> graph, int startingColor) {
        this.graph = graph;
        this.startingColor = startingColor;
    }

    public void execute() {
        List<GraphColoringNode<T>> nodes = this.graph.getNodes();

        for(var node: nodes) {
            Optional<Integer> lowestRegisterValue = this.getLowestRegisterAvailableFor(node);

            if(lowestRegisterValue.isPresent()) {
                node.setColor(lowestRegisterValue.get());
            } else {
                node.setColor(0);
            }
        }
    }

    private Optional<Integer> getLowestRegisterAvailableFor(GraphColoringNode<T> node) {
        List<Integer> registerValues = new ArrayList<>();

        for(var edge: node.getEdges()) {
            GraphColoringNode<T> destNode = (GraphColoringNode<T>) edge.getEnd();
            if(destNode.getColor().isEmpty()) continue;

            registerValues.add(destNode.getColor().get());
        }

        registerValues.sort((a, b) -> a - b);
        if(registerValues.isEmpty()) {
            return Optional.empty();
        } else {
            return Optional.of(this.getLowestElementNotOnList(registerValues, this.startingColor));
        }
    }

    private int getLowestElementNotOnList(List<Integer> list, int startingLowest) {
        int lowest = startingLowest;
        for(var n: list) {
            if(n == lowest) lowest++;
            else if (n > lowest) return lowest;
        }

        return lowest;
    }
}
