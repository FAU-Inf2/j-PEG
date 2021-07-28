package i2.act.packrat;

import i2.act.peg.info.SourcePosition;
import i2.act.peg.symbols.LexerSymbol;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public final class TokenStream implements Iterable<Token> {

  public final Token EOF;

  private final List<Token> tokens;

  public TokenStream(final List<Token> tokens) {
    this.tokens = tokens;

    final SourcePosition eofPosition;
    {
      if (tokens.isEmpty()) {
        eofPosition = new SourcePosition(0, 1, 1);
      } else {
        eofPosition = tokens.get(tokens.size() - 1).getEnd();
      }
    }

    this.EOF = new Token(LexerSymbol.EOF, "", eofPosition, eofPosition);
  }

  public final Token at(final int position) {
    if (position >= this.tokens.size()) {
      return this.EOF;
    }
    return this.tokens.get(position);
  }

  public final List<Token> getTokens() {
    return Collections.unmodifiableList(this.tokens);
  }

  public final int numberOfTokens() {
    return this.tokens.size();
  }

  @Override
  public final Iterator<Token> iterator() {
    return getTokens().iterator();
  }

  @Override
  public final String toString() {
    return this.tokens.toString();
  }

}
