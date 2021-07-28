package i2.act.packrat;

import i2.act.packrat.cst.Node;
import i2.act.packrat.cst.NonTerminalNode;
import i2.act.packrat.cst.TerminalNode;
import i2.act.peg.ast.*;
import i2.act.peg.ast.visitors.BaseASTVisitor;
import i2.act.peg.symbols.LexerSymbol;
import i2.act.peg.symbols.ParserSymbol;
import i2.act.peg.symbols.Symbol;

import java.util.*;
import java.util.stream.Collectors;

public abstract class Parser {

  private static final LexerSymbol EMPTY = new LexerSymbol("EMPTY", false, null);

  protected final Set<LexerSymbol> first;

  public Parser() {
    this.first = new HashSet<LexerSymbol>();
  }

  public final Node<?> parse(final TokenStream tokens) {
    final ParserResult result = parse(tokens, 0, null, 0, true);

    if (result instanceof ParserFailure) {
      throw constructException(tokens);
    }

    final ParserSuccess success = (ParserSuccess) result;

    if (success.syntaxTrees.isEmpty()) {
      return null;
    } else {
      final Node<?> rootNode = success.syntaxTrees.get(0);
      rootNode.setParentReferences();

      return rootNode;
    }
  }
  
  protected final ParserResult parse(final TokenStream tokens, final int position,
      final ParserReference parentRule, final int parentPosition,
      final boolean buildSyntaxTree) {
    final Token token = tokens.at(position);

    if (parentRule == this && ((ParserReference) this).growing.containsKey(position)) {
      // during recursive invocation
      final ParserResult result = ((ParserReference) this).growing.get(position);
      return result;
    } else if (parentRule == this && position == parentPosition) {
      // first left-recursive invocation
      final ParserReference parser = (ParserReference) this;

      parser.growing.put(position, ParserFailure.PARSER_FAILURE);
      ParserResult previousResult = ParserFailure.PARSER_FAILURE;

      while (true) {
        final ParserResult parserResult =
            apply(tokens, position, parentRule, parentPosition, buildSyntaxTree);

        final ParserResult seed = parser.growing.get(position);

        if ((parserResult == ParserFailure.PARSER_FAILURE)
            || ((seed instanceof ParserSuccess)
                && ((ParserSuccess) parserResult).position <= ((ParserSuccess) seed).position)) {
          parser.growing.remove(position);

          token.packratCache.put(this, previousResult);
          return previousResult;
        }

        parser.growing.put(position, parserResult);
        previousResult = seed;
      }
    } else {
      // non-left recursive call
      if (token.packratCache.containsKey(this)) {
        final ParserResult cachedResult = token.packratCache.get(this);
        return cachedResult;
      }

      if (!matchesFirst(token, this.first)) {
        final ParserResult result = ParserFailure.PARSER_FAILURE;
        token.packratCache.put(this, result);

        return result;
      }

      final ParserResult parserResult =
          apply(tokens, position, parentRule, parentPosition, buildSyntaxTree);

      if (this instanceof ParserReference) {
        token.packratCache.put(this, parserResult);
      }

      return parserResult;
    }
  }

  private final ParsingFailedException constructException(final TokenStream tokenStream) {
    Token lastErrorToken = null;
    {
      if (tokenStream.EOF.hasParserFailure()) {
        lastErrorToken = tokenStream.EOF;
      } else {
        final List<Token> tokens = tokenStream.getTokens();
        for (int index = tokens.size() - 1; index >= 0; --index) {
          final Token token = tokens.get(index);

          if (token.hasParserFailure()) {
            lastErrorToken = token;
            break;
          }
        }
      }
    }

    assert (lastErrorToken != null);

    boolean allExpectedSymbolsKnown = true;
    final Set<LexerSymbol> expectedSymbols = new HashSet<>();
    {
      for (final Parser parser : lastErrorToken.packratCache.keySet()) {
        if (parser.first.isEmpty()) {
          allExpectedSymbolsKnown = false;
          break;
        }

        expectedSymbols.addAll(parser.first);
      }
    }

    final String found = (lastErrorToken == tokenStream.EOF)
        ? "end of file"
        : String.format("'%s'", lastErrorToken.getValue());
    final String message;

    if (allExpectedSymbolsKnown) {
      // sort expected symbols
      final List<LexerSymbol> expectedSymbolsSorted = new ArrayList<>(expectedSymbols);
      Collections.sort(expectedSymbolsSorted,
          (final LexerSymbol s1, final LexerSymbol s2) -> s1.getName().compareTo(s2.getName()));
      final String expected = expectedSymbolsSorted.stream()
          .map(LexerSymbol::getName)
          .collect(Collectors.joining(", ", "{", "}"));

      message = String.format("expected %s, but found %s", expected, found);
    } else {
      message = String.format("unexpected %s", found);
    }

    return new ParsingFailedException(lastErrorToken.getBegin(), message);
  }

  private static final boolean matchesFirst(final Token lookAhead, final Set<LexerSymbol> first) {
    if (first.isEmpty() || first.contains(EMPTY)) {
      return true;
    }
    return first.contains(lookAhead.getTokenSymbol());
  }

  protected abstract ParserResult apply(final TokenStream tokens, final int position,
      final ParserReference parentRule, final int parentPosition, final boolean buildSyntaxTree);

  protected abstract boolean computeFirst();


  //------------------------------------------------------------------------------------------------


  public static final class ParserReference extends Parser {

    private final ParserSymbol symbol;

    private Parser parser; 

    protected final Map<Integer, ParserResult> growing;

    public ParserReference(final ParserSymbol symbol) {
      this.symbol = symbol;
      this.growing = new HashMap<Integer, ParserResult>();
    }

    public final boolean hasReference() {
      return this.parser != null;
    }

    public final Parser getReference() {
      return this.parser;
    }

    public final void setReference(final Parser parser) {
      this.parser = parser;
    }

    @Override
    public final ParserResult apply(final TokenStream tokens, final int position,
        final ParserReference parentRule, final int parentPosition, final boolean buildSyntaxTree) {
      if (!hasReference()) {
        throw new RuntimeException("unresolved parser reference");
      }

      final ParserResult parserResult =
          this.parser.parse(tokens, position, this, position, buildSyntaxTree);

      if (parserResult instanceof ParserFailure) {
        return ParserFailure.PARSER_FAILURE;
      }

      assert (parserResult instanceof ParserSuccess);
      final ParserSuccess parserSuccess = (ParserSuccess) parserResult;

      if (buildSyntaxTree && !parserSuccess.syntaxTrees.isEmpty()) {
        final Node<?> node = new NonTerminalNode(this.symbol, parserSuccess.syntaxTrees);
        node.addAnnotations(this.symbol.getProduction().getAnnotations());

        return new ParserSuccess(parserSuccess.position, node);
      } else {
        return parserSuccess;
      }
    }

    @Override
    protected final boolean computeFirst() {
      assert (this.parser != null);

      final boolean change = this.parser.computeFirst();
      this.first.addAll(this.parser.first);

      return change;
    }

    @Override
    public final String toString() {
      return String.format("ref(%s)", this.symbol);
    }

  }

  //------------------------------------------------------------------------------------------------

  public static final boolean DEFAULT_QUANTIFIER_NODES = true;

  public static final Parser token(final LexerSymbol tokenSymbol) {
    return new Parser() {

      @Override
      public final ParserResult apply(final TokenStream tokens, final int position,
          final ParserReference parentRule, final int parentPosition,
          final boolean buildSyntaxTree) {
        final Token token = tokens.at(position);

        if (token.getTokenSymbol() == tokenSymbol) {
          if (buildSyntaxTree) {
            final TerminalNode node = new TerminalNode(token);
            if (token.getTokenSymbol().getProduction() != null) {
              node.addAnnotations(token.getTokenSymbol().getProduction().getAnnotations());
            }

            return new ParserSuccess(position + 1, node);
          } else {
            return new ParserSuccess(position + 1);
          }
        }

        return ParserFailure.PARSER_FAILURE;
      }

      @Override
      protected final boolean computeFirst() {
        if (this.first.isEmpty()) {
          this.first.add(tokenSymbol);

          return true;
        } else {
          return false;
        }
      }

      @Override
      public final String toString() {
        return String.format("token(%s)", tokenSymbol);
      }

    };
  }

  public static final Parser sequence(final Parser... elements) {
    return new Parser() {

      @Override
      public final ParserResult apply(final TokenStream tokens, final int position,
          final ParserReference parentRule, final int parentPosition,
          final boolean buildSyntaxTree) {
        //System.out.println("sequence");

        final List<Node<?>> syntaxTrees = (buildSyntaxTree) ? (new ArrayList<>()) : (null);

        int currentPosition = position;

        for (final Parser element : elements) {
          final ParserResult elementResult =
              element.parse(tokens, currentPosition, parentRule, parentPosition, buildSyntaxTree);

          if (elementResult instanceof ParserFailure) {
            return ParserFailure.PARSER_FAILURE;
          }

          assert (elementResult instanceof ParserSuccess);
          final ParserSuccess elementSuccess = (ParserSuccess) elementResult;
          
          if (buildSyntaxTree) {
            syntaxTrees.addAll(elementSuccess.syntaxTrees);
          }

          currentPosition = elementSuccess.position;
        }

        if (buildSyntaxTree) {
          return new ParserSuccess(currentPosition, syntaxTrees);
        } else {
          return new ParserSuccess(currentPosition);
        }
      }

      @Override
      protected final boolean computeFirst() {
        final Set<LexerSymbol> newFirst = new HashSet<>();

        if (elements.length == 0) {
          newFirst.add(EMPTY);
        } else {
          for (final Parser element : elements) {
            newFirst.addAll(element.first);

            if (!element.first.contains(EMPTY)) {
              break;
            }
          }
        }

        final boolean change = !newFirst.equals(this.first);
        this.first.addAll(newFirst);

        return change;
      }

      @Override
      public final String toString() {
        return Arrays.stream(elements)
            .map(Parser::toString)
            .collect(Collectors.joining(", ", "seq(", ")"));
      }

    };
  }

  public static final Parser alternatives(final Parser... alternatives) {
    return new Parser() {

      @Override
      public final ParserResult apply(final TokenStream tokens, final int position,
          final ParserReference parentRule, final int parentPosition,
          final boolean buildSyntaxTree) {
        //System.err.println("alternatives");

        final Token lookAhead = tokens.at(position);

        for (final Parser alternative : alternatives) {
          if (matchesFirst(lookAhead, alternative.first)) {
            final ParserResult alternativeResult =
                alternative.parse(tokens, position, parentRule, parentPosition, buildSyntaxTree);

            if (alternativeResult instanceof ParserSuccess) {
              return alternativeResult;
            } else {
              // alternative does not match -> try next alternative
            }
          }
        }

        return ParserFailure.PARSER_FAILURE;
      }

      @Override
      protected final boolean computeFirst() {
        final Set<LexerSymbol> newFirst = new HashSet<>();

        for (final Parser alternative : alternatives) {
          newFirst.addAll(alternative.first);
        }

        final boolean change = !newFirst.equals(this.first);
        this.first.addAll(newFirst);

        return change;
      }

      @Override
      public final String toString() {
        return Arrays.stream(alternatives)
            .map(Parser::toString)
            .collect(Collectors.joining(", ", "alt(", ")"));
      }

    };
  }

  public static final Parser optional(final Parser parser) {
    return optional(parser, DEFAULT_QUANTIFIER_NODES, null);
  }

  public static final Parser optional(final Parser parser, final boolean quantifierNodes) {
    return optional(parser, quantifierNodes, null);
  }

  public static final Parser optional(final Parser parser, final boolean quantifierNodes,
      final Symbol<?> quantifiedSymbol) {
    assert (!quantifierNodes || quantifiedSymbol != null);

    return new Parser() {

      @Override
      public final ParserResult apply(final TokenStream tokens, final int position,
          final ParserReference parentRule, final int parentPosition,
          final boolean buildSyntaxTree) {
        //System.out.println("optional");

        final Token lookAhead = tokens.at(position);
        if (!matchesFirst(lookAhead, parser.first)) {
          return new ParserSuccess(position);
        }

        final ParserResult parserResult =
            parser.parse(tokens, position, parentRule, parentPosition, buildSyntaxTree);

        if (parserResult instanceof ParserSuccess) {
          final ParserSuccess success = (ParserSuccess) parserResult;
          
          if (buildSyntaxTree && quantifierNodes && success.position != position) {
            final NonTerminalNode itemNode =
                new NonTerminalNode(ParserSymbol.LIST_ITEM, success.syntaxTrees);

            if (quantifiedSymbol != null) {
              itemNode.setExpectedSymbol(quantifiedSymbol);
            }

            final List<Node<?>> itemNodeList = new ArrayList<>();
            itemNodeList.add(itemNode);

            final NonTerminalNode node =
                new NonTerminalNode(ParserSymbol.OPTIONAL, itemNodeList);
            return new ParserSuccess(success.position, node);
          } else {
            return success;
          }
        } else {
          // parser does not match -> do not advance in token stream
          return new ParserSuccess(position);
        }
      }

      @Override
      protected final boolean computeFirst() {
        final Set<LexerSymbol> newFirst = new HashSet<>();

        newFirst.addAll(parser.first);
        newFirst.add(EMPTY);

        final boolean change = !newFirst.equals(this.first);
        this.first.addAll(newFirst);

        return change;
      }

      @Override
      public final String toString() {
        return String.format("opt(%s)", parser);
      }

    };
  }

  public static final Parser many(final Parser parser) {
    return many(parser, DEFAULT_QUANTIFIER_NODES, null);
  }

  public static final Parser many(final Parser parser, final boolean quantifierNodes) {
    return many(parser, quantifierNodes, null);
  }

  public static final Parser many(final Parser parser, final boolean quantifierNodes,
      final Symbol<?> quantifiedSymbol) {
    assert (!quantifierNodes || quantifiedSymbol != null);

    return new Parser() {

      @Override
      public final ParserResult apply(final TokenStream tokens, final int position,
          final ParserReference parentRule, final int parentPosition,
          final boolean buildSyntaxTree) {
        //System.out.println("many");

        final List<Node<?>> syntaxTrees = (buildSyntaxTree) ? (new ArrayList<>()) : (null);

        int currentPosition = position;

        while (true) {
          final Token lookAhead = tokens.at(currentPosition);
          if (!matchesFirst(lookAhead, parser.first)) {
            break;
          }

          final ParserResult parserResult =
              parser.parse(tokens, currentPosition, parentRule, parentPosition, buildSyntaxTree);

          if (parserResult instanceof ParserFailure) {
            // parser no longer matches -> break
            break;
          }
          
          assert (parserResult instanceof ParserSuccess);
          final ParserSuccess success = (ParserSuccess) parserResult;

          if (success.position == currentPosition) {
            // did not consume any tokens
            // -> next call would not consume any tokens either
            // -> done
            break;
          }

          if (buildSyntaxTree) {
            if (quantifierNodes) {
              final NonTerminalNode itemNode =
                  new NonTerminalNode(ParserSymbol.LIST_ITEM, success.syntaxTrees);

              if (quantifiedSymbol != null) {
                itemNode.setExpectedSymbol(quantifiedSymbol);
              }

              syntaxTrees.add(itemNode);
            } else {
              syntaxTrees.addAll(success.syntaxTrees);
            }
          }

          currentPosition = success.position;
        }

        if (buildSyntaxTree && !syntaxTrees.isEmpty()) {
          if (quantifierNodes) {
            final NonTerminalNode node = new NonTerminalNode(ParserSymbol.STAR, syntaxTrees);
            return new ParserSuccess(currentPosition, node);
          } else {
            return new ParserSuccess(currentPosition, syntaxTrees);
          }
        } else {
          return new ParserSuccess(currentPosition);
        }
      }

      @Override
      protected final boolean computeFirst() {
        final Set<LexerSymbol> newFirst = new HashSet<>();

        newFirst.addAll(parser.first);
        newFirst.add(EMPTY);

        final boolean change = !newFirst.equals(this.first);
        this.first.addAll(newFirst);

        return change;
      }

      @Override
      public final String toString() {
        return String.format("many(%s)", parser);
      }

    };
  }

  public static final Parser manyOne(final Parser parser) {
    return manyOne(parser, DEFAULT_QUANTIFIER_NODES, null);
  }

  public static final Parser manyOne(final Parser parser, final boolean quantifierNodes) {
    return manyOne(parser, quantifierNodes, null);
  }

  public static final Parser manyOne(final Parser parser, final boolean quantifierNodes,
      final Symbol<?> quantifiedSymbol) {
    assert (!quantifierNodes || quantifiedSymbol != null);

    final Parser remainingParser = many(parser, quantifierNodes, quantifiedSymbol);

    return new Parser() {

      @Override
      public final ParserResult apply(final TokenStream tokens, final int position,
          final ParserReference parentRule, final int parentPosition,
          final boolean buildSyntaxTree) {
        //System.out.println("manyOne");

        final List<Node<?>> syntaxTrees = (buildSyntaxTree) ? (new ArrayList<>()) : (null);

        // first element
        final ParserResult firstResult =
            parser.parse(tokens, position, parentRule, parentPosition, buildSyntaxTree);

        if (firstResult instanceof ParserFailure) {
          return ParserFailure.PARSER_FAILURE;
        }

        assert (firstResult instanceof ParserSuccess);
        final ParserSuccess firstSuccess = (ParserSuccess) firstResult;

        if (firstSuccess.position == position) {
          return firstSuccess;
        }

        if (buildSyntaxTree) {
          if (quantifierNodes) {
            final NonTerminalNode itemNode =
                new NonTerminalNode(ParserSymbol.LIST_ITEM, firstSuccess.syntaxTrees);

            if (quantifiedSymbol != null) {
              itemNode.setExpectedSymbol(quantifiedSymbol);
            }

            syntaxTrees.add(itemNode);
          } else {
            syntaxTrees.addAll(firstSuccess.syntaxTrees);
          }
        }

        // remaining elements
        final ParserResult remainingResult;
        {
          if (remainingParser.first.isEmpty()) {
            remainingParser.computeFirst();
          }

          remainingResult = remainingParser.parse(
              tokens, firstSuccess.position, parentRule, parentPosition, buildSyntaxTree);
        }

        assert (remainingResult instanceof ParserSuccess);
        final ParserSuccess remainingSuccess = (ParserSuccess) remainingResult;

        if (buildSyntaxTree && !remainingSuccess.syntaxTrees.isEmpty()) {
          if (quantifierNodes) {
            assert (remainingSuccess.syntaxTrees.size() == 1);
            assert (remainingSuccess.syntaxTrees.get(0) instanceof NonTerminalNode);

            syntaxTrees.addAll(
                ((NonTerminalNode) remainingSuccess.syntaxTrees.get(0)).getChildren());
          } else {
            syntaxTrees.addAll(remainingSuccess.syntaxTrees);
          }
        }

        if (buildSyntaxTree) {
          if (quantifierNodes) {
            final NonTerminalNode node = new NonTerminalNode(ParserSymbol.PLUS, syntaxTrees);
            return new ParserSuccess(remainingSuccess.position, node);
          } else {
            return new ParserSuccess(remainingSuccess.position, syntaxTrees);
          }
        } else {
          return remainingSuccess;
        }
      }

      @Override
      protected final boolean computeFirst() {
        if (this.first.equals(parser.first)) {
          return false;
        }

        this.first.addAll(parser.first);
        return true;
      }

      @Override
      public final String toString() {
        return String.format("many_one(%s)", parser);
      }

    };
  }

  //------------------------------------------------------------------------------------------------

  public static final Parser fromGrammar(final Grammar grammar) {
    return fromGrammar(grammar, grammar.getRootProduction().getSymbol(), DEFAULT_QUANTIFIER_NODES);
  }

  public static final Parser fromGrammar(final Grammar grammar, final ParserSymbol startSymbol) {
    return fromGrammar(grammar, startSymbol, DEFAULT_QUANTIFIER_NODES);
  }

  public static final Parser fromGrammar(final Grammar grammar, final boolean quantifierNodes) {
    return fromGrammar(grammar, grammar.getRootProduction().getSymbol(), quantifierNodes);
  }

  public static final Parser fromGrammar(final Grammar grammar, final ParserSymbol startSymbol,
      final boolean quantifierNodes) {
    final Map<Symbol, Parser> symbolParsers = new HashMap<>();
    final List<Parser> allParsers = new ArrayList<>();

    // implicitly defined EOF production
    {
      final Parser eofParser = token(LexerSymbol.EOF);

      symbolParsers.put(LexerSymbol.EOF, eofParser);
      allParsers.add(eofParser);
    }

    // explicitly defined productions
    {
      for (final Production production : grammar) {
        final Symbol symbol = production.getSymbol();
        assert (symbol != null);

        if (production instanceof LexerProduction) {
          assert (symbol instanceof LexerSymbol);

          final Parser parser = token((LexerSymbol) symbol);

          symbolParsers.put(symbol, parser);
          allParsers.add(parser);
        } else {
          assert (production instanceof ParserProduction);
          assert (symbol instanceof ParserSymbol);

          final ParserReference parserReference = new ParserReference((ParserSymbol) symbol);

          symbolParsers.put(symbol, parserReference);
          allParsers.add(parserReference);
        }
      }
    }

    grammar.accept(new BaseASTVisitor<Void, Parser>() {

      @Override
      public final Parser visit(final ParserProduction parserProduction, final Void parameter) {
        final Alternatives rightHandSide = parserProduction.getRightHandSide();
        final Parser parser = rightHandSide.accept(this, parameter);

        final Symbol symbol = parserProduction.getSymbol();
        assert (symbol != null);

        final Parser parserReference = symbolParsers.get(symbol);
        assert (parserReference instanceof ParserReference);
        ((ParserReference) parserReference).setReference(parser);

        return parser;
      }

      @Override
      public final Parser visit(final LexerProduction lexerProduction, final Void parameter) {
        // nothing to do here
        return null;
      }

      @Override
      public final Parser visit(final ParserIdentifier parserIdentifier, final Void parameter) {
        final Symbol symbol = parserIdentifier.getSymbol();
        assert (symbol != null);

        final Parser symbolParser = symbolParsers.get(symbol);
        assert (symbolParser != null);

        final Symbol<?> quantifiedSymbol = symbol;

        final Parser quantifiedParser =
            applyQuantifier(symbolParser, parserIdentifier.getQuantifier(), quantifiedSymbol);
        allParsers.add(quantifiedParser);

        return quantifiedParser;
      }

      @Override
      public final Parser visit(final LexerIdentifier lexerIdentifier, final Void parameter) {
        final Symbol symbol = lexerIdentifier.getSymbol();
        assert (symbol != null);

        final Parser symbolParser = symbolParsers.get(symbol);
        assert (symbolParser != null);

        final Symbol<?> quantifiedSymbol = symbol;

        final Parser quantifiedParser
            = applyQuantifier(symbolParser, lexerIdentifier.getQuantifier(), quantifiedSymbol);
        allParsers.add(quantifiedParser);

        return quantifiedParser;
      }

      @Override
      public final Parser visit(final Alternatives alternatives, final Void parameter) {
        final int numberOfAlternatives = alternatives.getNumberOfAlternatives();
        final Parser[] alternativeParsers = new Parser[numberOfAlternatives]; 

        int index = 0;
        for (final Sequence alternative : alternatives) {
          final Parser alternativeParser = alternative.accept(this, parameter);
          alternativeParsers[index++] = alternativeParser;
        }

        final Parser alternativeParser = alternatives(alternativeParsers);
        allParsers.add(alternativeParser);

        final Symbol<?> quantifiedSymbol = (alternatives.hasQuantifier())
            ? getQuantifiedSymbol(alternatives)
            : null;

        final Parser quantifiedParser =
            applyQuantifier(alternativeParser, alternatives.getQuantifier(), quantifiedSymbol);
        allParsers.add(quantifiedParser);

        return quantifiedParser;
      }

      @Override
      public final Parser visit(final Sequence sequence, final Void parameter) {
        final int numberOfElements = sequence.getNumberOfElements();
        final Parser[] elementParsers = new Parser[numberOfElements];

        int index = 0;
        for (final Atom element : sequence) {
          final Parser elementParser = element.accept(this, parameter);
          elementParsers[index++] = elementParser;
        }

        final Parser sequenceParser = sequence(elementParsers);
        allParsers.add(sequenceParser);

        return sequenceParser;
      }

      private final Symbol<?> getQuantifiedSymbol(final Alternatives alternatives) {
        if (alternatives.hasImplicitQuantifierSymbol()) {
          return alternatives.getImplicitQuantifierSymbol();
        }

        if (alternatives.getNumberOfAlternatives() > 1) {
          assert (!alternatives.hasQuantifier());
          return null;
        }

        final Sequence singleSequence = alternatives.getAlternative(0);
        return getQuantifiedSymbol(singleSequence);
      }

      private final Symbol<?> getQuantifiedSymbol(final Sequence sequence) {
        assert (sequence.getNumberOfElements() == 1);

        final Atom singleElement = sequence.getElement(0);

        if (singleElement instanceof Identifier<?>) {
          return ((Identifier<?>) singleElement).getSymbol();
        } else {
          assert (singleElement instanceof Alternatives);
          return getQuantifiedSymbol((Alternatives) singleElement);
        }
      }

      private int quantifiedSymbolCounter = 0;

      private final Parser applyQuantifier(final Parser parser, final Atom.Quantifier quantifier,
          final Symbol<?> quantifiedSymbol) {
        switch (quantifier) {
          case QUANT_OPTIONAL: {
            return optional(parser, quantifierNodes, quantifiedSymbol);
          }
          case QUANT_STAR: {
            return many(parser, quantifierNodes, quantifiedSymbol);
          }
          case QUANT_PLUS: {
            return manyOne(parser, quantifierNodes, quantifiedSymbol);
          }
          default: {
            assert (quantifier == Atom.Quantifier.QUANT_NONE);
            return parser;
          }
        }
      }

    }, null);

    // compute first sets
    {
      boolean change = true;
      while (change) {
        change = false;

        for (final Parser parser : allParsers) {
          change |= parser.computeFirst();
        }
      }
    }

    // print first sets for testing
    if (false) {
      for (final Map.Entry<Symbol, Parser> entry : symbolParsers.entrySet()) {
        final Symbol symbol = entry.getKey();
        final Parser parser = entry.getValue();

        System.err.format("%s => %s\n", symbol, parser.first);
      }
    }

    assert (symbolParsers.containsKey(startSymbol));
    return symbolParsers.get(startSymbol);
  }

}
