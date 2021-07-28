package i2.act.peg.transformations;

import i2.act.peg.ast.*;
import i2.act.peg.ast.visitors.BaseASTVisitor;
import i2.act.peg.info.SourcePosition;
import i2.act.peg.symbols.ParserSymbol;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class RemoveDuplicateProductions implements GrammarTransformation {

  public static final Grammar transform(final Grammar originalGrammar) {
    return (new RemoveDuplicateProductions()).apply(originalGrammar);
  }

  @Override
  public final Grammar apply(final Grammar originalGrammar) {
    Grammar grammar = originalGrammar;

    while (true) {
      final Map<ParserSymbol, ParserSymbol> duplicates =
          findDuplicates(grammar.getProductions());

      if (duplicates.isEmpty()) {
        return grammar;
      }

      System.err.println(duplicates);

      final Grammar transformedGrammar = new Grammar(SourcePosition.UNKNOWN);

      grammar.accept(new BaseASTVisitor<Void, ASTNode>() {

        @Override
        public final ASTNode epilog(final ASTNode node, final Void parameter) {
          throw new RuntimeException(
              String.format("unhandled node of class '%s'", node.getClass().getSimpleName()));
        }

        @Override
        public final Grammar visit(final Grammar grammar, final Void parameter) {
          for (final Production production : grammar) {
            final Production transformedProduction =
                (Production) production.accept(this, parameter);

            if (transformedProduction != null) {
              transformedGrammar.addProduction(transformedProduction);
            }
          }

          return transformedGrammar;
        }

        @Override
        public final LexerProduction visit(final LexerProduction lexerProduction,
            final Void parameter) {
          // we do not have to do anything for lexer productions
          final LexerProduction lexerProductionClone = lexerProduction.clone(true);
          return lexerProductionClone;
        }

        @Override
        public final ParserProduction visit(final ParserProduction parserProduction,
            final Void parameter) {
          final ParserSymbol symbol = parserProduction.getSymbol();
          assert (symbol != null);

          if (duplicates.containsKey(symbol)) {
            // duplicate production -> do not copy
            return null;
          }

          final ParserIdentifier leftHandSideClone = parserProduction.getLeftHandSide().clone(true);
          final List<Annotation> annotationsClone = parserProduction.cloneAnnotations(true);

          final Alternatives rightHandSideClone =
              (Alternatives) parserProduction.getRightHandSide().accept(this, parameter);

          return new ParserProduction(SourcePosition.UNKNOWN, annotationsClone, leftHandSideClone,
              rightHandSideClone);
        }

        @Override
        public final Atom visit(final Alternatives alternatives, final Void parameter) {
          final List<Sequence> transformedSequences = new ArrayList<>();
          {
            for (final Sequence sequence : alternatives.getAlternatives()) {
              final Sequence transformedSequence = (Sequence) sequence.accept(this, parameter);
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
            final Atom transformedElement = (Atom) element.accept(this, parameter);
            transformedElements.add(transformedElement);
          }

          return new Sequence(SourcePosition.UNKNOWN, transformedElements);
        }

        @Override
        public final LexerIdentifier visit(final LexerIdentifier lexerIdentifier,
            final Void parameter) {
          return lexerIdentifier.clone(true);
        }

        @Override
        public final ParserIdentifier visit(final ParserIdentifier parserIdentifier,
            final Void parameter) {
          final ParserSymbol symbol = parserIdentifier.getSymbol();
          assert (symbol != null);

          if (duplicates.containsKey(symbol)) {
            final ParserSymbol newSymbol = duplicates.get(symbol);

            final ParserIdentifier newIdentifier = new ParserIdentifier(
                SourcePosition.UNKNOWN, newSymbol.getName(), parserIdentifier.getQuantifier());
            newIdentifier.setSymbol(newSymbol);

            return newIdentifier;
          } else {
            return parserIdentifier.clone(true);
          }
        }

      }, null);

      grammar = transformedGrammar;
    }
  }

  private static final Map<ParserSymbol, ParserSymbol> findDuplicates(
      final List<Production> productions) {
    final Map<ParserSymbol, ParserSymbol> duplicates = new HashMap<>();

    for (int i = 0; i < productions.size(); ++i) {
      if (!(productions.get(i) instanceof ParserProduction)) {
        continue;
      }

      final ParserProduction productionOne = (ParserProduction) productions.get(i);
      final ParserSymbol symbolOne = productionOne.getSymbol();

      if (duplicates.containsKey(symbolOne)) {
        continue;
      }

      for (int j = i + 1; j < productions.size(); ++j) {
        if (!(productions.get(j) instanceof ParserProduction)) {
          continue;
        }

        final ParserProduction productionTwo = (ParserProduction) productions.get(j);

        if (matches(productionOne, productionTwo)) {
          final ParserSymbol symbolTwo = productionTwo.getSymbol();
          duplicates.put(symbolTwo, symbolOne);
        }
      }
    }

    return duplicates;
  }

  private static final boolean matches(final ParserProduction productionOne,
      final ParserProduction productionTwo) {
    return productionOne.getRightHandSide().accept(new BaseASTVisitor<Expression, Boolean>() {

      @Override
      public final Boolean epilog(final ASTNode node, final Expression two) {
        throw new RuntimeException(
            String.format("unhandled node of class '%s'", node.getClass().getSimpleName()));
      }
      
      @Override
      public final Boolean visit(final Alternatives alternativesOne, final Expression two) {
        if (!(two instanceof Alternatives)) {
          return false;
        }

        final Alternatives alternativesTwo = (Alternatives) two;

        if (alternativesOne.getQuantifier() != alternativesTwo.getQuantifier()) {
          return false;
        }

        final List<Sequence> sequencesOne = alternativesOne.getAlternatives();
        final List<Sequence> sequencesTwo = alternativesTwo.getAlternatives();

        if (sequencesOne.size() != sequencesTwo.size()) {
          return false;
        }

        for (int index = 0; index < sequencesOne.size(); ++index) {
          final Sequence sequenceOne = sequencesOne.get(index);
          final Sequence sequenceTwo = sequencesTwo.get(index);

          if (!sequenceOne.accept(this, sequenceTwo)) {
            return false;
          }
        }

        return true;
      }
      
      @Override
      public final Boolean visit(final Sequence sequenceOne, final Expression two) {
        if (!(two instanceof Sequence)) {
          return false;
        }

        final Sequence sequenceTwo = (Sequence) two;

        final List<Atom> elementsOne = sequenceOne.getElements();
        final List<Atom> elementsTwo = sequenceTwo.getElements();

        if (elementsOne.size() != elementsTwo.size()) {
          return false;
        }

        for (int index = 0; index < elementsOne.size(); ++index) {
          final Atom elementOne = elementsOne.get(index);
          final Atom elementTwo = elementsTwo.get(index);

          if (!elementOne.accept(this, elementTwo)) {
            return false;
          }
        }

        return true;
      }
      
      @Override
      public final Boolean visit(final ParserIdentifier identifierOne, final Expression two) {
        if (!(two instanceof ParserIdentifier)) {
          return false;
        }

        final ParserIdentifier identifierTwo = (ParserIdentifier) two;
        return matches(identifierOne, identifierTwo);
      }

      @Override
      public final Boolean visit(final LexerIdentifier identifierOne, final Expression two) {
        if (!(two instanceof LexerIdentifier)) {
          return false;
        }

        final LexerIdentifier identifierTwo = (LexerIdentifier) two;
        return matches(identifierOne, identifierTwo);
      }

      private final boolean matches(final Identifier<?> identifierOne,
          final Identifier<?> identifierTwo) {
        assert (identifierOne.getSymbol() != null);
        assert (identifierTwo.getSymbol() != null);

        return (identifierOne.getQuantifier() == identifierTwo.getQuantifier())
            && (identifierOne.getSymbol() == identifierTwo.getSymbol());
      }

    }, productionTwo.getRightHandSide());
  }

}
