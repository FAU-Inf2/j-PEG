package i2.act.peg.ast;

import i2.act.peg.ast.visitors.ASTVisitor;
import i2.act.peg.info.SourcePosition;

public final class CharacterRange extends Range {

  private final SingleCharacter lowerCharacter;
  private final SingleCharacter upperCharacter;

  public CharacterRange(final SourcePosition position, final SingleCharacter lowerCharacter,
      final SingleCharacter upperCharacter) {
    super(position);

    this.lowerCharacter = lowerCharacter;
    this.upperCharacter = upperCharacter;
  }

  public final SingleCharacter getLowerCharacter() {
    return this.lowerCharacter;
  }

  public final SingleCharacter getUpperCharacter() {
    return this.upperCharacter;
  }

  @Override
  public final boolean inRange(final char character) {
    return character >= this.lowerCharacter.getValue()
        && character <= this.upperCharacter.getValue();
  }

  @Override
  public final CharacterRange clone(final boolean keepSymbols) {
    final SingleCharacter lowerCharacterClone = this.lowerCharacter.clone(keepSymbols);
    final SingleCharacter upperCharacterClone = this.upperCharacter.clone(keepSymbols);

    return new CharacterRange(this.position, lowerCharacterClone, upperCharacterClone);
  }

  @Override
  public final <P, R> R accept(final ASTVisitor<P, R> visitor, final P param) {
    return visitor.visit(this, param);
  }

}
