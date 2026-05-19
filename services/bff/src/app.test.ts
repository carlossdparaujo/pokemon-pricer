import { describe, it, expect, vi, afterEach } from 'vitest'
import app from './app'

afterEach(() => {
  vi.restoreAllMocks()
})

describe('GET /api/pokemon-cards', () => {
  it('returns cards from upstream on success', async () => {
    const cards = [
      { id: '1', name: 'Pikachu', set: 'Base', imageUrl: 'http://img/pikachu.png' },
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
})
