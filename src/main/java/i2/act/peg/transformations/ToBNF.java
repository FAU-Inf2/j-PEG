package i2.act.peg.transformations;

import i2.act.peg.ast.Grammar;

public final class ToBNF implements GrammarTransformation {

  public static final Grammar transform(final Grammar originalGrammar) {
    return (new ToBNF()).apply(originalGrammar);
  }

  @Override
  public final Grammar apply(final Grammar originalGrammar) {
    final Grammar transformedGrammar =
        RemoveDuplicateProductions.transform(
        RemoveSubAlternatives.transform(
        HoistSubAlternatives.transform(
        RemoveQuantifiers.transform(originalGrammar))));

    return transformedGrammar;
  }

}
