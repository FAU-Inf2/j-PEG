package i2.act.peg.ast.visitors;

import i2.act.peg.ast.*;

public abstract class BaseASTVisitor<P, R> implements ASTVisitor<P, R> {

  protected R prolog(final ASTNode node, final P param) {
    // intentionally left blank
    return null;
  }

  protected R epilog(final ASTNode node, final P param) {
    // intentionally left blank
    return null;
  }

  protected R beforeChild(final ASTNode parent, final ASTNode child, final P parameter) {
    // intentionally left blank
    return null;
  }

  protected R afterChild(final ASTNode parent, final ASTNode child, final P parameter) {
    // intentionally left blank
    return null;
  }

  protected final R visitChild(final ASTNode parent, final ASTNode child, final P parameter) {
    beforeChild(parent, child, parameter);
    final R returnValue = child.accept(this, parameter);
    afterChild(parent, child, parameter);

    return returnValue;
  }

  @Override
  public R visit(final Grammar grammar, final P param) {
    prolog(grammar, param);

    for (final Production production : grammar.getProductions()) {
      visitChild(grammar, production, param);
    }

    return epilog(grammar, param);
  }

  @Override
  public R visit(final ParserProduction parserProduction, final P param) {
    prolog(parserProduction, param);

    final ParserIdentifier leftHandSide = parserProduction.getLeftHandSide();
    visitChild(parserProduction, leftHandSide, param);

    final Alternatives rightHandSide = parserProduction.getRightHandSide();
    visitChild(parserProduction, rightHandSide, param);

    return epilog(parserProduction, param);
  }

  @Override
  public R visit(final LexerProduction lexerProduction, final P param) {
    prolog(lexerProduction, param);

    final LexerIdentifier leftHandSide = lexerProduction.getLeftHandSide();
    visitChild(lexerProduction, leftHandSide, param);

    final RegularExpression regularExpression = lexerProduction.getRegularExpression();
    visitChild(lexerProduction, regularExpression, param);

    return epilog(lexerProduction, param);
  }

  @Override
  public R visit(final ParserIdentifier identifier, final P param) {
    prolog(identifier, param);

    if (identifier.hasQuantifier()) {
      final Quantifier quantifier = identifier.getQuantifier();
      visitChild(identifier, quantifier, param);
    }

    return epilog(identifier, param);
  }

  @Override
  public R visit(final LexerIdentifier identifier, final P param) {
    prolog(identifier, param);

    if (identifier.hasQuantifier()) {
      final Quantifier quantifier = identifier.getQuantifier();
      visitChild(identifier, quantifier, param);
    }

    return epilog(identifier, param);
  }

  @Override
  public R visit(final Alternatives alternatives, final P param) {
    prolog(alternatives, param);

    for (final Sequence alternative : alternatives.getAlternatives()) {
      visitChild(alternatives, alternative, param);
    }

    if (alternatives.hasQuantifier()) {
      final Quantifier quantifier = alternatives.getQuantifier();
      visitChild(alternatives, quantifier, param);
    }

    return epilog(alternatives, param);
  }

  @Override
  public R visit(final Sequence sequence, final P param) {
    prolog(sequence, param);

    for (final Atom element : sequence.getElements()) {
      visitChild(sequence, element, param);
    }

    return epilog(sequence, param);
  }

  @Override
  public R visit(final RegularExpression regularExpression, final P param) {
    prolog(regularExpression, param);

    final Alternatives alternatives = regularExpression.getAlternatives();
    visitChild(regularExpression, alternatives, param);

    return epilog(regularExpression, param);
  }

  @Override
  public R visit(final Group group, final P param) {
    prolog(group, param);

    for (final Range range : group.getRanges()) {
      visitChild(group, range, param);
    }

    if (group.hasQuantifier()) {
      final Quantifier quantifier = group.getQuantifier();
      visitChild(group, quantifier, param);
    }

    return epilog(group, param);
  }

  @Override
  public R visit(final CharacterRange characterRange, final P param) {
    prolog(characterRange, param);

    final SingleCharacter lowerCharacter = characterRange.getLowerCharacter();
    visitChild(characterRange, lowerCharacter, param);

    final SingleCharacter upperCharacter = characterRange.getUpperCharacter();
    visitChild(characterRange, upperCharacter, param);

    return epilog(characterRange, param);
  }

  @Override
  public R visit(final SingleCharacter singleCharacter, final P param) {
    prolog(singleCharacter, param);
    return epilog(singleCharacter, param);
  }

  @Override
  public R visit(final Literal literal, final P param) {
    prolog(literal, param);
    return epilog(literal, param);
  }

  @Override
  public R visit(final Quantifier quantifier, final P param) {
    prolog(quantifier, param);
    return epilog(quantifier, param);
  }

  @Override
  public R visit(final Annotation annotation, final P param) {
    prolog(annotation, param);

    if (annotation.hasValue()) {
      final Argument value = annotation.getValue();
      visitChild(annotation, value, param);
    }

    return epilog(annotation, param);
  }

  @Override
  public R visit(final IntArgument intArgument, final P param) {
    prolog(intArgument, param);
    return epilog(intArgument, param);
  }

  @Override
  public R visit(final StringArgument stringArgument, final P param) {
    prolog(stringArgument, param);
    return epilog(stringArgument, param);
  }

}
