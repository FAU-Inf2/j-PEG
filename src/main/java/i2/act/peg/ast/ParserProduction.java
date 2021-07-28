package i2.act.peg.ast;

import i2.act.peg.ast.visitors.ASTVisitor;
import i2.act.peg.info.SourcePosition;
import i2.act.peg.symbols.ParserSymbol;

import java.util.List;

public final class ParserProduction extends Production<ParserIdentifier, ParserSymbol> {

  private final Alternatives rightHandSide;

  public ParserProduction(final SourcePosition position, final List<Annotation> annotations,
      final ParserIdentifier leftHandSide,
      final Alternatives rightHandSide) {
    super(position, annotations, leftHandSide);
    this.rightHandSide = rightHandSide;
  }

  public final Alternatives getRightHandSide() {
    return this.rightHandSide;
  }

  @Override
  public final ParserProduction clone(final boolean keepSymbols) {
    final ParserIdentifier leftHandSideClone = this.leftHandSide.clone(keepSymbols);
    final Alternatives rightHandSideClone = this.rightHandSide.clone(keepSymbols);
    final List<Annotation> annotationsClone = cloneAnnotations(keepSymbols);

    return new ParserProduction(
        this.position, annotationsClone, leftHandSideClone, rightHandSideClone);
  }

  @Override
  public final <P, R> R accept(final ASTVisitor<P, R> visitor, final P param) {
    return visitor.visit(this, param);
  }

}
