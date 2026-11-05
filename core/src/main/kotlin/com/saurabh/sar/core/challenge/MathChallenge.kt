package com.saurabh.sar.core.challenge

import kotlin.random.Random

enum class Op(val symbol: String) { PLUS("+"), MINUS("−") }

data class Question(val a: Int, val b: Int, val op: Op) {
    val answer: Int get() = if (op == Op.PLUS) a + b else a - b
    override fun toString() = "$a ${op.symbol} $b"
}

enum class AnswerResult { CORRECT, WRONG_NEW_SET, COMPLETED }

/**
 * Free a mode (FSD 7): 5 addition/subtraction questions in a row, operands 10–999,
 * never a negative result. A wrong answer throws away progress and starts a new set.
 * Leaving the screen discards the object, which discards progress.
 */
class MathChallenge(private val random: Random = Random.Default, val setSize: Int = 5) {
    var questions: List<Question> = newSet()
        private set
    var index: Int = 0
        private set
    val correctCount: Int get() = index
    val current: Question get() = questions[index.coerceAtMost(setSize - 1)]
    var completed: Boolean = false
        private set

    fun newQuestion(): Question {
        var a = random.nextInt(10, 1000)
        var b = random.nextInt(10, 1000)
        val op = if (random.nextBoolean()) Op.PLUS else Op.MINUS
        if (op == Op.MINUS && b > a) { val t = a; a = b; b = t }
        return Question(a, b, op)
    }

    private fun newSet() = List(setSize) { newQuestion() }

    fun submit(answer: Int): AnswerResult {
        check(!completed) { "Challenge already completed" }
        return if (answer == current.answer) {
            index++
            if (index >= setSize) { completed = true; AnswerResult.COMPLETED } else AnswerResult.CORRECT
        } else {
            questions = newSet()
            index = 0
            AnswerResult.WRONG_NEW_SET
        }
    }
}
