# Reminder production-domain rollout design

## Goal

Make the migrated Reminder mini-program backend reachable at
`https://reminder-api.wwmty.com`, and make a production mini-program upload
work without WeChat CloudBase.

## Chosen approach

Deploy Reminder as an independently versioned service on the existing SaaS
Docker host. It will reuse the existing host-local registry, `saas-app`
network, PostgreSQL container, Redis container, and audited saas-admin storage
interface. It will not be added to the saas-admin Compose project or its
release script.

The alternative of extending the saas-admin Compose project would couple
Reminder releases and rollback to saas-admin. A manual Compose-only release
would not leave an auditable Jenkins release history. Both are rejected.

## Components and data flow

1. Tencent Cloud DNS maps `reminder-api.wwmty.com` to the existing SaaS host.
2. The host Nginx terminates TLS and proxies only this hostname to
   `127.0.0.1:18080`.
3. A new Jenkins pipeline checks out the migration branch, tests and packages
   the single Spring Boot service, pushes an immutable Reminder image to the
   live SaaS registry, and deploys only the Reminder Compose service.
4. The container joins `saas-app`, uses PostgreSQL schema `reminder` and Redis
   logical database `9`, and calls saas-admin over the internal network for
   audited storage uploads.
5. Every mini-program request path resolves to
   `https://reminder-api.wwmty.com/api`; no production request helper may keep
   the legacy `api.reminder.com` value.
6. WeChat's request/uploadFile legal-domain list contains the Reminder HTTPS
   origin. Its downloadFile list contains the currently configured HTTPS
   public MinIO origin used by saas-admin.

## Runtime configuration and safety

Jenkins holds all runtime secrets in a Secret file and the existing audited
storage app identity in a separate credential. The repository, image, Compose
file, Jenkins log, and Nginx configuration contain no secrets.

The first rollout creates the `reminder` PostgreSQL schema and Quartz tables
only when the schema is empty. The release script keeps a `600`-permission
snapshot of the prior Reminder image, environment, and Compose configuration
under `/opt/saas-app/reminder/deploy-state`; a failed health check restores only
Reminder.

## Acceptance criteria

- Public HTTPS health endpoint returns healthy with the correct certificate.
- The Reminder container is healthy and attached to `saas-app`.
- Jenkins records a successful immutable-image release and can roll back only
  Reminder.
- Production mini-program configuration has no CloudBase endpoint and no
  legacy production API hostname.
- WeChat legal domains are saved for request and upload; storage download URL
  is HTTPS and whitelisted.
- A real HTTPS login, authenticated API request, and avatar upload succeed;
  the storage audit records the object below `app/reminder/`.

## Explicit rollout order

1. Align the Jenkins pipeline with the live SaaS host deployment pattern.
2. Prepare Jenkins runtime credentials and initialize the isolated database
   schema.
3. Create DNS, issue TLS, and install the isolated Nginx virtual host.
4. Deploy and verify the backend before changing the mini-program domain
   allowlist.
5. Update the mini-program production request configuration and WeChat legal
   domains, then perform the end-to-end request and upload checks.
