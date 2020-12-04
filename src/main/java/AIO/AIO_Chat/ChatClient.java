package AIO.AIO_Chat;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * @Author: Kayleh
 * @Date: 2020/12/4 22:52
 */
public class ChatClient
{
    private static final String LOCALHOST = "localhost";
    private static final int DEFAULT_PORT = 8888;
    private final String QUIT = "quit";
    private final int BUFFER = 1024;

    private String host;
    private int port;
    private AsynchronousSocketChannel clientChannel;
    private Charset charset = StandardCharsets.UTF_8;

    public ChatClient()
    {
        this(LOCALHOST, DEFAULT_PORT);
    }

    public ChatClient(String host, int port)
    {
        this.port = port;
        this.host = host;
    }

    public void start()
    {
        try
        {
            clientChannel = AsynchronousSocketChannel.open();
            Future<Void> connect = clientChannel.connect(new InetSocketAddress(host, port));
            connect.get();
            System.out.println("与服务已成功建立连接");
            new Thread(new UserInputHandler(this)).start();
        } catch (IOException | InterruptedException | ExecutionException e)
        {
            e.printStackTrace();
        } finally
        {
            close(clientChannel);
        }
    }

    public void sendMsg(String msg)
    {
        if (msg.isEmpty())
        {
            return;
        } else
        {
            ByteBuffer buffer = charset.encode(msg);
            Future<Integer> write = clientChannel.write(buffer);
            try
            {
                write.get();
            } catch (InterruptedException | ExecutionException e)
            {
                e.printStackTrace();
            }
        }
    }

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

    public boolean readyToQuit(String msg)
    {
        return QUIT.equals(msg);
    }

    public static void main(String[] args)
    {
        ChatClient chatClient = new ChatClient();
        chatClient.start();
    }


}
