$(document).ready(function()
{
     $("#submit").click(function () {
        console.log("submit clicked")
         name = $("#name").val();
         email = $("#email").val();
         cvLink = $("#cv_link").val();
         console.log("name: " + name);
         event = getFlockEvent();
         console.log("userId: " + event.userId);
         console.log("chat: " + event.chat);
         createCandidate();
         flock.close();
     });

     function createCandidate() {
          payload =
          {
              "candidate": {
                  "name": name,
                  "email": email,
                  "cv_link": cvLink,
                  "role": "platform",
                  "creator_id": event.userId,
              },
              "round": {
                  "collab_link": "http://testCollabLink",
                  "interviewer_id": "testinterviewerid",
                  "question_id": "test_ques_id",
                  "scheduled_time": "147314131"
              }
          };
          sendAjaxRequest(baseUrl + "create", "post", payload, function (response) {
              console.log("successful response: " + JSON.stringify(response));
          });
     }
});


