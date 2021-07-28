package i2.act.peg.error;

import i2.act.peg.info.SourcePosition;

public final class InvalidInputException extends RuntimeException {

  public InvalidInputException(final String message) {
    this(message, SourcePosition.UNKNOWN);
  }

  public InvalidInputException(final String message, final SourcePosition position) {
    super(String.format("[%s] %s", position, message));
  }

}
