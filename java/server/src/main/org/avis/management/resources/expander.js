$(document).ready (function () 
{ 
  var header = $('.expand-header');
  header.prepend ('<span class="expander-toggle">+ </span>');
  header.append  ('<span class="expander-ellipsis">…</span>');

  $('.expand-body').addClass ('contracted'); 

  $('.expand-header').click (function (event)
  { 
    event.preventDefault (); 

    $(this).children ('.expander-ellipsis').toggle ();

    var body = $(this).next ('.expand-body');
    var toggle = body.hasClass ('contracted') ? '- ' : '+ ';

    body.toggleClass ('contracted');
    $(this).children ('.expander-toggle').text (toggle);
  })
});
