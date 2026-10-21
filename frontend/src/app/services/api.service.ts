import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { ApiError, Build, Repository, RepositoryWithLatestBuild, SyncResponse } from '../models/repository.model';

@Injectable({
  providedIn: 'root'
})
export class ApiService {
  private http = inject(HttpClient);
  private apiUrl = environment.apiUrl;

  // Repository endpoints
  getAllRepositories(): Observable<Repository[]> {
    return this.http.get<Repository[]>(`${this.apiUrl}/repositories`);
  }

  getRepositoryById(id: number): Observable<Repository> {
    return this.http.get<Repository>(`${this.apiUrl}/repositories/${id}`);
  }

  /** Repositories with their recent builds, in one request. */
  getRepositoriesWithLatestBuilds(): Observable<RepositoryWithLatestBuild[]> {
    return this.http.get<RepositoryWithLatestBuild[]>(`${this.apiUrl}/repositories/summary`);
  }

  /** Accepts a GitHub URL or "owner/repo". */
  addRepository(githubUrl: string, name?: string): Observable<Repository> {
    return this.http.post<Repository>(`${this.apiUrl}/repositories`, { githubUrl, name });
  }

  deleteRepository(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/repositories/${id}`);
  }

  syncRepository(id: number): Observable<SyncResponse> {
    return this.http.post<SyncResponse>(`${this.apiUrl}/repositories/${id}/sync`, {});
  }

  // Build endpoints
  getAllBuilds(): Observable<Build[]> {
    return this.http.get<Build[]>(`${this.apiUrl}/builds`);
  }

  getBuildsByRepository(repositoryId: number): Observable<Build[]> {
    return this.http.get<Build[]>(`${this.apiUrl}/builds/repository/${repositoryId}`);
  }

  /** Sync every monitored repository from GitHub. */
  triggerScheduledSync(): Observable<{ status: string; message: string }> {
    return this.http.post<{ status: string; message: string }>(`${this.apiUrl}/scheduler/trigger-sync`, {});
  }
}

/** Turn an HTTP error into a message suitable for showing to the user. */
export function errorMessage(err: unknown, fallback: string): string {
  if (err instanceof HttpErrorResponse) {
    if (err.status === 0) {
      return 'Cannot reach the Pulse API. Is the backend running?';
    }
    const body = err.error as Partial<ApiError> | null;
    if (body?.details?.length) {
      return body.details.join(', ');
    }
    if (body?.message) {
      return body.message;
    }
  }
  return fallback;
}
