#!/usr/bin/env bash
# Generates the RS256 key pair identity-service signs tokens with.
# Output: keys/jwt-private.pem (PKCS#8) and keys/jwt-public.pem (X.509). Never commit the private key.
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p keys
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out keys/jwt-private.pem
openssl rsa -in keys/jwt-private.pem -pubout -out keys/jwt-public.pem
chmod 600 keys/jwt-private.pem
chmod 644 keys/jwt-public.pem
echo "Generated keys/jwt-private.pem and keys/jwt-public.pem"
