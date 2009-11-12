$(document).ready (function () 
{ 
  $('.expand-body').addClass ('contracted'); 

  var headers = $('.expand-header');

  headers.prepend ('<span class="expander-toggle">+ </span>');
  headers.append  ('<span class="expander-ellipsis">…</span>');

  headers.click (function (event)
  { 
    event.preventDefault (); 

    $(this).children ('.expander-ellipsis').toggle ();

    var body = $(this).next ('.expand-body');
    var switcher = body.hasClass ('contracted') ? '- ' : '+ ';

    body.toggleClass ('contracted');
    $(this).children ('.expander-toggle').text (switcher);
  })
});
