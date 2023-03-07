
package com.grpc.hogwarts.service;

import io.grpc.Server;
import io.grpc.ServerBuilder;

public class HogwartsServer {

    public static void main(String[] args) throws Exception {
        Server server = ServerBuilder.forPort(8080).addService(new HogwartsServiceImpl()).build();
        server.start();
        System.out.println("Server started");
        server.awaitTermination();

    }

}

