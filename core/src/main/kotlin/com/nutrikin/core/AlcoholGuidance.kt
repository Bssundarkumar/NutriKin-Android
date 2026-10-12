package com.nutrikin.core

/**
 * Ported from FamilyFoodScanner/Services/Scoring/AlcoholGuidance.swift - keep the two term lists in
 * sync if either platform's list changes; this is the thing that was buggy on iOS until a brand-name
 * backstop was added (OFF frequently has no ingredient/category data for spirits), so Android needs the
 * same two-layer check: generic terms, then known brand names.
 */
object AlcoholGuidance {
    val terms: List<String> = listOf(
        "alcohol", "ethanol", "beer", "liqueur", "rum", "whisky", "whiskey", "brandy", "vodka",
        "red wine", "white wine", "rice wine", "cooking wine", "alcoholic beverage", "gin", "tequila",
        "champagne", "sake", "mead", "ale", "lager", "stout", "port wine", "sherry", "vermouth",
        "alcoholic beverages", "beers", "wines", "spirits", "liqueurs", "cider", "ciders", "cocktails",
    )

    val knownBrands: List<String> = listOf(
        "grey goose", "absolut", "smirnoff", "titos", "tito's", "ketel one", "belvedere", "svedka",
        "jack daniels", "jack daniel's", "jim beam", "johnnie walker", "jameson", "crown royal",
        "makers mark", "maker's mark", "wild turkey", "chivas regal", "glenlivet", "glenfiddich",
        "macallan", "bacardi", "captain morgan", "malibu", "jose cuervo", "patron", "don julio",
        "hennessy", "remy martin", "courvoisier", "baileys", "kahlua", "grand marnier", "cointreau",
        "jagermeister", "jägermeister", "bombay sapphire", "tanqueray", "beefeater", "hendricks",
        "hendrick's", "budweiser", "heineken", "corona", "guinness", "stella artois", "coors",
        "miller lite", "modelo", "dos equis", "yuengling", "kingfisher beer", "moet", "moët",
        "veuve clicquot", "dom perignon", "dom pérignon", "barefoot wine", "yellow tail wine",
    )

    fun isAlcoholic(product: Product): Boolean {
        val text = product.searchText
        return (terms + knownBrands).any { term ->
            // Word-boundary match, same intent as iOS's NSRegularExpression (?<![a-z])term(?![a-z]).
            Regex("(?<![a-z])${Regex.escape(term)}(?![a-z])").containsMatchIn(text)
        }
    }
}
