package components.functions


object HexUtils {
    fun intelHexToBinary(inputFile: String, outputFilePath: String) {
        /*
        val binaryData = mutableListOf<Byte>()

        context.assets.open(hexFilePath).bufferedReader().useLines { lines ->
            lines.forEach { line ->
                if (line.startsWith(":")) {
                    // Remove the leading colon
                    val hexLine = line.substring(1)

                    // Parse the fields from the line
                    val byteCount = hexLine.substring(0, 2).toInt(16)
                    val address = hexLine.substring(2, 6).toInt(16)
                    val recordType = hexLine.substring(6, 8).toInt(16)

                    if (recordType == 0) {
                        // Data record type
                        val data = hexLine.substring(8 until 8 + byteCount * 2)
                        val bytes = data.chunked(2) { it.toString().toInt(16).toByte()}
                        binaryData.addAll(bytes)
                    } else if (recordType == 1) {
                        // End of file record type
                        return@forEach
                    }
                }
            }
        }

        val outputFile = File(context.filesDir, outputFilePath)
        outputFile.writeBytes(binaryData.toByteArray())
         */
    }

    fun motorolaHexToBinary(hexFilePath: String, outputFilePath: String) {
        /*
        val binaryData = mutableListOf<Byte>()

        context.assets.open(hexFilePath).bufferedReader().useLines { lines ->
            lines.forEach { line ->
                if (line.startsWith("S")) {

                    when (line[1]) {
                        '0', '1', '9' -> {
                            // Data record type
                            val byteCount = line.substring(2, 4).toInt(16)
                            val address = line.substring(4, 8).toInt(16)
                            val data = line.substring(8, 8 + byteCount * 2)

                            val bytes = data.chunked(2) { it.toString().toInt(16).toByte() }
                            binaryData.addAll(bytes)
                        }
                        '7', '8' -> {
                            // End of file record type
                            return@forEach
                        }
                    }
                }
            }
        }

        val outputFile = File(context.filesDir, outputFilePath)
        outputFile.writeBytes(binaryData.toByteArray())
         */
    }

    fun binaryToIntelHex(input: String, outputFilePath: String) {
        val binaryData = input.hexToByteArray()

        val hexLines = mutableListOf<String>()

        var address = 0
        val bytesPerLine = 16

        for (i in binaryData.indices step bytesPerLine) {
            val bytes = binaryData.copyOfRange(i, i + bytesPerLine)

            val byteCount = bytes.size
            val recordType = "00"
            val addressHex = address.toString(16).padStart(4, '0')

            val line = StringBuilder(":")
                .append(byteCount.toString(16).padStart(2, '0'))
                .append(addressHex)
                .append(recordType)

            bytes.forEach { byte ->
                line.append(byte.toInt().toString(16).padStart(2, '0'))
            }

            val checksum = (256 - (byteCount + address + recordType.toInt(16) + bytes.sum()).and(0xFF)).and(0xFF)
            line.append(checksum.toString(16).padStart(2, '0'))

            hexLines.add(line.toString())

            address += bytesPerLine
        }

        // File(outputFilePath).writeText(hexLines.joinToString("\n"))
    }

    fun binaryToMotorolaHex(input: String, outputFilePath: String) {
        val binaryData = input.hexToByteArray()

        val hexLines = mutableListOf<String>()

        var address = 0
        val bytesPerLine = 16

        for (i in binaryData.indices step bytesPerLine) {
            val bytes = binaryData.copyOfRange(i, i + bytesPerLine)

            val byteCount = bytes.size
            val addressHex = address.toString(16).padStart(6, '0')

            val line = StringBuilder("S1")
                .append(byteCount.toString(16).padStart(2, '0'))
                .append(addressHex)

            bytes.forEach { byte ->
                line.append(byte.toInt().toString(16).padStart(2, '0'))
            }

            val checksum = (255 - (byteCount + address + bytes.sum())).and(0xFF)
            line.append(checksum.toString(16).padStart(2, '0'))

            hexLines.add(line.toString())

            address += bytesPerLine
        }

        // File(outputFilePath).writeText(hexLines.joinToString("\n"))
    }

}
