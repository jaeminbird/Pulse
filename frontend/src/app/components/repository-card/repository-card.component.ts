import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Build, BuildStatus, RepositoryWithLatestBuild } from '../../models/repository.model';

@Component({
  selector: 'app-repository-card',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './repository-card.component.html',
  styleUrls: ['./repository-card.component.css']
})
export class RepositoryCardComponent {
  @Input() data!: RepositoryWithLatestBuild;
  @Output() sync = new EventEmitter<void>();
  @Output() remove = new EventEmitter<void>();

  getStatusColor(status?: BuildStatus): string {
    if (!status) return 'bg-gray-400';

    const colors: Record<BuildStatus, string> = {
      'SUCCESS': 'bg-build-success',
      'FAILED': 'bg-build-failure',
      'PENDING': 'bg-build-pending',
      'IN_PROGRESS': 'bg-build-progress',
      'CANCELLED': 'bg-build-cancelled'
    };

    return colors[status] || 'bg-gray-400';
  }

  getStatusText(status?: BuildStatus): string {
    if (!status) return 'No builds';

    const labels: Record<BuildStatus, string> = {
      'SUCCESS': 'Passing',
      'FAILED': 'Failing',
      'PENDING': 'Queued',
      'IN_PROGRESS': 'Running',
      'CANCELLED': 'Cancelled'
    };

    return labels[status] || status;
  }

  getStatusIcon(status?: BuildStatus): string {
    if (!status) return '○';

    const icons: Record<BuildStatus, string> = {
      'SUCCESS': '✓',
      'FAILED': '✕',
      'PENDING': '○',
      'IN_PROGRESS': '↻',
      'CANCELLED': '⊘'
    };

    return icons[status] || '?';
  }

  shouldAnimate(status?: BuildStatus): boolean {
    return status === 'IN_PROGRESS';
  }

  /** Oldest first, so the strip reads left-to-right like a timeline. */
  get history(): Build[] {
    return [...(this.data.recentBuilds ?? [])].reverse();
  }

  formatDate(dateString?: string): string {
    const date = parseUtc(dateString);
    if (!date) return 'N/A';

    const diff = Date.now() - date.getTime();
    const minutes = Math.floor(diff / 60000);
    const hours = Math.floor(diff / 3600000);
    const days = Math.floor(diff / 86400000);

    if (minutes < 1) return 'Just now';
    if (minutes < 60) return `${minutes}m ago`;
    if (hours < 24) return `${hours}h ago`;
    if (days < 7) return `${days}d ago`;

    return date.toLocaleDateString();
  }

  formatDuration(build: Build): string | null {
    const start = parseUtc(build.startedAt);
    const end = parseUtc(build.completedAt);
    if (!start || !end) return null;

    const seconds = Math.max(0, Math.round((end.getTime() - start.getTime()) / 1000));
    if (seconds < 60) return `${seconds}s`;
    const minutes = Math.floor(seconds / 60);
    if (minutes < 60) return `${minutes}m ${seconds % 60}s`;
    return `${Math.floor(minutes / 60)}h ${minutes % 60}m`;
  }

  tooltip(build: Build): string {
    const parts = [this.getStatusText(build.status), build.workflowName, build.branch, this.formatDate(build.startedAt)];
    return parts.filter(Boolean).join(' · ');
  }

  getShortCommitSha(sha?: string): string {
    if (!sha) return 'N/A';
    return sha.substring(0, 7);
  }
}

/** The API sends UTC timestamps without a zone suffix; read them as UTC rather than local time. */
function parseUtc(value?: string): Date | null {
  if (!value) return null;
  const hasZone = /([zZ]|[+-]\d{2}:?\d{2})$/.test(value);
  const date = new Date(hasZone ? value : `${value}Z`);
  return isNaN(date.getTime()) ? null : date;
}
