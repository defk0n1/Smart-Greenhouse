// sw.js - Service Worker for PWA
const CACHE_NAME = 'greenhouse-v3';
const urlsToCache = [
    '/',
    '/index.html',
    '/css/style.css',
    '/js/config.js',
    '/js/auth.js',
    '/js/mvp.js',
    '/js/main.js',
    '/manifest.json',
    '/icons/icon-192x192.png',
    '/icons/icon-512x512.png'
];

// Install event - cache resources
self.addEventListener('install', (event) => {
    console.log('Service Worker: Installing...');
    event.waitUntil(
        caches.open(CACHE_NAME)
            .then((cache) => {
                console.log('Service Worker: Caching files');
                return cache.addAll(urlsToCache);
            })
            .catch((err) => {
                console.log('Service Worker: Cache error', err);
            })
    );
});

// Activate event - clean up old caches
self.addEventListener('activate', (event) => {
    console.log('Service Worker: Activating...');
    event.waitUntil(
        caches.keys().then((cacheNames) => {
            return Promise.all(
                cacheNames.map((cache) => {
                    if (cache !== CACHE_NAME) {
                        console.log('Service Worker: Deleting old cache', cache);
                        return caches.delete(cache);
                    }
                })
            );
        })
    );
});

// Fetch event - serve from cache, fallback to network
self.addEventListener('fetch', (event) => {
    // Skip Service Worker for API requests to backend
    // Let them go directly to the server to avoid CORS issues
    const url = new URL(event.request.url);
    if (url.hostname === 'localhost' && url.port === '8080') {
        // This is an API request - don't intercept it
        return;
    }

    event.respondWith(
        caches.match(event.request)
            .then((response) => {
                // Return cached version or fetch from network
                return response || fetch(event.request);
            })
            .catch(() => {
                // Fallback for failed requests (only for PWA assets)
                console.log('Service Worker: Fetch failed for', event.request.url);
                return new Response('Network error or offline', {
                    status: 503,
                    statusText: 'Service Unavailable',
                    headers: new Headers({ 'Content-Type': 'text/plain' })
                });
            })
    );
});
