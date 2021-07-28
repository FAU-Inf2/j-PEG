package i2.act.peg.symbols;

import i2.act.peg.ast.Production;
import i2.act.peg.builder.GrammarBuilderNode;

public abstract class Symbol<P extends Production> implements GrammarBuilderNode {

  protected final String name;
  protected P production;

  public Symbol(final String name) {
    this(name, null);
  }

  public Symbol(final String name, final P production) {
    this.name = name;
    this.production = production;
  }

  public final String getName() {
    return this.name;
  }

  public final P getProduction() {
    return this.production;
  }

  public final void setProduction(final P production) {
    this.production = production;
  }

  @Override
  public final String toString() {
    return String.format("<%s>", this.name);
  }

}
