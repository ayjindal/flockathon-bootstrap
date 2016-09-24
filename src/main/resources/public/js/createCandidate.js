$(document).ready(function()
{
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

     $('#role').change(function(){
         role = $('#role').val();
         getQuestions(role);
     });

     function getTime(dateString) {
        var parts = dateString.match(/(\d{2})\/(\d{2})\/(\d{4}) (\d{2}):(\d{2})/);
        return Date.UTC(+parts[3], parts[2]-1, +parts[1], +parts[4], +parts[5]);
     }

     function getQuestions(role) {
          console.log("Get questions for role: " + role)
          sendAjaxRequest(baseUrl + "questions?role=" + role + "&groupId=g:123&sequence=1", "get", null, function (response) {
            if(role == $('#role').val()) {
                  $('#question').html('');
                  questions = JSON.parse(response);
                  $.each(questions, function (i, question) {
                      $('#question').append($('<option>', {
                          value: question.id,
                          text : question.title + " (" + question.level + ")"
                      }));
                  });
            }
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


