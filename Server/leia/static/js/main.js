var host = "http://"+window.location.host;

$(".response textarea").keyup(checkEnter);
$(".response button").click(sendResponse);

function checkEnter(event) {
    var code = (event.keyCode ? event.keyCode : event.which);
    if(code == 13) { // Enter pressed
        sendResponse(event);
    }
}

function sendResponse(event) {
    var message = $('.response textarea').val();
    $('<div class="message"><span class="you-title">You:</span> '+message+'</div>').appendTo('.chat-content');
    $('.response textarea').val("");
    $('.chat-content').scrollTop($('.chat-content')[0].scrollHeight);
    $.post(host+'/converse/'+chat_id+'/', 'sentence='+message, function (data) {
        $('<div class="message"><span class="bot-title">Leia:</span> '+data['message']+'</div>').appendTo('.chat-content');
        $('.chat-content').scrollTop($('.chat-content')[0].scrollHeight);
        if (data['suggestions'].length) {
            var rows = "";
            var restaurants = data['suggestions'];
            for (var i = 0; i < restaurants.length; i++)
            {
                var r = restaurants[i];
                rows += "<tr><td>";
                rows += "<strong>Name:</strong> "+restaurants[i].name+"<br>";
                rows += "<strong>Phone:</strong> "+restaurants[i].phone+"<br>";
                rows += "<strong>Address:</strong> "+restaurants[i].location+"<br>";
                rows += "<strong>Rating:</strong> "+restaurants[i].rating+"<br>";
                rows += "<strong>Niceness:</strong> "+restaurants[i].niceness+"<br>";
                rows += "<strong>Categories:</strong> "+restaurants[i].categories+"<br>";
                rows += "</tr></td>";
            }
            $('.restaurant-list').html(rows);
        }
    }, "json");
}

// Init
$.post(host+'/converse/'+chat_id+'/', 'sentence=init', function (data) {
    $('<div class="message"><span class="bot-title">Leia:</span> '+data['message']+'</div>').appendTo('.chat-content');
}, "json");
