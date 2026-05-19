package com.pokemonpricer.pokemonpricesservice

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
}
