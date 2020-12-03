package com.bwy;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Iterator;
import java.util.Set;

public class SocketMultiplexingSingleThreadv1 {
    private ServerSocketChannel server = null;
    private Selector selector=null;  // linux多路复用器 （select poll epoll kqueue） nginx event{}
    int port = 19090;

    public void initServer(){
        try {
            // 相当于是打开了一个新的文件描述符fd_listen
            server = ServerSocketChannel.open();
            server.configureBlocking(false);
            server.bind(new InetSocketAddress(port));


            // 如果是epoll ，可能会有一个 epoll_create ， 这个内核态的缓冲区中也有一个文件描述符 fd_k
            selector = Selector.open(); // select poll epoll 会优先选择 epoll
            // server 约等于 listen状态

            // 注册
            /**
             *  如果是select ,poll , 就是在JVM里开辟一个数组，将fd放进去
             *  如果是epoll ,相当于调用 epoll_ctl(fd_k ,ADD ,fd_listen ,EPOLLIN );
             *  将listening的Server也注册到内核中
             */
            server.register(selector, SelectionKey.OP_ACCEPT);

        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    public void start(){
        initServer();
        System.out.println("start ... ");
        try {
            while(true){
                Set<SelectionKey> keys = selector.keys();
                System.out.println(keys.size()+"  size");

                /**
                 *    这里调用了多路复用器（select,poll,epoll） //  相当于epoll_wait的系统调用
                 *
                 *    select()
                 *    1. 如果是 select/poll ,其实就是内核中的 select（fd_listen） / poll(fd_listen)
                 *    2. 如果是 epoll ,就是内核调用的 epoll_wait()
                 *    参数中的时间， 如果没有时间（或者设置为0） ，这个调用就会阻塞 epoll_wait
                 *    有设置timeout，就不会阻塞
                 *
                 */
                while(selector.select(500)>0){
                    // 返回的有状态的fd集合
                    Set<SelectionKey> selectionKeys= selector.selectedKeys();
                    Iterator<SelectionKey> iter=selectionKeys.iterator();

                    // 这里还是需要去由程序来处理IO
                    while(iter.hasNext()){
                        SelectionKey key= iter.next();
                        iter.remove();  // 从set中移除
                        if(key.isAcceptable()){
                            // 如果是接受一个新的连接，就要accept
                            /**
                             *  如果是 select,poll 因为内核中没有空间，需要在JVM中保存和前边的那个fd_listen放在一起
                             *  epoll , 我们需要通过epoll_ctl把新的客户端fd注册到内核空间中
                             */
                            acceptHandler(key);
                        }else if(key.isReadable()){
                            readHandler(key);
                        }
                    }


                }
            }
        }
        catch(IOException e){
            e.printStackTrace();
        }


    }
    public void acceptHandler(SelectionKey key){
        try{
            ServerSocketChannel ssc = (ServerSocketChannel) key.channel();
            SocketChannel client = ssc.accept(); // 调用accept接受客户端 fd_client
            client.configureBlocking(false);

            ByteBuffer buffer= ByteBuffer.allocate(8192);

            //新的client同样要调用register注册
            /**
             *   select, poll : jvm中开辟一个数组，将fd_client放进去
             *   epoll : epoll_ctl(fd_k,ADD，fd_client,EPOLLIN)
             */

            client.register(selector,SelectionKey.OP_READ,buffer);
            System.out.println("---------------");
            System.out.println("new client"+ client.getRemoteAddress());
            System.out.println("---------------");


        }catch (Exception e){
            e.printStackTrace();
        }


    }
    public void readHandler(SelectionKey key){
        ServerSocketChannel ssc = (ServerSocketChannel) key.channel();

        // todo some read stuff
        //
        ByteBuffer buffer = ByteBuffer.allocate(8192);



    }
    public static void main(String[] args) {
        SocketMultiplexingSingleThreadv1 test = new SocketMultiplexingSingleThreadv1();
        test.start();
    }
}
