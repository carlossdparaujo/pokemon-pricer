package com.pokemonpricer.pokemoncardsservice

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
private data class TcgDexCardBrief(val id: String, val name: String, val image: String? = null)

@Serializable
private data class TcgDexSetBrief(val id: String, val name: String)

class TcgDexCardsFetcher {

    private val client = HttpClient(CIO) {
        install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
    }

    fun build(): CardsFetcher = { nameFilter, setFilter, page, numberOfItems ->
        if (setFilter != null) fetchBySetFilter(setFilter, nameFilter, page, numberOfItems)
        else fetchByNameFilter(nameFilter, page, numberOfItems)
    }

    private suspend fun fetchBySetFilter(setFilter: String, nameFilter: String?, page: Int, numberOfItems: Int): List<Card> =
        findSetsMatching(setFilter, page, numberOfItems).flatMap { set -> fetchCardsInSet(set, nameFilter, page, numberOfItems) }

    private suspend fun fetchByNameFilter(nameFilter: String?, page: Int, numberOfItems: Int): List<Card> {
        val cards = fetchCardsBriefByName(nameFilter, page, numberOfItems)
        val setNames = resolveSetNames(cards)
        return cards.map { toCard(it, setNames) }
    }

    private suspend fun findSetsMatching(setFilter: String, page: Int, numberOfItems: Int): List<TcgDexSetBrief> =
        client.get("$BASE_URL/sets") {
            parameter("name", setFilter)
            paginationParams(page, numberOfItems)
        }.body()

    private suspend fun fetchCardsInSet(set: TcgDexSetBrief, nameFilter: String?, page: Int, numberOfItems: Int): List<Card> =
        client.get("$BASE_URL/sets/${set.id}/cards") {
            if (nameFilter != null) parameter("name", nameFilter)
            paginationParams(page, numberOfItems)
        }.body<List<TcgDexCardBrief>>()
            .map { card -> Card(id = card.id, name = card.name, set = set.name, imageUrl = imageUrl(card.image), priceSummary = zeroPriceSummary(card.id)) }

    private suspend fun fetchCardsBriefByName(nameFilter: String?, page: Int, numberOfItems: Int): List<TcgDexCardBrief> =
        client.get("$BASE_URL/cards") {
            if (nameFilter != null) parameter("name", nameFilter)
            paginationParams(page, numberOfItems)
        }.body()

    private fun HttpRequestBuilder.paginationParams(page: Int, numberOfItems: Int) {
        parameter("pagination:page", page)
        parameter("pagination:itemsPerPage", numberOfItems)
    }

    // The /cards endpoint returns no set info — only the card ID encodes the set (e.g. "base1-4" → set "base1").
    // We batch-fetch each unique set to get the display name (e.g. "Base Set") instead of exposing the raw ID.
    private suspend fun resolveSetNames(cards: List<TcgDexCardBrief>): Map<String, String> =
        cards.map { setIdFromCardId(it.id) }.distinct().associateWith { setId ->
            runCatching { client.get("$BASE_URL/sets/$setId").body<TcgDexSetBrief>().name }.getOrElse { setId }
        }

    private fun toCard(brief: TcgDexCardBrief, setNames: Map<String, String>): Card {
        val setId = setIdFromCardId(brief.id)
        return Card(id = brief.id, name = brief.name, set = setNames[setId] ?: setId, imageUrl = imageUrl(brief.image), priceSummary = zeroPriceSummary(brief.id))
    }

    private fun setIdFromCardId(cardId: String): String = cardId.substringBeforeLast("-")

    private fun imageUrl(image: String?): String = image?.let { "$it/high.webp" } ?: ""

    private fun zeroPriceSummary(cardId: String) = PriceSummary(cardId = cardId, average = 0.0, p10 = 0.0, p50 = 0.0, p90 = 0.0, p99 = 0.0)

    companion object {
        private const val BASE_URL = "https://api.tcgdex.net/v2/en"
    }
}
