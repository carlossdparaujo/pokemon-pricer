package com.pokemonpricer.pokemoncardsservice

import io.grpc.ServerBuilder
import kotlinx.serialization.Serializable
import org.slf4j.LoggerFactory

@Serializable
data class Card(
    val id: String,
    val name: String,
    val set: String,
    val imageUrl: String,
)

typealias CardsFetcher = suspend (nameFilter: String?, setFilter: String?, page: Int, numberOfItems: Int) -> List<Card>

private val logger = LoggerFactory.getLogger("com.pokemonpricer.pokemoncardsservice.Application")

fun main() {
    val cardsFetcher = TcgDexCardsFetcher().build()

    val port = System.getenv("GRPC_PORT")?.toInt() ?: 50051
    val grpcServer = ServerBuilder.forPort(port)
        .addService(PokemonCardsGrpcService(cardsFetcher))
        .build()
        .start()

    logger.info("Pokemon Cards service started on port $port")
    Runtime.getRuntime().addShutdownHook(Thread {
        logger.info("Shutting down Pokemon Cards service")
        grpcServer.shutdown()
    })
    grpcServer.awaitTermination()
}
