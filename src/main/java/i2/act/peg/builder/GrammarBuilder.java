package i2.act.peg.builder;

import i2.act.peg.ast.*;
import i2.act.peg.ast.visitors.ImplicitQuantifierSymbolsVisitor;
import i2.act.peg.info.SourcePosition;
import i2.act.peg.parser.PEGParser;
import i2.act.peg.symbols.LexerSymbol;
import i2.act.peg.symbols.ParserSymbol;
import i2.act.peg.symbols.Symbol;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class GrammarBuilder {

  private final Map<String, Symbol<?>> symbols;

  public GrammarBuilder() {
    this.symbols = new LinkedHashMap<String, Symbol<?>>();
  }

  public final ParserSymbol declare(final String name) {
    if (this.symbols.containsKey(name)) {
      final Symbol<?> symbol = this.symbols.get(name);

      if (symbol instanceof LexerSymbol) {
        throw new RuntimeException(String.format("redefinition of '%s'", name));
      }

      assert (symbol instanceof ParserSymbol);
      return (ParserSymbol) symbol;
    } else {
      final ParserSymbol symbol = new ParserSymbol(name);
      this.symbols.put(name, symbol);

      return symbol;
    }
  }

  public final LexerSymbol define(final String name, final String regularExpressionString) {
    return define(name, regularExpressionString, false);
  }

  public final LexerSymbol define(final String name, final String regularExpressionString,
      final boolean skippedToken) {
    if (this.symbols.containsKey(name)) {
      throw new RuntimeException(String.format("redefinition of '%s'", name));
    }

    final List<Annotation> annotations = new ArrayList<>();
    {
      if (skippedToken) {
        final Annotation skipAnnotation = new Annotation(SourcePosition.UNKNOWN, Annotation.SKIP);
        annotations.add(skipAnnotation);
      }
    }

    final LexerSymbol symbol = new LexerSymbol(name, skippedToken);
    this.symbols.put(name, symbol);
    
    final LexerIdentifier identifier = (LexerIdentifier) toIdentifier(symbol);

    final RegularExpression regularExpression =
        PEGParser.parseRegularExpression(regularExpressionString);

    final LexerProduction production = new LexerProduction(
        SourcePosition.UNKNOWN, annotations, identifier, regularExpression);

    symbol.setProduction(production);

    return symbol;
  }

  public final ParserSymbol define(final String name, final GrammarBuilderNode production) {
    return define(declare(name), production);
  }

  public final ParserSymbol define(final ParserSymbol symbol,
      final GrammarBuilderNode rightHandSide, final Annotation... annotations) {
    assert (symbol.getProduction() == null);

    final List<Annotation> annotationList = new ArrayList<Annotation>(Arrays.asList(annotations));

    final ParserIdentifier identifier = (ParserIdentifier) toIdentifier(symbol);

    final Alternatives alternatives = toAlternatives(rightHandSide);

    final ParserProduction production = new ParserProduction(
        SourcePosition.UNKNOWN, annotationList, identifier, alternatives);

    symbol.setProduction(production);

    return symbol;
  }

  public final Grammar build() {
    final Grammar grammar = new Grammar(SourcePosition.UNKNOWN);

    for (final Symbol<?> symbol : this.symbols.values()) {
      final Production production = symbol.getProduction();

      if (production == null) {
        throw new RuntimeException(String.format("no definition for '%s'", symbol.getName()));
      }

      grammar.addProduction(production);
    }

    // add implicit quantifier symbols
    ImplicitQuantifierSymbolsVisitor.addImplicitQuantifierSymbols(grammar);

    return grammar;
  }

  // ===============================================================================================

  private static final Atom quantified(final GrammarBuilderNode node,
      final Quantifier.Kind quantifierKind, final int weight) {
    final Atom atom = toAtom(node);

    final Atom result;
    {
      if (atom.hasQuantifier()) {
        result = wrapAtom(atom);
      } else {
        result = atom;
      }
    }

    final Quantifier quantifier = new Quantifier(SourcePosition.UNKNOWN, quantifierKind, weight);
    result.setQuantifier(quantifier);

    return result;
  }

  public static final Atom opt(final GrammarBuilderNode node) {
    return opt(node, 1);
  }

  public static final Atom opt(final GrammarBuilderNode node, final int weight) {
    return quantified(node, Quantifier.Kind.QUANT_OPTIONAL, weight);
  }

  public static final Atom many(final GrammarBuilderNode node) {
    return many(node, 1);
  }

  public static final Atom many(final GrammarBuilderNode node, final int weight) {
    return quantified(node, Quantifier.Kind.QUANT_STAR, weight);
  }

  public static final Atom manyOne(final GrammarBuilderNode node) {
    return manyOne(node, 1);
  }

  public static final Atom manyOne(final GrammarBuilderNode node, final int weight) {
    return quantified(node, Quantifier.Kind.QUANT_PLUS, weight);
  }

  // ===============================================================================================

  public static final Alternatives alt(final GrammarBuilderNode... elements) {
    final Alternatives alternatives = new Alternatives(SourcePosition.UNKNOWN);

    for (final GrammarBuilderNode element : elements) {
      final Sequence alternative = toSequence(element);
      alternatives.addAlternative(alternative);
    }

    return alternatives;
  }

  public static final Sequence seq(final GrammarBuilderNode... elements) {
    final Sequence sequence = new Sequence(SourcePosition.UNKNOWN);

    for (final GrammarBuilderNode element : elements) {
      final Atom atom = toAtom(element);
      sequence.addElement(atom);
    }

    return sequence;
  }

  // ===============================================================================================

  private static final Identifier toIdentifier(final Symbol<?> symbol) {
    final String name = symbol.getName();

    if (symbol instanceof ParserSymbol) {
      final ParserIdentifier identifier = new ParserIdentifier(SourcePosition.UNKNOWN, name);
      identifier.setSymbol((ParserSymbol) symbol);

      return identifier;
    } else {
      assert (symbol instanceof LexerSymbol);

      final LexerIdentifier identifier = new LexerIdentifier(SourcePosition.UNKNOWN, name);
      identifier.setSymbol((LexerSymbol) symbol);

      return identifier;
    }
  }

  public static final Atom wrapAtom(final Atom atom) {
    final Sequence sequence = toSequence(atom);

    final Alternatives wrappedAtom = new Alternatives(SourcePosition.UNKNOWN);
    wrappedAtom.addAlternative(sequence);

    return wrappedAtom;
  }

  public static final Atom toAtom(final GrammarBuilderNode node) {
    if (node instanceof Atom) {
      return (Atom) node;
    }

    if (node instanceof Symbol<?>) {
      final Symbol<?> symbol = (Symbol<?>) node;
      return toIdentifier(symbol);
    }

    assert (node instanceof Sequence);
    final Sequence sequence = (Sequence) node;

    final Alternatives atom = new Alternatives(SourcePosition.UNKNOWN);
    atom.addAlternative(sequence);

    return atom;
  }

  public static final Sequence toSequence(final GrammarBuilderNode node) {
    if (node instanceof Sequence) {
      return (Sequence) node;
    }

    final Sequence sequence = new Sequence(SourcePosition.UNKNOWN);
    final Atom element = toAtom(node);
    sequence.addElement(element);

    return sequence;
  }

  public static final Alternatives toAlternatives(final GrammarBuilderNode node) {
    if (node instanceof Alternatives) {
      return (Alternatives) node;
    }

    final Alternatives alternatives = new Alternatives(SourcePosition.UNKNOWN);
    final Sequence alternative = toSequence(node);
    alternatives.addAlternative(alternative);

    return alternatives;
  }

}
