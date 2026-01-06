"""
Simple HTTP Server for Admin PWA
Usage: python server.py
Access: http://localhost:8001
"""

import http.server
import socketserver
import os

PORT = 8001

class PWAHandler(http.server.SimpleHTTPRequestHandler):
    extensions_map = {
        '': 'application/octet-stream',
        '.manifest': 'text/cache-manifest',
        '.html': 'text/html',
        '.png': 'image/png',
        '.jpg': 'image/jpg',
        '.svg': 'image/svg+xml',
        '.css': 'text/css',
        '.js': 'application/javascript',
        '.json': 'application/json',
        '.xml': 'application/xml',
    }
    
    def end_headers(self):
        # PWA & Cache Headers
        self.send_header('Cache-Control', 'no-cache, no-store, must-revalidate')
        self.send_header('Pragma', 'no-cache')
        self.send_header('Expires', '0')
        self.send_header('Service-Worker-Allowed', '/')
        super().end_headers()

class ThreadingHTTPServer(socketserver.ThreadingMixIn, socketserver.TCPServer):
    daemon_threads = True
    allow_reuse_address = True

if __name__ == '__main__':
    os.chdir(os.path.dirname(os.path.abspath(__file__)))
    
    with ThreadingHTTPServer(("", PORT), PWAHandler) as httpd:
        print("=" * 60)
        print(f"🚀 Admin PWA Server Started!")
        print(f"📱 URL: http://localhost:8001")
        print(f"📁 Root: {os.getcwd()}")
        print("=" * 60)
        
        try:
            httpd.serve_forever()
        except KeyboardInterrupt:
            print("\n👋 Server stopped.")
