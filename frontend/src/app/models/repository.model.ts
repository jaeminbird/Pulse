export interface Repository {
  id: number;
  name: string;
  githubUrl: string;
  createdAt: string;
}

export interface Build {
  id: number;
  repositoryId: number;
  repositoryName: string;
  status: BuildStatus;
  commitSha: string;
  startedAt: string;
  completedAt?: string;
  githubRunId?: number;
  workflowName?: string;
  branch?: string;
  commitMessage?: string;
  authorName?: string;
  htmlUrl?: string;
}

export type BuildStatus = 'PENDING' | 'IN_PROGRESS' | 'SUCCESS' | 'FAILED' | 'CANCELLED';

export interface RepositoryWithLatestBuild {
  repository: Repository;
  latestBuild?: Build;
  recentBuilds: Build[];
  /** Percentage of finished recent builds that succeeded; null when none have finished. */
  successRate: number | null;
}

export interface SyncResponse {
  success: boolean;
  syncedCount: number;
  message: string;
  repositoryName?: string;
}

export interface ApiError {
  status: number;
  error: string;
  message: string;
  details?: string[];
}
