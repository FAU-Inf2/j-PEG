package i2.act.grammargraph;

import i2.act.grammargraph.GrammarGraphEdge.Alternative;
import i2.act.grammargraph.GrammarGraphEdge.Element;
import i2.act.grammargraph.GrammarGraphNode.Choice;
import i2.act.grammargraph.GrammarGraphNode.Sequence;
import i2.act.peg.ast.*;
import i2.act.peg.ast.visitors.BaseASTVisitor;
import i2.act.peg.symbols.LexerSymbol;
import i2.act.peg.symbols.ParserSymbol;
import i2.act.peg.symbols.Symbol;
import i2.act.util.SafeWriter;

import java.util.*;

public final class GrammarGraph implements Iterable<GrammarGraphNode<?, ?>> {

  public static final String DOT_STYLE_ALTERNATIVE_NODES_ANON =
      "shape=box, style=filled, fillcolor=gainsboro";

  public static final String DOT_STYLE_ALTERNATIVE_NODES_PARSER =
      "shape=box, style=filled, fillcolor=lightsalmon";

  public static final String DOT_STYLE_ALTERNATIVE_NODES_LEXER =
      "shape=box, style=filled, fillcolor=salmon3";

  public static final String DOT_STYLE_SEQUENCE_NODES =
      "shape=circle, style=filled, fillcolor=lightgoldenrod1, fixedsize=true, width=.25";

  private final Grammar grammar;

  private final Choice rootNode;

  private final Map<Symbol<?>, Choice> grammarNodes;
  private final List<GrammarGraphNode<?, ?>> allNodes;

  private GrammarGraph(final Grammar grammar, final Symbol<?> rootSymbol,
      final Choice rootNode) {
    this.grammar = grammar;
    this.rootNode = rootNode;

    this.grammarNodes = new HashMap<Symbol<?>, Choice>();
    this.allNodes = new ArrayList<GrammarGraphNode<?, ?>>();

    this.grammarNodes.put(rootSymbol, rootNode);
    this.allNodes.add(rootNode);
  }

  public final Grammar getGrammar() {
    return this.grammar;
  }

  public final Choice getRootNode() {
    return this.rootNode;
  }

  private final int getNodeId(final GrammarGraphNode<?, ?> node) {
    return node.id;
  }

  private final String getNodeName(final GrammarGraphNode<?, ?> node) {
    return String.format("node_%d", getNodeId(node));
  }

  private final void writeNode(final GrammarGraphNode<?, ?> node, final String label,
      final SafeWriter writer) {
    final String nodeName = getNodeName(node);
    final String style;
    {
      if (node instanceof Choice) {
        final Choice choice = (Choice) node;

        if (choice.hasGrammarSymbol()) {
          if (choice.getGrammarSymbol() instanceof LexerSymbol) {
            style = DOT_STYLE_ALTERNATIVE_NODES_LEXER;
          } else {
            assert (choice.getGrammarSymbol() instanceof ParserSymbol);
            style = DOT_STYLE_ALTERNATIVE_NODES_PARSER;
          }
        } else {
          style = DOT_STYLE_ALTERNATIVE_NODES_ANON;
        }
      } else {
        assert (node instanceof Sequence);
        style = DOT_STYLE_SEQUENCE_NODES;
      }
    }

    writer.write("    %s [label=\"%s\"%s];\n", nodeName, label, style);
  }

  private final void writeEdge(final GrammarGraphEdge<?, ?> edge, final SafeWriter writer) {
    final String nodeNameFrom = getNodeName(edge.getSource());
    final String nodeNameTo = getNodeName(edge.getTarget());

    final boolean dashed = (edge instanceof Alternative);
    final String label = getEdgeLabel(edge);

    writer.write("    %s -> %s [label=\"%s\",%s];\n", nodeNameFrom, nodeNameTo, label,
        (dashed ? "style=dashed" : ""));
  }

  private final String getEdgeLabel(final GrammarGraphEdge<?, ?> edge) {
    if (edge instanceof Element) {
      final Element element = (Element) edge;

      if (element.getQuantifier() == Element.Quantifier.QUANT_NONE) {
        return "";
      } else {
        final int weight = element.getWeight();

        if (weight == 1) {
          return element.getQuantifier().stringRepresentation;
        } else {
          return String.format("%d%s",
              element.getWeight(), element.getQuantifier().stringRepresentation);
        }
      }
    } else {
      assert (edge instanceof Alternative);
      final Alternative alternative = (Alternative) edge;

      final int weight = alternative.getWeight();

      if (weight == 1) {
        return "";
      } else {
        return String.valueOf(weight);
      }
    }
  }

  public final void printAsDot() {
    printAsDot(SafeWriter.openStdOut());
  }

  @SuppressWarnings("unchecked")
  public final void printAsDot(final SafeWriter writer) {
    writer.write("digraph {\n");
    writer.write("  graph [ordering=\"out\"];\n");
    writer.write("  node [fontname=\"Droid Sans Mono\"];\n");
    writer.write("  edge [fontname=\"Droid Sans Mono\"];\n");

    // nodes
    {
      for (final GrammarGraphNode<?, ?> node : this.allNodes) {
        final String label;
        {
          if ((node instanceof Choice) && ((Choice) node).hasGrammarSymbol()) {
            label = ((Choice) node).getGrammarSymbol().getName();
          } else {
            label = "";
          }
        }

        writeNode(node, label, writer);
      }
    }

    // edges
    {
      for (final GrammarGraphNode<?, ?> node : this.allNodes) {
        for (final GrammarGraphEdge<?, ?> successorEdge : node.getSuccessorEdges()) {
          writeEdge(successorEdge, writer);
        }
      }
    }

    writer.write("}\n");
    writer.flush();
  }

  @Override
  public final Iterator<GrammarGraphNode<?, ?>> iterator() {
    return Collections.unmodifiableList(this.allNodes).iterator();
  }

  public final List<LexerSymbol> gatherLexerSymbols() {
    final List<LexerSymbol> symbols = new ArrayList<>();

    for (final Choice grammarNode : this.grammarNodes.values()) {
      assert (grammarNode.hasGrammarSymbol());
      if (grammarNode.getGrammarSymbol() instanceof LexerSymbol) {
        symbols.add((LexerSymbol) grammarNode.getGrammarSymbol());
      }
    }

    return symbols;
  }

  public final List<ParserSymbol> gatherParserSymbols() {
    final List<ParserSymbol> symbols = new ArrayList<>();

    for (final Choice grammarNode : this.grammarNodes.values()) {
      assert (grammarNode.hasGrammarSymbol());
      if (grammarNode.getGrammarSymbol() instanceof ParserSymbol) {
        symbols.add((ParserSymbol) grammarNode.getGrammarSymbol());
      }
    }

    return symbols;
  }

  public final Symbol<?> getSymbol(final String name) {
    for (final Choice grammarNode : this.grammarNodes.values()) {
      assert (grammarNode.hasGrammarSymbol());
      final Symbol<?> symbol = grammarNode.getGrammarSymbol();

      if (symbol.getName().equals(name)) {
        return symbol;
      }
    }

    return null;
  }

  // ===============================================================================================

  public static final GrammarGraph fromGrammar(final Grammar grammar) {
    final Symbol<?> rootSymbol = grammar.getRootProduction().getSymbol();
    final Choice rootNode = new Choice(rootSymbol);

    final GrammarGraph grammarGraph = new GrammarGraph(grammar, rootSymbol, rootNode);

    // (1) add nodes for all productions
    {
      for (final Production<?, ?> production : grammar.getProductions()) {
        if (production != grammar.getRootProduction()) {
          final Symbol<?> productionSymbol = production.getSymbol();
          final Choice choice = new Choice(productionSymbol);

          grammarGraph.grammarNodes.put(productionSymbol, choice);
          grammarGraph.allNodes.add(choice);
        }
      }

      // add node for implicitly defined EOF
      final Symbol<?> eofSymbol = LexerSymbol.EOF;
      final Choice eofNode = new Choice(eofSymbol);

      grammarGraph.grammarNodes.put(eofSymbol, eofNode);
      grammarGraph.allNodes.add(eofNode);
    }

    // (2) add edges
    {
      for (final Production<?, ?> production : grammar.getProductions()) {
        if (!(production instanceof ParserProduction)) {
          continue;
        }

        final ParserProduction parserProduction = (ParserProduction) production;

        final Choice productionNode =
            grammarGraph.grammarNodes.get(parserProduction.getSymbol());
        assert (productionNode != null);

        final Alternatives alternatives = parserProduction.getRightHandSide();

        for (final i2.act.peg.ast.Sequence _sequence : alternatives.getAlternatives()) {
          final GrammarGraphNode<?, ?> node = _sequence.accept(
              new BaseASTVisitor<Void, GrammarGraphNode<?, ?>>() {

              @Override
              public GrammarGraphNode<?, ?> visit(final ParserIdentifier identifier,
                  final Void parameter) {
                final Symbol<?> symbol = identifier.getSymbol();

                assert (grammarGraph.grammarNodes.containsKey(symbol));
                return grammarGraph.grammarNodes.get(symbol);
              }

              @Override
              public GrammarGraphNode<?, ?> visit(final LexerIdentifier identifier,
                  final Void parameter) {
                final Symbol<?> symbol = identifier.getSymbol();

                assert (grammarGraph.grammarNodes.containsKey(symbol));
                return grammarGraph.grammarNodes.get(symbol);
              }

              @Override
              public GrammarGraphNode<?, ?> visit(final Alternatives alternatives,
                  final Void parameter) {
                final Atom skippableTo = skippableTo(alternatives);

                if (skippableTo != null) {
                  return skippableTo.accept(this, parameter);
                }

                final Choice choice =
                    new Choice(alternatives.getImplicitQuantifierSymbol());
                grammarGraph.allNodes.add(choice);

                for (final i2.act.peg.ast.Sequence alternative : alternatives.getAlternatives()) {
                  final GrammarGraphNode<?, ?> successorNode =
                      alternative.accept(this, parameter);

                  assert (successorNode instanceof Sequence);
                  final Sequence sequence = (Sequence) successorNode;

                  final int weight = alternative.getWeight();

                  final Alternative edge = new Alternative(choice, sequence, weight);
                  choice.successorEdges.add(edge);
                  sequence.predecessorEdges.add(edge);
                }

                return choice;
              }

              @Override
              public GrammarGraphNode<?, ?> visit(final i2.act.peg.ast.Sequence _sequence,
                  final Void parameter) {
                final Sequence sequence = new Sequence();
                grammarGraph.allNodes.add(sequence);

                for (final Atom element : _sequence.getElements()) {
                  final GrammarGraphNode<?, ?> successorNode = element.accept(this, parameter);

                  assert (successorNode instanceof Choice);
                  final Choice choice = (Choice) successorNode;

                  final Element.Quantifier quantifier = getQuantifier(element.getQuantifier());
                  final int weight = getWeight(element.getQuantifier());

                  final Element edge =
                      new Element(sequence, choice, quantifier, weight);

                  sequence.successorEdges.add(edge);
                  choice.predecessorEdges.add(edge);
                }

                return sequence;
              }

              private final Atom skippableTo(final Alternatives alternatives) {
                if (alternatives.getNumberOfAlternatives() != 1) {
                  return null;
                }

                final i2.act.peg.ast.Sequence onlyAlternative = alternatives.getAlternative(0);

                if (onlyAlternative.getNumberOfElements() != 1) {
                  return null;
                }

                final Atom onlyElement = onlyAlternative.getElement(0);

                if (onlyElement.hasQuantifier()) {
                  return null;
                }

                return onlyElement;
              }

              private final Element.Quantifier getQuantifier(final Quantifier quantifier) {
                if (quantifier == null) {
                    return Element.Quantifier.QUANT_NONE;
                }

                final Quantifier.Kind quantifierKind = quantifier.getKind();

                switch (quantifierKind) {
                  case QUANT_OPTIONAL: {
                    return Element.Quantifier.QUANT_OPTIONAL;
                  }
                  case QUANT_STAR: {
                    return Element.Quantifier.QUANT_STAR;
                  }
                  default: {
                    assert (quantifierKind == Quantifier.Kind.QUANT_PLUS);
                    return Element.Quantifier.QUANT_PLUS;
                  }
                }
              }

              private final int getWeight(final Quantifier quantifier) {
                if (quantifier == null) {
                  return 1;
                } else {
                  return quantifier.getWeight();
                }
              }

            }, null);

          assert (node instanceof Sequence);
          final Sequence sequence = (Sequence) node;

          final int weight = _sequence.getWeight();

          final Alternative edge = new Alternative(productionNode, sequence, weight);
          productionNode.successorEdges.add(edge);
          sequence.predecessorEdges.add(edge);
        }
      }
    }

    return grammarGraph;
  }

}
