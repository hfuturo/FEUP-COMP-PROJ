package pt.up.fe.comp2024.optimization.registers;

import org.specs.comp.ollir.AssignInstruction;
import org.specs.comp.ollir.Instruction;
import org.specs.comp.ollir.Node;
import org.specs.comp.ollir.Operand;
import org.specs.comp.ollir.tree.TreeNode;

import java.util.*;
import java.util.stream.Collectors;

public class CfgMetadata {
    private final HashMap<Node, Set<String>> use;
    private final HashMap<Node, Set<String>> def;
    private final HashMap<Node, Set<String>> liveIn;
    private final HashMap<Node, Set<String>> liveOut;

    public CfgMetadata(Node cfgBeginNode) {
        this.use = new HashMap<>();
        this.def = new HashMap<>();
        this.liveIn = new HashMap<>();
        this.liveOut = new HashMap<>();

        this.initHashMaps(cfgBeginNode);

        Node currentNode;
        List<Node> succNodes;
        Stack<Node> bfsStack = new Stack<>();
        bfsStack.push(cfgBeginNode);

        while(!bfsStack.isEmpty()) {
            currentNode = bfsStack.pop();
            if(!this.computationCfgNodeSentinel(currentNode)) {
                this.computeDef((Instruction) currentNode);
                this.computeUse((Instruction) currentNode);
            }

            succNodes = currentNode.getSuccessors();
            for(var succNode: succNodes) {
                bfsStack.push(succNode);
            }
        }

        this.computeLiveInAndOut(cfgBeginNode);
    }

    public boolean interfere(String variable1, String variable2) {
        for(Map.Entry<Node, Set<String>> map: this.liveIn.entrySet()) {
            Node currentNode = map.getKey();
            Set<String> liveInVariableSet = map.getValue();
            Set<String> liveOutDefVariableSet = this.liveOut.get(currentNode);
            liveOutDefVariableSet.addAll(this.def.get(currentNode));

            boolean bothAreOnLiveIn = liveInVariableSet.contains(variable1) && liveInVariableSet.contains(variable2);
            boolean bothAreOnLiveOutDef = liveOutDefVariableSet.contains(variable1) && liveOutDefVariableSet.contains(variable2);
            if(bothAreOnLiveIn || bothAreOnLiveOutDef) {
                return true;
            }
        }

        return false;
    }

    private boolean varInsideLiveInAndOutDef(Set<String> liveIn, Set<String> liveOutDef, String variable) {
        return liveIn.contains(variable) && liveOutDef.contains(variable);
    }

    private void initHashMaps(Node cfgBeginNode) {
        Node currentNode;
        List<Node> succNodes;
        Stack<Node> bfsStack = new Stack<>();
        bfsStack.push(cfgBeginNode);

        while(!bfsStack.isEmpty()) {
            currentNode = bfsStack.pop();

            this.def.put(currentNode, new TreeSet<>());
            this.use.put(currentNode, new TreeSet<>());
            this.liveIn.put(currentNode, new TreeSet<>());
            this.liveOut.put(currentNode, new TreeSet<>());

            succNodes = currentNode.getSuccessors();
            for(var succNode: succNodes) {
                bfsStack.push(succNode);
            }
        }
    }

    private boolean computationCfgNodeSentinel(Node cfgNode) {
        return (cfgNode.getNodeType().toString().equals("BEGIN") || cfgNode.getNodeType().toString().equals("END"));
    }

    private void computeUse(Instruction instruction) {
        if(this.computationCfgNodeSentinel(instruction)) return;

        List<TreeNode> operands = instruction.getDescendants().stream().filter(n -> n instanceof Operand).collect(Collectors.toList());

        // If is an assign, we ignore the first operand
        for(int i = 0; i < operands.size(); i++) {
            if(this.isAssign(instruction) && i == 0) {
                continue;
            }

            Operand operand = (Operand) operands.get(i);
            this.use.get(instruction).add(operand.getName());
        }
    }

    private boolean isAssign(Instruction instruction) {
        return instruction instanceof AssignInstruction;
    }

    private void computeDef(Instruction instruction) {
        if(instruction instanceof AssignInstruction) {
            Operand operand = (Operand) ((AssignInstruction) instruction).getDest();
            def.get(instruction).add(operand.getName());
        }
    }

    private void computeLiveInAndOut(Node currentNode) {
        if(currentNode.getNodeType().toString().equals("END")) return;

        List<Node> succNodes = currentNode.getSuccessors();

        for(var succ: succNodes) {
            this.computeLiveInAndOut(succ);
            this.liveOut.get(currentNode).addAll(this.liveIn.get(succ));
        }

        Set<String> liveOutDiffDef = new TreeSet<>(this.liveOut.get(currentNode));
        liveOutDiffDef.removeAll(this.def.get(currentNode));

        Set<String> useUnionLiveOutDiff = new TreeSet<>(this.use.get(currentNode));
        useUnionLiveOutDiff.addAll(liveOutDiffDef);
        this.liveIn.get(currentNode).addAll(useUnionLiveOutDiff);
    }
}
