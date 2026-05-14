import { useState } from 'react'
import styles from './App.module.css'

interface PokemonCard {
  name: string | null
  set: string | null
}

export default function App() {
  const [name, setName] = useState('')
  const [set, setSet] = useState('')
  const [card, setCard] = useState<PokemonCard | null>(null)

  function search(e: React.SubmitEvent<HTMLFormElement>) {
    e.preventDefault()
    const params = new URLSearchParams({ name, set })
    fetch(`/api/pokemon-cards?${params}`)
      .then(res => res.json())
      .then(setCard)
  }

  return (
    <main className={styles.main}>
      <svg className={styles.pokeball} viewBox="0 0 100 100" xmlns="http://www.w3.org/2000/svg">
        <circle cx="50" cy="50" r="48" fill="white" stroke="black" strokeWidth="4"/>
        <path d="M 2 50 A 48 48 0 0 1 98 50 Z" fill="#cc0000"/>
        <rect x="2" y="47" width="96" height="6" fill="black"/>
        <circle cx="50" cy="50" r="13" fill="black"/>
        <circle cx="50" cy="50" r="8" fill="white"/>
      </svg>
      <h1 className={styles.title}>Pokémon Pricer</h1>
      <form className={styles.form} onSubmit={search}>
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
      {card && (
        <div className={styles.result}>
          <p>Name: {card.name ?? '—'}</p>
          <p>Set: {card.set ?? '—'}</p>
        </div>
      )}
    </main>
  )
}
