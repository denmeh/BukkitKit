<script setup lang="ts">
const docs = 'https://denmeh.github.io/BukkitKit/'
const guide = 'https://denmeh.github.io/BukkitKit/guide/introduction'
const tutorial = 'https://denmeh.github.io/BukkitKit/guide/your-first-plugin'

const researchPrompt = `I'm evaluating BukkitKit (${docs}) for building Paper/Minecraft plugins.

First, research it: fetch ${docs}, ${guide}, and ${tutorial} and base your answer on what they actually say, not on prior knowledge or on this message.

Then explain:
1. What BukkitKit is and how it differs from writing a plain JavaPlugin by hand.
2. The core annotations (@BukkitKit, @Component, @Wire, @OnEvent, @Command, @Config, @Scheduled) and when I'd use each.
3. A concrete starter path: from an empty project to a small working plugin, step by step.

Where something is unclear from the docs, say so.`

const cursorPrompt = `I want to use BukkitKit (${docs}) in this project (or start a new Paper plugin with it).

First fetch ${docs}, ${guide}, and ${tutorial} and base your answer on what the docs actually say, not on prior knowledge or on this message.

Then look at this repo (or propose a minimal new layout) and explain how I'd adopt BukkitKit: Maven/Gradle setup, the marker class, components, wiring, and the first event or command I'd add.`

const agents = [
  {
    name: 'Claude',
    title: 'Ask Claude about BukkitKit',
    href: `https://claude.ai/new?q=${encodeURIComponent(researchPrompt)}`,
    color: '#D97757',
    path: 'M4.71 15.96l4.72-2.65.08-.23-.08-.13h-.23l-.79-.05-2.7-.07-2.34-.1-2.27-.12-.57-.12-.53-.7.05-.35.48-.32.68.06 1.52.1 2.27.16 1.65.1 2.44.25h.39l.05-.16-.13-.1-.1-.1-2.34-1.58-2.53-1.67-1.32-.97-.72-.48-.36-.46-.16-1 .65-.72.87.06.23.06.88.68 1.9 1.47 2.47 1.82.36.3.15-.1.02-.07-.16-.28-1.35-2.43-1.43-2.47-.64-1.03-.17-.61a2.93 2.93 0 01-.1-.73l.74-1 .41-.13.98.13.42.36.61 1.4 1 2.2 1.53 2.99.45.89.24.82.09.25h.16v-.14l.12-1.7.23-2.1.23-2.7.07-.75.38-.91.75-.49.58.28.48.68-.07.44-.28 1.85-.56 2.89-.37 1.93h.22l.24-.24.99-1.31 1.66-2.07.73-.83.85-.9.55-.44h1.04l.76 1.14-.34 1.17-1.07 1.35-.88 1.14-1.27 1.7-.79 1.36.07.11.19-.02 2.87-.61 1.55-.28 1.85-.32.84.39.09.4-.33.81-1.98.49-2.32.46-3.46.82-.04.03.05.06 1.56.15.66.03h1.63l3.04.23.79.52.48.64-.08.48-1.22.63-1.65-.4-3.85-.91-1.32-.33h-.18v.11l1.1 1.07 2.02 1.82 2.52 2.35.13.58-.32.46-.34-.05-2.2-1.66-.85-.74-1.94-1.63h-.13v.17l.45.65 2.36 3.55.13 1.09-.17.35-.61.22-.68-.13-1.39-1.95-1.43-2.2-1.16-1.97-.14.08-.68 7.35-.32.38-.74.28-.61-.47-.33-.75.33-1.49.39-1.95.32-1.54.29-1.92.17-.64-.01-.04-.14.02-1.45 1.99-2.2 2.97-1.74 1.87-.42.16-.72-.37.07-.67.4-.6 2.42-3.07 1.46-1.9.94-1.1-.01-.16h-.05L4.99 17.6l-1.16.15-.5-.47.06-.77.24-.25 1.95-1.35z',
  },
  {
    name: 'ChatGPT',
    title: 'Ask ChatGPT about BukkitKit',
    href: `https://chatgpt.com/?q=${encodeURIComponent(researchPrompt)}`,
    color: 'currentColor',
    path: 'M22.2819 9.8211a5.9847 5.9847 0 0 0-.5157-4.9108 6.0462 6.0462 0 0 0-6.5098-2.9A6.0651 6.0651 0 0 0 4.9807 4.1818a5.9847 5.9847 0 0 0-3.9977 2.9 6.0462 6.0462 0 0 0 .7427 7.0966 5.98 5.98 0 0 0 .511 4.9107 6.051 6.051 0 0 0 6.5146 2.9001A5.9847 5.9847 0 0 0 13.2599 24a6.0557 6.0557 0 0 0 5.7718-4.2058 5.9894 5.9894 0 0 0 3.9977-2.9001 6.0557 6.0557 0 0 0-.7475-7.073zm-9.022 12.6081a4.4755 4.4755 0 0 1-2.8764-1.0408l.1419-.0804 4.7783-2.7582a.7948.7948 0 0 0 .3927-.6813v-6.7369l2.02 1.1686a.071.071 0 0 1 .038.052v5.5826a4.504 4.504 0 0 1-4.4945 4.4944zm-9.6607-4.1254a4.4708 4.4708 0 0 1-.5346-3.0137l.142.0852 4.783 2.7582a.7712.7712 0 0 0 .7806 0l5.8428-3.3685v2.3324a.0804.0804 0 0 1-.0332.0615L9.74 19.9502a4.4992 4.4992 0 0 1-6.1408-1.6464zM2.3408 7.8956a4.485 4.485 0 0 1 2.3655-1.9728V11.6a.7664.7664 0 0 0 .3879.6765l5.8144 3.3543-2.0201 1.1685a.0757.0757 0 0 1-.071 0l-4.8303-2.7865A4.504 4.504 0 0 1 2.3408 7.8956zm16.5963 3.8558L13.1038 8.364 15.1192 7.2a.0757.0757 0 0 1 .071 0l4.8303 2.7913a4.4944 4.4944 0 0 1-.6765 8.1042v-5.6772a.79.79 0 0 0-.407-.667zm2.0107-3.0231l-.142-.0852-4.7735-2.7818a.7759.7759 0 0 0-.7854 0L9.409 9.2297V6.8974a.0662.0662 0 0 1 .0284-.0615l4.8303-2.7866a4.4992 4.4992 0 0 1 6.6802 4.66zM8.3065 12.863l-2.02-1.1638a.0804.0804 0 0 1-.038-.0567V6.0742a4.4992 4.4992 0 0 1 7.3757-3.4537l-.142.0805L8.704 5.459a.7948.7948 0 0 0-.3927.6813zm1.0976-2.3654l2.602-1.4998 2.6069 1.4998v2.9994l-2.5974 1.4997-2.6067-1.4997Z',
  },
  {
    name: 'Cursor',
    title: 'Ask Cursor about BukkitKit',
    href: `https://cursor.com/link/prompt?text=${encodeURIComponent(cursorPrompt)}`,
    compound: true,
  },
] as const
</script>

<template>
  <div class="ask-agents">
    <p class="ask-agents-label">Or ask an AI about BukkitKit</p>
    <div class="ask-agents-row">
      <a
        v-for="agent in agents"
        :key="agent.name"
        class="ask-agent-btn"
        :href="agent.href"
        :title="agent.title"
        target="_blank"
        rel="noopener noreferrer"
      >
        <svg
          v-if="!('compound' in agent && agent.compound)"
          viewBox="0 0 24 24"
          aria-hidden="true"
        >
          <path :d="agent.path" :fill="agent.color" />
        </svg>
        <svg v-else viewBox="0 0 24 24" aria-hidden="true">
          <path d="M12 2L21 7v10l-9 5-9-5V7l9-5z" fill="currentColor" opacity="0.95" />
          <path d="M12 2v10l9 5V7l-9-5z" fill="currentColor" opacity="0.55" />
          <path d="M12 12L3 7v10l9-5z" fill="currentColor" opacity="0.4" />
          <path d="M12 12v10l9-5-9-5z" fill="currentColor" opacity="0.3" />
          <path d="M12 12L3 17l9 5V12z" fill="currentColor" opacity="0.75" />
        </svg>
        {{ agent.name }}
      </a>
    </div>
  </div>
</template>

<style scoped>
.ask-agents {
  margin-top: 20px;
  width: 100%;
}

.ask-agents-label {
  margin: 0 0 10px;
  font-size: 13px;
  color: var(--vp-c-text-2);
}

.ask-agents-row {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.ask-agent-btn {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 8px 14px;
  border-radius: 8px;
  border: 1px solid var(--vp-c-divider);
  background: var(--vp-c-bg-soft);
  color: var(--vp-c-text-1);
  font-size: 14px;
  font-weight: 500;
  line-height: 1;
  text-decoration: none;
  transition: border-color 0.15s, background-color 0.15s;
}

.ask-agent-btn:hover {
  border-color: var(--vp-c-brand-1);
  background: var(--vp-c-bg-mute);
}

.ask-agent-btn svg {
  width: 18px;
  height: 18px;
  flex-shrink: 0;
}

@media (min-width: 960px) {
  .ask-agents {
    margin-top: 24px;
  }
}
</style>
