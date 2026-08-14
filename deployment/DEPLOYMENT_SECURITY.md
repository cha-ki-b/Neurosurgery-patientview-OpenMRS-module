# Deployment security hardening

These files update your `openmrs-orthanc-integration` infrastructure repo (the one with
`openmrs-docker-compose.yml` / `orthanc-docker-compose.yml`) to add HTTPS and remove
hardcoded credentials. They don't touch the `neuro-patientview` module itself.

## What changed and why

1. **HTTPS everywhere** (`nginx-docker-compose.yml`, `nginx/nginx.conf`): a single nginx
   reverse proxy now terminates TLS and is the only externally-facing HTTP(S) service.
   OpenMRS (8080) and the Orthanc REST/Explorer interface (8042) are rebound to
   `127.0.0.1` only, so they're no longer reachable directly from outside the host -
   all browser traffic (including login credentials and clinical data) is encrypted.
   The DICOM port (4242) stays open on all interfaces since real scanners need to reach
   it directly; restrict that at your firewall/security-group to known modality IPs.

2. **No more hardcoded passwords** (`.env.example`, updated compose files): every
   password is now `${VAR:?...}` - Docker Compose will refuse to start with a clear error
   if you haven't set it, instead of silently using a weak default. Copy `.env.example`
   to `.env`, fill in strong random values (`openssl rand -base64 24`), and never commit
   `.env` (already in `.gitignore`).

3. **Shared private network**: since the three compose files are started independently,
   they need an explicit shared Docker network to reach each other and to be reachable
   by nginx. Create it once: `docker network create openmrs_neuro_network`.

## Applying this

1. Replace your existing `openmrs-docker-compose.yml` and `orthanc-docker-compose.yml`
   with the versions here (or diff them and merge manually if you've customized yours).
2. `docker network create openmrs_neuro_network`
3. `cp .env.example .env` and fill in real values.
4. `./generate-dev-cert.sh` for a local self-signed certificate (browsers will warn
   it's untrusted - that's expected for dev). For a real deployment, put a certificate
   from a real CA (e.g. Let's Encrypt) at `nginx/certs/fullchain.pem` /
   `nginx/certs/privkey.pem` instead.
5. Bring everything up:
   ```
   docker compose --env-file .env -f openmrs-docker-compose.yml up -d
   docker compose --env-file .env -f orthanc-docker-compose.yml up -d
   docker compose -f nginx-docker-compose.yml up -d
   ```
6. Visit `https://localhost/openmrs/` and `https://localhost/orthanc/`.

## Known residual risks (being transparent about what this does *not* cover)

- **CSRF protection**: the module's `.form` POST endpoints rely on OpenMRS's
  session-based auth but don't carry a CSRF token, matching the rest of the legacy
  OpenMRS UI framework's module conventions. This is a pre-existing ecosystem-wide gap,
  not something introduced by this module - flagging it so it's a known, tracked risk
  rather than a silent one.
- **DICOM traffic itself is not encrypted** (only the HTTP(S)/REST layer is). Orthanc
  supports DICOM TLS (`DicomTlsEnabled`) if your imaging modalities support it; that's a
  separate, more involved configuration step not included here.
- Rotate the generated passwords periodically and restrict who has access to the `.env`
  file and the private key - both are as sensitive as the data they protect.
