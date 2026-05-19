import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import App from './App'

afterEach(() => vi.unstubAllGlobals())

function stubFetch(cards: object[]) {
  vi.stubGlobal(
    'fetch',
    vi.fn().mockResolvedValue(new Response(JSON.stringify({ cards }), { status: 200 }))
  )
}

test('renders Pokémon Pricer heading', () => {
  render(<App />)
  expect(screen.getByRole('heading', { name: /Pokémon Pricer/i })).toBeInTheDocument()
})

test('search submits name and set as query params to the BFF', () => {
  const fetchMock = vi.fn().mockReturnValue(new Promise(() => {}))
  vi.stubGlobal('fetch', fetchMock)

  render(<App />)
  fireEvent.change(screen.getByPlaceholderText('Pokémon name'), { target: { value: 'Pikachu' } })
  fireEvent.change(screen.getByPlaceholderText('Set name'), { target: { value: 'Base' } })
  fireEvent.submit(screen.getByRole('button', { name: /search/i }).closest('form')!)

  expect(fetchMock).toHaveBeenCalledWith('/api/pokemon-cards?name=Pikachu&set=Base')
})

test('renders a list of cards returned by the BFF', async () => {
  stubFetch([
    { id: 'base1-25', name: 'Pikachu', set: 'Base Set', imageUrl: 'https://img/pikachu.png' },
    { id: 'neo1-16', name: 'Pikachu', set: 'Neo Genesis', imageUrl: 'https://img/pikachu2.png' },
  ])

  render(<App />)
  fireEvent.submit(screen.getByRole('button', { name: /search/i }).closest('form')!)

  await waitFor(() => {
    expect(screen.getAllByRole('img')).toHaveLength(2)
  })
  expect(screen.getAllByText('Pikachu')).toHaveLength(2)
  expect(screen.getByText('Base Set')).toBeInTheDocument()
  expect(screen.getByText('Neo Genesis')).toBeInTheDocument()
})

test('shows empty state when no cards are returned', async () => {
  stubFetch([])

  render(<App />)
  fireEvent.submit(screen.getByRole('button', { name: /search/i }).closest('form')!)

  await waitFor(() => {
    expect(screen.getByText('No cards found.')).toBeInTheDocument()
  })
})
