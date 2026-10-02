---
name: Kinetic Obsidian
colors:
  surface: '#111316'
  surface-dim: '#111316'
  surface-bright: '#37393d'
  surface-container-lowest: '#0c0e11'
  surface-container-low: '#1a1c1f'
  surface-container: '#1e2023'
  surface-container-high: '#282a2d'
  surface-container-highest: '#333538'
  on-surface: '#e2e2e6'
  on-surface-variant: '#e4bfb1'
  inverse-surface: '#e2e2e6'
  inverse-on-surface: '#2f3034'
  outline: '#ab897d'
  outline-variant: '#5b4137'
  surface-tint: '#ffb599'
  primary: '#ffb599'
  on-primary: '#5a1c00'
  primary-container: '#ff5e00'
  on-primary-container: '#531900'
  inverse-primary: '#a63b00'
  secondary: '#ffffff'
  on-secondary: '#283500'
  secondary-container: '#c3f400'
  on-secondary-container: '#556d00'
  tertiary: '#00daf3'
  on-tertiary: '#00363d'
  tertiary-container: '#00a2b5'
  on-tertiary-container: '#003138'
  error: '#ffb4ab'
  on-error: '#690005'
  error-container: '#93000a'
  on-error-container: '#ffdad6'
  primary-fixed: '#ffdbce'
  primary-fixed-dim: '#ffb599'
  on-primary-fixed: '#370e00'
  on-primary-fixed-variant: '#7f2b00'
  secondary-fixed: '#c3f400'
  secondary-fixed-dim: '#abd600'
  on-secondary-fixed: '#161e00'
  on-secondary-fixed-variant: '#3c4d00'
  tertiary-fixed: '#9cf0ff'
  tertiary-fixed-dim: '#00daf3'
  on-tertiary-fixed: '#001f24'
  on-tertiary-fixed-variant: '#004f58'
  background: '#111316'
  on-background: '#e2e2e6'
  surface-variant: '#333538'
typography:
  display-lg:
    fontFamily: Outfit
    fontSize: 40px
    fontWeight: '800'
    lineHeight: 48px
    letterSpacing: -0.03em
  headline-lg:
    fontFamily: Outfit
    fontSize: 30px
    fontWeight: '700'
    lineHeight: 38px
    letterSpacing: -0.02em
  headline-lg-mobile:
    fontFamily: Outfit
    fontSize: 26px
    fontWeight: '700'
    lineHeight: 32px
    letterSpacing: -0.02em
  headline-md:
    fontFamily: Outfit
    fontSize: 22px
    fontWeight: '600'
    lineHeight: 28px
  headline-sm:
    fontFamily: Outfit
    fontSize: 18px
    fontWeight: '600'
    lineHeight: 24px
  body-lg:
    fontFamily: Plus Jakarta Sans
    fontSize: 16px
    fontWeight: '400'
    lineHeight: 24px
  body-md:
    fontFamily: Plus Jakarta Sans
    fontSize: 14px
    fontWeight: '400'
    lineHeight: 20px
  body-sm:
    fontFamily: Plus Jakarta Sans
    fontSize: 12px
    fontWeight: '400'
    lineHeight: 16px
  label-lg:
    fontFamily: Space Grotesk
    fontSize: 14px
    fontWeight: '700'
    lineHeight: 18px
    letterSpacing: 0.04em
  label-md:
    fontFamily: Space Grotesk
    fontSize: 12px
    fontWeight: '600'
    lineHeight: 16px
    letterSpacing: 0.06em
  label-sm:
    fontFamily: Space Grotesk
    fontSize: 10px
    fontWeight: '600'
    lineHeight: 12px
    letterSpacing: 0.08em
  metric-display:
    fontFamily: Space Grotesk
    fontSize: 32px
    fontWeight: '700'
    lineHeight: 36px
    letterSpacing: -0.04em
rounded:
  sm: 0.25rem
  DEFAULT: 0.5rem
  md: 0.75rem
  lg: 1rem
  xl: 1.5rem
  full: 9999px
spacing:
  gutter: 1rem
  margin: 1.25rem
  space-xs: 0.25rem
  space-sm: 0.5rem
  space-md: 1rem
  space-lg: 1.5rem
  space-xl: 2rem
---

## Brand & Style

This design system is engineered for high-performance fitness tracking, drawing direct inspiration from elite workout companions like Nike Training Club, Hevy, and Strong. It balances an intense athletic drive with precision-engineered utility. The experience is tuned for athletes and gym-goers who need instantaneous, glanceable feedback during high-exertion sets under fluctuating gym lighting.

The visual direction combines **Dark Tactical Modernism** with **High-Contrast Energy Accents**. Deep charcoal and obsidian layers eliminate cognitive clutter and conserve battery life, while electric kinetic orange provides an unmissable call-to-action signature. Typography is punchy, industrial, and distinctly athletic, ensuring numbers, sets, reps, and workout titles are legible at arm's length. Surfaces feel sculpted, tactile, and engineered rather than flat or decorative.

## Colors

The palette operates in a default dark mode tailored for gym environments.

- **Primary (`#FF5E00` - Blaze Orange):** The primary kinetic driver. Reserved for high-value actions such as starting a routine, completing a set, primary callouts, and active timers.
- **Secondary (`#CCFF00` - Volt Lime):** An energetic accent dedicated to progressive metric feedback, PR (personal record) badges, active streak counters, and positive completion states.
- **Tertiary (`#00E5FF` - Electric Cyan):** Used sparingly for secondary telemetry, data visualizations, volume bars, and heart-rate or rest-interval gauges.
- **Neutral Foundation (`#121417` Base Obsidian):** Structured in layered dark tones:
  - Deep Base (`#0B0D0E`): System background and status canvas.
  - Surface Default (`#16191E`): Base card and routine container fill.
  - Surface Elevated (`#1F242B`): Floating chips, active list tiles, and bottom navigation pill containers.
  - Border Subdued (`#282E37`): Razor-thin borders for card separation.
  - High Contrast Text (`#F5F7FA`): Crisp readability for exercise titles and primary numbers.
  - Muted Text (`#8B95A5`): Supporting telemetry like rest countdowns and set labels.

## Typography

The typographic hierarchy prioritizes rapid comprehension during active training.

- **Headline Font (Outfit):** Bold, geometric, and modern. Used for screen titles, workout headers, and celebratory PR modals. Its wide proportions bring an aggressive athletic stance.
- **Body Font (Plus Jakarta Sans):** Highly legible humanist sans-serif. Used for exercise descriptions, trainer notes, and general UI narrative.
- **Label & Telemetry Font (Space Grotesk):** A technical monospace-adjacent sans-serif used for weight, reps, timers, rest intervals, and micro-badges. The tabular feel keeps data perfectly aligned when scrubbing weight plates or rep counts.

## Layout & Spacing

The layout is built for fluid mobile-first thumb navigation with a strict 4px/8px incremental rhythm.

- **Screen Canvas & Safe Areas:** 20px (`1.25rem`) horizontal margins on mobile devices to create breathing room between screen edges and card walls, with generous bottom padding (`80px` to `96px`) to ensure content clears floating navigation docks and quick-log bars.
- **Stack Structure:**
  - **Top Utility Header:** Compact area containing routine actions (`+ Crear Rutina`), profile status, and quick streaks.
  - **Primary Routine Feed:** High-density, single-column fluid card layout for active routines, allowing rapid tap targets for workout launch.
  - **Dashboard Grid (Calendar & Analytics):** 2-column equal-split grid (`gutter: 1rem`) displaying calendar heatmaps and volume summaries side by side.
  - **Persistent Floating Dock:** Centered pill bar hosting primary tab switching with thumb-optimized 48x48px hit areas.

## Elevation & Depth

Visual hierarchy uses a refined hybrid of **tonal layering**, **micro-borders**, and **ambient neon glow**:

- **Layer 0 (Canvas):** Pure `#0B0D0E`, passive non-interactive substrate.
- **Layer 1 (Card & List Surfaces):** `#16191E` with a 1px solid border in `#282E37`. Provides clear visual segmentation without distracting high-contrast lines.
- **Layer 2 (Interactive Floating Modules & Modals):** `#1F242B` with a subtle top-lit ambient shadow: `0 8px 24px -4px rgba(0, 0, 0, 0.65)`.
- **Active State / Primary Elevation:** When a routine card is active or a primary action is focused, it inherits an ambient kinetic glow: `0 6px 20px -2px rgba(255, 94, 0, 0.35)` with an inner outline of `rgba(255, 94, 0, 0.25)`.
- **Dock Elevation:** Elevated floating dock featuring a `backdrop-filter: blur(16px)` with `rgba(22, 25, 30, 0.85)` surface fill, suspended by a crisp `0 12px 32px rgba(0, 0, 0, 0.8)` drop shadow.

## Shapes

The shape system leverages level `2` (Rounded) geometry, balancing industrial toughness with ergonomic, tactile softness:

- **Routine & Widget Cards:** 16px (`1rem` / `rounded-lg`) border radius to enclose complex information chunks in a clean, handheld form.
- **Floating Controls & Primary CTAs:** Full pill rounding (`9999px`) for high-priority buttons (e.g., "+ Crear Rutina", "Iniciar Entrenamiento") to denote instant clickability.
- **Telemetry Chips & Tag Badges:** 8px (`0.5rem` / `rounded-md`) corner radius for tags like target muscle groups, sets completed, and rest timers.
- **Bottom Navigation Dock:** 24px (`1.5rem` / `rounded-xl`) pill silhouette that floats above the bottom edge.

## Components

### Buttons
- **Primary CTA ("Crear Rutina +", "Iniciar"):** Solid Blaze Orange (`#FF5E00`) background, obsidian text (`#0B0D0E`), bold weight (`Space Grotesk`), pill radius. High-stress touch target (minimum height 48px, minimum width 120px). Subtle spring animation on tap.
- **Secondary / Ghost:** `#1F242B` surface, 1px border in `#282E37`, crisp white typography. Hover/active shifts border to `#FF5E00`.
- **Destructive / Abandon Workout:** Dark crimson tinted surface with vibrant red label.

### Routine Cards (Primary Feed)
- Styled as full-width interactive cards with an ergonomic 16px corner radius.
- Features a dark `#16191E` background, 1px border `#282E37`, and internal horizontal layout:
  - Left: Accent indicator bar or muscle group icon.
  - Center: Routine title in `Outfit Headline-sm`, followed by micro-chips for estimated duration (e.g., "55 min") and exercise count (e.g., "6 ejercicios").
  - Right: Quick-start kinetic action chevron or "Play" circle.

### Metric & Calendar Widgets
- Half-width dual cards displayed in a 2-column grid.
- **Calendar Module:** Micro-heatmap grid with week circles; active workout days light up in Volt Lime (`#CCFF00`) or Blaze Orange (`#FF5E00`).
- **Telemetry Tile:** Displays current weekly volume, PR count, or streak with large-scale `metric-display` typography.

### Floating Bottom Navigation Bar
- Floats 16px above the device home indicator.
- Glassmorphic translucent dark background (`rgba(22, 25, 30, 0.85)` + blur) with rounded-xl perimeter.
- Houses 4 to 5 streamlined monochrome SVG icons. The active tab is indicated with an energetic Blaze Orange tint and a luminous micro-dot indicator below.

### Chips & Badges
- Compact 24px-28px height, 8px radius.
- Muted carbon fill (`#1F242B`) with high-contrast text (`Space Grotesk Label-sm`).
- PR (Personal Record) tags utilize Volt Lime text on a low-opacity lime background (`rgba(204, 255, 0, 0.12)`).

### Input Fields & Set Checkboxes
- **Set Checkbox:** Circular or squircle 28px tap target. Unchecked: `#1F242B` with `#282E37` border. Checked: Instant fill with Volt Lime (`#CCFF00`) and a dark checkmark icon.
- **Numeric Weight/Rep Inputs:** Tabular numeric layout with large centered values, accompanied by stepped increment (`+` / `-`) auxiliary hit zones.