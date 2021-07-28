package i2.act.peg.lexer;

import i2.act.peg.info.SourcePosition;

public interface Token<K extends TokenKind> {

  public K getKind();

  public SourcePosition getBegin();

  public SourcePosition getEnd();

}
