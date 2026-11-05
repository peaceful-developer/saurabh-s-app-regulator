package com.saurabh.sar.core.challenge

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import kotlin.random.Random

class MathChallengeTest {
    @Test fun `questions use operands 10 to 999 and never go negative`() {
        val c = MathChallenge(Random(42))
        repeat(5000) {
            val q = c.newQuestion()
            assertThat(q.a).isIn(10..999)
            assertThat(q.b).isIn(10..999)
            assertThat(q.answer).isAtLeast(0)
        }
    }

    @Test fun `five correct answers in a row complete the challenge`() {
        val c = MathChallenge(Random(1))
        repeat(4) { assertThat(c.submit(c.current.answer)).isEqualTo(AnswerResult.CORRECT) }
        assertThat(c.submit(c.current.answer)).isEqualTo(AnswerResult.COMPLETED)
        assertThat(c.completed).isTrue()
    }

    @Test fun `a wrong answer starts a new set of five`() {
        val c = MathChallenge(Random(7))
        c.submit(c.current.answer)
        c.submit(c.current.answer)
        val before = c.questions
        assertThat(c.submit(c.current.answer + 1)).isEqualTo(AnswerResult.WRONG_NEW_SET)
        assertThat(c.correctCount).isEqualTo(0)
        assertThat(c.questions).isNotEqualTo(before)
        assertThat(c.questions).hasSize(5)
    }
}
