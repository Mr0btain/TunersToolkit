package components.parsers.a2l.classes.enums

import kotlinx.serialization.Serializable

/**
 * Enumeration for description of the addressing of table
 *     values or axis point values
 */
@Serializable
enum class AddressType {
    /** The relevant memory location has a 1 byte pointer to this table value or axis point value */
    PBYTE,
    /** The relevant memory location has a 2 byte pointer to this table value or axis point value */
    PWORD,
    /** The relevant memory location has a 4 byte pointer to this table value or axis point value */
    PLONG,
    /**
     * The relevant memory location has the first table value or axis point value, all others follow
     * with incrementing address.
     */
    DIRECT,
    // No AddressType could be found, return None
    NOT_SET;
}

/**
 * Parses a string and returns an AddressType if it contains one, else returns none
 */
fun String.parseAddressType(): AddressType {
    when (this.trim()) {
        "DIRECT" -> {
            return AddressType.DIRECT
        }
        "PBYTE" -> {
            return AddressType.PBYTE
        }
        "PWORD" -> {
            return AddressType.PWORD
        }
        "PLONG" -> {
            return AddressType.PLONG
        }
        else -> {
            return AddressType.NOT_SET
        }
    }
}

/*
enum addrtype: Enumeration for description of the addressing of table
    values or axis point values

PBYTE The relevant memory location has a 1 byte pointer to this
    table value or axis point value.

PWORD The relevant memory location has a 2 byte pointer to this
    table value or axis point value.

PLONG The relevant memory location has a 4 byte pointer to this
    table value or axis point value.

DIRECT The relevant memory location has the first table value or
    axis point value, all others follow with incrementing
    address.
 */