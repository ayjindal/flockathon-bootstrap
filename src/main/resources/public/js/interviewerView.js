$(document).ready(function()
{
    var event = getFlockEvent();
    $("#end").click(function () {
         console.log("end clicked")
         name = $("#candidate_name").text();
         email = $("#candidate_email").text();
         interviewerId = event.userId;
         rating = "4"; // TODO soft code
         verdict = "reject"; // TODO soft code
         comments = $('textarea#comments').val();
         console.log("comments: " + comments);
         updateRound();
         flock.close();
     });

     function updateRound() {
           payload =
           {
               "email": email,
               "interviewer_id": interviewerId,
               "comments": comments,
               "rating": rating,
               "verdict": verdict
           };
           sendAjaxRequest(baseUrl + "update", "post", payload, function (response) {
               console.log("successful response: " + JSON.stringify(response));
           });
      }
});