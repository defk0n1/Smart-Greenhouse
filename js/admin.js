import { IamService } from './iam-service.js';
import { GreenhouseService } from './greenhouse-service.js';
import { SensorService } from './sensor-service.js';
import { ActuatorService } from './actuator-service.js';
import { authManager } from './auth.js';

const iamService = new IamService(authManager);
const greenhouseService = new GreenhouseService();
const sensorService = new SensorService();
const actuatorService = new ActuatorService();

// State
let currentState = {
    view: 'dashboard', // dashboard, users, greenhouses, sensors, actuators
    data: [],
    selectedItem: null
};

// DOM Elements
const contentArea = document.getElementById('main-content');
const pageTitle = document.getElementById('page-title');
const navLinks = document.querySelectorAll('.nav-link');
const logoutBtn = document.getElementById('logout-btn');
const userInfoDisplay = document.getElementById('user-info-display');

// Init
function init() {
    setupNavigation();
    updateUserInfo();
    loadView('dashboard');
}

function updateUserInfo() {
    if (authManager.user) {
        userInfoDisplay.textContent = `${authManager.user.username} (Admin)`;
    }
}

function setupNavigation() {
    navLinks.forEach(link => {
        link.addEventListener('click', (e) => {
            e.preventDefault();
            const view = e.target.closest('a').dataset.view;
            loadView(view);
        });
    });

    logoutBtn.addEventListener('click', () => {
        authManager.logout();
    });
}

function setActiveLink(view) {
    navLinks.forEach(link => link.classList.remove('active'));
    document.querySelector(`[data-view="${view}"]`)?.classList.add('active');
}

async function loadView(view) {
    currentState.view = view;
    setActiveLink(view);
    contentArea.innerHTML = '<div class="loading-spinner"></div>';

    try {
        switch (view) {
            case 'dashboard':
                await renderDashboard();
                break;
            case 'users':
                await renderUsers();
                break;
            case 'greenhouses':
                await renderGreenhouses();
                break;
            case 'sensors':
                await renderSensors();
                break;
            case 'actuators':
                await renderActuators();
                break;
        }
    } catch (error) {
        console.error('Error loading view:', error);
        contentArea.innerHTML = `<div class="error-card">Error loading content: ${error.message}</div>`;
    }
}

// Expose loadView to window for inline onclick handlers
window.loadView = loadView;

// =============================================================================
// DASHBOARD
// =============================================================================
async function renderDashboard() {
    pageTitle.textContent = 'Dashboard';

    // 1. Render Structure Immediately (Skeleton)
    contentArea.innerHTML = `
        <div class="stats-grid">
            <div class="stat-card">
                <h3>Users</h3>
                <div class="value" id="stats-users"><span class="loading-dots">...</span></div>
            </div>
            <div class="stat-card">
                <h3>Greenhouses</h3>
                <div class="value" id="stats-greenhouses"><span class="loading-dots">...</span></div>
            </div>
            <div class="stat-card">
                <h3>Sensors</h3>
                <div class="value" id="stats-sensors"><span class="loading-dots">...</span></div>
            </div>
            <div class="stat-card">
                <h3>Actuators</h3>
                <div class="value" id="stats-actuators"><span class="loading-dots">...</span></div>
            </div>
        </div>
    `;

    // 2. Fetch Data Asynchronously and Update (Non-blocking)
    // We don't await here so the UI renders first
    Promise.all([
        iamService.getAllUsers().catch(() => []),
        greenhouseService.getAllGreenhouses().catch(() => []),
        sensorService.getAllSensors().catch(() => []),
        actuatorService.getAllActuators().catch(() => [])
    ]).then(([users, greenhouses, sensors, actuators]) => {
        // Update DOM if elements exist (checking in case user navigated away)
        const elUsers = document.getElementById('stats-users');
        if (elUsers) elUsers.textContent = users.length;

        const elGh = document.getElementById('stats-greenhouses');
        if (elGh) elGh.textContent = greenhouses.length;

        const elSensors = document.getElementById('stats-sensors');
        if (elSensors) elSensors.textContent = sensors.length;

        const elActuators = document.getElementById('stats-actuators');
        if (elActuators) elActuators.textContent = actuators.length;
    });
}

// =============================================================================
// USERS
// =============================================================================
async function renderUsers() {
    pageTitle.textContent = 'User Management';
    const users = await iamService.getAllUsers();

    let html = `
        <div class="table-container">
            <table class="styled-table">
                <thead>
                    <tr>
                        <th>USERNAME</th>
                        <th>EMAIL</th>
                        <th>ACTIONS</th>
                    </tr>
                </thead>
                <tbody>
    `;

    for (const user of users) {
        // Use data attributes to avoid quote escaping issues
        html += `
            <tr>
                <td>${user.username}</td>
                <td>${user.email || '-'}</td>
                <td>
                    <button class="btn-sm btn-success" 
                            onclick="window.openAssignGreenhouseModal('${user.username}', '${user.id}')">
                        Assign GH
                    </button>
                </td>
            </tr>
        `;
    }

    html += '</tbody></table></div>';
    contentArea.innerHTML = html;
}

window.toggleUserStatus = async (id, activate) => {
    try {
        await iamService.updateUserStatus(id, activate);
        loadView('users'); // Reload
    } catch (e) {
        alert('Error: ' + e.message);
    }
};

// =============================================================================
// GREENHOUSES
// =============================================================================
async function renderGreenhouses() {
    pageTitle.textContent = 'Greenhouse Management';
    const greenhouses = await greenhouseService.getAllGreenhouses();

    let html = `
        <div class="actions-bar">
            <button class="btn-primary" onclick="window.openCreateGreenhouseModal()">+ Create Greenhouse</button>
        </div>
        <div class="grid-container">
    `;

    if (greenhouses.length === 0) {
        html += '<div class="empty-state">No greenhouses found.</div>';
    } else {
        greenhouses.forEach(gh => {
            html += `
                <div class="card">
                    <div class="card-header">
                        <h3>${gh.name}</h3>
                        <span class="status-dot ${gh.status === 'ACTIVE' ? 'online' : 'offline'}"></span>
                    </div>
                    <div class="card-body">
                        <p>${gh.description || 'No description'}</p>
                        <p><small>Location: ${gh.location || 'Unknown'}</small></p>
                        <p><small>Owner: ${gh.ownerId}</small></p>
                    </div>
                    <div class="card-footer">
                        <button onclick="window.viewGreenhouseDetails('${gh.id}')">Manage Devices</button>
                        <button class="btn-danger" onclick="window.deleteGreenhouse('${gh.id}')">Delete</button>
                    </div>
                </div>
            `;
        });
    }

    html += '</div>';
    contentArea.innerHTML = html;
}

// Greenhouse Details (Attach/Detach Logic)
window.viewGreenhouseDetails = async (id) => {
    // We reuse the main area to show details
    contentArea.innerHTML = '<div class="loading-spinner"></div>';

    // Fetch all greenhouses and all devices
    const [allGreenhouses, allSensors, allActuators] = await Promise.all([
        greenhouseService.getAllGreenhouses(),
        sensorService.getAllSensors(),
        actuatorService.getAllActuators()
    ]);

    // Find current greenhouse
    const gh = allGreenhouses.find(g => g.id === id);

    // Fetch attached devices for current greenhouse
    const attachedSensors = await greenhouseService.getGreenhouseSensors(id);
    const attachedActuators = await greenhouseService.getGreenhouseActuators(id);

    const attachedSensorIds = attachedSensors.map(s => s.sensorId);
    const attachedActuatorIds = attachedActuators.map(a => a.actuatorId);

    // Build a list of ALL sensors/actuators attached to ANY greenhouse
    const allAttachedSensorIds = new Set();
    const allAttachedActuatorIds = new Set();

    for (const greenhouse of allGreenhouses) {
        try {
            const ghSensors = await greenhouseService.getGreenhouseSensors(greenhouse.id);
            const ghActuators = await greenhouseService.getGreenhouseActuators(greenhouse.id);

            ghSensors.forEach(s => allAttachedSensorIds.add(s.sensorId));
            ghActuators.forEach(a => allAttachedActuatorIds.add(a.actuatorId));
        } catch (e) {
            console.warn(`Could not fetch devices for greenhouse ${greenhouse.id}:`, e);
        }
    }

    // Filter available devices: only those NOT attached to ANY greenhouse
    const availableSensors = allSensors.filter(s => !allAttachedSensorIds.has(s.sensorId));
    const availableActuators = allActuators.filter(a => !allAttachedActuatorIds.has(a.actuatorId));

    contentArea.innerHTML = `
        <div class="details-header">
            <button onclick="window.loadView('greenhouses')" class="btn-back">← Back</button>
            <h2>${gh.name} Details</h2>
        </div>
        
        <div class="details-grid">
            <!-- SENSORS COLUMN -->
            <div class="details-col">
                <h3>Attached Sensors</h3>
                <ul class="device-list">
                    ${attachedSensors.map(s => `
                        <li>
                            <span>${s.type} (${s.sensorId})</span>
                            <button class="btn-sm btn-danger" onclick="window.detachSensor('${id}', '${s.sensorId}')">Detach</button>
                        </li>
                    `).join('')}
                    ${attachedSensors.length === 0 ? '<li class="muted">No sensors attached</li>' : ''}
                </ul>
                
                <h4>Attach Sensor</h4>
                <div class="attach-form">
                    <select id="sensor-select">
                        <option value="">Select Sensor...</option>
                        ${availableSensors.map(s => `<option value="${s.sensorId}">${s.type} (${s.sensorId})</option>`).join('')}
                    </select>
                    <button onclick="window.attachSensor('${id}')">Attach</button>
                </div>
            </div>

            <!-- ACTUATORS COLUMN -->
            <div class="details-col">
                <h3>Attached Actuators</h3>
                <ul class="device-list">
                    ${attachedActuators.map(a => `
                        <li>
                            <span>${a.type} (${a.actuatorId})</span>
                            <button class="btn-sm btn-danger" onclick="window.detachActuator('${id}', '${a.actuatorId}')">Detach</button>
                        </li>
                    `).join('')}
                    ${attachedActuators.length === 0 ? '<li class="muted">No actuators attached</li>' : ''}
                </ul>

                <h4>Attach Actuator</h4>
                <div class="attach-form">
                    <select id="actuator-select">
                        <option value="">Select Actuator...</option>
                        ${availableActuators.map(a => `<option value="${a.actuatorId}">${a.type} (${a.actuatorId})</option>`).join('')}
                    </select>
                    <button onclick="window.attachActuator('${id}')">Attach</button>
                </div>
            </div>
        </div>
    `;
}

// Global Actions for Greenhouse Details
window.attachSensor = async (ghId) => {
    const sensorId = document.getElementById('sensor-select').value;
    if (!sensorId) return;
    try {
        await greenhouseService.attachSensor(ghId, sensorId);
        window.viewGreenhouseDetails(ghId);
    } catch (e) { alert(e.message); }
};

window.detachSensor = async (ghId, sId) => {
    if (!confirm('Detach sensor?')) return;
    try {
        await greenhouseService.detachSensor(ghId, sId);
        window.viewGreenhouseDetails(ghId);
    } catch (e) { alert(e.message); }
};

window.attachActuator = async (ghId) => {
    const actId = document.getElementById('actuator-select').value;
    if (!actId) return;
    try {
        await greenhouseService.attachActuator(ghId, actId);
        window.viewGreenhouseDetails(ghId);
    } catch (e) { alert(e.message); }
};

window.detachActuator = async (ghId, aId) => {
    if (!confirm('Detach actuator?')) return;
    try {
        await greenhouseService.detachActuator(ghId, aId);
        window.viewGreenhouseDetails(ghId);
    } catch (e) { alert(e.message); }
};

window.assignUser = async (ghId) => {
    const username = document.getElementById('assign-username').value;
    if (!username) return;
    try {
        await greenhouseService.assignUserToGreenhouse(ghId, username);
        window.viewGreenhouseDetails(ghId);
    } catch (e) { alert(e.message); }
};

window.removeUser = async (ghId, username) => {
    if (!confirm('Remove user access?')) return;
    try {
        await greenhouseService.removeUserFromGreenhouse(ghId, username);
        window.viewGreenhouseDetails(ghId);
    } catch (e) { alert(e.message); }
};

window.deleteGreenhouse = async (id) => {
    if (!confirm('Are you sure you want to delete this greenhouse? This cannot be undone.')) return;
    try {
        await greenhouseService.deleteGreenhouse(id);
        renderGreenhouses();
    } catch (e) { alert(e.message); }
};

window.openCreateGreenhouseModal = () => {
    const name = prompt("Enter Greenhouse Name:");
    if (name) {
        greenhouseService.createGreenhouse({
            name: name,
            description: "Created via Admin PWA",
            location: "Unknown",
            status: "ACTIVE",
            sensors: [],
            actuators: [],
            authorizedUsers: []
        }).then(() => renderGreenhouses())
            .catch(e => alert(e.message));
    }
};


// =============================================================================
// SENSORS
// =============================================================================
async function renderSensors() {
    pageTitle.textContent = 'All Sensors';

    // Fetch both sensors and greenhouses to map relationships
    const [sensors, greenhouses] = await Promise.all([
        sensorService.getAllSensors(),
        greenhouseService.getAllGreenhouses()
    ]);

    // Create map of SensorID -> Greenhouse
    const sensorGhMap = {};
    greenhouses.forEach(gh => {
        if (gh.sensors) {
            gh.sensors.forEach(s => {
                const sId = (typeof s === 'object') ? s.sensorId : s;
                sensorGhMap[sId] = gh;
            });
        }
    });

    // Build greenhouse filter dropdown
    const ghFilterOptions = greenhouses.map(gh =>
        `<option value="${gh.id}">${gh.name}</option>`
    ).join('');

    let html = `
        <div class="filter-bar">
            <label>Filter by Greenhouse:</label>
            <select id="sensor-gh-filter" onchange="window.filterSensorsByGreenhouse()">
                <option value="all">All Greenhouses</option>
                ${ghFilterOptions}
            </select>
        </div>
        <div class="table-container">
            <table class="styled-table" id="sensors-table">
                <thead>
                    <tr>
                        <th>ID</th>
                        <th>Type</th>
                        <th>Value</th>
                        <th>Greenhouse</th>
                    </tr>
                </thead>
                <tbody>
    `;

    sensors.forEach(s => {
        const ghInfo = sensorGhMap[s.sensorId];
        const ghName = ghInfo ? ghInfo.name : 'Unassigned';
        const ghId = ghInfo ? ghInfo.id : 'none';

        html += `
            <tr data-greenhouse="${ghId}">
                <td>${s.sensorId}</td>
                <td>${s.type}</td>
                <td>${s.value !== undefined ? s.value.toFixed(2) : '-'} ${s.unit || ''}</td>
                <td>${ghName}</td>
            </tr>
        `;
    });

    html += '</tbody></table></div>';
    contentArea.innerHTML = html;
}

window.filterSensorsByGreenhouse = function () {
    const selectedGh = document.getElementById('sensor-gh-filter').value;
    const rows = document.querySelectorAll('#sensors-table tbody tr');

    rows.forEach(row => {
        if (selectedGh === 'all' || row.dataset.greenhouse === selectedGh) {
            row.style.display = '';
        } else {
            row.style.display = 'none';
        }
    });
};

// =============================================================================
// ACTUATORS
// =============================================================================
async function renderActuators() {
    pageTitle.textContent = 'All Actuators';

    const [actuators, greenhouses] = await Promise.all([
        actuatorService.getAllActuators(),
        greenhouseService.getAllGreenhouses()
    ]);

    // Map ActuatorID -> Greenhouse
    const actGhMap = {};
    greenhouses.forEach(gh => {
        if (gh.actuators) {
            gh.actuators.forEach(a => {
                const aId = (typeof a === 'object') ? a.actuatorId : a;
                actGhMap[aId] = gh;
            });
        }
    });

    // Build greenhouse filter dropdown
    const ghFilterOptions = greenhouses.map(gh =>
        `<option value="${gh.id}">${gh.name}</option>`
    ).join('');

    let html = `
        <div class="filter-bar">
            <label>Filter by Greenhouse:</label>
            <select id="actuator-gh-filter" onchange="window.filterActuatorsByGreenhouse()">
                <option value="all">All Greenhouses</option>
                ${ghFilterOptions}
            </select>
        </div>
        <div class="table-container">
            <table class="styled-table" id="actuators-table">
                <thead>
                    <tr>
                        <th>ID</th>
                        <th>Type</th>
                        <th>State</th>
                        <th>Greenhouse</th>
                    </tr>
                </thead>
                <tbody>
    `;

    actuators.forEach(a => {
        const ghInfo = actGhMap[a.actuatorId];
        const ghName = ghInfo ? ghInfo.name : 'Unassigned';
        const ghId = ghInfo ? ghInfo.id : 'none';

        html += `
            <tr data-greenhouse="${ghId}">
                <td>${a.actuatorId}</td>
                <td>${a.type}</td>
                <td>${a.status || 'UNKNOWN'}</td>
                <td>${ghName}</td>
            </tr>
        `;
    });

    html += '</tbody></table></div>';
    contentArea.innerHTML = html;
}

window.filterActuatorsByGreenhouse = function () {
    const selectedGh = document.getElementById('actuator-gh-filter').value;
    const rows = document.querySelectorAll('#actuators-table tbody tr');

    rows.forEach(row => {
        if (selectedGh === 'all' || row.dataset.greenhouse === selectedGh) {
            row.style.display = '';
        } else {
            row.style.display = 'none';
        }
    });
};

window.sendActuatorCommand = async (id, cmd) => {
    try {
        const res = await actuatorService.sendCommand(id, cmd, 0.0);
        alert(res);
        renderActuators();
    } catch (e) {
        alert(e.message);
    }
};

// =============================================================================
// USER ASSIGNMENT MODAL
// =============================================================================
window.openAssignGreenhouseModal = async (username, userId) => {
    try {
        console.log('Opening modal for user:', username, userId);

        const greenhouses = await greenhouseService.getAllGreenhouses();
        console.log('Fetched greenhouses:', greenhouses);

        if (!greenhouses || greenhouses.length === 0) {
            alert('No greenhouses available. Please create a greenhouse first.');
            return;
        }

        // Build checkbox list
        let checkboxList = greenhouses.map(gh => {
            const isAssigned = gh.authorizedUsers && gh.authorizedUsers.includes(username);
            const checkedAttr = isAssigned ? 'checked' : '';

            return `
                <div class="checkbox-item">
                    <input type="checkbox" 
                           id="gh-${gh.id}" 
                           value="${gh.id}" 
                           ${checkedAttr}
                           data-initial="${isAssigned}">
                    <label for="gh-${gh.id}">${gh.name}</label>
                </div>
            `;
        }).join('');

        // Create modal with checkboxes
        const modalHtml = `
            <div class="modal-overlay" id="assign-modal">
                <div class="modal-card">
                    <h3>Manage Greenhouses for ${username}</h3>
                    <p>Check/uncheck greenhouses to assign/unassign:</p>
                    <div class="checkbox-list">
                        ${checkboxList}
                    </div>
                    <div class="modal-actions">
                        <button onclick="document.getElementById('assign-modal').remove()">Cancel</button>
                        <button class="btn-primary" onclick="window.confirmAssignCheckboxes('${username}')">Save Changes</button>
                    </div>
                </div>
            </div>
        `;

        document.body.insertAdjacentHTML('beforeend', modalHtml);
        console.log('Modal added to DOM');
    } catch (error) {
        console.error('Error opening modal:', error);
        alert('Error opening modal: ' + error.message);
    }
};

window.confirmAssignCheckboxes = async (username) => {
    try {
        const checkboxes = document.querySelectorAll('#assign-modal input[type="checkbox"]');

        let promises = [];
        checkboxes.forEach(cb => {
            const ghId = cb.value;
            const wasAssigned = cb.dataset.initial === 'true';
            const isNowChecked = cb.checked;

            // If state changed, make API call
            if (wasAssigned && !isNowChecked) {
                // Unassign
                console.log('Unassigning', username, 'from', ghId);
                promises.push(greenhouseService.removeUserFromGreenhouse(ghId, username));
            } else if (!wasAssigned && isNowChecked) {
                // Assign
                console.log('Assigning', username, 'to', ghId);
                promises.push(greenhouseService.assignUserToGreenhouse(ghId, username));
            }
        });

        if (promises.length > 0) {
            await Promise.all(promises);
            alert(`Changes saved for ${username}`);
        } else {
            alert('No changes made');
        }

        document.getElementById('assign-modal').remove();
    } catch (e) {
        console.error('Assignment error:', e);
        alert('Failed to save changes: ' + e.message);
    }
};

// Start
init();
