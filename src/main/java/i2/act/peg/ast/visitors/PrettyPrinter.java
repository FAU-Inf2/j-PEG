package i2.act.peg.ast.visitors;

import i2.act.peg.ast.*;
import i2.act.util.SafeWriter;

import java.io.BufferedWriter;
import java.io.StringWriter;

public final class PrettyPrinter extends BaseASTVisitor<SafeWriter, Void> {

  public static final void prettyPrint(final ASTNode node, final SafeWriter writer) {
    final PrettyPrinter prettyPrinter = new PrettyPrinter();
    node.accept(prettyPrinter, writer);
    writer.flush();
  }

  public static final String prettyPrint(final ASTNode node) {
    final StringWriter stringWriter = new StringWriter();

    final SafeWriter safeWriter = SafeWriter.fromBufferedWriter(new BufferedWriter(stringWriter));

    prettyPrint(node, safeWriter);

    stringWriter.flush();

    final String string = stringWriter.toString();
    safeWriter.close();

    return string;
  }

  //------------------------------------------------------------------------------------------------

  private boolean topLevelAlternatives;
  private boolean lexerProduction;

  @Override
  public final Void visit(final Grammar grammar, final SafeWriter writer) {
    boolean first = true;
    for (final Production production : grammar.getProductions()) {
      if (first) {
        first = false;
      } else {
        writer.write("\n");
      }

      production.accept(this, writer);
    }

    return null;
  }

  @Override
  public final Void visit(final ParserProduction parserProduction, final SafeWriter writer) {
    assert (this.lexerProduction == false);

    for (final Annotation annotation : parserProduction.getAnnotations()) {
      annotation.accept(this, writer);
    }

    final ParserIdentifier leftHandSide = parserProduction.getLeftHandSide();
    leftHandSide.accept(this, writer);

    writer.write("\n");

    final boolean oldTopLevelAlternatives = this.topLevelAlternatives;
    this.topLevelAlternatives = true;

    final Alternatives rightHandSide = parserProduction.getRightHandSide();
    rightHandSide.accept(this, writer);

    this.topLevelAlternatives = oldTopLevelAlternatives;

    writer.write("  ;\n");

    return null;
  }

  @Override
  public final Void visit(final LexerProduction lexerProduction, final SafeWriter writer) {
    assert (this.lexerProduction == false);

    this.lexerProduction = true;

    for (final Annotation annotation : lexerProduction.getAnnotations()) {
      annotation.accept(this, writer);
    }

    final LexerIdentifier leftHandSide = lexerProduction.getLeftHandSide();
    leftHandSide.accept(this, writer);

    writer.write(": ");

    final RegularExpression regularExpression = lexerProduction.getRegularExpression();
    regularExpression.accept(this, writer);

    writer.write(" ;\n");

    this.lexerProduction = false;

    return null;
  }

  @Override
  public final Void visit(final ParserIdentifier identifier, final SafeWriter writer) {
    final String name = identifier.getName();
    writer.write(name);

    if (identifier.hasQuantifier()) {
      identifier.getQuantifier().accept(this, writer);
    }

    return null;
  }

  @Override
  public final Void visit(final LexerIdentifier identifier, final SafeWriter writer) {
    final String name = identifier.getName();
    writer.write(name);

    if (identifier.hasQuantifier()) {
      identifier.getQuantifier().accept(this, writer);
    }

    return null;
  }

  @Override
  public final Void visit(final Alternatives alternatives, final SafeWriter writer) {
    if (this.topLevelAlternatives) {
      if (!this.lexerProduction) {
        writer.write("  : ");
      }
    } else {
      writer.write("(");
    }

    boolean first = true;
    for (final Sequence alternative : alternatives.getAlternatives()) {
      if (first) {
        first = false;
      } else {
        if (this.topLevelAlternatives && !this.lexerProduction) {
          writer.write("\n  | ");
        } else {
          writer.write(" | ");
        }
      }

      alternative.accept(this, writer);
    }

    if (this.topLevelAlternatives) {
      if (!this.lexerProduction) {
        writer.write("\n");
      }
    } else {
      writer.write(")");
    }

    assert (!this.topLevelAlternatives || !alternatives.hasQuantifier());

    if (alternatives.hasQuantifier()) {
      alternatives.getQuantifier().accept(this, writer);
    }

    return null;
  }

  @Override
  public final Void visit(final Sequence sequence, final SafeWriter writer) {
    boolean first = true;
    for (final Atom element : sequence.getElements()) {
      if (first) {
        first = false;
      } else {
        writer.write(" ");
      }

      if (sequence.getWeight() != 1) {
        writer.write("<%d> ", sequence.getWeight());
      }

      final boolean oldTopLevelAlternatives = this.topLevelAlternatives;
      this.topLevelAlternatives = false;

      element.accept(this, writer);

      this.topLevelAlternatives = oldTopLevelAlternatives;
    }

    return null;
  }

  @Override
  public final Void visit(final RegularExpression regularExpression, final SafeWriter writer) {
    final boolean oldTopLevelAlternatives = this.topLevelAlternatives;
    this.topLevelAlternatives = true;

    final Alternatives alternatives = regularExpression.getAlternatives();
    alternatives.accept(this, writer);

    this.topLevelAlternatives = oldTopLevelAlternatives;

    return null;
  }

  @Override
  public final Void visit(final Group group, final SafeWriter writer) {
    writer.write("[");

    if (group.isInverted()) {
      writer.write("^");
    }

    for (final Range range : group.getRanges()) {
      range.accept(this, writer);
    }

    writer.write("]");

    if (group.hasQuantifier()) {
      group.getQuantifier().accept(this, writer);
    }

    return null;
  }

  @Override
  public final Void visit(final CharacterRange characterRange, final SafeWriter writer) {
    final SingleCharacter lowerCharacter = characterRange.getLowerCharacter();
    lowerCharacter.accept(this, writer);

    writer.write("-");

    final SingleCharacter upperCharacter = characterRange.getUpperCharacter();
    upperCharacter.accept(this, writer);

    return null;
  }

  @Override
  public final Void visit(final SingleCharacter singleCharacter, final SafeWriter writer) {
    writer.write(singleCharacter.getEscapedValue());
    return null;
  }

  @Override
  public final Void visit(final Literal literal, final SafeWriter writer) {
    writer.write("'%s'", literal.getEscapedValue());

    if (literal.hasQuantifier()) {
      literal.getQuantifier().accept(this, writer);
    }

    return null;
  }

  @Override
  public final Void visit(final Quantifier quantifier, final SafeWriter writer) {
    if (quantifier.getWeight() != 1) {
      writer.write("<%d>", quantifier.getWeight());
    }

    writer.write(quantifier.getKind().stringRepresentation);

    return null;
  }

  @Override
  public final Void visit(final Annotation annotation, final SafeWriter writer) {
    final String key = annotation.getKey();
    writer.write("@%s", key);

    if (annotation.hasValue()) {
      final Argument value = annotation.getValue();

      writer.write("(");
      value.accept(this, writer);
      writer.write(")");
    }

    writer.write("\n");

    return null;
  }

  @Override
  public final Void visit(final IntArgument intArgument, final SafeWriter writer) {
    writer.write(intArgument.getSerialization());
    return null;
  }

  @Override
  public final Void visit(final StringArgument stringArgument, final SafeWriter writer) {
    writer.write("\"%s\"", stringArgument.getSerialization());
    return null;
  }

}
