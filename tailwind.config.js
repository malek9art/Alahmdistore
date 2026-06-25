/** @type {import('tailwindcss').Config} */
module.exports = {
  content: [
    "./src/**/*.{html,js,jsx,ts,tsx,vue}",
    "./components/**/*.{html,js,jsx,ts,tsx,vue}",
    "./app/**/*.{html,js,jsx,ts,tsx,vue}",
    "./index.html"
  ],
  theme: {
    extend: {
      colors: {
        // Alahmadi Mobile Center - 'Geometric Balance' Color Palette
        'dark-teal': {
          DEFAULT: '#004d40', // Base Dark Teal
          dark: '#003d33',    // Deep dark teal
          light: '#00695c',   // Shifting lighter teal
          card: '#005b4f',    // Surface elevations
        },
        'gold-yellow': {
          DEFAULT: '#ffc107', // Golden Yellow accent
          light: '#ffe082',   // Soft gold highlight
          dark: '#ffb300',    // Hover states / focus
        },
        'slate-gray': {
          light: '#B0BEC5',   // Secondary metadata typography
          dark: '#607D8B',    // Subdued details
        },
        alert: {
          error: '#EF4444',   // Red warnings
          success: '#10B981', // Green approvals
          warning: '#F59E0B', // Amber pendings
          info: '#3B82F6',    // Blue trackers
        }
      },
      fontFamily: {
        // Setting 'IBM Plex Sans Arabic' as the primary font family
        sans: ['"IBM Plex Sans Arabic"', 'ui-sans-serif', 'system-ui', '-apple-system', 'BlinkMacSystemFont', '"Segoe UI"', 'Roboto', 'sans-serif'],
        mono: ['"JetBrains Mono"', 'ui-monospace', 'SFMono-Regular', 'monospace'],
      },
      borderRadius: {
        'geometric': '14px',    // Sharp elegant rounded borders
        'geometric-lg': '20px', // Custom card corners
      },
      boxShadow: {
        'gold-glow': '0 0 20px rgba(255, 193, 7, 0.25)', // Premium highlighting
        'teal-depth': '0 12px 36px rgba(0, 61, 51, 0.5)',  // Deep teal elevation shadows
      }
    },
  },
  plugins: [
    // RTL and LTR support utilities
    function ({ addUtilities }) {
      addUtilities({
        '.rtl': {
          direction: 'rtl',
        },
        '.ltr': {
          direction: 'ltr',
        },
      })
    }
  ],
}
