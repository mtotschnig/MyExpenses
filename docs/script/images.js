var imageDialog;

$(document).ready(function() {
    imageDialog =$("<div id='dialog'><img id='image' src=''/></div>").dialog({
      modal: true,
      resizable: false,
      draggable: false,
      autoOpen: false,
      height: 'auto',
      width: 'auto'
    });
       
    $('.screenshot img').click(function(event){
      event.preventDefault();
      PreviewImage($(this));
    });
});

PreviewImage = function(img) {
  var uri = img.attr('src');
  var title = img.attr('title');
  //Get the HTML Elements
  var imageTag = $('#image');
 
  //Split the URI so we can get the file name
  var uriParts = uri.split("/");
  uriParts.splice(-1,0,"large");

  //When the image has loaded, display the dialog
  imageTag.load(function(){
    imageDialog.dialog({title: title});
    imageDialog.dialog("open");
  });

  //Set the image src
  imageTag.attr('src', uriParts.join('/'));
}
