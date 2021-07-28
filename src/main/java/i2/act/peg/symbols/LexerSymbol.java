package i2.act.peg.symbols;

import i2.act.peg.ast.LexerProduction;

public final class LexerSymbol extends Symbol<LexerProduction> {

  public static final LexerSymbol EOF = new LexerSymbol("EOF", false, null);

  //------------------------------------------------------------------------------------------------

  private final boolean isSkippedToken;

  public LexerSymbol(final String name, final boolean isSkippedToken) {
    this(name, isSkippedToken, null);
  }

  public LexerSymbol(final String name, final boolean isSkippedToken,
      final LexerProduction production) {
    super(name, production);
    this.isSkippedToken = isSkippedToken;
  }

  public final boolean isSkippedToken() {
    return this.isSkippedToken;
  }

}
