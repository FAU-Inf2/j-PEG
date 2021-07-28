package i2.act.grammargraph;

import i2.act.grammargraph.GrammarGraphNode.AlternativeNode;
import i2.act.grammargraph.GrammarGraphNode.SequenceNode;

public abstract class GrammarGraphEdge
    <S extends GrammarGraphNode<?, ?>, T extends GrammarGraphNode<?, ?>> {

  private final S source;
  private final T target;

  public GrammarGraphEdge(final S source, final T target) {
    this.source = source;
    this.target = target;
  }

  public final S getSource() {
    return this.source;
  }

  public final T getTarget() {
    return this.target;
  }

  // ===============================================================================================

  public static final class AlternativeEdge
      extends GrammarGraphEdge<AlternativeNode, SequenceNode> {

    public AlternativeEdge(final AlternativeNode source, final SequenceNode target) {
      super(source, target);
    }

  }

  public static final class SequenceEdge
      extends GrammarGraphEdge<SequenceNode, AlternativeNode> {

    public static enum Quantifier {

      QUANT_NONE(""),
      QUANT_OPTIONAL("?"),
      QUANT_STAR("*"),
      QUANT_PLUS("+");

      public final String stringRepresentation;

      private Quantifier(final String stringRepresentation) {
        this.stringRepresentation = stringRepresentation;
      }

    }

    private final Quantifier quantifier;

    public SequenceEdge(final SequenceNode source, final AlternativeNode target,
        final Quantifier quantifier) {
      super(source, target);
      this.quantifier = quantifier;
    }

    public final Quantifier getQuantifier() {
      return this.quantifier;
    }

  }

}
