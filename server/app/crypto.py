"""
Call Monitor Server - Hybrid Decryption (RSA + AES-GCM)

File format from Android app:
[4 bytes: encrypted AES key length]
[N bytes: RSA-encrypted AES key]
[12 bytes: GCM IV/nonce]
[remaining: AES-GCM encrypted audio data with 16-byte auth tag appended]
"""
import struct
from pathlib import Path
from Crypto.PublicKey import RSA
from Crypto.Cipher import AES, PKCS1_OAEP
from Crypto.Hash import SHA256

from .config import PRIVATE_KEY_PATH


class HybridDecryptor:
    """Decrypts files encrypted with hybrid RSA + AES-GCM encryption"""
    
    def __init__(self, private_key_path: Path = PRIVATE_KEY_PATH):
        with open(private_key_path, 'rb') as f:
            self.private_key = RSA.import_key(f.read())
        self.rsa_cipher = PKCS1_OAEP.new(self.private_key, hashAlgo=SHA256)
    
    def decrypt_file(self, encrypted_path: Path, output_path: Path = None) -> Path:
        """
        Decrypt an encrypted file.
        
        Args:
            encrypted_path: Path to the encrypted .enc file
            output_path: Optional output path for decrypted file
            
        Returns:
            Path to the decrypted audio file
        """
        with open(encrypted_path, 'rb') as f:
            # Read encrypted AES key length (4 bytes, big-endian)
            key_len_bytes = f.read(4)
            key_length = struct.unpack('>I', key_len_bytes)[0]
            
            # Read encrypted AES key
            encrypted_aes_key = f.read(key_length)
            
            # Read IV (12 bytes for GCM)
            iv = f.read(12)
            
            # Read encrypted data (includes 16-byte auth tag at end)
            encrypted_data = f.read()
        
        # Decrypt AES key with RSA private key
        aes_key = self.rsa_cipher.decrypt(encrypted_aes_key)
        
        # Decrypt audio with AES-GCM
        cipher = AES.new(aes_key, AES.MODE_GCM, nonce=iv)
        
        # The auth tag is the last 16 bytes
        ciphertext = encrypted_data[:-16]
        auth_tag = encrypted_data[-16:]
        
        decrypted_data = cipher.decrypt_and_verify(ciphertext, auth_tag)
        
        # Determine output path
        if output_path is None:
            output_path = encrypted_path.with_suffix('.mp3')
        
        # Write decrypted file
        with open(output_path, 'wb') as f:
            f.write(decrypted_data)
        
        return output_path
    
    def decrypt_to_bytes(self, encrypted_path: Path) -> bytes:
        """
        Decrypt an encrypted file and return the bytes directly.
        
        Args:
            encrypted_path: Path to the encrypted .enc file
            
        Returns:
            Decrypted audio data as bytes
        """
        with open(encrypted_path, 'rb') as f:
            # Read encrypted AES key length (4 bytes, big-endian)
            key_len_bytes = f.read(4)
            key_length = struct.unpack('>I', key_len_bytes)[0]
            
            # Read encrypted AES key
            encrypted_aes_key = f.read(key_length)
            
            # Read IV (12 bytes for GCM)
            iv = f.read(12)
            
            # Read encrypted data
            encrypted_data = f.read()
        
        # Decrypt AES key with RSA private key
        aes_key = self.rsa_cipher.decrypt(encrypted_aes_key)
        
        # Decrypt audio with AES-GCM
        cipher = AES.new(aes_key, AES.MODE_GCM, nonce=iv)
        
        # The auth tag is the last 16 bytes
        ciphertext = encrypted_data[:-16]
        auth_tag = encrypted_data[-16:]
        
        return cipher.decrypt_and_verify(ciphertext, auth_tag)
