package co.flock.bootstrap.mail;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

public class MailServer {

    private static final String USER = "hyre.flockapp@gmail.com";
    private static final String PASSWORD = "qwedsa1234";

    private static final String SUBJECT = "Your Interview Details";

    public static int sendEmail(String emailAddress, String name, Date dateTime, String collabeditLink) {
        Session session = getSendMailSession();
        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress("hyre.flockapp@gmail.com"));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(emailAddress));
            message.setSubject(SUBJECT);
            message.setText("Dear " + name + ",\n\n" +
                    "Your interview has been scheduled on " + getDate(dateTime) + " at " + getTime(dateTime) + ".\n" +
                    "You will collaborate with the interviewer here: " + collabeditLink + "\n\n" +
                    "All the best!" + "\n\n" +
                    "Regards,\nTeam Hyre");
            Transport.send(message);
        } catch (MessagingException e) {
            System.out.println("Could not send email: " + e.getMessage());
            return -1;
        }
        return 0;
    }

    private static String getDate(Date dateTime) {
        SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy");
        return formatter.format(dateTime);
    }

    private static String getTime(Date dateTime) {
        SimpleDateFormat formatter = new SimpleDateFormat("HH:mm");
        return formatter.format(dateTime);
    }

    private static Session getSendMailSession() {
        return Session.getInstance(getGmailSMTPProperties(),
                new javax.mail.Authenticator() {
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(USER, PASSWORD);
                    }
                });
    }

    private static Properties getGmailSMTPProperties() {
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");
        return props;
    }

    public static void main(String args[]) {
        sendEmail("bharatsinghvi.1988@gmail.com", "Bharat Singhvi", new Date(), "http://collabedit.com/12sjh");
    }
}
