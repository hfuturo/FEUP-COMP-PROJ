package pt.up.fe.comp2024.utils.graph;

import java.util.ArrayList;
import java.util.List;

public class Graph<T> {
    private final List<T> nodes;

    public Graph() {
        this.nodes = new ArrayList<>();
    }

    public void addNode(T graphNode) {
        this.nodes.add(graphNode);
    }

    public List<T> getNodes() {
        return this.nodes;
    }
}
