export interface ExperienceEntryPreview {
  code: string;
  name: string;
  description: string | null;
  hostLabel: string;
  availability: string;
  availableDropCount: number;
}

const escapeHtml = (value: unknown): string => String(value ?? "")
  .replace(/&/g, "&amp;")
  .replace(/</g, "&lt;")
  .replace(/>/g, "&gt;")
  .replace(/"/g, "&quot;")
  .replace(/'/g, "&#39;");

const displayCode = (code: string): string => code.length === 8 ?
  `${code.slice(0, 4)}-${code.slice(4)}` :
  code;

const availabilityContent = (availability: string): {
  eyebrow: string;
  title: string;
  detail: string;
  action: string | null;
} => {
  if (availability === "UPCOMING") {
    return {
      eyebrow: "STARTS SOON",
      title: "Get ready to explore",
      detail: "You can install Kithe now. The drops will appear when this Experience starts.",
      action: "Get Kithe before it starts",
    };
  }
  if (availability === "ENDED") {
    return {
      eyebrow: "THIS ONE'S CLOSED",
      title: "This Experience has ended",
      detail: "The host has closed this Experience. Ask them if you expected it to still be active.",
      action: null,
    };
  }
  return {
    eyebrow: "READY TO EXPLORE",
    title: "Hidden drops are waiting",
    detail: "Walk around the venue and open what your host left there.",
    action: "Get Kithe and start exploring",
  };
};

export const renderExperienceEntryPage = (
  preview: ExperienceEntryPreview,
  playUrl: string
): string => {
  const name = escapeHtml(preview.name || preview.code);
  const host = escapeHtml(preview.hostLabel || "Host");
  const description = escapeHtml(preview.description ||
    "A location-based Experience made for the people who are here.");
  const code = escapeHtml(displayCode(preview.code));
  const dropCount = Number.isFinite(preview.availableDropCount) ?
    Math.max(0, Math.floor(preview.availableDropCount)) : 0;
  const dropLabel = `${dropCount} ${dropCount === 1 ? "drop" : "drops"}`;
  const state = availabilityContent(preview.availability);
  const action = state.action ?
    `<a class="primary-action" href="${escapeHtml(playUrl)}">${escapeHtml(state.action)}</a>` :
    "<p class=\"closed-note\">There is nothing you need to install for this closed Experience.</p>";

  return `<!doctype html>
<html lang="en">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width,initial-scale=1">
  <meta name="robots" content="noindex,nofollow">
  <meta name="theme-color" content="#0b5d5d">
  <title>${name} &middot; Kithe</title>
  <style>
    :root{color-scheme:light;--teal:#0b5d5d;--teal-dark:#064747;--cream:#fff8ec;--ink:#132322;--muted:#526563;--amber:#e07b24;--card:#fff;--line:#c8d8d5}
    *{box-sizing:border-box}body{margin:0;background:linear-gradient(165deg,#e0f1ee 0,#fff8ec 48%,#fff 100%);color:var(--ink);font:17px/1.55 system-ui,-apple-system,BlinkMacSystemFont,"Segoe UI",sans-serif;min-height:100vh}
    main{width:min(100% - 2rem,42rem);margin:0 auto;padding:2rem 0 3rem}.brand{display:flex;align-items:center;gap:.65rem;color:var(--teal);font-weight:850;letter-spacing:.15em}.mark{display:grid;place-items:center;width:2.4rem;height:2.4rem;border-radius:.8rem;background:var(--teal);color:#fff;font-size:1.35rem;letter-spacing:0}.brand-dot{width:.45rem;height:.45rem;border-radius:50%;background:var(--amber)}
    article{margin-top:2rem;background:rgba(255,255,255,.92);border:1px solid rgba(11,93,93,.16);border-radius:1.5rem;padding:clamp(1.25rem,5vw,2.25rem);box-shadow:0 1.25rem 3rem rgba(11,70,70,.12)}.eyebrow{margin:0;color:var(--teal);font-size:.82rem;font-weight:800;letter-spacing:.12em}h1{font-size:clamp(2rem,8vw,3.25rem);line-height:1.08;letter-spacing:-.035em;margin:.45rem 0 .75rem}.meta{color:var(--muted);margin:0 0 1.5rem}.description{font-size:1.12rem;margin:0 0 1.75rem}
    .entry-card{background:var(--cream);border-radius:1rem;padding:1.15rem;margin:0 0 1.5rem}.entry-card h2{font-size:1.12rem;margin:0 0 .35rem}.entry-card p{margin:0}.code-row{display:flex;align-items:center;justify-content:space-between;gap:1rem;margin:1rem 0 0;padding-top:1rem;border-top:1px solid var(--line)}.code-label{color:var(--muted);font-size:.9rem}.code{font:800 1.25rem/1.2 ui-monospace,SFMono-Regular,Consolas,monospace;letter-spacing:.08em;color:var(--teal-dark)}
    .primary-action{display:block;text-align:center;padding:1rem 1.2rem;border-radius:.9rem;background:var(--teal);color:#fff;text-decoration:none;font-weight:800;min-height:3.5rem}.primary-action:hover{background:var(--teal-dark)}.primary-action:focus-visible{outline:4px solid var(--amber);outline-offset:3px}.closed-note{padding:1rem;border:1px solid var(--line);border-radius:.9rem;text-align:center;font-weight:700}.privacy{color:var(--muted);font-size:.92rem;margin:1rem 0 0;text-align:center}
    @media (prefers-reduced-motion:no-preference){.primary-action{transition:background-color .16s ease,transform .16s ease}.primary-action:hover{transform:translateY(-1px)}}
  </style>
</head>
<body>
  <main>
    <header class="brand" aria-label="Kithe"><span class="mark" aria-hidden="true">K</span><span>KITHE</span><span class="brand-dot" aria-hidden="true"></span></header>
    <article>
      <p class="eyebrow">${escapeHtml(state.eyebrow)}</p>
      <h1>${name}</h1>
      <p class="meta">Hosted by ${host} &middot; ${dropLabel}</p>
      <p class="description">${description}</p>
      <section class="entry-card" aria-labelledby="entry-heading">
        <h2 id="entry-heading">${escapeHtml(state.title)}</h2>
        <p>${escapeHtml(state.detail)}</p>
        <div class="code-row"><span class="code-label">Experience code</span><span class="code">${code}</span></div>
      </section>
      ${action}
      <p class="privacy">Browse as a guest. Kithe asks you to sign in only when you try to unlock.</p>
    </article>
  </main>
</body>
</html>`;
};

export const renderExperienceEntryNotFound = (): string => `<!doctype html>
<html lang="en">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width,initial-scale=1">
  <meta name="robots" content="noindex,nofollow">
  <meta name="theme-color" content="#0b5d5d">
  <title>Experience not found &middot; Kithe</title>
  <style>*{box-sizing:border-box}body{margin:0;background:#fff8ec;color:#132322;font:17px/1.55 system-ui,sans-serif;min-height:100vh;display:grid;place-items:center}main{width:min(100% - 2rem,36rem);background:#fff;border:1px solid #c8d8d5;border-radius:1.5rem;padding:2rem}small{color:#0b5d5d;font-weight:800;letter-spacing:.12em}h1{font-size:2.2rem;line-height:1.1;margin:.5rem 0 1rem}p{margin:0}</style>
</head>
<body><main><small>KITHE EXPERIENCE</small><h1>We couldn't find that Experience</h1><p>Check the code on the invitation or ask the host for a current one.</p></main></body>
</html>`;
