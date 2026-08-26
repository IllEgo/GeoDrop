# A4 custom-domain and DNS approval package

Status: **A4a and A4b approved, completed, and verified**.

A4 connects the already verified Firebase Hosting site to the dedicated App Link host
`join.kitheapp.com`. It does not deploy new content, Functions, rules, indexes, Remote
Config, legal policies, a Play release, or a QR distribution.

## Read-only baseline — 2026-08-25 UTC

- Firebase project/site: `kithe-production` / `kithe-production`.
- Current Hosting release remains healthy at `https://kithe-production.web.app`.
- Firebase Hosting lists exactly its two default domains:
  `kithe-production.web.app` and `kithe-production.firebaseapp.com`.
- Firebase lists no custom domain.
- `kitheapp.com` uses the expected two Cloudflare authoritative nameservers and DNSSEC has
  a published DS record.
- Public DNS returns no CNAME, A, or AAAA answer for `join.kitheapp.com` and no TXT answer
  for `_acme-challenge.join.kitheapp.com`.
- The root `kitheapp.com` Cloudflare Pages site, email-routing records, DNSSEC, and every
  unrelated hostname are outside A4 and must remain unchanged.

Firebase does not reveal the exact validation and routing records until the custom-domain
association is created. Therefore A4 must use two separately approved mutations.

## A4a — create the pending Firebase association

After explicit **A4a approval** only:

1. In Firebase Hosting for site `kithe-production`, choose **Add custom domain**.
2. Enter exactly `join.kitheapp.com` as a direct serving domain. Do not configure a redirect.
3. Create only the pending Firebase custom-domain association.
4. Capture the exact DNS record set Firebase returns: record name, type, value/target, and
   any required validation state. Do not create or edit any Cloudflare record.
5. Stop and present that exact record set for A4b approval.

A4a is not approval for DNS. Its rollback is to remove only the pending
`join.kitheapp.com` association if Firebase selects the wrong site/domain, requests an
unexpected apex/redirect, reports a conflict, or cannot produce a bounded record set.

## A4b — apply only the Firebase-requested DNS records

### Exact A4a output — 2026-08-25T17:10:39Z

Firebase Hosting created a direct-serving `join.kitheapp.com` custom-domain association on
site `kithe-production`. Redirect mode was not selected. The association is **Needs setup**
and Firebase Quick setup returned exactly one required record:

| Type | Name | Firebase value | Cloudflare proxy | TTL |
| --- | --- | --- | --- | --- |
| CNAME | `join.kitheapp.com` | `kithe-production.web.app` | DNS only | Auto |

Firebase returned no A, AAAA, or TXT requirement. An independent resolver still returned
zero CNAME answers after A4a, proving no DNS mutation occurred. The generated Firebase entry
page remained HTTP 200 with the safe fixture.

After the owner explicitly approves this exact **A4b** record:

1. Add only the Firebase-requested `join.kitheapp.com` validation/routing records in the
   `kitheapp.com` Cloudflare zone.
2. Use DNS-only mode for Firebase validation/routing records unless Firebase explicitly
   documents a different requirement; never infer or invent an A, AAAA, CNAME, or TXT value.
3. Do not change the root Pages CNAME, MX, SPF, DKIM, Email Routing, nameservers, DNSSEC,
   redirects, page rules, Workers, SSL mode, or unrelated subdomains.
4. Verify the exact public DNS answers through an independent resolver, then wait for
   Firebase to report the custom domain and managed certificate active.
5. Verify all of the following on `https://join.kitheapp.com`:
   - valid HTTPS with no certificate warning;
   - root HTTP 200 and `noindex,nofollow`;
   - unknown route HTTP 404;
   - direct `/.well-known/assetlinks.json` HTTP 200, no redirect, JSON content type, one
     `com.kitheapp` statement, and exact parity with all three approved Play-held signing
     fingerprints;
   - `/e/R5PTEST2` HTTP 200 with the safe preview and Play link;
   - CSP, Permissions Policy, Referrer Policy, nosniff, and frame-denial headers; and
   - no test location, owner identifier, payload, token, or credential exposure.

### A4b completion evidence — 2026-08-25T18:02:13Z

- Before the mutation, Cloudflare showed the six recorded baseline entries and no conflicting
  `join.kitheapp.com` record.
- The owner explicitly approved A4b. Exactly one record was added: CNAME
  `join.kitheapp.com` to `kithe-production.web.app`, DNS only, TTL Auto. The root Pages,
  MX, SPF, DKIM, Email Routing, nameserver, DNSSEC, redirect, Worker, SSL, and unrelated
  hostname settings were not changed.
- Cloudflare's and Google's public recursive resolvers both returned the exact CNAME target
  with a 300-second TTL and DNSSEC-authenticated answers.
- Firebase Hosting reported `join.kitheapp.com` as **Connected** on site
  `kithe-production`.
- Certificate-validated HTTPS passed without bypassing verification. The root returned HTTP
  200 with `noindex,nofollow`; an unknown route returned HTTP 404; and
  `/e/R5PTEST2` returned HTTP 200 with only the approved safe rehearsal preview and Play
  handoff.
- Direct `/.well-known/assetlinks.json` returned HTTP 200 with no redirect and
  `application/json`. It is semantically identical to the tracked file: exactly one
  `com.kitheapp` statement and the same three approved Play-held SHA-256 fingerprints.
- CSP, Permissions Policy, Referrer Policy, HSTS, nosniff, and frame denial were present.
  No test location, owner identifier, drop payload, auth token, or credential was exposed.

A4 is complete. No rollback was required.

If verification fails, remove only the newly added DNS records, keep the generated Firebase
host available, and leave the Android release client fail-closed. Remove the pending Firebase
association only after the DNS rollback is confirmed or if Firebase requires it to clear a
bad binding.

## Abort conditions and remaining gates

Abort on a project/site/domain mismatch; an existing conflicting DNS answer; an unexpected
apex or redirect request; any instruction to proxy through an unrelated service; a change to
root/email/DNSSEC records; certificate or hostname mismatch; redirecting Digital Asset Links;
fingerprint drift; unsafe page content; or any unrelated Firebase/Cloudflare mutation.

A4 does not make the Android app live. A5 still separately gates the release App Bundle,
Play track/listing, legal-policy backend, release-client host/flag, never-installed device
path, and full external funnel matrix.
