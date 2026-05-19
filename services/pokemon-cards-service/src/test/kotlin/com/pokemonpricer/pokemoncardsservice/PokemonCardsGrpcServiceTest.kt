package com.pokemonpricer.pokemoncardsservice

import com.pokemonpricer.pokemoncardsservice.grpc.GetCardsRequest
import com.pokemonpricer.pokemoncardsservice.grpc.PokemonCardsServiceGrpcKt
import io.grpc.inprocess.InProcessChannelBuilder
import io.grpc.inprocess.InProcessServerBuilder
import io.grpc.testing.GrpcCleanupRule
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
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

    private fun MockRequestHandleScope.jsonOk(body: String) = respond(
        content = body,
        status = HttpStatusCode.OK,
        headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
    )

    @Test
    fun `returns cards matching name filter`() = runBlocking {
        val mockEngine = MockEngine { request ->
            when (request.url.encodedPath) {
                "/v2/en/cards" -> jsonOk("""[{"id":"base1-25","name":"Pikachu","image":"https://assets.tcgdex.net/en/base/base1/25"}]""")
                "/v2/en/sets/base1" -> jsonOk("""{"id":"base1","name":"Base Set"}""")
                else -> error("Unexpected request: ${request.url}")
            }
        }
        val stub = startServer(TcgDexCardsFetcher(mockEngine).build())

        val response = stub.getCards(GetCardsRequest.newBuilder().setName("pikachu").build())

        assertEquals(1, response.cardsList.size)
        assertEquals("Pikachu", response.cardsList.first().name)
    }

    @Test
    fun `returns cards from set when only set filter is present`() = runBlocking {
        val mockEngine = MockEngine { request ->
            when (request.url.encodedPath) {
                "/v2/en/sets/base set" -> jsonOk("""
                    {"serie":{"id":"base","name":"Base"},"name":"Base Set","cards":[{"id":"base1-4","name":"Charizard"},{"id":"base1-25","name":"Pikachu"}]}
                """.trimIndent())
                else -> error("Unexpected request: ${request.url}")
            }
        }
        val stub = startServer(TcgDexCardsFetcher(mockEngine).build())

        val response = stub.getCards(GetCardsRequest.newBuilder().setSet("Base Set").build())

        assertEquals(2, response.cardsList.size)
        assertTrue(response.cardsList.any { it.name == "Charizard" && it.set == "Base Set" })
        assertTrue(response.cardsList.any { it.name == "Pikachu" && it.set == "Base Set" })
    }

    @Test
    fun `returns cards matching both name and set filters`() = runBlocking {
        val mockEngine = MockEngine { request ->
            when (request.url.encodedPath) {
                "/v2/en/sets/base set" -> jsonOk("""
                    {"serie":{"id":"base","name":"Base"},"name":"Base Set","cards":[{"id":"base1-4","name":"Charizard"},{"id":"base1-25","name":"Pikachu"}]}
                """.trimIndent())
                else -> error("Unexpected request: ${request.url}")
            }
        }
        val stub = startServer(TcgDexCardsFetcher(mockEngine).build())

        val response = stub.getCards(
            GetCardsRequest.newBuilder().setName("charizard").setSet("Base Set").build()
        )

        assertEquals(1, response.cardsList.size)
        assertEquals("Charizard", response.cardsList.first().name)
        assertEquals("Base Set", response.cardsList.first().set)
    }

    @Test
    fun `returns all cards when no filter given`() = runBlocking {
        val mockEngine = MockEngine { request ->
            when (request.url.encodedPath) {
                "/v2/en/cards" -> jsonOk("""[{"id":"base1-4","name":"Charizard"},{"id":"base1-25","name":"Pikachu"}]""")
                "/v2/en/sets/base1" -> jsonOk("""{"id":"base1","name":"Base Set"}""")
                else -> error("Unexpected request: ${request.url}")
            }
        }
        val stub = startServer(TcgDexCardsFetcher(mockEngine).build())

        val response = stub.getCards(GetCardsRequest.newBuilder().build())

        assertEquals(2, response.cardsList.size)
    }

    @Test
    fun `returns empty list when no cards match`() = runBlocking {
        val mockEngine = MockEngine { jsonOk("[]") }
        val stub = startServer(TcgDexCardsFetcher(mockEngine).build())

        val response = stub.getCards(GetCardsRequest.newBuilder().setName("mewtwo").build())

        assertTrue(response.cardsList.isEmpty())
    }

    @Test
    fun `forwards name filter as lowercase to fetcher`() = runBlocking {
        var capturedNameParam: String? = null
        val mockEngine = MockEngine { request ->
            if (request.url.encodedPath == "/v2/en/cards") capturedNameParam = request.url.parameters["name"]
            jsonOk("[]")
        }
        val stub = startServer(TcgDexCardsFetcher(mockEngine).build())

        stub.getCards(GetCardsRequest.newBuilder().setName("Pikachu").build())

        assertEquals("pikachu", capturedNameParam)
    }

    @Test
    fun `forwards page to fetcher`() = runBlocking {
        var capturedPage: String? = null
        val mockEngine = MockEngine { request ->
            capturedPage = request.url.parameters["pagination:page"]
            jsonOk("[]")
        }
        val stub = startServer(TcgDexCardsFetcher(mockEngine).build())

        stub.getCards(GetCardsRequest.newBuilder().setPage(3).build())

        assertEquals("3", capturedPage)
    }

    @Test
    fun `page defaults to 1 when not provided`() = runBlocking {
        var capturedPage: String? = null
        val mockEngine = MockEngine { request ->
            capturedPage = request.url.parameters["pagination:page"]
            jsonOk("[]")
        }
        val stub = startServer(TcgDexCardsFetcher(mockEngine).build())

        stub.getCards(GetCardsRequest.newBuilder().build())

        assertEquals("1", capturedPage)
    }

    @Test
    fun `forwards numberOfItems to fetcher`() = runBlocking {
        var capturedItemsPerPage: String? = null
        val mockEngine = MockEngine { request ->
            capturedItemsPerPage = request.url.parameters["pagination:itemsPerPage"]
            jsonOk("[]")
        }
        val stub = startServer(TcgDexCardsFetcher(mockEngine).build())

        stub.getCards(GetCardsRequest.newBuilder().setNumberOfItems(50).build())

        assertEquals("50", capturedItemsPerPage)
    }

    @Test
    fun `numberOfItems defaults to 20 when not provided`() = runBlocking {
        var capturedItemsPerPage: String? = null
        val mockEngine = MockEngine { request ->
            capturedItemsPerPage = request.url.parameters["pagination:itemsPerPage"]
            jsonOk("[]")
        }
        val stub = startServer(TcgDexCardsFetcher(mockEngine).build())

        stub.getCards(GetCardsRequest.newBuilder().build())

        assertEquals("20", capturedItemsPerPage)
    }

    @Test
    fun `response card shape maps all fields correctly`() = runBlocking {
        val mockEngine = MockEngine { request ->
            when (request.url.encodedPath) {
                "/v2/en/cards" -> jsonOk("""[{"id":"base1-4","name":"Charizard","image":"https://assets.tcgdex.net/en/base/base1/4"}]""")
                "/v2/en/sets/base1" -> jsonOk("""{"serie":{"id":"base1","name":"Base Set"},"name":"Base Set","cards":[{"id":"base1-4","name":"Charizard","image":"https://assets.tcgdex.net/en/base/base1/4"}]}""")
                else -> error("Unexpected request: ${request.url}")
            }
        }
        val stub = startServer(TcgDexCardsFetcher(mockEngine).build())

        val response = stub.getCards(GetCardsRequest.newBuilder().build())

        assertEquals(1, response.cardsList.size)
        with(response.cardsList.first()) {
            assertEquals("base1-4", id)
            assertEquals("Charizard", name)
            assertEquals("Base Set", set)
            assertEquals("https://assets.tcgdex.net/en/base/base1/4/high.webp", imageUrl)
        }
    }

    @Test
    fun `empty name filter is treated as no filter`() = runBlocking {
        var nameParamPresent = false
        val mockEngine = MockEngine { request ->
            nameParamPresent = request.url.parameters.contains("name")
            jsonOk("[]")
        }
        val stub = startServer(TcgDexCardsFetcher(mockEngine).build())

        stub.getCards(GetCardsRequest.newBuilder().setName("").build())

        assertFalse(nameParamPresent)
    }

    @Test
    fun `empty set filter is treated as no filter`() = runBlocking {
        var requestPath = ""
        val mockEngine = MockEngine { request ->
            requestPath = request.url.encodedPath
            jsonOk("[]")
        }
        val stub = startServer(TcgDexCardsFetcher(mockEngine).build())

        stub.getCards(GetCardsRequest.newBuilder().setSet("").build())

        assertTrue(requestPath.endsWith("/cards"), "Expected /cards path but got $requestPath")
    }
}
