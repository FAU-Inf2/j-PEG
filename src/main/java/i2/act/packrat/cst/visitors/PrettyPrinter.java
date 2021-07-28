package i2.act.packrat.cst.visitors;

import i2.act.packrat.Token;
import i2.act.packrat.cst.Node;
import i2.act.packrat.cst.TerminalNode;
import i2.act.peg.symbols.LexerSymbol;
import i2.act.util.SafeWriter;

public final class PrettyPrinter extends SyntaxTreeVisitor<SafeWriter, Void> {

  public static final void print(final Node<?> syntaxTree, final SafeWriter writer) {
    syntaxTree.accept(new PrettyPrinter(), writer);
    writer.flush();
  }

  @Override
  public final Void visit(final TerminalNode node, final SafeWriter writer) {
    for (final Token skippedTokenBefore : node.getToken().getSkippedTokensBefore()) {
      writer.write(skippedTokenBefore.getValue());
    }

    if (node.getSymbol() != LexerSymbol.EOF) {
      writer.write(node.getToken().getValue());
    }

    return null;
  }

}
