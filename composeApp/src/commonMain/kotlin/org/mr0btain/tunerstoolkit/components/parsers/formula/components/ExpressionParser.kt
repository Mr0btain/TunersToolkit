package org.mr0btain.tunerstoolkit.components.parsers.formula.components
class ExpressionParser {
    fun parse(input: String): Double {
        val postfixExpression = shuntingYard(input)
        return postfixExpression.evaluatePostfixExpression()
    }

    private fun shuntingYard(expression: String): List<String> {
        // Output queue to store the final postfix expression
        val outputQueue = mutableListOf<String>()

        // Operator stack to store operators during the conversion
        val operatorStack = Stack<Char>()

        // Precedence of operators
        val operatorsPrecedence = setOf('+', '-', '*', '/')

        val currentNumber = StringBuilder()

        fun handleNumber() {
            if (currentNumber.isNotEmpty()) {
                outputQueue.add(currentNumber.toString())
                currentNumber.clear()
            }
        }

        // Iterate through each character in the input expression
        expression.forEach {  token ->
            when {
                // If the char is a digit, add it directly to the output queue
                token.isDigit() || token == '.' -> {
                    currentNumber.append(token)
                }

                // If the char is the beginning of a ( then add it to the stack
                token == '(' -> {
                    handleNumber()
                    operatorStack.push(token)
                }

                // If the token is a closing parenthesis
                token == ')' -> {
                    handleNumber()
                    // Pop operators from the stack to the output queue until an opening parenthesis is encountered
                    while (operatorStack.isNotEmpty() && operatorStack.peek() != '(') {
                        outputQueue.add(operatorStack.pop()!!.toString())
                    }

                    // Pop the opening parenthesis from the stack
                    operatorStack.pop()
                }

                // If the char is an operator
                token in operatorsPrecedence -> {
                    handleNumber()
                    // Pop operators from the stack to the output queue if they have higher or equal precedence
                    while (operatorStack.isNotEmpty() && operatorStack.peek()!!.getPriority() >= token.getPriority() ) {
                        outputQueue.add(operatorStack.pop().toString())
                    }

                    // Push the current operator onto the stack
                    operatorStack.push(token)
                }
            }
        }

        handleNumber()

        // Pop any remaining operators from the stack to the output queue
        while (operatorStack.isNotEmpty()) {
            outputQueue.add(operatorStack.pop().toString())
        }

        // Return the final postfix expression as a list of tokens
        return outputQueue
    }

    private fun List<String>.evaluatePostfixExpression(): Double {
        val stack = Stack<Double>()

        for (token in this) {
            when {
                // If token is a number, add it to the stack
                token.toDoubleOrNull() != null -> { stack.push(token.toDouble()) }

                // Check if its an operator and perform the operation
                token.length == 1 && token[0] in "+-*/" -> {
                    val operand2 = stack.pop()
                    val operand1 = stack.pop()
                    val result = when (token[0]) {
                        '+' -> operand2?.let { operand1?.plus(it) }
                        '-' -> operand2?.let { operand1?.minus(it) }
                        '*' -> operand2?.let { operand1?.times(it) }
                        '/' -> operand2?.let { operand1?.div(it) }
                        else -> throw IllegalArgumentException("Invalid operator: $token")
                    }
                    // Push it to the stack
                    result?.let { stack.push(it) }
                }
                else -> throw IllegalArgumentException("Invalid token: $token")
            }
        }

        if (stack.size != 1) {
            throw IllegalArgumentException("Invalid postfix expression")
        }

        return stack.pop() ?: 0.0
    }

    private fun Char.getPriority(): Int {
        return when (this) {
            '-' -> 1
            '+' -> 1
            '*' -> 2
            '/' -> 2
            else -> 0
        }
    }
}