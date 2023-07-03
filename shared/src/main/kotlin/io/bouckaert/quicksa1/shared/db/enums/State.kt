package io.bouckaert.quicksa1.shared.db.enums

enum class State(val fullText: String) {
    ACT("Australian Capital Territory"),
    NT("Northern Territory"),
    TAS("Tasmania"),
    WA("Western Australia"),
    SA("South Australia"),
    QLD("Queensland"),
    VIC("Victoria"),
    NSW("New South Wales");

    companion object {
        private val map = values().associateBy(State::fullText)
        private val abbreviationMap = values().associateBy { it.name.uppercase() }
        fun fromString(fullText: String) = map[fullText]
        fun fromAbbreviation(abbreviation: String) = abbreviationMap[abbreviation.uppercase().trim()]
    }

    override fun toString(): String = fullText
}