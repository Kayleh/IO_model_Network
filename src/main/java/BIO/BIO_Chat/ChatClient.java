package BIO.BIO_Chat;

import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * @Author: Kayleh
 * @Date: 2020/11/27 15:33
 */
public class ChatClient
{
    private String DEFAULT_SERVER_HOST = "127.0.0.1";
    private int DEFAULT_PORT = 8888;
    private final String QUIT = "quit";

    private Socket socket;
    private BufferedReader reader;
    private BufferedWriter writer;

    public void sendMsg(String msg) throws IOException
    {
        if (!socket.isOutputShutdown())
        {
            writer.write(msg + "\n");
            writer.flush();
        }
    }

    public String receviceMsg() throws IOException
    {
        if (!socket.isInputShutdown())
        {
            return reader.readLine();
        }
        return null;
    }

    //检查是否退出
    public boolean readyToQuit(String msg)
    {
        return QUIT.equals(msg);
    }

    //关闭
    public void close()
    {
        try
        {
            writer.close();
        } catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    public void start()
    {
        try
        {
            socket = new Socket(DEFAULT_SERVER_HOST, DEFAULT_PORT);
            writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            new Thread(new UserInputHandler(this)).start();

            String msg = null;
            while ((msg = receviceMsg()) != null)
            {
                System.out.println(msg);
            }
        } catch (UnknownHostException e)
        {
            e.printStackTrace();
        } catch (IOException e)
        {
            e.printStackTrace();
        } finally
        {
            close();
        }
    }

    public static void main(String[] args)
    {
        ChatClient chatClient = new ChatClient();
        chatClient.start();
    }
}
