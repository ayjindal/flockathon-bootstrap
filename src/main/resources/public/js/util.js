var baseUrl="https://5649ca12.ngrok.io/"
function sendAjaxRequest(url, methodType, objectTobeSent, successCallback) {
    $.ajax({
        url: url,
        type: methodType,
        data: JSON.stringify(objectTobeSent),
        success: function (response) {
            if (successCallback != null) {
                successCallback(response);
            }
        },
        error: function(response, status, error) {
            console.log('XHR failure: ' + status);
            console.log(error);
        }
    });
}
function getFlockEvent() {
   event = decodeURIComponent(getQueryVariable("flockEvent"));
   return JSON.parse(event);
}

function getQueryVariable(variable) {
     var query = window.location.search.substring(1);
     var vars = query.split("&");
     for (var i = 0; i < vars.length; i++) {
         var pair = vars[i].split("=");
         if (pair[0] == variable) {
             return pair[1];
         }
     }
     return null;
}
