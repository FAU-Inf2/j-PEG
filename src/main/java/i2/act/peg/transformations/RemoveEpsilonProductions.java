package i2.act.peg.transformations;

import i2.act.grammargraph.GrammarGraph;
import i2.act.grammargraph.GrammarGraphEdge;
import i2.act.grammargraph.GrammarGraphEdge.AlternativeEdge;
import i2.act.grammargraph.GrammarGraphEdge.SequenceEdge;
import i2.act.grammargraph.GrammarGraphNode;
import i2.act.grammargraph.GrammarGraphNode.AlternativeNode;
import i2.act.grammargraph.GrammarGraphNode.SequenceNode;
import i2.act.grammargraph.properties.PropertyComputation;
import i2.act.peg.ast.*;
import i2.act.peg.ast.visitors.BaseASTVisitor;
import i2.act.peg.ast.visitors.NameAnalysis;
import i2.act.peg.info.SourcePosition;
import i2.act.peg.parser.PEGParser;
import i2.act.peg.symbols.ParserSymbol;
import i2.act.peg.symbols.Symbol;
import i2.act.util.Pair;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class RemoveEpsilonProductions implements GrammarTransformation {

  public static final Grammar transform(final Grammar originalGrammar) {
    return (new RemoveEpsilonProductions()).apply(originalGrammar);
  }

  @Override
  public final Grammar apply(final Grammar originalGrammar) {
    if (!isInBNF(originalGrammar)) {
      throw new RuntimeException("grammar has to be in BNF");
    }

    final Set<ParserSymbol> nullableNonTerminals = determineNullableNonTerminals(originalGrammar);

    if (false) {
      for (final ParserSymbol nullableNonTerminal : nullableNonTerminals) {
        System.err.println(nullableNonTerminal);
      }
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

        final List<Sequence> originalSequences = alternatives.getAlternatives();

        for (final Sequence originalSequence : originalSequences) {
          if (originalSequence.getNumberOfElements() == 0) {
            // epsilon production -> skip
          } else {
            final List<Sequence> variants =
                generateVariants(originalSequence, nullableNonTerminals);
            transformedSequences.addAll(variants);
          }
        }

        assert (!alternatives.hasQuantifier());
        return new Alternatives(
            SourcePosition.UNKNOWN, Atom.Quantifier.QUANT_NONE, transformedSequences);
      }

    }, null);

    // introduce new start symbol
    {
      final ParserIdentifier originalRootIdentifier =
          originalGrammar.getRootProduction().getLeftHandSide();

      final ParserSymbol originalRootSymbol = originalRootIdentifier.getSymbol();
      assert (originalRootSymbol != null);

      final String originalRootName = originalRootIdentifier.getName();
      final String transformedRootName = "start__";

      final String transformedRootProductionText;
      {
        if (nullableNonTerminals.contains(originalRootSymbol)) {
          transformedRootProductionText = String.format("%s: %s | ;",
              transformedRootName, originalRootName);
        } else {
          transformedRootProductionText = String.format("%s: %s;",
              transformedRootName, originalRootName);
        }
      }

      final ParserProduction transformedRootProduction =
          (ParserProduction) PEGParser.parseProduction(transformedRootProductionText);

      transformedGrammar.addRootProduction(transformedRootProduction);
    }

    // perform name analysis on transformed grammar to annotate all identifiers with symbols
    // TODO should we keep the symbols of the original grammar?
    NameAnalysis.analyze(transformedGrammar);

    return transformedGrammar;
  }

  private final boolean isInBNF(final Grammar grammar) {
    final boolean[] inBNF = { true };

    grammar.accept(new BaseASTVisitor<Void, Void>() {

      private boolean topLevelAlternative;

      @Override
      public final Void visit(final LexerProduction lexerProduction, final Void parameter) {
        return null;
      }

      @Override
      public final Void visit(final ParserProduction parserProduction, final Void parameter) {
        this.topLevelAlternative = true;
        super.visit(parserProduction, parameter);
        this.topLevelAlternative = false;

        return null;
      }

      @Override
      public final Void visit(final Alternatives alternatives, final Void parameter) {
        if (!this.topLevelAlternative) {
          // found sub-alternative => not in BNF
          inBNF[0] = false;
        } else if (alternatives.hasQuantifier()) {
          // found quantifier => not in BNF
          inBNF[0] = false;
        } else {
          this.topLevelAlternative = false;
          super.visit(alternatives, parameter);
          this.topLevelAlternative = true;
        }

        return null;
      }

      @Override
      public final Void visit(final LexerIdentifier lexerIdentifier, final Void parameter) {
        if (lexerIdentifier.hasQuantifier()) {
          // found quantifier => not in BNF
          inBNF[0] = false;
        } else {
          super.visit(lexerIdentifier, parameter);
        }

        return null;
      }

      @Override
      public final Void visit(final ParserIdentifier parserIdentifier, final Void parameter) {
        if (parserIdentifier.hasQuantifier()) {
          // found quantifier => not in BNF
          inBNF[0] = false;
        } else {
          super.visit(parserIdentifier, parameter);
        }

        return null;
      }

    }, null);

    return inBNF[0];
  }

  private final Set<ParserSymbol> determineNullableNonTerminals(final Grammar originalGrammar) {
    final GrammarGraph grammarGraph = GrammarGraph.fromGrammar(originalGrammar);

    final PropertyComputation<Set<GrammarGraphNode<?, ?>>> nullableComputation =
        new PropertyComputation<Set<GrammarGraphNode<?, ?>>>(
            PropertyComputation.Direction.BACKWARDS) {

        @Override
        protected final Set<GrammarGraphNode<?, ?>> init(final AlternativeNode node,
            final GrammarGraph grammarGraph) {
          return new HashSet<>();
        }

        @Override
        protected final Set<GrammarGraphNode<?, ?>> init(final SequenceNode node,
            final GrammarGraph grammarGraph) {
          final Set<GrammarGraphNode<?, ?>> init = new HashSet<>();

          if (node.numberOfSuccessors() == 0) {
            init.add(node);
          }

          return init;
        }

        @Override
        protected final Set<GrammarGraphNode<?, ?>> transfer(final AlternativeNode node,
            final Set<GrammarGraphNode<?, ?>> in) {
          return in;
        }

        @Override
        protected final Set<GrammarGraphNode<?, ?>> transfer(final SequenceNode node,
            final Set<GrammarGraphNode<?, ?>> in) {
          return in;
        }

        @Override
        protected final Set<GrammarGraphNode<?, ?>> confluence(final AlternativeNode node,
            final Iterable<Pair<GrammarGraphEdge<?, ?>, Set<GrammarGraphNode<?, ?>>>> inSets) {
          final Set<GrammarGraphNode<?, ?>> confluence = confluence(inSets);

          for (final AlternativeEdge edge : node.getSuccessorEdges()) {
            final SequenceNode successor = edge.getTarget();

            if (confluence.contains(successor)) {
              confluence.add(node);
              break;
            }
          }

          return confluence;
        }

        @Override
        protected final Set<GrammarGraphNode<?, ?>> confluence(final SequenceNode node,
            final Iterable<Pair<GrammarGraphEdge<?, ?>, Set<GrammarGraphNode<?, ?>>>> inSets) {
          final Set<GrammarGraphNode<?, ?>> confluence = confluence(inSets);

          boolean allSuccessorsNullable = true;
          {
            for (final SequenceEdge edge : node.getSuccessorEdges()) {
              final AlternativeNode successor = edge.getTarget();

              final SequenceEdge.Quantifier quantifier = edge.getQuantifier();

              if (!confluence.contains(successor)
                  && quantifier != SequenceEdge.Quantifier.QUANT_OPTIONAL
                  && quantifier != SequenceEdge.Quantifier.QUANT_STAR) {
                allSuccessorsNullable = false;
                break;
              }
            }
          }

          if (allSuccessorsNullable) {
            confluence.add(node);
          }

          return confluence;
        }

        protected final Set<GrammarGraphNode<?, ?>> confluence(
            final Iterable<Pair<GrammarGraphEdge<?, ?>, Set<GrammarGraphNode<?, ?>>>> inSets) {
          final Set<GrammarGraphNode<?, ?>> confluence = new HashSet<>();

          for (final Pair<GrammarGraphEdge<?, ?>, Set<GrammarGraphNode<?, ?>>> in : inSets) {
            final Set<GrammarGraphNode<?, ?>> inSet = in.getSecond();
            confluence.addAll(inSet);
          }

          return confluence;
        }

    };


    final Set<ParserSymbol> nullableNonTerminals = new HashSet<>();
    {
      final Map<GrammarGraphNode<?, ?>, Set<GrammarGraphNode<?, ?>>> nullableNodesResult =
          nullableComputation.compute(grammarGraph);

      for (final Set<GrammarGraphNode<?, ?>> nullableNodes : nullableNodesResult.values()) {
        for (final GrammarGraphNode<?, ?> nullableNode : nullableNodes) {
          if (nullableNode instanceof AlternativeNode
              && ((AlternativeNode) nullableNode).hasGrammarSymbol()) {
            final Symbol<?> symbol = ((AlternativeNode) nullableNode).getGrammarSymbol();

            if (symbol instanceof ParserSymbol) {
              nullableNonTerminals.add((ParserSymbol) symbol);
            }
          }
        }
      }
    }

    return nullableNonTerminals;
  }

  private final List<Sequence> generateVariants(final Sequence originalSequence,
      final Set<ParserSymbol> nullableNonTerminals) {
    final List<Sequence> sequences = new ArrayList<>();
    {
      final List<List<Atom>> variants =
          generateVariants(0, originalSequence.getElements(), nullableNonTerminals);

      for (final List<Atom> variant : variants) {
        final Sequence sequence = new Sequence(SourcePosition.UNKNOWN, variant);
        sequences.add(sequence);
      }
    }

    return sequences;
  }

  private final List<List<Atom>> generateVariants(final int index, final List<Atom> elements,
      final Set<ParserSymbol> nullableNonTerminals) {
    if (index >= elements.size()) {
      final List<List<Atom>> result = new ArrayList<>();
      result.add(new ArrayList<Atom>());

      return result;
    }

    final List<List<Atom>> recursiveResult =
        generateVariants(index + 1, elements, nullableNonTerminals);

    final Atom element = elements.get(index);
    assert (element instanceof Identifier<?>);

    final Identifier<?> identifier = (Identifier<?>) element;
    final Symbol<?> symbol = identifier.getSymbol();

    if (identifier instanceof LexerIdentifier || !nullableNonTerminals.contains(symbol)) {
      // element has to be kept
      for (final List<Atom> variant : recursiveResult) {
        variant.add(0, element);
      }

      return recursiveResult;
    } else {
      // element is nullable -> generate two variants
      // NOTE: the order of the variants is important due to the semantics of PEGs!
      final List<List<Atom>> result = new ArrayList<>();

      for (final List<Atom> variant : recursiveResult) {
        final List<Atom> newVariant = new ArrayList<>(variant);
        newVariant.add(0, element);

        result.add(newVariant);
      }

      result.addAll(recursiveResult);

      return result;
    }
  }

}
