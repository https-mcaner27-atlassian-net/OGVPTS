import { Link } from 'react-router-dom'
import EmptyState from '../components/EmptyState'

export default function NotFoundPage() {
  return (
    <EmptyState icon="signpost-split" title="Sayfa bulunamadı">
      <Link to="/">Ana sayfaya dön</Link>
    </EmptyState>
  )
}
