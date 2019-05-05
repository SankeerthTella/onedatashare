package org.onedatashare.server.service;

import org.codehaus.jackson.map.ObjectMapper;
import org.onedatashare.server.model.ticket.RedmineResponse;
import org.onedatashare.server.model.ticket.SupportTicket;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * SupportTicketService is a service class that accepts the captured request from SupportTicketController
 * and connects to the Redmine server to create a ticket, returning the generated ticker number.
 *
 * Reference used for creating POST request in Java - https://www.mkyong.com/java/how-to-send-http-request-getpost-in-java/
 *
 * @author Linus Castelino
 * @version 1.0
 * @since 05-03-2019
 */
@Service
public class SupportTicketService {

    @Value("${redmine.server.url}")
    private String REDMINE_SERVER_ISSUES_URL;

    // Redmine account auth key through which tickets will be created
    private String REDMINE_AUTH_KEY = System.getenv("REDMINE_AUTH_KEY");

    private final String REQUEST_METHOD = "POST";
    private final String CONTENT_TYPE = "application/json";
    private final String CHARACTER_ENCODING = "utf-8";

    private ObjectMapper objectMapper = new ObjectMapper();

    /**
     * This method creates an http connection with the Redmine server, creates a ticket using the values passed
     * by the controller and returns the ticket number generated by Redmine server.
     *
     * @param supportTicket - Object containing request values
     * @return ticketNumber - An integer value returned by Redmine server after generating the ticket
     */
    public Mono<Integer> createSupportTicket(SupportTicket supportTicket){

        try {
            URL urlObj = new URL(REDMINE_SERVER_ISSUES_URL + "?key=" + REDMINE_AUTH_KEY);
            HttpURLConnection conn = (HttpURLConnection) urlObj.openConnection();

            conn.setRequestMethod(REQUEST_METHOD);
            conn.setRequestProperty("Content-Type", CONTENT_TYPE + "; " + CHARACTER_ENCODING);
            conn.setRequestProperty("Accept", CONTENT_TYPE);
            conn.setDoOutput(true);

            String jsonBody =  supportTicket.getRequestString();
            DataOutputStream outputStream = new DataOutputStream(conn.getOutputStream());
            outputStream.writeBytes(jsonBody);
            outputStream.flush();
            outputStream.close();

            int responseCode = conn.getResponseCode();
            if(responseCode == HttpURLConnection.HTTP_CREATED){
                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));

                String input = null;
                StringBuffer response = new StringBuffer();
                while((input = br.readLine()) != null)
                    response.append(input);

                br.close();

//                System.out.println(response);
                RedmineResponse responseObj = objectMapper.readValue(response.toString(), RedmineResponse.class);
                return Mono.just(responseObj.getTicketId());
            }
            else{
                // Support ticket was not created by Redmine due to some error
                System.out.println("An error occurred while trying to create a support ticket");
                System.out.println(conn.getResponseMessage());

            }
        }
        catch(MalformedURLException mue){
            System.out.println("Exception occurred while creating URL object");
            mue.printStackTrace();
        }
        catch(IOException ioe){
            System.out.println("Exception occurred while opening or reading from a connection with " + REDMINE_SERVER_ISSUES_URL);
            ioe.printStackTrace();
        }
        catch (Exception e){
            System.out.println("General exception occurred while trying to create a support ticket");
            e.printStackTrace();
        }

        return Mono.error(new Exception("Error occurred while trying to create a support ticket"));
    }    // createSupportTicket()

}    //class