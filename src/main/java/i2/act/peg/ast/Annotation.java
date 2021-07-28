package i2.act.peg.ast;

import i2.act.peg.ast.visitors.ASTVisitor;
import i2.act.peg.info.SourcePosition;

public final class Annotation extends ASTNode {

  public static final String SKIP = "skip";

  private final String key;
  private final Argument value;

  public Annotation(final SourcePosition position, final String key) {
    this(position, key, null);
  }

  public Annotation(final SourcePosition position, final String key, final Argument value) {
    super(position);
    this.key = key;
    this.value = value;
  }

  public final String getKey() {
    return this.key;
  }

  public final boolean hasValue() {
    return this.value != null;
  }

  public final Argument getValue() {
    return this.value;
  }

  @Override
  public final Annotation clone(final boolean keepSymbols) {
    final Argument valueClone;
    {
      if (this.value != null) {
        valueClone = this.value.clone(keepSymbols);
      } else {
        valueClone = null;
      }
    }

    return new Annotation(this.position, this.key, valueClone);
  }

  @Override
  public final <P, R> R accept(final ASTVisitor<P, R> visitor, final P param) {
    return visitor.visit(this, param);
  }

}
