package i2.act.peg.ast;

import i2.act.peg.ast.visitors.ASTVisitor;
import i2.act.peg.info.SourcePosition;

public final class Quantifier extends ASTNode {

  public static enum Kind {

    QUANT_OPTIONAL("?"),
    QUANT_STAR("*"),
    QUANT_PLUS("+");

    public final String stringRepresentation;

    private Kind(final String stringRepresentation) {
      this.stringRepresentation = stringRepresentation;
    }

  }


  // -----------------------------------------------------------------------------------------------


  public static final Quantifier optional(final SourcePosition position) {
    return optional(position, 1);
  }

  public static final Quantifier optional(final SourcePosition position, final int weight) {
    return new Quantifier(position, Kind.QUANT_OPTIONAL, weight);
  }

  public static final Quantifier star(final SourcePosition position) {
    return star(position, 1);
  }

  public static final Quantifier star(final SourcePosition position, final int weight) {
    return new Quantifier(position, Kind.QUANT_STAR, weight);
  }

  public static final Quantifier plus(final SourcePosition position) {
    return plus(position, 1);
  }

  public static final Quantifier plus(final SourcePosition position, final int weight) {
    return new Quantifier(position, Kind.QUANT_PLUS, weight);
  }


  // -----------------------------------------------------------------------------------------------


  private final Kind kind;
  private final int weight;

  public Quantifier(final SourcePosition position, final Kind kind) {
    this(position, kind, 1);
  }

  public Quantifier(final SourcePosition position, final Kind kind, final int weight) {
    super(position);
    this.kind = kind;
    this.weight = weight;
  }

  public final Kind getKind() {
    return this.kind;
  }

  public final int getWeight() {
    return this.weight;
  }

  @Override
  public String toString() {
    if (this.weight == 1) {
      return String.format(
          "%s(%s)", getClass().getSimpleName(), this.kind.stringRepresentation);
    } else {
      return String.format(
          "%s(%s,%d)", getClass().getSimpleName(), this.kind.stringRepresentation, this.weight);
    }
  }

  @Override
  public final ASTNode clone(final boolean keepSymbols) {
    return new Quantifier(this.position, this.kind, this.weight);
  }

  @Override
  public final <P, R> R accept(final ASTVisitor<P, R> visitor, final P param) {
    return visitor.visit(this, param);
  }

}
