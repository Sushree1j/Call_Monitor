"""
Call Monitor Server - Database Models and Operations
"""
import sqlite3
from datetime import datetime
from pathlib import Path
from typing import List, Optional
from dataclasses import dataclass

from .config import DATABASE_PATH


@dataclass
class Recording:
    id: int
    file_name: str
    file_path: str
    phone_number: str
    is_incoming: bool
    timestamp: int
    duration: int
    file_size: int
    uploaded_at: str
    
    def to_dict(self):
        return {
            "id": self.id,
            "file_name": self.file_name,
            "file_path": self.file_path,
            "phone_number": self.phone_number,
            "is_incoming": self.is_incoming,
            "timestamp": self.timestamp,
            "duration": self.duration,
            "file_size": self.file_size,
            "uploaded_at": self.uploaded_at,
            "call_type": "Incoming" if self.is_incoming else "Outgoing",
            "formatted_date": datetime.fromtimestamp(self.timestamp / 1000).strftime("%Y-%m-%d %H:%M:%S"),
            "formatted_duration": f"{self.duration // 60000}:{(self.duration // 1000) % 60:02d}"
        }


class Database:
    def __init__(self, db_path: Path = DATABASE_PATH):
        self.db_path = db_path
        self._init_db()
    
    def _init_db(self):
        """Initialize the database and create tables if they don't exist"""
        with sqlite3.connect(self.db_path) as conn:
            conn.execute("""
                CREATE TABLE IF NOT EXISTS recordings (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    file_name TEXT NOT NULL,
                    file_path TEXT NOT NULL,
                    phone_number TEXT NOT NULL,
                    is_incoming BOOLEAN NOT NULL,
                    timestamp INTEGER NOT NULL,
                    duration INTEGER NOT NULL,
                    file_size INTEGER NOT NULL,
                    uploaded_at TEXT NOT NULL
                )
            """)
            conn.commit()
    
    def add_recording(
        self,
        file_name: str,
        file_path: str,
        phone_number: str,
        is_incoming: bool,
        timestamp: int,
        duration: int,
        file_size: int
    ) -> int:
        """Add a new recording to the database"""
        with sqlite3.connect(self.db_path) as conn:
            cursor = conn.execute(
                """
                INSERT INTO recordings 
                (file_name, file_path, phone_number, is_incoming, timestamp, duration, file_size, uploaded_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """,
                (
                    file_name,
                    file_path,
                    phone_number,
                    is_incoming,
                    timestamp,
                    duration,
                    file_size,
                    datetime.now().isoformat()
                )
            )
            conn.commit()
            return cursor.lastrowid
    
    def get_all_recordings(self) -> List[Recording]:
        """Get all recordings"""
        with sqlite3.connect(self.db_path) as conn:
            conn.row_factory = sqlite3.Row
            cursor = conn.execute(
                "SELECT * FROM recordings ORDER BY timestamp DESC"
            )
            return [Recording(**dict(row)) for row in cursor.fetchall()]
    
    def get_recording(self, recording_id: int) -> Optional[Recording]:
        """Get a single recording by ID"""
        with sqlite3.connect(self.db_path) as conn:
            conn.row_factory = sqlite3.Row
            cursor = conn.execute(
                "SELECT * FROM recordings WHERE id = ?",
                (recording_id,)
            )
            row = cursor.fetchone()
            if row:
                return Recording(**dict(row))
            return None
    
    def delete_recording(self, recording_id: int) -> bool:
        """Delete a recording by ID"""
        with sqlite3.connect(self.db_path) as conn:
            cursor = conn.execute(
                "DELETE FROM recordings WHERE id = ?",
                (recording_id,)
            )
            conn.commit()
            return cursor.rowcount > 0
