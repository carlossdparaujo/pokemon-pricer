package com.pokemonpricer.pokemoncardsservice

import com.pokemonpricer.pokemoncardsservice.grpc.Card as GrpcCard
import com.pokemonpricer.pokemoncardsservice.grpc.GetCardsRequest
import com.pokemonpricer.pokemoncardsservice.grpc.GetCardsResponse
import com.pokemonpricer.pokemoncardsservice.grpc.PokemonCardsServiceGrpcKt
import org.slf4j.LoggerFactory

class PokemonCardsGrpcService(
    private val fetchCards: CardsFetcher,
) : PokemonCardsServiceGrpcKt.PokemonCardsServiceCoroutineImplBase() {

    private val logger = LoggerFactory.getLogger(PokemonCardsGrpcService::class.java)

    override suspend fun getCards(request: GetCardsRequest): GetCardsResponse {
        val nameFilter = request.name.takeIf { it.isNotBlank() }?.lowercase()
        val setFilter = request.set.takeIf { it.isNotBlank() }?.lowercase()
        val page = if (request.page > 0) request.page else 1
        val numberOfItems = if (request.numberOfItems > 0) request.numberOfItems else 20

        logger.info("getCards: name='{}', set='{}', page={}, numberOfItems={}", nameFilter, setFilter, page, numberOfItems)

        val cards = try {
            fetchCards(nameFilter, setFilter, page, numberOfItems)
        } catch (e: Exception) {
            logger.error("getCards failed: {}", e.message, e)
            throw e
        }

        logger.info("getCards: returning {} cards", cards.size)
        return GetCardsResponse.newBuilder()
            .addAllCards(cards.map { it.toGrpcCard() })
            .build()
    }

    private fun Card.toGrpcCard(): GrpcCard =
        GrpcCard.newBuilder()
            .setId(id)
            .setName(name)
            .setSet(set)
            .setImageUrl(imageUrl)
            .build()
}
