"""
Call Monitor Server - Configuration
"""
import os
from pathlib import Path

# Base paths
BASE_DIR = Path(__file__).parent.parent
KEYS_DIR = BASE_DIR / "keys"
STORAGE_DIR = BASE_DIR / "storage"
ENCRYPTED_DIR = STORAGE_DIR / "encrypted"
DECRYPTED_DIR = STORAGE_DIR / "decrypted"

# Create directories if they don't exist
ENCRYPTED_DIR.mkdir(parents=True, exist_ok=True)
DECRYPTED_DIR.mkdir(parents=True, exist_ok=True)

# Key paths
PRIVATE_KEY_PATH = KEYS_DIR / "private_key.pem"
PUBLIC_KEY_PATH = KEYS_DIR / "public_key.pem"

# Server settings
HOST = os.getenv("HOST", "0.0.0.0")
PORT = int(os.getenv("PORT", 8000))
DEBUG = os.getenv("DEBUG", "true").lower() == "true"

# Database
DATABASE_PATH = BASE_DIR / "recordings.db"
