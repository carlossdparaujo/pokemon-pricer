import * as grpc from '@grpc/grpc-js'
import * as protoLoader from '@grpc/proto-loader'
import path from 'path'
import { fileURLToPath } from 'url'

export interface Card {
  id: string
  name: string
  set: string
  imageUrl: string
}

export interface CardsClient {
  getCards(req: {
    name: string
    set: string
    page: number
    numberOfItems: number
  }): Promise<Card[]>
}

const __dirname = path.dirname(fileURLToPath(import.meta.url))

function loadProto() {
  const protoPath = path.join(__dirname, 'proto', 'cards.proto')
  const packageDefinition = protoLoader.loadSync(protoPath, {
    keepCase: false,
    longs: String,
    enums: String,
    defaults: true,
    oneofs: true,
  })
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  const proto = grpc.loadPackageDefinition(packageDefinition) as any
  return proto.pokemonpricer.cards
}

export function createGrpcCardsClient(address: string): CardsClient {
  const cardsProto = loadProto()
  const client = new cardsProto.PokemonCardsService(
    address,
    grpc.credentials.createInsecure()
  )

  return {
    getCards({ name, set, page, numberOfItems }): Promise<Card[]> {
      return new Promise((resolve, reject) => {
        client.getCards(
          { name, set, page, numberOfItems },
          (err: grpc.ServiceError | null, response: { cards: Card[] }) => {
            if (err) {
              reject(err)
              return
            }
            resolve(response.cards ?? [])
          }
        )
      })
    },
  }
}
