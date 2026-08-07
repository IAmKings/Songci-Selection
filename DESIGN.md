---
name: Classical Manuscript
colors:
  surface: '#fbf9f2'
  surface-dim: '#dbdad3'
  surface-bright: '#fbf9f2'
  surface-container-lowest: '#ffffff'
  surface-container-low: '#f5f4ed'
  surface-container: '#efeee7'
  surface-container-high: '#e9e8e1'
  surface-container-highest: '#e3e3dc'
  on-surface: '#1b1c18'
  on-surface-variant: '#44474e'
  inverse-surface: '#30312c'
  inverse-on-surface: '#f2f1ea'
  outline: '#74777f'
  outline-variant: '#c4c6cf'
  surface-tint: '#465f88'
  primary: '#002046'
  on-primary: '#ffffff'
  primary-container: '#1b365d'
  on-primary-container: '#87a0cd'
  inverse-primary: '#aec7f7'
  secondary: '#605e59'
  on-secondary: '#ffffff'
  secondary-container: '#e6e2db'
  on-secondary-container: '#66645f'
  tertiary: '#212119'
  on-tertiary: '#ffffff'
  tertiary-container: '#37362d'
  on-tertiary-container: '#a19f93'
  error: '#ba1a1a'
  on-error: '#ffffff'
  error-container: '#ffdad6'
  on-error-container: '#93000a'
  primary-fixed: '#d6e3ff'
  primary-fixed-dim: '#aec7f7'
  on-primary-fixed: '#001b3d'
  on-primary-fixed-variant: '#2e476f'
  secondary-fixed: '#e6e2db'
  secondary-fixed-dim: '#cac6bf'
  on-secondary-fixed: '#1d1c17'
  on-secondary-fixed-variant: '#484742'
  tertiary-fixed: '#e6e3d5'
  tertiary-fixed-dim: '#c9c7ba'
  on-tertiary-fixed: '#1c1c14'
  on-tertiary-fixed-variant: '#48473d'
  background: '#fbf9f2'
  on-background: '#1b1c18'
  surface-variant: '#e3e3dc'
  near-black: '#141413'
  stone: '#6B6A64'
  warm-sand: '#E8E6DC'
  backdrop: '#3D3D3A'
typography:
  display-title:
    fontFamily: notoSerif
    fontSize: 46px
    fontWeight: '500'
    lineHeight: '1.2'
    letterSpacing: 0.06em
  display-title-mobile:
    fontFamily: notoSerif
    fontSize: 36px
    fontWeight: '500'
    lineHeight: '1.2'
    letterSpacing: 0.06em
  poem-body:
    fontFamily: notoSerif
    fontSize: 20px
    fontWeight: '400'
    lineHeight: '2.1'
    letterSpacing: 0.02em
  poem-body-mobile:
    fontFamily: notoSerif
    fontSize: 18px
    fontWeight: '400'
    lineHeight: '2.05'
    letterSpacing: 0.02em
  author-subheading:
    fontFamily: notoSerif
    fontSize: 15px
    fontWeight: '400'
    lineHeight: '1.5'
    letterSpacing: 0.02em
  author-subheading-mobile:
    fontFamily: notoSerif
    fontSize: 14px
    fontWeight: '400'
    lineHeight: '1.5'
    letterSpacing: 0.02em
  label-metadata:
    fontFamily: inter
    fontSize: 12px
    fontWeight: '400'
    lineHeight: '1.4'
    letterSpacing: 0.14em
  label-metadata-mobile:
    fontFamily: inter
    fontSize: 11px
    fontWeight: '400'
    lineHeight: '1.4'
    letterSpacing: 0.14em
  ui-nav:
    fontFamily: inter
    fontSize: 14px
    fontWeight: '500'
    lineHeight: '1.0'
    letterSpacing: 0.05em
spacing:
  frame-padding-desktop: 72px 64px 44px
  frame-padding-mobile: 40px 30px 28px
  gutter-stanza: 56px
  margin-kicker: 22px
  margin-title: 12px
  margin-divider: 44px
  section-gap: 34px
---

## Brand & Style

This design system is rooted in the "digital manuscript" philosophy, treating the screen as a tactile, historical canvas. It targets scholars, poetry enthusiasts, and those seeking a meditative reading experience. The personality is **scholarly, serene, and refined**, evoking the quiet atmosphere of a private library or a traditional scroll.

The design style is a blend of **Minimalism** and **Traditional Chinese Aesthetics**. It prioritizes extreme typographic clarity and generous whitespace to encourage "slow reading." By utilizing a restricted palette of paper-like tones and high-contrast ink-like text, the system creates a sense of timelessness and intellectual rigor without the distraction of modern decorative trends.

## Colors

The color palette mimics the materials of classical Chinese literacy: ink, stone, and aged paper.

- **Primary**: A deep, scholarly blue used for brand anchors, titles, and primary interactive states.
- **Neutral**: The "Parchment" background color serves as the universal surface, reducing eye strain and providing a warm, tactile feel.
- **Secondary/Tertiary**: Earthy "Olive" and "Line" tones are reserved for metadata and structural dividers, ensuring they remain subordinate to the literary content.
- **Contrast**: Content should primarily use "Near-Black" for text to ensure high legibility against the parchment background.

## Typography

The typography system centers on the "Serif" (ideally TsangerJinKai02 or Noto Serif SC) for all literary and narrative content. This provides a calligraphic quality essential to the brand. 

- **Poem Body**: Uses a very high line-height (over 200%) to mimic traditional vertical manuscript spacing, even when presented horizontally.
- **Labels**: A clean Sans-Serif (Inter) is used for technical metadata and UI navigation to create a clear functional distinction between "the literature" and "the interface."
- **Letter Spacing**: Titles and labels use generous tracking to evoke a sense of air and importance.

## Layout & Spacing

This design system uses a **Fixed Grid** approach that simulates a physical page. The layout does not stretch to fill the viewport; instead, it maintains a structured "frame" with wide safe margins.

- **Desktop/Tablet**: A dual-column approach for poetry stanzas, separated by a vertical divider. 
- **Mobile**: A single-column flow with reduced horizontal margins.
- **Vertical Rhythm**: Spacing is precise and generous. The relationship between the "kicker" (top decorative bar) and the title is the anchor of every page.
- **Alignment**: Primary content is centered or left-aligned within the frame to maintain the "manuscript" feel.

## Elevation & Depth

Hierarchy is achieved entirely through **Tonal Layers** and **Contrast**, rather than shadows or blurs. 

- **Surface Strategy**: The background is a flat parchment layer. The only "depth" perceived is the separation of the content frame from the dark "environment" backdrop.
- **Low-Contrast Outlines**: When grouping of elements is necessary (e.g., cards or list items), use subtle `1px` borders in the "Line" or "Warm-Sand" color.
- **Zero Shadows**: No box-shadows are permitted. Depth is a concept of physical paper, and as such, elements do not "float" above the page; they are part of it.

## Shapes

The shape language is strictly **Sharp**. All corners for containers, buttons, and decorative elements (like the brand kicker) must be 0px. This reinforces the architectural and historical nature of the design, reflecting the cut of paper or the edge of a woodblock print.

## Components

- **The Kicker**: A primary-colored rectangular block (`5px` height for desktop, `4px` for mobile) placed at the top of the content area. This is the primary brand signature.
- **Buttons**: Flat, text-only or with a 1px border. Use `ui-nav` typography. No fill, except for primary actions which use a solid "Brand" blue background with white text.
- **Dividers**: Use `1px` solid "Line" or "Warm-Sand." Vertical dividers are used between stanzas in multi-column layouts; horizontal rules separate the header from the content body.
- **Cards (Search/Collections)**: Sharp-edged containers with a 1px "Line" border. Titles within cards use a smaller version of the display serif.
- **Link Components**: For "Word" or "Author" associations, use a subtle underline or the "Primary" blue color. These should feel like citations in a scholarly text.
- **Input Fields**: Minimalist lines rather than boxes. Use a bottom border only, in the "Stone" color, transitioning to "Brand" blue on focus.