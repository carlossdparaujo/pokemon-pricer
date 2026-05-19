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

    fun build(): CardsFetcher = { nameFilter, setFilter ->
        if (setFilter != null) fetchBySetFilter(setFilter, nameFilter)
        else fetchByNameFilter(nameFilter)
    }

    private suspend fun fetchBySetFilter(setFilter: String, nameFilter: String?): List<Card> =
        findSetsMatching(setFilter).flatMap { set -> fetchCardsInSet(set, nameFilter) }

    private suspend fun fetchByNameFilter(nameFilter: String?): List<Card> {
        val cards = fetchCardsBriefByName(nameFilter)
        val setNames = resolveSetNames(cards)
        return cards.map { toCard(it, setNames) }
    }

    private suspend fun findSetsMatching(setFilter: String): List<TcgDexSetBrief> =
        client.get("$BASE_URL/sets") {
            parameter("name", setFilter)
            firstPage()
        }.body()

    private suspend fun fetchCardsInSet(set: TcgDexSetBrief, nameFilter: String?): List<Card> =
        client.get("$BASE_URL/sets/${set.id}/cards") {
            if (nameFilter != null) parameter("name", nameFilter)
            firstPage()
        }.body<List<TcgDexCardBrief>>()
            .map { card -> Card(id = card.id, name = card.name, set = set.name, imageUrl = imageUrl(card.image)) }

    private suspend fun fetchCardsBriefByName(nameFilter: String?): List<TcgDexCardBrief> =
        client.get("$BASE_URL/cards") {
            if (nameFilter != null) parameter("name", nameFilter)
            firstPage()
        }.body()

    private fun HttpRequestBuilder.firstPage() {
        parameter("pagination:page", 1)
        parameter("pagination:itemsPerPage", PAGE_SIZE)
    }

    // The /cards endpoint returns no set info — only the card ID encodes the set (e.g. "base1-4" → set "base1").
    // We batch-fetch each unique set to get the display name (e.g. "Base Set") instead of exposing the raw ID.
    private suspend fun resolveSetNames(cards: List<TcgDexCardBrief>): Map<String, String> =
        cards.map { setIdFromCardId(it.id) }.distinct().associateWith { setId ->
            runCatching { client.get("$BASE_URL/sets/$setId").body<TcgDexSetBrief>().name }.getOrElse { setId }
        }

    private fun toCard(brief: TcgDexCardBrief, setNames: Map<String, String>): Card {
        val setId = setIdFromCardId(brief.id)
        return Card(id = brief.id, name = brief.name, set = setNames[setId] ?: setId, imageUrl = imageUrl(brief.image))
    }

    private fun setIdFromCardId(cardId: String): String = cardId.substringBeforeLast("-")

    private fun imageUrl(image: String?): String = image?.let { "$it/high.webp" } ?: ""

    companion object {
        private const val BASE_URL = "https://api.tcgdex.net/v2/en"
        private const val PAGE_SIZE = 20
    }
}
