package i2.act.peg.ast;

import i2.act.peg.builder.GrammarBuilderNode;
import i2.act.peg.info.SourcePosition;

public abstract class Atom extends Expression implements GrammarBuilderNode {

  protected Quantifier quantifier;

  public Atom(final SourcePosition position) {
    this(position, null);
  }

  public Atom(final SourcePosition position, final Quantifier quantifier) {
    super(position);
    this.quantifier = quantifier;
  }

  public final boolean hasQuantifier() {
    return this.quantifier != null;
  }

  public final Quantifier getQuantifier() {
    return this.quantifier;
  }

  public final void setQuantifier(final Quantifier quantifier) {
    this.quantifier = quantifier;
  }

  @Override
  public abstract Atom clone(final boolean keepSymbols);

}
