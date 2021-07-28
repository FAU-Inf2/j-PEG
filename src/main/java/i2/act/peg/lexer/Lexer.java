package i2.act.peg.lexer;

import i2.act.peg.error.InvalidInputException;
import i2.act.peg.info.SourcePosition;

public interface Lexer<K extends TokenKind, T extends Token<K>> {

  public SourcePosition getPosition();

  public void advanceTo(final SourcePosition sourcePosition);

  public T peek();

  default T pop() {
    final T token = peek();
    advanceTo(token.getEnd());

    return token;
  }

  default T assertPop(final K kind) {
    final T token = peek();
    final K actualKind = token.getKind();

    if (!kind.equals(actualKind)) {
      throw new InvalidInputException(
          String.format("expected '%s', but found '%s'", kind, actualKind), getPosition());
    }

    advanceTo(token.getEnd());

    return token;
  }

  @SuppressWarnings("unchecked")
  default void assertNotPeek(final K... kinds) {
    final T token = peek();
    final K actualKind = token.getKind();

    for (final K kind : kinds) {
      if (kind.equals(actualKind)) {
        throw new InvalidInputException(
            String.format("did not expect '%s'", actualKind));
      }
    }
  }

  @SuppressWarnings("unchecked")
  default void assertPeek(final K... kinds) {
    final T token = peek();
    final K actualKind = token.getKind();

    for (final K kind : kinds) {
      if (kind.equals(actualKind)) {
        return;
      }
    }

    // next token does not match any of the expected tokens -> throw exception

    final String expected;
    {
      final StringBuilder builder = new StringBuilder();

      for (int index = 0; index < kinds.length; ++index) {
        builder.append("'");
        builder.append(kinds[index]);
        builder.append("'");

        if (index < kinds.length - 1) {
          if (kinds.length > 2) {
            builder.append(",");
          }

          builder.append(" ");

          if (index == kinds.length - 2) {
            builder.append("or ");
          }
        }
      }

      expected = builder.toString();
    }

    throw new InvalidInputException(
        String.format("expected %s, but found '%s'", expected, actualKind), getPosition());
  }

  default boolean skip(final K kind) {
    final T token = peek();
    final K actualKind = token.getKind();

    if (kind.equals(actualKind)) {
      advanceTo(token.getEnd());
      return true;
    } else {
      return false;
    }
  }

}
