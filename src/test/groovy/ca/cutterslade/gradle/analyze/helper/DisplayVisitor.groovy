package ca.cutterslade.gradle.analyze.helper

import org.apache.commons.text.diff.CommandVisitor

class DisplayVisitor implements CommandVisitor<Character> {
    public StringBuilder left = new StringBuilder('')
    public StringBuilder right = new StringBuilder('')

    @Override
    void visitInsertCommand(final Character c) {
        right.append("(${c})")
    }

    @Override
    void visitKeepCommand(final Character c) {
        left.append("${c}")
        right.append("${c}")
    }

    @Override
    void visitDeleteCommand(final Character c) {
        left.append("{${c}}")
    }
}
