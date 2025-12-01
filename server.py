"""
Simple HTTP Server pour tester la PWA localement
Usage: python server.py
Accès: http://localhost:8000
"""

import http.server
import socketserver
import os

PORT = 8000

class MyHTTPRequestHandler(http.server.SimpleHTTPRequestHandler):
    extensions_map = {
        '': 'application/octet-stream',
        '.manifest': 'text/cache-manifest',
        '.html': 'text/html',
        '.png': 'image/png',
        '.jpg': 'image/jpg',
        '.svg': 'image/svg+xml',  # Support pour les fichiers SVG
        '.css': 'text/css',
        '.js': 'application/javascript',
        '.json': 'application/json',
        '.xml': 'application/xml',
        '.wasm': 'application/wasm',
    }
    
    def end_headers(self):
        # Ajout des headers nécessaires pour une PWA
        self.send_header('Cache-Control', 'no-cache, no-store, must-revalidate')
        self.send_header('Pragma', 'no-cache')
        self.send_header('Expires', '0')
        self.send_header('Service-Worker-Allowed', '/')
        super().end_headers()

    def log_message(self, format, *args):
        # Messages personnalisés pour le log
        print(f"[{self.log_date_time_string()}] {format % args}")

if __name__ == '__main__':
    os.chdir(os.path.dirname(os.path.abspath(__file__)))
    
    with socketserver.TCPServer(("", PORT), MyHTTPRequestHandler) as httpd:
        print("=" * 60)
        print(f"🚀 Serveur PWA Greenhouse démarré!")
        print(f"📱 URL: http://localhost:{PORT}")
        print(f"📁 Répertoire: {os.getcwd()}")
        print("=" * 60)
        print("\n✨ Pour installer la PWA:")
        print("   1. Ouvrez http://localhost:8000 dans Chrome")
        print("   2. Cliquez sur le bouton 'Install' qui apparaît")
        print("   3. Ou utilisez l'icône d'installation dans la barre d'adresse")
        print("\n⚠️  Appuyez sur Ctrl+C pour arrêter le serveur\n")
        
        try:
            httpd.serve_forever()
        except KeyboardInterrupt:
            print("\n\n👋 Serveur arrêté!")
