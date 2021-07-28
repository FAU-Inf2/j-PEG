package i2.act.peg.ast;

import i2.act.peg.info.SourcePosition;
import i2.act.peg.symbols.Symbol;

public abstract class Identifier<S extends Symbol<?>> extends Atom {

  protected final String name;

  protected S symbol;

  public Identifier(final SourcePosition position, final String name,
      final Atom.Quantifier quantifier) {
    super(position, quantifier);
    this.name = name;
  }

  public final String getName() {
    return this.name;
  }

  public final void setSymbol(final S symbol) {
    this.symbol = symbol;
  }

  public final S getSymbol() {
    return this.symbol;
  }

  @Override
  public String toString() {
    return String.format("<%s>", this.name);
  }

  @Override
  public abstract Identifier<S> clone(final boolean keepSymbols);

}
