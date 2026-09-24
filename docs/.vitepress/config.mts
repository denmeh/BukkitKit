import { defineConfig } from 'vitepress'

export default defineConfig({
  title: 'BukkitKit',
  description: 'Build Paper plugins with compile-time dependency injection',
  // GitHub Pages project site: https://denmeh.github.io/BukkitKit/
  // Local `just docs` keeps base at `/` unless DOCS_BASE is set.
  base: process.env.DOCS_BASE || '/',
  cleanUrls: true,

  themeConfig: {
    nav: [
      { text: 'Guide', link: '/guide/introduction' },
      { text: 'Tutorial', link: '/guide/your-first-plugin' },
    ],

    sidebar: [
      {
        text: 'Start here',
        items: [
          { text: 'Introduction', link: '/guide/introduction' },
          { text: 'Setup', link: '/guide/setup' },
        ],
      },
      {
        text: 'Tutorial',
        items: [
          { text: '1. Your first plugin', link: '/guide/your-first-plugin' },
          { text: '2. Components', link: '/guide/components' },
          { text: '3. Wiring dependencies', link: '/guide/wiring' },
          { text: '4. Built-in services', link: '/guide/built-ins' },
          { text: '5. Events', link: '/guide/events' },
          { text: '6. Lifecycle', link: '/guide/lifecycle' },
          { text: '7. Scheduled tasks', link: '/guide/scheduling' },
          { text: '8. Commands', link: '/guide/commands' },
          { text: '9. Plugin metadata', link: '/guide/plugin-yml' },
        ],
      },
      {
        text: 'Reference',
        items: [
          { text: 'Annotation cheat sheet', link: '/guide/cheat-sheet' },
        ],
      },
    ],

    socialLinks: [
      { icon: 'github', link: 'https://github.com/denmeh/BukkitKit' },
    ],

    search: {
      provider: 'local',
    },

    editLink: {
      pattern: 'https://github.com/denmeh/BukkitKit/edit/master/docs/:path',
      text: 'Edit this page',
    },

    footer: {
      message: 'An API for building Paper plugins',
      copyright: 'Copyright © denmeh',
    },
  },
})
