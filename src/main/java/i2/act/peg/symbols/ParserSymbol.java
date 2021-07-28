package i2.act.peg.symbols;

import i2.act.peg.ast.ParserProduction;

public final class ParserSymbol extends Symbol<ParserProduction> {

  public static final ParserSymbol OPTIONAL = new ParserSymbol("?", null);
  public static final ParserSymbol STAR = new ParserSymbol("*", null);
  public static final ParserSymbol PLUS = new ParserSymbol("+", null);
  public static final ParserSymbol LIST_ITEM = new ParserSymbol("ITEM", null);

  //------------------------------------------------------------------------------------------------

  public ParserSymbol(final String name) {
    this(name, null);
  }

  public ParserSymbol(final String name, final ParserProduction production) {
    super(name, production);
  }

}
