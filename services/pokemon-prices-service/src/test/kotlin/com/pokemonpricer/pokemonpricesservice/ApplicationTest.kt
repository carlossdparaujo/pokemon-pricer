package com.pokemonpricer.pokemonpricesservice

import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.testing.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ApplicationTest {

    private fun testApp(block: suspend ApplicationTestBuilder.() -> Unit) =
        testApplication {
            application {
                configureSerialization()
                configureRouting()
            }
            block()
        }

    @Test
    fun `GET prices cardId returns 200 with matching cardId`() = testApp {
        val client = createClient { install(ContentNegotiation) { json() } }
        val res = client.get("/prices/base1-4")
        assertEquals(HttpStatusCode.OK, res.status)
        val body = res.body<PriceSummary>()
        assertEquals("base1-4", body.cardId)
    }

    @Test
    fun `GET prices cardId returns non-zero price fields`() = testApp {
        val client = createClient { install(ContentNegotiation) { json() } }
        val res = client.get("/prices/base1-4")
        assertEquals(HttpStatusCode.OK, res.status)
        val body = res.body<PriceSummary>()
        assertTrue(body.average > 0.0, "average should be non-zero")
        assertTrue(body.p10 > 0.0, "p10 should be non-zero")
        assertTrue(body.p50 > 0.0, "p50 should be non-zero")
        assertTrue(body.p90 > 0.0, "p90 should be non-zero")
        assertTrue(body.p99 > 0.0, "p99 should be non-zero")
    }

    @Test
    fun `GET prices cardId uses path parameter as cardId`() = testApp {
        val client = createClient { install(ContentNegotiation) { json() } }
        val res = client.get("/prices/xy1-99")
        assertEquals(HttpStatusCode.OK, res.status)
        val body = res.body<PriceSummary>()
        assertEquals("xy1-99", body.cardId)
    }

    @Test
    fun `POST prices batch returns 200 with price summary for each requested card`() = testApp {
        val client = createClient { install(ContentNegotiation) { json() } }
        val res = client.post("/prices/batch") {
            contentType(ContentType.Application.Json)
            setBody(
                listOf(
                    CardPriceRequest(cardId = "base1-4", name = "Charizard", set = "Base Set"),
                    CardPriceRequest(cardId = "base1-25", name = "Pikachu", set = "Base Set"),
                )
            )
        }
        assertEquals(HttpStatusCode.OK, res.status)
        val body = res.body<JsonObject>()
        assertTrue(body.containsKey("base1-4"), "response should include base1-4")
        assertTrue(body.containsKey("base1-25"), "response should include base1-25")
    }

    @Test
    fun `POST prices batch returns non-zero price fields for each card`() = testApp {
        val client = createClient { install(ContentNegotiation) { json() } }
        val res = client.post("/prices/batch") {
            contentType(ContentType.Application.Json)
            setBody(listOf(CardPriceRequest(cardId = "base1-4", name = "Charizard", set = "Base Set")))
        }
        assertEquals(HttpStatusCode.OK, res.status)
        val body = res.body<JsonObject>()
        val price = Json.decodeFromJsonElement<PriceSummary>(body["base1-4"]!!)
        assertTrue(price.average > 0.0)
        assertTrue(price.p10 > 0.0)
        assertTrue(price.p50 > 0.0)
        assertTrue(price.p90 > 0.0)
        assertTrue(price.p99 > 0.0)
    }

    @Test
    fun `POST prices batch with empty list returns empty object`() = testApp {
        val client = createClient { install(ContentNegotiation) { json() } }
        val res = client.post("/prices/batch") {
            contentType(ContentType.Application.Json)
            setBody(emptyList<CardPriceRequest>())
        }
        assertEquals(HttpStatusCode.OK, res.status)
        val body = res.body<JsonObject>()
        assertTrue(body.isEmpty(), "response should be empty for empty request")
    }
}
