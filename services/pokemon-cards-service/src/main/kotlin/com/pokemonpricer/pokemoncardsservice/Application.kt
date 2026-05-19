package com.pokemonpricer.pokemoncardsservice

import io.grpc.ServerBuilder
import kotlinx.serialization.Serializable

@Serializable
data class Card(
    val id: String,
    val name: String,
    val set: String,
    val imageUrl: String,
)

typealias CardsFetcher = suspend (nameFilter: String?, setFilter: String?, page: Int, numberOfItems: Int) -> List<Card>

fun main() {
    val cardsFetcher = TcgDexCardsFetcher().build()

    val grpcServer = ServerBuilder.forPort(9090)
        .addService(PokemonCardsGrpcService(cardsFetcher))
        .build()
        .start()

    Runtime.getRuntime().addShutdownHook(Thread { grpcServer.shutdown() })
    grpcServer.awaitTermination()
}
