import { Hono } from 'hono'
import { CardsClient, createCardsClient } from './cards-client.js'
import { PricesClient, createPricesClient } from './prices-client.js'

const POKEMON_CARDS_SERVICE_GRPC_ADDRESS =
  process.env.POKEMON_CARDS_SERVICE_GRPC_ADDRESS ?? 'localhost:9090'

const POKEMON_PRICES_SERVICE_GRPC_ADDRESS =
  process.env.POKEMON_PRICES_SERVICE_GRPC_ADDRESS ?? 'localhost:9091'

export function createApp(cardsClient: CardsClient, pricesClient: PricesClient): Hono {
  const app = new Hono()

  app.get('/api/health', (c) => c.json({ status: 'ok' }))

  app.get('/api/pokemon-cards', async (c) => {
    const name = c.req.query('name') ?? ''
    const set = c.req.query('set') ?? ''
    const page = Number(c.req.query('page') ?? '1')

    try {
      const { cards } = await cardsClient.getCards({ name, set, page, numberOfItems: 20 })
      return c.json({ cards })
    } catch {
      return c.json({ error: 'Failed to fetch cards' }, 500)
    }
  })

  app.post('/api/pokemon-prices', async (c) => {
    try {
      const body = await c.req.json()
      const { prices } = await pricesClient.getPricesBatch({ cards: body })
      return c.json(prices)
    } catch {
      return c.json({ error: 'Failed to fetch prices' }, 500)
    }
  })

  return app
}

export default createApp(
  createCardsClient(POKEMON_CARDS_SERVICE_GRPC_ADDRESS),
  createPricesClient(POKEMON_PRICES_SERVICE_GRPC_ADDRESS)
)
