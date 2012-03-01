if (!inAppP)
  document.write('<h2><a href="../index.html">MyExpenses</a> Tutorial</h2>');
document.write('\
      <a id="nav1" href="tutorial1.html">\
        <span lang="en">Managing accounts</span>\
        <span lang="fr">Gestion des comptes</span>\
        <span lang="de">Verwaltung der Konten</span>\
         </a> |\
      <a id="nav2" href="tutorial2.html">\
        <span lang="en">Managing transactions</span>\
        <span lang="fr">Gestion des opérations</span>\
        <span lang="de">Verwaltung der Buchungen</span>\
      </a> |\
      <a id="nav3" href="tutorial3.html">\
        <span lang="en">Managing categories</span>\
        <span lang="fr">Gestion des catégories</span>\
        <span lang="de">Verwaltung der Kategorien</span>\
      </a> |\
      <a id="nav4" href="tutorial4.html"> \
        <span lang="en">Export transactions to Grisbi</span>\
        <span lang="fr">Exporter les opérations vers Grisbi</span>\
        <span lang="de">Buchungen exportieren</span>\
        </a>\
');
document.write('<span class="langselector"><select onchange="selectLang(this.value)">');
for (var lang in known) {
  document.write('<option value="'+lang+'"');
  if (lang == userLang) {
    document.write(' selected');
  }
  document.write('>'+known[lang]+'</option>');
}
document.write('</select></span>');
