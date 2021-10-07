package i2.act.peg.ast;

import i2.act.peg.ast.visitors.ASTVisitor;
import i2.act.peg.builder.GrammarBuilderNode;
import i2.act.peg.info.SourcePosition;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public final class Sequence extends Expression implements Iterable<Atom>, GrammarBuilderNode {

  private final List<Atom> elements;

  private int weight = 1;

  public Sequence(final SourcePosition position) {
    this(position, new ArrayList<Atom>());
  }

  public Sequence(final SourcePosition position, final List<Atom> elements) {
    super(position);
    this.elements = elements;
  }

  public final void addElement(final Atom element) {
    this.elements.add(element);
  }

  public final List<Atom> getElements() {
    return Collections.unmodifiableList(this.elements);
  }

  public final Atom getElement(final int index) {
    return this.elements.get(index);
  }

  public final int getNumberOfElements() {
    return this.elements.size();
  }

  public final int getWeight() {
    return this.weight;
  }

  public final void setWeight(final int weight) {
    this.weight = weight;
  }

  @Override
  public final Iterator<Atom> iterator() {
    return getElements().iterator();
  }

  @Override
  public final Sequence clone(final boolean keepSymbols) {
    final List<Atom> elementsClone = new ArrayList<>();
    {
      for (final Atom element : this.elements) {
        final Atom elementClone = element.clone(keepSymbols);
        elementsClone.add(elementClone);
      }
    }

    return new Sequence(this.position, elementsClone);
  }

  @Override
  public final <P, R> R accept(final ASTVisitor<P, R> visitor, final P param) {
    return visitor.visit(this, param);
  }

}
