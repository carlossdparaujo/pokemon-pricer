package com.pokemonpricer.pokemoncardsservice

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation as ClientContentNegotiation
import io.ktor.client.request.*
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

@Serializable
private data class TcgDexCardBrief(val id: String, val name: String, val image: String? = null)

@Serializable
private data class TcgDexSetBrief(val id: String, val name: String)

typealias CardsFetcher = suspend (nameFilter: String?, setFilter: String?) -> List<Card>

private const val TCGDEX_BASE_URL = "https://api.tcgdex.net/v2/en"

private val tcgDexJson = Json { ignoreUnknownKeys = true }

fun buildCardsFetcher(): CardsFetcher {
    val client = HttpClient(CIO) {
        install(ClientContentNegotiation) { json(tcgDexJson) }
    }
    return { nameFilter, setFilter ->
        if (setFilter != null) {
            val sets = client.get("$TCGDEX_BASE_URL/sets") {
                parameter("name", setFilter)
            }.body<List<TcgDexSetBrief>>()
            sets.flatMap { set ->
                client.get("$TCGDEX_BASE_URL/sets/${set.id}/cards") {
                    if (nameFilter != null) parameter("name", nameFilter)
                }.body<List<TcgDexCardBrief>>()
                    .map { card ->
                        Card(
                            id = card.id,
                            name = card.name,
                            set = set.name,
                            imageUrl = card.image?.let { "$it/high.webp" } ?: "",
                        )
                    }
            }
        } else {
            val cards = client.get("$TCGDEX_BASE_URL/cards") {
                if (nameFilter != null) parameter("name", nameFilter)
            }.body<List<TcgDexCardBrief>>()
            val setIds = cards.map { it.id.substringBeforeLast("-") }.distinct()
            val setNames = setIds.associateWith { setId ->
                runCatching { client.get("$TCGDEX_BASE_URL/sets/$setId").body<TcgDexSetBrief>().name }.getOrElse { setId }
            }
            cards.map { card ->
                val setId = card.id.substringBeforeLast("-")
                Card(
                    id = card.id,
                    name = card.name,
                    set = setNames[setId] ?: setId,
                    imageUrl = card.image?.let { "$it/high.webp" } ?: "",
                )
            }
        }
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
