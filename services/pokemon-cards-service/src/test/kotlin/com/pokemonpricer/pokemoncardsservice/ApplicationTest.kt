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

    private fun testApp(block: suspend ApplicationTestBuilder.() -> Unit) = testApplication {
        application {
            configureSerialization()
            configureRouting()
        }
        block()
    }

    private fun ApplicationTestBuilder.jsonClient() = createClient {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    @Test
    fun `GET cards with no filters returns all stub cards`() = testApp {
        val client = jsonClient()
        val response = client.get("/cards")

        assertEquals(HttpStatusCode.OK, response.status)

        val body = response.body<CardsResponse>()
        assertEquals(3, body.cards.size)
    }

    @Test
    fun `GET cards filtered by name returns matching cards`() = testApp {
        val client = jsonClient()
        val response = client.get("/cards?name=pikachu")

        assertEquals(HttpStatusCode.OK, response.status)

        val body = response.body<CardsResponse>()
        assertEquals(2, body.cards.size)
        assertTrue(body.cards.all { it.name.lowercase().contains("pikachu") })
    }

    @Test
    fun `GET cards filtered by set returns matching cards`() = testApp {
        val client = jsonClient()
        val response = client.get("/cards?set=Base+Set")

        assertEquals(HttpStatusCode.OK, response.status)

        val body = response.body<CardsResponse>()
        assertEquals(2, body.cards.size)
        assertTrue(body.cards.all { it.set.lowercase().contains("base set") })
    }

    @Test
    fun `GET cards filtered by name and set returns intersection`() = testApp {
        val client = jsonClient()
        val response = client.get("/cards?name=pikachu&set=Base+Set")

        assertEquals(HttpStatusCode.OK, response.status)

        val body = response.body<CardsResponse>()
        assertEquals(1, body.cards.size)
        assertEquals("base1-25", body.cards.first().id)
    }

    @Test
    fun `GET cards with non-matching filter returns empty list`() = testApp {
        val client = jsonClient()
        val response = client.get("/cards?name=mewtwo")

        assertEquals(HttpStatusCode.OK, response.status)

        val body = response.body<CardsResponse>()
        assertTrue(body.cards.isEmpty())
    }

    @Test
    fun `response card shape has expected fields`() = testApp {
        val client = jsonClient()
        val response = client.get("/cards?name=charizard")

        assertEquals(HttpStatusCode.OK, response.status)

        val body = response.body<CardsResponse>()
        assertEquals(1, body.cards.size)

        val card = body.cards.first()
        assertEquals("base1-4", card.id)
        assertEquals("Charizard", card.name)
        assertEquals("Base Set", card.set)
        assertTrue(card.imageUrl.isNotBlank())
    }

    @Test
    fun `response card has priceSummary with all required fields`() = testApp {
        val client = jsonClient()
        val response = client.get("/cards?name=charizard")

        assertEquals(HttpStatusCode.OK, response.status)

        val body = response.body<CardsResponse>()
        assertEquals(1, body.cards.size)

        val priceSummary = body.cards.first().priceSummary
        assertEquals("base1-4", priceSummary.cardId)
        assertTrue(priceSummary.average > 0.0)
        assertTrue(priceSummary.p10 > 0.0)
        assertTrue(priceSummary.p50 > 0.0)
        assertTrue(priceSummary.p90 > 0.0)
        assertTrue(priceSummary.p99 > 0.0)
        assertTrue(priceSummary.p10 < priceSummary.p50)
        assertTrue(priceSummary.p50 < priceSummary.p90)
        assertTrue(priceSummary.p90 < priceSummary.p99)
    }

    @Test
    fun `all stub cards have priceSummary`() = testApp {
        val client = jsonClient()
        val response = client.get("/cards")

        assertEquals(HttpStatusCode.OK, response.status)

        val body = response.body<CardsResponse>()
        assertTrue(body.cards.isNotEmpty())
        assertTrue(body.cards.all { it.priceSummary.cardId == it.id })
        assertTrue(body.cards.all { it.priceSummary.average > 0.0 })
    }
}
