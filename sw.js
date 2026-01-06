const CACHE_NAME = 'admin-pwa-v1';
const ASSETS = [
    './',
    './index.html',
    './css/style.css',
    './css/loading.css',
    './js/init.js',
    './js/auth.js',
    './js/config.js',
    './pages/welcome.html',
    './css/welcome.css',
    './js/welcome.js'
];

self.addEventListener('install', (e) => {
    e.waitUntil(
        caches.open(CACHE_NAME).then((cache) => cache.addAll(ASSETS))
    );
});

self.addEventListener('fetch', (e) => {
    // Basic network-first strategy for Admin to ensure fresh data
    const url = new URL(e.request.url);

    // Skip non-GET requests or API calls (let browser handle CORS/etc normally or handle specifically)
    // For API calls, if we return undefined, the browser does a normal fetch.
    if (e.request.method !== 'GET' || url.pathname.startsWith('/api/') || url.pathname.includes('/api/')) {
        return;
    }

    e.respondWith(
        fetch(e.request)
            .then(response => {
                // If network fetch succeeds, return it
                return response;
            })
            .catch(() => {
                // If network fails (offline), try cache
                return caches.match(e.request).then(response => {
                    if (response) {
                        return response;
                    }
                    // If neither, return a basic offline response or null (which might throw the error user saw)
                    // For now, let's just return a simple error response to avoid "Failed to convert value"
                    return new Response('Offline: Resource not found', { status: 404, statusText: 'Not Found' });
                });
            })
    );
});
