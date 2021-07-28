package i2.act.packrat.cst;

import i2.act.packrat.cst.visitors.SyntaxTreeVisitor;
import i2.act.peg.ast.Annotation;
import i2.act.peg.symbols.ParserSymbol;
import i2.act.peg.symbols.Symbol;
import i2.act.util.Pair;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class NonTerminalNode extends Node<ParserSymbol> {

  private final ParserSymbol symbol;
  private final List<Node<?>> children;

  public NonTerminalNode(final ParserSymbol symbol, final List<Node<?>> children) {
    super(symbol);
    this.symbol = symbol;
    this.children = children;
  }

  public final boolean isQuantifierNode() {
    return this.symbol == ParserSymbol.OPTIONAL
        || this.symbol == ParserSymbol.STAR
        || this.symbol == ParserSymbol.PLUS;
  }

  public final boolean isListItemNode() {
    return this.symbol == ParserSymbol.LIST_ITEM;
  }

  public final boolean isAuxiliaryNode() {
    return isQuantifierNode() || isListItemNode();
  }

  @Override
  public final int size() {
    int size = 1;

    for (final Node<?> child : this.children) {
      size += child.size();
    }

    return size;
  }

  @Override
  public final int numberOfTerminals() {
    int numberOfTerminals = 0;

    for (final Node<?> child : this.children) {
      numberOfTerminals += child.numberOfTerminals();
    }

    return numberOfTerminals;
  }

  @Override
  public final void setParentReferences() {
    for (final Node<?> child : this.children) {
      child.parent = this;
      child.setParentReferences();
    }
  }

  @Override
  public final List<Node<?>> getChildren() {
    return this.children;
  }

  @Override
  public final List<Node<?>> getChildren(final Symbol<?> symbol) {
    final List<Node<?>> matchingChildren = new ArrayList<>();

    for (final Node<?> child : this.children) {
      if (child.getSymbol() == symbol) {
        matchingChildren.add(child);
      }
    }

    return matchingChildren;
  }

  @Override
  public final Node<?> getChild(final Symbol<?> symbol, final int occurrence) {
    int found = 0;
    for (final Node<?> child : this.children) {
      if (child.getSymbol() == symbol) {
        if (found == occurrence) {
          return child;
        }
        ++found;
      }
    }

    return null;
  }

  public final void replaceChild(final Node<?> originalChild, final Node<?> newChild) {
    if (originalChild == newChild) {
      return;
    }

    for (int index = 0; index < this.children.size(); ++index) {
      final Node<?> node = this.children.get(index);

      if (node == originalChild) {
        this.children.set(index, newChild);

        newChild.parent = this;
        originalChild.parent = null;

        return;
      }
    }

    throw new RuntimeException("child does not exist");
  }

  @Override
  public final ParserSymbol getSymbol() {
    return this.symbol;
  }

  @Override
  public final String toString() {
    if (this.expectedSymbol != this.symbol && !isQuantifierNode()) {
      return String.format("%s (%s)", this.symbol.getName(), this.expectedSymbol.getName());
    } else {
      return this.symbol.getName();
    }
  }

  @Override
  public final String getText() {
    return "";
  }

  @Override
  public final void compactify() {
    int childIndex = 0;
    for (final Node<?> child : this.children) {
      child.compactify();

      if (canCompactify(child)) {
        assert (child instanceof NonTerminalNode);
        final Node<?> compactifiedChild = ((NonTerminalNode) child).getChild(0);

        // do not hoist auxiliary nodes (i.e., quantifier nodes and list item nodes)
        if (!((compactifiedChild instanceof NonTerminalNode)
            && ((NonTerminalNode) compactifiedChild).isAuxiliaryNode())) {
          this.children.set(childIndex, compactifiedChild);
          compactifiedChild.parent = this;

          compactifiedChild.setExpectedSymbol(child.getSymbol());
          compactifiedChild.addAnnotations(child.getAnnotations());
        }
      }

      ++childIndex;
    }
  }

  private final boolean canCompactify(final Node<?> child) {
    if (child.numberOfChildren() != 1) {
      return false;
    }
    
    // child has one child -> has to be a non-terminal node
    assert (child instanceof NonTerminalNode);
    final NonTerminalNode nonTerminalChild = (NonTerminalNode) child;

    return !nonTerminalChild.isAuxiliaryNode();
  }

  @Override
  public final Pair<Node<?>, Node<?>> cloneTree(final Node<?> subNode) {
    Node<?> clonedSubNode = null;

    final List<Node<?>> clonedChildren = new ArrayList<>();
    for (final Node<?> child : this.children) {
      final Pair<Node<?>, Node<?>> clonedChild = child.cloneTree(subNode);
      clonedChildren.add(clonedChild.getFirst());

      if (clonedChild.getSecond() != null) {
        assert (clonedSubNode == null);
        clonedSubNode = clonedChild.getSecond();
      }
    }

    final NonTerminalNode clone = cloneNode(clonedChildren);

    if (subNode == this) {
      assert (clonedSubNode == null);
      clonedSubNode = clone;
    }

    return new Pair<>(clone, clonedSubNode);
  }

  @Override
  protected final Node<?> cloneTree(final Collection<Node<?>> subNodes,
      final Map<Node<?>, Node<?>> clonedSubNodes) {
    final List<Node<?>> clonedChildren = new ArrayList<>();
    for (final Node<?> child : this.children) {
      final Node<?> clonedChild = child.cloneTree(subNodes, clonedSubNodes);
      clonedChildren.add(clonedChild);
    }

    final NonTerminalNode clone = cloneNode(clonedChildren);

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
  public final NonTerminalNode cloneNode() {
    return cloneNode(new ArrayList<Node<?>>());
  }

  public final NonTerminalNode cloneNode(final List<Node<?>> children) {
    final NonTerminalNode clone = new NonTerminalNode(this.symbol, children);
    clone.setExpectedSymbol(this.expectedSymbol);

    for (final Node<?> child : children) {
      child.parent = clone;
    }

    for (final Annotation annotation : this.annotations) {
      clone.annotations.add(annotation);
    }

    return clone;
  }

  @Override
  public final Node<?> pruneTo(final Set<Node<?>> keptNodes) {
    if (!keptNodes.contains(this)) {
      return null;
    }

    final Iterator<Node<?>> childIterator = this.children.iterator();
    while (childIterator.hasNext()) {
      final Node<?> child = childIterator.next();

      if (keptNodes.contains(child)) {
        child.pruneTo(keptNodes);
      } else {
        childIterator.remove();
        child.parent = null;
      }
    }

    return this;
  }

  @Override
  public final Node<?> prune(final Set<Node<?>> removedNodes) {
    if (removedNodes.contains(this)) {
      return null;
    }

    final Iterator<Node<?>> childIterator = this.children.iterator();
    while (childIterator.hasNext()) {
      final Node<?> child = childIterator.next();

      if (removedNodes.contains(child)) {
        childIterator.remove();
        child.parent = null;
      } else {
        child.prune(removedNodes);
      }
    }

    return this;
  }

  @Override
  public final <P, R> R accept(final SyntaxTreeVisitor<P, R> visitor, final P parameter) {
    return visitor.visit(this, parameter);
  }

}
