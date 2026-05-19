import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import App from './App'

afterEach(() => vi.unstubAllGlobals())

const stubPriceSummary = (cardId: string) => ({
  cardId,
  average: 2.5,
  p10: 1.0,
  p50: 2.25,
  p90: 4.0,
  p99: 6.5,
})

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
    { id: 'base1-25', name: 'Pikachu', set: 'Base Set', imageUrl: 'https://img/pikachu.png', priceSummary: stubPriceSummary('base1-25') },
    { id: 'neo1-16', name: 'Pikachu', set: 'Neo Genesis', imageUrl: 'https://img/pikachu2.png', priceSummary: stubPriceSummary('neo1-16') },
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

test('pokeball gains spinning class while search is in progress', () => {
  vi.stubGlobal('fetch', vi.fn().mockReturnValue(new Promise(() => {})))

  render(<App />)
  const pokeball = screen.getByTestId('pokeball')
  expect(pokeball.className).not.toMatch(/spinning/)

  fireEvent.submit(screen.getByRole('button', { name: /search/i }).closest('form')!)

  expect(pokeball.className).toMatch(/spinning/)
})

test('form fades out while search is in progress', () => {
  vi.stubGlobal('fetch', vi.fn().mockReturnValue(new Promise(() => {})))

  render(<App />)
  const form = screen.getByTestId('search-form')
  expect(form.className).not.toMatch(/formHidden/)

  fireEvent.submit(form)

  expect(form.className).toMatch(/formHidden/)
})

test('fades out previous results when a new search starts', async () => {
  stubFetch([
    { id: 'base1-25', name: 'Pikachu', set: 'Base Set', imageUrl: 'https://img/pikachu.png', priceSummary: stubPriceSummary('base1-25') },
  ])

  render(<App />)
  fireEvent.submit(screen.getByTestId('search-form'))
  await waitFor(() => expect(screen.getByText('Pikachu')).toBeInTheDocument())

  vi.stubGlobal('fetch', vi.fn().mockReturnValue(new Promise(() => {})))
  fireEvent.submit(screen.getByTestId('search-form'))

  expect(screen.getByTestId('results-container').className).toMatch(/resultsHidden/)
})

test('form fades back in after search completes', async () => {
  stubFetch([])

  render(<App />)
  const form = screen.getByTestId('search-form')
  fireEvent.submit(form)

  await waitFor(() => expect(form.className).not.toMatch(/formHidden/))
})

test('renders priceSummary values formatted as dollars', async () => {
  stubFetch([
    {
      id: 'base1-4',
      name: 'Charizard',
      set: 'Base Set',
      imageUrl: 'https://img/charizard.png',
      priceSummary: {
        cardId: 'base1-4',
        average: 350.0,
        p10: 200.0,
        p50: 330.0,
        p90: 500.0,
        p99: 750.0,
      },
    },
  ])

  render(<App />)
  fireEvent.submit(screen.getByRole('button', { name: /search/i }).closest('form')!)

  await waitFor(() => {
    expect(screen.getByText('$350.00')).toBeInTheDocument()
  })
  expect(screen.getByText('$200.00')).toBeInTheDocument()
  expect(screen.getByText('$330.00')).toBeInTheDocument()
  expect(screen.getByText('$500.00')).toBeInTheDocument()
  expect(screen.getByText('$750.00')).toBeInTheDocument()
})
