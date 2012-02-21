var known = { en: true, fr: true };
//unfortunately navigator.language is broken in Androids Webkit
//we set userAgent to language through webview settings
var userLang = navigator.userAgent,
  doc = document,
  head = doc.getElementsByTagName('head')[0],
  style = doc.createElement('style');
if(!known[userLang])
  userLang = 'en';
style.appendChild( doc.createTextNode(':lang( ' +userLang + ' ) { display:block }\n\
span:lang( ' +userLang + ' ) { display:inline }' ));
head.appendChild( style);