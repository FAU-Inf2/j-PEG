package i2.act.packrat;

import i2.act.packrat.nfa.NFA;
import i2.act.peg.ast.Grammar;
import i2.act.peg.ast.LexerProduction;
import i2.act.peg.ast.Production;
import i2.act.peg.ast.RegularExpression;
import i2.act.peg.error.InvalidInputException;
import i2.act.peg.info.SourcePosition;
import i2.act.peg.symbols.LexerSymbol;

import java.util.ArrayList;
import java.util.List;

public final class Lexer {

  private static final class TokenDefinition {

    public final LexerSymbol tokenSymbol;
    public final NFA nfa;

    public TokenDefinition(final LexerSymbol tokenSymbol, final NFA nfa) {
      this.tokenSymbol = tokenSymbol;
      this.nfa = nfa;
    }

  }

  public static final Lexer forGrammar(final Grammar grammar) {
    final List<TokenDefinition> tokenDefinitions = new ArrayList<>();

    for (final Production production : grammar) {
      if (production instanceof LexerProduction) {
        final LexerProduction lexerProduction = (LexerProduction) production;
        final LexerSymbol tokenSymbol = lexerProduction.getSymbol();

        final RegularExpression regularExpression = lexerProduction.getRegularExpression();
        final NFA nfa = NFA.fromRegularExpression(regularExpression);

        final TokenDefinition tokenDefinition = new TokenDefinition(tokenSymbol, nfa);
        tokenDefinitions.add(tokenDefinition);
      }
    }

    return new Lexer(tokenDefinitions);
  }


  //------------------------------------------------------------------------------------------------


  private final List<TokenDefinition> tokenDefinitions;
  
  private Lexer(final List<TokenDefinition> tokenDefinitions) {
    this.tokenDefinitions = tokenDefinitions;
  }

  public final TokenStream lex(final String input) {
    return lex(input, false);
  }

  public final TokenStream lex(final String input, final boolean includeSkippedTokens) {
    return lex(input.toCharArray(), includeSkippedTokens);
  }

  public final TokenStream lex(final char[] input) {
    return lex(input, false);
  }

  public final TokenStream lex(final char[] input, final boolean includeSkippedTokens) {
    final List<Token> tokens = new ArrayList<>();

    int line = 1;
    int column = 1;

    List<Token> skippedTokensBefore = new ArrayList<>();

    int index = 0;
    while (index < input.length) {
      int longestMatch = 0;
      TokenDefinition longestMatchDefinition = null;

      for (final TokenDefinition tokenDefinition : this.tokenDefinitions) {
        final int prefixMatch = tokenDefinition.nfa.prefixMatch(input, index);

        if (prefixMatch > longestMatch) {
          longestMatch = prefixMatch;
          longestMatchDefinition = tokenDefinition;
        }
      }

      if (longestMatch == 0) {
        // no NFA matched a prefix --> error
        final SourcePosition position = new SourcePosition(index, line, column);
        throw new InvalidInputException("no matching alternative", position);
      } else {
        assert (longestMatchDefinition != null);

        final int beginIndex = index;
        final SourcePosition begin = new SourcePosition(index, line, column);

        // advance indexes
        {
          for (int subIndex = 0; subIndex < longestMatch; ++subIndex) {
            final char character = input[index + subIndex];

            ++column;

            if (character == '\n') {
              ++line;
              column = 1;
            }
          }

          index += longestMatch;
        }

        // add token to token stream (if not skipped)
        final String value;
        {
          if (longestMatchDefinition.nfa.hasLiteralString()) {
            value = longestMatchDefinition.nfa.getLiteralString();
          } else {
            value = getSubString(input, beginIndex, longestMatch);
          }
        }
        final SourcePosition end = new SourcePosition(index, line, column);

        final LexerSymbol tokenSymbol = longestMatchDefinition.tokenSymbol;

        if (!includeSkippedTokens && tokenSymbol.isSkippedToken()) {
          final Token token = new Token(tokenSymbol, value, begin, end);
          skippedTokensBefore.add(token);
        } else {
          final Token token = new Token(tokenSymbol, value, begin, end, skippedTokensBefore);
          tokens.add(token);

          skippedTokensBefore = new ArrayList<Token>();
        }
      }
    }

    return new TokenStream(tokens);
  }

  private static final String getSubString(final char[] input, final int begin, final int size) {
    assert (begin + size <= input.length);

    final char[] substring = new char[size];
    System.arraycopy(input, begin, substring, 0, size);

    return new String(substring);
  }

}
