package pt.up.fe.comp2024.utils.graph.algorithms;

import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2024.utils.graph.Graph;
import pt.up.fe.comp2024.utils.graph.GraphColoringNode;

import java.util.HashSet;
import java.util.Optional;
import java.util.TreeSet;

public class KColorsAlgorithm<T> implements GraphColoringAlgorithm<T> {
    int numberOfColors;
    public KColorsAlgorithm(int numberOfColors) {
        this.numberOfColors = numberOfColors;
    }

    @Override
    public boolean execute(Graph<GraphColoringNode<T>> graph, int startingColorValue) {
        for(var node: graph.getNodes()) {
            int tentativeColor = startingColorValue;
            TreeSet<Integer> edgeIntegerValues = new TreeSet<>();

            if(node.getEdges().isEmpty()) node.setColor(startingColorValue);

            for(var edge: node.getEdges()) {
                GraphColoringNode<T> destNode = (GraphColoringNode<T>) edge.getEnd();
                Optional<Integer> destNodeColor = destNode.getColor();

                if(destNodeColor.isPresent()) edgeIntegerValues.add(destNodeColor.get());
            }


            if(edgeIntegerValues.isEmpty()) node.setColor(tentativeColor);

            boolean possibleColorFound = false;
            for(var i: edgeIntegerValues) {
                if(tentativeColor == i) {
                    tentativeColor++;
                } else {
                    possibleColorFound = true;
                    break;
                }
            }

            if(possibleColorFound || (tentativeColor - startingColorValue) < numberOfColors) {
                node.setColor(tentativeColor);
            } else {
                return false;
            }
        }

        return true;
    }
}
