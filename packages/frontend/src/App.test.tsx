import { render, screen, fireEvent } from '@testing-library/react'
import App from './App'

afterEach(() => vi.unstubAllGlobals())

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
