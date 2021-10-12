package i2.act.peg.transformations;

import i2.act.grammargraph.GrammarGraph;
import i2.act.grammargraph.GrammarGraphEdge;
import i2.act.grammargraph.GrammarGraphNode;
import i2.act.grammargraph.GrammarGraphNode.Choice;
import i2.act.peg.ast.*;
import i2.act.peg.ast.visitors.BaseASTVisitor;
import i2.act.peg.ast.visitors.NameAnalysis;
import i2.act.peg.info.SourcePosition;
import i2.act.peg.symbols.ParserSymbol;

import java.util.HashSet;
import java.util.Set;

public final class RemoveDeadProductions implements GrammarTransformation {

  public static final Grammar transform(final Grammar originalGrammar) {
    return (new RemoveDeadProductions()).apply(originalGrammar);
  }

  @Override
  public final Grammar apply(final Grammar originalGrammar) {
    final Set<ParserSymbol> reachableProductions = reachableProductions(originalGrammar);

    final Grammar transformedGrammar = new Grammar(SourcePosition.UNKNOWN);

    originalGrammar.accept(new BaseASTVisitor<Void, Void>() {

      @Override
      public final Void visit(final LexerProduction lexerProduction,
          final Void parameter) {
        final LexerProduction lexerProductionClone = lexerProduction.clone(false);
        transformedGrammar.addProduction(lexerProductionClone);

        return null;
      }

      @Override
      public final Void visit(final ParserProduction parserProduction,
          final Void parameter) {
        if (reachableProductions.contains(parserProduction.getSymbol())) {
          final ParserProduction parserProductionClone = parserProduction.clone(false);
          transformedGrammar.addProduction(parserProductionClone);
        }

        return null;
      }

    }, null);

    // perform name analysis on transformed grammar to annotate all identifiers with symbols
    // TODO should we keep the symbols of the original grammar?
    NameAnalysis.analyze(transformedGrammar);

    return transformedGrammar;
  }

  private final Set<ParserSymbol> reachableProductions(final Grammar originalGrammar) {
    final Set<ParserSymbol> reachableProductions = new HashSet<>();

    final GrammarGraph grammarGraph = GrammarGraph.fromGrammar(originalGrammar);
    final Set<GrammarGraphNode> visitedNodes = new HashSet<>();

    reachableProductions(grammarGraph.getRootNode(), visitedNodes, reachableProductions);

    return reachableProductions;
  }

  private final void reachableProductions(final GrammarGraphNode<?, ?> node,
      final Set<GrammarGraphNode> visitedNodes, final Set<ParserSymbol> reachableProductions) {
    if (node instanceof Choice) {
      if (((Choice) node).getGrammarSymbol() instanceof ParserSymbol) {
        reachableProductions.add((ParserSymbol) ((Choice) node).getGrammarSymbol());
      }
    }

    visitedNodes.add(node);

    for (final GrammarGraphEdge<?, ?> successorEdge : node.getSuccessorEdges()) {
      final GrammarGraphNode<?, ?> targetNode = successorEdge.getTarget();

      if (!visitedNodes.contains(targetNode)) {
        reachableProductions(targetNode, visitedNodes, reachableProductions);
      }
    }
  }

}
