package i2.act.packrat;

import i2.act.packrat.cst.Node;

import java.util.ArrayList;
import java.util.List;

public final class ParserSuccess extends ParserResult {

  public final int position;

  public List<Node<?>> syntaxTrees;

  public ParserSuccess(final int position) {
    this.position = position;
    this.syntaxTrees = new ArrayList<Node<?>>();
  }

  public ParserSuccess(final int position, final Node<?> syntaxTree) {
    this.position = position;
    this.syntaxTrees = new ArrayList<Node<?>>();
    this.syntaxTrees.add(syntaxTree);
  }

  public ParserSuccess(final int position, final List<Node<?>> syntaxTrees) {
    this.position = position;
    this.syntaxTrees = syntaxTrees;
  }

  @Override
  public final String toString() {
    return String.format("Success(%d)", this.position);
  }

}
