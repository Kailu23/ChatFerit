module.exports = {
  root: true,
  env: {
    es6: true,
    node: true,
  },
  extends: [
    "eslint:recommended",
    "google", // Assuming you're using this or a similar strict config
  ],
  parserOptions: {
    ecmaVersion: 2020, // Or your Node version's supported ES version
  },
  rules: {
    // You can keep other rules like quotes and indent if you like them
    "quotes": ["error", "double"],
    "indent": ["error", 2], // Or 4, or whatever your preference is

    // --- Disable JSDoc Rules ---
    "require-jsdoc": "off",
    "valid-jsdoc": "off", // This rule checks the validity of JSDoc comments
    // It's often the one that's picky about descriptions
  },
};
