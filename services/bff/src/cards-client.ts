import { createChannel, createClient } from 'nice-grpc'
import { PokemonCardsServiceDefinition, type Card, type GetCardsRequest, type GetCardsResponse } from './generated/cards.js'

export type { Card }

export interface CardsClient {
  getCards(request: GetCardsRequest): Promise<GetCardsResponse>
}

export function createCardsClient(address: string): CardsClient {
  // nice-grpc uses DeepPartial<T> for request params; the cast is safe since GetCardsRequest satisfies that at runtime
  return createClient(PokemonCardsServiceDefinition, createChannel(address)) as CardsClient
}
