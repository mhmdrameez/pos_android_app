package com.example.quickbillposs

import java.io.ByteArrayOutputStream

/**
 * Native Kotlin ESC/POS Thermal Printer Encoder
 * Designed for 58mm (32 chars) and 80mm (48 chars) thermal printers.
 */
class EscPosEncoder(paperWidth: Int = 58) {

    private val output = ByteArrayOutputStream()
    val maxChars: Int = if (paperWidth == 80) 48 else 32

    companion object {
        private const val ESC = 0x1B
        private const val GS = 0x1D
        private const val LF = 0x0A
    }

    init {
        initPrinter()
    }

    fun initPrinter(): EscPosEncoder {
        output.write(ESC)
        output.write(0x40) // ESC @
        font('a')
        return this
    }

    fun text(content: String): EscPosEncoder {
        for (char in content) {
            val code = char.code
            output.write(if (code in 0x20..0x7E) code else 0x3F)
        }
        return this
    }

    fun font(value: Char = 'a'): EscPosEncoder {
        output.write(ESC)
        output.write(0x4D)
        output.write(if (value == 'a') 0 else 1)
        return this
    }

    fun newline(count: Int = 1): EscPosEncoder {
        for (i in 0 until count) {
            output.write(LF)
        }
        return this
    }

    fun bold(enabled: Boolean = true): EscPosEncoder {
        output.write(ESC)
        output.write(0x45)
        output.write(if (enabled) 1 else 0)
        return this
    }

    fun align(mode: String): EscPosEncoder {
        val value = when (mode.lowercase()) {
            "center" -> 1
            "right" -> 2
            else -> 0
        }
        output.write(ESC)
        output.write(0x61)
        output.write(value)
        return this
    }

    fun size(width: Int = 1, height: Int = 1): EscPosEncoder {
        val w = (width.coerceIn(1, 2) - 1) shl 4
        val h = height.coerceIn(1, 2) - 1
        val value = w or h
        output.write(GS)
        output.write(0x21)
        output.write(value)
        return this
    }

    fun separator(char: Char = '-'): EscPosEncoder {
        return text(char.toString().repeat(maxChars)).newline()
    }

    fun tableRow(left: String, right: String): EscPosEncoder {
        val space = maxChars - left.length - right.length
        val padding = space.coerceAtLeast(1)
        return text("$left${" ".repeat(padding)}$right").newline()
    }

    fun feedLines(count: Int = 1): EscPosEncoder {
        val lines = count.coerceIn(0, 255)
        output.write(ESC)
        output.write(0x64)
        output.write(lines)
        return this
    }

    fun cut(partial: Boolean = false): EscPosEncoder {
        feedLines(1)
        output.write(GS)
        output.write(0x56)
        output.write(if (partial) 0x42 else 0x41)
        output.write(0x00)
        return this
    }

    fun encode(): ByteArray {
        return output.toByteArray()
    }
}
