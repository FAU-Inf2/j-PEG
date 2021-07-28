package i2.act.peg.transformations;

import i2.act.peg.ast.Grammar;

public enum GrammarTransformationFactory {

  HOIST_SUB_ALT(
      HoistSubAlternatives.class.getSimpleName(), () -> new HoistSubAlternatives()),
  REMOVE_CHAINS(
      RemoveChainProductions.class.getSimpleName(), () -> new RemoveChainProductions()),
  REMOVE_DEAD(
      RemoveDeadProductions.class.getSimpleName(), () -> new RemoveDeadProductions()),
  REMOVE_DUPS(
      RemoveDuplicateProductions.class.getSimpleName(), () -> new RemoveDuplicateProductions()),
  REMOVE_EPS(
      RemoveEpsilonProductions.class.getSimpleName(), () -> new RemoveEpsilonProductions()),
  REMOVE_QUANTIFIERS(
      RemoveQuantifiers.class.getSimpleName(), () -> new RemoveQuantifiers()),
  REMOVE_SUB_ALTS(
      RemoveSubAlternatives.class.getSimpleName(), () -> new RemoveSubAlternatives()),
  REMOVE_UNITS(
      RemoveUnitProductions.class.getSimpleName(), () -> new RemoveUnitProductions()),
  TO_BNF(
      ToBNF.class.getSimpleName(), () -> new ToBNF());


  private static interface Creator {

    public GrammarTransformation create();

  }


  private final String transformationName;
  private final Creator creator;

  private GrammarTransformationFactory(final String transformationName, final Creator creator) {
    this.transformationName = transformationName;
    this.creator = creator;
  }

  public final String getTransformationName() {
    return this.transformationName;
  }

  public final GrammarTransformation createTransformation() {
    return this.creator.create();
  }

  public final Grammar apply(final Grammar grammar) {
    return createTransformation().apply(grammar);
  }

  public static final GrammarTransformationFactory fromName(final String transformationName) {
    for (final GrammarTransformationFactory factory : values()) {
      if (factory.transformationName.equals(transformationName)) {
        return factory;
      }
    }

    return null;
  }

}
