package i2.act.peg.ast;

import i2.act.peg.builder.GrammarBuilderNode;
import i2.act.peg.info.SourcePosition;

public abstract class Atom extends Expression implements GrammarBuilderNode {

  public static enum Quantifier {

    QUANT_NONE(""),
    QUANT_OPTIONAL("?"),
    QUANT_STAR("*"),
    QUANT_PLUS("+");

    public final String stringRepresentation;

    private Quantifier(final String stringRepresentation) {
      this.stringRepresentation = stringRepresentation;
    }

  }

  protected Quantifier quantifier;

  public Atom(final SourcePosition position) {
    this(position, Quantifier.QUANT_NONE);
  }

  public Atom(final SourcePosition position, final Quantifier quantifier) {
    super(position);
    this.quantifier = quantifier;
  }

  public final boolean hasQuantifier() {
    return this.quantifier != Quantifier.QUANT_NONE;
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
