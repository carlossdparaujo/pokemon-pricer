import { describe, it, expect, vi, afterEach } from 'vitest'
import app from './app'

afterEach(() => {
  vi.restoreAllMocks()
})

describe('GET /api/pokemon-cards', () => {
  it('returns cards from upstream', async () => {
    const cards = [{ id: '1', name: 'Pikachu', set: 'Base', imageUrl: 'http://img/pikachu.png' }]
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(new Response(JSON.stringify({ cards }), { status: 200 }))
    )

    const res = await app.fetch(
      new Request('http://localhost/api/pokemon-cards?name=Pikachu&set=Base')
    )

    expect(res.status).toBe(200)
    expect(await res.json()).toEqual({ cards })
  })

  it('forwards name, set, and page params to cards service', async () => {
    const fetchMock = vi.fn().mockResolvedValue(
      new Response(JSON.stringify({ cards: [] }), { status: 200 })
    )
    vi.stubGlobal('fetch', fetchMock)

    await app.fetch(new Request('http://localhost/api/pokemon-cards?name=Pikachu&set=Base&page=3'))

    const upstreamUrl = new URL(fetchMock.mock.calls[0][0] as string)
    expect(upstreamUrl.searchParams.get('name')).toBe('Pikachu')
    expect(upstreamUrl.searchParams.get('set')).toBe('Base')
    expect(upstreamUrl.searchParams.get('page')).toBe('3')
  })

  it('defaults page to 1 when not provided', async () => {
    const fetchMock = vi.fn().mockResolvedValue(
      new Response(JSON.stringify({ cards: [] }), { status: 200 })
    )
    vi.stubGlobal('fetch', fetchMock)

    await app.fetch(new Request('http://localhost/api/pokemon-cards'))

    const upstreamUrl = new URL(fetchMock.mock.calls[0][0] as string)
    expect(upstreamUrl.searchParams.get('page')).toBe('1')
  })

  it('always sends numberOfItems=20 to cards service', async () => {
    const fetchMock = vi.fn().mockResolvedValue(
      new Response(JSON.stringify({ cards: [] }), { status: 200 })
    )
    vi.stubGlobal('fetch', fetchMock)

    await app.fetch(new Request('http://localhost/api/pokemon-cards'))

    const upstreamUrl = new URL(fetchMock.mock.calls[0][0] as string)
    expect(upstreamUrl.searchParams.get('numberOfItems')).toBe('20')
  })

  it('returns 500 when cards service responds with a non-2xx status', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(new Response('Error', { status: 500 }))
    )

    const res = await app.fetch(new Request('http://localhost/api/pokemon-cards'))

    expect(res.status).toBe(500)
    expect(await res.json()).toEqual({ error: 'Failed to fetch cards' })
  })

  it('returns 500 when the network call throws', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockRejectedValue(new Error('Network failure'))
    )

    const res = await app.fetch(new Request('http://localhost/api/pokemon-cards'))

    expect(res.status).toBe(500)
    expect(await res.json()).toEqual({ error: 'Failed to fetch cards' })
  })
})

describe('POST /api/pokemon-prices', () => {
  it('returns prices from upstream prices service', async () => {
    const priceMap = { 'base1-4': { cardId: 'base1-4', average: 5.0, p10: 1.0, p50: 4.5, p90: 12.0, p99: 25.0 } }
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(new Response(JSON.stringify(priceMap), { status: 200 }))
    )

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

  it('forwards the request body to the prices service', async () => {
    const fetchMock = vi.fn().mockResolvedValue(
      new Response(JSON.stringify({}), { status: 200 })
    )
    vi.stubGlobal('fetch', fetchMock)

    const payload = [{ cardId: 'base1-4', name: 'Charizard', set: 'Base Set' }]
    await app.fetch(
      new Request('http://localhost/api/pokemon-prices', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload),
      })
    )

    const [, options] = fetchMock.mock.calls[0]
    expect(JSON.parse(options.body as string)).toEqual(payload)
  })

  it('returns 500 when prices service responds with a non-2xx status', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(new Response('Error', { status: 503 }))
    )

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

  it('returns 500 when the network call throws', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockRejectedValue(new Error('Network failure'))
    )

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
