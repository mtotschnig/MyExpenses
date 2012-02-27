var known = { en: "English", fr: "Français", de: "Deutsch"};
var userLang;
var doc = document,
    head = doc.getElementsByTagName('head')[0],
    style = doc.createElement('style');
//unfortunately navigator.language is broken in Androids Webkit
//we set userAgent to language through webview settings

userLang = readCookie("lang");
if (!userLang) {
	userLang = navigator.userAgent;
	if (userLang.length > 2)
	   userLang = navigator.language.substr(0,2); 
	if(!known[userLang])
	  userLang = 'en';
  //console.log(userLang);
}

function init() {
	setLang(userLang);
}
function selectLang(lang) {
    document.cookie="lang=" + lang;
    setLang(lang)
}
function setLang(lang) {
    while (a= style.firstChild) {
        style.removeChild(a);
    }
    style.appendChild( doc.createTextNode('*[lang="' +lang + '"] { display:block }\n\
        span[lang="' +lang + '"] { display:inline }' ));
    head.appendChild( style);
    var imgs = doc.getElementsByTagName('img');
    for (var i = 0; i<imgs.length;i++) {
        console.log(imgs[i].id);
        imgs[i].src = lang + "/" + imgs[i].id + ".png";
  }
}
function readCookie(name) {
    var nameEQ = name + "=";
    var ca = document.cookie.split(';');
    for(var i=0;i < ca.length;i++) {
        var c = ca[i];
        while (c.charAt(0)==' ') c = c.substring(1,c.length);
        if (c.indexOf(nameEQ) == 0) return c.substring(nameEQ.length,c.length);
    }
    return null;
}
