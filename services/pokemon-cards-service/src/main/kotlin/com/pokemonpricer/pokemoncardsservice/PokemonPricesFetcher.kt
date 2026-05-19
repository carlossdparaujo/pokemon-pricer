package com.pokemonpricer.pokemoncardsservice

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

class PokemonPricesFetcher(
    private val baseUrl: String = System.getenv("POKEMON_PRICES_SERVICE_URL") ?: "http://localhost:8081",
) {

    private val client = HttpClient(CIO) {
        install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
    }

    fun build(): PricesFetcher = { cardId ->
        client.get("$baseUrl/prices/$cardId").body()
    }
}
