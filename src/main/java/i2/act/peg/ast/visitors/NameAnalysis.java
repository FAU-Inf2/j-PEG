package i2.act.peg.ast.visitors;

import i2.act.peg.ast.*;
import i2.act.peg.error.InvalidInputException;
import i2.act.peg.info.SourcePosition;
import i2.act.peg.symbols.LexerSymbol;
import i2.act.peg.symbols.ParserSymbol;
import i2.act.peg.symbols.Symbol;

import java.util.HashMap;
import java.util.Map;

public final class NameAnalysis extends BaseASTVisitor<NameAnalysis.SymbolTable, Void> {

  public static final class SymbolTable {

    private final Map<String, Symbol<?>> symbols;

    public SymbolTable() {
      this.symbols = new HashMap<String, Symbol<?>>();
      putSymbol(LexerSymbol.EOF);
    }

    public final void putSymbol(final Symbol<?> symbol) {
      final String name = symbol.getName();

      if (this.symbols.containsKey(name)) {
        throw new InvalidInputException(
            String.format("symbol '%s' already defined", name),
            symbol.getProduction().getPosition());
      }

      this.symbols.put(name, symbol);
    }

    public final Symbol<?> lookupSymbol(final String name, final SourcePosition position) {
      if (!this.symbols.containsKey(name)) {
        throw new InvalidInputException(
            String.format("symbol '%s' not defined", name),
            position);
      }

      return this.symbols.get(name);
    }

  }


  //------------------------------------------------------------------------------------------------


  public static final void analyze(final Grammar grammar) {
    final NameAnalysis analysis = new NameAnalysis();
    final SymbolTable symbolTable = new SymbolTable();

    analysis.visit(grammar, symbolTable);

    // add implicit quantifier symbols
    ImplicitQuantifierSymbolsVisitor.addImplicitQuantifierSymbols(grammar);
  }


  //------------------------------------------------------------------------------------------------


  private boolean firstVisit;

  @Override
  public final Void visit(final Grammar grammar, final SymbolTable symbolTable) {
    // first visit: analyze all declarations
    this.firstVisit = true;
    super.visit(grammar, symbolTable);

    // second visit: analyze all usages
    this.firstVisit = false;
    super.visit(grammar, symbolTable);

    return null;
  }

  @Override
  public final Void visit(final ParserProduction parserProduction, final SymbolTable symbolTable) {
    if (this.firstVisit) {
      final ParserIdentifier leftHandSide = parserProduction.getLeftHandSide();
      final ParserSymbol symbol;
      {
        if (leftHandSide.getSymbol() != null) {
          symbol = leftHandSide.getSymbol();
        } else {
          symbol = new ParserSymbol(leftHandSide.getName(), parserProduction);
        }
      }

      symbolTable.putSymbol(symbol);
      leftHandSide.setSymbol(symbol);
    } else {
      final Alternatives rightHandSide = parserProduction.getRightHandSide();
      rightHandSide.accept(this, symbolTable);
    }

    return null;
  }

  @Override
  public final Void visit(final LexerProduction lexerProduction, final SymbolTable symbolTable) {
    if (this.firstVisit) {
      final LexerIdentifier leftHandSide = lexerProduction.getLeftHandSide();

      final boolean isSkippedToken = lexerProduction.isSkippedToken();
      final LexerSymbol symbol;
      {
        if (leftHandSide.getSymbol() != null) {
          symbol = leftHandSide.getSymbol();
        } else {
          symbol = new LexerSymbol(leftHandSide.getName(), isSkippedToken, lexerProduction);
        }
      }

      symbolTable.putSymbol(symbol);
      leftHandSide.setSymbol(symbol);
    } else {
      final RegularExpression regularExpression = lexerProduction.getRegularExpression();
      regularExpression.accept(this, symbolTable);
    }

    return null;
  }

  @Override
  public final Void visit(final ParserIdentifier parserIdentifier, final SymbolTable symbolTable) {
    assert (!this.firstVisit);

    final String name = parserIdentifier.getName();

    final Symbol<?> symbol = symbolTable.lookupSymbol(name, parserIdentifier.getPosition());
    assert (symbol instanceof ParserSymbol) : symbol;

    parserIdentifier.setSymbol((ParserSymbol) symbol);

    return null;
  }

  @Override
  public final Void visit(final LexerIdentifier lexerIdentifier, final SymbolTable symbolTable) {
    assert (!this.firstVisit);

    final String name = lexerIdentifier.getName();

    final Symbol<?> symbol = symbolTable.lookupSymbol(name, lexerIdentifier.getPosition());
    assert (symbol instanceof LexerSymbol) : symbol;

    lexerIdentifier.setSymbol((LexerSymbol) symbol);

    return null;
  }

}
