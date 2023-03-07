package com.grpc.hogwarts.client;

import com.google.protobuf.Any;
import com.google.protobuf.StringValue;
import com.grpc.hogwarts.service.Data;
import io.grpc.*;
import com.grpc.hogwarts.service.HogwartsServiceGrpc;
import io.grpc.stub.StreamObserver;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;


public class HogwartsClient {

    private HogwartsServiceGrpc.HogwartsServiceStub stub;
    StreamObserver<Data> requestObserver;
    private String clientUUID = "0001wso2";
    public HogwartsClient(Channel channel){
        stub = HogwartsServiceGrpc.newStub(channel);
    }
    public static void main(String[] args) {
        ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", 8080).usePlaintext().build();
        HogwartsClient client = new HogwartsClient(channel);

        client.connect();
    }


    public void connect() {
        StreamObserver<Data> responseObserver = new StreamObserver<>() {
            @Override
            public void onNext(Data data) {
                Any any = data.getData();
                if (any.is(StringValue.class)) {
                    try {
                        String receivedStr = any.unpack(StringValue.class).getValue();
                        if (receivedStr.equals("Connection Established")){
                            System.out.println("Client: " + receivedStr);
                        }else if(receivedStr.contains("sold")){ //Update this item as sold
                            if (statusUpdate(receivedStr.split(" ")[1])){
                                System.out.println("Client: Updated Successfully!");
                                Data response = Data.newBuilder().setData(Any.pack(StringValue.of( "Item updated as sold.."))).build();
                                requestObserver.onNext(response);
                            }
                        }
                        else{
                            ArrayList<ArrayList<Object>> items = dataReader();
                            for(int i = 0; i<items.size(); i++){
                                if (items.get(i).get(0) == receivedStr && items.get(i).get(2) == "Available"){
                                    Data response = Data.newBuilder().setData(Any.pack(StringValue.of( receivedStr + "; item available in "+ clientUUID))).build();
                                    requestObserver.onNext(response);
                                    break;
                                }
                            }
                        }
                    }catch (IOException | ParseException ex) {
                        throw new RuntimeException(ex);
                    }
                }
            }


            @Override
            public void onError(Throwable throwable) {
                throwable.printStackTrace();
            }

            @Override
            public void onCompleted() {
                System.out.println("Client completed!");
            }

        };

        requestObserver = stub.connect(responseObserver);
        try {

            Data data = Data.newBuilder().setData(Any.pack(StringValue.of(clientUUID))).build() ;
            requestObserver.onNext(data);
//            while(true){
//
//            }
            requestObserver.onCompleted();
        }
        catch (RuntimeException e) {
            requestObserver.onError(e);
            throw e;
        }
    }

    private ArrayList<ArrayList<Object>> dataReader() throws IOException, ParseException {
        ArrayList<ArrayList<Object>> jsonData = new ArrayList<>();
        ArrayList<Object> jsonTempData = new ArrayList<>();
        JSONArray jsonArray = (JSONArray) new JSONParser().parse(new FileReader("items.json"));
        for (int i = 0; i < jsonArray.size(); i++) {
            JSONObject jsonObject = (JSONObject) jsonArray.get(i);
            jsonTempData.set(0, jsonObject.get("itemName"));
            jsonTempData.set(1, jsonObject.get("quantity"));
            jsonTempData.set(2, jsonObject.get("status"));
            jsonData.add(jsonTempData);
        }
        return jsonData;
    }

    private Boolean statusUpdate(String itemName) throws IOException, ParseException {
        Boolean updateFlag = false;
        JSONArray jsonArray = (JSONArray) new JSONParser().parse(new FileReader("items.json"));
        for (int i = 0; i < jsonArray.size(); i++) {
            JSONObject jsonObject = (JSONObject) jsonArray.get(i);
            if (jsonObject.get("itemName").equals(itemName)){
                jsonObject.put("status", "sold");
                updateFlag = true;
            }
        }
        return updateFlag;
    }
}
