package lostandfound.config.utils;
import io.github.cdimascio.dotenv.Dotenv;
import io.vertx.core.Vertx;
import io.vertx.ext.mail.MailClient;
import io.vertx.ext.mail.MailConfig;
import io.vertx.ext.mail.MailMessage;
import io.vertx.ext.mail.StartTLSOptions;



public class MailUtil {
    private static final MailClient mailClient;
    private static final String senderAddress;

    static {
        Dotenv dotenv = Dotenv.load();

        String username = dotenv.get("MAIL_USERNAME");
        String password = dotenv.get("MAIL_PASSWORD");
        String senderName = dotenv.get("MAIL_SENDER_NAME");

        senderAddress = senderName + " <" + username + ">";

        MailConfig config = new MailConfig()
                .setHostname("smtp.gmail.com")
                .setPort(587)
                .setStarttls(StartTLSOptions.REQUIRED)
                .setUsername(username)
                .setPassword(password);

        mailClient = MailClient.createShared(Vertx.vertx(), config, "mailPool");
    }


    public static void sendVerificationEmail( String to, String token) {
        String verifyLink = "http://localhost:8888/api/auth/verify/" + token;

        MailMessage message = new MailMessage()
                .setFrom(senderAddress)
                .setTo(to)
                .setSubject("Verify your email")
                .setText("Click the link below to verify your email:\n\n"
                        + verifyLink +
                        "\n\nNote: This link is valid for only 10 minutes. ");

        mailClient.sendMail(message, result -> {
            if (result.succeeded()) {
                System.out.println("Verification email sent to: " + to);
            } else {
                System.out.println("Failed to send email: " + result.cause().getMessage());
            }
        });
    }

    public static void sendContactMessage( String to, String from, String itemTitle, String userMessage) {
        MailMessage message = new MailMessage()
                .setFrom(senderAddress)
                .setTo(to)
                .setSubject("Someone responded to your Lost & Found post")
                .setText("Message from: " + from + "\n\n" +
                        "Regarding: " + itemTitle + "\n\n" +
                        "Message:\n" + userMessage);

        mailClient.sendMail(message, result -> {
            if (result.succeeded()) {
                System.out.println("Contact email sent successfully.");
            } else {
                System.err.println("Failed to send contact email: " + result.cause());
            }
        });
    }
    public static void sendForgotPasswordEmail( String to, String token) {
        String resetLink = "http://localhost:4200/reset?token=" + token;

        MailMessage message = new MailMessage()
                .setFrom(senderAddress)
                .setTo(to)
                .setSubject("Reset your password")
                .setText("You requested a password reset. Click the link below to reset your password:\n\n"
                        + resetLink +
                        "\n\nNote: This link is valid for only 10 minutes. "
                        + "If it expires, please request a new one.");

        mailClient.sendMail(message, result -> {
            if (result.succeeded()) {
                System.out.println("Password reset email sent to: " + to);
            } else {
                System.err.println("Failed to send password reset email: " + result.cause().getMessage());
            }
        });
    }
}


