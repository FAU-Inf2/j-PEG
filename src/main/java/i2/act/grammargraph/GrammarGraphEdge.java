package i2.act.grammargraph;

import i2.act.grammargraph.GrammarGraphNode.Choice;
import i2.act.grammargraph.GrammarGraphNode.Sequence;

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

  public static final class Alternative extends GrammarGraphEdge<Choice, Sequence> {

    private final int weight;

    public Alternative(final Choice source, final Sequence target, final int weight) {
      super(source, target);
      this.weight = weight;
    }

    public final int getWeight() {
      return this.weight;
    }

  }

  public static final class Element extends GrammarGraphEdge<Sequence, Choice> {

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
    private final int weight;

    public Element(final Sequence source, final Choice target, final Quantifier quantifier,
        final int weight) {
      super(source, target);
      this.quantifier = quantifier;
      this.weight = weight;
    }

    public final Quantifier getQuantifier() {
      return this.quantifier;
    }

    public final int getWeight() {
      return this.weight;
    }

  }

}
