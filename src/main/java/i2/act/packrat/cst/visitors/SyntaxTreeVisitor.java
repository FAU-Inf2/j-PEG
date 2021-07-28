package i2.act.packrat.cst.visitors;

import i2.act.packrat.cst.Node;
import i2.act.packrat.cst.NonTerminalNode;
import i2.act.packrat.cst.TerminalNode;

public abstract class SyntaxTreeVisitor<P, R> {

  protected R result(final R prologResult, final R epilogResult) {
    return epilogResult;
  }

  protected R prolog(final Node<?> node, final P parameter) {
    // intentionally left blank
    return null;
  }

  protected R epilog(final Node<?> node, final P parameter) {
    // intentionally left blank
    return null;
  }

  protected R beforeChild(final Node<?> parent, final Node<?> child, final P parameter) {
    // intentionally left blank
    return null;
  }

  protected R afterChild(final Node<?> parent, final Node<?> child, final P parameter) {
    // intentionally left blank
    return null;
  }

  protected final R visitChild(final Node<?> parent, final Node<?> child, final P parameter) {
    beforeChild(parent, child, parameter);
    final R returnValue = child.accept(this, parameter);
    afterChild(parent, child, parameter);

    return returnValue;
  }

  // -----------------------------------------------------------------------------------------------

  public R visit(final NonTerminalNode node, final P parameter) {
    final R prologResult = prolog(node, parameter);

    for (final Node child : node.getChildren()) {
      visitChild(node, child, parameter);
    }

    final R epilogResult = epilog(node, parameter);

    return result(prologResult, epilogResult);
  }

  public R visit(final TerminalNode node, final P parameter) {
    final R prologResult = prolog(node, parameter);
    final R epilogResult = epilog(node, parameter);

    return result(prologResult, epilogResult);
  }

}
