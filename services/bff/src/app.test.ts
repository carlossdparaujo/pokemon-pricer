import { describe, it, expect } from 'vitest'
import app from './app'

describe('GET /api/pokemon-cards', () => {
  it('returns name and set from query params', async () => {
    const res = await app.fetch(
      new Request('http://localhost/api/pokemon-cards?name=Pikachu&set=Base')
    )
    expect(res.status).toBe(200)
    expect(await res.json()).toEqual({ name: 'Pikachu', set: 'Base' })
  })
})
