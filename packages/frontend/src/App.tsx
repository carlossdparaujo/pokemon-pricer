import { useState } from 'react'
import styles from './App.module.css'
import CardList, { type Card, type PriceSummary } from './CardList'

type Tab = 'name' | 'set'

function loadPrices(
  cards: Card[],
  onData: (prices: Record<string, PriceSummary>) => void,
  onDone: () => void
) {
  const payload = cards.map(c => ({ cardId: c.id, name: c.name, set: c.set }))
  fetch('/api/pokemon-prices', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload),
  })
    .then(res => res.json())
    .then(data => onData(data as Record<string, PriceSummary>))
    .finally(onDone)
}

export default function App() {
  const [activeTab, setActiveTab] = useState<Tab>('name')

  // Tab 1: search by Pokémon name
  const [name, setName] = useState('')
  const [set, setSet] = useState('')
  const [nameCards, setNameCards] = useState<Card[] | null>(null)
  const [namePrices, setNamePrices] = useState<Record<string, PriceSummary> | null>(null)
  const [nameSearching, setNameSearching] = useState(false)
  const [namePricesLoading, setNamePricesLoading] = useState(false)
  const [page, setPage] = useState(1)

  // Tab 2: browse by set
  const [browseSet, setBrowseSet] = useState('')
  const [browseCards, setBrowseCards] = useState<Card[] | null>(null)
  const [browsePrices, setBrowsePrices] = useState<Record<string, PriceSummary> | null>(null)
  const [browseSearching, setBrowseSearching] = useState(false)
  const [browsePricesLoading, setBrowsePricesLoading] = useState(false)

  const isSearching = activeTab === 'name' ? nameSearching : browseSearching

  function fetchNamePage(nameFilter: string, setFilter: string, pageNumber: number) {
    if (nameSearching) return
    setNameSearching(true)
    setNameCards(null)
    setNamePrices(null)
    const params = new URLSearchParams({ name: nameFilter, set: setFilter, page: String(pageNumber) })
    fetch(`/api/pokemon-cards?${params}`)
      .then(res => res.json())
      .then(data => {
        const fetched: Card[] = data.cards
        setNameCards(fetched)
        if (fetched.length > 0) {
          setNamePricesLoading(true)
          loadPrices(fetched, setNamePrices, () => setNamePricesLoading(false))
        }
      })
      .finally(() => setNameSearching(false))
  }

  function searchByName(e: React.SubmitEvent<HTMLFormElement>) {
    e.preventDefault()
    window.scrollTo({ top: 0, behavior: 'smooth' })
    setPage(1)
    fetchNamePage(name, set, 1)
  }

  function goToPreviousPage() {
    window.scrollTo({ top: 0, behavior: 'smooth' })
    const newPage = page - 1
    setPage(newPage)
    fetchNamePage(name, set, newPage)
  }

  function goToNextPage() {
    window.scrollTo({ top: 0, behavior: 'smooth' })
    const newPage = page + 1
    setPage(newPage)
    fetchNamePage(name, set, newPage)
  }

  function searchBySet(e: React.SubmitEvent<HTMLFormElement>) {
    e.preventDefault()
    window.scrollTo({ top: 0, behavior: 'smooth' })
    if (browseSearching) return
    setBrowseSearching(true)
    setBrowseCards(null)
    setBrowsePrices(null)
    const params = new URLSearchParams({ set: browseSet, page: '1' })
    fetch(`/api/pokemon-cards?${params}`)
      .then(res => res.json())
      .then(data => {
        const fetched: Card[] = data.cards
        setBrowseCards(fetched)
        if (fetched.length > 0) {
          setBrowsePricesLoading(true)
          loadPrices(fetched, setBrowsePrices, () => setBrowsePricesLoading(false))
        }
      })
      .finally(() => setBrowseSearching(false))
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

      <div className={styles.tabs}>
        <button
          type="button"
          className={`${styles.tabButton}${activeTab === 'name' ? ` ${styles.activeTab}` : ''}`}
          onClick={() => setActiveTab('name')}
        >
          By Name
        </button>
        <button
          type="button"
          className={`${styles.tabButton}${activeTab === 'set' ? ` ${styles.activeTab}` : ''}`}
          onClick={() => setActiveTab('set')}
        >
          By Set
        </button>
      </div>

      {activeTab === 'name' && (
        <>
          <form
            data-testid="search-form"
            className={`${styles.form}${nameSearching ? ` ${styles.formHidden}` : ''}`}
            onSubmit={searchByName}
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
            <button className={styles.button} type="submit" disabled={!name}>Search</button>
          </form>
          <div
            data-testid="results-container"
            className={`${styles.resultsWrapper}${(nameSearching || nameCards === null) ? ` ${styles.resultsHidden}` : ''}`}
          >
            {nameCards !== null && (
              <CardList cards={nameCards} prices={namePrices} isPricesLoading={namePricesLoading} />
            )}
            {nameCards !== null && (nameCards.length >= 20 || page > 1) && (
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
                {nameCards.length >= 20 && (
                  <button
                    className={styles.button}
                    onClick={goToNextPage}
                    aria-label="Next page"
                  >
                    Next
                  </button>
                )}
              </div>
            )}
          </div>
        </>
      )}

      {activeTab === 'set' && (
        <>
          <form
            data-testid="set-search-form"
            className={`${styles.form}${browseSearching ? ` ${styles.formHidden}` : ''}`}
            onSubmit={searchBySet}
          >
            <input
              className={styles.input}
              placeholder="Set name"
              value={browseSet}
              onChange={e => setBrowseSet(e.target.value)}
            />
            <button className={styles.button} type="submit" disabled={!browseSet}>Search</button>
          </form>
          <div
            data-testid="set-results-container"
            className={`${styles.resultsWrapper}${(browseSearching || browseCards === null) ? ` ${styles.resultsHidden}` : ''}`}
          >
            {browseCards !== null && (
              <CardList cards={browseCards} prices={browsePrices} isPricesLoading={browsePricesLoading} />
            )}
          </div>
        </>
      )}
    </main>
  )
}
