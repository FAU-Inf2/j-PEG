package i2.act.peg.ast;

import i2.act.peg.ast.visitors.ASTVisitor;
import i2.act.peg.info.SourcePosition;
import i2.act.peg.symbols.LexerSymbol;
import i2.act.peg.symbols.ParserSymbol;
import i2.act.peg.symbols.Symbol;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

public final class Grammar extends ASTNode implements Iterable<Production> {

  private final List<Production> productions;

  public Grammar(final SourcePosition position) {
    this(position, new ArrayList<Production>());
  }

  public Grammar(final SourcePosition position, final List<Production> productions) {
    super(position);
    this.productions = productions;
  }

  public final void addProduction(final Production production) {
    this.productions.add(production);
  }

  public final void addRootProduction(final ParserProduction production) {
    this.productions.add(0, production);
  }

  public final List<Production> getProductions() {
    return Collections.unmodifiableList(this.productions);
  }

  public final List<Symbol> getSymbols() {
    return this.productions.stream()
        .map(p -> p.getSymbol())
        .collect(Collectors.toList());
  }

  public final List<ParserProduction> getParserProductions() {
    return this.productions.stream()
        .filter(p -> (p instanceof ParserProduction))
        .map(p -> (ParserProduction) p)
        .collect(Collectors.toList());
  }

  public final List<ParserSymbol> getParserSymbols() {
    return this.productions.stream()
        .filter(p -> (p instanceof ParserProduction))
        .map(p -> ((ParserProduction) p).getSymbol())
        .collect(Collectors.toList());
  }

  public final List<LexerProduction> getLexerProductions() {
    return this.productions.stream()
        .filter(p -> (p instanceof LexerProduction))
        .map(p -> (LexerProduction) p)
        .collect(Collectors.toList());
  }

  public final List<LexerSymbol> getLexerSymbols() {
    return this.productions.stream()
        .filter(p -> (p instanceof LexerProduction))
        .map(p -> ((LexerProduction) p).getSymbol())
        .collect(Collectors.toList());
  }

  public final ParserProduction getRootProduction() {
    for (final Production production : this.productions) {
      if (production instanceof ParserProduction) {
        return (ParserProduction) production;
      }
    }
    return null;
  }

  public final Symbol<?> getSymbol(final String name) {
    for (final Production production : this.productions) {
      final Symbol<?> symbol = production.getSymbol();
      if (symbol == null) {
        continue;
      }

      if (symbol.getName().equals(name)) {
        return symbol;
      }
    }

    return null;
  }

  @Override
  public final Iterator<Production> iterator() {
    return getProductions().iterator();
  }

  @Override
  public final Grammar clone(final boolean keepSymbols) {
    final List<Production> productionsClone = new ArrayList<>();
    {
      for (final Production production : this.productions) {
        final Production productionClone = production.clone(keepSymbols);
        productionsClone.add(productionClone);
      }
    }

    return new Grammar(this.position, productionsClone);
  }

  @Override
  public final <P, R> R accept(final ASTVisitor<P, R> visitor, final P param) {
    return visitor.visit(this, param);
  }

}
