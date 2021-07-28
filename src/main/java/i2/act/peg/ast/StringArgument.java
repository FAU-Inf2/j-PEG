package i2.act.peg.ast;

import i2.act.peg.ast.visitors.ASTVisitor;
import i2.act.peg.info.SourcePosition;

public final class StringArgument extends Argument {

  private final String value;

  public StringArgument(final SourcePosition position, final String value) {
    super(position);
    this.value = value;
  }

  public final String getValue() {
    return this.value;
  }

  @Override
  public final String getSerialization() {
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
        case '"': {
          builder.append("\\\"");
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

  @Override
  public final StringArgument clone(final boolean keepSymbols) {
    return new StringArgument(this.position, this.value);
  }

  @Override
  public final <P, R> R accept(final ASTVisitor<P, R> visitor, final P param) {
    return visitor.visit(this, param);
  }

}
