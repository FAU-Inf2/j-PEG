package i2.act.packrat.nfa;

import i2.act.peg.ast.Group;
import i2.act.peg.ast.Range;
import i2.act.peg.ast.visitors.PrettyPrinter;

public interface CharacterSet {

  public static final class SingleCharacter implements CharacterSet {

    private final char character;

    public SingleCharacter(final char character) {
      this.character = character;
    }

    public final char getCharacter() {
      return this.character;
    }

    @Override
    public final boolean matches(final char inputCharacter) {
      return this.character == inputCharacter;
    }

    @Override
    public final String toString() {
      return String.valueOf(this.character);
    }

  }

  public static final class CharacterGroup implements CharacterSet {

    private final Group group;

    public CharacterGroup(final Group group) {
      this.group = group;
    }

    public final Group getGroup() {
      return this.group;
    }

    @Override
    public final boolean matches(final char inputCharacter) {
      for (final Range range : this.group) {
        if (range.inRange(inputCharacter)) {
          return !this.group.isInverted();
        }
      }

      return this.group.isInverted();
    }

    @Override
    public final String toString() {
      return PrettyPrinter.prettyPrint(this.group);
    }

  }

  public boolean matches(final char character);

}
