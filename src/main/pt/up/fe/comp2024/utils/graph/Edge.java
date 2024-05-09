package pt.up.fe.comp2024.utils.graph;

public class Edge<T> {
    GraphNode<T> start;
    GraphNode<T> end;
    public Edge(GraphNode<T> start, GraphNode<T> end) {
        this.start = start;
        this.end = end;
    }

    public GraphNode<T> getStart() {
        return this.start;
    }

    public GraphNode<T> getEnd() {
        return this.end;
    }
}
