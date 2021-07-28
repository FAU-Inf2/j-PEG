package i2.act.packrat.cst.metrics;

import i2.act.packrat.cst.Node;
import i2.act.packrat.cst.TerminalNode;
import i2.act.packrat.cst.visitors.SyntaxTreeVisitor;
import i2.act.packrat.nfa.NFA;
import i2.act.peg.ast.Grammar;
import i2.act.peg.ast.LexerProduction;
import i2.act.peg.symbols.LexerSymbol;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class Halstead {

  public static final String ANNOTATION_OPERAND = "operand";
  public static final String ANNOTATION_OPERATOR = "operator";

  public static final class Result {
    
    private final int numberOfDistinctOperands;
    private final int numberOfDistinctOperators;

    private final int vocabulary;

    private final int numberOfOperands;
    private final int numberOfOperators;

    private final int length;

    private final double estimatedLength;

    private final double volume;

    private final double difficulty;

    private final double effort;

    private final double codingTime;

    private final double deliveredBugs;
  
    public Result(final int numberOfDistinctOperands, final int numberOfDistinctOperators,
        final int numberOfOperands, final int numberOfOperators) {
      this.numberOfDistinctOperands = numberOfDistinctOperands;
      this.numberOfDistinctOperators = numberOfDistinctOperators;

      this.numberOfOperands = numberOfOperands;
      this.numberOfOperators = numberOfOperators;

      this.vocabulary = numberOfDistinctOperands + numberOfDistinctOperators;
      this.length = numberOfOperands + numberOfOperators;

      this.estimatedLength =
          numberOfDistinctOperands * log2(numberOfDistinctOperands)
          + numberOfDistinctOperators * log2(numberOfDistinctOperators);

      this.volume = this.length * log2(this.vocabulary);

      this.difficulty =
          (numberOfDistinctOperators / 2.0)
          * (((double) numberOfOperands) / numberOfDistinctOperands);

      this.effort = this.difficulty * this.volume;

      this.codingTime = this.effort / 18.0;

      this.deliveredBugs = Math.pow(this.effort, 2.0 / 3.0) / 3000.0;
    }

    @Override
    public final String toString() {
      final StringBuilder builder = new StringBuilder();

      builder.append("{\n");

      builder.append(
          String.format("  \"distinct operands\": %d,\n", this.numberOfDistinctOperands));
      builder.append(
          String.format("  \"distinct operators\": %d,\n", this.numberOfDistinctOperators));
      builder.append(String.format("  \"vocabulary\": %d,\n", this.vocabulary));

      builder.append(String.format("  \"operands\": %d,\n", this.numberOfOperands));
      builder.append(String.format("  \"operators\": %d,\n", this.numberOfOperators));
      builder.append(String.format("  \"length\": %d,\n", this.length));

      builder.append(
          String.format(Locale.US,"  \"estimated length\": %.2f,\n", this.estimatedLength));

      builder.append(String.format(Locale.US, "  \"volume\": %.2f,\n", this.volume));
      builder.append(String.format(Locale.US, "  \"difficulty\": %.2f,\n", this.difficulty));
      builder.append(String.format(Locale.US, "  \"effort\": %.2f,\n", this.effort));

      builder.append(String.format(Locale.US, "  \"coding time\": %.2f,\n", this.codingTime));
      builder.append(String.format(Locale.US, "  \"bugs\": %.2f\n", this.deliveredBugs));

      builder.append("}");

      return builder.toString();
    }

  }

  public static final Result compute(final Node<?> program, final Grammar grammar) {
    final Set<LexerSymbol> operandSymbols = new HashSet<>();
    final Set<LexerSymbol> operatorSymbols = new HashSet<>();
    
    final List<LexerProduction> lexerProductions = grammar.getLexerProductions();
    for (final LexerProduction lexerProduction : lexerProductions) {
      final LexerSymbol symbol = lexerProduction.getSymbol();
      assert (symbol != null);

      if (isOperand(lexerProduction)) {
        operandSymbols.add(symbol);
      } else {
        assert (isOperator(lexerProduction));
        operatorSymbols.add(symbol);
      }
    }

    final Set<String> distinctOperands = new HashSet<>();
    final Set<LexerSymbol> distinctOperators = new HashSet<>();

    final int[] numberOfOperandsContainer = {0};
    final int[] numberOfOperatorsContainer = {0};

    program.accept(new SyntaxTreeVisitor<Void, Void>() {
      
      @Override
      public final Void visit(final TerminalNode terminalNode, final Void parameter) {
        final LexerSymbol symbol = terminalNode.getSymbol();
        assert (symbol != null);

        if (operandSymbols.contains(symbol)) {
          final String terminalValue = terminalNode.getToken().getValue();

          distinctOperands.add(terminalValue);
          ++numberOfOperandsContainer[0];
        } else if (operatorSymbols.contains(symbol)) {
          distinctOperators.add(symbol);
          ++numberOfOperatorsContainer[0];
        }

        return null;
      }

    }, null);

    final int numberOfDistinctOperands = distinctOperands.size();
    final int numberOfDistinctOperators = distinctOperators.size();
    
    final int numberOfOperands = numberOfOperandsContainer[0];
    final int numberOfOperators = numberOfOperatorsContainer[0];

    final Result result = new Result(numberOfDistinctOperands, numberOfDistinctOperators,
        numberOfOperands, numberOfOperators);

    return result;
  }

  private static final boolean isOperand(final LexerProduction lexerProduction) {
    if (lexerProduction.hasAnnotation(ANNOTATION_OPERAND)) {
      return true;
    }

    if (lexerProduction.hasAnnotation(ANNOTATION_OPERATOR)) {
      return false;
    }

    // if there is no manual annotation, we assume that a terminal is an operand if its regular
    // expression is not just a literal string
    return !NFA.fromRegularExpression(lexerProduction.getRegularExpression()).hasLiteralString();
  }

  private static final boolean isOperator(final LexerProduction lexerProduction) {
    return !isOperand(lexerProduction);
  }

  private static final double log2(final double value) {
    return Math.log(value) / Math.log(2);
  }

}
