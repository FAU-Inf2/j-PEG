package i2.act.peg.ast.visitors;

import i2.act.peg.ast.ASTNode;
import i2.act.peg.ast.Grammar;
import i2.act.peg.ast.LexerIdentifier;
import i2.act.peg.ast.ParserIdentifier;
import i2.act.util.SafeWriter;

import java.util.HashMap;
import java.util.Map;

public final class DotGenerator extends BaseASTVisitor<SafeWriter, Void>  {

  private final Map<ASTNode, Integer> nodeIDs;
  private int idCounter;

  private DotGenerator() {
    this.nodeIDs = new HashMap<ASTNode, Integer>();
    this.idCounter = 0;
  }

  public static final void printDot(final ASTNode node, final SafeWriter writer) {
    writer.write("digraph G {\n");

    // make sure that children are ordered the same way as in the AST
    writer.write("  graph [ordering=\"out\"];\n");

    final DotGenerator dotGenerator = new DotGenerator();
    node.accept(dotGenerator, writer);

    writer.write("}\n");

    writer.flush();
  }

  private final Integer getNodeID(final ASTNode astNode) {
    if (this.nodeIDs.containsKey(astNode)) {
      return this.nodeIDs.get(astNode);
    } else {
      final Integer nodeID = this.idCounter++;
      this.nodeIDs.put(astNode, nodeID);
      return nodeID;
    }
  }

  private final String getNodeRepresentation(final ASTNode astNode) {
    final int nodeID = getNodeID(astNode).intValue();
    return String.format("node_%d", nodeID);
  }

  private final void writeASTNode(final ASTNode astNode, final SafeWriter writer) {
    final String nodeRepresentation = getNodeRepresentation(astNode);
    final String label = astNode.toString();

    final String style;
    {
      if (astNode instanceof ParserIdentifier) {
        style = "shape=box, color=blue";
      } else if (astNode instanceof LexerIdentifier) {
        style = "shape=box, color=red";
      } else {
        style = "";
      }
    }

    writer.write("  %s [label=\"%s\", %s];\n", nodeRepresentation, label, style);
  }

  private final void writeASTEdge(final ASTNode from, final ASTNode to,
      final SafeWriter writer) {
    final String nodeRepresentationFrom = getNodeRepresentation(from);
    final String nodeRepresentationTo = getNodeRepresentation(to);

    writer.write("  %s -> %s;\n", nodeRepresentationFrom, nodeRepresentationTo);
  }

  @Override
  protected Void prolog(final ASTNode astNode, final SafeWriter writer) {
    writeASTNode(astNode, writer);

    return null;
  }

  @Override
  protected Void afterChild(final ASTNode parent, final ASTNode child, final SafeWriter writer) {
    writeASTEdge(parent, child, writer);

    return null;
  }

  @Override
  public Void visit(final Grammar grammar, final SafeWriter writer) {
    super.visit(grammar, writer);

    return null;
  }

}
