import { Hono } from 'hono'
import { serve } from '@hono/node-server'

const app = new Hono()

app.get('/api/health', (c) => c.json({ status: 'ok' }))

app.get('/api/pokemon-cards', (c) => {
  const name = c.req.query('name')
  const set = c.req.query('set')
  
  // Make call to pokemon-cards-service
  
  return c.json({ name, set })
})

serve({ fetch: app.fetch, port: 3000 }, () => {
  console.log('BFF running on http://localhost:3000')
})
