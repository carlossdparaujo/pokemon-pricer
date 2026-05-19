import { describe, it, expect, vi, afterEach } from 'vitest'
import app from './app'

afterEach(() => {
  vi.restoreAllMocks()
})

describe('GET /api/pokemon-cards', () => {
  it('returns cards from upstream on success', async () => {
    const cards = [
      {
        id: '1',
        name: 'Pikachu',
        set: 'Base',
        imageUrl: 'http://img/pikachu.png',
        priceSummary: {
          cardId: '1',
          average: 2.5,
          p10: 1.0,
          p50: 2.25,
          p90: 4.0,
          p99: 6.5,
        },
      },
    ]
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(
        new Response(JSON.stringify({ cards }), { status: 200 })
      )
    )

    const res = await app.fetch(
      new Request('http://localhost/api/pokemon-cards?name=Pikachu&set=Base')
    )

    expect(res.status).toBe(200)
    expect(await res.json()).toEqual({ cards })
  })

  it('passes priceSummary through from upstream unchanged', async () => {
    const priceSummary = {
      cardId: 'base1-4',
      average: 350.0,
      p10: 200.0,
      p50: 330.0,
      p90: 500.0,
      p99: 750.0,
    }
    const cards = [
      {
        id: 'base1-4',
        name: 'Charizard',
        set: 'Base Set',
        imageUrl: 'http://img/charizard.png',
        priceSummary,
      },
    ]
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(
        new Response(JSON.stringify({ cards }), { status: 200 })
      )
    )

    const res = await app.fetch(
      new Request('http://localhost/api/pokemon-cards?name=Charizard')
    )

    expect(res.status).toBe(200)
    const body = await res.json() as { cards: typeof cards }
    expect(body.cards[0].priceSummary).toEqual(priceSummary)
  })

  it('returns 500 when upstream responds with a non-2xx status', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(
        new Response('Internal Server Error', { status: 500 })
      )
    )

    const res = await app.fetch(
      new Request('http://localhost/api/pokemon-cards?name=Pikachu&set=Base')
    )

    expect(res.status).toBe(500)
    expect(await res.json()).toEqual({ error: 'Failed to fetch cards' })
  })

  it('returns 500 when the network call throws', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockRejectedValue(new Error('Network failure'))
    )

    const res = await app.fetch(
      new Request('http://localhost/api/pokemon-cards?name=Pikachu&set=Base')
    )

    expect(res.status).toBe(500)
    expect(await res.json()).toEqual({ error: 'Failed to fetch cards' })
  })

  it('forwards page param to upstream', async () => {
    const fetchMock = vi.fn().mockResolvedValue(
      new Response(JSON.stringify({ cards: [] }), { status: 200 })
    )
    vi.stubGlobal('fetch', fetchMock)

    await app.fetch(new Request('http://localhost/api/pokemon-cards?page=3'))

    const upstreamUrl = new URL(fetchMock.mock.calls[0][0] as string)
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

  it('always sends numberOfItems=20 to upstream regardless of request', async () => {
    const fetchMock = vi.fn().mockResolvedValue(
      new Response(JSON.stringify({ cards: [] }), { status: 200 })
    )
    vi.stubGlobal('fetch', fetchMock)

    await app.fetch(new Request('http://localhost/api/pokemon-cards?page=5'))

    const upstreamUrl = new URL(fetchMock.mock.calls[0][0] as string)
    expect(upstreamUrl.searchParams.get('numberOfItems')).toBe('20')
  })
})
