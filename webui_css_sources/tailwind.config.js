module.exports = {
  purge: {
    preserveHtmlElements: false,
    content: [
      '../myExpenses/src/main/assets/form.html'
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
