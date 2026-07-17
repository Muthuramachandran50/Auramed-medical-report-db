// AuraMed Diagnostics DBMS Frontend Engine

// --- STATE MANAGEMENT ---
const state = {
    patients: [],
    doctors: [],
    tests: [],
    reports: [],
    details: [],
    testCounts: []
};

// API Base URL
const API_URL = ''; // Relative to served host

// --- DOM READY INITIALIZATION ---
document.addEventListener('DOMContentLoaded', () => {
    // Tab Switching
    document.querySelectorAll('.menu-item').forEach(item => {
        item.addEventListener('click', (e) => {
            e.preventDefault();
            const tabId = item.getAttribute('data-tab');
            switchTab(tabId);
        });
    });

    // Theme Toggle
    const themeToggle = document.getElementById('themeToggle');
    themeToggle.addEventListener('click', toggleTheme);

    // Initial Load
    switchTab('dashboard');

    // Register Form Handlers
    initFormHandlers();
    
    // Register Modals Open Button Handlers
    initModalOpenHandlers();
    
    // Local Table Search Filters
    initSearchFilters();
});

// --- THEME SWITCHER ---
function toggleTheme() {
    const body = document.body;
    const btn = document.getElementById('themeToggle');
    if (body.classList.contains('dark-theme')) {
        body.classList.replace('dark-theme', 'light-theme');
        btn.innerHTML = '<i class="fa-solid fa-moon"></i> <span>Dark Mode</span>';
    } else {
        body.classList.replace('light-theme', 'dark-theme');
        btn.innerHTML = '<i class="fa-solid fa-sun"></i> <span>Light Mode</span>';
    }
}

// --- ROUTING / TAB MANAGER ---
function switchTab(tabId) {
    // Update menu items
    document.querySelectorAll('.menu-item').forEach(item => {
        if (item.getAttribute('data-tab') === tabId) {
            item.classList.add('active');
        } else {
            item.classList.remove('active');
        }
    });

    // Update sections
    document.querySelectorAll('.tab-section').forEach(section => {
        if (section.id === `tab-${tabId}`) {
            section.classList.add('active');
        } else {
            section.classList.remove('active');
        }
    });

    // Update Page Header
    const titleMap = {
        'dashboard': { title: 'Dashboard', desc: 'Welcome to AuraMed Diagnostic database management portal.' },
        'patients': { title: 'Patients Management', desc: 'Add, view, update, and remove patients in the database.' },
        'doctors': { title: 'Doctors Management', desc: 'Manage clinic doctors, specialties, and contact records.' },
        'tests': { title: 'Diagnostic Tests', desc: 'Maintain the list of medical laboratory tests available.' },
        'reports': { title: 'Medical Reports', desc: 'Create and view basic patient report cases and assignment dates.' },
        'details': { title: 'Report Details & Results', desc: 'Update and view specific laboratory findings and diagnoses.' },
        'patient-search': { title: 'Patient Chart Search', desc: 'Locate complete diagnostics records utilizing SQL JOINs.' },
        'test-counts': { title: 'Test Count Statistics', desc: 'Analyze database counts showing tests taken per patient.' }
    };

    const header = titleMap[tabId];
    if (header) {
        document.getElementById('pageTitle').innerText = header.title;
        document.getElementById('pageDescription').innerText = header.desc;
    }

    // Load tab-specific data
    loadTabData(tabId);
}

// --- DATA INJECTOR FOR ACTIVE TAB ---
function loadTabData(tabId) {
    if (tabId === 'dashboard') {
        refreshDashboardStats();
    } else if (tabId === 'patients') {
        fetchPatients();
    } else if (tabId === 'doctors') {
        fetchDoctors();
    } else if (tabId === 'tests') {
        fetchTests();
    } else if (tabId === 'reports') {
        fetchReports();
    } else if (tabId === 'details') {
        fetchDetails();
    } else if (tabId === 'test-counts') {
        fetchTestCounts();
    }
}

// --- FETCH ENGINE (GET OPERATIONS) ---

async function refreshDashboardStats() {
    try {
        // Fetch everything to calculate stats
        const [p, d, t, r] = await Promise.all([
            fetch(`${API_URL}/api/patients`).then(res => res.json()),
            fetch(`${API_URL}/api/doctors`).then(res => res.json()),
            fetch(`${API_URL}/api/tests`).then(res => res.json()),
            fetch(`${API_URL}/api/reports`).then(res => res.json())
        ]);
        
        state.patients = p;
        state.doctors = d;
        state.tests = t;
        state.reports = r;

        document.getElementById('statPatientsCount').innerText = p.length;
        document.getElementById('statDoctorsCount').innerText = d.length;
        document.getElementById('statTestsCount').innerText = t.length;
        document.getElementById('statReportsCount').innerText = r.length;

        // Fetch Test counts join query for Dashboard Bar Analytics
        const counts = await fetch(`${API_URL}/api/query/test-counts`).then(res => res.json());
        const analyticsList = document.getElementById('dashboardAnalyticsList');
        analyticsList.innerHTML = '';
        
        if (counts.length === 0) {
            analyticsList.innerHTML = '<p class="text-muted text-center py-4">No report details logged yet.</p>';
            return;
        }

        const maxTests = Math.max(...counts.map(c => c.number_of_tests), 1);
        counts.slice(0, 5).forEach(c => {
            const pct = (c.number_of_tests / maxTests) * 100;
            analyticsList.innerHTML += `
                <div class="bar-row">
                    <div class="bar-label" title="${c.patient_name}">${c.patient_name}</div>
                    <div class="bar-track">
                        <div class="bar-fill" style="width: ${pct}%"></div>
                    </div>
                    <div class="bar-value">${c.number_of_tests}</div>
                </div>
            `;
        });
    } catch (e) {
        showToast('Failed to refresh dashboard stats.', 'error');
    }
}

async function fetchPatients() {
    try {
        const res = await fetch(`${API_URL}/api/patients`);
        const data = await res.json();
        state.patients = data;
        renderPatientsTable();
    } catch (e) {
        showToast('Error fetching patient records.', 'error');
    }
}

async function fetchDoctors() {
    try {
        const res = await fetch(`${API_URL}/api/doctors`);
        const data = await res.json();
        state.doctors = data;
        renderDoctorsTable();
    } catch (e) {
        showToast('Error fetching doctor records.', 'error');
    }
}

async function fetchTests() {
    try {
        const res = await fetch(`${API_URL}/api/tests`);
        const data = await res.json();
        state.tests = data;
        renderTestsTable();
    } catch (e) {
        showToast('Error fetching test records.', 'error');
    }
}

async function fetchReports() {
    try {
        const [reportsRes, patientsRes, doctorsRes] = await Promise.all([
            fetch(`${API_URL}/api/reports`),
            fetch(`${API_URL}/api/patients`),
            fetch(`${API_URL}/api/doctors`)
        ]);
        state.reports = await reportsRes.json();
        state.patients = await patientsRes.json();
        state.doctors = await doctorsRes.json();
        renderReportsTable();
    } catch (e) {
        showToast('Error fetching medical report entries.', 'error');
    }
}

async function fetchDetails() {
    try {
        const [detailsRes, testsRes] = await Promise.all([
            fetch(`${API_URL}/api/details`),
            fetch(`${API_URL}/api/tests`)
        ]);
        state.details = await detailsRes.json();
        state.tests = await testsRes.json();
        renderDetailsTable();
    } catch (e) {
        showToast('Error fetching report details.', 'error');
    }
}

async function fetchTestCounts() {
    try {
        const res = await fetch(`${API_URL}/api/query/test-counts`);
        const data = await res.json();
        state.testCounts = data;
        renderTestCountsTable();
    } catch (e) {
        showToast('Error loading test statistics.', 'error');
    }
}

// --- TABLE RENDERING ENGINE ---

function renderPatientsTable(list = state.patients) {
    const tbody = document.querySelector('#patientsTable tbody');
    tbody.innerHTML = '';
    
    if (list.length === 0) {
        tbody.innerHTML = '<tr><td colspan="7" class="text-center text-muted">No patients found.</td></tr>';
        return;
    }
    
    list.forEach(p => {
        tbody.innerHTML += `
            <tr>
                <td><strong>${p.patient_id}</strong></td>
                <td>${p.name}</td>
                <td><span class="badge badge-info">${p.gender}</span></td>
                <td>${p.dob}</td>
                <td>${p.phone}</td>
                <td>${p.email}</td>
                <td class="actions-cell">
                    <button class="btn btn-secondary btn-sm" onclick="openEditPatient(${p.patient_id})">
                        <i class="fa-solid fa-pen"></i> Edit
                    </button>
                    <button class="btn btn-danger btn-sm" onclick="triggerDeletePatient(${p.patient_id})">
                        <i class="fa-solid fa-trash"></i> Delete
                    </button>
                </td>
            </tr>
        `;
    });
}

function renderDoctorsTable(list = state.doctors) {
    const tbody = document.querySelector('#doctorsTable tbody');
    tbody.innerHTML = '';
    
    if (list.length === 0) {
        tbody.innerHTML = '<tr><td colspan="5" class="text-center text-muted">No doctor records available.</td></tr>';
        return;
    }

    list.forEach(d => {
        tbody.innerHTML += `
            <tr>
                <td><strong>${d.doctor_id}</strong></td>
                <td>${d.name}</td>
                <td>${d.specialization}</td>
                <td>${d.phone}</td>
                <td class="actions-cell">
                    <button class="btn btn-secondary btn-sm" onclick="openEditDoctor(${d.doctor_id})">
                        <i class="fa-solid fa-pen"></i> Edit
                    </button>
                    <button class="btn btn-danger btn-sm" onclick="triggerDeleteDoctor(${d.doctor_id})">
                        <i class="fa-solid fa-trash"></i> Delete
                    </button>
                </td>
            </tr>
        `;
    });
}

function renderTestsTable(list = state.tests) {
    const tbody = document.querySelector('#testsTable tbody');
    tbody.innerHTML = '';
    
    if (list.length === 0) {
        tbody.innerHTML = '<tr><td colspan="3" class="text-center text-muted">No tests defined.</td></tr>';
        return;
    }

    list.forEach(t => {
        tbody.innerHTML += `
            <tr>
                <td><strong>${t.test_id}</strong></td>
                <td>${t.test_name}</td>
                <td class="actions-cell justify-content-center">
                    <button class="btn btn-secondary btn-sm" onclick="openEditTest(${t.test_id})">
                        <i class="fa-solid fa-pen"></i> Edit
                    </button>
                    <button class="btn btn-danger btn-sm" onclick="triggerDeleteTest(${t.test_id})">
                        <i class="fa-solid fa-trash"></i> Delete
                    </button>
                </td>
            </tr>
        `;
    });
}

function renderReportsTable(list = state.reports) {
    const tbody = document.querySelector('#reportsTable tbody');
    tbody.innerHTML = '';
    
    if (list.length === 0) {
        tbody.innerHTML = '<tr><td colspan="5" class="text-center text-muted">No medical reports compiled.</td></tr>';
        return;
    }

    list.forEach(r => {
        // Resolve patient and doctor names from state cache
        const patientObj = state.patients.find(p => p.patient_id === r.patient_id);
        const doctorObj = state.doctors.find(d => d.doctor_id === r.doctor_id);
        const pName = patientObj ? `${patientObj.name} (ID: ${r.patient_id})` : `Patient ID: ${r.patient_id}`;
        const dName = doctorObj ? `${doctorObj.name}` : `Doctor ID: ${r.doctor_id}`;

        tbody.innerHTML += `
            <tr>
                <td><strong>${r.report_id}</strong></td>
                <td>${pName}</td>
                <td>${dName}</td>
                <td>${r.report_date}</td>
                <td class="actions-cell">
                    <button class="btn btn-secondary btn-sm" onclick="openEditReport(${r.report_id})">
                        <i class="fa-solid fa-pen"></i> Edit
                    </button>
                    <button class="btn btn-danger btn-sm" onclick="triggerDeleteReport(${r.report_id})">
                        <i class="fa-solid fa-trash"></i> Delete
                    </button>
                </td>
            </tr>
        `;
    });
}

function renderDetailsTable(list = state.details) {
    const tbody = document.querySelector('#detailsTable tbody');
    tbody.innerHTML = '';
    
    if (list.length === 0) {
        tbody.innerHTML = '<tr><td colspan="6" class="text-center text-muted">No findings detailed yet.</td></tr>';
        return;
    }

    list.forEach(rd => {
        const testObj = state.tests.find(t => t.test_id === rd.test_id);
        const testName = testObj ? testObj.test_name : `Test ID: ${rd.test_id}`;

        tbody.innerHTML += `
            <tr>
                <td><strong>${rd.detail_id}</strong></td>
                <td>Report ID: ${rd.report_id}</td>
                <td>${testName}</td>
                <td>${rd.result}</td>
                <td>${rd.diagnosis}</td>
                <td class="actions-cell">
                    <button class="btn btn-secondary btn-sm" onclick="openEditDetail(${rd.report_id}, ${rd.test_id})">
                        <i class="fa-solid fa-pen"></i> Edit
                    </button>
                    <button class="btn btn-danger btn-sm" onclick="triggerDeleteDetail(${rd.report_id}, ${rd.test_id})">
                        <i class="fa-solid fa-trash"></i> Delete
                    </button>
                </td>
            </tr>
        `;
    });
}

function renderTestCountsTable() {
    const tbody = document.querySelector('#testCountsTable tbody');
    tbody.innerHTML = '';
    
    if (state.testCounts.length === 0) {
        tbody.innerHTML = '<tr><td colspan="3" class="text-center text-muted">No results found.</td></tr>';
        return;
    }

    const maxTests = Math.max(...state.testCounts.map(c => c.number_of_tests), 1);
    state.testCounts.forEach(c => {
        const pct = (c.number_of_tests / maxTests) * 100;
        tbody.innerHTML += `
            <tr>
                <td><strong>${c.patient_name}</strong></td>
                <td>${c.number_of_tests} test(s)</td>
                <td>
                    <div class="bar-track" style="margin: 0;">
                        <div class="bar-fill" style="width: ${pct}%"></div>
                    </div>
                </td>
            </tr>
        `;
    });
}

// --- MODAL CONTROLLERS & SUBMISSIONS ---

function openModal(id) {
    document.getElementById(id).classList.add('active');
}

function closeModal(id) {
    document.getElementById(id).classList.remove('active');
}

// Init Form Modal Open Handles
function initModalOpenHandlers() {
    document.getElementById('openAddPatientBtn').addEventListener('click', () => {
        document.getElementById('patientForm').reset();
        document.getElementById('patientFormMode').value = 'add';
        document.getElementById('patientFormId').disabled = false;
        document.getElementById('patientModalTitle').innerText = 'Add New Patient';
        openModal('patientModal');
    });

    document.getElementById('openAddDoctorBtn').addEventListener('click', () => {
        document.getElementById('doctorForm').reset();
        document.getElementById('doctorFormMode').value = 'add';
        document.getElementById('doctorModalTitle').innerText = 'Add New Doctor';
        openModal('doctorModal');
    });

    document.getElementById('openAddTestBtn').addEventListener('click', () => {
        document.getElementById('testForm').reset();
        document.getElementById('testFormMode').value = 'add';
        document.getElementById('testModalTitle').innerText = 'Add New Diagnostic Test';
        openModal('testModal');
    });

    document.getElementById('openAddReportBtn').addEventListener('click', async () => {
        document.getElementById('reportForm').reset();
        document.getElementById('reportFormMode').value = 'add';
        document.getElementById('reportModalTitle').innerText = 'Create Medical Report';
        
        // Dynamically load Patients & Doctors into select inputs
        await Promise.all([fetchPatients(), fetchDoctors()]);
        
        const patSel = document.getElementById('reportFormPatient');
        patSel.innerHTML = '<option value="">-- Choose Patient --</option>';
        state.patients.forEach(p => {
            patSel.innerHTML += `<option value="${p.patient_id}">${p.name} (ID: ${p.patient_id})</option>`;
        });

        const docSel = document.getElementById('reportFormDoctor');
        docSel.innerHTML = '<option value="">-- Choose Doctor --</option>';
        state.doctors.forEach(d => {
            docSel.innerHTML += `<option value="${d.doctor_id}">${d.name} (${d.specialization})</option>`;
        });

        // Set default date to today
        document.getElementById('reportFormDate').value = new Date().toISOString().split('T')[0];

        openModal('reportModal');
    });

    document.getElementById('openAddDetailBtn').addEventListener('click', async () => {
        document.getElementById('detailForm').reset();
        document.getElementById('detailFormMode').value = 'add';
        document.getElementById('detailModalTitle').innerText = 'Add Report Details';
        document.getElementById('detailFormReportGroup').style.display = 'flex';
        document.getElementById('detailFormTestGroup').style.display = 'flex';

        // Load Reports & Tests into selection boxes
        await Promise.all([fetchReports(), fetchTests()]);

        const repSel = document.getElementById('detailFormReport');
        repSel.innerHTML = '<option value="">-- Select Medical Report ID --</option>';
        state.reports.forEach(r => {
            const pat = state.patients.find(p => p.patient_id === r.patient_id);
            const patName = pat ? pat.name : 'Unknown';
            repSel.innerHTML += `<option value="${r.report_id}">Report ID: ${r.report_id} - ${patName} (${r.report_date})</option>`;
        });

        const testSel = document.getElementById('detailFormTest');
        testSel.innerHTML = '<option value="">-- Choose Laboratory Test --</option>';
        state.tests.forEach(t => {
            testSel.innerHTML += `<option value="${t.test_id}">${t.test_name} (ID: ${t.test_id})</option>`;
        });

        openModal('detailModal');
    });
}

// Edit Modal Launchers (CLI equivalents)
function openEditPatient(id) {
    const p = state.patients.find(item => item.patient_id === id);
    if (!p) return;

    document.getElementById('patientFormMode').value = 'edit';
    document.getElementById('patientFormId').value = p.patient_id;
    document.getElementById('patientFormId').disabled = true; // Cannot edit Primary Key
    document.getElementById('patientFormName').value = p.name;
    document.getElementById('patientFormGender').value = p.gender.charAt(0) + p.gender.slice(1).toLowerCase(); // Normalize enum
    document.getElementById('patientFormDob').value = p.dob;
    document.getElementById('patientFormPhone').value = p.phone;
    document.getElementById('patientFormEmail').value = p.email;

    document.getElementById('patientModalTitle').innerText = 'Edit Patient details';
    openModal('patientModal');
}

function openEditDoctor(id) {
    const d = state.doctors.find(item => item.doctor_id === id);
    if (!d) return;

    document.getElementById('doctorFormMode').value = 'edit';
    document.getElementById('doctorFormId').value = d.doctor_id;
    document.getElementById('doctorFormName').value = d.name;
    document.getElementById('doctorFormSpecialization').value = d.specialization;
    document.getElementById('doctorFormPhone').value = d.phone;

    document.getElementById('doctorModalTitle').innerText = 'Edit Doctor Details';
    openModal('doctorModal');
}

function openEditTest(id) {
    const t = state.tests.find(item => item.test_id === id);
    if (!t) return;

    document.getElementById('testFormMode').value = 'edit';
    document.getElementById('testFormId').value = t.test_id;
    document.getElementById('testFormName').value = t.test_name;

    document.getElementById('testModalTitle').innerText = 'Edit Test Name';
    openModal('testModal');
}

function openEditReport(id) {
    const r = state.reports.find(item => item.report_id === id);
    if (!r) return;

    document.getElementById('reportFormMode').value = 'edit';
    document.getElementById('reportFormId').value = r.report_id;

    // Load Dropdowns
    const patSel = document.getElementById('reportFormPatient');
    patSel.innerHTML = '';
    state.patients.forEach(p => {
        patSel.innerHTML += `<option value="${p.patient_id}">${p.name} (ID: ${p.patient_id})</option>`;
    });
    patSel.value = r.patient_id;

    const docSel = document.getElementById('reportFormDoctor');
    docSel.innerHTML = '';
    state.doctors.forEach(d => {
        docSel.innerHTML += `<option value="${d.doctor_id}">${d.name} (${d.specialization})</option>`;
    });
    docSel.value = r.doctor_id;

    document.getElementById('reportFormDate').value = r.report_date;
    document.getElementById('reportModalTitle').innerText = `Edit Medical Report #${r.report_id}`;
    openModal('reportModal');
}

function openEditDetail(reportId, testId) {
    const rd = state.details.find(item => item.report_id === reportId && item.test_id === testId);
    if (!rd) return;

    document.getElementById('detailFormMode').value = 'edit';
    
    // Set values in select but hide them or disable them (since Composite key report_id + test_id cannot be edited, only result/diagnosis can)
    document.getElementById('detailFormReportGroup').style.display = 'none';
    document.getElementById('detailFormTestGroup').style.display = 'none';
    
    // Set values anyway for form parsing
    const repSel = document.getElementById('detailFormReport');
    repSel.innerHTML = `<option value="${reportId}">${reportId}</option>`;
    repSel.value = reportId;

    const testSel = document.getElementById('detailFormTest');
    testSel.innerHTML = `<option value="${testId}">${testId}</option>`;
    testSel.value = testId;

    document.getElementById('detailFormResult').value = rd.result;
    document.getElementById('detailFormDiagnosis').value = rd.diagnosis;

    document.getElementById('detailModalTitle').innerText = `Edit Report Details (Report: ${reportId})`;
    openModal('detailModal');
}

// --- FORM SUBMIT HANDLERS ---
function initFormHandlers() {
    // 1. Patient Form
    document.getElementById('patientForm').addEventListener('submit', async (e) => {
        e.preventDefault();
        const mode = document.getElementById('patientFormMode').value;
        const id = document.getElementById('patientFormId').value;
        const payload = {
            patient_id: id,
            name: document.getElementById('patientFormName').value,
            gender: document.getElementById('patientFormGender').value,
            dob: document.getElementById('patientFormDob').value,
            phone: document.getElementById('patientFormPhone').value,
            email: document.getElementById('patientFormEmail').value
        };

        const url = mode === 'add' ? `${API_URL}/api/patients` : `${API_URL}/api/patients?id=${id}`;
        const method = mode === 'add' ? 'POST' : 'PUT';

        await saveRecord(url, method, payload, 'patientModal', fetchPatients);
    });

    // 2. Doctor Form
    document.getElementById('doctorForm').addEventListener('submit', async (e) => {
        e.preventDefault();
        const mode = document.getElementById('doctorFormMode').value;
        const id = document.getElementById('doctorFormId').value;
        const payload = {
            name: document.getElementById('doctorFormName').value,
            specialization: document.getElementById('doctorFormSpecialization').value,
            phone: document.getElementById('doctorFormPhone').value
        };

        const url = mode === 'add' ? `${API_URL}/api/doctors` : `${API_URL}/api/doctors?id=${id}`;
        const method = mode === 'add' ? 'POST' : 'PUT';

        await saveRecord(url, method, payload, 'doctorModal', fetchDoctors);
    });

    // 3. Test Form
    document.getElementById('testForm').addEventListener('submit', async (e) => {
        e.preventDefault();
        const mode = document.getElementById('testFormMode').value;
        const id = document.getElementById('testFormId').value;
        const payload = {
            test_name: document.getElementById('testFormName').value
        };

        const url = mode === 'add' ? `${API_URL}/api/tests` : `${API_URL}/api/tests?id=${id}`;
        const method = mode === 'add' ? 'POST' : 'PUT';

        await saveRecord(url, method, payload, 'testModal', fetchTests);
    });

    // 4. Report Form
    document.getElementById('reportForm').addEventListener('submit', async (e) => {
        e.preventDefault();
        const mode = document.getElementById('reportFormMode').value;
        const id = document.getElementById('reportFormId').value;
        const payload = {
            patient_id: document.getElementById('reportFormPatient').value,
            doctor_id: document.getElementById('reportFormDoctor').value,
            report_date: document.getElementById('reportFormDate').value
        };

        const url = mode === 'add' ? `${API_URL}/api/reports` : `${API_URL}/api/reports?id=${id}`;
        const method = mode === 'add' ? 'POST' : 'PUT';

        await saveRecord(url, method, payload, 'reportModal', fetchReports);
    });

    // 5. Details Form
    document.getElementById('detailForm').addEventListener('submit', async (e) => {
        e.preventDefault();
        const mode = document.getElementById('detailFormMode').value;
        const reportId = document.getElementById('detailFormReport').value;
        const testId = document.getElementById('detailFormTest').value;
        
        const payload = {
            report_id: reportId,
            test_id: testId,
            result: document.getElementById('detailFormResult').value,
            diagnosis: document.getElementById('detailFormDiagnosis').value
        };

        const url = mode === 'add' ? `${API_URL}/api/details` : `${API_URL}/api/details?report_id=${reportId}&test_id=${testId}`;
        const method = mode === 'add' ? 'POST' : 'PUT';

        await saveRecord(url, method, payload, 'detailModal', fetchDetails);
    });

    // 6. Custom Join Query: Patient Chart Search
    document.getElementById('runPatientQueryBtn').addEventListener('click', runPatientReportQuery);
    document.getElementById('queryPatientName').addEventListener('keypress', (e) => {
        if (e.key === 'Enter') runPatientReportQuery();
    });
}

// Helper to Post/Put records to API and display trigger warning details
async function saveRecord(url, method, payload, modalId, refreshCallback) {
    try {
        const res = await fetch(url, {
            method: method,
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });
        
        const reply = await res.json();
        
        if (res.ok && reply.success) {
            showToast(reply.message, 'success');
            closeModal(modalId);
            refreshCallback();
        } else {
            // Show trigger validation details
            showToast(reply.message || 'Database error occurred.', 'error');
        }
    } catch (e) {
        showToast('Network error or connection lost.', 'error');
    }
}

// --- DELETE OPERATIONS CONTROLLER ---

function triggerDeletePatient(id) {
    confirmDelete(`${API_URL}/api/patients?id=${id}`, fetchPatients, 'Patient record successfully removed.');
}

function triggerDeleteDoctor(id) {
    confirmDelete(`${API_URL}/api/doctors?id=${id}`, fetchDoctors, 'Doctor record removed.');
}

function triggerDeleteTest(id) {
    confirmDelete(`${API_URL}/api/tests?id=${id}`, fetchTests, 'Test record deleted.');
}

function triggerDeleteReport(id) {
    confirmDelete(`${API_URL}/api/reports?id=${id}`, fetchReports, 'Medical report deleted.');
}

function triggerDeleteDetail(reportId, testId) {
    confirmDelete(`${API_URL}/api/details?report_id=${reportId}&test_id=${testId}`, fetchDetails, 'Detailed findings removed.');
}

function confirmDelete(url, refreshCallback, successMsg) {
    openModal('deleteConfirmModal');
    
    // Replace delete button to clear previous listeners
    const oldBtn = document.getElementById('deleteConfirmBtn');
    const newBtn = oldBtn.cloneNode(true);
    oldBtn.parentNode.replaceChild(newBtn, oldBtn);

    newBtn.addEventListener('click', async () => {
        try {
            const res = await fetch(url, { method: 'DELETE' });
            const reply = await res.json();
            
            if (res.ok && reply.success) {
                showToast(successMsg, 'success');
                closeModal('deleteConfirmModal');
                refreshCallback();
            } else {
                showToast(reply.message || 'Deletion failed.', 'error');
                closeModal('deleteConfirmModal');
            }
        } catch (e) {
            showToast('Failed to complete delete request.', 'error');
            closeModal('deleteConfirmModal');
        }
    });
}

// --- CUSTOM JOIN SEARCH HANDLER ---
async function runPatientReportQuery() {
    const name = document.getElementById('queryPatientName').value.trim();
    if (!name) {
        showToast('Please enter a patient name to search.', 'warning');
        return;
    }

    try {
        const res = await fetch(`${API_URL}/api/query/patient-report?name=${encodeURIComponent(name)}`);
        const list = await res.json();

        const container = document.getElementById('queryPatientResultsContainer');
        const emptyState = document.getElementById('queryPatientEmptyState');
        const tbody = document.querySelector('#queryPatientTable tbody');
        
        tbody.innerHTML = '';
        document.getElementById('queryResultsHeader').innerText = `Diagnostics Chart Results for "${name}"`;

        if (list.length === 0) {
            container.style.display = 'none';
            emptyState.style.display = 'block';
            emptyState.innerHTML = `
                <i class="fa-solid fa-folder-open"></i>
                <p>No diagnostics files found matching patient name "${name}".</p>
            `;
            return;
        }

        list.forEach(item => {
            tbody.innerHTML += `
                <tr>
                    <td><strong>${item.patient_name}</strong></td>
                    <td><span class="badge badge-info">${item.test_name}</span></td>
                    <td>${item.result}</td>
                    <td>${item.diagnosis}</td>
                    <td>${item.report_date}</td>
                </tr>
            `;
        });

        emptyState.style.display = 'none';
        container.style.display = 'block';
    } catch (e) {
        showToast('Error searching patient diagnostic reports.', 'error');
    }
}

// --- TOAST NOTIFICATIONS ENGINE ---
function showToast(message, type = 'info') {
    const container = document.getElementById('toastContainer');
    const toast = document.createElement('div');
    toast.className = `toast ${type}`;
    
    let icon = 'fa-info-circle';
    if (type === 'success') icon = 'fa-check-circle';
    else if (type === 'error') icon = 'fa-exclamation-triangle';
    else if (type === 'warning') icon = 'fa-exclamation-circle';

    toast.innerHTML = `
        <i class="fa-solid ${icon}"></i>
        <div class="toast-message">${message}</div>
        <button class="toast-close">&times;</button>
        <div class="toast-progress"></div>
    `;

    container.appendChild(toast);

    // Auto dismiss
    const timer = setTimeout(() => {
        dismissToast(toast);
    }, 4500);

    toast.querySelector('.toast-close').addEventListener('click', () => {
        clearTimeout(timer);
        dismissToast(toast);
    });
}

function dismissToast(toast) {
    toast.style.animation = 'slideIn 0.3s ease reverse forwards';
    toast.addEventListener('animationend', () => {
        toast.remove();
    });
}

// --- LOCAL INSTANT TABLE FILTERS ---
function initSearchFilters() {
    // Patients Table Filter
    document.getElementById('patientSearchInput').addEventListener('input', (e) => {
        const val = e.target.value.toLowerCase();
        const filtered = state.patients.filter(p => 
            p.name.toLowerCase().includes(val) || 
            p.phone.toLowerCase().includes(val) ||
            p.patient_id.toString().includes(val)
        );
        renderPatientsTable(filtered);
    });

    // Doctors Table Filter
    document.getElementById('doctorSearchInput').addEventListener('input', (e) => {
        const val = e.target.value.toLowerCase();
        const filtered = state.doctors.filter(d => 
            d.name.toLowerCase().includes(val) || 
            d.specialization.toLowerCase().includes(val)
        );
        renderDoctorsTable(filtered);
    });

    // Tests Table Filter
    document.getElementById('testSearchInput').addEventListener('input', (e) => {
        const val = e.target.value.toLowerCase();
        const filtered = state.tests.filter(t => 
            t.test_name.toLowerCase().includes(val)
        );
        renderTestsTable(filtered);
    });

    // Reports Table Filter
    document.getElementById('reportSearchInput').addEventListener('input', (e) => {
        const val = e.target.value.toLowerCase();
        const filtered = state.reports.filter(r => {
            const pat = state.patients.find(p => p.patient_id === r.patient_id);
            const doc = state.doctors.find(d => d.doctor_id === r.doctor_id);
            return r.report_id.toString().includes(val) ||
                   r.patient_id.toString().includes(val) ||
                   r.doctor_id.toString().includes(val) ||
                   (pat && pat.name.toLowerCase().includes(val)) ||
                   (doc && doc.name.toLowerCase().includes(val));
        });
        renderReportsTable(filtered);
    });

    // Details Table Filter
    document.getElementById('detailSearchInput').addEventListener('input', (e) => {
        const val = e.target.value.toLowerCase();
        const filtered = state.details.filter(rd => {
            const test = state.tests.find(t => t.test_id === rd.test_id);
            return rd.report_id.toString().includes(val) ||
                   rd.result.toLowerCase().includes(val) ||
                   rd.diagnosis.toLowerCase().includes(val) ||
                   (test && test.test_name.toLowerCase().includes(val));
        });
        renderDetailsTable(filtered);
    });
}
