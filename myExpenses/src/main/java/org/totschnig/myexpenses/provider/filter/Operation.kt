package org.totschnig.myexpenses.provider.filter

const val LIKE_ESCAPE_CHAR = "\\"

enum class Operation(private val op: String?) {
    NOPE(""),
    EQ("=?"),
    NEQ("!=?"),
    GT(">?"),
    GTE(">=?"),
    LT("<?"),
    LTE("<=?"),
    BTW("BETWEEN ? AND ?"),
    IS_NULL("is NULL"),
    LIKE("LIKE ? ESCAPE '$LIKE_ESCAPE_CHAR'"),
    IN(null),
    IS_NULL_OR_BLANK("IFNULL(TRIM(column), '') = ''")
    ;

    fun getOp(length: Int): String {
        if (this == IN) {
            val sb = StringBuilder()
            sb.append("IN (")
            for (i in 0 until length) {
                sb.append("?")
                if (i < length - 1) {
                    sb.append(",")
                }
            }
            sb.append(")")
            return sb.toString()
        }
        return op!!
    }
}