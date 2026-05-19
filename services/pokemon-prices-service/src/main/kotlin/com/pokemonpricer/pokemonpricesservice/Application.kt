package com.pokemonpricer.pokemonpricesservice

import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class PriceSummary(
    val cardId: String,
    val average: Double,
    val p10: Double,
    val p50: Double,
    val p90: Double,
    val p99: Double,
)

@Serializable
data class CardPriceRequest(
    val cardId: String,
    val name: String,
    val set: String,
)

fun Application.configureRouting() {
    routing {
        get("/prices/{cardId}") {
            val cardId = call.parameters["cardId"]!!
            call.respond(stubPriceSummary(cardId))
        }
        post("/prices/batch") {
            delay(1_000)
            val requests = call.receive<List<CardPriceRequest>>()
            val prices = requests.associate { req -> req.cardId to stubPriceSummary(req.cardId) }
            call.respond(prices)
        }
    }
}

private fun stubPriceSummary(cardId: String) = PriceSummary(
    cardId = cardId,
    average = 5.0,
    p10 = 1.0,
    p50 = 4.5,
    p90 = 12.0,
    p99 = 25.0,
)

fun Application.configureSerialization() {
    install(ContentNegotiation) {
        json(Json { prettyPrint = true })
    }
}

fun main() {
    embeddedServer(Netty, port = 8081, host = "0.0.0.0") {
        configureSerialization()
        configureRouting()
    }.start(wait = true)
}
