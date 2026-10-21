import { Component, DestroyRef, OnInit, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { EMPTY, Subject, catchError, filter, finalize, map, merge, switchMap, timer } from 'rxjs';
import { ApiService, errorMessage } from '../../services/api.service';
import { BuildStatus, RepositoryWithLatestBuild } from '../../models/repository.model';
import { RepositoryCardComponent } from '../repository-card/repository-card.component';

type StatusFilter = 'ALL' | BuildStatus | 'NONE';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule, RepositoryCardComponent],
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.css']
})
export class DashboardComponent implements OnInit {
  private apiService = inject(ApiService);
  private destroyRef = inject(DestroyRef);
  private manualRefresh$ = new Subject<void>();

  readonly refreshInterval = 30000; // 30 seconds

  repositories: RepositoryWithLatestBuild[] = [];
  loading = true;
  syncing = false;
  error: string | null = null;
  notice: string | null = null;
  lastUpdated: Date | null = null;
  autoRefresh = true;
  statusFilter: StatusFilter = 'ALL';

  newRepoUrl = '';
  adding = false;
  addError: string | null = null;

  ngOnInit(): void {
    const scheduled$ = timer(0, this.refreshInterval).pipe(
      filter((tick) => tick === 0 || this.autoRefresh),
      map(() => undefined)
    );

    merge(scheduled$, this.manualRefresh$)
      .pipe(
        // switchMap so a manual refresh supersedes a slower in-flight one
        switchMap(() => {
          this.loading = true;
          return this.apiService.getRepositoriesWithLatestBuilds().pipe(
            catchError((err) => {
              this.error = errorMessage(err, 'Failed to load repository data.');
              return EMPTY;
            }),
            finalize(() => (this.loading = false))
          );
        }),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe((data) => {
        this.repositories = data;
        this.lastUpdated = new Date();
        this.error = null;
      });
  }

  refresh(): void {
    this.manualRefresh$.next();
  }

  toggleAutoRefresh(): void {
    this.autoRefresh = !this.autoRefresh;
  }

  syncAll(): void {
    this.syncing = true;
    this.apiService.triggerScheduledSync()
      .pipe(finalize(() => (this.syncing = false)))
      .subscribe({
        next: () => {
          this.showNotice('Synced all repositories from GitHub');
          this.refresh();
        },
        error: (err) => (this.error = errorMessage(err, 'Failed to sync from GitHub.'))
      });
  }

  addRepository(): void {
    const url = this.newRepoUrl.trim();
    if (!url || this.adding) {
      return;
    }
    this.adding = true;
    this.addError = null;
    this.apiService.addRepository(url)
      .pipe(finalize(() => (this.adding = false)))
      .subscribe({
        next: (repo) => {
          this.newRepoUrl = '';
          this.showNotice(`Now monitoring ${repo.name}`);
          this.refresh();
        },
        error: (err) => (this.addError = errorMessage(err, 'Could not add repository.'))
      });
  }

  syncRepository(item: RepositoryWithLatestBuild): void {
    this.apiService.syncRepository(item.repository.id).subscribe({
      next: (res) => {
        this.showNotice(`${item.repository.name}: ${res.message}`);
        this.refresh();
      },
      error: (err) => (this.error = errorMessage(err, `Failed to sync ${item.repository.name}.`))
    });
  }

  removeRepository(item: RepositoryWithLatestBuild): void {
    if (!confirm(`Stop monitoring ${item.repository.name}? Its build history will be deleted.`)) {
      return;
    }
    this.apiService.deleteRepository(item.repository.id).subscribe({
      next: () => {
        this.repositories = this.repositories.filter((r) => r.repository.id !== item.repository.id);
        this.showNotice(`Removed ${item.repository.name}`);
      },
      error: (err) => (this.error = errorMessage(err, `Failed to remove ${item.repository.name}.`))
    });
  }

  setFilter(filter: StatusFilter): void {
    this.statusFilter = this.statusFilter === filter ? 'ALL' : filter;
  }

  get filteredRepositories(): RepositoryWithLatestBuild[] {
    if (this.statusFilter === 'ALL') {
      return this.repositories;
    }
    return this.repositories.filter((r) =>
      this.statusFilter === 'NONE' ? !r.latestBuild : r.latestBuild?.status === this.statusFilter
    );
  }

  get statusCounts() {
    const counts = { total: this.repositories.length, success: 0, failed: 0, inProgress: 0, pending: 0 };
    for (const repo of this.repositories) {
      switch (repo.latestBuild?.status) {
        case 'SUCCESS': counts.success++; break;
        case 'FAILED': counts.failed++; break;
        case 'IN_PROGRESS': counts.inProgress++; break;
        case 'PENDING': counts.pending++; break;
      }
    }
    return counts;
  }

  trackById(_: number, item: RepositoryWithLatestBuild): number {
    return item.repository.id;
  }

  private showNotice(message: string): void {
    this.notice = message;
    setTimeout(() => {
      if (this.notice === message) {
        this.notice = null;
      }
    }, 4000);
  }
}
