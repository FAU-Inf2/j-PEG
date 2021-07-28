package i2.act.peg.transformations;

import i2.act.peg.ast.*;
import i2.act.peg.ast.visitors.BaseASTVisitor;
import i2.act.peg.ast.visitors.NameAnalysis;
import i2.act.peg.info.SourcePosition;
import i2.act.peg.symbols.LexerSymbol;
import i2.act.peg.symbols.ParserSymbol;
import i2.act.peg.symbols.Symbol;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class RemoveChainProductions implements GrammarTransformation {

  public static final Grammar transform(final Grammar originalGrammar) {
    return (new RemoveChainProductions()).apply(originalGrammar);
  }

  @Override
  public final Grammar apply(final Grammar originalGrammar) {
    final Map<ParserSymbol, Symbol<?>> chainProductions = gatherChainProductions(originalGrammar);

    if (chainProductions.isEmpty()) {
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
              && chainProductions.containsKey(((ParserIdentifier) element).getSymbol())) {
            final Symbol<?> replacementSymbol = 
                chainProductions.get(((ParserIdentifier) element).getSymbol());

            final Identifier replacementIdentifier;
            {
              if (replacementSymbol instanceof ParserSymbol) {
                replacementIdentifier = new ParserIdentifier(
                    SourcePosition.UNKNOWN, replacementSymbol.getName(), element.getQuantifier());
              } else {
                assert (replacementSymbol instanceof LexerSymbol);
                replacementIdentifier = new LexerIdentifier(
                    SourcePosition.UNKNOWN, replacementSymbol.getName(), element.getQuantifier());
              }
            }

            transformedElements.add(replacementIdentifier);
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

  private final Map<ParserSymbol, Symbol<?>> gatherChainProductions(
      final Grammar originalGrammar) {
    final Map<ParserSymbol, Symbol<?>> chainProductions = new HashMap<>();

    for (final Production production : originalGrammar) {
      if (production instanceof ParserProduction) {
        final ParserProduction parserProduction = (ParserProduction) production;
        final Alternatives rightHandSide = parserProduction.getRightHandSide();

        final Symbol<?> chainSymbol = isChain(rightHandSide);
        if (chainSymbol != null) {
          final ParserSymbol parserSymbol = parserProduction.getSymbol();
          chainProductions.put(parserSymbol, chainSymbol);
        }
      }
    }

    return chainProductions;
  }

  private final Symbol<?> isChain(final Alternatives alternatives) {
    if (alternatives.getNumberOfAlternatives() != 1 || alternatives.hasQuantifier()) {
      return null;
    }

    final Sequence singleAlternative = alternatives.getAlternative(0);
    return isChain(singleAlternative);
  }

  private final Symbol<?> isChain(final Sequence sequence) {
    if (sequence.getNumberOfElements() != 1) {
      return null;
    }

    final Atom singleElement = sequence.getElement(0);

    if (singleElement instanceof Alternatives) {
      return isChain((Alternatives) singleElement);
    } else {
      assert (singleElement instanceof Identifier<?>);
      return isChain((Identifier<?>) singleElement);
    }
  }

  private final Symbol<?> isChain(final Identifier<?> identifier) {
    if (identifier.hasQuantifier()) {
      return null;
    }

    return identifier.getSymbol();
  }

}
