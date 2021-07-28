package i2.act.peg.main;

import i2.act.packrat.Lexer;
import i2.act.packrat.Parser;
import i2.act.packrat.TokenStream;
import i2.act.packrat.cst.Node;
import i2.act.packrat.cst.metrics.Halstead;
import i2.act.peg.ast.Grammar;
import i2.act.peg.ast.visitors.NameAnalysis;
import i2.act.peg.parser.PEGParser;
import i2.act.util.options.ProgramArguments;
import i2.act.util.options.ProgramArgumentsParser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public final class HalsteadMain {

  private static final ProgramArgumentsParser argumentsParser;

  private static final String OPTION_INPUT = "--in";
  private static final String OPTION_GRAMMAR = "--grammar";

  static {
    argumentsParser = new ProgramArgumentsParser();

    argumentsParser.addOption(OPTION_INPUT, true, true, "<file name of input program>");
    argumentsParser.addOption(OPTION_GRAMMAR, true, true, "<file name of grammar>");
  }

  private static final void usage() {
    System.err.format("USAGE: java %s\n", HalsteadMain.class.getSimpleName());
    System.err.println(argumentsParser.usage("  "));
  }

  private static final void abort(final String formatString, final Object... formatArguments) {
    System.err.format(formatString + "\n", formatArguments);
    usage();
    System.exit(1);
  }

  public static final void main(final String[] args) {
    ProgramArguments arguments = null;
  
    try {
      arguments = argumentsParser.parseArgs(args);
    } catch (final Exception exception) {
      abort("[!] %s", exception.getMessage());
    }

    assert (arguments != null);

    final String fileNameInput = arguments.getOption(OPTION_INPUT);
    final String fileNameGrammar = arguments.getOption(OPTION_GRAMMAR);

    final Grammar grammar = readGrammar(fileNameGrammar);

    final Lexer lexer = Lexer.forGrammar(grammar);
    final Parser parser = Parser.fromGrammar(grammar, true);

    try {
      final String input = readFile(fileNameInput);

      final TokenStream tokens = lexer.lex(input);

      final Node<?> syntaxTree = parser.parse(tokens);

      final Halstead.Result result = Halstead.compute(syntaxTree, grammar);
      System.out.println(result);
    } catch (final Exception exception) {
      abort("[!] %s", exception.getMessage());
    }
  }

  private static final Grammar readGrammar(final String grammarPath) {
    final String grammarInput = readFile(grammarPath);
    final Grammar grammar = PEGParser.parse(grammarInput);
    NameAnalysis.analyze(grammar);

    return grammar;
  }

  private static final String readFile(final String fileName) {
    try {
      byte[] bytes = Files.readAllBytes(Paths.get(fileName));
      return new String(bytes);
    } catch (final IOException exception) {
      throw new RuntimeException("unable to read file", exception);
    }
  }

}
