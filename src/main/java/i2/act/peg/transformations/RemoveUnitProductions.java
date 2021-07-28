package i2.act.peg.transformations;

import i2.act.peg.ast.*;
import i2.act.peg.ast.visitors.BaseASTVisitor;
import i2.act.peg.ast.visitors.NameAnalysis;
import i2.act.peg.info.SourcePosition;
import i2.act.peg.symbols.ParserSymbol;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class RemoveUnitProductions implements GrammarTransformation {

  public static final Grammar transform(final Grammar originalGrammar) {
    return (new RemoveUnitProductions()).apply(originalGrammar);
  }

  @Override
  public final Grammar apply(final Grammar originalGrammar) {
    final Map<ParserSymbol, Sequence> unitProductions = gatherUnitProductions(originalGrammar);

    if (unitProductions.isEmpty()) {
      return originalGrammar;
    }

    final Grammar transformedGrammar = new Grammar(SourcePosition.UNKNOWN);

    originalGrammar.accept(new BaseASTVisitor<Void, ASTNode>() {

      @Override
      public final ASTNode epilog(final ASTNode node, final Void parameter) {
        throw new RuntimeException(
            String.format("unhandled node of class '%s'", node.getClass().getSimpleName()));
      }

      @Override
      public final Grammar visit(final Grammar grammar, final Void parameter) {
        for (final Production production : grammar) {
          final Production transformedProduction = (Production) production.accept(this, parameter);
          transformedGrammar.addProduction(transformedProduction);
        }

        return transformedGrammar;
      }

      @Override
      public final LexerProduction visit(final LexerProduction lexerProduction,
          final Void parameter) {
        // we do not have to do anything for lexer productions
        final LexerProduction lexerProductionClone = lexerProduction.clone(false);
        return lexerProductionClone;
      }

      @Override
      public final ParserProduction visit(final ParserProduction parserProduction,
          final Void parameter) {
        final ParserIdentifier leftHandSideClone = parserProduction.getLeftHandSide().clone(false);
        final List<Annotation> annotationsClone = parserProduction.cloneAnnotations(false);

        final Alternatives rightHandSideClone =
            (Alternatives) parserProduction.getRightHandSide().accept(this, parameter);

        return new ParserProduction(SourcePosition.UNKNOWN, annotationsClone, leftHandSideClone,
            rightHandSideClone);
      }

      @Override
      public final Alternatives visit(final Alternatives alternatives, final Void parameter) {
        final List<Sequence> transformedSequences = new ArrayList<>();
        {
          for (final Sequence sequence : alternatives.getAlternatives()) {
            final Sequence transformedSequence = (Sequence) sequence.accept(this, parameter);
            transformedSequences.add(transformedSequence);
          }
        }

        return new Alternatives(
            SourcePosition.UNKNOWN, Atom.Quantifier.QUANT_NONE, transformedSequences);
      }

      @Override
      public final Sequence visit(final Sequence sequence, final Void parameter) {
        final List<Atom> transformedElements = new ArrayList<>();
        for (final Atom element : sequence) {
          if (element instanceof ParserIdentifier
              && unitProductions.containsKey(((ParserIdentifier) element).getSymbol())) {
            final Sequence replacement =
                unitProductions.get(((ParserIdentifier) element).getSymbol());
            final Sequence transformedReplacement = (Sequence) replacement.accept(this, parameter);

            for (final Atom replacementElement : transformedReplacement) {
              transformedElements.add(replacementElement);
            }
          } else {
            final Atom transformedElement = (Atom) element.accept(this, parameter);
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

  private final Map<ParserSymbol, Sequence> gatherUnitProductions(final Grammar originalGrammar) {
    final Map<ParserSymbol, Sequence> unitProductions = new HashMap<>();

    for (final Production production : originalGrammar) {
      if (production instanceof ParserProduction) {
        final ParserProduction parserProduction = (ParserProduction) production;
        final Alternatives rightHandSide = parserProduction.getRightHandSide();

        if (isUnit(rightHandSide)) {
          final ParserSymbol parserSymbol = parserProduction.getSymbol();
          unitProductions.put(parserSymbol, rightHandSide.getAlternative(0));
        }
      }
    }

    return unitProductions;
  }

  private final boolean isUnit(final Alternatives alternatives) {
    // TODO only consider "plain" sequences as unit?
    return alternatives.getNumberOfAlternatives() == 1;
  }

}
