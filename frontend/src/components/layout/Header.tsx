import { APP_SUBTITLE, APP_TITLE } from '../../constants/app'

export function Header() {
  return (
    <header className="header">
      <div>
        <p className="eyebrow">Dealflow360</p>
        <h1>{APP_TITLE}</h1>
        <p className="subtitle">{APP_SUBTITLE}</p>
      </div>
    </header>
  )
}
