module.exports = {
  content: {
    preserveHtmlElements: false,
    content: [
      '../webui/src/main/assets/form.html'
    ]
  },
  safelist: [
      {
            pattern: /grid-cols-(1|2|3|4|5|6|7|8|9|10|11|12)/,
      }
    ],
  darkMode: false, // or 'media' or 'class'
  theme: {
    extend: {},
  },
  variants: {
    extend: {
      opacity: ['disabled'],
    }
  },
  plugins: [],
}
