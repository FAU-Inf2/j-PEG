package i2.act.peg.ast;

import i2.act.peg.ast.visitors.ASTVisitor;
import i2.act.peg.info.SourcePosition;
import i2.act.peg.symbols.LexerSymbol;

import java.util.List;

public final class LexerProduction extends Production<LexerIdentifier, LexerSymbol> {

  private final RegularExpression regularExpression;

  public LexerProduction(final SourcePosition position, final List<Annotation> annotations,
      final LexerIdentifier leftHandSide, final RegularExpression regularExpression) {
    super(position, annotations, leftHandSide);
    this.regularExpression = regularExpression;
  }

  public final boolean isSkippedToken() {
    return hasAnnotation(Annotation.SKIP);
  }

  public final RegularExpression getRegularExpression() {
    return this.regularExpression;
  }

  @Override
  public final LexerProduction clone(final boolean keepSymbols) {
    final LexerIdentifier leftHandSideClone = this.leftHandSide.clone(keepSymbols);
    final RegularExpression regularExpressionClone = this.regularExpression.clone(keepSymbols);
    final List<Annotation> annotationsClone = cloneAnnotations(keepSymbols);

    return new LexerProduction(
        this.position, annotationsClone, leftHandSideClone, regularExpressionClone);
  }

  @Override
  public final <P, R> R accept(final ASTVisitor<P, R> visitor, final P param) {
    return visitor.visit(this, param);
  }

}
