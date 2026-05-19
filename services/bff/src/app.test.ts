import { describe, it, expect, vi, afterEach } from 'vitest'
import { createApp } from './app.js'
import type { CardsClient, Card } from './cards-client.js'
import type { PricesClient, PriceSummary } from './prices-client.js'

afterEach(() => {
  vi.restoreAllMocks()
})

function makeCardsClient(cards: Card[] = []): CardsClient {
  return {
    getCards: vi.fn().mockResolvedValue(cards),
  }
}

function makePricesClient(prices: Record<string, PriceSummary> = {}): PricesClient {
  return {
    getPricesBatch: vi.fn().mockResolvedValue(prices),
  }
}

describe('GET /api/pokemon-cards', () => {
  it('returns cards from upstream', async () => {
    const cards: Card[] = [{ id: '1', name: 'Pikachu', set: 'Base', imageUrl: 'http://img/pikachu.png' }]
    const app = createApp(makeCardsClient(cards), makePricesClient())

    const res = await app.fetch(
      new Request('http://localhost/api/pokemon-cards?name=Pikachu&set=Base')
    )

    expect(res.status).toBe(200)
    expect(await res.json()).toEqual({ cards })
  })

  it('forwards name, set, and page params to cards client', async () => {
    const mockClient: CardsClient = { getCards: vi.fn().mockResolvedValue([]) }
    const app = createApp(mockClient, makePricesClient())

    await app.fetch(new Request('http://localhost/api/pokemon-cards?name=Pikachu&set=Base&page=3'))

    expect(mockClient.getCards).toHaveBeenCalledWith({
      name: 'Pikachu',
      set: 'Base',
      page: 3,
      numberOfItems: 20,
    })
  })

  it('defaults page to 1 when not provided', async () => {
    const mockClient: CardsClient = { getCards: vi.fn().mockResolvedValue([]) }
    const app = createApp(mockClient, makePricesClient())

    await app.fetch(new Request('http://localhost/api/pokemon-cards'))

    expect(mockClient.getCards).toHaveBeenCalledWith(
      expect.objectContaining({ page: 1 })
    )
  })

  it('always sends numberOfItems=20 to cards client', async () => {
    const mockClient: CardsClient = { getCards: vi.fn().mockResolvedValue([]) }
    const app = createApp(mockClient, makePricesClient())

    await app.fetch(new Request('http://localhost/api/pokemon-cards'))

    expect(mockClient.getCards).toHaveBeenCalledWith(
      expect.objectContaining({ numberOfItems: 20 })
    )
  })

  it('returns 500 when cards client throws', async () => {
    const mockClient: CardsClient = {
      getCards: vi.fn().mockRejectedValue(new Error('gRPC failure')),
    }
    const app = createApp(mockClient, makePricesClient())

    const res = await app.fetch(new Request('http://localhost/api/pokemon-cards'))

    expect(res.status).toBe(500)
    expect(await res.json()).toEqual({ error: 'Failed to fetch cards' })
  })
})

describe('POST /api/pokemon-prices', () => {
  it('returns prices from upstream prices service', async () => {
    const priceMap: Record<string, PriceSummary> = {
      'base1-4': { cardId: 'base1-4', average: 5.0, p10: 1.0, p50: 4.5, p90: 12.0, p99: 25.0 },
    }
    const mockPricesClient = makePricesClient(priceMap)
    const app = createApp(makeCardsClient(), mockPricesClient)

    const res = await app.fetch(
      new Request('http://localhost/api/pokemon-prices', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify([{ cardId: 'base1-4', name: 'Charizard', set: 'Base Set' }]),
      })
    )

    expect(res.status).toBe(200)
    expect(await res.json()).toEqual(priceMap)
  })

  it('forwards the request body to the prices client', async () => {
    const mockPricesClient: PricesClient = {
      getPricesBatch: vi.fn().mockResolvedValue({}),
    }
    const app = createApp(makeCardsClient(), mockPricesClient)

    const payload = [{ cardId: 'base1-4', name: 'Charizard', set: 'Base Set' }]
    await app.fetch(
      new Request('http://localhost/api/pokemon-prices', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload),
      })
    )

    expect(mockPricesClient.getPricesBatch).toHaveBeenCalledWith(payload)
  })

  it('returns 500 when prices client throws', async () => {
    const mockPricesClient: PricesClient = {
      getPricesBatch: vi.fn().mockRejectedValue(new Error('gRPC failure')),
    }
    const app = createApp(makeCardsClient(), mockPricesClient)

    const res = await app.fetch(
      new Request('http://localhost/api/pokemon-prices', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify([]),
      })
    )

    expect(res.status).toBe(500)
    expect(await res.json()).toEqual({ error: 'Failed to fetch prices' })
  })
})
