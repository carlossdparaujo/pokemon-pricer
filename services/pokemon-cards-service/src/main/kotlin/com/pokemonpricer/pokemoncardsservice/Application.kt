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
data class Card(
    val id: String,
    val name: String,
    val set: String,
    val imageUrl: String,
)

@Serializable
data class CardsResponse(val cards: List<Card>)

val stubCards = listOf(
    Card(
        id = "base1-4",
        name = "Charizard",
        set = "Base Set",
        imageUrl = "https://images.pokemontcg.io/base1/4.png",
    ),
    Card(
        id = "base1-25",
        name = "Pikachu",
        set = "Base Set",
        imageUrl = "https://images.pokemontcg.io/base1/25.png",
    ),
    Card(
        id = "neo1-16",
        name = "Pikachu",
        set = "Neo Genesis",
        imageUrl = "https://images.pokemontcg.io/neo1/16.png",
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
