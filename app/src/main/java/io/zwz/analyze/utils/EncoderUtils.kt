package io.zwz.analyze.utils

@Suppress("unused")
object EncoderUtils {

    fun escape(src: String): String {
        val tmp = StringBuilder()
        for (char in src) {
            val charCode = char.toInt()
            if (charCode in 48..57 || charCode in 65..90 || charCode in 97..122) {
                tmp.append(char)
                continue
            }

            val prefix = when {
                charCode < 16 -> "%0"
                charCode < 256 -> "%"
                else -> "%u"
            }
            tmp.append(prefix).append(charCode.toString(16))
        }
        return tmp.toString()
    }
    /*
    @JvmOverloads
    fun base64Decode(str: String, flags: Int = Base64.DEFAULT): String {
        val bytes = Base64.decode(str, flags)
        return String(bytes)
    }

    @JvmOverloads
    fun base64Encode(str: String, flags: Int = Base64.NO_WRAP): String? {
        return Base64.encodeToString(str.toByteArray(), flags)
    }

    @JvmOverloads
    fun compress(primStr: String?): String? {
        if (primStr == null || primStr.isEmpty()) {
            return primStr
        }
        val out = ByteArrayOutputStream()
        var gzip: GZIPOutputStream? = null
        try {
            gzip = GZIPOutputStream(out)
            gzip.write(primStr.toByteArray())
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            if (gzip != null) {
                try {
                    gzip.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
        return Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
    }

    @JvmOverloads
    fun uncompress(compressedStr: String?): String? {
        if (compressedStr == null) {
            return null
        }
        val out = ByteArrayOutputStream()
        var input: ByteArrayInputStream? = null
        var ginzip: GZIPInputStream? = null
        var compressed: ByteArray? = null
        var decompressed: String? = null
        try {
            compressed = Base64.decode(compressedStr, Base64.NO_WRAP)
            input = ByteArrayInputStream(compressed)
            ginzip = GZIPInputStream(input)
            val buffer = ByteArray(1024)
            var offset = -1
            while (ginzip.read(buffer).also { offset = it } != -1) {
                out.write(buffer, 0, offset)
            }
            decompressed = out.toString()
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            if (ginzip != null) {
                try {
                    ginzip.close()
                } catch (e: IOException) {
                }
            }
            if (input != null) {
                try {
                    input.close()
                } catch (e: IOException) {
                }
            }
            try {
                out.close()
            } catch (e: IOException) {
            }
        }
        return decompressed
    }
     */
}