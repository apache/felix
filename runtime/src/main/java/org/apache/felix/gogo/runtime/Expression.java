/*
 * Copyright 2012 Udo Klimaschewski
 *
 * http://UdoJava.com/
 * http://about.me/udo.klimaschewski
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */
package org.apache.felix.gogo.runtime;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.stream.Collectors;

/**
 * Enhanced to provide assignment operators and variables from a map, comparison operators, string operations and more.
 */
/**
 * <h1>EvalEx - Java Expression Evaluator</h1>
 *
 * <h2>Introduction</h2>
 * EvalEx is a handy expression evaluator for Java, that allows to evaluate simple mathematical and boolean expressions.
 * <br>
 * Key Features:
 * <ul>
 * <li>Uses BigDecimal for calculation and result</li>
 * <li>Single class implementation, very compact</li>
 * <li>No dependencies to external libraries</li>
 * <li>Precision and rounding mode can be set</li>
 * <li>Supports variables</li>
 * <li>Standard boolean and mathematical operators</li>
 * <li>Standard basic mathematical and boolean functions</li>
 * <li>Custom functions and operators can be added at runtime</li>
 * </ul>
 * <br>
 * <h2>Examples</h2>
 * <pre>
 *  BigDecimal result = null;
 *
 *  Expression expression = new Expression("1+1/3");
 *  result = expression.eval():
 *  expression.setPrecision(2);
 *  result = expression.eval():
 *
 *  result = new Expression("(3.4 + -4.1)/2").eval();
 *
 *  result = new Expression("SQRT(a^2 + b^2").with("a","2.4").and("b","9.253").eval();
 *
 *  BigDecimal a = new BigDecimal("2.4");
 *  BigDecimal b = new BigDecimal("9.235");
 *  result = new Expression("SQRT(a^2 + b^2").with("a",a).and("b",b).eval();
 *
 *  result = new Expression("2.4/PI").setPrecision(128).setRoundingMode(RoundingMode.UP).eval();
 *
 *  result = new Expression("random() > 0.5").eval();
 *
 *  result = new Expression("not(x<7 || sqrt(max(x,9)) <= 3))").with("x","22.9").eval();
 * </pre>
 * <br>
 * <h2>Supported Operators</h2>
 * <table>
 *   <tr><th>Mathematical Operators</th></tr>
 *   <tr><th>Operator</th><th>Description</th></tr>
 *   <tr><td>+</td><td>Additive operator</td></tr>
 *   <tr><td>-</td><td>Subtraction operator</td></tr>
 *   <tr><td>*</td><td>Multiplication operator</td></tr>
 *   <tr><td>/</td><td>Division operator</td></tr>
 *   <tr><td>%</td><td>Remainder operator (Modulo)</td></tr>
 *   <tr><td>^</td><td>Power operator</td></tr>
 * </table>
 * <br>
 * <table>
 *   <tr><th>Boolean Operators<sup>*</sup></th></tr>
 *   <tr><th>Operator</th><th>Description</th></tr>
 *   <tr><td>=</td><td>Equals</td></tr>
 *   <tr><td>==</td><td>Equals</td></tr>
 *   <tr><td>!=</td><td>Not equals</td></tr>
 *   <tr><td>&lt;&gt;</td><td>Not equals</td></tr>
 *   <tr><td>&lt;</td><td>Less than</td></tr>
 *   <tr><td>&lt;=</td><td>Less than or equal to</td></tr>
 *   <tr><td>&gt;</td><td>Greater than</td></tr>
 *   <tr><td>&gt;=</td><td>Greater than or equal to</td></tr>
 *   <tr><td>&amp;&amp;</td><td>Boolean and</td></tr>
 *   <tr><td>||</td><td>Boolean or</td></tr>
 * </table>
 * *Boolean operators result always in a BigDecimal value of 1 or 0 (zero). Any non-zero value is treated as a _true_ value. Boolean _not_ is implemented by a function.
 * <br>
 * <h2>Supported Functions</h2>
 * <table>
 *   <tr><th>Function<sup>*</sup></th><th>Description</th></tr>
 *   <tr><td>NOT(<i>expression</i>)</td><td>Boolean negation, 1 (means true) if the expression is not zero</td></tr>
 *   <tr><td>IF(<i>condition</i>,<i>value_if_true</i>,<i>value_if_false</i>)</td><td>Returns one value if the condition evaluates to true or the other if it evaluates to false</td></tr>
 *   <tr><td>RANDOM()</td><td>Produces a random number between 0 and 1</td></tr>
 *   <tr><td>MIN(<i>e1</i>,<i>e2</i>)</td><td>Returns the smaller of both expressions</td></tr>
 *   <tr><td>MAX(<i>e1</i>,<i>e2</i>)</td><td>Returns the bigger of both expressions</td></tr>
 *   <tr><td>ABS(<i>expression</i>)</td><td>Returns the absolute (non-negative) value of the expression</td></tr>
 *   <tr><td>ROUND(<i>expression</i>,precision)</td><td>Rounds a value to a certain number of digits, uses the current rounding mode</td></tr>
 *   <tr><td>FLOOR(<i>expression</i>)</td><td>Rounds the value down to the nearest integer</td></tr>
 *   <tr><td>CEILING(<i>expression</i>)</td><td>Rounds the value up to the nearest integer</td></tr>
 *   <tr><td>LOG(<i>expression</i>)</td><td>Returns the natural logarithm (base e) of an expression</td></tr>
 *   <tr><td>SQRT(<i>expression</i>)</td><td>Returns the square root of an expression</td></tr>
 *   <tr><td>SIN(<i>expression</i>)</td><td>Returns the trigonometric sine of an angle (in degrees)</td></tr>
 *   <tr><td>COS(<i>expression</i>)</td><td>Returns the trigonometric cosine of an angle (in degrees)</td></tr>
 *   <tr><td>TAN(<i>expression</i>)</td><td>Returns the trigonometric tangens of an angle (in degrees)</td></tr>
 *   <tr><td>SINH(<i>expression</i>)</td><td>Returns the hyperbolic sine of a value</td></tr>
 *   <tr><td>COSH(<i>expression</i>)</td><td>Returns the hyperbolic cosine of a value</td></tr>
 *   <tr><td>TANH(<i>expression</i>)</td><td>Returns the hyperbolic tangens of a value</td></tr>
 *   <tr><td>RAD(<i>expression</i>)</td><td>Converts an angle measured in degrees to an approximately equivalent angle measured in radians</td></tr>
 *   <tr><td>DEG(<i>expression</i>)</td><td>Converts an angle measured in radians to an approximately equivalent angle measured in degrees</td></tr>
 * </table>
 * *Functions names are case insensitive.
 * <br>
 * <h2>Supported Constants</h2>
 * <table>
 *   <tr><th>Constant</th><th>Description</th></tr>
 *   <tr><td>PI</td><td>The value of <i>PI</i>, exact to 100 digits</td></tr>
 *   <tr><td>TRUE</td><td>The value one</td></tr>
 *   <tr><td>FALSE</td><td>The value zero</td></tr>
 * </table>
 *
 * <h2>Add Custom Operators</h2>
 *
 * Custom operators can be added easily, simply create an instance of `Expression.Operator` and add it to the expression.
 * Parameters are the operator string, its precedence and if it is left associative. The operators `eval()` method will be called with the BigDecimal values of the operands.
 * All existing operators can also be overridden.
 * <br>
 * For example, add an operator `x >> n`, that moves the decimal point of _x_ _n_ digits to the right:
 *
 * <pre>
 * Expression e = new Expression("2.1234 >> 2");
 *
 * e.addOperator(e.new Operator(">>", 30, true) {
 *     {@literal @}Override
 *     public BigDecimal eval(BigDecimal v1, BigDecimal v2) {
 *         return v1.movePointRight(v2.toBigInteger().intValue());
 *     }
 * });
 *
 * e.eval(); // returns 212.34
 * </pre>
 * <br>
 * <h2>Add Custom Functions</h2>
 *
 * Adding custom functions is as easy as adding custom operators. Create an instance of `Expression.Function`and add it to the expression.
 * Parameters are the function name and the count of required parameters. The functions `eval()` method will be called with a list of the BigDecimal parameters.
 * All existing functions can also be overridden.
 * <br>
 * For example, add a function `average(a,b,c)`, that will calculate the average value of a, b and c:
 * <br>
 * <pre>
 * Expression e = new Expression("2 * average(12,4,8)");
 *
 * e.addFunction(e.new Function("average", 3) {
 *     {@literal @}Override
 *     public BigDecimal eval(List<BigDecimal> parameters) {
 *         BigDecimal sum = parameters.get(0).add(parameters.get(1)).add(parameters.get(2));
 *         return sum.divide(new BigDecimal(3));
 *     }
 * });
 *
 * e.eval(); // returns 16
 * </pre>
 * The software is licensed under the MIT Open Source license (see LICENSE file).
 * <br>
 * <ul>
 * <li>The *power of* operator (^) implementation was copied from [Stack Overflow](http://stackoverflow.com/questions/3579779/how-to-do-a-fractional-power-on-bigdecimal-in-java) Thanks to Gene Marin</li>
 * <li>The SQRT() function implementation was taken from the book [The Java Programmers Guide To numerical Computing](http://www.amazon.de/Java-Number-Cruncher-Programmers-Numerical/dp/0130460419) (Ronald Mak, 2002)</li>
 * </ul>
 *
 *@author Udo Klimaschewski (http://about.me/udo.klimaschewski)
 */
public class Expression {

    /**
     * Definition of PI as a constant, can be used in expressions as variable.
     */
    public static final BigDecimal PI = new BigDecimal(
            "3.1415926535897932384626433832795028841971693993751058209749445923078164062862089986280348253421170679");

    /**
     * The {@link MathContext} to use for calculations.
     */
    private MathContext mc = MathContext.DECIMAL32;

    /**
     * The original infix expression.
     */
    private String expression = null;

    /**
     * The cached RPN (Reverse Polish Notation) of the expression.
     */
    private List<Token> rpn = null;

    /**
     * All defined operators with name and implementation.
     */
    private Map<String, Operator> operators = new HashMap<>();

    /**
     * All defined functions with name and implementation.
     */
    private Map<String, Function> functions = new HashMap<>();

    /**
     * All defined variables with name and value.
     */
    private Map<String, Object> constants = new HashMap<>();

    /**
     * What character to use for decimal separators.
     */
    private final char decimalSeparator = '.';

    /**
     * What character to use for minus sign (negative values).
     */
    private final char minusSign = '-';

    /**
     * The expression evaluators exception class.
     */
    public class ExpressionException extends RuntimeException {
        private static final long serialVersionUID = 1118142866870779047L;

        public ExpressionException(String message) {
            super(message);
        }
    }

    interface Token {

    }

    public class Constant implements Token {

        private final Object value;

        public Constant(Object value) {
            this.value = value;
        }

        public Object getValue() {
            return value;
        }
    }

    public class Variable implements Token {

        private final String name;

        public Variable(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public class LeftParen implements Token {
        @Override
        public String toString() {
            return "(";
        }
    }

    public class Comma implements Token {
    }

    /**
     * Abstract definition of a supported expression function. A function is
     * defined by a name, the number of parameters and the actual processing
     * implementation.
     */
    public abstract class Function implements Token {
        /**
         * Name of this function.
         */
        private String name;
        /**
         * Number of parameters expected for this function.
         */
        private int numParams;

        /**
         * Creates a new function with given name and parameter count.
         *
         * @param name
         *            The name of the function.
         * @param numParams
         *            The number of parameters for this function.
         */
        public Function(String name, int numParams) {
            this.name = name.toUpperCase();
            this.numParams = numParams;
        }

        public String getName() {
            return name;
        }

        public int getNumParams() {
            return numParams;
        }

        public BigDecimal eval(Map<String, Object> variables, List<Object> parameters) {
            List<BigDecimal> numericParameters = parameters.stream()
                    .map(o -> toBigDecimal(variables, o))
                    .collect(Collectors.toList());
            return eval(numericParameters);
        }

        /**
         * Implementation for this function.
         *
         * @param parameters
         *            Parameters will be passed by the expression evaluator as a
         *            {@link List} of {@link BigDecimal} values.
         * @return The function must return a new {@link BigDecimal} value as a
         *         computing result.
         */
        public abstract BigDecimal eval(List<BigDecimal> parameters);

        @Override
        public String toString() {
            return name;
        }
    }

    /**
     * Abstract definition of a supported operator. An operator is defined by
     * its name (pattern), precedence and if it is left- or right associative.
     */
    public abstract class Operator implements Token {
        /**
         * This operators name (pattern).
         */
        private String oper;
        /**
         * Operators precedence.
         */
        private int precedence;
        /**
         * Operator is left associative.
         */
        private boolean leftAssoc;

        /**
         * Creates a new operator.
         *
         * @param oper
         *            The operator name (pattern).
         * @param precedence
         *            The operators precedence.
         * @param leftAssoc
         *            <code>true</code> if the operator is left associative,
         *            else <code>false</code>.
         */
        public Operator(String oper, int precedence, boolean leftAssoc) {
            this.oper = oper;
            this.precedence = precedence;
            this.leftAssoc = leftAssoc;
        }

        public String getOper() {
            return oper;
        }

        public int getPrecedence() {
            return precedence;
        }

        public boolean isLeftAssoc() {
            return leftAssoc;
        }

        public Object eval(Map<String, Object> variables, Object v1, Object v2) {
            if (v1 instanceof Variable) {
                v1 = variables.get(((Variable) v1).getName());
            }
            if (v2 instanceof Variable) {
                v2 = variables.get(((Variable) v2).getName());
            }
            boolean numeric = isNumber(v1) && isNumber(v2);
            if (numeric) {
                return eval(toBigDecimal(variables, v1), toBigDecimal(variables, v2));
            } else {
                return eval(v1 != null ? v1.toString() : "", v2 != null ? v2.toString() : "");
            }
        }

        public Object eval(String v1, String v2) {
            return eval(toBigDecimal(v1), toBigDecimal(v2));
        }

        /**
         * Implementation for this operator.
         *
         * @param v1
         *            Operand 1.
         * @param v2
         *            Operand 2.
         * @return The result of the operation.
         */
        public abstract BigDecimal eval(BigDecimal v1, BigDecimal v2);

        @Override
        public String toString() {
            return oper;
        }
    }

    public abstract class Comparator extends Operator {

        public Comparator(String oper, int precedence) {
            super(oper, precedence, false);
        }

        @Override
        public BigDecimal eval(BigDecimal v1, BigDecimal v2) {
            return compare(v1, v2) ? BigDecimal.ONE : BigDecimal.ZERO;
        }

        @Override
        public Object eval(String v1, String v2) {
            return compare(v1, v2) ? BigDecimal.ONE : BigDecimal.ZERO;
        }

        /**
         * This method actually implements the comparison.
         * It will be called with either 2 BigIntegers or 2 Strings.
         *
         * @param v1
         *            Operand 1.
         * @param v2
         *            Operand 2.
         * @return The result of the comparison.
         */
        public abstract boolean compare(Comparable v1, Comparable v2);
    }

    /**
     * Marker class for assignment operators.
     * Those operators need a variable on the left hand side.
     */
    public abstract class Assignment extends Operator {

        public Assignment(String assign, int precedence) {
            super(assign, precedence, true);
        }

        public Object eval(Map<String, Object> variables, Object v1, Object v2) {
            if (!(v1 instanceof Variable))  {
                throw new IllegalArgumentException("Left hand side of operator " + getOper() + " should be a variable but found " + v1.toString());
            }
            String name = ((Variable) v1).getName();
            Object r = super.eval(variables, v1, v2);
            if (r instanceof Number) {
                r = toResult(toBigDecimal(r));
            }
            variables.put(name, r);
            return r;
        }

    }


    /**
     * Expression tokenizer that allows to iterate over a {@link String}
     * expression token by token. Blank characters will be skipped.
     */
    private class Tokenizer implements Iterator<String> {

        /**
         * Actual position in expression string.
         */
        private int pos = 0;

        /**
         * The original input expression.
         */
        private String input;
        /**
         * The previous token or <code>null</code> if none.
         */
        private String previousToken;

        /**
         * Creates a new tokenizer for an expression.
         *
         * @param input
         *            The expression string.
         */
        public Tokenizer(String input) {
            this.input = input;
        }

        public boolean hasNext() {
            return (pos < input.length());
        }

        /**
         * Peek at the next character, without advancing the iterator.
         *
         * @return The next character or character 0, if at end of string.
         */
        private char peekNextChar() {
            if (pos < (input.length() - 1)) {
                return input.charAt(pos + 1);
            } else {
                return 0;
            }
        }

        public String next() {
            StringBuilder token = new StringBuilder();
            if (pos >= input.length()) {
                return previousToken = null;
            }
            char ch = input.charAt(pos);
            while (Character.isWhitespace(ch) && pos < input.length()) {
                ch = input.charAt(++pos);
            }
            if (Character.isDigit(ch)) {
                while ((Character.isDigit(ch) || ch == decimalSeparator)
                        && (pos < input.length())) {
                    token.append(input.charAt(pos++));
                    ch = pos == input.length() ? 0 : input.charAt(pos);
                }
            } else if (ch == minusSign
                    && Character.isDigit(peekNextChar())
                    && ("(".equals(previousToken) || ",".equals(previousToken)
                    || previousToken == null || operators
                    .containsKey(previousToken))) {
                token.append(minusSign);
                pos++;
                token.append(next());
            } else if (Character.isLetter(ch)) {
                while ((Character.isLetter(ch) || Character.isDigit(ch) || (ch == '_')) && (pos < input.length())) {
                    token.append(input.charAt(pos++));
                    ch = pos == input.length() ? 0 : input.charAt(pos);
                }
            } else if (ch == '"') {
                boolean escaped = false;
                token.append(input.charAt(pos++));
                do {
                    if (pos == input.length()) {
                        throw new IllegalArgumentException("Non terminated quote");
                    }
                    ch = input.charAt(pos++);
                    escaped = (!escaped && ch == '\\');
                    token.append(ch);
                } while (escaped || ch != '"');
            } else if (ch == '(' || ch == ')' || ch == ',') {
                token.append(ch);
                pos++;
            } else {
                while (!Character.isLetter(ch) && !Character.isDigit(ch)
                        && !Character.isWhitespace(ch) && ch != '('
                        && ch != ')' && ch != ',' && (pos < input.length())) {
                    token.append(input.charAt(pos));
                    pos++;
                    ch = pos == input.length() ? 0 : input.charAt(pos);
                    if (ch == minusSign) {
                        break;
                    }
                }
                if (!operators.containsKey(token.toString())) {
                    throw new ExpressionException("Unknown operator '" + token
                            + "' at position " + (pos - token.length() + 1));
                }
            }
            return previousToken = token.toString();
        }

        public void remove() {
            throw new ExpressionException("remove() not supported");
        }

        /**
         * Get the actual character position in the string.
         *
         * @return The actual character position.
         */
        public int getPos() {
            return pos;
        }

    }

    /**
     * Creates a new expression instance from an expression string.
     *
     * @param expression
     *            The expression. E.g. <code>"2.4*sin(3)/(2-4)"</code> or
     *            <code>"sin(y)>0 & max(z, 3)>3"</code>
     */
    public Expression(String expression) {
        this.expression = expression;

        addOperator(new Assignment("=", 5) {
            @Override
            public BigDecimal eval(BigDecimal v1, BigDecimal v2) {
                return v2;
            }
        });
        addOperator(new Assignment("+=", 5) {
            @Override
            public BigDecimal eval(BigDecimal v1, BigDecimal v2) {
                return v1.add(v2, mc);
            }

            @Override
            public Object eval(String v1, String v2) {
                return v1 + v2;
            }
        });
        addOperator(new Assignment("-=", 5) {
            @Override
            public BigDecimal eval(BigDecimal v1, BigDecimal v2) {
                return v1.subtract(v2, mc);
            }
        });
        addOperator(new Assignment("*=", 5) {
            @Override
            public BigDecimal eval(BigDecimal v1, BigDecimal v2) {
                return v1.multiply(v2, mc);
            }
        });
        addOperator(new Assignment("/=", 5) {
            @Override
            public BigDecimal eval(BigDecimal v1, BigDecimal v2) {
                return v1.divide(v2, mc);
            }
        });
        addOperator(new Assignment("%=", 5) {
            @Override
            public BigDecimal eval(BigDecimal v1, BigDecimal v2) {
                return v1.remainder(v2, mc);
            }
        });
        addOperator(new Assignment("|=", 5) {
            @Override
            public BigDecimal eval(BigDecimal v1, BigDecimal v2) {
                return new BigDecimal(v1.toBigInteger().or(v2.toBigInteger()), mc);
            }
        });
        addOperator(new Assignment("&=", 5) {
            @Override
            public BigDecimal eval(BigDecimal v1, BigDecimal v2) {
                return new BigDecimal(v1.toBigInteger().and(v2.toBigInteger()), mc);
            }
        });
        addOperator(new Assignment("^=", 5) {
            @Override
            public BigDecimal eval(BigDecimal v1, BigDecimal v2) {
                return new BigDecimal(v1.toBigInteger().xor(v2.toBigInteger()), mc);
            }
        });
        addOperator(new Assignment("<<=", 5) {
            @Override
            public BigDecimal eval(BigDecimal v1, BigDecimal v2) {
                return new BigDecimal(v1.toBigInteger().shiftLeft(v2.intValue()), mc);
            }
        });
        addOperator(new Assignment(">>=", 5) {
            @Override
            public BigDecimal eval(BigDecimal v1, BigDecimal v2) {
                return new BigDecimal(v1.toBigInteger().shiftRight(v2.intValue()), mc);
            }
        });

        addOperator(new Operator("<<", 10, true) {
            @Override
            public BigDecimal eval(BigDecimal v1, BigDecimal v2) {
                return new BigDecimal(v1.toBigInteger().shiftLeft(v2.intValue()), mc);
            }
        });
        addOperator(new Operator(">>", 10, true) {
            @Override
            public BigDecimal eval(BigDecimal v1, BigDecimal v2) {
                return new BigDecimal(v1.toBigInteger().shiftRight(v2.intValue()), mc);
            }
        });
        addOperator(new Operator("|", 15, true) {
            @Override
            public BigDecimal eval(BigDecimal v1, BigDecimal v2) {
                return new BigDecimal(v1.toBigInteger().or(v2.toBigInteger()), mc);
            }
        });
        addOperator(new Operator("&", 15, true) {
            @Override
            public BigDecimal eval(BigDecimal v1, BigDecimal v2) {
                return new BigDecimal(v1.toBigInteger().and(v2.toBigInteger()), mc);
            }
        });
        addOperator(new Operator("^", 15, true) {
            @Override
            public BigDecimal eval(BigDecimal v1, BigDecimal v2) {
                return new BigDecimal(v1.toBigInteger().xor(v2.toBigInteger()), mc);
            }
        });
        addOperator(new Operator("+", 20, true) {
            @Override
            public BigDecimal eval(BigDecimal v1, BigDecimal v2) {
                return v1.add(v2, mc);
            }
            @Override
            public Object eval(String v1, String v2) {
                return v1 + v2;
            }
        });
        addOperator(new Operator("-", 20, true) {
            @Override
            public BigDecimal eval(BigDecimal v1, BigDecimal v2) {
                return v1.subtract(v2, mc);
            }
        });
        addOperator(new Operator("*", 30, true) {
            @Override
            public BigDecimal eval(BigDecimal v1, BigDecimal v2) {
                return v1.multiply(v2, mc);
            }
        });
        addOperator(new Operator("/", 30, true) {
            @Override
            public BigDecimal eval(BigDecimal v1, BigDecimal v2) {
                return v1.divide(v2, mc);
            }
        });
        addOperator(new Operator("%", 30, true) {
            @Override
            public BigDecimal eval(BigDecimal v1, BigDecimal v2) {
                return v1.remainder(v2, mc);
            }
        });
        addOperator(new Operator("**", 40, false) {
            @Override
            public BigDecimal eval(BigDecimal v1, BigDecimal v2) {
				/*-
				 * Thanks to Gene Marin:
				 * http://stackoverflow.com/questions/3579779/how-to-do-a-fractional-power-on-bigdecimal-in-java
				 */
                int signOf2 = v2.signum();
                double dn1 = v1.doubleValue();
                v2 = v2.multiply(new BigDecimal(signOf2)); // n2 is now positive
                BigDecimal remainderOf2 = v2.remainder(BigDecimal.ONE);
                BigDecimal n2IntPart = v2.subtract(remainderOf2);
                BigDecimal intPow = v1.pow(n2IntPart.intValueExact(), mc);
                BigDecimal doublePow = new BigDecimal(Math.pow(dn1,
                        remainderOf2.doubleValue()));

                BigDecimal result = intPow.multiply(doublePow, mc);
                if (signOf2 == -1) {
                    result = BigDecimal.ONE.divide(result, mc.getPrecision(),
                            RoundingMode.HALF_UP);
                }
                return result;
            }
        });
        addOperator(new Operator("&&", 4, false) {
            @Override
            public BigDecimal eval(BigDecimal v1, BigDecimal v2) {
                boolean b1 = !v1.equals(BigDecimal.ZERO);
                boolean b2 = !v2.equals(BigDecimal.ZERO);
                return b1 && b2 ? BigDecimal.ONE : BigDecimal.ZERO;
            }
        });

        addOperator(new Operator("||", 2, false) {
            @Override
            public BigDecimal eval(BigDecimal v1, BigDecimal v2) {
                boolean b1 = !v1.equals(BigDecimal.ZERO);
                boolean b2 = !v2.equals(BigDecimal.ZERO);
                return b1 || b2 ? BigDecimal.ONE : BigDecimal.ZERO;
            }
        });

        addOperator(new Comparator(">", 10) {
            @Override @SuppressWarnings("unchecked")
            public boolean compare(Comparable v1, Comparable v2) {
                return v1.compareTo(v2) > 0;
            }
        });

        addOperator(new Comparator(">=", 10) {
            @Override @SuppressWarnings("unchecked")
            public boolean compare(Comparable v1, Comparable v2) {
                return v1.compareTo(v2) >= 0;
            }
        });

        addOperator(new Comparator("<", 10) {
            @Override @SuppressWarnings("unchecked")
            public boolean compare(Comparable v1, Comparable v2) {
                return v1.compareTo(v2) < 0;
            }
        });

        addOperator(new Comparator("<=", 10) {
            @Override @SuppressWarnings("unchecked")
            public boolean compare(Comparable v1, Comparable v2) {
                return v1.compareTo(v2) <= 0;
            }
        });

        addOperator(new Comparator("==", 7) {
            @Override @SuppressWarnings("unchecked")
            public boolean compare(Comparable v1, Comparable v2) {
                return v1.compareTo(v2) == 0;
            }
        });

        addOperator(new Comparator("!=", 7) {
            @Override @SuppressWarnings("unchecked")
            public boolean compare(Comparable v1, Comparable v2) {
                return v1.compareTo(v2) != 0;
            }
        });

        addFunction(new Function("NOT", 1) {
            @Override
            public BigDecimal eval(List<BigDecimal> parameters) {
                boolean zero = parameters.get(0).compareTo(BigDecimal.ZERO) == 0;
                return zero ? BigDecimal.ONE : BigDecimal.ZERO;
            }
        });

        addFunction(new Function("IF", 3) {
            @Override
            public BigDecimal eval(List<BigDecimal> parameters) {
                boolean isTrue = !parameters.get(0).equals(BigDecimal.ZERO);
                return isTrue ? parameters.get(1) : parameters.get(2);
            }
        });

        addFunction(new Function("RANDOM", 0) {
            @Override
            public BigDecimal eval(List<BigDecimal> parameters) {
                double d = Math.random();
                return new BigDecimal(d, mc);
            }
        });
        addFunction(new Function("SIN", 1) {
            @Override
            public BigDecimal eval(List<BigDecimal> parameters) {
                double d = Math.sin(Math.toRadians(parameters.get(0)
                        .doubleValue()));
                return new BigDecimal(d, mc);
            }
        });
        addFunction(new Function("COS", 1) {
            @Override
            public BigDecimal eval(List<BigDecimal> parameters) {
                double d = Math.cos(Math.toRadians(parameters.get(0)
                        .doubleValue()));
                return new BigDecimal(d, mc);
            }
        });
        addFunction(new Function("TAN", 1) {
            @Override
            public BigDecimal eval(List<BigDecimal> parameters) {
                double d = Math.tan(Math.toRadians(parameters.get(0)
                        .doubleValue()));
                return new BigDecimal(d, mc);
            }
        });
        addFunction(new Function("SINH", 1) {
            @Override
            public BigDecimal eval(List<BigDecimal> parameters) {
                double d = Math.sinh(parameters.get(0).doubleValue());
                return new BigDecimal(d, mc);
            }
        });
        addFunction(new Function("COSH", 1) {
            @Override
            public BigDecimal eval(List<BigDecimal> parameters) {
                double d = Math.cosh(parameters.get(0).doubleValue());
                return new BigDecimal(d, mc);
            }
        });
        addFunction(new Function("TANH", 1) {
            @Override
            public BigDecimal eval(List<BigDecimal> parameters) {
                double d = Math.tanh(parameters.get(0).doubleValue());
                return new BigDecimal(d, mc);
            }
        });
        addFunction(new Function("RAD", 1) {
            @Override
            public BigDecimal eval(List<BigDecimal> parameters) {
                double d = Math.toRadians(parameters.get(0).doubleValue());
                return new BigDecimal(d, mc);
            }
        });
        addFunction(new Function("DEG", 1) {
            @Override
            public BigDecimal eval(List<BigDecimal> parameters) {
                double d = Math.toDegrees(parameters.get(0).doubleValue());
                return new BigDecimal(d, mc);
            }
        });
        addFunction(new Function("MAX", 2) {
            @Override
            public BigDecimal eval(List<BigDecimal> parameters) {
                BigDecimal v1 = parameters.get(0);
                BigDecimal v2 = parameters.get(1);
                return v1.compareTo(v2) > 0 ? v1 : v2;
            }
        });
        addFunction(new Function("MIN", 2) {
            @Override
            public BigDecimal eval(List<BigDecimal> parameters) {
                BigDecimal v1 = parameters.get(0);
                BigDecimal v2 = parameters.get(1);
                return v1.compareTo(v2) < 0 ? v1 : v2;
            }
        });
        addFunction(new Function("ABS", 1) {
            @Override
            public BigDecimal eval(List<BigDecimal> parameters) {
                return parameters.get(0).abs(mc);
            }
        });
        addFunction(new Function("LOG", 1) {
            @Override
            public BigDecimal eval(List<BigDecimal> parameters) {
                double d = Math.log(parameters.get(0).doubleValue());
                return new BigDecimal(d, mc);
            }
        });
        addFunction(new Function("ROUND", 2) {
            @Override
            public BigDecimal eval(List<BigDecimal> parameters) {
                BigDecimal toRound = parameters.get(0);
                int precision = parameters.get(1).intValue();
                return toRound.setScale(precision, mc.getRoundingMode());
            }
        });
        addFunction(new Function("FLOOR", 1) {
            @Override
            public BigDecimal eval(List<BigDecimal> parameters) {
                BigDecimal toRound = parameters.get(0);
                return toRound.setScale(0, RoundingMode.FLOOR);
            }
        });
        addFunction(new Function("CEILING", 1) {
            @Override
            public BigDecimal eval(List<BigDecimal> parameters) {
                BigDecimal toRound = parameters.get(0);
                return toRound.setScale(0, RoundingMode.CEILING);
            }
        });
        addFunction(new Function("SQRT", 1) {
            @Override
            public BigDecimal eval(List<BigDecimal> parameters) {
				/*
				 * From The Java Programmers Guide To numerical Computing
				 * (Ronald Mak, 2003)
				 */
                BigDecimal x = parameters.get(0);
                if (x.compareTo(BigDecimal.ZERO) == 0) {
                    return new BigDecimal(0);
                }
                if (x.signum() < 0) {
                    throw new ExpressionException(
                            "Argument to SQRT() function must not be negative");
                }
                BigInteger n = x.movePointRight(mc.getPrecision() << 1)
                        .toBigInteger();

                int bits = (n.bitLength() + 1) >> 1;
                BigInteger ix = n.shiftRight(bits);
                BigInteger ixPrev;

                do {
                    ixPrev = ix;
                    ix = ix.add(n.divide(ix)).shiftRight(1);
                    // Give other threads a chance to work;
                    Thread.yield();
                } while (ix.compareTo(ixPrev) != 0);

                return new BigDecimal(ix, mc.getPrecision());
            }
        });

        constants.put("PI", PI);
        constants.put("TRUE", Boolean.TRUE);
        constants.put("FALSE", Boolean.FALSE);

    }

    /**
     * Is the string a number?
     *
     * @param st
     *            The string.
     * @return <code>true</code>, if the input string is a number.
     */
    private boolean isNumber(String st) {
        if (st.charAt(0) == minusSign && st.length() == 1)
            return false;
        for (char ch : st.toCharArray()) {
            if (!Character.isDigit(ch) && ch != minusSign
                    && ch != decimalSeparator)
                return false;
        }
        return true;
    }

    private boolean isNumber(Object obj) {
        if (obj instanceof Number) {
            return true;
        } else if (obj != null) {
            return isNumber(obj.toString());
        } else {
            return false;
        }
    }

    /**
     * Implementation of the <i>Shunting Yard</i> algorithm to transform an
     * infix expression to a RPN expression.
     *
     * @param expression
     *            The input expression in infx.
     * @return A RPN representation of the expression, with each token as a list
     *         member.
     */
    private List<Token> shuntingYard(String expression) {
        List<Token> outputQueue = new ArrayList<>();
        Stack<Token> stack = new Stack<>();

        Tokenizer tokenizer = new Tokenizer(expression);

        String previousToken = null;
        while (tokenizer.hasNext()) {
            String token = tokenizer.next();
            if (token.charAt(0) == '"') {
                StringBuilder sb = new StringBuilder();
                boolean escaped = false;
                for (int i = 1; i < token.length() - 1; i++) {
                    char ch = token.charAt(i);
                    if (escaped || ch != '\\') {
                        sb.append(ch);
                    } else {
                        escaped = true;
                    }
                }
                outputQueue.add(new Constant(sb.toString()));
            } else if (isNumber(token)) {
                outputQueue.add(new Constant(toBigDecimal(token)));
            } else if (constants.containsKey(token)) {
                outputQueue.add(new Constant(constants.get(token)));
            } else if (functions.containsKey(token.toUpperCase())) {
                stack.push(functions.get(token.toUpperCase()));
            } else if (Character.isLetter(token.charAt(0))) {
                outputQueue.add(new Variable(token));
            } else if (",".equals(token)) {
                while (!stack.isEmpty() && !(stack.peek() instanceof LeftParen)) {
                    outputQueue.add(stack.pop());
                }
                if (stack.isEmpty()) {
                    outputQueue.add(new Comma());
                }
            } else if (operators.containsKey(token)) {
                Operator o1 = operators.get(token);
                Token token2 = stack.isEmpty() ? null : stack.peek();
                while (token2 instanceof Operator
                        && ((o1.isLeftAssoc() && o1.getPrecedence() <= ((Operator) token2).getPrecedence())
                          || (o1.getPrecedence() < ((Operator) token2).getPrecedence()))) {
                    outputQueue.add(stack.pop());
                    token2 = stack.isEmpty() ? null : stack.peek();
                }
                stack.push(o1);
            } else if ("(".equals(token)) {
                if (previousToken != null) {
                    if (isNumber(previousToken)) {
                        throw new ExpressionException("Missing operator at character position " + tokenizer.getPos());
                    }
                }
                stack.push(new LeftParen());
            } else if (")".equals(token)) {
                while (!stack.isEmpty() && !(stack.peek() instanceof LeftParen)) {
                    outputQueue.add(stack.pop());
                }
                if (stack.isEmpty()) {
                    throw new RuntimeException("Mismatched parentheses");
                }
                stack.pop();
                if (!stack.isEmpty() && stack.peek() instanceof Function) {
                    outputQueue.add(stack.pop());
                }
            }
            previousToken = token;
        }
        while (!stack.isEmpty()) {
            Token element = stack.pop();
            if (element instanceof LeftParen) {
                throw new RuntimeException("Mismatched parentheses");
            }
            if (!(element instanceof Operator)) {
                throw new RuntimeException("Unknown operator or function: " + element);
            }
            outputQueue.add(element);
        }
        return outputQueue;
    }

    /**
     * Evaluates the expression.
     *
     * @return The result of the expression.
     */
    public Object eval() {
        return eval(new HashMap<>());
    }

    /**
     * Evaluates the expression.
     *
     * @return The result of the expression.
     */
    public Object eval(Map<String, Object> variables) {

        Stack<Object> stack = new Stack<>();

        for (Token token : getRPN()) {
            if (token instanceof Operator) {
                Object v1 = stack.pop();
                Object v2 = stack.pop();
                Object oResult = ((Operator) token).eval(variables, v2, v1);
                stack.push(oResult);
            } else if (token instanceof Constant) {
                stack.push(((Constant) token).getValue());
            } else if (token instanceof Function) {
                Function f = (Function) token;
                List<Object> p = new ArrayList<>(f.getNumParams());
                for (int i = 0; i < f.numParams; i++) {
                    p.add(0, stack.pop());
                }
                Object fResult = f.eval(variables, p);
                stack.push(fResult);
            } else if (token instanceof Comma) {
                stack.pop();
            } else {
                stack.push(token);
            }
        }
        if (stack.size() > 1) {
            throw new IllegalArgumentException("Missing operator");
        }
        Object result = stack.pop();
        if (result instanceof Variable) {
            result = variables.get(((Variable) result).getName());
        }
        if (result instanceof BigDecimal) {
            result = toResult((BigDecimal) result);
        }
        return result;
    }

    private Number toResult(BigDecimal r) {
        long l = r.longValue();
        if (new BigDecimal(l).compareTo(r) == 0) {
            return l;
        }
        double d = r.doubleValue();
        if (new BigDecimal(d).compareTo(r) == 0) {
            return d;
        } else {
            return r.stripTrailingZeros();
        }
    }

    private BigDecimal toBigDecimal(Map<String, Object> variables, Object o) {
        if (o instanceof Variable) {
            o = variables.get(((Variable) o).getName());
        }
        if (o instanceof String) {
            if (isNumber((String) o)) {
                return new BigDecimal((String) o, mc);
            } else if (Character.isLetter(((String) o).charAt(0))) {
                o = variables.get(o);
            }
        }
        return toBigDecimal(o);
    }

    private BigDecimal toBigDecimal(Object o) {
        if (o == null) {
            return BigDecimal.ZERO;
        } else if (o instanceof Boolean) {
            return ((Boolean) o) ? BigDecimal.ONE : BigDecimal.ZERO;
        } else if (o instanceof BigDecimal) {
            return ((BigDecimal) o).round(mc);
        } else if (o instanceof BigInteger) {
            return new BigDecimal((BigInteger) o, mc);
        } else if (o instanceof Number) {
            return new BigDecimal(((Number) o).doubleValue(), mc);
        } else {
            try {
                return new BigDecimal(o.toString(), mc);
            } catch (NumberFormatException e) {
                return new BigDecimal(Double.NaN);
            }
        }
    }

    /**
     * Sets the precision for expression evaluation.
     *
     * @param precision
     *            The new precision.
     *
     * @return The expression, allows to chain methods.
     */
    public Expression setPrecision(int precision) {
        this.mc = new MathContext(precision);
        return this;
    }

    /**
     * Sets the rounding mode for expression evaluation.
     *
     * @param roundingMode
     *            The new rounding mode.
     * @return The expression, allows to chain methods.
     */
    public Expression setRoundingMode(RoundingMode roundingMode) {
        this.mc = new MathContext(mc.getPrecision(), roundingMode);
        return this;
    }

    /**
     * Adds an operator to the list of supported operators.
     *
     * @param operator
     *            The operator to add.
     * @return The previous operator with that name, or <code>null</code> if
     *         there was none.
     */
    public Operator addOperator(Operator operator) {
        return operators.put(operator.getOper(), operator);
    }

    /**
     * Adds a function to the list of supported functions
     *
     * @param function
     *            The function to add.
     * @return The previous operator with that name, or <code>null</code> if
     *         there was none.
     */
    public Function addFunction(Function function) {
        return functions.put(function.getName(), function);
    }

    /**
     * Sets a constant value.
     *
     * @param name
     *            The constant name.
     * @param value
     *            The constant value.
     * @return The expression, allows to chain methods.
     */
    public Expression addConstant(String name, Object value) {
        constants.put(name, value);
        return this;
    }

    /**
     * Get an iterator for this expression, allows iterating over an expression
     * token by token.
     *
     * @return A new iterator instance for this expression.
     */
    public Iterator<String> getExpressionTokenizer() {
        return new Tokenizer(this.expression);
    }

    /**
     * Cached access to the RPN notation of this expression, ensures only one
     * calculation of the RPN per expression instance. If no cached instance
     * exists, a new one will be created and put to the cache.
     *
     * @return The cached RPN instance.
     */
    private List<Token> getRPN() {
        if (rpn == null) {
            rpn = shuntingYard(this.expression);
        }
        return rpn;
    }

    /**
     * Get a string representation of the RPN (Reverse Polish Notation) for this
     * expression.
     *
     * @return A string with the RPN representation for this expression.
     */
    public String toRPN() {
        StringBuilder result = new StringBuilder();
        for (Token st : getRPN()) {
            if (result.length() > 0) {
                result.append(" ");
            }
            result.append(st);
        }
        return result.toString();
    }

}