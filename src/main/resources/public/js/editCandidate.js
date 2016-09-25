$(document).ready(function()
{
     getQuestions(role);
     var event = getFlockEvent();
     var userId = event.userId;
     console.log("groupId: " + groupId);
     getInterviewers(groupId);
     $("#submit").click(function () {
         interviewerId = $('#interviewer').val();
         question = $("#question").val();
         date = $("#datepicker").find("input").val();
         time = $("#timepicker").find("input").val();
         time = getTime(date + " " + time);
         editCandidate();
         flock.close();
     });

     function getTime(dateString) {
        var parts = dateString.match(/(\d{2})\/(\d{2})\/(\d{4}) (\d{2}):(\d{2})/);
        return Date.UTC(+parts[3], parts[2]-1, +parts[1], +parts[4], +parts[5]);
     }

     function getInterviewers(groupId) {
        console.log("Get interviewers for group: " + groupId + ", user: " + userId);
        url = baseUrl + "interviewers?userId=" + userId + "&groupId=" + groupId + "&sequence=1" + "&email=" + email;
        sendAjaxRequest(url, "get", null, function(response) {
            interviewers = JSON.parse(response);
             $.each(interviewers, function (i, interviewer) {
                  $('#interviewer').append($('<option>', {
                      value: interviewer.userId,
                      text : interviewer.name
                  }));
             });
        });
     }

     function getQuestions(role) {
          console.log("Get questions for role: " + role)
          sendAjaxRequest(baseUrl + "questions?role=" + role + "&groupId=" + groupId + "&sequence=2", "get", null, function (response) {
          questions = JSON.parse(response);
          $.each(questions, function (i, question) {
              $('#question').append($('<option>', {
                  value: question.id,
                  text : question.title + " (" + question.level + ")"
              }));
          });

          });
     }

     function editCandidate() {
          collabLink = getInterviewPadUrl();
          payload =
          {
              "email": email,
              "round": {
                  "collab_link": collabLink,
                  "interviewer_id": interviewerId,
                  "question_id": question,
                  "scheduled_time": JSON.stringify(time)
              }
          };
          sendAjaxRequest(baseUrl + "edit", "post", payload, function (response) {
              console.log("successful response: " + JSON.stringify(response));
          });
     }
});


