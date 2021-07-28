package i2.act.packrat.cst.visitors;

import i2.act.packrat.cst.Node;
import i2.act.packrat.cst.NonTerminalNode;
import i2.act.packrat.cst.TerminalNode;
import i2.act.util.SafeWriter;

import java.util.HashMap;
import java.util.Map;

public final class DotGenerator extends SyntaxTreeVisitor<SafeWriter, Void> {

  public static interface DotStyle {

    public String styleNode(final Node<?> node);

    public String styleEdge(final Node<?> from, final Node<?> to);

  }

  public abstract static class BaseDotStyle implements DotStyle {

    @Override
    public String styleNode(final Node<?> node) {
      if (node instanceof NonTerminalNode) {
        final NonTerminalNode nonTerminalNode = (NonTerminalNode) node;

        if (nonTerminalNode.isQuantifierNode()) {
          return "shape=circle, " + styleQuantifier();
        } else if (nonTerminalNode.isListItemNode()) {
          return "shape=box, " + styleListItem();
        } else {
          return "shape=box, " + styleNonTerminal();
        }
      } else {
        assert (node instanceof TerminalNode);
        return "shape=box, " + styleTerminal();
      }
    }

    public String styleNonTerminal() {
      return "";
    }

    public String styleTerminal() {
      return "";
    }

    public String styleQuantifier() {
      return "";
    }

    public String styleListItem() {
      return "";
    }

    @Override
    public String styleEdge(final Node<?> from, final Node<?> to) {
      return "";
    }

  }

  public static final DotStyle DEFAULT_DOT_STYLE = new BaseDotStyle() {

    @Override
    public final String styleNonTerminal() {
      return "style=filled, fillcolor=lightgoldenrod1";
    }

    @Override
    public final String styleTerminal() {
      return "style=filled, fillcolor=goldenrod";
    }

    @Override
    public final String styleQuantifier() {
      return "style=filled, fillcolor=indianred1";
    }

    @Override
    public final String styleListItem() {
      return "style=filled, fillcolor=gainsboro";
    }

  };

  // ===============================================================================================

  private final DotStyle style;

  private final Map<Node, String> nodeIDs;
  private int idCounter;

  private DotGenerator(final DotStyle style) {
    this.style = style;

    this.nodeIDs = new HashMap<Node, String>();
    this.idCounter = 0;
  }

  public static final void print(final Node<?> syntaxTree) {
    final SafeWriter writer = SafeWriter.openStdOut();
    print(syntaxTree, writer);
    writer.flush();
  }

  public static final void print(final Node<?> syntaxTree, final DotStyle style) {
    final SafeWriter writer = SafeWriter.openStdOut();
    print(syntaxTree, writer, style);
    writer.flush();
  }

  public static final void print(final Node<?> syntaxTree, final SafeWriter writer) {
    print(syntaxTree, writer, DEFAULT_DOT_STYLE);
  }

  public static final void print(final Node<?> syntaxTree, final SafeWriter writer,
      final DotStyle style) {
    writer.write("digraph G {\n");
    writer.write("  graph [ordering=\"out\"];\n");
    writer.write("  node [fontname=\"Droid Sans Mono\"];\n");

    syntaxTree.accept(new DotGenerator(style), writer);

    writer.write("}");
    writer.flush();
  }

  private final String getNodeID(final Node<?> node) {
    return this.nodeIDs.computeIfAbsent(node,
        n -> String.format("n%d", DotGenerator.this.idCounter++));
  }

  private final void writeNode(final Node<?> node, final SafeWriter writer) {
    final String nodeID = getNodeID(node);
    final String nodeLabel = node.toString().replace("\"", "\\\"");
    final String nodeStyle = this.style.styleNode(node);

    writer.write("  %s [label=\"%s\", %s];\n", nodeID, nodeLabel, nodeStyle);
  }

  private final void writeEdge(final Node<?> from, final Node<?> to, final SafeWriter writer) {
    final String nodeIDFrom = getNodeID(from);
    final String nodeIDTo = getNodeID(to);
    final String edgeStyle = this.style.styleEdge(from, to);

    writer.write("  %s -> %s [%s];\n", nodeIDFrom, nodeIDTo, edgeStyle);
  }

  @Override
  protected final Void prolog(final Node<?> node, final SafeWriter writer) {
    writeNode(node, writer);
    return null;
  }

  @Override
  protected final Void afterChild(final Node<?> parent, final Node<?> child,
      final SafeWriter writer) {
    writeEdge(parent, child, writer);
    return null;
  }

}
