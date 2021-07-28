package i2.act.packrat.cst.visitors;

import i2.act.packrat.cst.Node;
import i2.act.packrat.cst.NonTerminalNode;
import i2.act.packrat.cst.TerminalNode;
import i2.act.peg.symbols.Symbol;

import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

public final class TreeVisitor<P, R> extends SyntaxTreeVisitor<P, R> {

  public static enum Order {
    LEFT_TO_RIGHT, RIGHT_TO_LEFT;
  }

  public static interface Visit<P, R> {

    public R visit(final Node<?> node, final P parameter);

  }

  // ===============================================================================================

  private final Order order;
  private final Map<Symbol<?>, Visit<P, R>> visits;

  public TreeVisitor(final Order order) {
    this.order = order;
    this.visits = new HashMap<Symbol<?>, Visit<P, R>>();
  }

  public static final <P, R> TreeVisitor<P, R> leftToRight() {
    return new TreeVisitor<P, R>(Order.LEFT_TO_RIGHT);
  }

  public static final <P, R> TreeVisitor<P, R> leftToRight(final Symbol<?> symbol,
      final Visit<P, R> visit) {
    return TreeVisitor.<P, R>leftToRight().add(symbol, visit);
  }

  public static final <P, R> TreeVisitor<P, R> rightToLeft() {
    return new TreeVisitor<P, R>(Order.RIGHT_TO_LEFT);
  }

  public static final <P, R> TreeVisitor<P, R> rightToLeft(final Symbol<?> symbol,
      final Visit<P, R> visit) {
    return TreeVisitor.<P, R>rightToLeft().add(symbol, visit);
  }

  public final TreeVisitor<P, R> add(final Symbol<?> symbol, final Visit<P, R> visit) {
    this.visits.put(symbol, visit);
    return this;
  }

  public final R visit(final Node<?> syntaxTree) {
    return handle(syntaxTree, null);
  }

  public final R visit(final Node<?> syntaxTree, final P parameter) {
    return handle(syntaxTree, parameter);
  }

  @Override
  public final R visit(final NonTerminalNode node, final P parameter) {
    return handle(node, parameter);
  }

  @Override
  public final R visit(final TerminalNode node, final P parameter) {
    return handle(node, parameter);
  }

  private final R handle(final Node<?> node, final P parameter) {
    final Symbol<?> symbol = node.getSymbol();
    assert (symbol != null);

    if (this.visits.containsKey(symbol)) {
      return this.visits.get(symbol).visit(node, parameter);
    } else {
      // visit children
      final List<Node<?>> children = node.getChildren();

      R result = null;

      if (this.order == Order.LEFT_TO_RIGHT) {
        for (final Node<?> child : children) {
          result = handle(child, parameter);
        }
      } else {
        assert (this.order == Order.RIGHT_TO_LEFT);
        final ListIterator<Node<?>> listIterator = children.listIterator(children.size());
        while (listIterator.hasPrevious()) {
          final Node<?> child = listIterator.previous();
          result = handle(child, parameter);
        }
      }

      return result;
    }
  }

}
