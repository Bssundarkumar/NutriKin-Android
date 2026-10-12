package com.nutrikin.core

/** Term lists and score caps mirrored from the iOS child and pregnancy guidance. */
object SafetyGuidance {
    data class Rule(val title: String, val terms: List<String>, val cap: Int, val reason: String, val maxAge: Int? = null)
    fun contains(text: String, terms: List<String>) = terms.any {
        Regex("(?<![a-z])${Regex.escape(it)}(?![a-z])").containsMatchIn(text.lowercase())
    }
    val caffeineTerms = listOf("caffeine", "guarana", "energy drink", "coffee", "yerba mate", "matcha")
    val mercuryFishTerms = listOf("shark", "swordfish", "king mackerel", "marlin", "tilefish", "orange roughy")
    val honeyTerms = listOf("honey", "raw honey", "wildflower honey", "manuka honey", "honeycomb")
    val juiceTerms = listOf("juice", "fruit juice", "100% juice", "apple juice", "orange juice", "grape juice", "juice drink", "juice box")
    val curedMeatTerms = listOf("hot dog", "hot dogs", "frankfurter", "sausage", "salami", "bologna", "deli meat", "lunch meat", "bacon", "ham", "pepperoni", "nitrate", "nitrite", "sodium nitrite", "sodium nitrate")
    val chokingHazardTerms = listOf("whole grape", "whole grapes", "whole cherry tomato", "whole cherry tomatoes", "whole nuts", "whole nut", "popcorn", "hard candy", "hard candies", "chewing gum", "bubble gum", "marshmallow", "marshmallows", "whole hot dog", "hot dogs", "large chunks of meat")
    val addedSweetenerTerms = listOf("sugar", "cane sugar", "brown sugar", "corn syrup", "high fructose corn syrup", "glucose syrup", "sucrose", "dextrose", "fructose", "maltose", "honey", "molasses", "agave", "agave syrup", "maple syrup", "golden syrup", "invert sugar", "syrup")
    val childRules = listOf(
        Rule("Caffeine", caffeineTerms, 60, "Caffeine is commonly advised against or limited for children — it affects a child's heart rate, sleep and anxiety more than an adult's.", null),
        Rule("High-mercury fish", mercuryFishTerms, 55, "These fish can be high in mercury. The FDA and EPA advise limiting them for young children, same as in pregnancy.", null),
        Rule("Common choking hazard", chokingHazardTerms, 65, "This is on the common list of choking-hazard foods for young children (AAP/NHS guidance) — cut it up small and supervise eating, don't serve it whole.", 4),
        Rule("Cured or processed meat", curedMeatTerms, 55, "Cured and processed meats contain nitrates/nitrites, which pediatric guidance advises limiting for infants under 1 — their gut handles them differently than an older child's or adult's.", 0),
    )
    val pregnancyRules = listOf(
        Rule("Contains alcohol", AlcoholGuidance.terms, 20, "Health services advise avoiding alcohol in pregnancy, as no amount is known to be safe. Ask your midwife or doctor if unsure.", null),
        Rule("Raw or undercooked", listOf("raw egg", "raw eggs", "uncooked egg", "runny egg", "raw fish", "sushi", "sashimi", "raw oyster", "raw shellfish", "raw meat", "carpaccio", "tartare", "raw sprouts", "raw sprout"), 35, "Raw or undercooked eggs, meat, fish and shellfish are commonly advised against in pregnancy because of food-poisoning risks. Well-cooked versions are usually fine.", null),
        Rule("Unpasteurised dairy", listOf("unpasteurized", "unpasteurised", "raw milk"), 35, "Unpasteurised milk and the cheeses made from it can carry listeria, and are commonly advised against in pregnancy.", null),
        Rule("Liver, pate or retinol", listOf("liver", "pate", "pâté", "paté", "cod liver oil", "retinol"), 55, "Liver, pate and cod liver oil are high in vitamin A, which is commonly advised to be limited in pregnancy.", null),
        Rule("High-mercury fish", mercuryFishTerms, 55, "These fish can be high in mercury and are commonly advised against in pregnancy.", null),
        Rule("Caffeine", caffeineTerms, 65, "Caffeine is usually limited in pregnancy (often to about 200 mg a day, roughly two cups of coffee). Check your total for the day.", null),
    )
    fun childMatches(product: Product, age: Int?) = childRules.filter {
        (it.maxAge == null || age == null || age <= it.maxAge) && contains(product.searchText, it.terms)
    }
    fun pregnancyMatches(product: Product) = pregnancyRules.filter {
        if (it.title == "Contains alcohol") AlcoholGuidance.isAlcoholic(product) else contains(product.searchText, it.terms)
    }
}
