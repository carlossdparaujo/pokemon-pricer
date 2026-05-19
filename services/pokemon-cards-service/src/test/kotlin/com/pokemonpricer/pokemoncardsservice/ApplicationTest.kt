package com.pokemonpricer.pokemoncardsservice

import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.testing.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ApplicationTest {

    private fun testApp(
        fetcher: CardsFetcher,
        block: suspend ApplicationTestBuilder.() -> Unit,
    ) = testApplication {
        application {
            configureSerialization()
            configureRouting(fetchCards = fetcher)
        }
        block()
    }

    private fun card(id: String, name: String, set: String = "Base Set") = Card(
        id = id, name = name, set = set,
        imageUrl = "https://img/$id.png",
    )

    @Test
    fun `returns cards matching name filter`() = testApp(
        fetcher = { nameFilter, _, _, _ ->
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
        fetcher = { _, _, _, _ -> listOf(card("base1-4", "Charizard"), card("base1-25", "Pikachu")) }
    ) {
        val client = createClient { install(ContentNegotiation) { json() } }
        val res = client.get("/cards")
        assertEquals(HttpStatusCode.OK, res.status)
        assertEquals(2, res.body<CardsResponse>().cards.size)
    }

    @Test
    fun `returns empty list when no cards match`() = testApp(
        fetcher = { _, _, _, _ -> emptyList() }
    ) {
        val client = createClient { install(ContentNegotiation) { json() } }
        val res = client.get("/cards?name=mewtwo")
        assertEquals(HttpStatusCode.OK, res.status)
        assertTrue(res.body<CardsResponse>().cards.isEmpty())
    }

    @Test
    fun `set filter is passed through to fetcher`() = testApp(
        fetcher = { _, setFilter, _, _ ->
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
        fetcher = { _, _, _, _ -> (1..20).map { i -> card("set1-$i", "Card $i", "Set One") } }
    ) {
        val client = createClient { install(ContentNegotiation) { json() } }
        val res = client.get("/cards")
        assertEquals(HttpStatusCode.OK, res.status)
        assertEquals(20, res.body<CardsResponse>().cards.size)
    }

    @Test
    fun `response card shape has expected fields`() = testApp(
        fetcher = { _, _, _, _ -> listOf(card("base1-4", "Charizard")) }
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
    fun `page param is forwarded to fetcher`() {
        var capturedPage = 0
        testApp(
            fetcher = { _, _, page, _ ->
                capturedPage = page
                emptyList()
            }
        ) {
            val client = createClient { install(ContentNegotiation) { json() } }
            client.get("/cards?page=3")
            assertEquals(3, capturedPage)
        }
    }

    @Test
    fun `numberOfItems param is forwarded to fetcher`() {
        var capturedNumberOfItems = 0
        testApp(
            fetcher = { _, _, _, numberOfItems ->
                capturedNumberOfItems = numberOfItems
                emptyList()
            }
        ) {
            val client = createClient { install(ContentNegotiation) { json() } }
            client.get("/cards?numberOfItems=50")
            assertEquals(50, capturedNumberOfItems)
        }
    }

    @Test
    fun `page defaults to 1 when not provided`() {
        var capturedPage = 0
        testApp(
            fetcher = { _, _, page, _ ->
                capturedPage = page
                emptyList()
            }
        ) {
            val client = createClient { install(ContentNegotiation) { json() } }
            client.get("/cards")
            assertEquals(1, capturedPage)
        }
    }

    @Test
    fun `numberOfItems defaults to 20 when not provided`() {
        var capturedNumberOfItems = 0
        testApp(
            fetcher = { _, _, _, numberOfItems ->
                capturedNumberOfItems = numberOfItems
                emptyList()
            }
        ) {
            val client = createClient { install(ContentNegotiation) { json() } }
            client.get("/cards")
            assertEquals(20, capturedNumberOfItems)
        }
    }
}
