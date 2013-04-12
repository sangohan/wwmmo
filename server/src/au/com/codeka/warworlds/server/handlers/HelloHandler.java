package au.com.codeka.warworlds.server.handlers;

import com.google.protobuf.Message;

import au.com.codeka.warworlds.model.protobuf.Messages;
import au.com.codeka.warworlds.server.RequestException;
import au.com.codeka.warworlds.server.RequestHandler;

public class HelloHandler extends RequestHandler {

    @Override
    protected Message put() throws RequestException {
        Messages.HelloRequest pbHelloRequest = getRequestBody(Messages.HelloRequest.class);

        return Messages.HelloResponse.newBuilder()
                .setMotd(Messages.MessageOfTheDay.newBuilder()
                                 .setMessage("<p>Welcome to the new client!!</p>"))
                .build();
    }

    @Override
    protected Message get() throws RequestException {
        return Messages.HelloResponse.newBuilder()
                .setMotd(Messages.MessageOfTheDay.newBuilder()
                                 .setMessage("<p>Welcome to the new client!!</p>")
                                 .setLastUpdate("what?"))
                .build();
    }
}
