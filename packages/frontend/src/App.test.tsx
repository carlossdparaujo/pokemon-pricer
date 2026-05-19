import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import App from './App'

afterEach(() => vi.unstubAllGlobals())

interface CardFixture {
  id: string
  name: string
  set: string
  imageUrl: string
}

const stubPriceSummary = (cardId: string) => ({
  cardId,
  average: 2.5,
  p10: 1.0,
  p50: 2.25,
  p90: 4.0,
  p99: 6.5,
})

const makeCards = (n: number): CardFixture[] =>
  Array.from({ length: n }, (_, i) => ({
    id: `base1-${i}`,
    name: 'Pikachu',
    set: 'Base Set',
    imageUrl: 'https://img/pikachu.png',
  }))

function stubFetch(cards: CardFixture[], customPriceMap?: Record<string, object>) {
  const priceMap = customPriceMap ?? Object.fromEntries(
    cards.map(c => [c.id, stubPriceSummary(c.id)])
  )
  vi.stubGlobal('fetch', vi.fn()
    .mockResolvedValueOnce(new Response(JSON.stringify({ cards }), { status: 200 }))
    .mockResolvedValueOnce(new Response(JSON.stringify(priceMap), { status: 200 }))
  )
}

// For pagination tests that re-fetch on every page change
function makeSearchFetchMock(cards: CardFixture[]) {
  const priceMap = Object.fromEntries(cards.map(c => [c.id, stubPriceSummary(c.id)]))
  return vi.fn().mockImplementation((url: string | URL, options?: RequestInit) => {
    if (url.toString().includes('/api/pokemon-prices')) {
      return Promise.resolve(new Response(JSON.stringify(priceMap), { status: 200 }))
    }
    return Promise.resolve(new Response(JSON.stringify({ cards }), { status: 200 }))
  })
}

test('renders Pokémon Pricer heading', () => {
  render(<App />)
  expect(screen.getByRole('heading', { name: /Pokémon Pricer/i })).toBeInTheDocument()
})

test('scrolls to top when a search is submitted', () => {
  const scrollTo = vi.fn()
  vi.stubGlobal('scrollTo', scrollTo)
  vi.stubGlobal('fetch', vi.fn().mockReturnValue(new Promise(() => {})))

  render(<App />)
  fireEvent.submit(screen.getByTestId('search-form'))

  expect(scrollTo).toHaveBeenCalledWith({ top: 0, behavior: 'smooth' })
})

test('search submits name and set as query params to the BFF', () => {
  const fetchMock = vi.fn().mockReturnValue(new Promise(() => {}))
  vi.stubGlobal('fetch', fetchMock)

  render(<App />)
  fireEvent.change(screen.getByPlaceholderText('Pokémon name'), { target: { value: 'Pikachu' } })
  fireEvent.change(screen.getByPlaceholderText('Set name'), { target: { value: 'Base' } })
  fireEvent.submit(screen.getByRole('button', { name: /search/i }).closest('form')!)

  expect(fetchMock).toHaveBeenCalledWith('/api/pokemon-cards?name=Pikachu&set=Base&page=1')
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

test('pokeball gains spinning class and wrapper gains bouncing class while search is in progress', () => {
  vi.stubGlobal('fetch', vi.fn().mockReturnValue(new Promise(() => {})))

  render(<App />)
  const pokeball = screen.getByTestId('pokeball')
  const wrapper = screen.getByTestId('pokeball-wrapper')
  expect(pokeball.className).not.toMatch(/spinning/)
  expect(wrapper.className).not.toMatch(/bouncing/)

  fireEvent.submit(screen.getByRole('button', { name: /search/i }).closest('form')!)

  expect(pokeball.className).toMatch(/spinning/)
  expect(wrapper.className).toMatch(/bouncing/)
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
  stubFetch([{ id: 'base1-25', name: 'Pikachu', set: 'Base Set', imageUrl: 'https://img/pikachu.png' }])

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

test('results container fades back in after search completes', async () => {
  stubFetch([])

  render(<App />)
  fireEvent.submit(screen.getByTestId('search-form'))

  await waitFor(() => expect(screen.getByTestId('results-container').className).not.toMatch(/resultsHidden/))
})

test('cards are visible before prices finish loading', async () => {
  const cards = [{ id: 'base1-4', name: 'Charizard', set: 'Base Set', imageUrl: 'https://img/c.png' }]
  vi.stubGlobal('fetch', vi.fn()
    .mockResolvedValueOnce(new Response(JSON.stringify({ cards }), { status: 200 }))
    .mockReturnValueOnce(new Promise(() => {}))
  )

  render(<App />)
  fireEvent.submit(screen.getByTestId('search-form'))

  await waitFor(() => expect(screen.getByText('Charizard')).toBeInTheDocument())
  expect(screen.getByText('Base Set')).toBeInTheDocument()
  expect(screen.getByRole('img', { name: 'Charizard' })).toBeInTheDocument()
})

test('shows loading placeholder while prices are being fetched', async () => {
  const cards = [{ id: 'base1-4', name: 'Charizard', set: 'Base Set', imageUrl: 'https://img/c.png' }]
  vi.stubGlobal('fetch', vi.fn()
    .mockResolvedValueOnce(new Response(JSON.stringify({ cards }), { status: 200 }))
    .mockReturnValueOnce(new Promise(() => {}))
  )

  render(<App />)
  fireEvent.submit(screen.getByTestId('search-form'))

  await waitFor(() => expect(screen.getByText('Charizard')).toBeInTheDocument())
  expect(screen.getByTestId('prices-loading-indicator')).toBeInTheDocument()
})

test('fetches prices for all returned cards via POST /api/pokemon-prices', async () => {
  const cards = [
    { id: 'base1-4', name: 'Charizard', set: 'Base Set', imageUrl: 'https://img/c.png' },
    { id: 'base1-25', name: 'Pikachu', set: 'Base Set', imageUrl: 'https://img/p.png' },
  ]
  const fetchMock = vi.fn()
    .mockResolvedValueOnce(new Response(JSON.stringify({ cards }), { status: 200 }))
    .mockResolvedValueOnce(new Response(JSON.stringify({}), { status: 200 }))
  vi.stubGlobal('fetch', fetchMock)

  render(<App />)
  fireEvent.submit(screen.getByTestId('search-form'))
  await waitFor(() => expect(screen.getByText('Charizard')).toBeInTheDocument())

  const [pricesUrl, pricesOptions] = fetchMock.mock.calls[1]
  expect(pricesUrl).toBe('/api/pokemon-prices')
  expect(pricesOptions.method).toBe('POST')
  expect(JSON.parse(pricesOptions.body as string)).toEqual([
    { cardId: 'base1-4', name: 'Charizard', set: 'Base Set' },
    { cardId: 'base1-25', name: 'Pikachu', set: 'Base Set' },
  ])
})

test('renders price values formatted as dollars after prices load', async () => {
  const cards = [{ id: 'base1-4', name: 'Charizard', set: 'Base Set', imageUrl: 'https://img/c.png' }]
  const priceMap = {
    'base1-4': { cardId: 'base1-4', average: 350.0, p10: 200.0, p50: 330.0, p90: 500.0, p99: 750.0 },
  }
  stubFetch(cards, priceMap)

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

test('scrolls to top when navigating to the next page', async () => {
  const cards = makeCards(20)
  const fetchMock = makeSearchFetchMock(cards)
  vi.stubGlobal('fetch', fetchMock)
  const scrollTo = vi.fn()
  vi.stubGlobal('scrollTo', scrollTo)

  render(<App />)
  fireEvent.submit(screen.getByTestId('search-form'))
  await waitFor(() => expect(screen.getByTestId('pagination-bar')).toBeInTheDocument())

  vi.stubGlobal('fetch', vi.fn().mockReturnValue(new Promise(() => {})))
  scrollTo.mockClear()
  fireEvent.click(screen.getByRole('button', { name: /next page/i }))

  expect(scrollTo).toHaveBeenCalledWith({ top: 0, behavior: 'smooth' })
})

test('scrolls to top when navigating to the previous page', async () => {
  const cards = makeCards(20)
  const fetchMock = makeSearchFetchMock(cards)
  vi.stubGlobal('fetch', fetchMock)
  const scrollTo = vi.fn()
  vi.stubGlobal('scrollTo', scrollTo)

  render(<App />)
  fireEvent.submit(screen.getByTestId('search-form'))
  await waitFor(() => expect(screen.getByTestId('pagination-bar')).toBeInTheDocument())

  fireEvent.click(screen.getByRole('button', { name: /next page/i }))
  await waitFor(() => expect(screen.getByTestId('current-page').textContent).toBe('Page 2'))

  vi.stubGlobal('fetch', vi.fn().mockReturnValue(new Promise(() => {})))
  scrollTo.mockClear()
  fireEvent.click(screen.getByRole('button', { name: /previous page/i }))

  expect(scrollTo).toHaveBeenCalledWith({ top: 0, behavior: 'smooth' })
})

test('page resets to 1 when a new search starts', async () => {
  const cards = makeCards(20)
  vi.stubGlobal('fetch', makeSearchFetchMock(cards))

  render(<App />)

  fireEvent.submit(screen.getByTestId('search-form'))
  await waitFor(() => expect(screen.getByTestId('pagination-bar')).toBeInTheDocument())

  fireEvent.click(screen.getByRole('button', { name: /next page/i }))
  await waitFor(() => expect(screen.getByTestId('current-page').textContent).toBe('Page 2'))

  fireEvent.submit(screen.getByTestId('search-form'))
  await waitFor(() => expect(screen.getByTestId('current-page').textContent).toBe('Page 1'))
})

test('Previous button is disabled on page 1', async () => {
  stubFetch(makeCards(20))

  render(<App />)
  fireEvent.submit(screen.getByTestId('search-form'))

  await waitFor(() => expect(screen.getByTestId('pagination-bar')).toBeInTheDocument())

  expect(screen.getByRole('button', { name: /previous page/i })).toBeDisabled()
})

test('pagination bar is hidden when fewer than 20 cards are returned', async () => {
  stubFetch(makeCards(19))

  render(<App />)
  fireEvent.submit(screen.getByTestId('search-form'))

  await waitFor(() => expect(screen.getAllByRole('img')).toHaveLength(19))
  expect(screen.queryByTestId('pagination-bar')).not.toBeInTheDocument()
})

test('clicking Next increments page and re-fetches with new page number', async () => {
  const cards = makeCards(20)
  const fetchMock = makeSearchFetchMock(cards)
  vi.stubGlobal('fetch', fetchMock)

  render(<App />)
  fireEvent.submit(screen.getByTestId('search-form'))
  await waitFor(() => expect(screen.getByTestId('pagination-bar')).toBeInTheDocument())

  fireEvent.click(screen.getByRole('button', { name: /next page/i }))

  await waitFor(() => expect(screen.getByTestId('current-page').textContent).toBe('Page 2'))

  expect(fetchMock).toHaveBeenCalledWith(expect.stringContaining('page=2'))
})

test('clicking Previous decrements page and re-fetches with new page number', async () => {
  const cards = makeCards(20)
  const fetchMock = makeSearchFetchMock(cards)
  vi.stubGlobal('fetch', fetchMock)

  render(<App />)
  fireEvent.submit(screen.getByTestId('search-form'))
  await waitFor(() => expect(screen.getByTestId('pagination-bar')).toBeInTheDocument())

  fireEvent.click(screen.getByRole('button', { name: /next page/i }))
  await waitFor(() => expect(screen.getByTestId('current-page').textContent).toBe('Page 2'))

  fireEvent.click(screen.getByRole('button', { name: /previous page/i }))
  await waitFor(() => expect(screen.getByTestId('current-page').textContent).toBe('Page 1'))

  expect(fetchMock).toHaveBeenCalledWith(expect.stringContaining('page=1'))
})

// By Name tab: search button disabled state
test('search button is disabled when name is empty', () => {
  render(<App />)
  expect(screen.getByRole('button', { name: /search/i })).toBeDisabled()
})

test('search button is enabled when name has a value', () => {
  render(<App />)
  fireEvent.change(screen.getByPlaceholderText('Pokémon name'), { target: { value: 'Pikachu' } })
  expect(screen.getByRole('button', { name: /search/i })).not.toBeDisabled()
})

test('search button stays disabled when set is filled but name is empty', () => {
  render(<App />)
  fireEvent.change(screen.getByPlaceholderText('Set name'), { target: { value: 'Base Set' } })
  expect(screen.getByRole('button', { name: /search/i })).toBeDisabled()
})

// By Set tab
test('clicking By Set tab reveals the set search form', () => {
  render(<App />)
  fireEvent.click(screen.getByRole('button', { name: /by set/i }))
  expect(screen.getByTestId('set-search-form')).toBeInTheDocument()
})

test('By Set tab: search button is disabled when set input is empty', () => {
  render(<App />)
  fireEvent.click(screen.getByRole('button', { name: /by set/i }))
  expect(screen.getByRole('button', { name: /search/i })).toBeDisabled()
})

test('By Set tab: search button is enabled when set input has a value', () => {
  render(<App />)
  fireEvent.click(screen.getByRole('button', { name: /by set/i }))
  fireEvent.change(screen.getByPlaceholderText('Set name'), { target: { value: 'Base Set' } })
  expect(screen.getByRole('button', { name: /search/i })).not.toBeDisabled()
})

test('By Set tab: submitting calls /api/pokemon-cards with set and page=1', () => {
  const fetchMock = vi.fn().mockReturnValue(new Promise(() => {}))
  vi.stubGlobal('fetch', fetchMock)

  render(<App />)
  fireEvent.click(screen.getByRole('button', { name: /by set/i }))
  fireEvent.change(screen.getByPlaceholderText('Set name'), { target: { value: 'Base' } })
  fireEvent.submit(screen.getByTestId('set-search-form'))

  expect(fetchMock).toHaveBeenCalledWith('/api/pokemon-cards?set=Base&page=1')
})

test('By Set tab: renders cards without a pagination bar', async () => {
  const cards = makeCards(20)
  vi.stubGlobal('fetch', makeSearchFetchMock(cards))

  render(<App />)
  fireEvent.click(screen.getByRole('button', { name: /by set/i }))
  fireEvent.change(screen.getByPlaceholderText('Set name'), { target: { value: 'Base' } })
  fireEvent.submit(screen.getByTestId('set-search-form'))

  await waitFor(() => expect(screen.getAllByRole('img')).toHaveLength(20))
  expect(screen.queryByTestId('pagination-bar')).not.toBeInTheDocument()
})

test('By Set tab: shows empty state when no cards are returned', async () => {
  stubFetch([])

  render(<App />)
  fireEvent.click(screen.getByRole('button', { name: /by set/i }))
  fireEvent.change(screen.getByPlaceholderText('Set name'), { target: { value: 'Nonexistent' } })
  fireEvent.submit(screen.getByTestId('set-search-form'))

  await waitFor(() => expect(screen.getByText('No cards found.')).toBeInTheDocument())
})

test('By Set tab: pokeball spins while search is in progress', () => {
  vi.stubGlobal('fetch', vi.fn().mockReturnValue(new Promise(() => {})))

  render(<App />)
  fireEvent.click(screen.getByRole('button', { name: /by set/i }))
  fireEvent.change(screen.getByPlaceholderText('Set name'), { target: { value: 'Base' } })
  fireEvent.submit(screen.getByTestId('set-search-form'))

  expect(screen.getByTestId('pokeball').className).toMatch(/spinning/)
  expect(screen.getByTestId('pokeball-wrapper').className).toMatch(/bouncing/)
})

test('By Set tab: scrolls to top when search is submitted', () => {
  const scrollTo = vi.fn()
  vi.stubGlobal('scrollTo', scrollTo)
  vi.stubGlobal('fetch', vi.fn().mockReturnValue(new Promise(() => {})))

  render(<App />)
  fireEvent.click(screen.getByRole('button', { name: /by set/i }))
  fireEvent.change(screen.getByPlaceholderText('Set name'), { target: { value: 'Base' } })
  fireEvent.submit(screen.getByTestId('set-search-form'))

  expect(scrollTo).toHaveBeenCalledWith({ top: 0, behavior: 'smooth' })
})
