var imageDialog;

$(document).ready(function() {
imageDialog =$("<div id='dialog'><img id='image' src=''/></div>").dialog({
      modal: true,
      resizable: false,
      draggable: false,
      width: 'auto',
      autoOpen: false
    });
       
  $('.screen > img').click(function(event){
       
    event.preventDefault();
    PreviewImage($(this).attr('src'));
                                       
  });                     
});

PreviewImage = function(uri) {
  uri = "large/" +uri;
  //Get the HTML Elements
  imageTag = $('#image');
 
  //Split the URI so we can get the file name
  uriParts = uri.split("/");

  //Set the image src
  imageTag.attr('src', uri);
 
  //When the image has loaded, display the dialog
  imageTag.load(function(){
    imageDialog.dialog("open");
  });
}
