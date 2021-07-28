package i2.act.packrat.nfa;

import java.util.*;

public final class NFAState implements Iterable<Transition> {

  private final List<Transition> transitions;

  private Set<NFAState> epsilonClosure; // computed lazily

  public NFAState() {
    this.transitions = new ArrayList<Transition>();
  }

  public final List<Transition> getTransitions() {
    // NOTE: using an unmodifiable list would be appropriate but this introduces unnecessary
    // overhead...
    return this.transitions;
  }

  public final void addTransition(final Transition transition) {
    this.transitions.add(transition);
  }

  @Override
  public final Iterator<Transition> iterator() {
    return getTransitions().iterator();
  }

  public final void addEpsilonTransition(final NFAState to) {
    addTransition(Transition.epsilonTransition(to));
  }

  public final boolean hasNonEpsilonTransition() {
    for (final Transition transition : this.transitions) {
      if (!transition.isEpsilonTransition()) {
        return true;
      }
    }

    return false;
  }

  public final Set<NFAState> epsilonClosure(final Set<NFAState> acceptingStates) {
    if (this.epsilonClosure == null) {
      this.epsilonClosure = new HashSet<>();
      epsilonClosure(this, new HashSet<NFAState>(), acceptingStates);
    }

    // NOTE: using an unmodifiable set would be appropriate but this introduces unnecessary
    // overhead...
    return this.epsilonClosure;
  }

  private final void epsilonClosure(final NFAState state, final Set<NFAState> visited,
      final Set<NFAState> acceptingStates) {
    if (state.hasNonEpsilonTransition() || acceptingStates.contains(state)) {
      this.epsilonClosure.add(state);
    }

    visited.add(state);

    for (final Transition transition : state) {
      if (transition.isEpsilonTransition()) {
        final NFAState toState = transition.getTo();

        if (!visited.contains(toState)) {
          epsilonClosure(toState, visited, acceptingStates);
        }
      }
    }
  }

}
