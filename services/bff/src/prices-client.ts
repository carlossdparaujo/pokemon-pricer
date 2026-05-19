import { createChannel, createClient } from 'nice-grpc'
import { PokemonPricesServiceDefinition, type CardPriceRequest, type PriceSummary, type GetPricesBatchRequest, type GetPricesBatchResponse } from './generated/prices.js'

export type { CardPriceRequest, PriceSummary }

export interface PricesClient {
  getPricesBatch(request: GetPricesBatchRequest): Promise<GetPricesBatchResponse>
}

export function createPricesClient(address: string): PricesClient {
  // nice-grpc uses DeepPartial<T> for request params; the cast is safe since GetPricesBatchRequest satisfies that at runtime
  return createClient(PokemonPricesServiceDefinition, createChannel(address)) as PricesClient
}
