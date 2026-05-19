package com.pokemonpricer.pokemoncardsservice

import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.testing.*
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ApplicationTest {

    private fun testApp(fetcher: CardsFetcher, block: suspend ApplicationTestBuilder.() -> Unit) =
        testApplication {
            application {
                configureSerialization()
                configureRouting(fetcher)
            }
            block()
        }

    private fun card(id: String, name: String, set: String = "Base Set") = Card(
        id = id, name = name, set = set,
        imageUrl = "https://img/$id.png",
        priceSummary = PriceSummary(cardId = id, average = 0.0, p10 = 0.0, p50 = 0.0, p90 = 0.0, p99 = 0.0),
    )

    @Test
    fun `returns cards matching name filter`() = testApp(
        fetcher = { nameFilter, _ ->
            listOf(card("base1-4", "Charizard"), card("base1-25", "Pikachu"))
                .filter { nameFilter == null || it.name.lowercase().contains(nameFilter) }
        }
    ) {
        val client = createClient { install(ContentNegotiation) { json() } }
        val res = client.get("/cards?name=pikachu")
        assertEquals(HttpStatusCode.OK, res.status)
        val body = res.body<CardsResponse>()
        assertEquals(1, body.cards.size)
        assertEquals("Pikachu", body.cards.first().name)
    }

    @Test
    fun `returns all cards when no filter given`() = testApp(
        fetcher = { _, _ -> listOf(card("base1-4", "Charizard"), card("base1-25", "Pikachu")) }
    ) {
        val client = createClient { install(ContentNegotiation) { json() } }
        val res = client.get("/cards")
        assertEquals(HttpStatusCode.OK, res.status)
        assertEquals(2, res.body<CardsResponse>().cards.size)
    }

    @Test
    fun `returns empty list when no cards match`() = testApp(
        fetcher = { _, _ -> emptyList() }
    ) {
        val client = createClient { install(ContentNegotiation) { json() } }
        val res = client.get("/cards?name=mewtwo")
        assertEquals(HttpStatusCode.OK, res.status)
        assertTrue(res.body<CardsResponse>().cards.isEmpty())
    }

    @Test
    fun `set filter is passed through to fetcher`() = testApp(
        fetcher = { _, setFilter ->
            listOf(card("base1-4", "Charizard"), card("neo1-16", "Togetic", "Neo Genesis"))
                .filter { setFilter == null || it.set.lowercase().contains(setFilter) }
        }
    ) {
        val client = createClient { install(ContentNegotiation) { json() } }
        val res = client.get("/cards?set=neo+genesis")
        assertEquals(HttpStatusCode.OK, res.status)
        val body = res.body<CardsResponse>()
        assertEquals(1, body.cards.size)
        assertEquals("Togetic", body.cards.first().name)
    }

    @Test
    fun `returns all cards provided by fetcher up to page size`() = testApp(
        fetcher = { _, _ -> (1..20).map { i -> card("set1-$i", "Card $i", "Set One") } }
    ) {
        val client = createClient { install(ContentNegotiation) { json() } }
        val res = client.get("/cards")
        assertEquals(HttpStatusCode.OK, res.status)
        assertEquals(20, res.body<CardsResponse>().cards.size)
    }

    @Test
    fun `response card shape has expected fields`() = testApp(
        fetcher = { _, _ -> listOf(card("base1-4", "Charizard")) }
    ) {
        val client = createClient { install(ContentNegotiation) { json() } }
        val res = client.get("/cards?name=charizard")
        assertEquals(HttpStatusCode.OK, res.status)
        val body = res.body<CardsResponse>()
        assertEquals(1, body.cards.size)
        val card = body.cards.first()
        assertEquals("base1-4", card.id)
        assertEquals("Charizard", card.name)
        assertEquals("Base Set", card.set)
        assertTrue(card.imageUrl.isNotBlank())
    }

    @Test
    fun `response card has priceSummary with all required fields`() = testApp(
        fetcher = { _, _ ->
            listOf(
                Card(
                    id = "base1-4", name = "Charizard", set = "Base Set",
                    imageUrl = "https://img/charizard.png",
                    priceSummary = PriceSummary(cardId = "base1-4", average = 350.0, p10 = 200.0, p50 = 330.0, p90 = 500.0, p99 = 750.0),
                )
            )
        }
    ) {
        val client = createClient { install(ContentNegotiation) { json() } }
        val res = client.get("/cards?name=charizard")
        assertEquals(HttpStatusCode.OK, res.status)
        val body = res.body<CardsResponse>()
        assertEquals(1, body.cards.size)
        val priceSummary = body.cards.first().priceSummary
        assertEquals("base1-4", priceSummary.cardId)
        assertTrue(priceSummary.average > 0.0)
        assertTrue(priceSummary.p10 > 0.0)
        assertTrue(priceSummary.p50 > 0.0)
        assertTrue(priceSummary.p90 > 0.0)
        assertTrue(priceSummary.p99 > 0.0)
    }

    @Test
    fun `all returned cards have priceSummary`() = testApp(
        fetcher = { _, _ ->
            listOf(
                Card("base1-4", "Charizard", "Base Set", "https://img/c.png", PriceSummary("base1-4", 350.0, 200.0, 330.0, 500.0, 750.0)),
                Card("base1-25", "Dewgong", "Base Set", "https://img/d.png", PriceSummary("base1-25", 2.5, 1.0, 2.25, 4.0, 6.5)),
            )
        }
    ) {
        val client = createClient { install(ContentNegotiation) { json() } }
        val res = client.get("/cards")
        assertEquals(HttpStatusCode.OK, res.status)
        val body = res.body<CardsResponse>()
        assertTrue(body.cards.isNotEmpty())
        assertTrue(body.cards.all { it.priceSummary.cardId == it.id })
        assertTrue(body.cards.all { it.priceSummary.average > 0.0 })
    }
}
