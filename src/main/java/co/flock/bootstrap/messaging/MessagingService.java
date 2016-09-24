package co.flock.bootstrap.messaging;

import co.flock.bootstrap.database.Candidate;
import co.flock.bootstrap.database.Round;
import co.flock.bootstrap.database.User;
import co.flock.www.FlockApiClient;
import co.flock.www.model.messages.Attachments.*;
import co.flock.www.model.messages.FlockMessage;
import co.flock.www.model.messages.Message;
import com.google.gson.Gson;
import org.apache.log4j.Logger;

import java.util.regex.Pattern;

public class MessagingService
{
    private static final Logger _logger = Logger.getLogger(MessagingService.class);
    private static final Pattern _whitespacePattern = Pattern.compile("\\s+");
    private static final String BOT_TOKEN = "c0532cdb-b59a-4605-94b5-cb8f244f02d3";

    public void sendCreationMessage(Candidate candidate, Round round, User user, User interviewer)
    {
        _logger.debug("sendCreationMessage candidate: " + candidate + "round: " + round);
        Message message = new Message(candidate.getGroupId(), "@" +
                getTrimmedName(interviewer.getName()) + " Please help with this interview");
        message.setFlockml("<flockml><user userId=\"" + round.getInterviewerID() + "\">@" +
                getTrimmedName(interviewer.getName()) + "</user> Please help with this interview</flockml>");
        WidgetView widgetView = new WidgetView();
        String widgetUrl = round.getCollabLink() + "&email=" + candidate.getEmail();
        widgetView.setSrc(widgetUrl);
        Attachment attachment = new Attachment();

        Button[] buttons = new Button[1];
        buttons[0] = new Button();
        buttons[0].setName("View");
        Action action = new Action();
        action.addOpenWidget(widgetUrl, "modal", "modal");
        buttons[0].setAction(action);
        attachment.setButtons(buttons);

        View view = new View();
        view.setWidget(widgetView);
        view.setFlockml("<flockml> Name: " + candidate.getName() + "<br />" + "Collabedit Link: " + round.getCollabLink() + "</flockml>");
        attachment.setViews(view);
        message.setAttachments(new Attachment[]{attachment});
        message.setMentions(new String[]{round.getInterviewerID()});
        String messageJson = new Gson().toJson(message);
        _logger.debug("messageJson: " + messageJson);
        sendMessage(user.getToken(), message);
    }

    public void sendRoundEndedMessage(Candidate candidate, User interviewer, String verdict)
    {
        _logger.debug("sendRoundEndedMessage candidate: " + candidate + " interviewer: " + interviewer);
        sendMessageToCreator(candidate, verdict);
        sendMessageToInterviewer(candidate, interviewer, verdict);
    }

    private void sendMessageToInterviewer(Candidate candidate, User interviewer, String verdict)
    {
        Message message = new Message(interviewer.getId(),
                "Thank you for taking the interview for" + candidate.getName());
        String messageJson = new Gson().toJson(message);
        _logger.debug("messageJson: " + messageJson);
        sendMessage(BOT_TOKEN, message);
    }

    private void sendMessageToCreator(Candidate candidate, String verdict)
    {
        Message message = new Message(candidate.getCreatorId(),
                "Interview ended for " + candidate.getName() +" Verdict: " + verdict);
        String messageJson = new Gson().toJson(message);
        _logger.debug("messageJson: " + messageJson);
        sendMessage(BOT_TOKEN, message);
    }

    public static String getTrimmedName(String senderName)
    {
        String contactName;
        senderName = senderName.trim();
        String[] nameParts = _whitespacePattern.split(senderName);
        contactName = nameParts[0];
        if (nameParts.length > 1 && !nameParts[1].isEmpty()) {
            contactName += " " + nameParts[1].charAt(0);
        }
        return contactName;
    }

    private static void sendMessage(String token, Message message)
    {
        _logger.debug("Sending message to  : " + message.getTo() + " text : " + message.getText());
        FlockMessage flockMessage = new FlockMessage(message);
        FlockApiClient flockApiClient = new FlockApiClient(token);
        try {
            String responseBody = flockApiClient.chatSendMessage(flockMessage);
            _logger.debug("responseBody: " + responseBody);
        } catch (Exception e) {
            _logger.error("Failed to send message: ", e);
        }
    }
}
