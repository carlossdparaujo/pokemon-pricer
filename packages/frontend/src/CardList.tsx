import styles from './App.module.css'

export interface PriceSummary {
  cardId: string
  average: number
  p10: number
  p50: number
  p90: number
  p99: number
}

export interface Card {
  id: string
  name: string
  set: string
  imageUrl: string
}

interface Props {
  cards: Card[]
  prices: Record<string, PriceSummary> | null
  isPricesLoading: boolean
}

function formatPrice(value: number): string {
  return `$${value.toFixed(2)}`
}

export default function CardList({ cards, prices, isPricesLoading }: Props) {
  if (cards.length === 0) {
    return <p className={styles.empty}>No cards found.</p>
  }
  return (
    <ul className={styles.results}>
      {cards.map(card => (
        <li key={card.id} className={styles.card}>
          <img src={card.imageUrl} alt={card.name} className={styles.cardImage} />
          <p className={styles.cardName}>{card.name}</p>
          <p className={styles.cardSet}>{card.set}</p>
          {(isPricesLoading || prices?.[card.id]) && (
            <div className={styles.priceArea}>
              <div
                data-testid="prices-loading-indicator"
                className={`${styles.pricesPokeballWrapper}${prices?.[card.id] ? ` ${styles.pricesPokeballHidden}` : ''}`}
              >
                <svg
                  className={styles.pricesPokeballSpinner}
                  viewBox="0 0 100 100"
                  xmlns="http://www.w3.org/2000/svg"
                >
                  <circle cx="50" cy="50" r="48" fill="#e0e0e0" stroke="#9e9e9e" strokeWidth="4"/>
                  <path d="M 2 50 A 48 48 0 0 1 98 50 Z" fill="#9e9e9e"/>
                  <rect x="2" y="47" width="96" height="6" fill="#757575"/>
                  <circle cx="50" cy="50" r="13" fill="#757575"/>
                  <circle cx="50" cy="50" r="8" fill="#e0e0e0"/>
                </svg>
              </div>
              <dl className={`${styles.prices}${!prices?.[card.id] ? ` ${styles.pricesHidden}` : ''}`}>
                {prices?.[card.id] && (
                  <>
                    <div className={styles.priceRow}>
                      <dt className={styles.priceLabel}>Avg</dt>
                      <dd className={styles.priceValue}>{formatPrice(prices[card.id].average)}</dd>
                    </div>
                    <div className={styles.priceRow}>
                      <dt className={styles.priceLabel}>P10</dt>
                      <dd className={styles.priceValue}>{formatPrice(prices[card.id].p10)}</dd>
                    </div>
                    <div className={styles.priceRow}>
                      <dt className={styles.priceLabel}>P50</dt>
                      <dd className={styles.priceValue}>{formatPrice(prices[card.id].p50)}</dd>
                    </div>
                    <div className={styles.priceRow}>
                      <dt className={styles.priceLabel}>P90</dt>
                      <dd className={styles.priceValue}>{formatPrice(prices[card.id].p90)}</dd>
                    </div>
                    <div className={styles.priceRow}>
                      <dt className={styles.priceLabel}>P99</dt>
                      <dd className={styles.priceValue}>{formatPrice(prices[card.id].p99)}</dd>
                    </div>
                  </>
                )}
              </dl>
            </div>
          )}
        </li>
      ))}
    </ul>
  )
}
