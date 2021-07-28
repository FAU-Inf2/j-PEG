package i2.act.peg.ast;

import i2.act.peg.ast.visitors.ASTVisitor;
import i2.act.peg.info.SourcePosition;
import i2.act.peg.symbols.ParserSymbol;

public final class ParserIdentifier extends Identifier<ParserSymbol> {

  public ParserIdentifier(final SourcePosition position, final String name) {
    this(position, name, Atom.Quantifier.QUANT_NONE);
  }

  public ParserIdentifier(final SourcePosition position, final String name,
      final Atom.Quantifier quantifier) {
    super(position, name, quantifier);
  }

  @Override
  public final ParserIdentifier clone(final boolean keepSymbols) {
    final ParserIdentifier clone = new ParserIdentifier(this.position, this.name, this.quantifier);

    if (keepSymbols) {
      clone.setSymbol(this.symbol);
    }

    return clone;
  }

  @Override
  public final <P, R> R accept(final ASTVisitor<P, R> visitor, final P param) {
    return visitor.visit(this, param);
  }

}
