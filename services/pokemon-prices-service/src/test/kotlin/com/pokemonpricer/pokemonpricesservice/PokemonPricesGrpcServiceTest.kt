package com.pokemonpricer.pokemonpricesservice

import com.pokemonpricer.pokemonpricesservice.grpc.CardPriceRequest
import com.pokemonpricer.pokemonpricesservice.grpc.GetPricesBatchRequest
import com.pokemonpricer.pokemonpricesservice.grpc.PokemonPricesServiceGrpcKt
import com.pokemonpricer.pokemonpricesservice.grpc.cardPriceRequest
import com.pokemonpricer.pokemonpricesservice.grpc.getPricesBatchRequest
import io.grpc.ManagedChannel
import io.grpc.Server
import io.grpc.inprocess.InProcessChannelBuilder
import io.grpc.inprocess.InProcessServerBuilder
import kotlinx.coroutines.runBlocking
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PokemonPricesGrpcServiceTest {

    private lateinit var server: Server
    private lateinit var channel: ManagedChannel
    private lateinit var stub: PokemonPricesServiceGrpcKt.PokemonPricesServiceCoroutineStub

    @BeforeTest
    fun setUp() {
        val serverName = InProcessServerBuilder.generateName()
        server = InProcessServerBuilder.forName(serverName)
            .directExecutor()
            .addService(PokemonPricesGrpcService())
            .build()
            .start()
        channel = InProcessChannelBuilder.forName(serverName)
            .directExecutor()
            .build()
        stub = PokemonPricesServiceGrpcKt.PokemonPricesServiceCoroutineStub(channel)
    }

    @AfterTest
    fun tearDown() {
        channel.shutdownNow()
        server.shutdownNow()
    }

    @Test
    fun `getPricesBatch returns price summary for each requested card`() = runBlocking {
        val request = getPricesBatchRequest {
            cards.add(cardPriceRequest {
                cardId = "base1-4"
                name = "Charizard"
                set = "Base Set"
            })
            cards.add(cardPriceRequest {
                cardId = "base1-25"
                name = "Pikachu"
                set = "Base Set"
            })
        }

        val response = stub.getPricesBatch(request)

        assertTrue(response.pricesMap.containsKey("base1-4"), "response should include base1-4")
        assertTrue(response.pricesMap.containsKey("base1-25"), "response should include base1-25")
    }

    @Test
    fun `getPricesBatch returns correct cardId in each price summary`() = runBlocking {
        val request = getPricesBatchRequest {
            cards.add(cardPriceRequest {
                cardId = "xy1-99"
                name = "Mewtwo"
                set = "XY"
            })
        }

        val response = stub.getPricesBatch(request)

        val summary = response.pricesMap["xy1-99"]
        assertEquals("xy1-99", summary?.cardId)
    }

    @Test
    fun `getPricesBatch returns non-zero price fields`() = runBlocking {
        val request = getPricesBatchRequest {
            cards.add(cardPriceRequest {
                cardId = "base1-4"
                name = "Charizard"
                set = "Base Set"
            })
        }

        val response = stub.getPricesBatch(request)

        val summary = response.pricesMap["base1-4"]!!
        assertTrue(summary.average > 0.0, "average should be non-zero")
        assertTrue(summary.p10 > 0.0, "p10 should be non-zero")
        assertTrue(summary.p50 > 0.0, "p50 should be non-zero")
        assertTrue(summary.p90 > 0.0, "p90 should be non-zero")
        assertTrue(summary.p99 > 0.0, "p99 should be non-zero")
    }

    @Test
    fun `getPricesBatch with empty list returns empty prices map`() = runBlocking {
        val request = getPricesBatchRequest {}

        val response = stub.getPricesBatch(request)

        assertTrue(response.pricesMap.isEmpty(), "response should be empty for empty request")
    }
}
