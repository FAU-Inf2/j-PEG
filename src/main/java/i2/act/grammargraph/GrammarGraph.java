package i2.act.grammargraph;

import i2.act.grammargraph.GrammarGraphEdge.AlternativeEdge;
import i2.act.grammargraph.GrammarGraphEdge.SequenceEdge;
import i2.act.grammargraph.GrammarGraphNode.AlternativeNode;
import i2.act.grammargraph.GrammarGraphNode.SequenceNode;
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

  private final AlternativeNode rootNode;

  private final Map<Symbol<?>, AlternativeNode> grammarNodes;
  private final List<GrammarGraphNode<?, ?>> allNodes;

  private GrammarGraph(final Grammar grammar, final Symbol<?> rootSymbol,
      final AlternativeNode rootNode) {
    this.grammar = grammar;
    this.rootNode = rootNode;

    this.grammarNodes = new HashMap<Symbol<?>, AlternativeNode>();
    this.allNodes = new ArrayList<GrammarGraphNode<?, ?>>();

    this.grammarNodes.put(rootSymbol, rootNode);
    this.allNodes.add(rootNode);
  }

  public final Grammar getGrammar() {
    return this.grammar;
  }

  public final AlternativeNode getRootNode() {
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
      if (node instanceof AlternativeNode) {
        final AlternativeNode alternativeNode = (AlternativeNode) node;

        if (alternativeNode.hasGrammarSymbol()) {
          if (alternativeNode.getGrammarSymbol() instanceof LexerSymbol) {
            style = DOT_STYLE_ALTERNATIVE_NODES_LEXER;
          } else {
            assert (alternativeNode.getGrammarSymbol() instanceof ParserSymbol);
            style = DOT_STYLE_ALTERNATIVE_NODES_PARSER;
          }
        } else {
          style = DOT_STYLE_ALTERNATIVE_NODES_ANON;
        }
      } else {
        assert (node instanceof SequenceNode);
        style = DOT_STYLE_SEQUENCE_NODES;
      }
    }

    writer.write("    %s [label=\"%s\"%s];\n", nodeName, label, style);
  }

  private final void writeEdge(final GrammarGraphEdge<?, ?> edge, final SafeWriter writer) {
    final String nodeNameFrom = getNodeName(edge.getSource());
    final String nodeNameTo = getNodeName(edge.getTarget());

    final boolean dashed = (edge instanceof AlternativeEdge);
    final String label = (edge instanceof SequenceEdge)
        ? (((SequenceEdge) edge).getQuantifier().stringRepresentation)
        : "";

    writer.write("    %s -> %s [label=\"%s\",%s];\n", nodeNameFrom, nodeNameTo, label,
        (dashed ? "style=dashed" : ""));
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
          if ((node instanceof AlternativeNode) && ((AlternativeNode) node).hasGrammarSymbol()) {
            label = ((AlternativeNode) node).getGrammarSymbol().getName();
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

    for (final AlternativeNode grammarNode : this.grammarNodes.values()) {
      assert (grammarNode.hasGrammarSymbol());
      if (grammarNode.getGrammarSymbol() instanceof LexerSymbol) {
        symbols.add((LexerSymbol) grammarNode.getGrammarSymbol());
      }
    }

    return symbols;
  }

  public final List<ParserSymbol> gatherParserSymbols() {
    final List<ParserSymbol> symbols = new ArrayList<>();

    for (final AlternativeNode grammarNode : this.grammarNodes.values()) {
      assert (grammarNode.hasGrammarSymbol());
      if (grammarNode.getGrammarSymbol() instanceof ParserSymbol) {
        symbols.add((ParserSymbol) grammarNode.getGrammarSymbol());
      }
    }

    return symbols;
  }

  public final Symbol<?> getSymbol(final String name) {
    for (final AlternativeNode grammarNode : this.grammarNodes.values()) {
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
    final AlternativeNode rootNode = new AlternativeNode(rootSymbol);

    final GrammarGraph grammarGraph = new GrammarGraph(grammar, rootSymbol, rootNode);

    // (1) add nodes for all productions
    {
      for (final Production<?, ?> production : grammar.getProductions()) {
        if (production != grammar.getRootProduction()) {
          final Symbol<?> productionSymbol = production.getSymbol();
          final AlternativeNode alternativeNode = new AlternativeNode(productionSymbol);

          grammarGraph.grammarNodes.put(productionSymbol, alternativeNode);
          grammarGraph.allNodes.add(alternativeNode);
        }
      }

      // add node for implicitly defined EOF
      final Symbol<?> eofSymbol = LexerSymbol.EOF;
      final AlternativeNode eofNode = new AlternativeNode(eofSymbol);

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

        final AlternativeNode productionNode =
            grammarGraph.grammarNodes.get(parserProduction.getSymbol());
        assert (productionNode != null);

        final Alternatives alternatives = parserProduction.getRightHandSide();

        for (final Sequence sequence : alternatives.getAlternatives()) {
          final GrammarGraphNode<?, ?> _sequenceNode = sequence.accept(
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
                final AlternativeNode alternativeNode =
                    new AlternativeNode(alternatives.getImplicitQuantifierSymbol());
                grammarGraph.allNodes.add(alternativeNode);

                for (final Sequence alternative : alternatives.getAlternatives()) {
                  final GrammarGraphNode<?, ?> successorNode =
                      alternative.accept(this, parameter);

                  assert (successorNode instanceof SequenceNode);
                  final SequenceNode sequenceNode = (SequenceNode) successorNode;

                  final AlternativeEdge edge = new AlternativeEdge(alternativeNode, sequenceNode);
                  alternativeNode.successorEdges.add(edge);
                  sequenceNode.predecessorEdges.add(edge);
                }

                return alternativeNode;
              }

              @Override
              public GrammarGraphNode<?, ?> visit(final Sequence sequence,
                  final Void parameter) {
                final SequenceNode sequenceNode = new SequenceNode();
                grammarGraph.allNodes.add(sequenceNode);

                for (final Atom element : sequence.getElements()) {
                  final GrammarGraphNode<?, ?> successorNode = element.accept(this, parameter);

                  assert (successorNode instanceof AlternativeNode);
                  final AlternativeNode alternativeNode = (AlternativeNode) successorNode;

                  final SequenceEdge.Quantifier quantifier = getQuantifier(element.getQuantifier());
                  final SequenceEdge edge =
                      new SequenceEdge(sequenceNode, alternativeNode, quantifier);

                  sequenceNode.successorEdges.add(edge);
                  alternativeNode.predecessorEdges.add(edge);
                }

                return sequenceNode;
              }

              private final SequenceEdge.Quantifier getQuantifier(
                  final Atom.Quantifier atomQuantifier) {
                switch (atomQuantifier) {
                  case QUANT_OPTIONAL: {
                    return SequenceEdge.Quantifier.QUANT_OPTIONAL;
                  }
                  case QUANT_STAR: {
                    return SequenceEdge.Quantifier.QUANT_STAR;
                  }
                  case QUANT_PLUS: {
                    return SequenceEdge.Quantifier.QUANT_PLUS;
                  }
                  default: {
                    assert (atomQuantifier == Atom.Quantifier.QUANT_NONE);
                    return SequenceEdge.Quantifier.QUANT_NONE;
                  }
                }
              }

            }, null);

          assert (_sequenceNode instanceof SequenceNode);
          final SequenceNode sequenceNode = (SequenceNode) _sequenceNode;

          final AlternativeEdge edge = new AlternativeEdge(productionNode, sequenceNode);
          productionNode.successorEdges.add(edge);
          sequenceNode.predecessorEdges.add(edge);
        }
      }
    }

    return grammarGraph;
  }

}
