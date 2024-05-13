package pt.up.fe.comp2024.utils.graph;

import org.w3c.dom.Node;

import java.util.ArrayList;
import java.util.List;

public class GraphNode<T> {
    private T value;
    private final List<Edge<T>> edges;
    public GraphNode(T value) {
        this.value = value;
        this.edges = new ArrayList<>();
    }

    public T getValue() {
        return this.value;
    }

    public void addEdges(List<GraphNode<T>> graphNodes) {
        for(GraphNode<T> graphNode: graphNodes) {
            graphNode.addEdge(this);
            this.addEdge(graphNode);
        }
    }

    public void addBidirectionalEdge(GraphNode<T> destNode) {
        this.edges.add(new Edge(this, destNode));
        destNode.addEdge(this);
    }

    public void addEdge(GraphNode<T> destNode) {
        this.edges.add(new Edge(this, destNode));
    }

    public List<Edge<T>> getEdges() {
        return this.edges;
    }
}
