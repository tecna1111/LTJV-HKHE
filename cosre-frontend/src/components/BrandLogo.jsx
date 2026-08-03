import { Layers3 } from 'lucide-react';
import './BrandLogo.css';

function BrandLogo({ compact = false, inverse = false }) {
  return (
    <div className={`brand-lockup${compact ? ' brand-lockup--compact' : ''}${inverse ? ' brand-lockup--inverse' : ''}`}>
      <span className="brand-mark" aria-hidden="true"><Layers3 size={compact ? 16 : 20} /></span>
      <span className="brand-words">
        <strong>CollabSphere</strong>
        {!compact && <small>COSRE</small>}
      </span>
    </div>
  );
}

export default BrandLogo;
