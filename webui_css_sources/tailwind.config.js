module.exports = {
  purge: {
    preserveHtmlElements: false,
    content: [
      '../webui/src/main/assets/form.html'
    ]
  },
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
