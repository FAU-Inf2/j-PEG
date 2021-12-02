package i2.act.util.options;

import java.util.LinkedHashMap;

public final class ProgramArgumentsParser {

  private final String positionalArgumentsName;

  private final LinkedHashMap<String, ProgramOption> allowedOptions;

  public ProgramArgumentsParser() {
    this(null);
  }

  public ProgramArgumentsParser(final String positionalArgumentsName) {
    this.positionalArgumentsName = positionalArgumentsName;
    this.allowedOptions = new LinkedHashMap<String, ProgramOption>();
  }

  private final boolean allowPositionalArguments() {
    return this.positionalArgumentsName != null;
  }

  private final void addOption(final ProgramOption option) {
    this.allowedOptions.put(option.optionKey, option);
  }

  public final void addOption(final String optionKey, final boolean required) {
    final ProgramOption option = new ProgramOption(optionKey, required, false, "");
    addOption(option);
  }

  public final void addOption(final String optionKey, final boolean required,
      final boolean takesArgument, final String argumentDescription) {
    final ProgramOption option =
        new ProgramOption(optionKey, required, takesArgument, argumentDescription);
    addOption(option);
  }

  public final String usage() {
    return usage("");
  }

  public final String usage(final String indentation) {
    final StringBuilder builder = new StringBuilder();

    int index = 0;
    final int numberOfAllowedOptions = this.allowedOptions.size();

    for (final ProgramOption option : this.allowedOptions.values()) {
      builder.append(indentation);

      if (!option.required) {
        builder.append('[');
      }

      builder.append(option.optionKey);

      if (option.takesArgument && option.argumentDescription != null
          && !option.argumentDescription.isEmpty()) {
        builder.append(' ');
        builder.append(option.argumentDescription);
      }

      if (!option.required) {
        builder.append(']');
      }

      if (index < numberOfAllowedOptions - 1) {
        builder.append('\n');
      }

      ++index;
    }

    if (allowPositionalArguments()) {
      if (numberOfAllowedOptions > 0) {
        builder.append('\n');
      }

      builder.append(indentation);
      builder.append(this.positionalArgumentsName);
    }

    return builder.toString();
  }

  private final void verifyThatRequiredOptionsWereSpecified(
      final ProgramArguments programArguments) {
    for (final ProgramOption option : this.allowedOptions.values()) {
      if (option.required) {
        programArguments.requireOption(option.optionKey);
      }
    }
  }

  public final ProgramArguments parseArgs(final String[] args) {
    final ProgramArguments programArguments = new ProgramArguments();

    int idx = 0;
    boolean reachedPositionalArgumentSeparator = false;

    while (idx < args.length) {
      final String optionKey = args[idx];

      if (allowPositionalArguments()
          && !reachedPositionalArgumentSeparator
          && "--".equals(optionKey)) {
        reachedPositionalArgumentSeparator = true;

        idx += 1;
        continue;
      }

      if (this.allowedOptions.containsKey(optionKey)) {
        final ProgramOption option = this.allowedOptions.get(optionKey);

        if (option.takesArgument) {
          if (idx == args.length - 1) {
            throw new InvalidProgramArgumentsException(
              String.format("missing value for option '%s'", optionKey));
          }

          final String argument = args[idx + 1];

          programArguments.addOption(optionKey, argument);

          idx += 2;
        } else {
          programArguments.addOption(optionKey);

          idx += 1;
        }
      } else {
        if (allowPositionalArguments()
            && (reachedPositionalArgumentSeparator || !optionKey.startsWith("-"))) {
          programArguments.addPositionalArgument(optionKey);

          idx += 1;
        } else {
          throw new InvalidProgramArgumentsException(
            String.format("invalid option '%s'", optionKey));
        }
      }
    }

    verifyThatRequiredOptionsWereSpecified(programArguments);

    return programArguments;
  }

}
