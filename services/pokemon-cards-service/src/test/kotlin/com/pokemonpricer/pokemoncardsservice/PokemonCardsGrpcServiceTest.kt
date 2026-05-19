package com.pokemonpricer.pokemoncardsservice

import com.pokemonpricer.pokemoncardsservice.grpc.GetCardsRequest
import com.pokemonpricer.pokemoncardsservice.grpc.PokemonCardsServiceGrpcKt
import io.grpc.inprocess.InProcessChannelBuilder
import io.grpc.inprocess.InProcessServerBuilder
import io.grpc.testing.GrpcCleanupRule
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PokemonCardsGrpcServiceTest {

    private val grpcCleanup = GrpcCleanupRule()

    private fun startServer(fetcher: CardsFetcher): PokemonCardsServiceGrpcKt.PokemonCardsServiceCoroutineStub {
        val serverName = InProcessServerBuilder.generateName()

        grpcCleanup.register(
            InProcessServerBuilder.forName(serverName)
                .directExecutor()
                .addService(PokemonCardsGrpcService(fetcher))
                .build()
                .start()
        )

        val channel = grpcCleanup.register(
            InProcessChannelBuilder.forName(serverName).directExecutor().build()
        )

        return PokemonCardsServiceGrpcKt.PokemonCardsServiceCoroutineStub(channel)
    }

    private fun card(id: String, name: String, set: String = "Base Set") = Card(
        id = id, name = name, set = set, imageUrl = "https://img/$id.png"
    )

    @Test
    fun `returns cards matching name filter`() = runBlocking {
        val stub = startServer { nameFilter, _, _, _ ->
            listOf(card("base1-4", "Charizard"), card("base1-25", "Pikachu"))
                .filter { nameFilter == null || it.name.lowercase().contains(nameFilter) }
        }

        val request = GetCardsRequest.newBuilder().setName("pikachu").build()
        val response = stub.getCards(request)

        assertEquals(1, response.cardsList.size)
        assertEquals("Pikachu", response.cardsList.first().name)
    }

    @Test
    fun `returns all cards when no filter given`() = runBlocking {
        val stub = startServer { _, _, _, _ ->
            listOf(card("base1-4", "Charizard"), card("base1-25", "Pikachu"))
        }

        val request = GetCardsRequest.newBuilder().build()
        val response = stub.getCards(request)

        assertEquals(2, response.cardsList.size)
    }

    @Test
    fun `returns empty list when no cards match`() = runBlocking {
        val stub = startServer { _, _, _, _ -> emptyList() }

        val request = GetCardsRequest.newBuilder().setName("mewtwo").build()
        val response = stub.getCards(request)

        assertTrue(response.cardsList.isEmpty())
    }

    @Test
    fun `forwards name filter as lowercase to fetcher`() = runBlocking {
        var capturedNameFilter: String? = null
        val stub = startServer { nameFilter, _, _, _ ->
            capturedNameFilter = nameFilter
            emptyList()
        }

        val request = GetCardsRequest.newBuilder().setName("Pikachu").build()
        stub.getCards(request)

        assertEquals("pikachu", capturedNameFilter)
    }

    @Test
    fun `forwards set filter as lowercase to fetcher`() = runBlocking {
        var capturedSetFilter: String? = null
        val stub = startServer { _, setFilter, _, _ ->
            capturedSetFilter = setFilter
            emptyList()
        }

        val request = GetCardsRequest.newBuilder().setSet("Base Set").build()
        stub.getCards(request)

        assertEquals("base set", capturedSetFilter)
    }

    @Test
    fun `forwards page to fetcher`() = runBlocking {
        var capturedPage = 0
        val stub = startServer { _, _, page, _ ->
            capturedPage = page
            emptyList()
        }

        val request = GetCardsRequest.newBuilder().setPage(3).build()
        stub.getCards(request)

        assertEquals(3, capturedPage)
    }

    @Test
    fun `page defaults to 1 when not provided`() = runBlocking {
        var capturedPage = 0
        val stub = startServer { _, _, page, _ ->
            capturedPage = page
            emptyList()
        }

        val request = GetCardsRequest.newBuilder().build()
        stub.getCards(request)

        assertEquals(1, capturedPage)
    }

    @Test
    fun `forwards numberOfItems to fetcher`() = runBlocking {
        var capturedNumberOfItems = 0
        val stub = startServer { _, _, _, numberOfItems ->
            capturedNumberOfItems = numberOfItems
            emptyList()
        }

        val request = GetCardsRequest.newBuilder().setNumberOfItems(50).build()
        stub.getCards(request)

        assertEquals(50, capturedNumberOfItems)
    }

    @Test
    fun `numberOfItems defaults to 20 when not provided`() = runBlocking {
        var capturedNumberOfItems = 0
        val stub = startServer { _, _, _, numberOfItems ->
            capturedNumberOfItems = numberOfItems
            emptyList()
        }

        val request = GetCardsRequest.newBuilder().build()
        stub.getCards(request)

        assertEquals(20, capturedNumberOfItems)
    }

    @Test
    fun `response card shape maps all fields correctly`() = runBlocking {
        val stub = startServer { _, _, _, _ ->
            listOf(card("base1-4", "Charizard", "Base Set"))
        }

        val request = GetCardsRequest.newBuilder().build()
        val response = stub.getCards(request)

        assertEquals(1, response.cardsList.size)
        val grpcCard = response.cardsList.first()
        assertEquals("base1-4", grpcCard.id)
        assertEquals("Charizard", grpcCard.name)
        assertEquals("Base Set", grpcCard.set)
        assertEquals("https://img/base1-4.png", grpcCard.imageUrl)
    }

    @Test
    fun `empty name filter is treated as no filter`() = runBlocking {
        var capturedNameFilter: String? = "sentinel"
        val stub = startServer { nameFilter, _, _, _ ->
            capturedNameFilter = nameFilter
            emptyList()
        }

        val request = GetCardsRequest.newBuilder().setName("").build()
        stub.getCards(request)

        assertEquals(null, capturedNameFilter)
    }

    @Test
    fun `empty set filter is treated as no filter`() = runBlocking {
        var capturedSetFilter: String? = "sentinel"
        val stub = startServer { _, setFilter, _, _ ->
            capturedSetFilter = setFilter
            emptyList()
        }

        val request = GetCardsRequest.newBuilder().setSet("").build()
        stub.getCards(request)

        assertEquals(null, capturedSetFilter)
    }
}
