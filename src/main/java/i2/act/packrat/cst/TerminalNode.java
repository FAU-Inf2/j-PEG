package i2.act.packrat.cst;

import i2.act.packrat.Token;
import i2.act.packrat.cst.visitors.SyntaxTreeVisitor;
import i2.act.peg.ast.Annotation;
import i2.act.peg.symbols.LexerSymbol;
import i2.act.peg.symbols.Symbol;
import i2.act.util.Pair;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class TerminalNode extends Node<LexerSymbol> {

  private final Token token;

  public TerminalNode(final Token token) {
    super(token.getTokenSymbol());
    this.token = token;
  }

  public final Token getToken() {
    return this.token;
  }

  @Override
  public final int size() {
    return 1;
  }

  @Override
  public final int numberOfTerminals() {
    return 1;
  }

  @Override
  public final void setParentReferences() {
    /* intentionally left blank */
  }

  @Override
  public final List<Node<?>> getChildren() {
    return Collections.emptyList();
  }

  @Override
  public final List<Node<?>> getChildren(final Symbol<?> symbol) {
    return Collections.emptyList();
  }

  @Override
  public final Node<?> getChild(final Symbol<?> symbol, final int occurrence) {
    return null; // a terminal node does not have children
  }

  @Override
  public final LexerSymbol getSymbol() {
    return this.token.getTokenSymbol();
  }

  @Override
  public final String toString() {
    final String string;
    {
      if (this.token.getValue() == null) {
        string = this.token.getTokenSymbol().getName();
      } else {
        string = this.token.getEscapedValue();
      }
    }

    if (this.expectedSymbol != this.token.getTokenSymbol()) {
      return String.format("%s (%s)", string, this.expectedSymbol.getName());
    } else {
      return string;
    }
  }

  @Override
  public final String getText() {
    return this.token.getValue();
  }

  @Override
  public final void compactify() {
    // a terminal node cannot be compactified
  }

  @Override
  public final Pair<Node<?>, Node<?>> cloneTree(final Node<?> subNode) {
    final TerminalNode clone = cloneNode();

    if (subNode == this) {
      return new Pair<>(clone, clone);
    } else {
      return new Pair<>(clone, null);
    }
  }

  @Override
  protected final Node<?> cloneTree(final Collection<Node<?>> subNodes,
      final Map<Node<?>, Node<?>> clonedSubNodes) {
    final TerminalNode clone = cloneNode();

    for (final Node<?> subNode : subNodes) {
      if (subNode == this) {
        assert (!clonedSubNodes.containsKey(this));
        clonedSubNodes.put(this, clone);
        break;
      }
    }

    return clone;
  }

  @Override
  public final TerminalNode cloneNode() {
    final TerminalNode clone = new TerminalNode(this.token);
    clone.expectedSymbol = this.expectedSymbol;

    for (final Annotation annotation : this.annotations) {
      clone.annotations.add(annotation);
    }

    return clone;
  }

  @Override
  public final Node<?> pruneTo(final Set<Node<?>> keptNodes) {
    if (keptNodes.contains(this)) {
      return this;
    } else {
      return null;
    }
  }

  @Override
  public final Node<?> prune(final Set<Node<?>> removedNodes) {
    if (removedNodes.contains(this)) {
      return null;
    } else {
      return this;
    }
  }

  @Override
  public final <P, R> R accept(final SyntaxTreeVisitor<P, R> visitor, final P parameter) {
    return visitor.visit(this, parameter);
  }

}
