package pt.up.fe.comp2024.optimization.registers;

import org.specs.comp.ollir.AssignInstruction;
import org.specs.comp.ollir.Instruction;
import org.specs.comp.ollir.Node;
import org.specs.comp.ollir.Operand;
import org.specs.comp.ollir.tree.TreeNode;

import java.util.*;

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

        // Se não for nenhum tipo de assign, é só percorrer e ver as crianças que são operandos e adicioná-las ao set do use

        List<TreeNode> l = instruction.getChildren();
        System.out.println("debug");
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
