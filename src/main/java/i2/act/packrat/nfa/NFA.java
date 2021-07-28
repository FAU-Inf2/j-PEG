package i2.act.packrat.nfa;

import i2.act.peg.ast.*;
import i2.act.peg.ast.visitors.BaseASTVisitor;
import i2.act.peg.ast.visitors.PrettyPrinter;
import i2.act.util.SafeWriter;

import java.util.*;

public final class NFA {

  private final NFAState startState;
  private final Set<NFAState> acceptingStates;

  private final String literalString;

  private Set<CharacterSet> firstCharacters; // computed lazily

  private NFA(final NFAState startState) {
    this(startState, null);
  }

  private NFA(final NFAState startState, final String literalString) {
    this.startState = startState;
    this.acceptingStates = new HashSet<NFAState>();
    this.literalString = literalString;
  }

  public final boolean hasLiteralString() {
    return this.literalString != null;
  }

  public final String getLiteralString() {
    return this.literalString;
  }

  public final NFAState getStartState() {
    return this.startState;
  }

  public final Set<NFAState> getAcceptingStates() {
    // NOTE: using an unmodifiable set would be appropriate but this introduces unnecessary
    // overhead...
    return this.acceptingStates;
  }

  private final void addAcceptingState(final NFAState acceptingState) {
    this.acceptingStates.add(acceptingState);
  }

  public final void toDot(final SafeWriter writer) {
    writer.write("digraph {\n");
    writer.write("  rankdir=\"LR\";\n");

    final String startStateName = toDot(this.startState, new HashMap<NFAState, String>(), writer);

    writer.write("  start [label=\"\", shape=point];\n");
    writer.write("  start -> %s\n", startStateName);

    writer.write("}\n");
    writer.flush();
  }

  private final String toDot(final NFAState state, final Map<NFAState, String> names,
      final SafeWriter writer) {
    if (names.containsKey(state)) {
      return names.get(state);
    }

    final String stateName = String.format("state%d", names.size());
    names.put(state, stateName);

    final String style;
    {
      if (this.acceptingStates.contains(state)) {
        style = "shape=doublecircle";
      } else {
        style = "shape=circle";
      }
    }

    writer.write("  %s [label=\"\", %s];\n", stateName, style);

    for (final Transition transition : state) {
      final String label;
      {
        if (transition.isEpsilonTransition()) {
          label = "ε";
        } else {
          label = transition.getCharacters().toString().replace("\"", "\\\"");
        }
      }

      final NFAState toState = transition.getTo();

      final String toStateName = toDot(toState, names, writer);
      writer.write("  %s -> %s [label=\"%s\"];\n", stateName, toStateName, label);
    }

    return stateName;
  }

  public final boolean matches(final String input) {
    return prefixMatch(input) == input.length();
  }

  public final int prefixMatch(final String input) {
    return prefixMatch(input.toCharArray(), 0);
  }

  public final int prefixMatch(final char[] input, final int offset) {
    if (this.literalString != null) {
      final int literalStringLength = this.literalString.length();

      if (offset + literalStringLength > input.length) {
        return 0;
      }

      for (int index = 0; index < literalStringLength; ++index) {
        if (input[offset + index] != this.literalString.charAt(index)) {
          return 0;
        }
      }

      return literalStringLength;
    } else {
      final char firstChar = input[offset];
      if (!matchesFirstCharacter(firstChar)) {
        return 0;
      }

      Set<NFAState> currentStates = new HashSet<>();
      currentStates.addAll(this.startState.epsilonClosure(this.acceptingStates));

      int longestMatch = 0;

      for (int index = offset; index < input.length; ++index) {
        final char nextChar = input[index];
        currentStates = nextStates(currentStates, nextChar);

        if (currentStates.isEmpty()) {
          break;
        }

        if (containsAcceptingState(currentStates)) {
          longestMatch = index - offset + 1;
        }
      }

      return longestMatch;
    }
  }

  private final boolean containsAcceptingState(final Set<NFAState> states) {
    for (final NFAState state : states) {
      if (this.acceptingStates.contains(state)) {
        return true;
      }
    }

    return false;
  }

  private final Set<NFAState> nextStates(final Set<NFAState> currentStates, final char nextChar) {
    final Set<NFAState> nextStates = new HashSet<>();

    for (final NFAState state : currentStates) {
      for (final Transition transition : state) {
        if (transition.isEpsilonTransition()) {
          continue;
        }

        if (transition.matches(nextChar)) {
          final NFAState nextState = transition.getTo();
          nextStates.addAll(nextState.epsilonClosure(this.acceptingStates));
        }
      }
    }

    return nextStates;
  }

  private final boolean matchesFirstCharacter(final char firstCharacter) {
    for (final CharacterSet characterSet : getFirstCharacters()) {
      if (characterSet.matches(firstCharacter)) {
        return true;
      }
    }

    return false;
  }

  private final Set<CharacterSet> getFirstCharacters() {
    if (this.firstCharacters != null) {
      return this.firstCharacters;
    }

    this.firstCharacters = new HashSet<CharacterSet>();

    for (final Transition transition : this.startState) {
      if (!transition.isEpsilonTransition()) {
        this.firstCharacters.add(transition.getCharacters());
      }
    }

    for (final NFAState state : this.startState.epsilonClosure(this.acceptingStates)) {
      for (final Transition transition : state) {
        if (!transition.isEpsilonTransition()) {
          this.firstCharacters.add(transition.getCharacters());
        }
      }
    }

    return this.firstCharacters;
  }


  //------------------------------------------------------------------------------------------------


  public static final NFA fromRegularExpression(final RegularExpression regularExpression) {
    return regularExpression.accept(new BaseASTVisitor<NFAState, NFA>() {

      @Override
      public final NFA visit(final RegularExpression regularExpression,
          final NFAState previousState) {
        assert (previousState == null);

        final NFA nfa = regularExpression.getAlternatives().accept(this, null);

        return nfa;
      }

      @Override
      public final NFA visit(final Alternatives alternatives, final NFAState previousState) {
        if (alternatives.getNumberOfAlternatives() == 1) {
          final Sequence singleAlternative = alternatives.getAlternatives().get(0);
          final NFA singleAlternativeNFA = singleAlternative.accept(this, previousState);

          return applyQuantifier(singleAlternativeNFA, alternatives.getQuantifier());
        }

        final NFAState startState = (previousState == null) ? (new NFAState()) : (previousState);
        final NFA nfa = new NFA(startState);

        for (final Sequence alternative : alternatives) {
          final NFA alternativeNFA = alternative.accept(this, startState);

          if (startState != alternativeNFA.getStartState()) {
            startState.addEpsilonTransition(alternativeNFA.getStartState());
          }
          nfa.acceptingStates.addAll(alternativeNFA.acceptingStates);
        }

        return applyQuantifier(nfa, alternatives.getQuantifier());
      }

      @Override
      public final NFA visit(final Sequence sequence, final NFAState previousState) {
        if (sequence.getNumberOfElements() == 0) {
          final NFAState singleState = (previousState == null) ? (new NFAState()) : (previousState);
          final NFA nfa = new NFA(singleState);
          nfa.addAcceptingState(singleState);

          return nfa;
        } else if (sequence.getNumberOfElements() == 1) {
          final Atom singleElement = sequence.getElements().get(0);
          final NFA nfa = singleElement.accept(this, previousState);

          return nfa;
        }

        final NFAState startState = (previousState == null) ? (new NFAState()) : (previousState);
        final NFA nfa = new NFA(startState);

        NFAState acceptingState = startState;

        for (final Atom element : sequence) {
          final NFA elementNFA = element.accept(this, acceptingState);

          if (acceptingState != elementNFA.getStartState()) {
            acceptingState.addEpsilonTransition(elementNFA.getStartState());
          }

          if (elementNFA.acceptingStates.size() == 1) {
            acceptingState = elementNFA.acceptingStates.iterator().next();
          } else {
            acceptingState = new NFAState();
            for (final NFAState elementAcceptingState : elementNFA.acceptingStates) {
              elementAcceptingState.addEpsilonTransition(acceptingState);
            }
          }

          if (acceptingState.getTransitions().size() > 0) {
            final NFAState newAcceptingState = new NFAState();
            acceptingState.addEpsilonTransition(newAcceptingState);
            acceptingState = newAcceptingState;
          }
        }

        nfa.addAcceptingState(acceptingState);

        return nfa;
      }

      @Override
      public final NFA visit(final Group group, final NFAState previousState) {
        final NFAState startState = (previousState == null) ? (new NFAState()) : (previousState);
        final NFA nfa = new NFA(startState);

        final NFAState acceptingState = new NFAState();

        startState.addTransition(Transition.characterTransition(new CharacterSet() {

            @Override
            public final boolean matches(final char inputCharacter) {
              for (final Range range : group) {
                if (range.inRange(inputCharacter)) {
                  return !group.isInverted();
                }
              }
              return group.isInverted();
            }

            @Override
            public final String toString() {
              return PrettyPrinter.prettyPrint(group);
            }

        }, acceptingState));

        nfa.addAcceptingState(acceptingState);

        return applyQuantifier(nfa, group.getQuantifier());
      }

      @Override
      public final NFA visit(final Literal literal, final NFAState previousState) {
        final NFAState startState = (previousState == null) ? (new NFAState()) : (previousState);
        final NFA nfa = new NFA(startState, literal.getValue());

        NFAState acceptingState = startState;

        for (final char character : literal.getValue().toCharArray()) {
          final NFAState nextState = new NFAState();

          acceptingState.addTransition(Transition.characterTransition(new CharacterSet() {

            @Override
            public final boolean matches(final char inputCharacter) {
              return character == inputCharacter;
            }

            @Override
            public final String toString() {
              return String.valueOf(character);
            }
          
          }, nextState));

          acceptingState = nextState;
        }

        nfa.addAcceptingState(acceptingState);

        return applyQuantifier(nfa, literal.getQuantifier());
      }

      private final NFA applyQuantifier(final NFA baseNFA,
          final Atom.Quantifier quantifier) {
        if (quantifier == Atom.Quantifier.QUANT_NONE) {
          return baseNFA;
        }

        final NFAState startState = baseNFA.getStartState();
        final NFAState acceptingState;
        {
          if (baseNFA.acceptingStates.size() == 1) {
            acceptingState = baseNFA.acceptingStates.iterator().next();
          } else {
            acceptingState = new NFAState();

            for (final NFAState baseAcceptingState : baseNFA.acceptingStates) {
              baseAcceptingState.addEpsilonTransition(acceptingState);
            }
          }
        }

        final NFA nfa = new NFA(startState);
        nfa.addAcceptingState(acceptingState);

        switch (quantifier) {
          case QUANT_STAR: {
            acceptingState.addEpsilonTransition(startState);
            startState.addEpsilonTransition(acceptingState);
            break;
          }
          case QUANT_PLUS: {
            acceptingState.addEpsilonTransition(startState);
            break;
          }
          case QUANT_OPTIONAL: {
            startState.addEpsilonTransition(acceptingState);
            break;
          }
          default: {
            throw new AssertionError("unknown quantifier: " + quantifier);
          }
        }

        return nfa;
      }

    }, null);
  }

}
