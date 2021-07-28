package i2.act.peg.ast;

import i2.act.peg.info.SourcePosition;

public abstract class Argument extends ASTNode {

  public Argument(final SourcePosition position) {
    super(position);
  }

  public abstract String getSerialization();

  @Override
  public abstract Argument clone(final boolean keepSymbols);

}
