package i2.act.peg.ast;

import i2.act.peg.info.SourcePosition;

public abstract class Expression extends ASTNode {

  public Expression(final SourcePosition position) {
    super(position);
  }

}
