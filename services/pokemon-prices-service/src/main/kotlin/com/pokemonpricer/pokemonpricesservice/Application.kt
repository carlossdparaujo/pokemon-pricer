package com.pokemonpricer.pokemonpricesservice

import io.grpc.ServerBuilder

fun main() {
    val grpcServer = ServerBuilder.forPort(9091)
        .addService(PokemonPricesGrpcService())
        .build()
        .start()

    Runtime.getRuntime().addShutdownHook(Thread { grpcServer.shutdown() })
    grpcServer.awaitTermination()
}
