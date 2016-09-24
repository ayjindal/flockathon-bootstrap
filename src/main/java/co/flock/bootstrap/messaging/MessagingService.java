package co.flock.bootstrap.messaging;

import co.flock.bootstrap.database.Candidate;
import co.flock.bootstrap.database.Round;
import co.flock.www.FlockApiClient;
import co.flock.www.model.messages.FlockMessage;
import co.flock.www.model.messages.Message;
import com.google.gson.Gson;
import org.apache.log4j.Logger;

public class MessagingService
{
    private static final Logger _logger = Logger.getLogger(MessagingService.class);

    public void sendCreationMessage(Candidate candidate, Round round, String userToken)
    {
        _logger.debug("sendCreationMessage candidate: " + candidate + "round: " + round);
        Message message = new Message(round.getInterviewerID(), "Please help with this interview");
        String messageJson = new Gson().toJson(message);
        _logger.debug("messageJson: " + messageJson);
        sendMessage(userToken, message);
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
