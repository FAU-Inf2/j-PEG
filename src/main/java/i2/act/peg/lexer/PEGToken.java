package i2.act.peg.lexer;

import i2.act.peg.info.SourcePosition;

public final class PEGToken implements Token<PEGToken.PEGTokenKind> {

  public static enum PEGTokenKind implements TokenKind {

    TK_ASSIGN(":"),
    TK_TERMINATOR(";"),
    TK_OR("|"),
    TK_LPAREN("("),
    TK_RPAREN(")"),
    TK_LBRACK("["),
    TK_RBRACK("]"),
    TK_NEGATE("^"),
    TK_OPTIONAL("?"),
    TK_STAR("*"),
    TK_PLUS("+"),
    TK_DASH("-"),
    TK_PLAIN_CHAR("PLAIN_CHAR"),
    TK_PARSER_ID("PARSER_ID"),
    TK_LEXER_ID("LEXER_ID"),
    TK_LITERAL("LITERAL"),
    TK_ANNOTATION_KEY("ANNOTATION_KEY"),
    TK_INT("INT"),
    TK_STRING("STRING"),
    TK_EOF("EOF");

    public final String stringRepresentation;

    private PEGTokenKind(final String stringRepresentation) {
      this.stringRepresentation = stringRepresentation;
    }

    @Override
    public final String toString() {
      return this.stringRepresentation;
    }

  }

  public final PEGTokenKind kind;

  public final SourcePosition begin;
  public final SourcePosition end;

  public final String string;

  public PEGToken(final PEGTokenKind kind, final SourcePosition begin) {
    this(kind, begin,
        new SourcePosition(
            begin.offset + kind.stringRepresentation.length(),
            begin.line,
            begin.column + kind.stringRepresentation.length()),
        null);
  }

  public PEGToken(final PEGTokenKind kind, final SourcePosition begin, final SourcePosition end) {
    this(kind, begin, end, null);
  }

  public PEGToken(final PEGTokenKind kind, final SourcePosition begin, final SourcePosition end,
      final String string) {
    this.kind = kind;

    this.begin = begin;
    this.end = end;

    this.string = string;
  }

  @Override
  public final PEGTokenKind getKind() {
    return this.kind;
  }

  @Override
  public final SourcePosition getBegin() {
    return this.begin;
  }

  @Override
  public final SourcePosition getEnd() {
    return this.end;
  }

  @Override
  public final String toString() {
    if (this.string == null) {
      return String.format("TK<%s, %s>", this.kind, this.begin);
    } else {
      return String.format("TK<%s:'%s', %s, %s>", this.kind, this.string, this.begin, this.end);
    }
  }

}
