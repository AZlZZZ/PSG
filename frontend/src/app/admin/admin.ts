import { Component, OnInit, signal, computed } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { AuthService } from '../auth/auth.service';

interface User {
  id: number;
  username: string;
  organizationName: string;
}

interface EvaluationProject {
  id: number;
  title: string;
  description: string;
  assignedUser: User;
  deadline: string;
  status: string;
  physicalSecurityStatus?: string;
  networkSecurityStatus?: string;
  dataProtectionStatus?: string;
  comments?: string;
}

interface Sector {
  id: number;
  name: string;
  description?: string;
}

interface Organization {
  id: number;
  name: string;
  sectorId: number;
  sectorName: string;
  userCount?: number;
  createdAt?: string;
  updatedAt?: string;
}

interface FirmUser {
  id: number;
  username: string;
  role: string;
  organizationId: number;
  organizationName: string;
  sectorName?: string;
}

interface EvaluationItemDTO {
  id: number;
  securityActionId: number;
  actionCode: string;
  isoControl: string;
  theme: string;
  projectSubdomain: string;
  description: string;
  coefficient: number;
  sectorDefaultCoefficient?: number;
  implementationStatus: string;
  justification?: string;
  comment?: string;
}

interface Evaluation {
  id: number;
  organizationId: number;
  organizationName: string;
  sectorId?: number;
  sectorName?: string;
  status: string;
  itemCount: number;
  items?: EvaluationItemDTO[];
  startedAt?: string;
  submittedAt?: string;
  createdAt: string;
  updatedAt: string;
}

export interface EvaluationScoreResult {
  evaluationId: number;
  organizationName: string;
  sectorName: string;
  overallScore: number | null;
  overallLevel: string;
  applicableActionCount: number;
  notApplicableActionCount: number;
  themeScores?: {
    theme: string;
    score: number | null;
    level: string;
    applicableActionCount: number;
  }[];
  subdomainScores?: {
    subdomain: string;
    theme: string;
    score: number | null;
    level: string;
    applicableActionCount: number;
  }[];
  actionResults?: {
    actionCode: string;
    isoControl?: string;
    actionDescription: string;
    coefficient: number;
    implementationStatus: string;
    implementationRate: number | null;
    weightedContribution: number | null;
    weaknessPriority: number | null;
    priorityLevel: string | null;
  }[];
}

export interface PsgActionDTO {
  id: number;
  code: string;
  isoControl: string;
  theme: string;
  projectSubdomain: string;
  description: string;
  coefficient: number;
  sectorId?: number;
  sectorName?: string;
}

export interface PsgDisplayRow {
  id: number;
  code: string;
  isoControl: string;
  theme: string;
  projectSubdomain: string;
  description: string;
  coefficient: number;
  evaluationItemId?: number;
  implementationStatus?: string;
  justification?: string;
  comment?: string;
}

export type AdminTab = 'dashboard' | 'organizations' | 'evaluations' | 'analytics' | 'psg';

@Component({
  selector: 'app-admin',
  templateUrl: './admin.html',
  styleUrl: './admin.scss',
  standalone: false
})
export class Admin implements OnInit {
  activeTab = signal<AdminTab>('dashboard');

  projects = signal<EvaluationProject[]>([]);
  sectors = signal<Sector[]>([]);
  organizations = signal<Organization[]>([]);
  evaluations = signal<Evaluation[]>([]);
  firmUsers = signal<FirmUser[]>([]);

  // Loading & Error states
  loadingEvaluations = signal(false);
  loadingOrganizations = signal(false);
  loadingSectors = signal(false);
  loadingFirmUsers = signal(false);

  orgForm: FormGroup;
  evalForm: FormGroup;
  accountForm: FormGroup;

  orgSaving = signal(false);
  evalSaving = signal(false);
  accountSaving = signal(false);

  editingOrgId = signal<number | null>(null);
  editingEvalId = signal<number | null>(null);
  editingAccountOrg = signal<Organization | null>(null);

  // Account management modal state
  selectedOrgAccount = signal<Organization | null>(null);

  // Selected Org preview in Eval form
  selectedOrgForEval = signal<Organization | null>(null);

  // Confirmation modal state for evaluation assignment ("Envoyer / Assigner")
  assignConfirmEval = signal<Evaluation | null>(null);
  assigningEval = signal(false);

  // Messages
  orgMessage = signal<string | null>(null);
  orgError = signal<string | null>(null);
  evalMessage = signal<string | null>(null);
  evalError = signal<string | null>(null);
  accountMessage = signal<string | null>(null);
  accountError = signal<string | null>(null);

  // Computed stats
  totalOrganizations = computed(() => this.organizations().length);
  totalEvaluations = computed(() => this.evaluations().length);
  inProgressEvaluations = computed(() =>
    this.evaluations().filter(e => e.status === 'IN_PROGRESS' || e.status === 'ASSIGNED').length
  );
  completedEvaluations = computed(() =>
    this.evaluations().filter(e => e.status === 'COMPLETED').length
  );
  submittedEvaluations = computed(() =>
    this.evaluations().filter(e => e.status === 'SUBMITTED')
  );
  submittedCount = computed(() => this.submittedEvaluations().length);
  draftEvaluationsCount = computed(() =>
    this.evaluations().filter(e => e.status === 'DRAFT').length
  );

  // Evaluation table filter status: 'ALL' | 'SUBMITTED' | 'IN_PROGRESS' | 'ASSIGNED' | 'DRAFT' | 'COMPLETED'
  evalFilterStatus = signal<string>('ALL');

  filteredEvaluations = computed(() => {
    const filter = this.evalFilterStatus();
    if (filter === 'ALL') return this.evaluations();
    if (filter === 'IN_PROGRESS') {
      return this.evaluations().filter(e => e.status === 'IN_PROGRESS' || e.status === 'ASSIGNED');
    }
    return this.evaluations().filter(e => e.status === filter);
  });

  // --- PSG STATE SIGNALS ---
  psgSelectedSectorId = signal<number | null>(null);
  psgSelectedEvalId = signal<number | null>(null); // null = "Matrice de référence sectorielle"
  psgReferenceActions = signal<PsgActionDTO[]>([]);
  psgActiveEvaluation = signal<Evaluation | null>(null);
  loadingPsgActions = signal(false);
  loadingPsgEvaluation = signal(false);

  // PSG Filters & Search
  psgSearchQuery = signal<string>('');
  psgSelectedTheme = signal<string>('ALL');
  psgSelectedCoeff = signal<number | null>(null);
  psgSelectedStatus = signal<string>('ALL');

  // PSG Selected Sector
  psgSelectedSector = computed(() => {
    const secId = this.psgSelectedSectorId();
    if (!secId) return null;
    return this.sectors().find(s => s.id === secId) || null;
  });

  // PSG Available Evaluations for the selected sector (or all if no sector selected)
  psgAvailableEvaluations = computed(() => {
    const sectorId = this.psgSelectedSectorId();
    const all = this.evaluations();
    if (!sectorId) return all;
    return all.filter(e => {
      if (e.sectorId && e.sectorId === sectorId) return true;
      const org = this.organizations().find(o => o.id === e.organizationId);
      return org && org.sectorId === sectorId;
    });
  });

  // PSG Display Rows (unified for Evaluation Items or Reference Matrix)
  psgDisplayRows = computed<PsgDisplayRow[]>(() => {
    const activeEval = this.psgActiveEvaluation();
    const query = this.psgSearchQuery().trim().toLowerCase();
    const theme = this.psgSelectedTheme();
    const coeff = this.psgSelectedCoeff();
    const status = this.psgSelectedStatus();

    let rows: PsgDisplayRow[] = [];

    if (activeEval && activeEval.items && activeEval.items.length > 0) {
      rows = activeEval.items.map(item => ({
        id: item.id,
        code: item.actionCode,
        isoControl: item.isoControl,
        theme: item.theme,
        projectSubdomain: item.projectSubdomain,
        description: item.description,
        coefficient: item.coefficient,
        evaluationItemId: item.id,
        implementationStatus: item.implementationStatus,
        justification: item.justification,
        comment: item.comment
      }));
    } else {
      rows = this.psgReferenceActions().map(action => ({
        id: action.id,
        code: action.code,
        isoControl: action.isoControl,
        theme: action.theme,
        projectSubdomain: action.projectSubdomain,
        description: action.description,
        coefficient: action.coefficient
      }));
    }

    return rows.filter(r => {
      // Filter by theme
      if (theme !== 'ALL' && r.theme.toLowerCase() !== theme.toLowerCase()) {
        return false;
      }
      // Filter by coefficient
      if (coeff !== null && r.coefficient !== coeff) {
        return false;
      }
      // Filter by implementation status (only applicable when an evaluation is active)
      if (activeEval && status !== 'ALL' && r.implementationStatus !== status) {
        return false;
      }
      // Filter by text search
      if (query) {
        const matchesCode = r.code?.toLowerCase().includes(query);
        const matchesIso = r.isoControl?.toLowerCase().includes(query);
        const matchesDesc = r.description?.toLowerCase().includes(query);
        const matchesSubdomain = r.projectSubdomain?.toLowerCase().includes(query);
        if (!matchesCode && !matchesIso && !matchesDesc && !matchesSubdomain) {
          return false;
        }
      }
      return true;
    });
  });

  // PSG Theme Counts for Filter Pills
  psgThemeCounts = computed(() => {
    const activeEval = this.psgActiveEvaluation();
    const all = (activeEval && activeEval.items && activeEval.items.length > 0) 
      ? activeEval.items 
      : this.psgReferenceActions();

    let org = 0, people = 0, physical = 0, tech = 0;
    for (const item of all) {
      const t = ('theme' in item ? item.theme : '').toLowerCase();
      if (t === 'organizational') org++;
      else if (t === 'people') people++;
      else if (t === 'physical') physical++;
      else if (t === 'technological') tech++;
    }
    return {
      total: all.length,
      org,
      people,
      physical,
      tech
    };
  });

  // PSG Evaluation Compliance Stats
  psgComplianceStats = computed(() => {
    const activeEval = this.psgActiveEvaluation();
    if (!activeEval || !activeEval.items) {
      return null;
    }
    const items = activeEval.items;
    const total = items.length;
    const implemented = items.filter(i => i.implementationStatus === 'IMPLEMENTED').length;
    const inProgress = items.filter(i => i.implementationStatus === 'IN_PROGRESS').length;
    const planned = items.filter(i => i.implementationStatus === 'PLANNED').length;
    const notPlanned = items.filter(i => i.implementationStatus === 'NOT_PLANNED').length;
    const notApplicable = items.filter(i => i.implementationStatus === 'NOT_APPLICABLE').length;
    const answered = total - notPlanned;
    const progressPercent = total > 0 ? Math.round((answered / total) * 100) : 0;
    return {
      total,
      implemented,
      inProgress,
      planned,
      notPlanned,
      notApplicable,
      answered,
      progressPercent
    };
  });

  // =========================================================================
  // ANALYTICS & RESULTS STATE SIGNALS & COMPUTEDS
  // =========================================================================
  analyticsSelectedEvalId = signal<number | null>(null);
  analyticsScoreData = signal<EvaluationScoreResult | null>(null);
  analyticsLoading = signal(false);
  analyticsError = signal<string | null>(null);

  // Compare mode
  analyticsCompareMode = signal(false);
  analyticsCompareEvalId = signal<number | null>(null);
  analyticsCompareScoreData = signal<EvaluationScoreResult | null>(null);
  analyticsCompareLoading = signal(false);

  // Weaknesses filtering & sorting
  analyticsWeaknessPriorityFilter = signal<string>('ALL');
  analyticsWeaknessStatusFilter = signal<string>('ALL');
  analyticsWeaknessSearch = signal<string>('');
  analyticsWeaknessSortBy = signal<'priority-desc' | 'priority-asc' | 'coeff-desc' | 'code-asc'>('priority-desc');

  // Subdomain filtering & sorting
  analyticsSubdomainThemeFilter = signal<string>('ALL');
  analyticsSubdomainSearch = signal<string>('');
  analyticsSubdomainSortBy = signal<'score-asc' | 'score-desc' | 'name-asc'>('score-asc');

  selectedAnalyticsEval = computed(() => {
    const id = this.analyticsSelectedEvalId();
    if (!id) return null;
    return this.evaluations().find(e => e.id === id) || null;
  });

  selectedCompareEval = computed(() => {
    const id = this.analyticsCompareEvalId();
    if (!id) return null;
    return this.evaluations().find(e => e.id === id) || null;
  });

  analyticsPriorityDistribution = computed(() => {
    const score = this.analyticsScoreData();
    if (!score || !score.actionResults) {
      return {
        critique: 0,
        elevee: 0,
        moyenne: 0,
        faible: 0,
        aucun: 0,
        na: 0,
        total: 0,
        critiquePct: 0,
        eleveePct: 0,
        moyennePct: 0,
        faiblePct: 0,
        aucunPct: 0,
      };
    }
    const actions = score.actionResults;
    let critique = 0, elevee = 0, moyenne = 0, faible = 0, aucun = 0, na = 0;

    for (const a of actions) {
      if (a.implementationStatus === 'NOT_APPLICABLE') {
        na++;
        continue;
      }
      const lvl = (a.priorityLevel || '').toLowerCase();
      if (lvl === 'critique') critique++;
      else if (lvl === 'élevée' || lvl === 'elevee') elevee++;
      else if (lvl === 'moyenne') moyenne++;
      else if (lvl === 'faible') faible++;
      else aucun++;
    }

    const total = critique + elevee + moyenne + faible + aucun;
    const calcPct = (cnt: number) => total > 0 ? Math.round((cnt / total) * 100) : 0;

    return {
      critique,
      elevee,
      moyenne,
      faible,
      aucun,
      na,
      total,
      critiquePct: calcPct(critique),
      eleveePct: calcPct(elevee),
      moyennePct: calcPct(moyenne),
      faiblePct: calcPct(faible),
      aucunPct: calcPct(aucun),
    };
  });

  analyticsComparePriorityDistribution = computed(() => {
    const score = this.analyticsCompareScoreData();
    if (!score || !score.actionResults) return null;
    const actions = score.actionResults;
    let critique = 0, elevee = 0, moyenne = 0, faible = 0, aucun = 0, na = 0;

    for (const a of actions) {
      if (a.implementationStatus === 'NOT_APPLICABLE') {
        na++;
        continue;
      }
      const lvl = (a.priorityLevel || '').toLowerCase();
      if (lvl === 'critique') critique++;
      else if (lvl === 'élevée' || lvl === 'elevee') elevee++;
      else if (lvl === 'moyenne') moyenne++;
      else if (lvl === 'faible') faible++;
      else aucun++;
    }

    const total = critique + elevee + moyenne + faible + aucun;
    const calcPct = (cnt: number) => total > 0 ? Math.round((cnt / total) * 100) : 0;

    return {
      critique,
      elevee,
      moyenne,
      faible,
      aucun,
      na,
      total,
      critiquePct: calcPct(critique),
      eleveePct: calcPct(elevee),
      moyennePct: calcPct(moyenne),
      faiblePct: calcPct(faible),
      aucunPct: calcPct(aucun),
    };
  });

  analyticsDomainCards = computed(() => {
    const score = this.analyticsScoreData();
    const themes = score?.themeScores || [];

    const domainDefs = [
      {
        key: 'Organizational',
        name: 'Organisationnel',
        subtitle: 'Gouvernance, politiques & gestion du risque',
        icon: 'shield'
      },
      {
        key: 'People',
        name: 'Ressources Humaines',
        subtitle: 'Sensibilisation, conformité & habilitations',
        icon: 'users'
      },
      {
        key: 'Physical',
        name: 'Sécurité Physique',
        subtitle: 'Protection des locaux, périmètres & actifs',
        icon: 'lock'
      },
      {
        key: 'Technological',
        name: 'Technologique & SI',
        subtitle: 'Réseaux, systèmes d\'information, IAM & chiffrement',
        icon: 'cpu'
      }
    ];

    return domainDefs.map(def => {
      const match = themes.find(t => t.theme.toLowerCase() === def.key.toLowerCase());
      const rawScore = match && match.score !== null && match.score !== undefined ? match.score : null;
      const level = match?.level || 'Non évalué';
      const count = match?.applicableActionCount || 0;
      return {
        ...def,
        score: rawScore,
        scoreFormatted: rawScore !== null ? `${rawScore}%` : 'N/A',
        scorePercent: rawScore !== null ? Math.min(100, Math.max(0, rawScore)) : 0,
        level,
        count
      };
    });
  });

  analyticsCompareDomainCards = computed(() => {
    const score = this.analyticsCompareScoreData();
    if (!score) return null;
    const themes = score.themeScores || [];

    const domainDefs = [
      { key: 'Organizational', name: 'Organisationnel' },
      { key: 'People', name: 'Ressources Humaines' },
      { key: 'Physical', name: 'Sécurité Physique' },
      { key: 'Technological', name: 'Technologique & SI' }
    ];

    return domainDefs.map(def => {
      const match = themes.find(t => t.theme.toLowerCase() === def.key.toLowerCase());
      const rawScore = match && match.score !== null && match.score !== undefined ? match.score : null;
      return {
        ...def,
        score: rawScore,
        scoreFormatted: rawScore !== null ? `${rawScore}%` : 'N/A',
        scorePercent: rawScore !== null ? Math.min(100, Math.max(0, rawScore)) : 0,
        level: match?.level || 'Non évalué',
        count: match?.applicableActionCount || 0
      };
    });
  });

  analyticsFilteredWeaknesses = computed(() => {
    const score = this.analyticsScoreData();
    if (!score || !score.actionResults) return [];

    const search = this.analyticsWeaknessSearch().trim().toLowerCase();
    const pFilter = this.analyticsWeaknessPriorityFilter();
    const sFilter = this.analyticsWeaknessStatusFilter();
    const sortBy = this.analyticsWeaknessSortBy();

    let list = score.actionResults.filter(a => {
      if (pFilter !== 'ALL') {
        const lvl = (a.priorityLevel || '').toLowerCase();
        if (pFilter.toLowerCase() === 'critique' && lvl !== 'critique') return false;
        if ((pFilter.toLowerCase() === 'elevee' || pFilter.toLowerCase() === 'élevée') && lvl !== 'élevée' && lvl !== 'elevee') return false;
        if (pFilter.toLowerCase() === 'moyenne' && lvl !== 'moyenne') return false;
        if (pFilter.toLowerCase() === 'faible' && lvl !== 'faible') return false;
        if (pFilter.toLowerCase() === 'aucun' && lvl !== 'aucun') return false;
      }

      if (sFilter !== 'ALL' && a.implementationStatus !== sFilter) {
        return false;
      }

      if (search) {
        const code = a.actionCode?.toLowerCase() || '';
        const iso = a.isoControl?.toLowerCase() || '';
        const desc = a.actionDescription?.toLowerCase() || '';
        if (!code.includes(search) && !iso.includes(search) && !desc.includes(search)) {
          return false;
        }
      }
      return true;
    });

    list = [...list].sort((a, b) => {
      if (sortBy === 'priority-desc') {
        return (b.weaknessPriority ?? -1) - (a.weaknessPriority ?? -1);
      }
      if (sortBy === 'priority-asc') {
        return (a.weaknessPriority ?? 999) - (b.weaknessPriority ?? 999);
      }
      if (sortBy === 'coeff-desc') {
        return (b.coefficient ?? 0) - (a.coefficient ?? 0);
      }
      if (sortBy === 'code-asc') {
        return (a.actionCode || '').localeCompare(b.actionCode || '');
      }
      return 0;
    });

    return list;
  });

  analyticsFilteredSubdomains = computed(() => {
    const score = this.analyticsScoreData();
    if (!score || !score.subdomainScores) return [];

    const theme = this.analyticsSubdomainThemeFilter();
    const search = this.analyticsSubdomainSearch().trim().toLowerCase();
    const sortBy = this.analyticsSubdomainSortBy();

    let list = score.subdomainScores.filter(s => {
      if (theme !== 'ALL' && s.theme.toLowerCase() !== theme.toLowerCase()) {
        return false;
      }
      if (search) {
        const name = s.subdomain?.toLowerCase() || '';
        const tName = s.theme?.toLowerCase() || '';
        if (!name.includes(search) && !tName.includes(search)) {
          return false;
        }
      }
      return true;
    });

    list = [...list].sort((a, b) => {
      if (sortBy === 'score-asc') {
        return (a.score ?? 999) - (b.score ?? 999);
      }
      if (sortBy === 'score-desc') {
        return (b.score ?? -1) - (a.score ?? -1);
      }
      if (sortBy === 'name-asc') {
        return (a.subdomain || '').localeCompare(b.subdomain || '');
      }
      return 0;
    });

    return list;
  });

  analyticsOverallDelta = computed(() => {
    const a = this.analyticsScoreData()?.overallScore;
    const b = this.analyticsCompareScoreData()?.overallScore;
    if (a === null || a === undefined || b === null || b === undefined) return null;
    return Number((b - a).toFixed(2));
  });

  constructor(
    private http: HttpClient,
    private fb: FormBuilder,
    protected authService: AuthService
  ) {
    this.orgForm = this.fb.group({
      name: ['', [Validators.required]],
      sectorId: ['', [Validators.required]]
    });

    this.evalForm = this.fb.group({
      organizationId: ['', [Validators.required]],
      status: ['DRAFT']
    });

    this.accountForm = this.fb.group({
      username: ['', [Validators.required]],
      password: ['']
    });

    // Watch organizationId changes to update preview card
    this.evalForm.get('organizationId')?.valueChanges.subscribe(val => {
      if (!val) {
        this.selectedOrgForEval.set(null);
        return;
      }
      const orgId = Number(val);
      const found = this.organizations().find(o => o.id === orgId) || null;
      this.selectedOrgForEval.set(found);
    });
  }

  ngOnInit() {
    this.loadSectors();
    this.loadOrganizations();
    this.loadEvaluations();
    this.loadFirmUsers();
    this.loadPsgReferenceActions();
  }

  setActiveTab(tab: AdminTab) {
    this.activeTab.set(tab);
    if (tab === 'analytics') {
      if (!this.analyticsSelectedEvalId() && this.evaluations().length > 0) {
        const candidate = this.evaluations().find(e => e.status === 'SUBMITTED' || e.status === 'COMPLETED') || this.evaluations()[0];
        if (candidate) {
          this.analyticsSelectedEvalId.set(candidate.id);
        }
      }
      if (this.analyticsSelectedEvalId() && !this.analyticsScoreData() && !this.analyticsLoading()) {
        this.loadAnalyticsScore(this.analyticsSelectedEvalId()!);
      }
    }
    if (tab === 'psg' && this.psgReferenceActions().length === 0) {
      this.loadPsgReferenceActions(this.psgSelectedSectorId());
    }
  }

  loadSectors() {
    this.loadingSectors.set(true);
    this.http.get<Sector[]>('/api/admin/sectors').subscribe({
      next: (res) => {
        this.sectors.set(Array.isArray(res) ? res : []);
        this.loadingSectors.set(false);
      },
      error: (err) => {
        this.sectors.set([]);
        this.loadingSectors.set(false);
        this.orgError.set(this.mapHttpError(err, 'Impossible de charger les secteurs.'));
      }
    });
  }

  loadOrganizations() {
    this.loadingOrganizations.set(true);
    this.http.get<Organization[]>('/api/admin/organizations').subscribe({
      next: (res) => {
        this.organizations.set(Array.isArray(res) ? res : []);
        this.orgError.set(null);
        this.loadingOrganizations.set(false);
      },
      error: (err) => {
        this.organizations.set([]);
        this.loadingOrganizations.set(false);
        this.orgError.set(this.mapHttpError(err, 'Impossible de charger les organisations.'));
      }
    });
  }

  loadEvaluations() {
    this.loadingEvaluations.set(true);
    this.http.get<Evaluation[]>('/api/admin/evaluations').subscribe({
      next: (res) => {
        const evals = Array.isArray(res) ? res : [];
        this.evaluations.set(evals);
        this.evalError.set(null);
        this.loadingEvaluations.set(false);
        if (!this.analyticsSelectedEvalId() && evals.length > 0) {
          const candidate = evals.find(e => e.status === 'SUBMITTED' || e.status === 'COMPLETED') || evals[0];
          if (candidate) {
            this.analyticsSelectedEvalId.set(candidate.id);
          }
        }
      },
      error: (err) => {
        this.evaluations.set([]);
        this.loadingEvaluations.set(false);
        this.evalError.set(this.mapHttpError(err, 'Impossible de charger les évaluations.'));
      }
    });
  }

  loadFirmUsers() {
    this.loadingFirmUsers.set(true);
    this.http.get<FirmUser[]>('/api/admin/firm-users').subscribe({
      next: (res) => {
        this.firmUsers.set(Array.isArray(res) ? res : []);
        this.loadingFirmUsers.set(false);
      },
      error: (err) => {
        this.firmUsers.set([]);
        this.loadingFirmUsers.set(false);
      }
    });
  }

  // --- ORGANIZATIONS ---

  startEditOrganization(org: Organization) {
    this.editingOrgId.set(org.id);
    this.orgForm.patchValue({
      name: org.name,
      sectorId: org.sectorId
    });
    this.orgMessage.set(null);
    this.orgError.set(null);
  }

  cancelEditOrganization() {
    this.editingOrgId.set(null);
    this.orgForm.reset({ name: '', sectorId: '' });
  }

  saveOrganization() {
    if (this.orgForm.invalid) return;

    this.orgSaving.set(true);
    this.orgMessage.set(null);
    this.orgError.set(null);

    const body = {
      name: this.orgForm.value.name,
      sectorId: Number(this.orgForm.value.sectorId)
    };
    const editingId = this.editingOrgId();
    const request$ = editingId
      ? this.http.put<Organization>(`/api/admin/organizations/${editingId}`, body)
      : this.http.post<Organization>('/api/admin/organizations', body);

    request$.subscribe({
      next: () => {
        this.orgSaving.set(false);
        this.orgMessage.set(editingId ? 'Organisation mise à jour avec succès.' : 'Organisation créée avec succès.');
        this.cancelEditOrganization();
        this.loadOrganizations();
        this.loadFirmUsers();
      },
      error: (err) => {
        this.orgSaving.set(false);
        this.orgError.set(this.mapHttpError(err, 'Impossible d\'enregistrer l\'organisation.'));
      }
    });
  }

  deleteOrganization(org: Organization) {
    if (!confirm(`Confirmer la suppression de l'organisation « ${org.name} » ?`)) return;

    this.orgMessage.set(null);
    this.orgError.set(null);
    this.http.delete(`/api/admin/organizations/${org.id}`).subscribe({
      next: () => {
        this.orgMessage.set('Organisation supprimée.');
        if (this.editingOrgId() === org.id) {
          this.cancelEditOrganization();
        }
        this.loadOrganizations();
        this.loadFirmUsers();
      },
      error: (err) => {
        this.orgError.set(this.mapHttpError(err, 'Impossible de supprimer cette organisation.'));
      }
    });
  }

  // --- ORGANIZATION ACCOUNT (USERNAME & PASSWORD) MANAGEMENT ---

  getFirmUserForOrg(orgId: number): FirmUser | null {
    return this.firmUsers().find(u => u.organizationId === orgId) || null;
  }

  openAccountModal(org: Organization) {
    this.selectedOrgAccount.set(org);
    const existingUser = this.getFirmUserForOrg(org.id);
    
    if (existingUser) {
      this.accountForm.setValue({
        username: existingUser.username,
        password: ''
      });
      this.accountForm.get('password')?.clearValidators();
    } else {
      this.accountForm.setValue({
        username: org.name.toLowerCase().replace(/[^a-z0-9]/g, '') + '_user',
        password: ''
      });
      this.accountForm.get('password')?.setValidators([Validators.required]);
    }
    this.accountForm.get('password')?.updateValueAndValidity();
    this.accountMessage.set(null);
    this.accountError.set(null);
  }

  closeAccountModal() {
    this.selectedOrgAccount.set(null);
    this.accountForm.reset();
  }

  saveAccount() {
    const org = this.selectedOrgAccount();
    if (!org) return;

    const existingUser = this.getFirmUserForOrg(org.id);
    if (!existingUser && (!this.accountForm.value.password || !this.accountForm.value.password.trim())) {
      this.accountError.set('Le mot de passe est obligatoire pour la création du compte.');
      return;
    }

    if (this.accountForm.invalid) return;

    this.accountSaving.set(true);
    this.accountMessage.set(null);
    this.accountError.set(null);

    const formVal = this.accountForm.value;
    const body: { username: string; organizationId: number; password?: string } = {
      username: formVal.username.trim(),
      organizationId: org.id
    };

    if (formVal.password && formVal.password.trim()) {
      body.password = formVal.password.trim();
    }

    const request$ = existingUser
      ? this.http.put<FirmUser>(`/api/admin/firm-users/${existingUser.id}`, body)
      : this.http.post<FirmUser>('/api/admin/firm-users', body);

    request$.subscribe({
      next: () => {
        this.accountSaving.set(false);
        this.orgMessage.set(`Compte d'accès mis à jour avec succès pour « ${org.name} ».`);
        this.closeAccountModal();
        this.loadFirmUsers();
        this.loadOrganizations();
      },
      error: (err) => {
        this.accountSaving.set(false);
        this.accountError.set(this.mapHttpError(err, 'Impossible d\'enregistrer les identifiants.'));
      }
    });
  }

  // --- EVALUATIONS ---

  startCreateEvaluation() {
    this.editingEvalId.set(null);
    this.evalForm.reset({
      organizationId: '',
      status: 'DRAFT'
    });
    this.selectedOrgForEval.set(null);
    this.evalMessage.set(null);
    this.evalError.set(null);
    setTimeout(() => {
      const el = document.getElementById('evalFormCard');
      if (el) el.scrollIntoView({ behavior: 'smooth', block: 'start' });
    }, 50);
  }

  startEditEvaluation(evalItem: Evaluation) {
    this.editingEvalId.set(evalItem.id);
    this.evalForm.patchValue({
      organizationId: evalItem.organizationId,
      status: evalItem.status
    });
    const found = this.organizations().find(o => o.id === evalItem.organizationId) || null;
    this.selectedOrgForEval.set(found);
    this.evalMessage.set(null);
    this.evalError.set(null);
    setTimeout(() => {
      const el = document.getElementById('evalFormCard');
      if (el) el.scrollIntoView({ behavior: 'smooth', block: 'start' });
    }, 50);
  }

  cancelEditEvaluation() {
    this.editingEvalId.set(null);
    this.evalForm.reset({ organizationId: '', status: 'DRAFT' });
    this.selectedOrgForEval.set(null);
  }

  saveEvaluation() {
    if (this.evalForm.invalid) return;

    this.evalSaving.set(true);
    this.evalMessage.set(null);
    this.evalError.set(null);

    const formVal = this.evalForm.value;
    const editingId = this.editingEvalId();

    if (editingId) {
      // Update status
      const body = { status: formVal.status };
      this.http.put<Evaluation>(`/api/admin/evaluations/${editingId}/status`, body).subscribe({
        next: () => {
          this.evalSaving.set(false);
          this.evalMessage.set('Évaluation mise à jour avec succès.');
          this.cancelEditEvaluation();
          this.loadEvaluations();
        },
        error: (err) => {
          this.evalSaving.set(false);
          this.evalError.set(this.mapHttpError(err, 'Impossible de mettre à jour l\'évaluation.'));
        }
      });
    } else {
      // Create new evaluation
      const body = { organizationId: Number(formVal.organizationId) };
      this.http.post<Evaluation>('/api/admin/evaluations', body).subscribe({
        next: (createdEval) => {
          this.evalSaving.set(false);
          this.evalMessage.set(`Évaluation #${createdEval.id} créée avec succès.`);
          this.cancelEditEvaluation();
          this.loadEvaluations();
        },
        error: (err) => {
          this.evalSaving.set(false);
          this.evalError.set(this.mapHttpError(err, 'Impossible de créer l\'évaluation.'));
        }
      });
    }
  }

  // Evaluation actions/coefficients detail modal state
  detailedEvaluation = signal<Evaluation | null>(null);
  evalDetailLoading = signal(false);
  evalModalSearchQuery = signal<string>('');
  evalModalSelectedTheme = signal<string>('ALL');
  evalModalStatusFilter = signal<string>('ALL');
  activeEvalScore = signal<EvaluationScoreResult | null>(null);
  loadingScore = signal(false);
  validatingEval = signal(false);
  coeffUpdateSuccessMessage = signal<string | null>(null);
  coeffUpdatingItemId = signal<number | null>(null);

  evalModalFilteredItems = computed(() => {
    const evalData = this.detailedEvaluation();
    if (!evalData || !evalData.items) return [];

    const query = this.evalModalSearchQuery().trim().toLowerCase();
    const theme = this.evalModalSelectedTheme();
    const status = this.evalModalStatusFilter();

    return evalData.items.filter(item => {
      if (theme !== 'ALL' && item.theme?.toLowerCase() !== theme.toLowerCase()) {
        return false;
      }
      if (status !== 'ALL' && item.implementationStatus !== status) {
        return false;
      }
      if (query) {
        const matchesCode = item.actionCode?.toLowerCase().includes(query);
        const matchesIso = item.isoControl?.toLowerCase().includes(query);
        const matchesDesc = item.description?.toLowerCase().includes(query);
        const matchesSubdomain = item.projectSubdomain?.toLowerCase().includes(query);
        if (!matchesCode && !matchesIso && !matchesDesc && !matchesSubdomain) {
          return false;
        }
      }
      return true;
    });
  });

  evalModalThemeCounts = computed(() => {
    const evalData = this.detailedEvaluation();
    if (!evalData || !evalData.items) {
      return { total: 0, org: 0, people: 0, physical: 0, tech: 0 };
    }
    const all = evalData.items;
    let org = 0, people = 0, physical = 0, tech = 0;
    for (const item of all) {
      const t = (item.theme || '').toLowerCase();
      if (t === 'organizational') org++;
      else if (t === 'people') people++;
      else if (t === 'physical') physical++;
      else if (t === 'technological') tech++;
    }
    return { total: all.length, org, people, physical, tech };
  });

  evalModalComplianceStats = computed(() => {
    const evalData = this.detailedEvaluation();
    if (!evalData || !evalData.items) return null;
    const items = evalData.items;
    const total = items.length;
    const implemented = items.filter(i => i.implementationStatus === 'IMPLEMENTED').length;
    const inProgress = items.filter(i => i.implementationStatus === 'IN_PROGRESS').length;
    const planned = items.filter(i => i.implementationStatus === 'PLANNED').length;
    const notPlanned = items.filter(i => i.implementationStatus === 'NOT_PLANNED').length;
    const notApplicable = items.filter(i => i.implementationStatus === 'NOT_APPLICABLE').length;
    const answered = total - notPlanned;
    const progressPercent = total > 0 ? Math.round((answered / total) * 100) : 0;
    return {
      total,
      implemented,
      inProgress,
      planned,
      notPlanned,
      notApplicable,
      answered,
      progressPercent
    };
  });

  viewEvaluationDetail(evalItem: Evaluation) {
    this.evalDetailLoading.set(true);
    this.evalModalSearchQuery.set('');
    this.evalModalSelectedTheme.set('ALL');
    this.evalModalStatusFilter.set('ALL');
    this.coeffUpdateSuccessMessage.set(null);
    this.loadEvaluationScore(evalItem.id);

    this.http.get<Evaluation>(`/api/admin/evaluations/${evalItem.id}`).subscribe({
      next: (fullEval) => {
        this.detailedEvaluation.set(fullEval);
        this.evalDetailLoading.set(false);
      },
      error: (err) => {
        this.evalDetailLoading.set(false);
        this.evalError.set(this.mapHttpError(err, 'Impossible de charger le détail de l\'évaluation.'));
      }
    });
  }

  loadEvaluationScore(evalId: number) {
    this.loadingScore.set(true);
    this.http.get<EvaluationScoreResult>(`/api/admin/evaluations/${evalId}/score`).subscribe({
      next: (score) => {
        this.activeEvalScore.set(score);
        this.loadingScore.set(false);
      },
      error: () => {
        this.activeEvalScore.set(null);
        this.loadingScore.set(false);
      }
    });
  }

  closeEvaluationDetail() {
    this.detailedEvaluation.set(null);
    this.activeEvalScore.set(null);
    this.evalModalSearchQuery.set('');
    this.evalModalSelectedTheme.set('ALL');
    this.evalModalStatusFilter.set('ALL');
    this.coeffUpdateSuccessMessage.set(null);
  }

  setEvalModalTheme(theme: string) {
    this.evalModalSelectedTheme.set(theme);
  }

  setEvalModalStatus(status: string) {
    this.evalModalStatusFilter.set(status);
  }

  updateItemCoefficientInEval(evalId: number, itemId: number, newCoeff: number) {
    if (newCoeff < 1 || newCoeff > 5) return;
    this.coeffUpdatingItemId.set(itemId);
    this.http.put<Evaluation>(`/api/admin/evaluations/${evalId}/items/${itemId}/coefficient`, { coefficient: newCoeff }).subscribe({
      next: (updatedEval) => {
        this.coeffUpdatingItemId.set(null);
        this.detailedEvaluation.set(updatedEval);
        const item = updatedEval.items?.find(i => i.id === itemId);
        const code = item ? item.actionCode : '';
        this.coeffUpdateSuccessMessage.set(`Coefficient pour la mesure ${code} enregistré (${newCoeff}).`);
        setTimeout(() => {
          if (this.coeffUpdateSuccessMessage()?.includes(code)) {
            this.coeffUpdateSuccessMessage.set(null);
          }
        }, 3000);
        this.loadEvaluationScore(evalId);
        this.loadEvaluations();
      },
      error: (err) => {
        this.coeffUpdatingItemId.set(null);
        this.evalError.set(this.mapHttpError(err, 'Impossible de modifier le coefficient.'));
      }
    });
  }

  resetItemCoefficientInEval(evalId: number, itemId: number) {
    this.coeffUpdatingItemId.set(itemId);
    this.http.put<Evaluation>(`/api/admin/evaluations/${evalId}/items/${itemId}/coefficient/reset`, {}).subscribe({
      next: (updatedEval) => {
        this.coeffUpdatingItemId.set(null);
        this.detailedEvaluation.set(updatedEval);
        const item = updatedEval.items?.find(i => i.id === itemId);
        const code = item ? item.actionCode : '';
        const def = item ? item.coefficient : '';
        this.coeffUpdateSuccessMessage.set(`Coefficient de la mesure ${code} rétabli à la valeur du secteur (${def}).`);
        setTimeout(() => {
          if (this.coeffUpdateSuccessMessage()?.includes(code)) {
            this.coeffUpdateSuccessMessage.set(null);
          }
        }, 3000);
        this.loadEvaluationScore(evalId);
        this.loadEvaluations();
      },
      error: (err) => {
        this.coeffUpdatingItemId.set(null);
        this.evalError.set(this.mapHttpError(err, 'Impossible de réinitialiser le coefficient.'));
      }
    });
  }

  validateEvaluation(evalItem: Evaluation) {
    if (!confirm(`Confirmer la validation et la clôture définitive de l'évaluation #${evalItem.id} de « ${evalItem.organizationName} » ?`)) return;
    this.validatingEval.set(true);
    this.http.put<Evaluation>(`/api/admin/evaluations/${evalItem.id}/validate`, {}).subscribe({
      next: (updatedEval) => {
        this.validatingEval.set(false);
        this.evalMessage.set(`L'évaluation #${evalItem.id} (« ${evalItem.organizationName} ») a été validée et clôturée avec succès.`);
        if (this.detailedEvaluation()?.id === evalItem.id) {
          this.detailedEvaluation.set(updatedEval);
        }
        this.loadEvaluations();
      },
      error: (err) => {
        this.validatingEval.set(false);
        this.evalError.set(this.mapHttpError(err, 'Impossible de valider cette évaluation.'));
      }
    });
  }

  requestRevision(evalItem: Evaluation) {
    if (!confirm(`Renvoyer l'évaluation #${evalItem.id} à l'organisation « ${evalItem.organizationName} » pour révision / compléments ?`)) return;
    this.validatingEval.set(true);
    this.http.put<Evaluation>(`/api/admin/evaluations/${evalItem.id}/request-revision`, {}).subscribe({
      next: (updatedEval) => {
        this.validatingEval.set(false);
        this.evalMessage.set(`L'évaluation #${evalItem.id} a été renvoyée à l'organisation pour révision.`);
        if (this.detailedEvaluation()?.id === evalItem.id) {
          this.detailedEvaluation.set(updatedEval);
        }
        this.loadEvaluations();
      },
      error: (err) => {
        this.validatingEval.set(false);
        this.evalError.set(this.mapHttpError(err, 'Impossible de renvoyer l\'évaluation.'));
      }
    });
  }

  // Prompt confirmation to assign/send evaluation
  promptAssignEvaluation(evalItem: Evaluation) {
    this.assignConfirmEval.set(evalItem);
  }

  cancelAssignEvaluation() {
    this.assignConfirmEval.set(null);
  }

  confirmAssignEvaluation() {
    const evalItem = this.assignConfirmEval();
    if (!evalItem) return;

    this.assigningEval.set(true);
    this.evalMessage.set(null);
    this.evalError.set(null);

    this.http.put<Evaluation>(`/api/admin/evaluations/${evalItem.id}/status`, { status: 'ASSIGNED' }).subscribe({
      next: (updatedEval) => {
        this.assigningEval.set(false);
        this.evalMessage.set(`L'évaluation #${evalItem.id} a été transmise avec succès à l'organisation « ${evalItem.organizationName} ».`);
        this.assignConfirmEval.set(null);
        if (this.detailedEvaluation()?.id === evalItem.id) {
          this.detailedEvaluation.set(updatedEval);
        }
        if (this.psgActiveEvaluation()?.id === evalItem.id) {
          this.psgActiveEvaluation.set(updatedEval);
        }
        this.loadEvaluations();
      },
      error: (err) => {
        this.assigningEval.set(false);
        this.evalError.set(this.mapHttpError(err, 'Impossible de transmettre l\'évaluation.'));
        this.assignConfirmEval.set(null);
      }
    });
  }

  deleteEvaluation(evalItem: Evaluation) {
    if (!confirm(`Supprimer définitivement l'évaluation #${evalItem.id} de « ${evalItem.organizationName} » ?`)) return;

    this.evalMessage.set(null);
    this.evalError.set(null);
    this.http.delete(`/api/admin/evaluations/${evalItem.id}`).subscribe({
      next: () => {
        this.evalMessage.set('Évaluation supprimée.');
        if (this.editingEvalId() === evalItem.id) {
          this.cancelEditEvaluation();
        }
        this.loadEvaluations();
      },
      error: (err) => {
        this.evalError.set(this.mapHttpError(err, 'Impossible de supprimer cette évaluation.'));
      }
    });
  }

  getEvaluationStatusClass(status: string): string {
    switch (status) {
      case 'DRAFT': return 'status-draft';
      case 'ASSIGNED': return 'status-assigned';
      case 'IN_PROGRESS': return 'status-progress';
      case 'SUBMITTED': return 'status-submitted';
      case 'COMPLETED': return 'status-completed';
      default: return 'status-draft';
    }
  }

  getEvaluationStatusLabel(status: string): string {
    switch (status) {
      case 'DRAFT': return 'Brouillon';
      case 'ASSIGNED': return 'Assignée';
      case 'IN_PROGRESS': return 'En cours';
      case 'SUBMITTED': return 'Soumise';
      case 'COMPLETED': return 'Terminée';
      default: return status;
    }
  }

  getSectorName(sectorId: number): string {
    const s = this.sectors().find(sec => sec.id === sectorId);
    return s ? s.name : 'Non défini';
  }

  // --- PSG REFERENTIAL & EVALUATION METHODS ---

  loadPsgReferenceActions(sectorId?: number | null) {
    this.loadingPsgActions.set(true);
    const url = sectorId ? `/api/admin/sectors/${sectorId}/psg-actions` : '/api/admin/psg-actions';
    this.http.get<PsgActionDTO[]>(url).subscribe({
      next: (res) => {
        this.psgReferenceActions.set(Array.isArray(res) ? res : []);
        this.loadingPsgActions.set(false);
      },
      error: () => {
        this.psgReferenceActions.set([]);
        this.loadingPsgActions.set(false);
      }
    });
  }

  onPsgSectorChange(sectorIdVal: string) {
    const sectorId = sectorIdVal ? Number(sectorIdVal) : null;
    this.psgSelectedSectorId.set(sectorId);

    // If an evaluation was chosen, check if it belongs to the new sector
    const currentEval = this.psgActiveEvaluation();
    if (currentEval && sectorId) {
      const org = this.organizations().find(o => o.id === currentEval.organizationId);
      const evalSectorId = currentEval.sectorId || (org ? org.sectorId : null);
      if (evalSectorId !== sectorId) {
        // Reset evaluation selection
        this.psgSelectedEvalId.set(null);
        this.psgActiveEvaluation.set(null);
      }
    }

    this.loadPsgReferenceActions(sectorId);
  }

  onPsgEvaluationChange(evalIdVal: string) {
    if (!evalIdVal) {
      // Switched back to Reference Matrix
      this.psgSelectedEvalId.set(null);
      this.psgActiveEvaluation.set(null);
      this.loadPsgReferenceActions(this.psgSelectedSectorId());
      return;
    }

    const evalId = Number(evalIdVal);
    this.psgSelectedEvalId.set(evalId);
    this.loadingPsgEvaluation.set(true);

    this.http.get<Evaluation>(`/api/admin/evaluations/${evalId}`).subscribe({
      next: (fullEval) => {
        this.psgActiveEvaluation.set(fullEval);
        this.loadingPsgEvaluation.set(false);
        // If no sector was explicitly selected, sync sector with evaluation's sector
        if (!this.psgSelectedSectorId()) {
          const org = this.organizations().find(o => o.id === fullEval.organizationId);
          if (org) {
            this.psgSelectedSectorId.set(org.sectorId);
            this.loadPsgReferenceActions(org.sectorId);
          }
        }
      },
      error: () => {
        this.psgActiveEvaluation.set(null);
        this.loadingPsgEvaluation.set(false);
      }
    });
  }

  updatePsgItemCoefficient(evalId: number, itemId: number, newCoeff: number) {
    if (newCoeff < 1 || newCoeff > 5) return;
    this.http.put<Evaluation>(`/api/admin/evaluations/${evalId}/items/${itemId}/coefficient`, { coefficient: newCoeff }).subscribe({
      next: (updatedEval) => {
        this.psgActiveEvaluation.set(updatedEval);
        this.loadEvaluations();
      },
      error: (err) => {
        alert(this.mapHttpError(err, 'Impossible de modifier le coefficient.'));
      }
    });
  }

  setPsgTheme(theme: string) {
    this.psgSelectedTheme.set(theme);
  }

  setPsgCoeff(coeff: number | null) {
    this.psgSelectedCoeff.set(coeff);
  }

  setPsgStatus(status: string) {
    this.psgSelectedStatus.set(status);
  }

  resetPsgFilters() {
    this.psgSearchQuery.set('');
    this.psgSelectedTheme.set('ALL');
    this.psgSelectedCoeff.set(null);
    this.psgSelectedStatus.set('ALL');
  }

  getCoeffBadgeClass(coeff: number): string {
    switch (coeff) {
      case 5: return 'coeff-5';
      case 4: return 'coeff-4';
      case 3: return 'coeff-3';
      case 2: return 'coeff-2';
      case 1: return 'coeff-1';
      default: return 'coeff-3';
    }
  }

  getCoeffLabel(coeff: number): string {
    switch (coeff) {
      case 5: return '5 — Critique';
      case 4: return '4 — Élevé';
      case 3: return '3 — Moyen';
      case 2: return '2 — Faible';
      case 1: return '1 — Très faible';
      default: return `${coeff}`;
    }
  }

  getImplStatusBadgeClass(status?: string): string {
    switch (status) {
      case 'IMPLEMENTED': return 'impl-implemented';
      case 'IN_PROGRESS': return 'impl-progress';
      case 'PLANNED': return 'impl-planned';
      case 'NOT_PLANNED': return 'impl-not-planned';
      case 'NOT_APPLICABLE': return 'impl-na';
      default: return 'impl-not-planned';
    }
  }

  getImplStatusLabel(status?: string): string {
    switch (status) {
      case 'IMPLEMENTED': return 'Mis en œuvre (100%)';
      case 'IN_PROGRESS': return 'En cours (50%)';
      case 'PLANNED': return 'Planifié (25%)';
      case 'NOT_PLANNED': return 'Non planifié (0%)';
      case 'NOT_APPLICABLE': return 'Non applicable';
      default: return status || 'Non défini';
    }
  }

  getThemeLabel(theme?: string): string {
    switch (theme?.toLowerCase()) {
      case 'organizational': return 'Organisationnel';
      case 'people': return 'Personnel';
      case 'physical': return 'Physique';
      case 'technological': return 'Technologique';
      default: return theme || '';
    }
  }

  getThemeBadgeClass(theme?: string): string {
    switch (theme?.toLowerCase()) {
      case 'organizational': return 'theme-org';
      case 'people': return 'theme-people';
      case 'physical': return 'theme-physical';
      case 'technological': return 'theme-tech';
      default: return 'badge-neutral';
    }
  }

  private mapHttpError(err: HttpErrorResponse, fallback: string): string {
    if (err.status === 403) {
      return 'Accès refusé. Cette action est réservée à l\'administrateur.';
    }
    if (err.status === 404) {
      return this.extractServerMessage(err) || 'Ressource introuvable.';
    }
    if (err.status === 409) {
      return this.extractServerMessage(err) || 'Opération refusée : un conflit empêche cette action.';
    }
    if (err.status === 400) {
      return this.extractServerMessage(err) || 'Données invalides.';
    }
    if (err.status >= 500) {
      return 'Erreur serveur. Veuillez réessayer.';
    }
    return this.extractServerMessage(err) || fallback;
  }

  openAnalyticsForEvaluation(evalId: number) {
    this.closeEvaluationDetail();
    this.analyticsSelectedEvalId.set(evalId);
    this.activeTab.set('analytics');
    this.loadAnalyticsScore(evalId);
  }

  loadAnalyticsScore(evalId: number) {
    this.analyticsLoading.set(true);
    this.analyticsError.set(null);
    this.http.get<EvaluationScoreResult>(`/api/admin/evaluations/${evalId}/score`).subscribe({
      next: (score) => {
        this.analyticsScoreData.set(score);
        this.analyticsLoading.set(false);
      },
      error: (err) => {
        this.analyticsScoreData.set(null);
        this.analyticsLoading.set(false);
        this.analyticsError.set(this.mapHttpError(err, 'Impossible de calculer le score pour cette évaluation.'));
      }
    });
  }

  onAnalyticsEvalChange(evalIdStr: string) {
    const id = parseInt(evalIdStr, 10);
    if (!isNaN(id)) {
      this.analyticsSelectedEvalId.set(id);
      this.loadAnalyticsScore(id);
    }
  }

  toggleAnalyticsCompare() {
    const next = !this.analyticsCompareMode();
    this.analyticsCompareMode.set(next);
    if (next && !this.analyticsCompareEvalId()) {
      const currentId = this.analyticsSelectedEvalId();
      const other = this.evaluations().find(e => e.id !== currentId);
      if (other) {
        this.analyticsCompareEvalId.set(other.id);
        this.loadAnalyticsCompareScore(other.id);
      }
    }
  }

  onAnalyticsCompareEvalChange(evalIdStr: string) {
    const id = parseInt(evalIdStr, 10);
    if (!isNaN(id)) {
      this.analyticsCompareEvalId.set(id);
      this.loadAnalyticsCompareScore(id);
    }
  }

  loadAnalyticsCompareScore(evalId: number) {
    this.analyticsCompareLoading.set(true);
    this.http.get<EvaluationScoreResult>(`/api/admin/evaluations/${evalId}/score`).subscribe({
      next: (score) => {
        this.analyticsCompareScoreData.set(score);
        this.analyticsCompareLoading.set(false);
      },
      error: () => {
        this.analyticsCompareScoreData.set(null);
        this.analyticsCompareLoading.set(false);
      }
    });
  }

  printAnalyticsReport() {
    window.print();
  }

  getScoreBandClass(level?: string): string {
    switch (level?.toLowerCase()) {
      case 'très bon':
      case 'tres bon':
        return 'score-band-excellent';
      case 'bon':
        return 'score-band-good';
      case 'à améliorer':
      case 'a ameliorer':
        return 'score-band-moderate';
      case 'faible':
        return 'score-band-weak';
      case 'critique':
        return 'score-band-critical';
      default:
        return 'score-band-neutral';
    }
  }

  getPriorityBadgeClass(level?: string | null): string {
    switch (level?.toLowerCase()) {
      case 'critique':
        return 'priority-badge-critical';
      case 'élevée':
      case 'elevee':
        return 'priority-badge-high';
      case 'moyenne':
        return 'priority-badge-medium';
      case 'faible':
        return 'priority-badge-low';
      case 'aucun':
        return 'priority-badge-none';
      default:
        return 'priority-badge-neutral';
    }
  }

  getScoreGaugeOffset(score: number | null): number {
    if (score === null || score === undefined) return 235.62;
    const clamped = Math.max(0, Math.min(100, score));
    return 235.62 * (1 - clamped / 100);
  }

  getActionIsoControl(action: any): string {
    if (action.isoControl && action.isoControl.trim()) {
      return action.isoControl;
    }
    const currentEval = this.evaluations().find(e => e.id === this.analyticsSelectedEvalId());
    const matched = currentEval?.items?.find(i => i.actionCode === action.actionCode);
    return matched?.isoControl || '-';
  }

  private extractServerMessage(err: HttpErrorResponse): string | null {
    const body = err.error;
    if (!body) return null;
    if (typeof body === 'string' && body.trim()) return body;
    if (typeof body.message === 'string' && body.message.trim()) {
      return body.message.replace(/^\d+\s+\w+\s+"?|"$/g, '').replace(/^"|"$/g, '');
    }
    return null;
  }
}