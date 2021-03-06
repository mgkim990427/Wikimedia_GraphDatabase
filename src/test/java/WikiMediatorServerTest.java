package cpen221.mp3;

import com.google.gson.Gson;
import cpen221.mp3.server.Request;
import cpen221.mp3.server.Response;
import cpen221.mp3.server.WikiMediatorServer;
import cpen221.mp3.wikimediator.WikiMediator;
import fastily.jwiki.core.Wiki;
import org.junit.Assert;
import org.junit.Test;

import java.io.*;
import java.net.Socket;
import java.util.logging.SocketHandler;

public class WikiMediatorServerTest {

    public final int PORT_NUMBER = 8080;


    public WikiMediatorServerTest() throws IOException {
    }

    @Test
    public void testRequest() {
        Request request = new Request("1", "simpleSearch", "", "Canada", 10, "", 0, null, null);

        Assert.assertEquals("1", request.getId());
        Assert.assertEquals("simpleSearch", request.getType());
        Assert.assertEquals("", request.getTimeout());
        Assert.assertEquals("Canada", request.getQuery());
        Assert.assertEquals(10, request.getLimit());
        Assert.assertEquals("", request.getPageTitle());
        Assert.assertEquals(0, request.getHops());
    }

    @Test
    public void testResponse() {
        Response response = new Response("1", "success", "Invalid");

        Assert.assertEquals("1", response.getId());
        Assert.assertEquals("simpleSearch", response.getStatus());
        Assert.assertEquals("Invalid", response.getResponse());
    }

    @Test
    public void testWikiMediatorServerConstructor() throws IllegalArgumentException {
        WikiMediatorServer wms = new WikiMediatorServer(30, 2);
        WikiMediatorServer wms1 = new WikiMediatorServer(0, 100);
        WikiMediatorServer wms2 = new WikiMediatorServer(1000, 0);

        try {
            WikiMediatorServer wms3 = new WikiMediatorServer(100000, 2);
        } catch (IllegalArgumentException ioe) {
            ioe.printStackTrace();
        }
    }

    @Test
    public void testWikiMediatorServer() throws IOException {


        try {
            WikiMediatorServer wms = new WikiMediatorServer(PORT_NUMBER, 2);
            //WikiMediatorServerTest wmst = new WikiMediatorServerTest();
            Request request = new Request("1", "simpleSearch", "", "Canada", 10, "", 0, null, null);

            Gson gson = new Gson();
            Socket socket = new Socket("localhost", PORT_NUMBER);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));

            out.print(gson.toJson(request));
            out.flush();
            System.out.println(in.readLine());

            in.close();
            out.close();
            socket.close();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }
}
