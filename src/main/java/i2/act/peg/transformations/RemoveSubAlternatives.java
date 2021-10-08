package i2.act.peg.transformations;

import i2.act.peg.ast.*;
import i2.act.peg.ast.visitors.BaseASTVisitor;
import i2.act.peg.ast.visitors.NameAnalysis;
import i2.act.peg.info.SourcePosition;

import java.util.ArrayList;
import java.util.List;

public final class RemoveSubAlternatives implements GrammarTransformation {

  public static final Grammar transform(final Grammar originalGrammar) {
    return (new RemoveSubAlternatives()).apply(originalGrammar);
  }

  @Override
  public final Grammar apply(final Grammar originalGrammar) {
    final Grammar transformedGrammar = new Grammar(SourcePosition.UNKNOWN);

    originalGrammar.accept(new BaseASTVisitor<Boolean, ASTNode>() {

      private final List<Production> helperProductions = new ArrayList<>();

      private String currentProductionName = null;
      private int currentProductionCounter = 0;

      @Override
      public final ASTNode epilog(final ASTNode node, final Boolean topLevelAlternative) {
        throw new RuntimeException(
            String.format("unhandled node of class '%s'", node.getClass().getSimpleName()));
      }

      @Override
      public final Grammar visit(final Grammar grammar, final Boolean topLevelAlternative) {
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
          final Boolean topLevelAlternative) {
        // we do not have to do anything for lexer productions
        return lexerProduction.clone(false);
      }

      @Override
      public final ParserProduction visit(final ParserProduction parserProduction,
          final Boolean topLevelAlternative) {
        final ParserIdentifier leftHandSideClone = parserProduction.getLeftHandSide().clone(false);
        final List<Annotation> annotationsClone = parserProduction.cloneAnnotations(false);

        this.currentProductionName = parserProduction.getLeftHandSide().getName();
        this.currentProductionCounter = 0;

        final Alternatives rightHandSideClone =
            (Alternatives) parserProduction.getRightHandSide().accept(this, true);

        this.currentProductionName = null;

        return new ParserProduction(SourcePosition.UNKNOWN, annotationsClone, leftHandSideClone,
            rightHandSideClone);
      }

      @Override
      public final Atom visit(final Alternatives alternatives,
          final Boolean topLevelAlternative) {
        final List<Sequence> transformedSequences = new ArrayList<>();
        {
          for (final Sequence sequence : alternatives.getAlternatives()) {
            final Sequence transformedSequence = (Sequence) sequence.accept(this, false);
            transformedSequences.add(transformedSequence);
          }
        }

        final Alternatives transformedAlternatives = new Alternatives(
            SourcePosition.UNKNOWN, null, transformedSequences);

        if (topLevelAlternative) {
          return transformedAlternatives;
        } else {
          final String altProductionName = String.format("h_%s_subalt_%d",
              this.currentProductionName, this.currentProductionCounter++);

          // new production for sub-alternatives
          {
            final ParserIdentifier altProductionIdentifier =
                new ParserIdentifier(SourcePosition.UNKNOWN, altProductionName);

            final ParserProduction altProduction = new ParserProduction(
                SourcePosition.UNKNOWN, new ArrayList<Annotation>(), altProductionIdentifier,
                transformedAlternatives);

            this.helperProductions.add(altProduction);
          }

          // replacement for sub-alternatives
          {
            final ParserIdentifier altProductionIdentifier =
                new ParserIdentifier(SourcePosition.UNKNOWN, altProductionName);
            altProductionIdentifier.setQuantifier(alternatives.getQuantifier());

            return altProductionIdentifier;
          }
        }
      }

      @Override
      public final Sequence visit(final Sequence sequence, final Boolean topLevelAlternative) {
        final List<Atom> transformedElements = new ArrayList<>();
        for (final Atom element : sequence) {
          final Atom transformedElement = (Atom) element.accept(this, false);
          transformedElements.add(transformedElement);
        }

        return new Sequence(SourcePosition.UNKNOWN, transformedElements);
      }

      @Override
      public final LexerIdentifier visit(final LexerIdentifier lexerIdentifier,
          final Boolean topLevelAlternative) {
        return lexerIdentifier.clone(false);
      }

      @Override
      public final ParserIdentifier visit(final ParserIdentifier parserIdentifier,
          final Boolean topLevelAlternative) {
        return parserIdentifier.clone(false);
      }

    }, null);

    // perform name analysis on transformed grammar to annotate all identifiers with symbols
    // TODO should we keep the symbols of the original grammar?
    NameAnalysis.analyze(transformedGrammar);

    return transformedGrammar;
  }

}
