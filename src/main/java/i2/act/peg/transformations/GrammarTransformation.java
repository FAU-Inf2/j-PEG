package i2.act.peg.transformations;

import i2.act.peg.ast.Grammar;

public interface GrammarTransformation {

  public Grammar apply(final Grammar originalGrammar);

}
