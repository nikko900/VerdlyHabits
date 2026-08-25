package com.saintnico.verdlyhabits.data.remote.firestore

/**
 * Builds Firestore search index fields so rivals can be found by partial username,
 * display name, or name fragments (Instagram-style discovery).
 */
object UserSearchIndex {

    fun usernameLower(username: String): String =
        username.trim().removePrefix("@").lowercase()

    fun displayNameLower(displayName: String): String =
        displayName.trim().lowercase()

    fun buildSearchKeywords(username: String, displayName: String): List<String> {
        val terms = linkedSetOf<String>()
        val u = usernameLower(username)
        val d = displayNameLower(displayName)

        fun addTerm(raw: String) {
            val cleaned = raw.trim().lowercase()
            if (cleaned.length >= 2) terms.add(cleaned)
        }

        if (u.isNotBlank() && !u.equals("unknownrival", ignoreCase = true)) {
            addTerm(u)
            u.split(Regex("[^a-z0-9]+")).filter { it.length >= 2 }.forEach(::addTerm)
            addUsernameSubstrings(u, terms)
        }

        if (d.isNotBlank() && !d.equals("rival", ignoreCase = true)) {
            addTerm(d)
            addTerm(d.replace(" ", ""))
            d.split(Regex("\\s+")).filter { it.length >= 2 }.forEach(::addTerm)
        }

        return terms.take(40).toList()
    }

    private fun addUsernameSubstrings(username: String, terms: MutableSet<String>) {
        if (username.length < 3) return
        var added = 0
        for (len in 3..username.length) {
            for (start in 0..username.length - len) {
                terms.add(username.substring(start, start + len))
                if (++added >= 28) return
            }
        }
    }

    fun rank(query: String, username: String, displayName: String): Int {
        val q = query.lowercase()
        val u = usernameLower(username)
        val d = displayNameLower(displayName)
        return when {
            u == q -> 1_000
            u.startsWith(q) -> 850 + q.length
            d.startsWith(q) -> 750 + q.length
            u.contains(q) -> 550 + q.length
            d.contains(q) -> 450 + q.length
            else -> 100
        }
    }
}
