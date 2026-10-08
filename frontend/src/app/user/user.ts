import { Component, OnInit, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { AuthService } from '../auth/auth.service';

interface EvaluationItemDTO {
  id: number;
  securityActionId: number;
  actionCode: string;
  isoControl: string;
  theme: string;
  projectSubdomain: string;
  description: string;
  coefficient: number;
  implementationStatus: string;
  justification?: string;
  comment?: string;
}

interface Evaluation {
  id: number;
  organizationId: number;
  organizationName: string;
  sectorName?: string;
  status: string;
  itemCount: number;
  items?: EvaluationItemDTO[];
  createdAt: string;
  updatedAt: string;
}

@Component({
  selector: 'app-user',
  templateUrl: './user.html',
  styleUrl: './user.scss',
  standalone: false
})
export class UserComponent implements OnInit {
  assignedEvaluation = signal<Evaluation | null>(null);
  loading = signal(false);
  submitting = signal(false);
  successMessage = signal<string | null>(null);
  errorMessage = signal<string | null>(null);
  organizationLabel = signal('Organisme');

  constructor(
    private http: HttpClient,
    protected authService: AuthService
  ) {}

  ngOnInit() {
    this.loadMyAssignedEvaluation();
  }

  loadMyAssignedEvaluation() {
    this.loading.set(true);
    this.errorMessage.set(null);
    this.http.get<Evaluation>('/api/evaluations/my-assigned').subscribe({
      next: (evalObj) => {
        this.assignedEvaluation.set(evalObj);
        this.organizationLabel.set(evalObj.organizationName || 'Organisme');
        this.loading.set(false);
      },
      error: (err) => {
        this.loading.set(false);
        if (err.status === 404) {
          this.assignedEvaluation.set(null);
        } else {
          const serverMsg = err.error?.message || err.error || err.message;
          const display = typeof serverMsg === 'string' && serverMsg.trim()
            ? serverMsg
            : `Erreur ${err.status} — Impossible de charger votre évaluation.`;
          this.errorMessage.set(display);
        }
      }
    });
  }

  updateItemStatus(itemId: number, status: string) {
    const activeEval = this.assignedEvaluation();
    if (!activeEval || activeEval.status === 'SUBMITTED' || activeEval.status === 'COMPLETED') return;

    this.http.put<Evaluation>(`/api/evaluations/${activeEval.id}/items/${itemId}`, {
      implementationStatus: status
    }).subscribe({
      next: (updatedEval) => {
        this.assignedEvaluation.set(updatedEval);
      },
      error: (err) => {
        console.error('Error updating item response', err);
      }
    });
  }

  submitFinalEvaluation() {
    const activeEval = this.assignedEvaluation();
    if (!activeEval) return;

    if (!confirm('Êtes-vous sûr de vouloir soumettre définitivement votre évaluation ?')) return;

    this.submitting.set(true);
    this.successMessage.set(null);
    this.errorMessage.set(null);

    this.http.post<Evaluation>(`/api/evaluations/${activeEval.id}/submit`, {}).subscribe({
      next: (submittedEval) => {
        this.submitting.set(false);
        this.assignedEvaluation.set(submittedEval);
        this.successMessage.set('Votre évaluation de conformité PSG a été soumise avec succès !');
      },
      error: (err) => {
        this.submitting.set(false);
        this.errorMessage.set('Impossible de soumettre l\'évaluation.');
      }
    });
  }

  // --- Progress helpers ---

  /** Count items that have been answered (not NOT_PLANNED) */
  getAnsweredCount(evaluation: Evaluation): number {
    return (evaluation.items || []).filter(i => i.implementationStatus !== 'NOT_PLANNED').length;
  }

  /** Count items that are IMPLEMENTED */
  getComplianceCount(evaluation: Evaluation): number {
    return (evaluation.items || []).filter(i => i.implementationStatus === 'IMPLEMENTED').length;
  }

  /** Percent of items answered */
  getProgressPercent(evaluation: Evaluation): number {
    const total = (evaluation.items || []).length;
    if (total === 0) return 0;
    return Math.round((this.getAnsweredCount(evaluation) / total) * 100);
  }

  // --- Label helpers ---

  getStatusBadgeClass(status: string): string {
    switch (status) {
      case 'DRAFT': return 'status-draft';
      case 'ASSIGNED': return 'status-assigned';
      case 'IN_PROGRESS': return 'status-progress';
      case 'SUBMITTED': return 'status-submitted';
      case 'COMPLETED': return 'status-completed';
      default: return 'status-draft';
    }
  }

  getStatusLabel(status: string): string {
    switch (status) {
      case 'DRAFT': return 'Brouillon';
      case 'ASSIGNED': return 'Assignée';
      case 'IN_PROGRESS': return 'En cours';
      case 'SUBMITTED': return 'Soumise';
      case 'COMPLETED': return 'Terminée';
      default: return status;
    }
  }

  getImplStatusLabel(status: string): string {
    switch (status) {
      case 'NOT_PLANNED': return 'Non planifié';
      case 'PLANNED': return 'Planifié (25%)';
      case 'IN_PROGRESS': return 'En cours (50%)';
      case 'IMPLEMENTED': return 'Mis en œuvre (100%)';
      case 'NOT_APPLICABLE': return 'Non Applicable';
      default: return status;
    }
  }

  getImplBadgeClass(status: string): string {
    switch (status) {
      case 'IMPLEMENTED': return 'badge-impl-implemented';
      case 'IN_PROGRESS': return 'badge-impl-progress';
      case 'PLANNED': return 'badge-impl-planned';
      case 'NOT_APPLICABLE': return 'badge-impl-na';
      case 'NOT_PLANNED':
      default:
        return 'badge-impl-not';
    }
  }
}
