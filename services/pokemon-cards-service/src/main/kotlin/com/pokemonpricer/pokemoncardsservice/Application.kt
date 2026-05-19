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
data class Card(
    val id: String,
    val name: String,
    val set: String,
    val imageUrl: String,
)

@Serializable
data class CardsResponse(val cards: List<Card>)

typealias CardsFetcher = suspend (nameFilter: String?, setFilter: String?, page: Int, numberOfItems: Int) -> List<Card>

fun Application.configureRouting(
    fetchCards: CardsFetcher = TcgDexCardsFetcher().build(),
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
