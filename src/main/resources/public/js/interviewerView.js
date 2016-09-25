$(document).ready(function()
{
    var event = getFlockEvent();

    var verdict = "undecided";


     $("#pass") // select the radio by its id
         .change(function(){ // bind a function to the change event
             if( $(this).is(":checked") ){ // check if the radio is checked
                 verdict = "pass"; // retrieve the value
             }else {
                verdict = "reject";
             }
         });

          $("#reject") // select the radio by its id
                  .change(function(){ // bind a function to the change event
                      if( $(this).is(":checked") ){ // check if the radio is checked
                          verdict = "reject"; // retrieve the value
                      }else {
                         verdict = "pass";
                      }
                  });



    $("#end").click(function () {
         console.log("end clicked")
         name = $("#candidate_name").text();
         email = $("#candidate_email").text();
         interviewerId = event.userId;
         if(verdict === "undecided")  {
              alert('Please select verdict');
          } else {
         comments = $('#comments').val();
         console.log("comments: " + comments);
         console.log("rating: " + rating);
         console.log("verdict: " + verdict);

         updateRound();
         flock.close();
         }
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