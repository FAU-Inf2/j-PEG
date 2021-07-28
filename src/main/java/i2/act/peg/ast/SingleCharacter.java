package i2.act.peg.ast;

import i2.act.peg.ast.visitors.ASTVisitor;
import i2.act.peg.info.SourcePosition;

public final class SingleCharacter extends Range {
  
  private final char value;

  public SingleCharacter(final SourcePosition position, final char value) {
    super(position);
    this.value = value;
  }

  public final char getValue() {
    return this.value;
  }

  public final String getEscapedValue() {
    return toEscapedString(this.value);
  }

  @Override
  public final String toString() {
    return String.format("<%s>", this.value);
  }

  @Override
  public final boolean inRange(final char character) {
    return character == this.value;
  }

  @Override
  public final SingleCharacter clone(final boolean keepSymbols) {
    return new SingleCharacter(this.position, this.value);
  }

  @Override
  public final <P, R> R accept(final ASTVisitor<P, R> visitor, final P param) {
    return visitor.visit(this, param);
  }

  public static final String toEscapedString(final char character) {
    switch (character) {
      case '\t': {
        return "\\t";
      }
      case '\n': {
        return "\\n";
      }
      case '\r': {
        return "\\r";
      }
      case '\'': {
        return "\\'";
      }
      case '\\': {
        return "\\\\";
      }
      default: {
        return String.valueOf(character);
      }
    }
  }

}
