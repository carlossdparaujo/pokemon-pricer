import * as grpc from '@grpc/grpc-js'
import * as protoLoader from '@grpc/proto-loader'
import { fileURLToPath } from 'url'
import { join, dirname } from 'path'

const __filename = fileURLToPath(import.meta.url)
const __dirname = dirname(__filename)

export interface CardPriceRequest {
  cardId: string
  name: string
  set: string
}

export interface PriceSummary {
  cardId: string
  average: number
  p10: number
  p50: number
  p90: number
  p99: number
}

export interface PricesClient {
  getPricesBatch(cards: CardPriceRequest[]): Promise<Record<string, PriceSummary>>
}

export function createGrpcPricesClient(address: string): PricesClient {
  const protoPath = join(__dirname, 'proto', 'prices.proto')

  const packageDefinition = protoLoader.loadSync(protoPath, {
    keepCase: false,
    longs: String,
    enums: String,
    defaults: true,
    oneofs: true,
  })

  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  const proto = grpc.loadPackageDefinition(packageDefinition) as any
  const ServiceConstructor = proto.pokemonpricer.prices.PokemonPricesService
  const client = new ServiceConstructor(address, grpc.credentials.createInsecure())

  return {
    getPricesBatch(cards: CardPriceRequest[]): Promise<Record<string, PriceSummary>> {
      const grpcCards = cards.map((c) => ({
        card_id: c.cardId,
        name: c.name,
        set: c.set,
      }))

      return new Promise((resolve, reject) => {
        client.getPricesBatch({ cards: grpcCards }, (err: grpc.ServiceError | null, response: { prices: Record<string, { card_id: string; average: number; p10: number; p50: number; p90: number; p99: number }> }) => {
          if (err) {
            reject(err)
            return
          }
          const prices: Record<string, PriceSummary> = {}
          for (const [key, val] of Object.entries(response.prices)) {
            prices[key] = {
              cardId: val.card_id,
              average: val.average,
              p10: val.p10,
              p50: val.p50,
              p90: val.p90,
              p99: val.p99,
            }
          }
          resolve(prices)
        })
      })
    },
  }
}
