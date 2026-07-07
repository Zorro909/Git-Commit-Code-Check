package de.zorro909.codecheck.core.diagnostic;

import com.github.javaparser.Position;

public record SourcePosition(int line, int column) {

    public static SourcePosition from(Position position) {
        return new SourcePosition(position.line, position.column);
    }

    public Position toJavaParserPosition() {
        return new Position(line, column);
    }
}
