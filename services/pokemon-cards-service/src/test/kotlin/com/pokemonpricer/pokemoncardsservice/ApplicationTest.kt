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

    @Test
    fun `returns cards matching name filter`() = testApp(
        fetcher = { nameFilter, _ ->
            listOf(
                Card("base1-4", "Charizard", "Base Set", "https://img/charizard.png"),
                Card("base1-25", "Pikachu", "Base Set", "https://img/pikachu.png"),
            ).filter { nameFilter == null || it.name.lowercase().contains(nameFilter) }
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
        fetcher = { _, _ ->
            listOf(
                Card("base1-4", "Charizard", "Base Set", "https://img/charizard.png"),
                Card("base1-25", "Pikachu", "Base Set", "https://img/pikachu.png"),
            )
        }
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
            listOf(
                Card("base1-4", "Charizard", "Base Set", "https://img/charizard.png"),
                Card("neo1-16", "Togetic", "Neo Genesis", "https://img/togetic.png"),
            ).filter { setFilter == null || it.set.lowercase().contains(setFilter) }
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
    fun `response card shape has expected fields`() = testApp(
        fetcher = { _, _ ->
            listOf(Card("base1-4", "Charizard", "Base Set", "https://img/charizard.png"))
        }
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
}
