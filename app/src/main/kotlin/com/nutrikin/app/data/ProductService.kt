package com.nutrikin.app.data

import com.nutrikin.core.Nutrition
import com.nutrikin.core.Product
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.android.Android
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.url
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonPrimitive

sealed class ProductError(message: String) : Exception(message) {
    data object NotFound : ProductError("This product isn't in the database yet.")
    data object BadResponse : ProductError("Couldn't reach the product database. Check your connection.")
}

/**
 * Ported from FamilyFoodScanner/Services/Products/ProductService.swift's barcode fetch (the search and
 * similar-products paths aren't ported yet - see the README). Same endpoint, same field list, same
 * "prefer per-serving over per-100g when present" rule, same User-Agent contact convention Open Food
 * Facts asks every app to identify itself with.
 */
class ProductService {
    private val client = HttpClient(Android)

    private val fields = "code,product_name,brands,image_front_small_url,ingredients_text,ingredients_tags," +
        "additives_tags,allergens_tags,serving_size,nutriments,nutriscore_grade,nova_group,categories_tags,traces_tags"

    suspend fun fetch(barcode: String): Product {
        val code = barcode.filter(Char::isDigit)
        val response = client.get {
            url("https://world.openfoodfacts.org/api/v2/product/$code.json")
            header("User-Agent", "NutriKinAndroid/0.1 (bssundarkumar@gmail.com)")
            url.parameters.append("fields", fields)
        }
        if (response.status == HttpStatusCode.NotFound) throw ProductError.NotFound
        if (!response.status.isSuccess()) throw ProductError.BadResponse

        val body = Json { ignoreUnknownKeys = true }.parseToJsonElement(response.body<String>())
            as? kotlinx.serialization.json.JsonObject ?: throw ProductError.BadResponse
        val status = body["status"]?.jsonPrimitive?.doubleOrNull?.toInt() ?: 0
        val product = body["product"] as? kotlinx.serialization.json.JsonObject
        if (status != 1 || product == null) throw ProductError.NotFound

        return toProduct(code, product)
    }

    private fun toProduct(barcode: String, p: kotlinx.serialization.json.JsonObject): Product {
        fun str(key: String): String? = (p[key] as? JsonPrimitive)?.contentOrNull?.takeIf { it.isNotBlank() }
        fun tags(key: String): List<String> =
            (p[key] as? kotlinx.serialization.json.JsonArray)?.mapNotNull { it.jsonPrimitive.contentOrNull } ?: emptyList()

        val nutriments = p["nutriments"] as? kotlinx.serialization.json.JsonObject
        fun nut(key: String): Double? = (nutriments?.get(key) as? JsonPrimitive)?.doubleOrNull

        val useServing = nut("energy-kcal_serving") != null
        val suffix = if (useServing) "_serving" else "_100g"
        val servingSize = str("serving_size")
        val basis = if (useServing) "per serving" + (servingSize?.let { " ($it)" } ?: "") else "per 100 g"

        fun nutritionFor(suffix: String, basis: String) = Nutrition(
            calories = nut("energy-kcal$suffix"),
            sugarG = nut("sugars$suffix"),
            carbsG = nut("carbohydrates$suffix"),
            sodiumMg = nut("sodium$suffix")?.times(1000),
            satFatG = nut("saturated-fat$suffix"),
            proteinG = nut("proteins$suffix"),
            basis = basis,
        )

        val name = str("product_name") ?: "Unnamed product"
        val brand = str("brands")?.split(",")?.firstOrNull()?.trim()

        return Product(
            barcode = barcode,
            name = name,
            brand = brand,
            ingredientsText = str("ingredients_text"),
            allergenTags = tags("allergens_tags"),
            nutrition = nutritionFor(suffix, basis),
            ingredientTags = tags("ingredients_tags"),
            categoryTags = tags("categories_tags"),
        )
    }
}
