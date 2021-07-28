package i2.act.peg.ast;

import i2.act.peg.ast.visitors.ASTVisitor;
import i2.act.peg.info.SourcePosition;

public final class IntArgument extends Argument {

  private final int value;

  public IntArgument(final SourcePosition position, final int value) {
    super(position);
    this.value = value;
  }

  public final int getValue() {
    return this.value;
  }

  @Override
  public final String getSerialization() {
    return String.valueOf(this.value);
  }

  @Override
  public final IntArgument clone(final boolean keepSymbols) {
    return new IntArgument(this.position, this.value);
  }

  @Override
  public final <P, R> R accept(final ASTVisitor<P, R> visitor, final P param) {
    return visitor.visit(this, param);
  }

}
