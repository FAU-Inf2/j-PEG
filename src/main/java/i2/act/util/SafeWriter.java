package i2.act.util;

import java.io.*;

public final class SafeWriter {

  private final BufferedWriter writer;

  private SafeWriter(final BufferedWriter writer) {
    this.writer = writer;
  }

  public static final SafeWriter fromBufferedWriter(final BufferedWriter writer) {
    return new SafeWriter(writer);
  }

  public static final SafeWriter openStdOut() {
    try {
      final BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(System.out));
      return new SafeWriter(writer);
    } catch (final Exception exception) {
      throw new RuntimeException(exception);
    }
  }

  public static final SafeWriter openFile(final String fileName) {
    if ("-".equals(fileName)) {
      return openStdOut();
    } else {
      final File file = new File(fileName);
      return openFile(file);
    }
  }

  public static final SafeWriter openFile(final File file) {
    try {
      final BufferedWriter writer = new BufferedWriter(new FileWriter(file));
      return new SafeWriter(writer);
    } catch (final IOException exception) {
      throw new RuntimeException(exception);
    }
  }

  public final SafeWriter write(final String string) {
    try {
      this.writer.write(string);
    } catch (final IOException exception) {
      throw new RuntimeException(exception);
    }

    return this;
  }

  public final SafeWriter write(final Object object) {
    return write(object == null ? "null" : object.toString());
  }

  public final SafeWriter write(final String formatString, final Object... formatArguments) {
    return write(String.format(formatString, formatArguments));
  }

  public final void flush() {
    try {
      this.writer.flush();
    } catch (final IOException exception) {
      throw new RuntimeException(exception);
    }
  }

  public final void close() {
    try {
      this.writer.close();
    } catch (final IOException exception) {
      throw new RuntimeException(exception);
    }
  }

}
