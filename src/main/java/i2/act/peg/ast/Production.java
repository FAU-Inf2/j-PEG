package i2.act.peg.ast;

import i2.act.peg.info.SourcePosition;
import i2.act.peg.symbols.Symbol;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class Production<I extends Identifier<S>, S extends Symbol<?>> extends ASTNode {

  protected final List<Annotation> annotations;
  protected final I leftHandSide;

  public Production(final SourcePosition position, final List<Annotation> annotations,
      final I leftHandSide) {
    super(position);
    this.annotations = annotations;
    this.leftHandSide = leftHandSide;
  }

  public final List<Annotation> getAnnotations() {
    return Collections.unmodifiableList(this.annotations);
  }

  public final I getLeftHandSide() {
    return this.leftHandSide;
  }

  public final S getSymbol() {
    return this.leftHandSide.getSymbol();
  }

  public final boolean hasAnnotation(final String key) {
    for (final Annotation annotation : this.annotations) {
      if (key.equals(annotation.getKey())) {
        return true;
      }
    }

    return false;
  }

  public final Argument getAnnotationValue(final String key) {
    for (final Annotation annotation : this.annotations) {
      if (key.equals(annotation.getKey())) {
        return annotation.getValue();
      }
    }

    return null;
  }

  @Override
  public abstract Production clone(final boolean keepSymbols);

  public final List<Annotation> cloneAnnotations(final boolean keepSymbols) {
    final List<Annotation> annotationsClone = new ArrayList<>();
    for (final Annotation annotation : this.annotations) {
      final Annotation annotationClone = annotation.clone(keepSymbols);
      annotationsClone.add(annotationClone);
    }

    return annotationsClone;
  }

}
