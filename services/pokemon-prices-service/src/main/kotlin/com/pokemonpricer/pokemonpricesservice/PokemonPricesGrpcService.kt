package com.pokemonpricer.pokemonpricesservice

import com.pokemonpricer.pokemonpricesservice.grpc.GetPricesBatchRequest
import com.pokemonpricer.pokemonpricesservice.grpc.GetPricesBatchResponse
import com.pokemonpricer.pokemonpricesservice.grpc.PokemonPricesServiceGrpcKt
import com.pokemonpricer.pokemonpricesservice.grpc.PriceSummary
import com.pokemonpricer.pokemonpricesservice.grpc.priceSummary
import com.pokemonpricer.pokemonpricesservice.grpc.getPricesBatchResponse
import kotlinx.coroutines.delay

class PokemonPricesGrpcService : PokemonPricesServiceGrpcKt.PokemonPricesServiceCoroutineImplBase() {

    override suspend fun getPricesBatch(request: GetPricesBatchRequest): GetPricesBatchResponse {
        delay(1_000)
        val prices = request.cardsList.associate { card ->
            card.cardId to stubPriceSummary(card.cardId)
        }
        return getPricesBatchResponse { this.prices.putAll(prices) }
    }

    private fun stubPriceSummary(cardId: String): PriceSummary = priceSummary {
        this.cardId = cardId
        average = 5.0
        p10 = 1.0
        p50 = 4.5
        p90 = 12.0
        p99 = 25.0
    }
}
