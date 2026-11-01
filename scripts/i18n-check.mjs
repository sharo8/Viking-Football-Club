import { readFileSync } from "node:fs";

const files = {
  en: JSON.parse(readFileSync("src/locales/office/en.json", "utf8")),
  fr: JSON.parse(readFileSync("src/locales/office/fr.json", "utf8")),
  rw: JSON.parse(readFileSync("src/locales/office/rw.json", "utf8")),
};

const keys = Object.keys(files.en).sort();
let failed = false;
for (const locale of ["fr", "rw"]) {
  const theirs = Object.keys(files[locale]).sort();
  for (const key of keys) {
    if (!files[locale][key]) {
      console.error(`${locale} is missing ${key}`);
      failed = true;
    }
  }
  for (const key of theirs) {
    if (!(key in files.en)) {
      console.error(`${locale} has an extra key ${key}`);
      failed = true;
    }
  }
}

const source = readFileSync("src/components/admin/peopleOffice.jsx", "utf8");
const literals = [...source.matchAll(/>([A-Za-z][^<{]{2,})</g)].map((match) => match[1].trim());
if (literals.length) {
  console.error("Hard-coded text in peopleOffice.jsx:");
  literals.forEach((line) => console.error(`  ${line}`));
  failed = true;
}

if (failed) process.exit(1);
console.log(`i18n ok (${keys.length} keys)`);
