#!/bin/bash
# Generates a self-signed certificate for LOCAL DEVELOPMENT ONLY.
# Browsers will show a "not trusted" warning - that's expected for a self-signed cert.
# For a real deployment, replace nginx/certs/{fullchain.pem,privkey.pem} with a certificate
# from a real CA (e.g. Let's Encrypt via certbot) instead of running this script.
set -e

CERT_DIR="$(dirname "$0")/nginx/certs"
mkdir -p "$CERT_DIR"

openssl req -x509 -nodes -days 365 -newkey rsa:2048 \
  -keyout "$CERT_DIR/privkey.pem" \
  -out "$CERT_DIR/fullchain.pem" \
  -subj "/CN=localhost" \
  -addext "subjectAltName=DNS:localhost,IP:127.0.0.1"

chmod 600 "$CERT_DIR/privkey.pem"
echo "Self-signed dev certificate written to $CERT_DIR (valid 365 days)."
echo "Replace with a real CA certificate before exposing this to real users."
