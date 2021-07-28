package i2.act.packrat.nfa;

public final class Transition {

  public static final Transition epsilonTransition(final NFAState to) {
    return new Transition(null, to);
  }

  public static final Transition characterTransition(final CharacterSet characters,
      final NFAState to) {
    return new Transition(characters, to);
  }

  //------------------------------------------------------------------------------------------------

  private final CharacterSet characters;
  private final NFAState to;

  private Transition(final CharacterSet characters, final NFAState to) {
    this.characters = characters;
    this.to = to;
  }

  public final boolean isEpsilonTransition() {
    return this.characters == null;
  }

  public final CharacterSet getCharacters() {
    return this.characters;
  }

  public final NFAState getTo() {
    return this.to;
  }

  public final boolean matches(final char character) {
    if (isEpsilonTransition()) {
      return true;
    }
    return this.characters.matches(character);
  }

}
