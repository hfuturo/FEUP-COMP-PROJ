package pt.up.fe.comp2024.utils.graph;

import org.specs.comp.ollir.Node;

import java.util.Optional;

public class GraphColoringNode<T> extends GraphNode<T> {
    Optional<Integer> color;
    public GraphColoringNode(T value) {
        super(value);
        this.color = Optional.empty();
    }

    public void setColor(int color) {
        this.color = Optional.of(color);
    }

    public Optional<Integer> getColor() {
        return this.color;
    }
}
