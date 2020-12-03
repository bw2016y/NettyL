package com.bwy;

import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BioTest {
    public static void main(String[] args) {
        ExecutorService executorService= Executors.newFixedThreadPool(10);
        try {
            ServerSocket server  = new ServerSocket(6666);
            System.out.println("server start...");
            while(true){
                final Socket socket = server.accept();
                System.out.println("client start...");
                executorService.execute(new Runnable() {
                    @Override
                    public void run() {
                        handler(socket);
                    }
                });
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public static void handler(Socket socket){
         byte[] bytes=new byte[1024];
        try {
            InputStream inputStream=socket.getInputStream();
            while (true){
                int read = inputStream.read(bytes);
                if(read!=-1){
                    System.out.println(Thread.currentThread().getId()+"----->"+ new String(bytes,0,read));
                }else {
                    break;
                }
           }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
