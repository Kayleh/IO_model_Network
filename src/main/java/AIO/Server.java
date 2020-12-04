package AIO;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.HashMap;
import java.util.Map;

public class Server
{
    private static final String LOCALHOST = "localhost";
    private static final int DEFAULT_PORT = 8888;

    private AsynchronousServerSocketChannel serverChannel;

    private void close(Closeable closeable)
    {
        try
        {
            closeable.close();
        } catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    public void start()
    {
        try
        {
            //绑定端口号,调用的open方法（无参），这个参数类型为AsynchronousChannelGroup，其中包含共享的系统资源，如线程池，
            //因为我们没有传入参数，会从默认的ProviderHolder中，提供一个我们需要的AsynchronousServerSocketChannel对象
            //Handler会在不同的线程中进行处理，如我们的AcceptHandler和ClientHandler，它就是动用的共享资源：线程，来执行
            serverChannel = AsynchronousServerSocketChannel.open();
            serverChannel.bind(new InetSocketAddress(LOCALHOST, DEFAULT_PORT));
            System.out.println("服务器启动成功，监听端口号：" + DEFAULT_PORT);

            while (true)
            {
                //该accept方法为异步调用，没有需要返回的结果也会返回，即没有收到客户连接的请求时
                //就会返回结果了；但是我们要保证在有客户连接的时候，主线程还在运行，否则主线程返回
                //服务器就直接宕机了，我们采用下面的小技巧来避免这种情况

                //accept在系统层面完成的时候（系统帮助我们完成了这个IO处理），返回的结果会被AcceptHandler来处理，
                //成功时执行的completed方法，失败执行的是failed方法
                //附带对象无；AcceptHandler为实现接口CompletionHandler的类，处理accept请求
                serverChannel.accept(null, new AcceptHandler());

                //用这个来避免while过于频繁，相当于将主线程阻塞，以保证建立连接时与客户端的响应
                System.in.read();
            }


        } catch (IOException e)
        {
            e.printStackTrace();
        } finally
        {
            close(serverChannel);
        }
    }

    /**
     * 程序处理accept请求的时候，并不是在主线程中执行的，
     * 而是从AsynchronousChannelGroup中取出另一个线程来执行
     * CompletionHandler泛型为，第一个为返回的结果类型；第二个为附带的对象类型
     */
    private class AcceptHandler implements CompletionHandler<AsynchronousSocketChannel, Object>
    {
        /**
         * completed 该方法对应的是，我们之前上方调用accept方法，正常返回了，那么会执行该方法
         *
         * @param result     方法执行成功的返回值
         * @param attachment 附带信息
         */
        @Override
        public void completed(AsynchronousSocketChannel result, Object attachment)
        {
            if (serverChannel.isOpen())
            {
                //确保服务器还在运行
                //服务器继续等待下一个客户端来请求，但是这里并不是产生过多的accept方法压栈
                //而造成的栈溢出问题，这在底层已经进行保护了
                serverChannel.accept(null, this);
            }

            //处理已连接客户端的读写操作
            AsynchronousSocketChannel clientChannel = result;
            if (clientChannel != null && clientChannel.isOpen())
            {
                ClientHandler handler = new ClientHandler(clientChannel);
                ByteBuffer buffer = ByteBuffer.allocate(1024);

                Map<String, Object> attachmentInfo = new HashMap<>();
                attachmentInfo.put("type", "read");
                attachmentInfo.put("buffer", buffer);

                //依靠ClientHandler异步处理，读写操作，将其回传给客户端
                clientChannel.read(buffer, attachmentInfo, handler);
            }

        }

        @Override
        public void failed(Throwable exc, Object attachment)
        {
            //失败时的调用
        }
    }

    private class ClientHandler implements CompletionHandler<Integer, Map<String, Object>>
    {
        private AsynchronousSocketChannel clientChannel;

        public ClientHandler(AsynchronousSocketChannel clientChannel)
        {
            this.clientChannel = clientChannel;
        }

        @Override
        public void completed(Integer result, Map<String, Object> attachment)
        {
            String type = (String) attachment.get("type");
            ByteBuffer buffer = (ByteBuffer) attachment.get("buffer");
            if ("read".equals(type))
            {
                //已经读取到了客户端传过来的消息，将其回音给客户端
                buffer.flip();//回音要读缓冲区
                attachment.put("type", "write");
                clientChannel.write(buffer, attachment, this);
            } else if ("write".equals(type))
            {
                //将其回传给客户端后，等待客户端的新的信息
                //将这里将再次进行异步调用，读取客户端发来的信息存储在缓冲区中
                ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
                attachment.put("type", "read");
                attachment.put("buffer", byteBuffer);
                clientChannel.read(byteBuffer, attachment, this);
            }
        }

        @Override
        public void failed(Throwable exc, Map<String, Object> attachment)
        {

        }
    }

    public static void main(String[] args)
    {
        Server server = new Server();
        server.start();
    }
}
