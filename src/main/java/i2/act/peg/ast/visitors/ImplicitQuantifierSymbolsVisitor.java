package i2.act.peg.ast.visitors;

import i2.act.peg.ast.*;
import i2.act.peg.symbols.ParserSymbol;

public final class ImplicitQuantifierSymbolsVisitor extends BaseASTVisitor<Void, Void> {

  public static final void addImplicitQuantifierSymbols(final Grammar grammar) {
    final ImplicitQuantifierSymbolsVisitor visitor = new ImplicitQuantifierSymbolsVisitor();
    visitor.visit(grammar, null);
  }


  //------------------------------------------------------------------------------------------------


  private boolean isRegularExpression;
  private int implicitQuantifierSymbolCounter = 0;

  @Override
  public final Void visit(final ParserProduction parserProduction, final Void parameter) {
    this.isRegularExpression = false;
    return super.visit(parserProduction, parameter);
  }

  @Override
  public final Void visit(final LexerProduction lexerProduction, final Void parameter) {
    this.isRegularExpression = true;
    return super.visit(lexerProduction, parameter);
  }

  @Override
  public final Void visit(final Alternatives alternatives, final Void parameter) {
    super.visit(alternatives, parameter);

    if (!this.isRegularExpression
        && alternatives.hasQuantifier() && needsImplicitQuantifierSymbol(alternatives)) {
      final ParserSymbol implicitQuantifierSymbol = createImplicitQuantifierSymbol();
      alternatives.setImplicitQuantifierSymbol(implicitQuantifierSymbol);
    }

    return null;
  }

  private final boolean needsImplicitQuantifierSymbol(final Alternatives alternatives) {
    if (alternatives.hasImplicitQuantifierSymbol()) {
      return false;
    }

    if (alternatives.getNumberOfAlternatives() > 1) {
      return true;
    }

    final Sequence singleSequence = alternatives.getAlternative(0);
    return needsImplicitQuantifierSymbol(singleSequence);
  }

  private final boolean needsImplicitQuantifierSymbol(final Sequence sequence) {
    if (sequence.getNumberOfElements() > 1) {
      return true;
    }

    final Atom singleElement = sequence.getElement(0);

    if (singleElement.hasQuantifier()) {
      return true;
    }

    if (singleElement instanceof Identifier<?>) {
      return false;
    } else {
      assert (singleElement instanceof Alternatives);
      return needsImplicitQuantifierSymbol((Alternatives) singleElement);
    }
  }

  private final ParserSymbol createImplicitQuantifierSymbol() {
    final String symbolName = String.format("_quantified_%d",
        ++this.implicitQuantifierSymbolCounter);
    return new ParserSymbol(symbolName);
  }

}
