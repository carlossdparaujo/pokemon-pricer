import { Hono } from 'hono'

const POKEMON_CARDS_SERVICE_URL =
  process.env.POKEMON_CARDS_SERVICE_URL ?? 'http://localhost:8080'

const app = new Hono()

app.get('/api/health', (c) => c.json({ status: 'ok' }))

app.get('/api/pokemon-cards', async (c) => {
  const name = c.req.query('name')
  const set = c.req.query('set')
  const page = c.req.query('page') ?? '1'

  const params = new URLSearchParams()
  if (name) params.set('name', name)
  if (set) params.set('set', set)
  params.set('page', page)
  params.set('numberOfItems', '20')

  try {
    const upstream = await fetch(
      `${POKEMON_CARDS_SERVICE_URL}/cards?${params.toString()}`
    )

    if (!upstream.ok) {
      return c.json({ error: 'Failed to fetch cards' }, 500)
    }

    const data = await upstream.json()
    return c.json(data)
  } catch {
    return c.json({ error: 'Failed to fetch cards' }, 500)
  }
})

export default app
