import { useState } from 'react'
import styles from './App.module.css'

interface PriceSummary {
  cardId: string
  average: number
  p10: number
  p50: number
  p90: number
  p99: number
}

interface PokemonCard {
  id: string
  name: string
  set: string
  imageUrl: string
  priceSummary: PriceSummary
}

function formatPrice(value: number): string {
  return `$${value.toFixed(2)}`
}

export default function App() {
  const [name, setName] = useState('')
  const [set, setSet] = useState('')
  const [cards, setCards] = useState<PokemonCard[] | null>(null)
  const [isSearching, setIsSearching] = useState(false)
  const [page, setPage] = useState(1)

  function fetchPage(nameFilter: string, setFilter: string, pageNumber: number) {
    if (isSearching) return
    setIsSearching(true)
    const params = new URLSearchParams({ name: nameFilter, set: setFilter, page: String(pageNumber) })
    fetch(`/api/pokemon-cards?${params}`)
      .then(res => res.json())
      .then(data => setCards(data.cards))
      .finally(() => setIsSearching(false))
  }

  function search(e: React.SubmitEvent<HTMLFormElement>) {
    e.preventDefault()
    const newPage = 1
    setPage(newPage)
    fetchPage(name, set, newPage)
  }

  function goToPreviousPage() {
    const newPage = page - 1
    setPage(newPage)
    fetchPage(name, set, newPage)
  }

  function goToNextPage() {
    const newPage = page + 1
    setPage(newPage)
    fetchPage(name, set, newPage)
  }

  return (
    <main className={styles.main}>
      <div data-testid="pokeball-wrapper" className={isSearching ? styles.bouncing : undefined}>
        <svg
          data-testid="pokeball"
          className={`${styles.pokeball}${isSearching ? ` ${styles.spinning}` : ''}`}
          viewBox="0 0 100 100"
          xmlns="http://www.w3.org/2000/svg"
        >
          <circle cx="50" cy="50" r="48" fill="white" stroke="black" strokeWidth="4"/>
          <path d="M 2 50 A 48 48 0 0 1 98 50 Z" fill="#cc0000"/>
          <rect x="2" y="47" width="96" height="6" fill="black"/>
          <circle cx="50" cy="50" r="13" fill="black"/>
          <circle cx="50" cy="50" r="8" fill="white"/>
        </svg>
      </div>
      <h1 className={styles.title}>Pokémon Pricer</h1>
      <form
        data-testid="search-form"
        className={`${styles.form}${isSearching ? ` ${styles.formHidden}` : ''}`}
        onSubmit={search}
      >
        <input
          className={styles.input}
          placeholder="Pokémon name"
          value={name}
          onChange={e => setName(e.target.value)}
        />
        <input
          className={styles.input}
          placeholder="Set name"
          value={set}
          onChange={e => setSet(e.target.value)}
        />
        <button className={styles.button} type="submit">Search</button>
      </form>
      <div
        data-testid="results-container"
        className={`${styles.resultsWrapper}${(isSearching || cards === null) ? ` ${styles.resultsHidden}` : ''}`}
      >
        {cards !== null && (
          cards.length === 0 ? (
            <p className={styles.empty}>No cards found.</p>
          ) : (
            <ul className={styles.results}>
              {cards.map(card => (
                <li key={card.id} className={styles.card}>
                  <img src={card.imageUrl} alt={card.name} className={styles.cardImage} />
                  <p className={styles.cardName}>{card.name}</p>
                  <p className={styles.cardSet}>{card.set}</p>
                  <dl className={styles.prices}>
                    <div className={styles.priceRow}>
                      <dt className={styles.priceLabel}>Avg</dt>
                      <dd className={styles.priceValue}>{formatPrice(card.priceSummary.average)}</dd>
                    </div>
                    <div className={styles.priceRow}>
                      <dt className={styles.priceLabel}>P10</dt>
                      <dd className={styles.priceValue}>{formatPrice(card.priceSummary.p10)}</dd>
                    </div>
                    <div className={styles.priceRow}>
                      <dt className={styles.priceLabel}>P50</dt>
                      <dd className={styles.priceValue}>{formatPrice(card.priceSummary.p50)}</dd>
                    </div>
                    <div className={styles.priceRow}>
                      <dt className={styles.priceLabel}>P90</dt>
                      <dd className={styles.priceValue}>{formatPrice(card.priceSummary.p90)}</dd>
                    </div>
                    <div className={styles.priceRow}>
                      <dt className={styles.priceLabel}>P99</dt>
                      <dd className={styles.priceValue}>{formatPrice(card.priceSummary.p99)}</dd>
                    </div>
                  </dl>
                </li>
              ))}
            </ul>
          )
        )}
        {cards !== null && cards.length > 0 && (
          <div data-testid="pagination-bar" className={styles.pagination}>
            <button
              className={styles.button}
              onClick={goToPreviousPage}
              disabled={page === 1}
              aria-label="Previous page"
            >
              Previous
            </button>
            <span data-testid="current-page" className={styles.pageIndicator}>Page {page}</span>
            <button
              className={styles.button}
              onClick={goToNextPage}
              aria-label="Next page"
            >
              Next
            </button>
          </div>
        )}
      </div>
    </main>
  )
}
