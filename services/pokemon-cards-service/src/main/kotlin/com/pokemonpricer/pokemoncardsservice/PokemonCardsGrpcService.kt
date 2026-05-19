package com.pokemonpricer.pokemoncardsservice

import com.pokemonpricer.pokemoncardsservice.grpc.Card as GrpcCard
import com.pokemonpricer.pokemoncardsservice.grpc.GetCardsRequest
import com.pokemonpricer.pokemoncardsservice.grpc.GetCardsResponse
import com.pokemonpricer.pokemoncardsservice.grpc.PokemonCardsServiceGrpcKt

class PokemonCardsGrpcService(
    private val fetchCards: CardsFetcher,
) : PokemonCardsServiceGrpcKt.PokemonCardsServiceCoroutineImplBase() {

    override suspend fun getCards(request: GetCardsRequest): GetCardsResponse {
        val nameFilter = request.name.takeIf { it.isNotBlank() }?.lowercase()
        val setFilter = request.set.takeIf { it.isNotBlank() }?.lowercase()
        val page = if (request.page > 0) request.page else 1
        val numberOfItems = if (request.numberOfItems > 0) request.numberOfItems else 20

        val cards = fetchCards(nameFilter, setFilter, page, numberOfItems)

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
