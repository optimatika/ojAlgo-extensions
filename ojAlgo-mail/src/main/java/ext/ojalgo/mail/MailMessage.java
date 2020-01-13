/*
 * Copyright 1997-2020 Optimatika
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package ext.ojalgo.mail;

import java.util.ArrayList;
import java.util.Properties;

import javax.mail.Authenticator;
import javax.mail.Message.RecipientType;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.ojalgo.netio.ASCII;
import org.ojalgo.type.TypeUtils;

public class MailMessage {

    public static class Template implements Cloneable {

        private final String myFrom;
        private final String mySubject;
        private final StringBuilder myText = new StringBuilder();

        public Template(final String subject, final String from) {

            super();

            mySubject = subject;
            myFrom = from;
        }

        public Template append(final String str) {
            myText.append(str);
            return this;
        }

        public Template appendln(final String str) {
            myText.append(str);
            myText.append(ASCII.LF);
            return this;
        }

        public Template line() {
            myText.append(ASCII.LF);
            return this;
        }

        public Template space() {
            myText.append(ASCII.SP);
            return this;
        }

        public Template tab() {
            myText.append(ASCII.HT);
            return this;
        }

        @Override
        protected Object clone() throws CloneNotSupportedException {
            return super.clone();
        }

        String getFrom() {
            return myFrom;
        }

        String getSubject() {
            return mySubject;
        }

        String getText() {
            return myText.toString();
        }

        String getText(final Object... args) {
            return TypeUtils.format(myText.toString(), args);
        }

    }

    public static final String MAIL_SMTP_AUTH = "mail.smtp.auth";
    public static final String MAIL_SMTP_HOST = "mail.smtp.host";
    public static final String MAIL_SMTP_PASSWORD = "mail.smtp.password";
    public static final String MAIL_SMTP_SSL_ENABLE = "mail.smtp.ssl.enable";
    public static final String MAIL_SMTP_USER = "mail.smtp.user";

    private final ArrayList<String> myBccList = new ArrayList<>();
    private final ArrayList<String> myCcList = new ArrayList<>();
    private final Template myTemplate;
    private Object[] myTextArgs = null;
    private final ArrayList<String> myToList = new ArrayList<>();

    public MailMessage(final String subject, final String from) {

        super();

        myTemplate = new Template(subject, from);
    }

    public MailMessage(final Template aTemplate) {

        super();

        try {
            myTemplate = (Template) aTemplate.clone();
        } catch (final CloneNotSupportedException anException) {
            throw new RuntimeException(anException);
        }
    }

    public MailMessage append(final String str) {
        myTemplate.append(str);
        return this;
    }

    public MailMessage appendln(final String str) {
        myTemplate.appendln(str);
        return this;
    }

    public MailMessage args(final Object... args) {
        myTextArgs = args;
        return this;
    }

    public MailMessage bcc(final String address) {
        myBccList.add(address);
        return this;
    }

    public MailMessage cc(final String address) {
        myCcList.add(address);
        return this;
    }

    public MailMessage line() {
        myTemplate.line();
        return this;
    }

    public boolean send(final Properties properties) {

        final Session tmpSession;

        if (properties.getProperty(MAIL_SMTP_AUTH).equalsIgnoreCase("true")) {

            final Authenticator tmpAuthenticator = new Authenticator() {

                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(properties.getProperty(MAIL_SMTP_USER), properties.getProperty(MAIL_SMTP_PASSWORD));
                }

            };

            tmpSession = Session.getDefaultInstance(properties, tmpAuthenticator);

        } else {

            tmpSession = Session.getDefaultInstance(properties);
        }

        final MimeMessage tmpMessage = new MimeMessage(tmpSession);

        try {

            tmpMessage.setFrom(new InternetAddress(myTemplate.getFrom()));
            for (final String tmpToItem : myToList) {
                tmpMessage.addRecipient(RecipientType.TO, new InternetAddress(tmpToItem));
            }
            for (final String tmpCcItem : myCcList) {
                tmpMessage.addRecipient(RecipientType.CC, new InternetAddress(tmpCcItem));
            }
            for (final String tmpBccItem : myBccList) {
                tmpMessage.addRecipient(RecipientType.BCC, new InternetAddress(tmpBccItem));
            }

            tmpMessage.setSubject(myTemplate.getSubject());
            tmpMessage.setText(this.getText());

            Transport.send(tmpMessage);

        } catch (final AddressException anException) {
            anException.printStackTrace();
            return false;
        } catch (final MessagingException anException) {
            anException.printStackTrace();
            return false;
        }

        return true;
    }

    public boolean send(final String smtp, final String user, final String password, final boolean ssl) {

        final Properties tmpProperties = System.getProperties();

        if ((smtp != null) && (smtp.length() != 0)) {
            tmpProperties.setProperty(MAIL_SMTP_HOST, smtp);
        }
        if ((user != null) && (user.length() != 0)) {
            tmpProperties.setProperty(MAIL_SMTP_AUTH, "true");
            tmpProperties.setProperty(MAIL_SMTP_USER, user);
        } else {
            tmpProperties.setProperty(MAIL_SMTP_AUTH, "false");
        }
        if ((password != null) && (password.length() != 0)) {
            tmpProperties.setProperty(MAIL_SMTP_PASSWORD, password);
        }
        tmpProperties.setProperty(MAIL_SMTP_SSL_ENABLE, Boolean.toString(ssl));

        return this.send(tmpProperties);
    }

    public MailMessage space() {
        myTemplate.space();
        return this;
    }

    public MailMessage tab() {
        myTemplate.tab();
        return this;
    }

    public MailMessage to(final String address) {
        myToList.add(address);
        return this;
    }

    private String getText() {
        if (myTextArgs != null) {
            return myTemplate.getText(myTextArgs);
        } else {
            return myTemplate.getText();
        }
    }

}
