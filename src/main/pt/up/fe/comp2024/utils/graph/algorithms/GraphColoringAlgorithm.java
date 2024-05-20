package pt.up.fe.comp2024.utils.graph.algorithms;

import pt.up.fe.comp2024.utils.graph.Graph;
import pt.up.fe.comp2024.utils.graph.GraphColoringNode;

public interface GraphColoringAlgorithm<T> {
    public void execute(Graph<GraphColoringNode<T>> graph, int startingColor);
}
