package Hospitality_and_tourism_system;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.*;
import javax.mail.internet.*;
import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class EmailSender {

    
    private static final String SENDER_EMAIL = "denethkawishka2008@gmail.com"; 
    private static final String SENDER_PASSWORD = "tbwaxzasurgpekrv"; // <-- paste new 16-digit password here

    public static void sendBookingEmailWithAttractions(String recipientEmail, String customerName, String invoiceDetails) {
        
     
        Properties prop = new Properties();
        prop.put("mail.smtp.auth", "true");
        prop.put("mail.smtp.starttls.enable", "true");
        prop.put("mail.smtp.starttls.required", "true");
        prop.put("mail.smtp.host", "smtp.gmail.com");
        prop.put("mail.smtp.port", "587");
        prop.put("mail.smtp.ssl.protocols", "TLSv1.2");
        prop.put("mail.smtp.ssl.trust", "smtp.gmail.com");

        Session session = Session.getInstance(prop, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(SENDER_EMAIL, SENDER_PASSWORD);
            }
        });

        // Debug mode enabled to print full SMTP log in NetBeans Output window
        session.setDebug(true);

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(SENDER_EMAIL, "DK Tourism Hotel"));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipientEmail));
            message.setSubject("Booking Confirmation & Travel Guide - DK Tourism Hotel");

            MimeMultipart multipart = new MimeMultipart("related");

       
            StringBuilder htmlBuilder = new StringBuilder();
            htmlBuilder.append("<div style='font-family: Arial, sans-serif; color: #333; max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #e2e8f0; border-radius: 10px;'>")
                       .append("<h2 style='color: #d4af37; text-align: center;'>DK Tourism Hotel & Travels</h2>")
                       .append("<p>Dear <b>").append(customerName).append("</b>,</p>")
                       .append("<p>Thank you very much for choosing our hotel! We are constantly working to provide you with the highest quality service and ensure you have an unforgettable travel experience. Warm wishes for a pleasant stay!</p>")
                       
                       // Invoice Section
                       .append("<div style='background: #f1f5f9; padding: 15px; border-radius: 8px; margin: 20px 0;'>")
                       .append("<h3 style='margin-top:0; color: #1e293b;'>Booking Invoice Details</h3>")
                       .append("<pre style='font-family: monospace; font-size: 13px;'>").append(invoiceDetails).append("</pre>")
                       .append("</div>")

                       // Food & Dining Request Menu Section
                       .append("<div style='background: #fffbeb; padding: 15px; border-radius: 8px; border: 1px solid #fde68a; margin: 20px 0;'>")
                       .append("<h3 style='color: #d97706; margin-top:0;'>🍽️ Food & Dining Request Menu</h3>")
                       .append("<p style='font-size: 13px; color: #78350f;'>During your stay, you can order or request the following delicious meals:</p>")
                       .append("<ul style='font-size: 13px; color: #78350f;'>")
                       .append("<li><b>Sri Lankan Traditional Rice & Curry</b> (Breakfast / Lunch / Dinner)</li>")
                       .append("<li><b>Western & Continental Breakfast Platters</b></li>")
                       .append("<li><b>Fresh Seafood Specialties & BBQ Nights</b></li>")
                       .append("<li><b>Fresh Tropical Fruit Juices & Desserts</b></li>")
                       .append("</ul>")
                       .append("<p style='font-size: 12px; color: #92400e; margin-bottom: 0;'>To place food orders online, please use our guest portal button below or contact Reception.</p>")
                       .append("</div>")

                       // Dynamic Tourism Places Header
                       .append("<h2 style='color: #2563eb; border-bottom: 2px solid #2563eb; padding-bottom: 5px; margin-top: 30px;'>Explore Nearby Tourist Attractions</h2>");

            // Fetch Places from DB
            String dbUrl = "jdbc:mysql://localhost:3306/hospitality_db?allowPublicKeyRetrieval=true&useSSL=false";
            List<String[]> placesList = new ArrayList<>();

            try (Connection conn = DriverManager.getConnection(dbUrl, "root", "deneth");
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT place_name, description, map_url, image_path FROM tourism_places")) {

                while (rs.next()) {
                    placesList.add(new String[]{
                        rs.getString("place_name"),
                        rs.getString("description"),
                        rs.getString("map_url"),
                        rs.getString("image_path")
                    });
                }
            } catch (Exception dbEx) {
                System.out.println("Warning: Tourism places DB error: " + dbEx.getMessage());
            }

            int imgCount = 0;
            for (String[] place : placesList) {
                imgCount++;
                String pName = place[0];
                String desc = place[1];
                String mapUrl = place[2];
                String imgPath = place[3];
                String cid = "img_" + imgCount;

                htmlBuilder.append("<div style='margin-bottom: 25px; border-bottom: 1px solid #e2e8f0; padding-bottom: 20px;'>")
                           .append("<h3 style='color: #1e293b; margin-bottom: 5px;'>").append(pName).append("</h3>")
                           .append("<p style='color: #475569; font-size: 14px; line-height: 1.5;'>").append(desc).append("</p>");

                if (imgPath != null && !imgPath.isEmpty() && new File(imgPath).exists()) {
                    htmlBuilder.append("<img src='cid:").append(cid).append("' style='max-width:100%; height:auto; border-radius:8px; box-shadow: 0 4px 6px rgba(0,0,0,0.1); margin-top: 10px;'><br/><br/>");
                }

                if (mapUrl != null && !mapUrl.isEmpty()) {
                    htmlBuilder.append("<a href='").append(mapUrl)
                               .append("' style='background: #2563eb; color: white; padding: 10px 16px; text-decoration: none; border-radius: 5px; font-weight: bold; display: inline-block;'>📍 View on Google Maps</a>");
                }
                htmlBuilder.append("</div>");
            }

            // Feedback & Web Portal Link (Live Domain)
            htmlBuilder.append("<div style='background: #f8fafc; padding: 20px; border-radius: 8px; text-align: center; margin-top: 30px; border: 1px solid #cbd5e1;'>")
                       .append("<h3 style='color: #0f172a; margin-top:0;'>⭐ Rate Your Experience & Place Orders</h3>")
                       .append("<p style='font-size: 14px; color: #475569;'>We value your feedback! Please take a moment to rate your stay, submit comments, or place food orders online:</p>")
                       .append("<div style='font-size: 28px; letter-spacing: 6px; margin: 10px 0;'>")
                       .append("<span style='color: #eab308;'>★</span><span style='color: #eab308;'>★</span><span style='color: #eab308;'>★</span><span style='color: #eab308;'>★</span><span style='color: #eab308;'>★</span>")
                       .append("</div>")
                       .append("<p style='font-size: 13px; color: #64748b; margin-bottom: 20px;'>Click the button below to open our online guest portal:</p>")
                       .append("<a href='http://dk-hotel.page.gd' style='background: linear-gradient(to right, #2563eb, #1d4ed8); color: white; padding: 12px 24px; text-decoration: none; border-radius: 6px; font-weight: bold; font-size: 14px; display: inline-block;'>🌐 Open Feedback & Order Portal</a>")
                       .append("</div>");

            htmlBuilder.append("<p style='margin-top: 30px; text-align: center; color: #64748b; font-size: 14px;'>Thank you for choosing DK Tourism Hotel!<br/><b>DK Tourism Hotel Management Team</b></p>")
                       .append("</div>");

            MimeBodyPart htmlBodyPart = new MimeBodyPart();
            htmlBodyPart.setContent(htmlBuilder.toString(), "text/html; charset=utf-8");
            multipart.addBodyPart(htmlBodyPart);

            // Attach Inline Images
            int attachCount = 0;
            for (String[] place : placesList) {
                attachCount++;
                String imgPath = place[3];
                if (imgPath != null && !imgPath.isEmpty() && new File(imgPath).exists()) {
                    MimeBodyPart imagePart = new MimeBodyPart();
                    DataSource fds = new FileDataSource(imgPath);
                    imagePart.setDataHandler(new DataHandler(fds));
                    imagePart.setHeader("Content-ID", "<img_" + attachCount + ">");
                    multipart.addBodyPart(imagePart);
                }
            }

            message.setContent(multipart);
            
            // Send email
            Transport.send(message);
            System.out.println("✅ SUCCESS: Email sent successfully to " + recipientEmail);

        } catch (Exception e) {
            System.err.println("❌ ERROR: Email failed to send!");
            e.printStackTrace();
        }
    }
}