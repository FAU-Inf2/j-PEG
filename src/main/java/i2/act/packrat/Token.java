package i2.act.packrat;

import i2.act.peg.info.SourcePosition;
import i2.act.peg.symbols.LexerSymbol;

import java.util.*;

public final class Token {

  public static final Token pseudoToken(final String value) {
    return new Token(null, value);
  }

  // ===============================================================================================

  private final LexerSymbol tokenSymbol;
  private final String value;

  private final SourcePosition begin;
  private final SourcePosition end;

  private final List<Token> skippedTokensBefore;

  public final Map<Parser, ParserResult> packratCache;

  public Token(final LexerSymbol tokenSymbol, final String value) {
    this(tokenSymbol, value, SourcePosition.UNKNOWN, SourcePosition.UNKNOWN);
  }

  public Token(final LexerSymbol tokenSymbol, final String value,
      final SourcePosition begin, final SourcePosition end) {
    this(tokenSymbol, value, begin, end, new ArrayList<Token>());
  }

  public Token(final LexerSymbol tokenSymbol, final String value,
      final SourcePosition begin, final SourcePosition end, final List<Token> skippedTokensBefore) {
    this.tokenSymbol = tokenSymbol;
    this.value = value;

    this.begin = begin;
    this.end = end;

    this.skippedTokensBefore = skippedTokensBefore;

    this.packratCache = new HashMap<Parser, ParserResult>();
  }

  public final LexerSymbol getTokenSymbol() {
    return this.tokenSymbol;
  }

  public final String getValue() {
    return this.value;
  }

  public final String getEscapedValue() {
    final StringBuilder builder = new StringBuilder();

    for (final char character : this.value.toCharArray()) {
      switch (character) {
        case '\t': {
          builder.append("\\t");
          break;
        }
        case '\n': {
          builder.append("\\n");
          break;
        }
        case '\r': {
          builder.append("\\r");
          break;
        }
        case '\'': {
          builder.append("\\'");
          break;
        }
        case '\\': {
          builder.append("\\\\");
          break;
        }
        default: {
          builder.append(character);
        }
      }
    }

    return builder.toString();
  }

  public final SourcePosition getBegin() {
    return this.begin;
  }

  public final SourcePosition getEnd() {
    return this.end;
  }

  public final List<Token> getSkippedTokensBefore() {
    return Collections.unmodifiableList(this.skippedTokensBefore);
  }

  public final boolean hasParserFailure() {
    for (final ParserResult result : this.packratCache.values()) {
      if (result instanceof ParserFailure) {
        return true;
      }
    }

    return false;
  }

  @Override
  public final Token clone() {
    return clone(this.begin, this.end);
  }

  public final Token clone(final SourcePosition begin, final SourcePosition end) {
    final List<Token> skippedTokensBefore = new ArrayList<>();
    {
      for (final Token skippedToken : this.skippedTokensBefore) {
        skippedTokensBefore.add(skippedToken.clone());
      }
    }

    return new Token(this.tokenSymbol, this.value, begin, end, skippedTokensBefore);
  }

  @Override
  public final String toString() {
    return String.format("<%s, '%s', %s>",
        (this.tokenSymbol == null) ? "null" : this.tokenSymbol.getName(), this.value, this.begin);
  }

}
