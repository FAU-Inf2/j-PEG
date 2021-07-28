package i2.act.peg.parser;

import i2.act.peg.ast.*;
import i2.act.peg.lexer.PEGLexer;
import i2.act.peg.lexer.PEGToken;
import i2.act.peg.lexer.PEGToken.PEGTokenKind;

import java.util.ArrayList;
import java.util.List;

public final class PEGParser {

  public static final Grammar parse(final String input) {
    return parse(new PEGLexer(input));
  }

  public static final Grammar parse(final PEGLexer lexer) {
    return parseGrammar(lexer);
  }

  private static final Grammar parseGrammar(final PEGLexer lexer) {
    // grammar: production* EOF ;

    final Grammar grammar = new Grammar(lexer.getPosition());

    while (lexer.peek().kind != PEGTokenKind.TK_EOF) {
      final Production production = parseProduction(lexer);
      grammar.addProduction(production);
    }

    lexer.assertPop(PEGTokenKind.TK_EOF);

    return grammar;
  }

  public static final Production parseProduction(final String input) {
    return parseProduction(new PEGLexer(input));
  }

  private static final Production parseProduction(final PEGLexer lexer) {
    // production: ( annotation )* ( parser_production | lexer_production ) ;

    final List<Annotation> annotations = parseAnnotations(lexer);

    lexer.assertPeek(PEGTokenKind.TK_PARSER_ID, PEGTokenKind.TK_LEXER_ID);

    if (lexer.peek().kind == PEGTokenKind.TK_PARSER_ID) {
      return parseParserProduction(lexer, annotations);
    } else {
      assert (lexer.peek().kind == PEGTokenKind.TK_LEXER_ID);
      return parseLexerProduction(lexer, annotations);
    }
  }

  private static final List<Annotation> parseAnnotations(final PEGLexer lexer) {
    // annotation: ANNOTATION_KEY ( LPAREN ( argument )? RPAREN )?

    final List<Annotation> annotations = new ArrayList<>();

    while (lexer.peek().kind == PEGTokenKind.TK_ANNOTATION_KEY) {
      final PEGToken annotationKeyToken = lexer.assertPop(PEGTokenKind.TK_ANNOTATION_KEY);
      final String key = annotationKeyToken.string;

      final Argument value;
      {
        if (lexer.peek().kind == PEGTokenKind.TK_LPAREN) {
          lexer.assertPop(PEGTokenKind.TK_LPAREN);

          if (lexer.peek().kind == PEGTokenKind.TK_RPAREN) {
            value = null;
          } else {
            value = parseArgument(lexer);
          }

          lexer.assertPop(PEGTokenKind.TK_RPAREN);
        } else {
          value = null;
        }
      }

      final Annotation annotation = new Annotation(annotationKeyToken.getBegin(), key, value);
      annotations.add(annotation);
    }

    return annotations;
  }

  private static final Argument parseArgument(final PEGLexer lexer) {
    // argument: INT | STRING ;

    lexer.assertPeek(PEGTokenKind.TK_INT, PEGTokenKind.TK_STRING);
    final PEGToken token = lexer.pop();

    if (token.kind == PEGTokenKind.TK_INT) {
      final int value = Integer.parseInt(token.string);
      return new IntArgument(token.getBegin(), value);
    } else {
      assert (token.kind == PEGTokenKind.TK_STRING);
      return new StringArgument(token.getBegin(), token.string);
    }
  }

  private static final ParserProduction parseParserProduction(final PEGLexer lexer,
      final List<Annotation> annotations) {
    // parser_production: parser_name ASSIGN alternatives TERMINATOR ;

    final ParserIdentifier parserName = parseParserName(lexer);

    lexer.assertPop(PEGTokenKind.TK_ASSIGN);

    final Alternatives alternatives = parseAlternatives(lexer);

    lexer.assertPop(PEGTokenKind.TK_TERMINATOR);

    return new ParserProduction(parserName.getPosition(), annotations, parserName, alternatives);
  }

  private static final ParserIdentifier parseParserName(final PEGLexer lexer) {
    // parser_name: PARSER_ID ;

    final PEGToken parserID = lexer.assertPop(PEGTokenKind.TK_PARSER_ID);

    final ParserIdentifier parserName =
        new ParserIdentifier(parserID.getBegin(), parserID.string);
    return parserName;
  }

  private static final LexerIdentifier parseLexerName(final PEGLexer lexer) {
    // lexer_name: LEXER_ID ;

    final PEGToken lexerID = lexer.assertPop(PEGTokenKind.TK_LEXER_ID);

    final LexerIdentifier lexerName =
        new LexerIdentifier(lexerID.getBegin(), lexerID.string);
    return lexerName;
  }

  private static final Alternatives parseAlternatives(final PEGLexer lexer) {
    // alternatives: sequence ( OR sequence )* ;

    final Alternatives alternatives = new Alternatives(lexer.getPosition());

    final Sequence firstAlternative = parseSequence(lexer);
    alternatives.addAlternative(firstAlternative);

    while (lexer.peek().kind == PEGTokenKind.TK_OR) {
      lexer.assertPop(PEGTokenKind.TK_OR);

      final Sequence nextAlternative = parseSequence(lexer);
      alternatives.addAlternative(nextAlternative);
    }

    return alternatives;
  }

  private static final Sequence parseSequence(final PEGLexer lexer) {
    // sequence: atom* ;

    final Sequence sequence = new Sequence(lexer.getPosition());

    PEGTokenKind peekKind;
    while ((peekKind = lexer.peek().kind) != PEGTokenKind.TK_TERMINATOR
        && peekKind != PEGTokenKind.TK_RPAREN
        && peekKind != PEGTokenKind.TK_OR) {
      final Atom nextElement = parseAtom(lexer);
      sequence.addElement(nextElement);
    }

    return sequence;
  }

  private static final Atom parseAtom(final PEGLexer lexer) {
    // atom: ( production_name | LPAREN alternatives RPAREN ) quantifier? ;

    lexer.assertPeek(PEGTokenKind.TK_PARSER_ID, PEGTokenKind.TK_LEXER_ID,
        PEGTokenKind.TK_LPAREN);

    final Atom atom;
    {
      final PEGTokenKind peekKind = lexer.peek().kind;

      if (peekKind == PEGTokenKind.TK_PARSER_ID || peekKind == PEGTokenKind.TK_LEXER_ID) {
        atom = parseProductionName(lexer);
      } else {
        lexer.assertPop(PEGTokenKind.TK_LPAREN);
      
        atom = parseAlternatives(lexer);

        lexer.assertPop(PEGTokenKind.TK_RPAREN);
      }
    }

    final Atom.Quantifier quantifier;
    {
      final PEGTokenKind peekKind = lexer.peek().kind;

      if (peekKind == PEGTokenKind.TK_OPTIONAL || peekKind == PEGTokenKind.TK_STAR
          || peekKind == PEGTokenKind.TK_PLUS) {
        quantifier = parseQuantifier(lexer);
      } else {
        quantifier = Atom.Quantifier.QUANT_NONE;
      }
    }

    atom.setQuantifier(quantifier);

    return atom;
  }

  private static final Identifier parseProductionName(final PEGLexer lexer) {
    // production_name: parser_name | lexer_name ;

    lexer.assertPeek(PEGTokenKind.TK_PARSER_ID, PEGTokenKind.TK_LEXER_ID);

    final PEGTokenKind peekKind = lexer.peek().kind;

    if (peekKind == PEGTokenKind.TK_PARSER_ID) {
      return parseParserName(lexer);
    } else {
      assert (peekKind == PEGTokenKind.TK_LEXER_ID);
      return parseLexerName(lexer);
    }
  }

  private static final Atom.Quantifier parseQuantifier(final PEGLexer lexer) {
    // quantifier: OPTIONAL | STAR | PLUS ;

    lexer.assertPeek(PEGTokenKind.TK_OPTIONAL, PEGTokenKind.TK_STAR, PEGTokenKind.TK_PLUS);

    final PEGTokenKind popKind = lexer.pop().kind;

    switch (popKind) {
      case TK_OPTIONAL: {
        return Atom.Quantifier.QUANT_OPTIONAL;
      }
      case TK_STAR: {
        return Atom.Quantifier.QUANT_STAR;
      }
      default: {
        assert (popKind == PEGTokenKind.TK_PLUS);
        return Atom.Quantifier.QUANT_PLUS;
      }
    }
  }

  private static final LexerProduction parseLexerProduction(final PEGLexer lexer,
      final List<Annotation> annotations) {
    // lexer_production: lexer_name ASSIGN regular_expression TERMINATOR ;

    final LexerIdentifier lexerName = parseLexerName(lexer);

    lexer.assertPop(PEGTokenKind.TK_ASSIGN);

    final RegularExpression regularExpression = parseRegularExpression(lexer);

    lexer.assertPop(PEGTokenKind.TK_TERMINATOR);

    return new LexerProduction(lexerName.getPosition(), annotations, lexerName, regularExpression);
  }

  public static final RegularExpression parseRegularExpression(final String input) {
    return parseRegularExpression(new PEGLexer(input));
  }

  private static final RegularExpression parseRegularExpression(final PEGLexer lexer) {
    // regular_expression: re_alternatives ;

    final Alternatives alternatives = parseREAlternatives(lexer);

    return new RegularExpression(alternatives.getPosition(), alternatives);
  }

  private static final Alternatives parseREAlternatives(final PEGLexer lexer) {
    // re_alternatives: re_sequence ( OR re_sequence )* ;

    final Alternatives alternatives = new Alternatives(lexer.getPosition());

    final Sequence firstAlternative = parseRESequence(lexer);
    alternatives.addAlternative(firstAlternative);

    while (lexer.peek().kind == PEGTokenKind.TK_OR) {
      lexer.assertPop(PEGTokenKind.TK_OR);

      final Sequence nextAlternative = parseRESequence(lexer);
      alternatives.addAlternative(nextAlternative);
    }

    return alternatives;
  }

  private static final Sequence parseRESequence(final PEGLexer lexer) {
    // re_sequence: re_atom+ ;

    final Sequence sequence = new Sequence(lexer.getPosition());

    final Atom firstElement = parseREAtom(lexer);
    sequence.addElement(firstElement);

    PEGTokenKind peekKind;
    while ((peekKind = lexer.peek().kind) != PEGTokenKind.TK_TERMINATOR
        && peekKind != PEGTokenKind.TK_RPAREN
        && peekKind != PEGTokenKind.TK_OR
        && peekKind != PEGTokenKind.TK_EOF) {
      final Atom nextElement = parseREAtom(lexer);
      sequence.addElement(nextElement);
    }

    return sequence;
  }

  private static final Atom parseREAtom(final PEGLexer lexer) {
    // re_atom: ( LPAREN re_alternatives RPAREN | re_group | re_literal ) quantifier? ;

    lexer.assertPeek(PEGTokenKind.TK_LPAREN, PEGTokenKind.TK_LBRACK, PEGTokenKind.TK_LITERAL);

    final Atom atom;
    {
      final PEGTokenKind peekKind = lexer.peek().kind;

      if (peekKind == PEGTokenKind.TK_LPAREN) {
        lexer.assertPop(PEGTokenKind.TK_LPAREN);

        atom = parseREAlternatives(lexer);

        lexer.assertPop(PEGTokenKind.TK_RPAREN);
      } else if (peekKind == PEGTokenKind.TK_LBRACK) {
        atom = parseGroup(lexer);
      } else {
        atom = parseLiteral(lexer);
      }
    }

    final Atom.Quantifier quantifier;
    {
      final PEGTokenKind peekKind = lexer.peek().kind;

      if (peekKind == PEGTokenKind.TK_OPTIONAL || peekKind == PEGTokenKind.TK_STAR
          || peekKind == PEGTokenKind.TK_PLUS) {
        quantifier = parseQuantifier(lexer);
      } else {
        quantifier = Atom.Quantifier.QUANT_NONE;
      }
    }

    atom.setQuantifier(quantifier);

    return atom;
  }

  private static final Group parseGroup(final PEGLexer lexer) {
    // group: LBRACK NEGATE? range+ RBRACK ;

    lexer.assertPop(PEGTokenKind.TK_LBRACK);

    lexer.assertNotPeek(PEGTokenKind.TK_EOF);
    final PEGToken nextToken = lexer.peek();

    final Group group;

    if (nextToken.kind == PEGTokenKind.TK_NEGATE) {
      lexer.assertPop(PEGTokenKind.TK_NEGATE);
      group = new Group(lexer.getPosition(), true);
    } else {
      group = new Group(lexer.getPosition(), false);
    }

    do {
      final Range range = parseRange(lexer);
      group.addRange(range);
    } while (lexer.peek().kind != PEGTokenKind.TK_RBRACK);

    lexer.assertPop(PEGTokenKind.TK_RBRACK);

    return group;
  }

  private static final Range parseRange(final PEGLexer lexer) {
    // range: plain_character ( DASH plain_character )? ;

    final SingleCharacter firstCharacter = parsePlainCharacter(lexer);

    if (lexer.skip(PEGTokenKind.TK_DASH)) {
      final SingleCharacter secondCharacter = parsePlainCharacter(lexer);
      return new CharacterRange(firstCharacter.getPosition(), firstCharacter, secondCharacter);
    } else {
      return firstCharacter;
    }
  }

  private static final SingleCharacter parsePlainCharacter(final PEGLexer lexer) {
    // plain_character: PLAIN_CHAR ;

    lexer.assertNotPeek(PEGTokenKind.TK_EOF);

    final PEGToken firstCharacter = lexer.pop(true);

    assert (firstCharacter.kind == PEGTokenKind.TK_PLAIN_CHAR);
    assert (firstCharacter.string.length() == 1);

    return new SingleCharacter(firstCharacter.getBegin(), firstCharacter.string.charAt(0));
  }

  private static final Literal parseLiteral(final PEGLexer lexer) {
    // re_literal: LITERAL ;

    final PEGToken literalToken = lexer.assertPop(PEGTokenKind.TK_LITERAL);

    return new Literal(literalToken.getBegin(), literalToken.string);
  }

}
