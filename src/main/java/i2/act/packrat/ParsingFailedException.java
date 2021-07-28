package i2.act.packrat;

import i2.act.peg.info.SourcePosition;

public final class ParsingFailedException extends RuntimeException {

  private final SourcePosition position;

  public ParsingFailedException(final SourcePosition position, final String message) {
    super(String.format("parsing failed at %s: %s", position, message));
    this.position = position;
  }

  public final SourcePosition getPosition() {
    return this.position;
  }

}
