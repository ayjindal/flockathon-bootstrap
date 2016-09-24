$(document).ready(function()
{
     var platformQuestions;
     getQuestions("platform");
     $("#submit").click(function () {
        console.log("submit clicked")
         name = $("#name").val();
         email = $("#email").val();
         cvLink = $("#cv_link").val();
         role = $("#role").val();
         question = $("#question").val();
         time = getTime($("#time").val());
         event = getFlockEvent();
         console.log("userId: " + event.userId);
         console.log("chat: " + event.chat);
         createCandidate();
         flock.close();
     });

     function getTime(dateString) {
        var parts = dateString.match(/(\d{2})\/(\d{2})\/(\d{4}) (\d{2}):(\d{2})/);
        return Date.UTC(+parts[3], parts[2]-1, +parts[1], +parts[4], +parts[5]);
     }

     function getQuestions(role) {
          payload =
          {
               "role": role,
               "groupId": "g:123"
          }
          sendAjaxRequest(baseUrl + "questions", "get", payload, function (response) {
              questions = JSON.parse(response)
              $.each(questions, function (i, question) {
                  $('#question').append($('<option>', {
                      value: question.id,
                      text : question.title + " (" + question.level + ")"
                  }));
              });
          });
     }

     function createCandidate() {
          payload =
          {
              "candidate": {
                  "name": name,
                  "email": email,
                  "cv_link": cvLink,
                  "role": role,
                  "creator_id": event.userId,
              },
              "round": {
                  "collab_link": "http://testCollabLink",
                  "interviewer_id": "testinterviewerid",
                  "question_id": question,
                  "scheduled_time": JSON.stringify(time)
              }
          };
          sendAjaxRequest(baseUrl + "create", "post", payload, function (response) {
              console.log("successful response: " + JSON.stringify(response));
          });
     }
});


