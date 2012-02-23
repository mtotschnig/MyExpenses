function init() {
	var known = { en: true, fr: true };
	//unfortunately navigator.language is broken in Androids Webkit
	//we set userAgent to language through webview settings
	var userLang = navigator.userAgent,
	  doc = document,
	  head = doc.getElementsByTagName('head')[0],
	  style = doc.createElement('style');
	if(!known[userLang])
	  userLang = 'en';
	  //console.log(userLang);
	style.appendChild( doc.createTextNode('*[lang="' +userLang + '"] { display:block }\n\
span[lang="' +userLang + '"] { display:inline }' ));
	head.appendChild( style);
	var imgs = doc.getElementsByTagName('img');
	for (var i = 0; i<imgs.length;i++) {
	    console.log(imgs[i].id);
	    imgs[i].src = userLang + "/" + imgs[i].id + ".png";
  }
}