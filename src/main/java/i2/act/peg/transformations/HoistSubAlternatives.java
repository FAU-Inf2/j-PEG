package i2.act.peg.transformations;

import i2.act.peg.ast.*;
import i2.act.peg.ast.visitors.BaseASTVisitor;
import i2.act.peg.ast.visitors.NameAnalysis;
import i2.act.peg.info.SourcePosition;

import java.util.ArrayList;
import java.util.List;

public final class HoistSubAlternatives implements GrammarTransformation {

  public static final Grammar transform(final Grammar originalGrammar) {
    return (new HoistSubAlternatives()).apply(originalGrammar);
  }

  @Override
  public final Grammar apply(final Grammar originalGrammar) {
    final Grammar transformedGrammar = new Grammar(SourcePosition.UNKNOWN);

    originalGrammar.accept(new BaseASTVisitor<Void, ASTNode>() {

      private final List<Production> helperProductions = new ArrayList<>();

      private String currentProductionName = null;
      private int currentProductionCounter = 0;

      @Override
      public final ASTNode epilog(final ASTNode node, final Void parameter) {
        throw new RuntimeException(
            String.format("unhandled node of class '%s'", node.getClass().getSimpleName()));
      }

      @Override
      public final Grammar visit(final Grammar grammar, final Void parameter) {
        for (final Production production : grammar) {
          final Production transformedProduction = (Production) production.accept(this, null);
          transformedGrammar.addProduction(transformedProduction);

          // add helper productions
          for (final Production helperProduction : this.helperProductions) {
            transformedGrammar.addProduction(helperProduction);
          }
          this.helperProductions.clear();
        }

        return transformedGrammar;
      }

      @Override
      public final LexerProduction visit(final LexerProduction lexerProduction,
          final Void parameter) {
        // we do not have to do anything for lexer productions
        return lexerProduction.clone(false);
      }

      @Override
      public final ParserProduction visit(final ParserProduction parserProduction,
          final Void parameter) {
        final ParserIdentifier leftHandSideClone = parserProduction.getLeftHandSide().clone(false);
        final List<Annotation> annotationsClone = parserProduction.cloneAnnotations(false);

        this.currentProductionName = parserProduction.getLeftHandSide().getName();
        this.currentProductionCounter = 0;

        final Alternatives rightHandSideClone =
            (Alternatives) parserProduction.getRightHandSide().accept(this, null);

        this.currentProductionName = null;

        return new ParserProduction(SourcePosition.UNKNOWN, annotationsClone, leftHandSideClone,
            rightHandSideClone);
      }

      @Override
      public final Alternatives visit(final Alternatives alternatives,
          final Void parameter) {
        final List<Sequence> transformedSequences = new ArrayList<>();
        {
          for (final Sequence sequence : alternatives.getAlternatives()) {
            final Sequence transformedSequence = (Sequence) sequence.accept(this, null);
            transformedSequences.add(transformedSequence);
          }
        }

        final Alternatives transformedAlternatives = new Alternatives(
            SourcePosition.UNKNOWN, Atom.Quantifier.QUANT_NONE, transformedSequences);

        return transformedAlternatives;
      }

      @Override
      public final Sequence visit(final Sequence sequence, final Void parameter) {
        final List<Atom> transformedElements = new ArrayList<>();
        for (final Atom element : sequence) {
          if (element instanceof Alternatives
              && !element.hasQuantifier()
              && ((Alternatives) element).getNumberOfAlternatives() == 1) {
            final Sequence subSequence = ((Alternatives) element).getAlternative(0);
            final Sequence transformedSubSequence = (Sequence) subSequence.accept(this, parameter);

            transformedElements.addAll(transformedSubSequence.getElements());
          } else {
            final Atom transformedElement = (Atom) element.accept(this, null);
            transformedElements.add(transformedElement);
          }
        }

        return new Sequence(SourcePosition.UNKNOWN, transformedElements);
      }

      @Override
      public final LexerIdentifier visit(final LexerIdentifier lexerIdentifier,
          final Void parameter) {
        return lexerIdentifier.clone(false);
      }

      @Override
      public final ParserIdentifier visit(final ParserIdentifier parserIdentifier,
          final Void parameter) {
        return parserIdentifier.clone(false);
      }

    }, null);

    // perform name analysis on transformed grammar to annotate all identifiers with symbols
    // TODO should we keep the symbols of the original grammar?
    NameAnalysis.analyze(transformedGrammar);

    return transformedGrammar;
  }

}
