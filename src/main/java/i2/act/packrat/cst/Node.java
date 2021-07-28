package i2.act.packrat.cst;

import i2.act.packrat.cst.visitors.PrettyPrinter;
import i2.act.packrat.cst.visitors.SyntaxTreeVisitor;
import i2.act.peg.ast.Annotation;
import i2.act.peg.symbols.Symbol;
import i2.act.util.Pair;
import i2.act.util.SafeWriter;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class Node<S extends Symbol<?>> {

  protected Node<?> parent;

  protected Symbol<?> expectedSymbol;

  protected final List<Annotation> annotations;

  public Node(final Symbol<?> expectedSymbol) {
    this.expectedSymbol = expectedSymbol;
    this.annotations = new ArrayList<Annotation>();
  }

  public final Node<?> getParent() {
    return this.parent;
  }

  public abstract int size();

  public abstract int numberOfTerminals();

  public abstract void setParentReferences();

  public abstract List<Node<?>> getChildren();

  public abstract List<Node<?>> getChildren(final Symbol<?> symbol);

  public final int numberOfChildren() {
    return getChildren().size();
  }

  public final Node<?> getChild(final int index) {
    if (index >= 0 && index < numberOfChildren()) {
      return getChildren().get(index);
    } else {
      return null;
    }
  }

  public final Node<?> getChild(final Symbol<?> symbol) {
    return getChild(symbol, 0);
  }

  public abstract Node<?> getChild(final Symbol<?> symbol, final int occurrence);

  public final boolean hasChild(final Symbol<?> symbol) {
    return getChild(symbol) != null;
  }

  public final boolean hasChild(final Symbol<?> symbol, final int occurrence) {
    return getChild(symbol, occurrence) != null;
  }

  public abstract S getSymbol();

  public final Symbol<?> getExpectedSymbol() {
    return this.expectedSymbol;
  }

  public final void setExpectedSymbol(final Symbol<?> expectedSymbol) {
    this.expectedSymbol = expectedSymbol;
  }

  public final String print() {
    final StringWriter stringWriter = new StringWriter();
    final BufferedWriter bufferedWriter = new BufferedWriter(stringWriter);

    final SafeWriter writer = SafeWriter.fromBufferedWriter(bufferedWriter);
    PrettyPrinter.print(this, writer);
    writer.flush();

    final String serialized = stringWriter.toString();

    try {
      bufferedWriter.close();
    } catch (final IOException exception) {
      // intentionally left blank
    }

    return serialized;
  }

  public abstract String getText();

  public abstract void compactify();

  @SuppressWarnings("unchecked")
  public final Node<S> cloneTree() {
    return (Node<S>) cloneTree((Node<?>) null).getFirst();
  }

  public abstract Pair<Node<?>, Node<?>> cloneTree(final Node<?> subNode);

  public final Pair<Node<?>, Map<Node<?>, Node<?>>> cloneTree(final Collection<Node<?>> subNodes) {
    final Map<Node<?>, Node<?>> clonedSubNodes = new HashMap<>();
    final Node<?> clonedTree = cloneTree(subNodes, clonedSubNodes);

    return new Pair<Node<?>, Map<Node<?>, Node<?>>>(clonedTree, clonedSubNodes);
  }

  protected abstract Node<?> cloneTree(final Collection<Node<?>> subNodes,
      final Map<Node<?>, Node<?>> clonedSubNodes);

  public abstract Node<?> cloneNode();

  public abstract Node<?> pruneTo(final Set<Node<?>> keptNodes);

  public abstract Node<?> prune(final Set<Node<?>> removedNodes);

  public final boolean containsNode(final Node<?> node) {
    Node<?> parentNode = node;
    while (parentNode != null) {
      if (parentNode == this) {
        return true;
      }
      parentNode = parentNode.parent;
    }

    return false;
  }

  public final Node<?> replaceWith(final Node<?> replacement) {
    if (this.parent != null) {
      assert (this.parent instanceof NonTerminalNode);
      ((NonTerminalNode) this.parent).replaceChild(this, replacement);
    }
    return replacement;
  }

  public final void addAnnotation(final Annotation annotation) {
    this.annotations.add(annotation);
  }

  public final void addAnnotations(final List<Annotation> annotations) {
    this.annotations.addAll(annotations);
  }

  public final List<Annotation> getAnnotations() {
    return Collections.unmodifiableList(this.annotations);
  }

  public final boolean hasAnnotation() {
    return !this.annotations.isEmpty();
  }

  public final boolean hasAnnotation(final String key) {
    for (final Annotation annotation : this.annotations) {
      if (key.equals(annotation.getKey())) {
        return true;
      }
    }

    return false;
  }

  public abstract <P, R> R accept(final SyntaxTreeVisitor<P, R> visitor, final P parameter);

}
