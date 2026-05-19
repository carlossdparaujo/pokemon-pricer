package com.pokemonpricer.pokemoncardsservice

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class PriceSummary(
    val cardId: String,
    val average: Double,
    val p10: Double,
    val p50: Double,
    val p90: Double,
    val p99: Double,
)

@Serializable
data class Card(
    val id: String,
    val name: String,
    val set: String,
    val imageUrl: String,
    val priceSummary: PriceSummary,
)

@Serializable
data class CardsResponse(val cards: List<Card>)

val stubCards = listOf(
    Card(
        id = "base1-4",
        name = "Charizard",
        set = "Base Set",
        imageUrl = "https://images.pokemontcg.io/base1/4.png",
        priceSummary = PriceSummary(
            cardId = "base1-4",
            average = 350.0,
            p10 = 200.0,
            p50 = 330.0,
            p90 = 500.0,
            p99 = 750.0,
        ),
    ),
    Card(
        id = "base1-25",
        name = "Dewgong",
        set = "Base Set",
        imageUrl = "https://images.pokemontcg.io/base1/25.png",
        priceSummary = PriceSummary(
            cardId = "base1-25",
            average = 2.5,
            p10 = 1.0,
            p50 = 2.25,
            p90 = 4.0,
            p99 = 6.5,
        ),
    ),
    Card(
        id = "neo1-16",
        name = "Togetic",
        set = "Neo Genesis",
        imageUrl = "https://images.pokemontcg.io/neo1/16.png",
        priceSummary = PriceSummary(
            cardId = "neo1-16",
            average = 8.0,
            p10 = 3.5,
            p50 = 7.5,
            p90 = 13.0,
            p99 = 20.0,
        ),
    ),
)

fun Application.configureRouting() {
    routing {
        get("/cards") {
            val nameFilter = call.request.queryParameters["name"]?.lowercase()
            val setFilter = call.request.queryParameters["set"]?.lowercase()

            val filtered = stubCards.filter { card ->
                (nameFilter == null || card.name.lowercase().contains(nameFilter)) &&
                (setFilter == null || card.set.lowercase().contains(setFilter))
            }

            call.respond(CardsResponse(cards = filtered))
        }
    }
}

fun Application.configureSerialization() {
    install(ContentNegotiation) {
        json(Json { prettyPrint = true })
    }
}

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0") {
        configureSerialization()
        configureRouting()
    }.start(wait = true)
}
