package i2.act.grammargraph.properties;

import i2.act.grammargraph.GrammarGraph;
import i2.act.grammargraph.GrammarGraphEdge;
import i2.act.grammargraph.GrammarGraphNode;
import i2.act.grammargraph.GrammarGraphNode.AlternativeNode;
import i2.act.grammargraph.GrammarGraphNode.SequenceNode;
import i2.act.peg.symbols.Symbol;
import i2.act.util.Pair;

import java.util.*;
import java.util.stream.Collectors;

public abstract class PropertyComputation<P> {

  public static enum Direction {
    FORWARDS,
    BACKWARDS;
  }

  private final Direction direction;

  public PropertyComputation(final Direction direction) {
    this.direction = direction;
  }

  protected abstract P init(final AlternativeNode node, final GrammarGraph grammarGraph);

  protected abstract P init(final SequenceNode node, final GrammarGraph grammarGraph);

  protected P init(final GrammarGraphNode<?, ?> node, final GrammarGraph grammarGraph) {
    if (node instanceof AlternativeNode) {
      return init((AlternativeNode) node, grammarGraph);
    } else {
      assert (node instanceof SequenceNode);
      return init((SequenceNode) node, grammarGraph);
    }
  }

  protected abstract P transfer(final AlternativeNode node, final P in);

  protected abstract P transfer(final SequenceNode node, final P in);

  protected P transfer(final GrammarGraphNode<?, ?> node, final P in) {
    if (node instanceof AlternativeNode) {
      return transfer((AlternativeNode) node, in);
    } else {
      assert (node instanceof SequenceNode);
      return transfer((SequenceNode) node, in);
    }
  }

  protected abstract P confluence(final AlternativeNode node,
      final Iterable<Pair<GrammarGraphEdge<?, ?>, P>> inSets);

  protected abstract P confluence(final SequenceNode node,
      final Iterable<Pair<GrammarGraphEdge<?, ?>, P>> inSets);

  protected P confluence(final GrammarGraphNode<?, ?> node,
      final Iterable<Pair<GrammarGraphEdge<?, ?>, P>> inSets) {
    if (node instanceof AlternativeNode) {
      return confluence((AlternativeNode) node, inSets);
    } else {
      assert (node instanceof SequenceNode);
      return confluence((SequenceNode) node, inSets);
    }
  }

  private final Iterable<? extends GrammarGraphEdge<?, ?>> getInEdges(
      final GrammarGraphNode<?, ?> node) {
    if (this.direction == Direction.FORWARDS) {
      return node.getPredecessorEdges();
    } else {
      assert (this.direction == Direction.BACKWARDS);
      return node.getSuccessorEdges();
    }
  }

  private final GrammarGraphNode<?, ?> getInNode(final GrammarGraphEdge<?, ?> edge) {
    if (this.direction == Direction.FORWARDS) {
      return edge.getSource();
    } else {
      assert (this.direction == Direction.BACKWARDS);
      return edge.getTarget();
    }
  }

  private final Iterable<Pair<GrammarGraphEdge<?, ?>, P>> getInSets(
      final GrammarGraphNode<?, ?> node, final Map<GrammarGraphNode<?, ?>, P> properties) {
    final List<Pair<GrammarGraphEdge<?, ?>, P>> inSets = new ArrayList<>();

    final Iterable<? extends GrammarGraphEdge<?, ?>> inEdges = getInEdges(node);
    for (final GrammarGraphEdge<?, ?> inEdge : inEdges) {
      final GrammarGraphNode<?, ?> inNode = getInNode(inEdge);

      assert (properties.containsKey(inNode)) : "missing properties";
      inSets.add(new Pair<>(inEdge, properties.get(inNode)));
    }

    return inSets;
  }

  private final Iterable<? extends GrammarGraphEdge<?, ?>> getOutEdges(
      final GrammarGraphNode<?, ?> node) {
    if (this.direction == Direction.FORWARDS) {
      return node.getSuccessorEdges();
    } else {
      assert (this.direction == Direction.BACKWARDS);
      return node.getPredecessorEdges();
    }
  }

  private final GrammarGraphNode<?, ?> getOutNode(final GrammarGraphEdge<?, ?> edge) {
    if (this.direction == Direction.FORWARDS) {
      return edge.getTarget();
    } else {
      assert (this.direction == Direction.BACKWARDS);
      return edge.getSource();
    }
  }

  protected P handle(final GrammarGraphNode<?, ?> node,
      final Map<GrammarGraphNode<?, ?>, P> properties) {
    final Iterable<Pair<GrammarGraphEdge<?, ?>, P>> inSets = getInSets(node, properties);

    final P in = confluence(node, inSets);
    final P out = transfer(node, in);

    return out;
  }

  protected final Map<Symbol<?>, P> filter(final Map<GrammarGraphNode<?, ?>, P> result) {
    return result.entrySet().stream()
        .filter(e -> (e.getKey() instanceof AlternativeNode)
            && ((AlternativeNode) e.getKey()).hasGrammarSymbol())
        .collect(Collectors.toMap(
            e -> ((AlternativeNode) e.getKey()).getGrammarSymbol(), Map.Entry::getValue));
  }

  public final Map<GrammarGraphNode<?, ?>, P> compute(final GrammarGraph grammarGraph) {
    final Map<GrammarGraphNode<?, ?>, P> properties = new HashMap<>();

    // initialize with empty sets
    for (final GrammarGraphNode<?, ?> node : grammarGraph) {
      final P init = init(node, grammarGraph);
      properties.put(node, init);
    }

    // fix point computation
    final Queue<GrammarGraphNode<?, ?>> worklist = new LinkedList<>();

    for (final GrammarGraphNode<?, ?> node : grammarGraph) {
      // if a node does not have any "in nodes", its value cannot change after initialization
      // => only add nodes to worklist that have at least one "in node"
      // (this should handle unreachable nodes correctly)
      if (getInEdges(node).iterator().hasNext()) {
        worklist.add(node);
      }
    }

    while (!worklist.isEmpty()) {
      final GrammarGraphNode<?, ?> node = worklist.remove();

      final P out = handle(node, Collections.unmodifiableMap(properties));

      if (out == null) {
        continue;
      }

      if (!out.equals(properties.get(node))) {
        properties.put(node, out);

        for (final GrammarGraphEdge<?, ?> outEdge : getOutEdges(node)) {
          final GrammarGraphNode<?, ?> outNode = getOutNode(outEdge);

          if (!worklist.contains(outNode)) {
            worklist.add(outNode);
          }
        }
      }
    }

    return properties;
  }

}
