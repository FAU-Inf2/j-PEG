package i2.act.peg.ast.visitors;

import i2.act.peg.ast.*;

public interface ASTVisitor<P, R> {

  public R visit(final Grammar grammar, final P param);

  public R visit(final ParserProduction parserProduction, final P param);

  public R visit(final LexerProduction lexerProduction, final P param);

  public R visit(final ParserIdentifier parserIdentifier, final P param);

  public R visit(final LexerIdentifier lexerIdentifier, final P param);

  public R visit(final Alternatives alternatives, final P param);

  public R visit(final Sequence sequence, final P param);

  public R visit(final RegularExpression regularExpression, final P param);

  public R visit(final Group group, final P param);

  public R visit(final CharacterRange characterRange, final P param);

  public R visit(final SingleCharacter singleCharacter, final P param);

  public R visit(final Literal literal, final P param);

  public R visit(final Quantifier quantifier, final P param);

  public R visit(final Annotation annotation, final P param);

  public R visit(final IntArgument intArgument, final P param);

  public R visit(final StringArgument stringArgument, final P param);

}
