# Call Monitor Server

Python FastAPI server that receives encrypted call recordings from the Android app and provides decryption for playback.

## Setup

1. Install dependencies:
```bash
pip install -r requirements.txt
```

2. Run the server:
```bash
python run.py
```

Or with uvicorn directly:
```bash
uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload
```

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/` | Web dashboard with recording list |
| POST | `/upload` | Upload encrypted recording from Android app |
| GET | `/recordings` | List all recordings as JSON |
| GET | `/recordings/{id}` | Get recording details |
| GET | `/recordings/{id}/stream` | Stream decrypted audio for playback |
| GET | `/recordings/{id}/download` | Download decrypted audio file |
| DELETE | `/recordings/{id}` | Delete a recording |
| GET | `/health` | Health check |

## Configuration

The server uses the RSA private key at `keys/private_key.pem` to decrypt recordings.

- `HOST`: Server bind address (default: 0.0.0.0)
- `PORT`: Server port (default: 8000)

## Security Note

The private key (`keys/private_key.pem`) must be kept secure. Only the server can decrypt recordings - the Android app only has the public key.
