package com.quas.payment.payment.api;

import com.quas.payment.payment.infrastructure.TestEventProducer;
import jakarta.inject.Inject;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

@Path("/test/kafka")
public class TestKafkaResource {

    @Inject
    TestEventProducer producer;

    @POST
    public Response send() {
        producer.send("Hello from Quas Payment!");
        return Response.accepted().build();
    }
}
