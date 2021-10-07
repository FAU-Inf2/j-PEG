package i2.act.peg.lexer;

import i2.act.peg.error.InvalidInputException;
import i2.act.peg.info.SourcePosition;
import i2.act.peg.lexer.PEGToken.PEGTokenKind;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.Function;

public final class PEGLexer implements Lexer<PEGTokenKind, PEGToken>, Iterable<PEGToken> {

  private final char[] characters;

  private int position;

  private int line;
  private int column;

  private int lookaheadPosition;
  private int lookaheadLine;
  private int lookaheadColumn;

  public PEGLexer(final String string) {
    this(string.toCharArray());
  }

  public PEGLexer(final char[] characters) {
    this.characters = characters;
    this.position = 0;
    this.line = 1;
    this.column = 1;
  }

  @Override
  public final SourcePosition getPosition() {
    return new SourcePosition(this.position, this.line, this.column);
  }

  @Override
  public final void advanceTo(final SourcePosition position) {
    this.position = position.offset;
    this.line = position.line;
    this.column = position.column;
  }

  @Override
  public final PEGToken peek() {
    return peek(false);
  }

  public final PEGToken peek(final boolean forceCharacterToken) {
    this.lookaheadPosition = this.position;
    this.lookaheadLine = this.line;
    this.lookaheadColumn = this.column;

    while (true) { // skip whitespace
      if (this.lookaheadPosition >= this.characters.length) {
        final SourcePosition begin =
            new SourcePosition(this.lookaheadPosition, this.lookaheadLine, this.lookaheadColumn);
        return new PEGToken(PEGTokenKind.TK_EOF, begin);
      }

      if (forceCharacterToken) {
        return parseCharacter();
      }

      final char firstChar = this.characters[this.lookaheadPosition];

      // handle consecutive whitespace
      if (isWhitespaceCharacter(firstChar)) {
        while (this.lookaheadPosition < this.characters.length) {
          final char nextChar = this.characters[this.lookaheadPosition];

          if (!isWhitespaceCharacter(nextChar)) {
            break;
          }

          ++this.lookaheadPosition;
          ++this.lookaheadColumn;

          if (nextChar == '\n') {
            ++this.lookaheadLine;
            this.lookaheadColumn = 1;
          }
        }

        continue;
      }

      // handle comments
      if (isCommentCharacter(firstChar)) {
        while (this.lookaheadPosition < this.characters.length) {
          final char nextChar = this.characters[this.lookaheadPosition];

          if (nextChar == '\n') {
            break;
          }

          ++this.lookaheadPosition;
          ++this.lookaheadColumn;
        }

        continue;
      }

      final SourcePosition begin =
          new SourcePosition(this.lookaheadPosition, this.lookaheadLine, this.lookaheadColumn);

      switch (firstChar) {
        case ':': {
          return new PEGToken(PEGTokenKind.TK_ASSIGN, begin);
        }
        case ';': {
          return new PEGToken(PEGTokenKind.TK_TERMINATOR, begin);
        }
        case '|': {
          return new PEGToken(PEGTokenKind.TK_OR, begin);
        }
        case '(': {
          return new PEGToken(PEGTokenKind.TK_LPAREN, begin);
        }
        case ')': {
          return new PEGToken(PEGTokenKind.TK_RPAREN, begin);
        }
        case '[': {
          return new PEGToken(PEGTokenKind.TK_LBRACK, begin);
        }
        case ']': {
          return new PEGToken(PEGTokenKind.TK_RBRACK, begin);
        }
        case '<': {
          return new PEGToken(PEGTokenKind.TK_LANGLE, begin);
        }
        case '>': {
          return new PEGToken(PEGTokenKind.TK_RANGLE, begin);
        }
        case '^': {
          return new PEGToken(PEGTokenKind.TK_NEGATE, begin);
        }
        case '?': {
          return new PEGToken(PEGTokenKind.TK_OPTIONAL, begin);
        }
        case '*': {
          return new PEGToken(PEGTokenKind.TK_STAR, begin);
        }
        case '+': {
          return new PEGToken(PEGTokenKind.TK_PLUS, begin);
        }
        case '-': {
          return new PEGToken(PEGTokenKind.TK_DASH, begin);
        }
        case '\'': {
          ++this.lookaheadPosition;
          ++this.lookaheadColumn;

          final String literal = parseStringContent('\'', begin);

          final SourcePosition end =
              new SourcePosition(this.lookaheadPosition, this.lookaheadLine, this.lookaheadColumn);

          return new PEGToken(PEGTokenKind.TK_LITERAL, begin, end, literal);
        }
        case '"': {
          ++this.lookaheadPosition;
          ++this.lookaheadColumn;

          final String literal = parseStringContent('"', begin);

          final SourcePosition end =
              new SourcePosition(this.lookaheadPosition, this.lookaheadLine, this.lookaheadColumn);

          return new PEGToken(PEGTokenKind.TK_STRING, begin, end, literal);
        }
        case '0':
        case '1':
        case '2':
        case '3':
        case '4':
        case '5':
        case '6':
        case '7':
        case '8':
        case '9': {
          final Function<Character, Boolean> matches = (c -> (c >= '0' && c <= '9'));

          final String value = parseCharacterSequence(matches);

          final SourcePosition end =
              new SourcePosition(this.lookaheadPosition, this.lookaheadLine, this.lookaheadColumn);

          return new PEGToken(PEGTokenKind.TK_INT, begin, end, value);
        }
        case '@': {
          ++this.lookaheadPosition;
          ++this.lookaheadColumn;

          final Function<Character, Boolean> matches =
              (c -> ((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || (c == '_')));

          final String annotationKey = parseCharacterSequence(matches);

          final SourcePosition end =
              new SourcePosition(this.lookaheadPosition, this.lookaheadLine, this.lookaheadColumn);

          return new PEGToken(PEGTokenKind.TK_ANNOTATION_KEY, begin, end, annotationKey);
        }
        default: {
          final Function<Character, Boolean> matches;
          final PEGTokenKind tokenKind;
          {
            if (firstChar >= 'A' && firstChar <= 'Z') {
              matches = (c -> ((c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || c == '_'));
              tokenKind = PEGTokenKind.TK_LEXER_ID;
            } else if (firstChar >= 'a' && firstChar <= 'z') {
              matches = (c -> ((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '_'));
              tokenKind = PEGTokenKind.TK_PARSER_ID;
            } else {
              final SourcePosition end = new SourcePosition(
                  this.lookaheadPosition + 1, this.lookaheadLine, this.lookaheadColumn + 1);

              return new PEGToken(
                  PEGTokenKind.TK_PLAIN_CHAR, begin, end, String.valueOf(firstChar));
            }
          }

          final String tokenText = parseCharacterSequence(matches);

          final SourcePosition end =
              new SourcePosition(this.lookaheadPosition, this.lookaheadLine, this.lookaheadColumn);

          return new PEGToken(tokenKind, begin, end, tokenText);
        }
      }
    }
  }

  private final PEGToken parseCharacter() {
    assert (this.lookaheadPosition < this.characters.length);

    final char firstChar = this.characters[this.lookaheadPosition];

    final SourcePosition begin =
        new SourcePosition(this.lookaheadPosition, this.lookaheadLine, this.lookaheadColumn);

    ++this.lookaheadPosition;
    ++this.lookaheadColumn;

    final String characterString;

    if (firstChar == '\\' && this.lookaheadPosition < this.characters.length) {
      final char escapedChar = this.characters[this.lookaheadPosition];

      ++this.lookaheadPosition;
      ++this.lookaheadColumn;

      characterString = getEscapedValue(escapedChar);
    } else {
      characterString = String.valueOf(firstChar);
    }

    final SourcePosition end = new SourcePosition(
        this.lookaheadPosition, this.lookaheadLine, this.lookaheadColumn);

    return new PEGToken(PEGTokenKind.TK_PLAIN_CHAR, begin, end, characterString);
  }

  private final String getEscapedValue(final char escapedChar) {
    switch (escapedChar) {
      case 't': {
        return "\t";
      }
      case 'n': {
        return "\n";
      }
      case 'r': {
        return "\r";
      }
      case '\'': {
        return "'";
      }
      case '"': {
        return "\"";
      }
      case '\\': {
        return "\\";
      }
      default: {
        final SourcePosition escapePosition = new SourcePosition(
            this.lookaheadPosition - 2, this.lookaheadLine, this.lookaheadColumn - 2);

        throw new InvalidInputException(
            String.format("unknown escape sequence '\\%s'", escapedChar), escapePosition);
      }
    }
  }

  private final String parseStringContent(final char terminator, final SourcePosition begin) {
    final StringBuilder builder = new StringBuilder();

    boolean foundEnd = false;

    while (this.lookaheadPosition < this.characters.length) {
      final char nextChar = this.characters[this.lookaheadPosition];

      ++this.lookaheadPosition;
      ++this.lookaheadColumn;

      if (nextChar == terminator) {
        // found end of literal
        foundEnd = true;
        break;
      } else if (nextChar == '\n' || nextChar == '\r') {
        System.out.println(builder.toString());
        throw new InvalidInputException("unclosed literal", begin);
      } else if (nextChar == '\\') {
        if (this.lookaheadPosition == this.characters.length) {
          throw new InvalidInputException("unclosed literal", begin);
        }

        final char escapedChar = this.characters[this.lookaheadPosition];

        ++this.lookaheadPosition;
        ++this.lookaheadColumn;

        final String escapedValue = getEscapedValue(escapedChar);
        builder.append(escapedValue);
      } else {
        builder.append(nextChar);
      }
    }

    if (!foundEnd) {
      throw new InvalidInputException("unclosed literal", begin);
    }

    return builder.toString();
  }

  private final String parseCharacterSequence(final Function<Character, Boolean> matches) {
    final StringBuilder builder = new StringBuilder();

    while (this.lookaheadPosition < this.characters.length) {
      final char nextChar = this.characters[this.lookaheadPosition];

      if (matches.apply(nextChar)) {
        builder.append(nextChar);

        ++this.lookaheadPosition;
        ++this.lookaheadColumn;

        assert (nextChar != '\n');
      } else {
        break;
      }
    }

    return builder.toString();
  }

  @Override
  public final PEGToken pop() {
    return pop(false);
  }

  public final PEGToken pop(final boolean forceCharacterToken) {
    final PEGToken token = peek(forceCharacterToken);
    advanceTo(token.getEnd());

    return token;
  }

  private static final boolean isWhitespaceCharacter(final char c) {
    return c == ' ' || c == '\n' || c == '\r' || c == '\t';
  }

  private static final boolean isCommentCharacter(final char c) {
    return c == '#';
  }

  @Override
  public final Iterator<PEGToken> iterator() {
    return new Iterator<PEGToken>() {

        private boolean hasReturnedEOF = false;

        @Override
        public final boolean hasNext() {
          return !this.hasReturnedEOF;
        }

        @Override
        public final PEGToken next() {
          if (!hasNext()) {
            throw new NoSuchElementException();
          }

          final PEGToken token = pop();

          if (token.kind == PEGTokenKind.TK_EOF) {
            this.hasReturnedEOF = true;
          }

          return token;
        }

    };
  }

}
