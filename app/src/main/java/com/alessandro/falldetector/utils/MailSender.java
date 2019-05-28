package com.alessandro.falldetector.utils;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;

import android.content.SharedPreferences;
import android.location.Location;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import com.alessandro.falldetector.R;
import com.alessandro.falldetector.SettingFragment;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Properties;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class MailSender {
    //fall detector notification mail account
    private String username = "forzaitalia0192@gmail.com";
    private String password = "ale10ric";
    private String name;
    private Context mcontext;
    private SharedPreference sharedPreference;

    public MailSender(Context context) {
        mcontext = context;
        sharedPreference = new SharedPreference();
    }

    public void sendMail(Location location) {
        Session session = createSessionObject();

        List<Contact> contacts = sharedPreference.getContacts(mcontext);
        if(contacts == null || contacts.isEmpty())
            return;
        String messageBody = getMessageBody(location);
        String subject = "Fall detected";
        try {
            Message message = createMessage(contacts, subject, messageBody, session);
            new SendMailTask().execute(message);
        } catch (AddressException e) {
            e.printStackTrace();
        } catch (MessagingException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    private String getMessageBody(Location location) {
        // se non si accede alle impostazioni il file falldetector_preferences non viene generato
        // quindi non la stringa smsMessage è nulla come valore di default causando il crash probabilmente
        // ho aggiunto un controllo nell' else
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mcontext);
        String smsMessage = sharedPreferences.getString(SettingFragment.KEY_SMS_MESSAGE, null);
        Boolean userWantLocation = sharedPreferences.getBoolean("pref_location_enabled", false);
        name = sharedPreferences.getString("pref_user_name", "User");

        if (userWantLocation && location != null) {
            String lat = String.valueOf(location.getLatitude());
            String lon = String.valueOf(location.getLongitude());
            return smsMessage + " Open this link to find the fall position: " + "www.google.it/maps/place/" + lat + "," + lon + "/" + "@" + lat + "," + lon + ",15z ";

        } else {
            if(smsMessage == null){
                return "Fall Detector Application has detect a fall!";
            }
            return smsMessage;
        }
    }

    private Message createMessage(List<Contact> contacts, String subject, String messageBody, Session session) throws MessagingException, UnsupportedEncodingException {
        Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress(username, name));
        boolean first = true;
        boolean cc = false;
        InternetAddress[] ccAddress = new InternetAddress[contacts.size() - 1];
        int i = 0;
        for (Contact contact : contacts) {
            if(first) {
                message.setRecipient(Message.RecipientType.TO, new InternetAddress(contact.getNumber(), contact.getName()));
                first = false;
            } else{
                ccAddress[i] = new InternetAddress(contact.getNumber(), contact.getName());
                i++;
                cc = true;
            }
        }
        if(cc)
            message.setRecipients(Message.RecipientType.CC, ccAddress);

        message.setSubject(subject);
        message.setText(messageBody);
        return message;
    }

    private Session createSessionObject() {
        Properties properties = new Properties();
        properties.put("mail.smtp.auth", "true");
        properties.put("mail.smtp.starttls.enable", "true");
        properties.put("mail.smtp.host", "smtp.gmail.com");
        properties.put("mail.smtp.port", "587");

        return Session.getInstance(properties, new javax.mail.Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        });
    }

    private class SendMailTask extends AsyncTask<Message, Void, Void> {
        private String error;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            error = null;
            NotificationManager mNotificationManager = (NotificationManager) mcontext.getSystemService(Context.NOTIFICATION_SERVICE);

            Notification.Builder builder = new Notification.Builder(mcontext)
                    .setSmallIcon(R.drawable.ic_fall_app_icon_green)
                    .setContentTitle("Fall detector email service")
                    .setContentText("Sendig the email");

            Notification notification = builder.build();

            mNotificationManager.notify(13, notification);

        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            NotificationManager mNotificationManager = (NotificationManager) mcontext.getSystemService(Context.NOTIFICATION_SERVICE);
            if (error != null) {
                Notification.Builder builder = new Notification.Builder(mcontext)
                        .setSmallIcon(R.drawable.ic_fall_app_icon_green)
                        .setContentTitle("Error sending email")
                        .setContentText(error);

                Notification notification = builder.build();

                mNotificationManager.notify(13, notification);
            } else {
                mNotificationManager.cancel(13);
            }
        }

        @Override
        protected Void doInBackground(Message... messages) {
            try {
                Transport.send(messages[0]);
            } catch (MessagingException e) {
                e.printStackTrace();
                error = e.toString();
            }
            return null;
        }
    }
}
