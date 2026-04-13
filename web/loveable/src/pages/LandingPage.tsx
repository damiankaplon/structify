import React, {useEffect, useRef} from 'react';
import {Link} from 'react-router-dom';
import heroVideo from '@/assets/Structify_Data_Extraction_Guide.mp4';
import screenshotCreate from '@/assets/screenshot-create.jpg';
import screenshotSchema from '@/assets/screenshot-schema.jpg';
import screenshotExtracted from '@/assets/screenshot-extracted.jpg';

/* ── tiny intersection-observer hook for scroll-triggered animations ── */
function useReveal() {
  const ref = useRef<HTMLDivElement>(null);
  useEffect(() => {
    const el = ref.current;
    if (!el) return;
    const obs = new IntersectionObserver(
      ([e]) => {
        if (e.isIntersecting) {
          el.classList.add('revealed');
          obs.unobserve(el);
        }
      },
      {threshold: 0.15},
    );
    obs.observe(el);
    return () => obs.disconnect();
  }, []);
  return ref;
}

const Reveal: React.FC<{ children: React.ReactNode; className?: string; delay?: string }> = ({
                                                                                               children,
                                                                                               className = '',
                                                                                               delay = '0ms',
                                                                                             }) => {
  const ref = useReveal();
  return (
    <div
      ref={ref}
      className={`reveal-block ${className}`}
      style={{transitionDelay: delay}}
    >
      {children}
    </div>
  );
};

/* ── Section components ── */

const Hero = () => (
  <section className="relative overflow-hidden pb-20 pt-28 sm:pt-36 lg:pt-44">
    {/* gradient bg blobs */}
    <div className="pointer-events-none absolute -top-40 left-1/2 -translate-x-1/2">
      <div className="h-[600px] w-[900px] rounded-full bg-primary/10 blur-[120px]"/>
    </div>
    <div className="pointer-events-none absolute -bottom-32 right-0">
      <div className="h-[400px] w-[400px] rounded-full bg-accent/10 blur-[100px]"/>
    </div>

    <div className="relative mx-auto max-w-5xl px-6 text-center">
      <Reveal>
        <span
          className="mb-4 inline-block rounded-full border border-primary/20 bg-primary/5 px-4 py-1.5 text-xs font-semibold uppercase tracking-wider text-primary">
          AI-Powered Data Extraction
        </span>
      </Reveal>

      <Reveal delay="80ms">
        <h1
          className="mx-auto max-w-3xl text-4xl font-extrabold leading-tight tracking-tight text-foreground sm:text-5xl lg:text-6xl">
          Turn <span className="text-primary">any document</span> into structured data
        </h1>
      </Reveal>

      <Reveal delay="160ms">
        <p className="mx-auto mt-6 max-w-2xl text-lg leading-relaxed text-muted-foreground sm:text-xl">
          Structify enables you to collect data from any source effortlessly. Define your table,
          upload your file, and let AI handle the rest while you focus on what matters.
        </p>
      </Reveal>

      <Reveal delay="240ms">
        <div className="mt-10 flex flex-wrap items-center justify-center gap-4">
          <Link
            to="/app"
            className="inline-flex h-12 items-center gap-2 rounded-xl bg-primary px-8 text-sm font-semibold text-primary-foreground shadow-lg shadow-primary/25 transition-all hover:shadow-xl hover:shadow-primary/30 hover:-translate-y-0.5"
          >
            Get Started — It's Free
            <svg className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
              <path strokeLinecap="round" strokeLinejoin="round" d="M13 7l5 5m0 0l-5 5m5-5H6"/>
            </svg>
          </Link>
          <a
            href="#how-it-works"
            className="inline-flex h-12 items-center rounded-xl border border-border bg-card px-8 text-sm font-semibold text-foreground transition-colors hover:bg-secondary"
          >
            See How It Works
          </a>
        </div>
      </Reveal>

      {/* hero screenshot */}
      <Reveal delay="350ms" className="mt-16">
        <div
          className="relative mx-auto max-w-4xl overflow-hidden rounded-2xl border border-border bg-card shadow-2xl shadow-primary/5">
          <video
            src={heroVideo}
            className="w-full"
            autoPlay
            loop
            playsInline
            controls
            width={1280}
            height={720}
          />
          <div className="pointer-events-none absolute inset-0 rounded-2xl ring-1 ring-inset ring-foreground/5"/>
        </div>
      </Reveal>
    </div>
  </section>
);

const FEATURES = [
  {
    icon: (
      <svg className="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={1.5}>
        <path strokeLinecap="round" strokeLinejoin="round"
              d="M3.375 19.5h17.25m-17.25 0a1.125 1.125 0 01-1.125-1.125M3.375 19.5h7.5c.621 0 1.125-.504 1.125-1.125m-9.75 0V5.625m0 12.75v-1.5c0-.621.504-1.125 1.125-1.125m18.375 2.625V5.625m0 12.75c0 .621-.504 1.125-1.125 1.125m1.125-1.125v-1.5c0-.621-.504-1.125-1.125-1.125m0 3.75h-7.5A1.125 1.125 0 0112 18.375m9.75-12.75c0-.621-.504-1.125-1.125-1.125H3.375c-.621 0-1.125.504-1.125 1.125m19.5 0v1.5c0 .621-.504 1.125-1.125 1.125M2.25 5.625v1.5c0 .621.504 1.125 1.125 1.125m0 0h17.25m-17.25 0h7.5c.621 0 1.125.504 1.125 1.125M3.375 8.25c-.621 0-1.125.504-1.125 1.125v1.5c0 .621.504 1.125 1.125 1.125m17.25-3.75h-7.5c-.621 0-1.125.504-1.125 1.125m8.625-1.125c.621 0 1.125.504 1.125 1.125v1.5c0 .621-.504 1.125-1.125 1.125m-17.25 0h7.5m-7.5 0c-.621 0-1.125.504-1.125 1.125v1.5c0 .621.504 1.125 1.125 1.125M12 10.875v-1.5m0 1.5c0 .621-.504 1.125-1.125 1.125M12 10.875c0 .621.504 1.125 1.125 1.125m-2.25 0c.621 0 1.125.504 1.125 1.125M12 12h7.5m-7.5 0c-.621 0-1.125.504-1.125 1.125M21.375 12c.621 0 1.125.504 1.125 1.125v1.5c0 .621-.504 1.125-1.125 1.125M12 17.25v-5.625m0 5.625c0 .621.504 1.125 1.125 1.125h2.25c.621 0 1.125-.504 1.125-1.125v-1.5c0-.621-.504-1.125-1.125-1.125H12m0 0h-2.25"/>
      </svg>
    ),
    title: 'Flexible Schemas',
    desc: 'Define exactly the structure you need — from flat tables to deeply nested objects. Be broad or specific.',
  },
  {
    icon: (
      <svg className="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={1.5}>
        <path strokeLinecap="round" strokeLinejoin="round"
              d="M19.5 14.25v-2.625a3.375 3.375 0 00-3.375-3.375h-1.5A1.125 1.125 0 0113.5 7.125v-1.5a3.375 3.375 0 00-3.375-3.375H8.25m6.75 12H9.75m2.25-2.25L9.75 15m2.25 0l2.25 2.25M6.375 19.5a2.625 2.625 0 01-2.625-2.625V6.75a2.625 2.625 0 012.625-2.625h4.872a2.625 2.625 0 011.856.77l3.498 3.497a2.625 2.625 0 01.769 1.856V16.875a2.625 2.625 0 01-2.625 2.625H6.375z"/>
      </svg>
    ),
    title: 'PDF Upload & Extract',
    desc: 'Upload any PDF — invoices, contracts, job posts — and let AI extract structured rows automatically.',
  },
  {
    icon: (
      <svg className="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={1.5}>
        <path strokeLinecap="round" strokeLinejoin="round"
              d="M16.023 9.348h4.992v-.001M2.985 19.644v-4.992m0 0h4.992m-4.993 0l3.181 3.183a8.25 8.25 0 0013.803-3.7M4.031 9.865a8.25 8.25 0 0113.803-3.7l3.181 3.182M21.014 4.356v4.992"/>
      </svg>
    ),
    title: 'Version History',
    desc: 'Restore any previously defined structure without data loss. Iterate on your schema with confidence.',
  },
  {
    icon: (
      <svg className="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={1.5}>
        <path strokeLinecap="round" strokeLinejoin="round"
              d="M9.813 15.904L9 18.75l-.813-2.846a4.5 4.5 0 00-3.09-3.09L2.25 12l2.846-.813a4.5 4.5 0 003.09-3.09L9 5.25l.813 2.846a4.5 4.5 0 003.09 3.09L15.75 12l-2.846.813a4.5 4.5 0 00-3.09 3.09zM18.259 8.715L18 9.75l-.259-1.035a3.375 3.375 0 00-2.455-2.456L14.25 6l1.036-.259a3.375 3.375 0 002.455-2.456L18 2.25l.259 1.035a3.375 3.375 0 002.455 2.456L21.75 6l-1.036.259a3.375 3.375 0 00-2.455 2.456zM16.894 20.567L16.5 21.75l-.394-1.183a2.25 2.25 0 00-1.423-1.423L13.5 18.75l1.183-.394a2.25 2.25 0 001.423-1.423l.394-1.183.394 1.183a2.25 2.25 0 001.423 1.423l1.183.394-1.183.394a2.25 2.25 0 00-1.423 1.423z"/>
      </svg>
    ),
    title: 'AI-Powered',
    desc: 'Powered by OpenAI, Structify understands context and nuance to extract the right data every time.',
  },
];

const Features = () => (
  <section id="features" className="border-t border-border bg-card py-24">
    <div className="mx-auto max-w-5xl px-6">
      <Reveal>
        <p className="text-center text-sm font-semibold uppercase tracking-wider text-primary">Features</p>
        <h2 className="mt-2 text-center text-3xl font-bold text-foreground sm:text-4xl">
          Everything you need to extract data
        </h2>
      </Reveal>

      <div className="mt-16 grid gap-8 sm:grid-cols-2">
        {FEATURES.map((f, i) => (
          <Reveal key={f.title} delay={`${i * 100}ms`}>
            <div
              className="group rounded-2xl border border-border bg-background p-6 transition-all hover:border-primary/30 hover:shadow-lg hover:shadow-primary/5 hover:-translate-y-1">
              <div
                className="mb-4 flex h-11 w-11 items-center justify-center rounded-xl bg-primary/10 text-primary transition-colors group-hover:bg-primary group-hover:text-primary-foreground">
                {f.icon}
              </div>
              <h3 className="text-lg font-semibold text-foreground">{f.title}</h3>
              <p className="mt-2 text-sm leading-relaxed text-muted-foreground">{f.desc}</p>
            </div>
          </Reveal>
        ))}
      </div>
    </div>
  </section>
);

const STEPS = [
  {
    num: '01',
    title: 'Define your table',
    desc: 'Create a table and describe what data it stores — invoices, job offers, songs, anything.',
    img: screenshotCreate
  },
  {
    num: '02',
    title: 'Build the schema',
    desc: 'Add columns with types, descriptions, and nested objects. Be as detailed as you want — it helps AI extract better.',
    img: screenshotSchema
  },
  {
    num: '03',
    title: 'Upload & extract',
    desc: 'Upload a PDF and let Structify extract a structured row automatically. Review, edit, and export.',
    img: screenshotExtracted
  },
];

const HowItWorks = () => (
  <section id="how-it-works" className="py-24">
    <div className="mx-auto max-w-5xl px-6">
      <Reveal>
        <p className="text-center text-sm font-semibold uppercase tracking-wider text-primary">How it works</p>
        <h2 className="mt-2 text-center text-3xl font-bold text-foreground sm:text-4xl">
          Three simple steps
        </h2>
      </Reveal>

      <div className="mt-20 space-y-24">
        {STEPS.map((s, i) => (
          <div
            key={s.num}
            className={`flex flex-col items-center gap-12 lg:flex-row ${i % 2 === 1 ? 'lg:flex-row-reverse' : ''}`}
          >
            <Reveal className="flex-1">
              <span className="text-5xl font-black text-primary/15">{s.num}</span>
              <h3 className="mt-2 text-2xl font-bold text-foreground">{s.title}</h3>
              <p className="mt-3 max-w-md text-muted-foreground leading-relaxed">{s.desc}</p>
            </Reveal>
            <Reveal delay="150ms" className="flex-1">
              <div className="overflow-hidden rounded-2xl border border-border shadow-xl shadow-primary/5">
                <img src={s.img} alt={s.title} className="w-full" loading="lazy" width={1280} height={720}/>
              </div>
            </Reveal>
          </div>
        ))}
      </div>
    </div>
  </section>
);

const UseCases = () => (
  <section className="border-t border-border bg-card py-24">
    <div className="mx-auto max-w-5xl px-6 text-center">
      <Reveal>
        <p className="text-sm font-semibold uppercase tracking-wider text-primary">Use cases</p>
        <h2 className="mt-2 text-3xl font-bold text-foreground sm:text-4xl">Works with any document type</h2>
      </Reveal>

      <div className="mt-14 grid gap-6 sm:grid-cols-3">
        {[
          {
            emoji: '🧾',
            label: 'Invoices & Receipts',
            desc: 'Extract amounts, vendors, dates, line items automatically.'
          },
          {emoji: '💼', label: 'Job Postings', desc: 'Pull salaries, requirements, company info from any listing.'},
          {emoji: '📝', label: 'Business Memos', desc: 'Structure meeting notes, decisions, and action items.'},
          {emoji: '📄', label: 'Contracts', desc: 'Extract parties, terms, dates, and obligations.'},
          {emoji: '🎵', label: 'Song Lyrics', desc: 'Catalog titles, artists, genres, and themes.'},
          {emoji: '📊', label: 'Reports', desc: 'Pull KPIs, figures, and summaries from PDF reports.'},
        ].map((uc, i) => (
          <Reveal key={uc.label} delay={`${i * 80}ms`}>
            <div
              className="rounded-2xl border border-border bg-background p-6 text-left transition-all hover:border-primary/20 hover:shadow-md">
              <span className="text-3xl">{uc.emoji}</span>
              <h3 className="mt-3 font-semibold text-foreground">{uc.label}</h3>
              <p className="mt-1 text-sm text-muted-foreground">{uc.desc}</p>
            </div>
          </Reveal>
        ))}
      </div>
    </div>
  </section>
);

const CTA = () => (
  <section className="py-24">
    <div className="mx-auto max-w-3xl px-6 text-center">
      <Reveal>
        <div
          className="relative overflow-hidden rounded-3xl bg-primary px-8 py-16 shadow-2xl shadow-primary/25 sm:px-16">
          <div
            className="pointer-events-none absolute -right-16 -top-16 h-64 w-64 rounded-full bg-primary-foreground/10 blur-3xl"/>
          <div
            className="pointer-events-none absolute -bottom-20 -left-20 h-64 w-64 rounded-full bg-accent/20 blur-3xl"/>
          <h2 className="relative text-3xl font-bold text-primary-foreground sm:text-4xl">
            Ready to structure your data?
          </h2>
          <p className="relative mt-4 text-primary-foreground/80">
            Start extracting structured data from your documents in minutes — no code required.
          </p>
          <Link
            to="/app"
            className="relative mt-8 inline-flex h-12 items-center gap-2 rounded-xl bg-primary-foreground px-8 text-sm font-semibold text-primary shadow-lg transition-all hover:-translate-y-0.5 hover:shadow-xl"
          >
            Launch Structify
            <svg className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
              <path strokeLinecap="round" strokeLinejoin="round" d="M13 7l5 5m0 0l-5 5m5-5H6"/>
            </svg>
          </Link>
        </div>
      </Reveal>
    </div>
  </section>
);

const Footer = () => (
  <footer className="border-t border-border py-10">
    <div className="mx-auto flex max-w-5xl flex-col items-center gap-4 px-6 sm:flex-row sm:justify-between">
      <div className="flex items-center gap-2">
        <div className="flex h-7 w-7 items-center justify-center rounded-lg bg-primary">
          <span className="text-xs font-bold text-primary-foreground">S</span>
        </div>
        <span className="font-semibold text-foreground">Structify</span>
      </div>
      <p className="text-xs text-muted-foreground">&copy; {new Date().getFullYear()} Structify. All rights reserved.</p>
    </div>
  </footer>
);

/* ── Nav ── */
const LandingNav = () => (
  <header className="fixed inset-x-0 top-0 z-50 border-b border-border/50 bg-background/80 backdrop-blur-lg">
    <div className="mx-auto flex h-14 max-w-5xl items-center justify-between px-6">
      <div className="flex items-center gap-2">
        <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-primary">
          <span className="text-sm font-bold text-primary-foreground">S</span>
        </div>
        <span className="text-lg font-semibold text-foreground">Structify</span>
      </div>
      <nav className="flex items-center gap-6">
        <a href="#features"
           className="hidden text-sm text-muted-foreground transition-colors hover:text-foreground sm:block">Features</a>
        <a href="#how-it-works"
           className="hidden text-sm text-muted-foreground transition-colors hover:text-foreground sm:block">How it
          works</a>
        <Link to="/app"
              className="rounded-lg bg-primary px-4 py-2 text-xs font-semibold text-primary-foreground transition-colors hover:bg-primary/90">
          Launch App
        </Link>
      </nav>
    </div>
  </header>
);

/* ── Page ── */
const LandingPage: React.FC = () => (
  <div className="min-h-screen bg-background">
    <LandingNav/>
    <Hero/>
    <Features/>
    <HowItWorks/>
    <UseCases/>
    <CTA/>
    <Footer/>
  </div>
);

export default LandingPage;
