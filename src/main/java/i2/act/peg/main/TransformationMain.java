package i2.act.peg.main;

import i2.act.peg.ast.Grammar;
import i2.act.peg.ast.visitors.NameAnalysis;
import i2.act.peg.ast.visitors.PrettyPrinter;
import i2.act.peg.parser.PEGParser;
import i2.act.peg.transformations.GrammarTransformation;
import i2.act.peg.transformations.GrammarTransformationFactory;
import i2.act.util.SafeWriter;
import i2.act.util.options.ProgramArguments;
import i2.act.util.options.ProgramArgumentsParser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class TransformationMain {

  private static final ProgramArgumentsParser argumentsParser;

  private static final String OPTION_INPUT_GRAMMAR = "--in";
  private static final String OPTION_OUTPUT_GRAMMAR = "--out";

  private static final String OPTION_TRANSFORMATIONS = "--transformations";

  static {
    argumentsParser = new ProgramArgumentsParser();

    argumentsParser.addOption(OPTION_INPUT_GRAMMAR, true, true, "<file name of input grammar>");
    argumentsParser.addOption(OPTION_OUTPUT_GRAMMAR, true, true, "<file name of output grammar>");

    argumentsParser.addOption(OPTION_TRANSFORMATIONS, true, true,
        "<comma-separated list of transformations>");
  }

  private static final void usage() {
    System.err.format("USAGE: java %s\n", TransformationMain.class.getSimpleName());
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

    final String fileNameInput = arguments.getOption(OPTION_INPUT_GRAMMAR);
    final String fileNameOutput = arguments.getOption(OPTION_OUTPUT_GRAMMAR);

    final String transformationsOption = arguments.getOption(OPTION_TRANSFORMATIONS);
    final List<GrammarTransformation> transformations = getTransformations(transformationsOption);

    final Grammar grammar = readGrammar(fileNameInput);

    Grammar transformedGrammar = grammar;
    for (final GrammarTransformation transformation : transformations) {
      transformedGrammar = transformation.apply(transformedGrammar);
    }

    final SafeWriter outputWriter = SafeWriter.openFile(fileNameOutput);
    PrettyPrinter.prettyPrint(transformedGrammar, outputWriter);
    outputWriter.close();
  }

  private static final List<GrammarTransformation> getTransformations(
      final String transformationsOption) {
    final List<GrammarTransformation> transformations = new ArrayList<>();

    Arrays.stream(transformationsOption.split(",")).map(String::trim)
        .forEach(transformationName -> {
          final GrammarTransformationFactory factory =
              GrammarTransformationFactory.fromName(transformationName);

          if (factory == null) {
            abort("invalid transformation name '%s'", transformationName);
          } else {
            transformations.add(factory.createTransformation());
          }
        });

    return transformations;
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
