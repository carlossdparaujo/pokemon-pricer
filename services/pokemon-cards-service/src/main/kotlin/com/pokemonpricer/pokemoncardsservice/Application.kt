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
import net.tcgdex.sdk.TCGdex

@Serializable
data class Card(
    val id: String,
    val name: String,
    val set: String,
    val imageUrl: String,
)

@Serializable
data class CardsResponse(val cards: List<Card>)

// Inject for testability
typealias CardsFetcher = (nameFilter: String?, setFilter: String?) -> List<Card>

fun buildCardsFetcher(): CardsFetcher = { nameFilter, setFilter ->
    val api = TCGdex("en")
    val resumes = api.fetchCards() ?: emptyArray()

    // Filter by name in-memory first (cheap)
    val nameMatched = resumes.filter { card ->
        nameFilter == null || card.name.lowercase().contains(nameFilter)
    }

    // For each name-matched card, fetch full card to get set name (needed for set filter + response)
    nameMatched.mapNotNull { resume ->
        api.fetchCard(resume.id)
    }.filter { card ->
        setFilter == null || card.set.name.lowercase().contains(setFilter)
    }.map { card ->
        Card(
            id = card.id,
            name = card.name,
            set = card.set.name,
            imageUrl = card.image?.let { "$it/high.webp" } ?: "",
        )
    }
}

fun Application.configureRouting(fetchCards: CardsFetcher = buildCardsFetcher()) {
    routing {
        get("/cards") {
            val nameFilter = call.request.queryParameters["name"]?.lowercase()
            val setFilter = call.request.queryParameters["set"]?.lowercase()
            call.respond(CardsResponse(cards = fetchCards(nameFilter, setFilter)))
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
