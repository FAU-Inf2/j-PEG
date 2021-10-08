package i2.act.peg.ast;

import i2.act.peg.ast.visitors.ASTVisitor;
import i2.act.peg.info.SourcePosition;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public final class Group extends Atom implements Iterable<Range> {

  private final boolean inverted;
  private final List<Range> ranges;

  public Group(final SourcePosition position, final boolean inverted) {
    this(position, inverted, new ArrayList<Range>(), null);
  }

  private Group(final SourcePosition position, final boolean inverted, final List<Range> ranges, 
      final Quantifier quantifier) {
    super(position, quantifier);
    this.inverted = inverted;
    this.ranges = ranges;
  }

  public final boolean isInverted() {
    return this.inverted;
  }

  public final List<Range> getRanges() {
    return Collections.unmodifiableList(this.ranges);
  }

  public final void addRange(final Range range) {
    this.ranges.add(range);
  }

  @Override
  public final Iterator<Range> iterator() {
    return getRanges().iterator();
  }

  @Override
  public final Group clone(final boolean keepSymbols) {
    final List<Range> rangesClone = new ArrayList<>();
    {
      for (final Range range : this.ranges) {
        final Range rangeClone = range.clone(keepSymbols);
        rangesClone.add(rangeClone);
      }
    }

    return new Group(this.position, this.inverted, rangesClone, this.quantifier);
  }

  @Override
  public final <P, R> R accept(final ASTVisitor<P, R> visitor, final P param) {
    return visitor.visit(this, param);
  }

}
