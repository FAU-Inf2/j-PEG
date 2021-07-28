package i2.act.peg.ast;

import i2.act.peg.ast.visitors.ASTVisitor;
import i2.act.peg.info.SourcePosition;

public final class RegularExpression extends ASTNode {

  private final Alternatives alternatives;

  public RegularExpression(final SourcePosition sourcePosition, final Alternatives alternatives) {
    super(sourcePosition);
    this.alternatives = alternatives;
  }

  public final Alternatives getAlternatives() {
    return this.alternatives;
  }

  @Override
  public final RegularExpression clone(final boolean keepSymbols) {
    final Alternatives alternativesClone = this.alternatives.clone(keepSymbols);
    return new RegularExpression(this.position, alternativesClone);
  }

  @Override
  public final <P, R> R accept(final ASTVisitor<P, R> visitor, final P param) {
    return visitor.visit(this, param);
  }

}
