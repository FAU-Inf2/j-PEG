package i2.act.peg.transformations;

import i2.act.peg.ast.*;
import i2.act.peg.ast.visitors.BaseASTVisitor;
import i2.act.peg.ast.visitors.NameAnalysis;
import i2.act.peg.ast.visitors.PrettyPrinter;
import i2.act.peg.info.SourcePosition;
import i2.act.peg.parser.PEGParser;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

// TODO left/right recursion?

public final class RemoveQuantifiers implements GrammarTransformation {

  public static final Grammar transform(final Grammar originalGrammar) {
    return (new RemoveQuantifiers()).apply(originalGrammar);
  }

  @Override
  public final Grammar apply(final Grammar originalGrammar) {
    final Grammar transformedGrammar = new Grammar(SourcePosition.UNKNOWN);

    originalGrammar.accept(new BaseASTVisitor<Void, ASTNode>() {

      private final List<Production> helperProductions = new ArrayList<>();

      private final Set<String> optional = new HashSet<>();
      private final Set<String> many = new HashSet<>();
      private final Set<String> manyOne = new HashSet<>();

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
          final Production transformedProduction = (Production) production.accept(this, parameter);
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
        final LexerProduction lexerProductionClone = lexerProduction.clone(false);
        return lexerProductionClone;
      }

      @Override
      public final ParserProduction visit(final ParserProduction parserProduction,
          final Void parameter) {
        final ParserIdentifier leftHandSideClone = parserProduction.getLeftHandSide().clone(false);
        final List<Annotation> annotationsClone = parserProduction.cloneAnnotations(false);

        this.currentProductionName = parserProduction.getLeftHandSide().getName();
        this.currentProductionCounter = 0;

        final Alternatives rightHandSideClone =
            (Alternatives) parserProduction.getRightHandSide().accept(this, parameter);

        this.currentProductionName = null;

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

        if (alternatives.hasQuantifier()) {
          assert (this.currentProductionName != null);

          final String productionName;
          final String productionText;

          switch (alternatives.getQuantifier()) {
            case QUANT_OPTIONAL: {
              productionName = String.format("h_%s_qf_%d",
                  this.currentProductionName, this.currentProductionCounter++);

              productionText = String.format("%s: %s | ;",
                  productionName, PrettyPrinter.prettyPrint(transformedAlternatives));

              break;
            }
            case QUANT_STAR: {
              productionName = String.format("h_%s_qf_%d",
                  this.currentProductionName, this.currentProductionCounter++);

              productionText = String.format("%s: %s %s | ;",
                  productionName, PrettyPrinter.prettyPrint(transformedAlternatives),
                  productionName);

              break;
            }
            case QUANT_PLUS: {
              productionName = String.format("h_%s_qf_%d",
                  this.currentProductionName, this.currentProductionCounter++);

              productionText = String.format("%s: %s %s | %s ;",
                  productionName, PrettyPrinter.prettyPrint(transformedAlternatives),
                  productionName, PrettyPrinter.prettyPrint(transformedAlternatives));

              break;
            }
            default: {
              assert (false) : "unknown quantifier: " + alternatives.getQuantifier();
              return transformedAlternatives;
            }
          }

          final Production production = PEGParser.parseProduction(productionText);
          this.helperProductions.add(production);

          final ParserIdentifier quantifierFreeIdentifier =
              new ParserIdentifier(SourcePosition.UNKNOWN, productionName);

          return quantifierFreeIdentifier;
        } else {
          return transformedAlternatives;
        }
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
      public final Identifier<?> visit(final LexerIdentifier lexerIdentifier,
          final Void parameter) {
        return visit(lexerIdentifier);
      }

      @Override
      public final Identifier<?> visit(final ParserIdentifier parserIdentifier,
          final Void parameter) {
        return visit(parserIdentifier);
      }

      private final Identifier<?> visit(final Identifier<?> originalIdentifier) {
        if (!originalIdentifier.hasQuantifier()) {
          return originalIdentifier.clone(false);
        }

        final String originalName = originalIdentifier.getName();

        final Set<String> lookupSet;
        final String transformedName;
        final String productionText;

        switch (originalIdentifier.getQuantifier()) {
          case QUANT_OPTIONAL: {
            lookupSet = this.optional;

            if (originalIdentifier instanceof LexerIdentifier) {
              transformedName = "h_lex_" + originalName.toLowerCase() + "_opt";
            } else {
              transformedName = "h_" + originalName + "_opt";
            }

            productionText = String.format("%s: %s | ;",
                transformedName, originalName);

            break;
          }
          case QUANT_STAR: {
            lookupSet = this.many;

            if (originalIdentifier instanceof LexerIdentifier) {
              transformedName = "h_lex_" + originalName.toLowerCase() + "_many";
            } else {
              transformedName = "h_" + originalName + "_many";
            }

            productionText = String.format("%s: %s %s | ;",
                transformedName, originalName, transformedName);

            break;
          }
          case QUANT_PLUS: {
            lookupSet = this.manyOne;

            if (originalIdentifier instanceof LexerIdentifier) {
              transformedName = "h_lex_" + originalName.toLowerCase() + "_many_one";
            } else {
              transformedName = "h_" + originalName + "_many_one";
            }

            productionText = String.format("%s: %s %s | %s ;",
                transformedName, originalName, transformedName, originalName);

            break;
          }
          default: {
            assert (false) : "unknown quantifier: " + originalIdentifier.getQuantifier();
            return null;
          }
        }

        if (!lookupSet.contains(transformedName)) {
          final Production production = PEGParser.parseProduction(productionText);
          this.helperProductions.add(production);

          lookupSet.add(transformedName);
        }

        return new ParserIdentifier(SourcePosition.UNKNOWN, transformedName);
      }

    }, null);

    // perform name analysis on transformed grammar to annotate all identifiers with symbols
    // TODO should we keep the symbols of the original grammar?
    NameAnalysis.analyze(transformedGrammar);

    return transformedGrammar;
  }

}
