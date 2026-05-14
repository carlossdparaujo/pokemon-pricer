import { render, screen } from '@testing-library/react'
import App from './App'

test('renders Pokémon Pricer heading', () => {
  render(<App />)
  expect(screen.getByRole('heading', { name: /Pokémon Pricer/i })).toBeInTheDocument()
})
