package i2.act.peg.ast;

import i2.act.peg.ast.visitors.ASTVisitor;
import i2.act.peg.info.SourcePosition;
import i2.act.peg.symbols.ParserSymbol;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public final class Alternatives extends Atom implements Iterable<Sequence> {

  private final List<Sequence> alternatives;

  private ParserSymbol implicitQuantifierSymbol;

  public Alternatives(final SourcePosition position) {
    this(position, null);
  }

  public Alternatives(final SourcePosition position, final Quantifier quantifier) {
    this(position, quantifier, new ArrayList<Sequence>());
  }

  public Alternatives(final SourcePosition position, final Quantifier quantifier,
      final List<Sequence> alternatives) {
    this(position, quantifier, alternatives, null);
  }

  public Alternatives(final SourcePosition position, final Quantifier quantifier,
      final List<Sequence> alternatives, final ParserSymbol implicitQuantifierSymbol) {
    super(position, quantifier);
    this.alternatives = alternatives;
    this.implicitQuantifierSymbol = implicitQuantifierSymbol;
  }

  public final void addAlternative(final Sequence sequence) {
    this.alternatives.add(sequence);
  }

  public final List<Sequence> getAlternatives() {
    return Collections.unmodifiableList(this.alternatives);
  }

  public final int getNumberOfAlternatives() {
    return this.alternatives.size();
  }

  public final Sequence getAlternative(final int index) {
    return this.alternatives.get(index);
  }

  public final void replaceAlternatives(final List<Sequence> newAlternatives) {
    this.alternatives.clear();
    this.alternatives.addAll(newAlternatives);
  }

  public final void setImplicitQuantifierSymbol(final ParserSymbol implicitQuantifierSymbol) {
    this.implicitQuantifierSymbol = implicitQuantifierSymbol;
  }

  public final boolean hasImplicitQuantifierSymbol() {
    return this.implicitQuantifierSymbol != null;
  }

  public final ParserSymbol getImplicitQuantifierSymbol() {
    return this.implicitQuantifierSymbol;
  }

  @Override
  public final Iterator<Sequence> iterator() {
    return getAlternatives().iterator();
  }

  @Override
  public final Alternatives clone(final boolean keepSymbols) {
    final List<Sequence> alternativesClone = new ArrayList<>();
    {
      for (final Sequence alternative : this.alternatives) {
        final Sequence alternativeClone = alternative.clone(keepSymbols);
        alternativesClone.add(alternativeClone);
      }
    }

    return new Alternatives(this.position, this.quantifier, alternativesClone,
        this.implicitQuantifierSymbol);
  }

  @Override
  public final <P, R> R accept(final ASTVisitor<P, R> visitor, final P param) {
    return visitor.visit(this, param);
  }

}
