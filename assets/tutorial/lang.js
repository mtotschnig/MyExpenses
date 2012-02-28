var inAppP = false;
var known = { en: "English", fr: "Français", de: "Deutsch"};
var userLang;
var doc = document,
    head = doc.getElementsByTagName('head')[0],
    style = doc.createElement('style');
head.appendChild( style);
//unfortunately navigator.language is broken in Androids Webkit
//we set userAgent to language through webview settings
//and use this as test if we are displaying the tutorial as In-App-Activity
var userAgent = navigator.userAgent;
if (userAgent.length == 2)
    inAppP = true;

userLang = readCookie("lang");
if (!userLang) {
  if (inAppP)
    userLang = userAgent;
  else {
    userLang = navigator.userLanguage || navigator.language;
    userLang = userLang.substr(0,2);
  }
	if(!known[userLang])
	  userLang = 'en';
  //console.log(userLang);
}

function init() {
	setLang(userLang);
}
function selectLang(lang) {
    document.cookie="lang=" + lang;
    setLang(lang);
    userLang = lang;
}
function setLang(lang) {
    setVisible(lang);
    var imgs = doc.getElementsByTagName('img');
    for (var i = 0; i<imgs.length;i++) {
	if (imgs[i].id.substr(0,4) == "step") {
	  console.log(imgs[i].id);
	  imgs[i].src = lang + "/" + imgs[i].id + ".png";
    }
  }
}
function setVisible(lang) {
    if (inAppP) {
	    while (a= style.firstChild) {
	        style.removeChild(a);
	    }
	    style.appendChild( doc.createTextNode('*[lang="' +lang + '"] { display:block }\n\
	        span[lang="' +lang + '"] { display:inline }' ));
    } else {
        $('*[lang]').hide();
        $('*[lang="'+lang+'"]').show();
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
