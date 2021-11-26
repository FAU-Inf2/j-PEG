package i2.act.packrat.cst.visitors;

import i2.act.packrat.cst.Node;
import i2.act.packrat.cst.NonTerminalNode;
import i2.act.packrat.cst.TerminalNode;
import i2.act.util.SafeWriter;

public final class LaTeXGenerator extends SyntaxTreeVisitor<SafeWriter, Void> {

  private int indentation;

  private LaTeXGenerator(final int indentation) {
    this.indentation = indentation;
  }

  public static final void print(final Node<?> syntaxTree) {
    final SafeWriter writer = SafeWriter.openStdOut();
    print(syntaxTree, writer);
    writer.flush();
  }

  public static final void print(final Node<?> syntaxTree, final SafeWriter writer) {
    writer.write("\\documentclass[crop,tikz]{standalone}\n\n");
    writer.write("\\usepackage[T1]{fontenc}\n");
    writer.write("\\usepackage[utf8]{inputenc}\n");
    writer.write("\\usepackage{inconsolata}\n");
    writer.write("\\usepackage{tikz}\n");
    writer.write("\\usepackage[edges]{forest}\n\n");
    writer.write("\\begin{document}\n\n");

    writer.write(
        "\\tikzstyle{tree node}=[%\n"
        + "  rectangle, draw=black, line width=1.0pt,%\n"
        + "  font=\\fontsize{13}{14}\\selectfont\\ttfamily,%\n"
        + "  text depth=0.2ex,%\n"
        + "  text height=2.0ex,%\n"
        + "  minimum height=4.0ex,%\n"
        + "]%\n"
        + "\n"
        + "\\tikzstyle{terminal node}=[%\n"
        + "  tree node, fill=red!10,%\n"
        + "]%\n"
        + "\n"
        + "\\tikzstyle{nonterminal node}=[%\n"
        + "  tree node, fill=blue!10,%\n"
        + "]%\n"
        + "\n"
        + "\\tikzstyle{quantifier node}=[%\n"
        + "  tree node, circle, inner sep=0, minimum width=2.6ex,%\n"
        + "  fill=yellow!85!red!30,%\n"
        + "]%\n"
        + "\n"
        + "\\tikzstyle{item node}=[%\n"
        + "  tree node, chamfered rectangle, chamfered rectangle sep=0.3ex, inner ysep=0.0ex,%\n"
        + "  fill=yellow!85!red!30,%\n"
        + "]%\n"
        + "\n"
        + "\\tikzstyle{tree edge}=[%\n"
        + "  draw=black, line width=1.0pt,%\n"
        + "]%\n"
        + "\n");

    writer.write(
        "\\begin{forest}%\n"
        + "  forked edges,%\n"
        + "  %\n"
        + "  for tree={%\n"
        + "    if n children=0{terminal node}{nonterminal node},%\n"
        + "    edge+={tree edge},%\n"
        + "  },%\n"
        + "  %\n");

    syntaxTree.accept(new LaTeXGenerator(2), writer);

    writer.write("\\end{forest}\n\n");

    writer.write("\\end{document}\n");

    writer.flush();
  }

  private final void indent(final SafeWriter writer) {
    for (int count = 0; count < this.indentation; ++count) {
      writer.write(" ");
    }
  }

  private final String getEscapedLabel(final Node<?> node) {
    final String label = (node instanceof TerminalNode)
        ? (((TerminalNode) node).getToken().getValue())
        : (node.getSymbol().getName());

    return label
        .replace("\\", "\\\\")
        .replace("{", "\\{")
        .replace("}", "\\}")
        .replace("_", "\\_");
  }

  private final String getStyle(final Node<?> node) {
    if (node instanceof NonTerminalNode) {
      final NonTerminalNode nonTerminalNode = (NonTerminalNode) node;

      if (nonTerminalNode.isQuantifierNode()) {
        return "quantifier node";
      }

      if (nonTerminalNode.isListItemNode()) {
        return "item node";
      }
    }

    return "";
  }

  @Override
  protected Void prolog(final Node<?> node, final SafeWriter writer) {
    indent(writer);

    final String label = getEscapedLabel(node);
    final String style = getStyle(node);

    writer.write("[ {%s}, %s", label, style);

    if (node.numberOfChildren() != 0) {
      writer.write("\n");
    }

    this.indentation += 2;

    return null;
  }

  @Override
  protected Void epilog(final Node<?> node, final SafeWriter writer) {
    this.indentation -= 2;

    if (node.numberOfChildren() == 0) {
      writer.write(" ]\n");
    } else {
      indent(writer);
      writer.write("]\n");
    }

    return null;
  }

}
