"""
Call Monitor Server - FastAPI Application

This server receives encrypted call recordings from the Android app,
stores them, and provides decryption for playback.
"""
import os
import tempfile
from pathlib import Path
from datetime import datetime

from fastapi import FastAPI, File, UploadFile, Form, HTTPException
from fastapi.responses import FileResponse, HTMLResponse, StreamingResponse
from fastapi.staticfiles import StaticFiles
from fastapi.middleware.cors import CORSMiddleware
import aiofiles

from .config import ENCRYPTED_DIR, DECRYPTED_DIR, HOST, PORT
from .crypto import HybridDecryptor
from .database import Database


# Initialize FastAPI app
app = FastAPI(
    title="Call Monitor Server",
    description="Receives and decrypts call recordings from Android app",
    version="1.0.0"
)

# Add CORS middleware
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Initialize services
db = Database()
decryptor = HybridDecryptor()


@app.get("/", response_class=HTMLResponse)
async def home():
    """Home page with recording list"""
    recordings = db.get_all_recordings()
    
    rows = ""
    for rec in recordings:
        rec_dict = rec.to_dict()
        rows += f"""
        <tr>
            <td>{rec_dict['id']}</td>
            <td>{rec_dict['phone_number']}</td>
            <td>{rec_dict['call_type']}</td>
            <td>{rec_dict['formatted_date']}</td>
            <td>{rec_dict['formatted_duration']}</td>
            <td>{rec_dict['file_size'] // 1024} KB</td>
            <td>
                <button onclick="playRecording({rec_dict['id']})">▶️ Play</button>
                <a href="/recordings/{rec_dict['id']}/download" download>⬇️ Download</a>
            </td>
        </tr>
        """
    
    html = f"""
    <!DOCTYPE html>
    <html>
    <head>
        <title>Call Monitor Server</title>
        <style>
            body {{
                font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
                max-width: 1200px;
                margin: 0 auto;
                padding: 20px;
                background: #1a1a2e;
                color: #eee;
            }}
            h1 {{
                color: #00d9ff;
                text-align: center;
            }}
            table {{
                width: 100%;
                border-collapse: collapse;
                margin-top: 20px;
                background: #16213e;
                border-radius: 8px;
                overflow: hidden;
            }}
            th, td {{
                padding: 12px 15px;
                text-align: left;
                border-bottom: 1px solid #0f3460;
            }}
            th {{
                background: #0f3460;
                color: #00d9ff;
                font-weight: 600;
            }}
            tr:hover {{
                background: #1f4068;
            }}
            button {{
                background: #00d9ff;
                color: #1a1a2e;
                border: none;
                padding: 8px 16px;
                border-radius: 4px;
                cursor: pointer;
                font-weight: 600;
                margin-right: 8px;
            }}
            button:hover {{
                background: #00b4d8;
            }}
            a {{
                color: #00d9ff;
                text-decoration: none;
            }}
            a:hover {{
                text-decoration: underline;
            }}
            .stats {{
                display: flex;
                gap: 20px;
                margin-bottom: 20px;
            }}
            .stat-card {{
                background: #16213e;
                padding: 20px;
                border-radius: 8px;
                flex: 1;
                text-align: center;
            }}
            .stat-card h3 {{
                margin: 0;
                color: #00d9ff;
                font-size: 2em;
            }}
            .stat-card p {{
                margin: 5px 0 0;
                color: #888;
            }}
            #audio-player {{
                position: fixed;
                bottom: 20px;
                left: 50%;
                transform: translateX(-50%);
                background: #16213e;
                padding: 20px;
                border-radius: 8px;
                display: none;
                box-shadow: 0 4px 20px rgba(0,0,0,0.5);
            }}
            #audio-player.active {{
                display: block;
            }}
            audio {{
                width: 400px;
            }}
        </style>
    </head>
    <body>
        <h1>📞 Call Monitor Server</h1>
        
        <div class="stats">
            <div class="stat-card">
                <h3>{len(recordings)}</h3>
                <p>Total Recordings</p>
            </div>
            <div class="stat-card">
                <h3>{sum(1 for r in recordings if r.is_incoming)}</h3>
                <p>Incoming Calls</p>
            </div>
            <div class="stat-card">
                <h3>{sum(1 for r in recordings if not r.is_incoming)}</h3>
                <p>Outgoing Calls</p>
            </div>
        </div>
        
        <table>
            <thead>
                <tr>
                    <th>ID</th>
                    <th>Phone Number</th>
                    <th>Type</th>
                    <th>Date/Time</th>
                    <th>Duration</th>
                    <th>Size</th>
                    <th>Actions</th>
                </tr>
            </thead>
            <tbody>
                {rows if rows else '<tr><td colspan="7" style="text-align:center;">No recordings yet</td></tr>'}
            </tbody>
        </table>
        
        <div id="audio-player">
            <p id="now-playing"></p>
            <audio id="audio" controls></audio>
            <button onclick="closePlayer()">Close</button>
        </div>
        
        <script>
            function playRecording(id) {{
                const audio = document.getElementById('audio');
                const player = document.getElementById('audio-player');
                const nowPlaying = document.getElementById('now-playing');
                
                audio.src = '/recordings/' + id + '/stream';
                nowPlaying.textContent = 'Playing recording #' + id;
                player.classList.add('active');
                audio.play();
            }}
            
            function closePlayer() {{
                const audio = document.getElementById('audio');
                const player = document.getElementById('audio-player');
                audio.pause();
                player.classList.remove('active');
            }}
        </script>
    </body>
    </html>
    """
    return html


@app.post("/upload")
async def upload_recording(
    file: UploadFile = File(...),
    phone_number: str = Form(...),
    is_incoming: str = Form(...),
    timestamp: str = Form(...),
    duration: str = Form(...)
):
    """
    Receive an encrypted recording from the Android app.
    """
    try:
        # Generate unique filename
        file_name = f"{timestamp}_{phone_number.replace('+', '').replace(' ', '')}.enc"
        file_path = ENCRYPTED_DIR / file_name
        
        # Save the encrypted file
        async with aiofiles.open(file_path, 'wb') as f:
            content = await file.read()
            await f.write(content)
        
        # Parse metadata
        is_incoming_bool = is_incoming.lower() == "true"
        timestamp_int = int(timestamp)
        duration_int = int(duration)
        file_size = os.path.getsize(file_path)
        
        # Save to database
        recording_id = db.add_recording(
            file_name=file_name,
            file_path=str(file_path),
            phone_number=phone_number,
            is_incoming=is_incoming_bool,
            timestamp=timestamp_int,
            duration=duration_int,
            file_size=file_size
        )
        
        print(f"✅ Received recording: {file_name} (ID: {recording_id})")
        
        return {
            "status": "success",
            "file_id": str(recording_id),
            "message": "Recording uploaded successfully"
        }
        
    except Exception as e:
        print(f"❌ Upload error: {e}")
        raise HTTPException(status_code=500, detail=str(e))


@app.get("/recordings")
async def list_recordings():
    """List all recordings"""
    recordings = db.get_all_recordings()
    return {
        "count": len(recordings),
        "recordings": [rec.to_dict() for rec in recordings]
    }


@app.get("/recordings/{recording_id}")
async def get_recording(recording_id: int):
    """Get details of a specific recording"""
    recording = db.get_recording(recording_id)
    if not recording:
        raise HTTPException(status_code=404, detail="Recording not found")
    return recording.to_dict()


@app.get("/recordings/{recording_id}/stream")
async def stream_recording(recording_id: int):
    """
    Decrypt and stream a recording for playback.
    Decrypts on-the-fly and streams the audio.
    """
    recording = db.get_recording(recording_id)
    if not recording:
        raise HTTPException(status_code=404, detail="Recording not found")
    
    encrypted_path = Path(recording.file_path)
    if not encrypted_path.exists():
        raise HTTPException(status_code=404, detail="Recording file not found")
    
    try:
        # Decrypt to bytes
        decrypted_data = decryptor.decrypt_to_bytes(encrypted_path)
        
        # Stream the decrypted audio
        return StreamingResponse(
            iter([decrypted_data]),
            media_type="audio/mpeg",
            headers={
                "Content-Disposition": f"inline; filename={recording.file_name.replace('.enc', '.mp3')}"
            }
        )
        
    except Exception as e:
        print(f"❌ Decryption error: {e}")
        raise HTTPException(status_code=500, detail=f"Decryption failed: {str(e)}")


@app.get("/recordings/{recording_id}/download")
async def download_recording(recording_id: int):
    """
    Decrypt and download a recording.
    """
    recording = db.get_recording(recording_id)
    if not recording:
        raise HTTPException(status_code=404, detail="Recording not found")
    
    encrypted_path = Path(recording.file_path)
    if not encrypted_path.exists():
        raise HTTPException(status_code=404, detail="Recording file not found")
    
    try:
        # Decrypt to a temporary file
        decrypted_path = DECRYPTED_DIR / recording.file_name.replace('.enc', '.mp3')
        decryptor.decrypt_file(encrypted_path, decrypted_path)
        
        return FileResponse(
            decrypted_path,
            media_type="audio/mpeg",
            filename=recording.file_name.replace('.enc', '.mp3')
        )
        
    except Exception as e:
        print(f"❌ Decryption error: {e}")
        raise HTTPException(status_code=500, detail=f"Decryption failed: {str(e)}")


@app.delete("/recordings/{recording_id}")
async def delete_recording(recording_id: int):
    """Delete a recording"""
    recording = db.get_recording(recording_id)
    if not recording:
        raise HTTPException(status_code=404, detail="Recording not found")
    
    # Delete the encrypted file
    encrypted_path = Path(recording.file_path)
    if encrypted_path.exists():
        encrypted_path.unlink()
    
    # Delete from database
    db.delete_recording(recording_id)
    
    return {"status": "success", "message": "Recording deleted"}


@app.get("/health")
async def health_check():
    """Health check endpoint"""
    return {"status": "healthy", "timestamp": datetime.now().isoformat()}


if __name__ == "__main__":
    import uvicorn
    print(f"🚀 Starting Call Monitor Server on http://{HOST}:{PORT}")
    uvicorn.run(app, host=HOST, port=PORT)
