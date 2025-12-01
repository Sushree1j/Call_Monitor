"""
Call Monitor Server - Run Script
"""
import uvicorn
from app.config import HOST, PORT

if __name__ == "__main__":
    print(f"""
╔══════════════════════════════════════════════════════════════╗
║                    📞 Call Monitor Server                      ║
╠══════════════════════════════════════════════════════════════╣
║  Starting server on http://{HOST}:{PORT}                         ║
║                                                                ║
║  Endpoints:                                                    ║
║    GET  /              - Web dashboard                         ║
║    POST /upload        - Upload encrypted recording            ║
║    GET  /recordings    - List all recordings                   ║
║    GET  /recordings/id - Get recording details                 ║
║    GET  /recordings/id/stream   - Stream decrypted audio       ║
║    GET  /recordings/id/download - Download decrypted audio     ║
║                                                                ║
║  Configure your Android app with this server's IP address     ║
╚══════════════════════════════════════════════════════════════╝
    """)
    
    uvicorn.run(
        "app.main:app",
        host=HOST,
        port=PORT,
        reload=True,
        log_level="info"
    )
