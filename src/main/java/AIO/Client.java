package AIO;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class Client {

    private final String LOCALHOST = "localhost";
    private final int DEFAULT_PORT = 8888;

    AsynchronousSocketChannel clientChannel;

    private void close(Closeable closeable){
        try {
            closeable.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void start(){
        try {
            clientChannel = AsynchronousSocketChannel.open();

            Future<Void> connect = clientChannel.connect(new InetSocketAddress(LOCALHOST, DEFAULT_PORT));
            connect.get();//阻塞式调用，直到有结果才返回

            //读取用户的输入
            BufferedReader in = new BufferedReader(new InputStreamReader(System.in));

            while (true){
                String input = in.readLine();
                byte[] inputBytes = input.getBytes();
                ByteBuffer buffer = ByteBuffer.wrap(inputBytes);

                //向服务器发送消息
                Future<Integer> write = clientChannel.write(buffer);
                write.get();

                //接收服务器传来的消息
                buffer.flip();
                Future<Integer> read = clientChannel.read(buffer);
                read.get();

                String s = new String(buffer.array());
                System.out.println(s);

                buffer.clear();
            }


        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }finally {
            close(clientChannel);
        }
    }

    public static void main(String[] args) {
        Client client = new Client();
        client.start();
    }
}
