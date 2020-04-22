import javax.mail.Authenticator;
import javax.mail.PasswordAuthentication;

public class atwebSmtpAuthenticator extends Authenticator {

    String username;
    String password;

    public atwebSmtpAuthenticator(String usr, String pwd) {
        super();
        this.username = usr;
        this.password = pwd;
    }

    @Override
    public PasswordAuthentication getPasswordAuthentication() {

        if ( (this.username.length() > 0) && (this.password.length() > 0)) {

            return new PasswordAuthentication(username, password);
        }

        return null;
    }
}