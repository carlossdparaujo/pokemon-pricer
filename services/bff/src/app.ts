import { Hono } from 'hono'

const app = new Hono()

app.get('/api/health', (c) => c.json({ status: 'ok' }))

app.get('/api/pokemon-cards', (c) => {
  const name = c.req.query('name')
  const set = c.req.query('set')

  // Make call to pokemon-cards-service

  return c.json({ name, set })
})

export default app
