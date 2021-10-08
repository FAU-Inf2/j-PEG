package i2.act.peg.ast;

import i2.act.peg.ast.visitors.ASTVisitor;
import i2.act.peg.info.SourcePosition;
import i2.act.peg.symbols.LexerSymbol;

public final class LexerIdentifier extends Identifier<LexerSymbol> {

  public LexerIdentifier(final SourcePosition position, final String name) {
    this(position, name, null);
  }

  public LexerIdentifier(final SourcePosition position, final String name,
      final Quantifier quantifier) {
    super(position, name, quantifier);
  }

  @Override
  public final LexerIdentifier clone(final boolean keepSymbols) {
    final LexerIdentifier clone = new LexerIdentifier(this.position, this.name, this.quantifier);

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
