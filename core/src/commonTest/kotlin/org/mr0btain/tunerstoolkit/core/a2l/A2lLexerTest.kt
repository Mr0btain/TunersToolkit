package org.mr0btain.tunerstoolkit.core.a2l

import org.mr0btain.tunerstoolkit.core.diagnostics.Severity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class A2lLexerTest {
    private fun tokens(text: String): List<A2lToken> {
        val lexer = A2lLexer(text)
        return generateSequence { lexer.next().takeIf { it.kind != TokenKind.EOF } }.toList()
    }

    private fun kinds(text: String) = tokens(text).map { it.kind to it.text }

    @Test
    fun namesStringsNumbersAndBlocks() {
        assertEquals(
            listOf(
                TokenKind.BEGIN to "/begin", TokenKind.IDENT to "CHARACTERISTIC", TokenKind.IDENT to "map.x[2]",
                TokenKind.STRING to "Long id", TokenKind.NUMBER to "0x1A2b", TokenKind.END to "/end", TokenKind.IDENT to "CHARACTERISTIC"
            ),
            kinds("/begin CHARACTERISTIC map.x[2] \"Long id\" 0x1A2b /end CHARACTERISTIC")
        )
    }

    @Test
    fun numbersOfEveryShape() {
        val numbers = listOf("0", "-12", "+3", "1.5", "-0.25", ".5", "1e3", "2.5E-4", "-1.0e+10", "0xFFFF", "0X10")
        assertEquals(numbers.map { TokenKind.NUMBER to it }, kinds(numbers.joinToString(" ")))
        assertEquals(255L, A2lValue.parseLong("0xFF"))
        assertEquals(-16L, A2lValue.parseLong("-0x10"))
        assertEquals(0xFFFFFFFFL, A2lValue.parseLong("0xFFFFFFFF"))
    }

    @Test
    fun beginAndEndInAnyCase() {
        assertEquals(listOf(TokenKind.BEGIN, TokenKind.IDENT, TokenKind.END, TokenKind.IDENT), tokens("/BEGIN X /End X").map { it.kind })
        assertEquals(TokenKind.INCLUDE, tokens("/include \"a.a2l\"").first().kind)
    }

    @Test
    fun stringEscapes() {
        assertEquals(
            listOf("say \"hi\"", "back\\slash", "a\"b", "line\nbreak", ""),
            tokens("\"say \\\"hi\\\"\" \"back\\\\slash\" \"a\"\"b\" \"line\\nbreak\" \"\"").map { it.text }
        )
    }

    @Test
    fun commentsAreSkippedAndLinesCounted() {
        val result = tokens("A /* one\n two */ B // three\n C")
        assertEquals(listOf("A", "B", "C"), result.map { it.text })
        assertEquals(listOf(1, 2, 3), result.map { it.line })
        assertEquals(listOf(1, 9, 2), result.map { it.column })
    }

    @Test
    fun aStringCanSpanLines() {
        val result = tokens("\"one\ntwo\" X")
        assertEquals("one\ntwo", result[0].text)
        assertEquals(2, result[1].line)
    }

    @Test
    fun brokenInputIsReportedNotThrown() {
        val lexer = A2lLexer("A \"never closed", "f.a2l")
        val all = generateSequence { lexer.next().takeIf { it.kind != TokenKind.EOF } }.toList()
        assertEquals(listOf("A", "never closed"), all.map { it.text })
        assertEquals(Severity.ERROR, lexer.diagnostics.single().severity)

        assertEquals(listOf(TokenKind.IDENT, TokenKind.OTHER, TokenKind.IDENT), tokens("A ; B").map { it.kind })
        assertTrue(A2lLexer("/* open").let { it.next(); it.diagnostics.isNotEmpty() })
    }

    @Test
    fun rawTextUpToTheMatchingEnd() {
        val lexer = A2lLexer("/begin A2ML struct { int; }; \"/end in a string\" /* /end */ /begin X /end X /end A2ML AFTER")
        lexer.next(); lexer.next()
        assertEquals(" struct { int; }; \"/end in a string\" /* /end */ /begin X /end X ", lexer.rawUntilEnd())
        assertEquals("A2ML", lexer.next().text)
        assertEquals("AFTER", lexer.next().text)
        assertNull(A2lLexer("never ended").rawUntilEnd())
    }

    @Test
    fun randomTextNeverThrows() {
        val random = kotlin.random.Random(7)
        val alphabet = "/begin/end\"\\*0x1.e-+ \n\tABC_[].;{}é"
        repeat(300) {
            val text = CharArray(random.nextInt(0, 200)) { alphabet[random.nextInt(alphabet.length)] }.concatToString()
            val lexer = A2lLexer(text)
            var guard = 0
            while (lexer.next().kind != TokenKind.EOF) check(guard++ < 10_000) { "Lexer stuck on $text" }
            A2lParser(text).parse()
        }
    }
}
