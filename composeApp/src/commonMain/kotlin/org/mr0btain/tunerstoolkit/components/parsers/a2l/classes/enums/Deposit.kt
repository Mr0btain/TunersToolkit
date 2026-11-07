package org.mr0btain.tunerstoolkit.components.parsers.a2l.classes.enums
import org.mr0btain.tunerstoolkit.components.parsers.a2l.objects.A2lOptional
import kotlinx.serialization.Serializable

@Serializable
enum class Deposit : A2lOptional {
    ABSOLUTE,
    DIFFERENCE,
    NOT_SET;
}

fun String.parseDeposit(): Deposit {
    return when (this.replace("DEPOSIT", "").trim()) {
        "ABSOLUTE" -> Deposit.ABSOLUTE
        "DIFFERENCE" -> Deposit.DIFFERENCE
        else -> Deposit.NOT_SET
    }
}