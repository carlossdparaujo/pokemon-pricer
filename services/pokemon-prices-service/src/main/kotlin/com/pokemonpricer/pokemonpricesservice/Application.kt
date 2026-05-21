package com.pokemonpricer.pokemonpricesservice

import io.grpc.ServerBuilder
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("com.pokemonpricer.pokemonpricesservice.Application")

fun main() {
    val port = System.getenv("GRPC_PORT")?.toInt() ?: 50051
    val grpcServer = ServerBuilder.forPort(port)
        .addService(PokemonPricesGrpcService())
        .build()
        .start()

    logger.info("Pokemon Prices service started on port $port")
    Runtime.getRuntime().addShutdownHook(Thread {
        logger.info("Shutting down Pokemon Prices service")
        grpcServer.shutdown()
    })
    grpcServer.awaitTermination()
}
