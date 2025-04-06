/*--------------------------------------------------------------*
  Copyright (C) 2006-2015 OpenSim Ltd.

  This file is distributed WITHOUT ANY WARRANTY. See the file
  'License' for details on this and other legal matters.
*--------------------------------------------------------------*/

package org.omnetpp.common.util;

import java.io.IOException;
import java.io.PushbackReader;
import java.io.Reader;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.Assert;
import org.omnetpp.common.Debug;

/**
 * Parser for filter texts with error recovery.
 *
 * A filter expression is called simple if it is a conjunction of field matchers.
 * E.g: name("n") AND replication("r10") AND module(sink).
 *
 * The purpose of this parser is to build a parse tree even if the input
 * is incomplete, erroneous. The parse tree can be used to split a simple filter
 * expression into separate field matchers or to offer completions at a given position.
 *
 * Therefore the language it accepts is an extension of the language that can be used
 * as a filter in {@link ScaveModelUtil.filterIDList}.
 *
 * DEPRECATED. We probably don't need the parser any more. The problem with it is that
 * it doesn't scale to very long expressions we have in Scave: it fails with
 * StackOverflowException due to recursive descent parsing. Tokenizer is OK and still
 * very much used. --Andras
 *
 * @author tomi
 */
public class MatchExpressionSyntax {
    private static boolean debug = false;

    /**
     * Parse the <code>filter</code> string and returns the parse tree.
     */
    public static Node parseFilter(String filter) {
        Lexer lexer = new Lexer(filter);
        Parser parser = new Parser(lexer);
        Node tree = parser.parse();
        if (debug)
            Debug.println("MatchExpressionSyntax.parseFilter():\n  " + filter + "\nparsed as:\n" + tree.toString());
        return tree;
    }

    public enum TokenType {
        AND,
        OR,
        NOT,
        MATCHES,
        STRING_LITERAL,
        OP,
        CP,
        END,
    }

    /**
     * Leaf nodes of the parse tree.
     */
    public static class Token {
        TokenType type;
        String value;
        int startPos, endPos;
        Node parent;
        boolean incomplete;

        public Token(TokenType type, int startPos, int endPos) {
            this(type, null, startPos, endPos);
        }

        public Token(TokenType type, String value, int startPos, int endPos) {
            this.type = type;
            this.value = value;
            this.startPos = startPos;
            this.endPos = endPos;
        }

        public TokenType getType() {
            return type;
        }

        public String getValue() {
            return value;
        }

        public int getStartPos() {
            return startPos;
        }

        public int getEndPos() {
            return endPos;
        }

        public Node getParent() {
            return parent;
        }

        public boolean isEmpty() {
            return startPos == endPos;
        }

        public boolean isIncomplete() {
            return incomplete;
        }

        public String toString() {
            if (value == null)
                return String.format("%s<%d,%d>", type.name(), startPos, endPos);
            else
                return String.format("%s(%s)<%d,%d>", type.name(), value, startPos, endPos);
        }

    }

    public static interface INodeVisitor {
        /**
         * @return true if traversal should be continued
         */
        boolean visit(Node node);
        void visit(Token token);
    }

    /**
     * Internal nodes of the parse tree.
     */
    public static class Node {

        public static final int ROOT = 0;
        public static final int FIELDPATTERN = 1; // fieldName =~ pattern
        public static final int PATTERN = 2;      // pattern
        public static final int UNARY_OPERATOR_EXPR = 3;          // <operator> <node>
        public static final int BINARY_OPERATOR_EXPR = 4;          // <node> <operator> <node>
        public static final int PARENTHESISED_EXPR = 5;

        public int type;
        Object[] content; // Tokens and Nodes
        Node parent;

        public Node(Node expr, Token end) {
            type = ROOT;
            content = new Object[2];
            addChild(0, expr);
            addChild(1, end);
        }

        public Node(Token fieldName, Token matches, Token pattern) {
            type = FIELDPATTERN;
            content = new Object[3];
            addChild(0, fieldName);
            addChild(1, matches);
            addChild(2, pattern);
        }

        public Node(Token pattern) {
            type = PATTERN;
            content = new Object[1];
            addChild(0, pattern);
        }

        public Node(Token operator, Node operand) {
            type = UNARY_OPERATOR_EXPR;
            content = new Object[2];
            addChild(0, operator);
            addChild(1, operand);
        }

        public Node(Token operator, Node leftOperand, Node rightOperand) {
            type = BINARY_OPERATOR_EXPR;
            content = new Object[3];
            addChild(0, leftOperand);
            addChild(1, operator);
            addChild(2, rightOperand);
        }

        public Node(Token open, Node expr, Token close) {
            type = PARENTHESISED_EXPR;
            content = new Object[3];
            addChild(0, open);
            addChild(1, expr);
            addChild(2, close);
        }

        protected void addChild(int index, Node child) {
            child.parent = this;
            content[index] = child;
        }

        protected void addChild(int index, Token child) {
            child.parent = this;
            content[index] = child;
        }

        public Node getParent() {
            return parent;
        }

        public int getType() {
            return type;
        }

        public Object[] getContent() {
            return content;
        }

        public Node getExpr() {
            Assert.isTrue(type == ROOT || type == PARENTHESISED_EXPR);
            if (type == ROOT)
                return (Node)content[0];
            else
                return (Node)content[1];
        }

        public Token getOperator() {
            Assert.isTrue(type == UNARY_OPERATOR_EXPR || type == BINARY_OPERATOR_EXPR);
            if (type == UNARY_OPERATOR_EXPR)
                return (Token)content[0];
            else
                return (Token)content[1];
        }

        public Node getOperand() {
            Assert.isTrue(type == UNARY_OPERATOR_EXPR);
            return (Node)content[1];
        }

        public Node getLeftOperand() {
            Assert.isTrue(type == BINARY_OPERATOR_EXPR);
            return (Node)content[0];
        }

        public Node getRightOperand() {
            Assert.isTrue(type == BINARY_OPERATOR_EXPR);
            return (Node)content[2];
        }

        public Token getField() {
            Assert.isTrue(type == FIELDPATTERN);
            return (Token)content[0];
        }

        public String getFieldName() {
            return getField().getValue();
        }

        public Token getMatchesOp() {
            Assert.isTrue(type == FIELDPATTERN);
            return (Token)content[1];
        }

        public Token getOpeningParen() {
            Assert.isTrue(type == PARENTHESISED_EXPR);
            return (Token)content[0];
        }

        public Token getClosingParen() {
            Assert.isTrue(type == PARENTHESISED_EXPR);
            return (Token)content[2];
        }

        public Token getPattern() {
            Assert.isTrue(type == FIELDPATTERN || type == PATTERN);
            if (type == FIELDPATTERN)
                return (Token)content[2];
            else
                return (Token)content[0];
        }

        public String getPatternString() {
            return getPattern().getValue();
        }

        public void accept(INodeVisitor visitor) {
            boolean cont = visitor.visit(this);
            if (cont) {
                for (Object obj : content) {
                    if (obj instanceof Node)
                        ((Node)obj).accept(visitor);
                    else if (obj instanceof Token)
                        visitor.visit((Token)obj);
                }
            }
        }


        public String toString() {
            StringBuffer sb = new StringBuffer();
            format(sb, 0);
            return sb.toString();
        }

        protected void format(StringBuffer sb, int level) {
            // indent
            sb.append(StringUtils.repeat("  ", level));

            switch (type) {
            case ROOT:
                getExpr().format(sb, level);
                break;
            case FIELDPATTERN:
                sb.append(getFieldName()).append(" =~ ").append(getPatternString()).append("\n");
                break;
            case PATTERN:
                sb.append(getPatternString()).append("\n");
                break;
            case UNARY_OPERATOR_EXPR:
                sb.append(getOperator().getValue()).append("\n");
                if (getOperand() != null)
                    getOperand().format(sb, level+1);
                break;
            case BINARY_OPERATOR_EXPR:
                sb.append(getOperator().getValue()).append("\n");
                if (getLeftOperand() != null)
                    getLeftOperand().format(sb, level+1);
                if (getRightOperand() != null)
                    getRightOperand().format(sb, level+1);
                break;
            case PARENTHESISED_EXPR:
                sb.append("(");
                getExpr().format(sb, level+1);
                sb.append(")");
            default:
                sb.append("<ERROR>\n");
            }
        }
    }

    /**
     * Lexer for filter expression.
     *
     * TODO: \\ and \" within patterns
     */
    public static class Lexer {
        public static final int EOF = -1;
        public static final Map<String, TokenType> keywords = new HashMap<String, TokenType>(6);

        static {
            keywords.put("OR", TokenType.OR);
            keywords.put("or", TokenType.OR);
            keywords.put("AND", TokenType.AND);
            keywords.put("and", TokenType.AND);
            keywords.put("NOT", TokenType.NOT);
            keywords.put("not", TokenType.NOT);
            keywords.put("=~", TokenType.MATCHES);
        }

        PushbackReader input;
        int pos;                // index of the next character
        boolean finished;

        public Lexer(String input) {
            this(input, 0);
        }

        public Lexer(String input, int startPos) {
            this(new StringReader(input), startPos);
        }

        public Lexer(Reader input, int startPos) {
            this.input = new PushbackReader(input, 1);
            this.pos = startPos;
        }

        public TokenType getKeywordByPrefix(String prefix) { // XXX assumes that keyword prefixes are unique
            for (Map.Entry<String,TokenType> entry : keywords.entrySet())
                if (entry.getKey().startsWith(prefix))
                    return entry.getValue();
            return null;
        }

        public Token getNextToken() {
            int ch, la;
            StringBuffer value;

            if (finished)
                return new Token(TokenType.END, pos, pos);

            while (true) {
                int startPos = pos;
                ch = getChar();
                switch (ch) {
                case EOF: return new Token(TokenType.END, startPos, pos);
                case '(': return new Token(TokenType.OP, startPos, pos);
                case ')': return new Token(TokenType.CP, startPos, pos);
                case '=':
                    ch = getChar();
                    if (ch == '~')
                        return new Token(TokenType.MATCHES, "=~", startPos, pos);
                    else {
                        ungetChar(ch);
                        break;
                    }
                case '"':
                    value = new StringBuffer(30);
                    while (true) {
                        switch (ch = getChar()) {
                        case '"': return new Token(TokenType.STRING_LITERAL, value.toString(), startPos, pos);
                        case EOF: return new Token(TokenType.STRING_LITERAL, value.toString(), startPos, pos);
                        case '\\': la = getChar();
                                 if (la == '\\' || la == '"')
                                     value.append(la);
                                 else {
                                     value.append(ch);
                                     ungetChar(la);
                                 }
                                 break;
                        default: value.append((char)ch);
                        }
                    }
                case ' ': case '\n': case '\t': continue;
                default:
                    value = new StringBuffer(30);
                    value.append((char)ch);
                    LOOP:
                    while (true) {
                        switch (ch = getChar()) {
                        case EOF: break LOOP;
                        case ' ': case '\t': case '\n': case '(': case ')': ungetChar(ch); break LOOP;
                        case '=': int lah = getChar(); if (lah != EOF) ungetChar(lah); if (lah == '~') {ungetChar(ch); break LOOP;} else value.append((char)ch);
                        default: value.append((char)ch); break;
                        }
                    };
                    String v = value.toString();
                    if (keywords.containsKey(v))
                        return new Token(keywords.get(v), v, startPos, pos);
                    else
                        return new Token(TokenType.STRING_LITERAL, v, startPos, pos);
                }
            }
        }

        protected int getChar() {
            try {
                int ch = input.read();
                if (ch == EOF)
                    finished = true;
                else
                    pos++;
                return ch;
            } catch (IOException e) {
                return EOF;
            }
        }

        protected void ungetChar(int ch) {
            try {
                input.unread(ch);
                pos--;
            } catch (IOException e) {
                // Should never happen, because an ungetChar() call
                // always preceded by a getChar() call. (No two adjacent
                // ungetChar() calls.)
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Parser for filter expressions.
     *
     */
    public static class Parser {
        protected Lexer lexer;

        protected Token lookAhead1;
        protected Token lookAhead2;

        public Parser(Lexer lexer) {
            this.lexer = lexer;
        }

        /*
         * expression:      expr EOF
         *
         * expr:            orExpr
         *
         * orExpr:              andExpr OR orExpr
         *                  |   andExpr
         *
         * andExpr:             notExpr AND andExpr
         *                  |   notExpr
         *
         * notExpr:             NOT primaryExpr
         *                  |   primaryExpr
         *
         * primaryExpr:         ( expr )
         *                  |   LITERAL =~ LITERAL
         *                  |   LITERAL
         */
        public Node parse() {
            getNextToken();
            getNextToken();
            return expression();
        }

        protected Node expression() {
            Node expr = expr();
            // when there is something extra at the end,
            // if it is a prefix for OR or AND try to continue, otherwise ignore
            while (lookAhead1.type == TokenType.STRING_LITERAL) {
                Token literal = getNextToken();
                TokenType keyword = lexer.getKeywordByPrefix(literal.value);
                if (keyword != TokenType.AND && keyword != TokenType.OR)
                    break;
                Token operator = literal;
                operator.type = keyword;
                operator.incomplete = true;
                Node expr2 = expr();
                expr = new Node(operator, expr, expr2);
            }
            Token end = match(TokenType.END);
            return new Node(expr, end);
        }

        protected Node expr() {
            return orExpr();
        }

        protected Node orExpr() {
            Node first = andExpr();
            if (lookAhead1.type == TokenType.OR) {
                Token operator = match(TokenType.OR);
                Node second = orExpr();
                return new Node(operator, first, second);
            }
            else
                return first;
        }

        protected Node andExpr() {
            Node first = notExpr();
            if (lookAhead1.type == TokenType.AND) {
                Token operator = match(TokenType.AND);
                Node second = andExpr();
                return new Node(operator, first, second);
            }
            else
                return first;
        }

        protected Node notExpr() {
            if (lookAhead1.type == TokenType.NOT) {
                Token operator = match(TokenType.NOT);
                Node expr = primaryExpr();
                return new Node(operator, expr);
            }
            else
                return primaryExpr();
        }

        protected Node primaryExpr() {

            if (lookAhead1.type == TokenType.OP) {
                Token open = match(TokenType.OP);
                Node expr = expr();
                Token close = match(TokenType.CP, true);
                return new Node(open, expr, close);
            }
            else if (lookAhead1.type == TokenType.STRING_LITERAL && lookAhead2.type == TokenType.MATCHES) {
                Token fieldName = match(TokenType.STRING_LITERAL);
                Token matches = match(TokenType.MATCHES);
                Token pattern = match(TokenType.STRING_LITERAL, true);
                return new Node(fieldName, matches, pattern);
            }
            else if (lookAhead1.type == TokenType.STRING_LITERAL) {
                Token pattern = match(TokenType.STRING_LITERAL);
                return new Node(pattern);
            }

            // recover
            while (lookAhead1.type != TokenType.END && lookAhead1.type != TokenType.OP && lookAhead1.type != TokenType.STRING_LITERAL)
                getNextToken();

            if (lookAhead1.type != TokenType.END)
                return primaryExpr();
            else {
                Token pattern = match(TokenType.STRING_LITERAL);
                return new Node(pattern);
            }
        }

        protected Token match(TokenType token) {
            return match(token, false);
        }

        protected Token match(TokenType tokenExpected, boolean create) {

            if (tokenExpected != lookAhead1.type && !create) {
                while (lookAhead1.type != TokenType.END && lookAhead1.type != tokenExpected) {
                    getNextToken();
                }
            }

            if (tokenExpected == lookAhead1.type) {
                return getNextToken();
            }
            else { // create
                return new Token(tokenExpected, "", lookAhead1.startPos, lookAhead1.startPos);
            }
        }

        protected Token getNextToken() {
            Token token = lookAhead1;
            lookAhead1 = lookAhead2;
            lookAhead2 = lexer.getNextToken();
            return token;
        }
    }
}
