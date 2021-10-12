package i2.act.grammargraph;

import i2.act.grammargraph.GrammarGraphEdge.Alternative;
import i2.act.grammargraph.GrammarGraphEdge.Element;
import i2.act.peg.symbols.Symbol;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class GrammarGraphNode
    <S extends GrammarGraphEdge<?, ?>, P extends GrammarGraphEdge<?, ?>> {

  private static int idCounter;

  public final int id;

  protected final List<P> predecessorEdges;
  protected final List<S> successorEdges;

  public GrammarGraphNode() {
    this.id = idCounter++;

    this.predecessorEdges = new ArrayList<P>();
    this.successorEdges = new ArrayList<S>();
  }

  public final List<P> getPredecessorEdges() {
    return Collections.unmodifiableList(this.predecessorEdges);
  }

  public final List<S> getSuccessorEdges() {
    return Collections.unmodifiableList(this.successorEdges);
  }

  public final int numberOfPredecessors() {
    return this.predecessorEdges.size();
  }

  public final int numberOfSuccessors() {
    return this.successorEdges.size();
  }

  public final boolean isRoot() {
    return this.predecessorEdges.isEmpty();
  }

  public final boolean isLeaf() {
    return this.successorEdges.isEmpty();
  }

  // ===============================================================================================

  public static final class Choice extends GrammarGraphNode<Alternative, Element> {

    private final Symbol<?> grammarSymbol;

    public Choice() {
      this(null);
    }

    public Choice(final Symbol<?> grammarSymbol) {
      this.grammarSymbol = grammarSymbol;
    }

    public final boolean hasGrammarSymbol() {
      return this.grammarSymbol != null;
    }

    public final Symbol<?> getGrammarSymbol() {
      return this.grammarSymbol;
    }

  }

  public static final class Sequence extends GrammarGraphNode<Element, Alternative> {

  }

}
