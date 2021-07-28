package i2.act.peg.ast;

import i2.act.peg.info.SourcePosition;

public abstract class Range extends ASTNode {

  public Range(final SourcePosition position) {
    super(position);
  }

  public abstract boolean inRange(final char character);

  @Override
  public abstract Range clone(final boolean keepSymbols);

}
