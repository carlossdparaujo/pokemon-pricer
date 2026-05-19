package com.pokemonpricer.pokemonpricesservice

import com.pokemonpricer.pokemonpricesservice.grpc.GetPricesBatchRequest
import com.pokemonpricer.pokemonpricesservice.grpc.GetPricesBatchResponse
import com.pokemonpricer.pokemonpricesservice.grpc.PokemonPricesServiceGrpcKt
import com.pokemonpricer.pokemonpricesservice.grpc.PriceSummary
import com.pokemonpricer.pokemonpricesservice.grpc.priceSummary
import com.pokemonpricer.pokemonpricesservice.grpc.getPricesBatchResponse
import kotlinx.coroutines.delay
import org.slf4j.LoggerFactory

class PokemonPricesGrpcService : PokemonPricesServiceGrpcKt.PokemonPricesServiceCoroutineImplBase() {

    private val logger = LoggerFactory.getLogger(PokemonPricesGrpcService::class.java)

    override suspend fun getPricesBatch(request: GetPricesBatchRequest): GetPricesBatchResponse {
        logger.info("getPricesBatch: {} cards requested", request.cardsCount)
        delay(1_000)
        val prices = request.cardsList.associate { card ->
            card.cardId to stubPriceSummary(card.cardId)
        }
        logger.info("getPricesBatch: returning {} prices", prices.size)
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
