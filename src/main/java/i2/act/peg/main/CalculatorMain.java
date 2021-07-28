package i2.act.peg.main;

import i2.act.packrat.*;
import i2.act.packrat.cst.Node;
import i2.act.packrat.cst.visitors.TreeVisitor;
import i2.act.peg.ast.Grammar;
import i2.act.peg.builder.GrammarBuilder;
import i2.act.peg.symbols.LexerSymbol;
import i2.act.peg.symbols.ParserSymbol;
import i2.act.util.SafeWriter;

import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import static i2.act.peg.builder.GrammarBuilder.*;

public final class CalculatorMain {

  public static final void main(final String[] args) {
    final GrammarBuilder builder = new GrammarBuilder();

    final LexerSymbol ASSIGN = builder.define("ASSIGN", "':='");
    final LexerSymbol SEMICOLON = builder.define("SEMICOLON", "';'");
    final LexerSymbol ADD = builder.define("ADD", "'+'");
    final LexerSymbol SUB = builder.define("SUB", "'-'");
    final LexerSymbol MUL = builder.define("MUL", "'*'");
    final LexerSymbol DIV = builder.define("DIV", "'/'");
    final LexerSymbol LPAREN = builder.define("LPAREN", "'('");
    final LexerSymbol RPAREN = builder.define("RPAREN", "')'");
    final LexerSymbol SPACE = builder.define("SPACE", "( ' ' | '\\n' | '\\r' | '\\t' )+", true);
    final LexerSymbol NUM = builder.define("NUM", "'0' | ( [1-9][0-9]* )");
    final LexerSymbol VAR_NAME = builder.define("VAR_NAME", "[a-zA-Z] [a-zA-Z0-9]*");

    final ParserSymbol calculation = builder.declare("calculation");
    final ParserSymbol statement = builder.declare("statement");
    final ParserSymbol expression = builder.declare("expression");
    final ParserSymbol add_expression = builder.declare("add_expression");
    final ParserSymbol mul_expression = builder.declare("mul_expression");
    final ParserSymbol factor = builder.declare("factor");

    builder.define(calculation,
        seq(many(statement), LexerSymbol.EOF));

    builder.define(statement,
        seq(opt(seq(VAR_NAME, ASSIGN)), expression, opt(SEMICOLON)));

    builder.define(expression,
        add_expression);

    builder.define(add_expression,
        alt(seq(add_expression, alt(ADD, SUB), mul_expression),
            mul_expression));

    builder.define(mul_expression,
        alt(seq(mul_expression, alt(MUL, DIV), factor),
            factor));

    builder.define(factor,
        alt(NUM, VAR_NAME, seq(LPAREN, expression, RPAREN)));

    final Grammar grammar = builder.build();

    i2.act.peg.ast.visitors.PrettyPrinter.prettyPrint(grammar, SafeWriter.openStdOut());

    final TreeVisitor<Map<String, Integer>, Integer> visitor =
        TreeVisitor.<Map<String, Integer>, Integer>leftToRight();

    // NUM
    visitor.add(NUM, (node, values) -> {
      final String numberValue = node.getText();
      return Integer.parseInt(numberValue);
    });

    // VAR_NAME
    visitor.add(VAR_NAME, (node, values) -> {
      final String varName = node.getText();

      if (values.containsKey(varName)) {
        return values.get(varName);
      } else {
        return 0;
      }
    });

    // add_expression
    visitor.add(add_expression, (node, values) -> {
      if (node.numberOfChildren() == 1) {
        return visitor.visit(node.getChild(0), values);
      } else {
        assert (node.numberOfChildren() == 3);

        final int leftValue = visitor.visit(node.getChild(0), values);
        final int rightValue = visitor.visit(node.getChild(2), values);

        if (node.getChild(1).getSymbol() == ADD) {
          return leftValue + rightValue;
        } else {
          assert (node.getChild(1).getSymbol() == SUB);
          return leftValue - rightValue;
        }
      }
    });

    // mul_expression
    visitor.add(mul_expression, (node, values) -> {
      if (node.numberOfChildren() == 1) {
        return visitor.visit(node.getChild(0), values);
      } else {
        assert (node.numberOfChildren() == 3);

        final int leftValue = visitor.visit(node.getChild(0), values);
        final int rightValue = visitor.visit(node.getChild(2), values);

        if (node.getChild(1).getSymbol() == MUL) {
          return leftValue * rightValue;
        } else {
          assert (node.getChild(1).getSymbol() == DIV);
          return leftValue / rightValue;
        }
      }
    });

    // factor
    visitor.add(factor, (node, values) -> {
      if (node.numberOfChildren() == 1) {
        return visitor.visit(node.getChild(0), values);
      } else {
        assert (node.numberOfChildren() == 3);
        return visitor.visit(node.getChild(1), values);
      }
    });

    // statement
    visitor.add(statement, (node, values) -> {
      final Integer result = visitor.visit(node.getChild(expression), values);

      if (node.hasChild(VAR_NAME)) {
        final String varName = node.getChild(VAR_NAME).getText();
        values.put(varName, result);
      }

      return result;
    });

    // calculation
    visitor.add(calculation, (node, values) -> {
      Integer result = 0;
      for (final Node<?> statementNode : node.getChildren(statement)) {
        result = visitor.visit(statementNode, values);
      }
      return result;
    });

    final Map<String, Integer> values = new HashMap<>();

    System.out.println("\n======================================\n");

    final Lexer lexer = Lexer.forGrammar(grammar);
    final Parser parser = Parser.fromGrammar(grammar, calculation, false);

    System.out.print("=> ");

    final Scanner scanner = new Scanner(System.in);
    while (scanner.hasNextLine()) {
      final String input = scanner.nextLine();

      try {
        final TokenStream tokens = lexer.lex(input);
        final Node syntaxTree = parser.parse(tokens);

        System.out.println(visitor.visit(syntaxTree, values));
      } catch (final Exception exception) {
        System.err.format("[!] %s\n", exception.getMessage());
      }

      System.out.print("=> ");
    }
  }

}
