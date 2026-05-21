package com.pokemonpricer.pokemonpricesservice

import io.grpc.ServerBuilder
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("com.pokemonpricer.pokemonpricesservice.Application")

fun main() {
    val grpcServer = ServerBuilder.forPort(50051)
        .addService(PokemonPricesGrpcService())
        .build()
        .start()

    logger.info("Pokemon Prices service started on port 50051")
    Runtime.getRuntime().addShutdownHook(Thread {
        logger.info("Shutting down Pokemon Prices service")
        grpcServer.shutdown()
    })
    grpcServer.awaitTermination()
}
