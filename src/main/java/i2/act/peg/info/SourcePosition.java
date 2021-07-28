package i2.act.peg.info;

import java.util.Objects;

public final class SourcePosition {

  public static final SourcePosition UNKNOWN = new SourcePosition(-1, -1, -1);

  public final int offset;

  public final int line;
  public final int column;
  
  public SourcePosition(final int offset, final int line, final int column) {
    this.offset = offset;
    this.line = line;
    this.column = column;
  }

  @Override
  public final String toString() {
    if (this == UNKNOWN) {
      return "UNKNOWN";
    } else {
      return String.format("%d:%d", this.line, this.column);
    }
  }

  @Override
  public final boolean equals(final Object other) {
    if (!(other instanceof SourcePosition)) {
      return false;
    }

    final SourcePosition otherSourcePosition = (SourcePosition) other;

    return this.offset == otherSourcePosition.offset
        && this.line == otherSourcePosition.line
        && this.column == otherSourcePosition.column;
  }

  @Override
  public final int hashCode() {
    return Objects.hash(this.offset, this.line, this.column);
  }

}
