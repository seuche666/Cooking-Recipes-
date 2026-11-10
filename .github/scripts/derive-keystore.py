#!/usr/bin/env python3
"""Leitet den Signierschlüssel der App deterministisch aus SIGNING_PASSWORD ab.

Damit muss kein Keystore als Secret hinterlegt oder vom Handy kopiert werden: Es genügt das
Secret SIGNING_PASSWORD. Aus demselben Passwort entsteht bei jedem Lauf exakt derselbe Schlüssel
und dasselbe Zertifikat. Das ist nötig, damit Android neue Versionen als Update akzeptiert.

- Privater Schlüssel: scrypt(Passwort) → EC P-256. Nichts davon wird gespeichert oder geloggt.
- Zertifikat: feste Felder (Name, Seriennummer, Gültigkeit) und deterministische ECDSA-Signatur
  (RFC 6979), dadurch Byte für Byte reproduzierbar.

NICHTS AN DEN KONSTANTEN UNTEN ÄNDERN: Jede Änderung ergibt einen anderen Schlüssel, und bereits
installierte Versionen ließen sich nicht mehr aktualisieren.

Aufruf: SIGNING_PASSWORD=… derive-keystore.py <ziel.p12>
Ausgabe: SHA-256-Fingerabdruck des Zertifikats (öffentlich, steckt ohnehin in jeder APK).
"""
import datetime
import hashlib
import os
import sys

from cryptography import x509
from cryptography.hazmat.primitives import hashes, serialization
from cryptography.hazmat.primitives.asymmetric import ec
from cryptography.hazmat.primitives.kdf.scrypt import Scrypt
from cryptography.hazmat.primitives.serialization import pkcs12
from cryptography.x509.oid import NameOID

SALT = b"de.rezeptbuch.app/release-signing/v1"
ALIAS = "rezeptbuch"
COMMON_NAME = "Rezeptbuch"
SERIAL = 1
NOT_BEFORE = datetime.datetime(2026, 1, 1, tzinfo=datetime.timezone.utc)
NOT_AFTER = datetime.datetime(2076, 1, 1, tzinfo=datetime.timezone.utc)
# Ordnung der Kurve P-256
P256_ORDER = 0xFFFFFFFF00000000FFFFFFFFFFFFFFFFBCE6FAADA7179E84F3B9CAC2FC632551
MIN_PASSWORD_LENGTH = 16


def main() -> int:
    password = os.environ.get("SIGNING_PASSWORD", "")
    if len(password) < MIN_PASSWORD_LENGTH:
        print(
            f"::error::SIGNING_PASSWORD fehlt oder ist kürzer als {MIN_PASSWORD_LENGTH} Zeichen.",
            file=sys.stderr,
        )
        return 1
    target = sys.argv[1]

    # Langsame Schlüsselableitung (scrypt, 128 MiB) erschwert das Durchprobieren von Passwörtern.
    seed = Scrypt(salt=SALT, length=48, n=2**17, r=8, p=1).derive(password.encode("utf-8"))
    secret = int.from_bytes(seed, "big") % (P256_ORDER - 1) + 1
    key = ec.derive_private_key(secret, ec.SECP256R1())

    name = x509.Name([x509.NameAttribute(NameOID.COMMON_NAME, COMMON_NAME)])
    cert = (
        x509.CertificateBuilder()
        .subject_name(name)
        .issuer_name(name)
        .public_key(key.public_key())
        .serial_number(SERIAL)
        .not_valid_before(NOT_BEFORE)
        .not_valid_after(NOT_AFTER)
        .sign(key, hashes.SHA256(), ecdsa_deterministic=True)
    )

    encryption = (
        serialization.PrivateFormat.PKCS12.encryption_builder()
        .kdf_rounds(50_000)
        .key_cert_algorithm(pkcs12.PBES.PBESv2SHA256AndAES256CBC)
        .hmac_hash(hashes.SHA256())
        .build(password.encode("utf-8"))
    )
    data = pkcs12.serialize_key_and_certificates(ALIAS.encode(), key, cert, None, encryption)
    with open(target, "wb") as f:
        f.write(data)

    fingerprint = hashlib.sha256(cert.public_bytes(serialization.Encoding.DER)).hexdigest()
    print(":".join(fingerprint[i : i + 2] for i in range(0, len(fingerprint), 2)).upper())
    return 0


if __name__ == "__main__":
    sys.exit(main())
