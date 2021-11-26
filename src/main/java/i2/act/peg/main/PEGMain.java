package i2.act.peg.main;

import i2.act.grammargraph.GrammarGraph;
import i2.act.packrat.Lexer;
import i2.act.packrat.Parser;
import i2.act.packrat.TokenStream;
import i2.act.packrat.cst.Node;
import i2.act.packrat.cst.NonTerminalNode;
import i2.act.packrat.cst.TerminalNode;
import i2.act.packrat.cst.visitors.DotGenerator;
import i2.act.packrat.cst.visitors.LaTeXGenerator;
import i2.act.packrat.cst.visitors.PrettyPrinter;
import i2.act.packrat.cst.visitors.SyntaxTreeVisitor;
import i2.act.peg.ast.Grammar;
import i2.act.peg.ast.visitors.NameAnalysis;
import i2.act.peg.parser.PEGParser;
import i2.act.util.SafeWriter;
import i2.act.util.options.ProgramArguments;
import i2.act.util.options.ProgramArgumentsParser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public final class PEGMain {

  private static final ProgramArgumentsParser argumentsParser;

  private static final String OPTION_INPUT = "--in";
  private static final String OPTION_GRAMMAR = "--grammar";

  private static final String OPTION_PRETTY_PRINT_GRAMMAR = "--prettyPrintGrammar";

  private static final String OPTION_OMIT_QUANTIFIERS = "--omitQuantifiers";
  private static final String OPTION_NO_COMPACTIFY = "--noCompactify";

  private static final String OPTION_PRETTY_PRINT_IN = "--prettyPrintIn";
  private static final String OPTION_TO_DOT = "--toDot";
  private static final String OPTION_TO_LATEX = "--toLaTeX";
  private static final String OPTION_PRINT_GRAMMAR_GRAPH = "--printGG";

  private static final String OPTION_TREE_STATS = "--treeStats";
  private static final String OPTION_PARSER_STATS = "--parserStats";

  private static final String OPTION_HIGHLIGHT_ANNOTATIONS = "--annotations";

  static {
    argumentsParser = new ProgramArgumentsParser();

    argumentsParser.addOption(OPTION_INPUT, true, true, "<file name of input program>");
    argumentsParser.addOption(OPTION_GRAMMAR, true, true, "<file name of grammar>");

    argumentsParser.addOption(OPTION_PRETTY_PRINT_GRAMMAR, false);

    argumentsParser.addOption(OPTION_OMIT_QUANTIFIERS, false);
    argumentsParser.addOption(OPTION_NO_COMPACTIFY, false);

    argumentsParser.addOption(OPTION_PRETTY_PRINT_IN, false);
    argumentsParser.addOption(OPTION_TO_DOT, false);
    argumentsParser.addOption(OPTION_TO_LATEX, false);
    argumentsParser.addOption(OPTION_PRINT_GRAMMAR_GRAPH, false);

    argumentsParser.addOption(OPTION_TREE_STATS, false);
    argumentsParser.addOption(OPTION_PARSER_STATS, false);

    argumentsParser.addOption(OPTION_HIGHLIGHT_ANNOTATIONS, false);
  }

  // ===============================================================================================

  private static final class DotStyleAnnotations extends DotGenerator.BaseDotStyle {

    @Override
    public String styleNode(final Node<?> node) {
      if (node.hasAnnotation()) {
        return super.styleNode(node) + "style=filled, fillcolor=lightgoldenrod1";
      } else {
        return super.styleNode(node);
      }
    }

  }

  // ===============================================================================================

  private static final void usage() {
    System.err.format("USAGE: java %s\n", PEGMain.class.getSimpleName());
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

    final boolean quantifierNodes = !arguments.hasOption(OPTION_OMIT_QUANTIFIERS);
    final boolean compactifyTree = !arguments.hasOption(OPTION_NO_COMPACTIFY);

    final boolean highlightAnnotations = arguments.hasOption(OPTION_HIGHLIGHT_ANNOTATIONS);

    final Grammar grammar = readGrammar(fileNameGrammar);

    if (arguments.hasOption(OPTION_PRETTY_PRINT_GRAMMAR)) {
      i2.act.peg.ast.visitors.PrettyPrinter.prettyPrint(grammar, SafeWriter.openStdOut());
    }

    final Lexer lexer = Lexer.forGrammar(grammar);
    final Parser parser = Parser.fromGrammar(grammar, quantifierNodes);

    if (arguments.hasOption(OPTION_PRINT_GRAMMAR_GRAPH)) {
      final GrammarGraph grammarGraph = GrammarGraph.fromGrammar(grammar);
      grammarGraph.printAsDot();
    }

    try {
      final String input = readFile(fileNameInput);

      final long timeBeforeLexer = System.currentTimeMillis();
      final TokenStream tokens = lexer.lex(input);
      final long timeAfterLexer = System.currentTimeMillis();

      final long timeBeforeParser = System.currentTimeMillis();
      final Node<?> syntaxTree = parser.parse(tokens);
      final long timeAfterParser = System.currentTimeMillis();

      if (arguments.hasOption(OPTION_PARSER_STATS)) {
        final long lexerTime = timeAfterLexer - timeBeforeLexer;
        final long parserTime = timeAfterParser - timeAfterLexer;

        System.err.format("[i] time in lexer:  %6d ms\n", lexerTime);
        System.err.format("[i] time in parser: %6d ms\n", parserTime);
        System.err.format("[i] total time:     %6d ms\n", lexerTime + parserTime);
      }

      if (compactifyTree) {
        syntaxTree.compactify();
      }

      if (arguments.hasOption(OPTION_PRETTY_PRINT_IN)) {
        PrettyPrinter.print(syntaxTree, SafeWriter.openStdOut());
      }

      if (arguments.hasOption(OPTION_TO_DOT)) {
        final DotGenerator.DotStyle style;
        {
          if (highlightAnnotations) {
            style = new DotStyleAnnotations();
          } else {
            style = DotGenerator.DEFAULT_DOT_STYLE;
          }
        }

        DotGenerator.print(syntaxTree, style);
      }

      if (arguments.hasOption(OPTION_TO_LATEX)) {
        LaTeXGenerator.print(syntaxTree);
      }

      if (arguments.hasOption(OPTION_TREE_STATS)) {
        printStats(syntaxTree);
      }
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

  private static final void printStats(final Node<?> syntaxTree) {
    final int[] terminalCounter = { 0 };
    final int[] nonTerminalCounter = { 0 };
    final int[] nonTerminalCounterAll = { 0 };

    final int[] quantifierCounter = { 0 };
    final int[] itemCounter = { 0 };
    final int[] singleElementCounter = { 0 };

    syntaxTree.accept(new SyntaxTreeVisitor<Void, Void>() {

      @Override
      public final Void visit(final NonTerminalNode node, final Void parameter) {
        ++nonTerminalCounterAll[0];

        if (!node.isAuxiliaryNode()) {
          ++nonTerminalCounter[0];
        }

        if (node.isQuantifierNode()) {
          ++quantifierCounter[0];

          if (node.numberOfChildren() == 1) {
            ++singleElementCounter[0];
          }
        }

        if (node.isListItemNode()) {
          ++itemCounter[0];
        }

        return super.visit(node, parameter);
      }

      @Override
      public final Void visit(final TerminalNode node, final Void parameter) {
        ++terminalCounter[0];
        return super.visit(node, parameter);
      }

    }, null);

    System.err.format("[i] number of     terminals:          %7d\n", terminalCounter[0]);
    System.err.format("[i] number of non-terminals:          %7d\n", nonTerminalCounter[0]);
    System.err.format("[i] number of non-terminals (w/ aux): %7d\n", nonTerminalCounterAll[0]);
    System.err.format("[i] number of quantifiers:            %7d\n", quantifierCounter[0]);
    System.err.format("[i] number of quantifiers w/ 1 item:  %7d\n", singleElementCounter[0]);
    System.err.format("[i] number of list items:             %7d\n", itemCounter[0]);
  }

}
