package com.pokemonpricer.pokemoncardsservice

import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
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

typealias CardsFetcher = suspend (nameFilter: String?, setFilter: String?, page: Int, numberOfItems: Int) -> List<Card>

typealias PricesFetcher = suspend (cardId: String) -> PriceSummary

fun Application.configureRouting(
    pricesFetcher: PricesFetcher = PokemonPricesFetcher().build(),
    fetchCards: CardsFetcher = TcgDexCardsFetcher(pricesFetcher).build(),
) {
    routing {
        get("/cards") {
            val nameFilter = call.request.queryParameters["name"]?.lowercase()
            val setFilter = call.request.queryParameters["set"]?.lowercase()
            val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
            val numberOfItems = call.request.queryParameters["numberOfItems"]?.toIntOrNull() ?: 20
            call.respond(CardsResponse(cards = fetchCards(nameFilter, setFilter, page, numberOfItems)))
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
