package i2.act.peg.ast;

import i2.act.peg.ast.visitors.ASTVisitor;
import i2.act.peg.info.SourcePosition;

public final class Literal extends Atom {
  
  private final String value;

  public Literal(final SourcePosition position, final String value) {
    this(position, value, Atom.Quantifier.QUANT_NONE);
  }

  public Literal(final SourcePosition position, final String value,
      final Atom.Quantifier quantifier) {
    super(position, quantifier);
    this.value = value;
  }

  public final String getValue() {
    return this.value;
  }

  public final String getEscapedValue() {
    final StringBuilder builder = new StringBuilder();

    for (final char character : this.value.toCharArray()) {
      builder.append(SingleCharacter.toEscapedString(character));
    }

    return builder.toString();
  }

  @Override
  public final String toString() {
    return String.format("<%s>", getEscapedValue());
  }

  @Override
  public final Literal clone(final boolean keepSymbols) {
    return new Literal(this.position, this.value, this.quantifier);
  }

  @Override
  public final <P, R> R accept(final ASTVisitor<P, R> visitor, final P param) {
    return visitor.visit(this, param);
  }

}
