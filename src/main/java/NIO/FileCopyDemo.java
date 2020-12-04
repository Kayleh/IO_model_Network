package NIO;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

interface FileCopyRunner
{
    void copyFile(File source, File target);
}

public class FileCopyDemo
{
    private static void close(Closeable closeable)
    {
        if (closeable != null)
        {
            try
            {
                closeable.close();
            } catch (IOException e)
            {
                e.printStackTrace();
            }
        }
    }

    //不使用任何缓冲的留的拷贝
    private static FileCopyRunner noBufferStreamCopy = new FileCopyRunner()
    {
        @Override
        public void copyFile(File source, File target)
        {
            InputStream fin = null;
            OutputStream fout = null;
            try
            {
                fin = new FileInputStream(source);
                fout = new FileOutputStream(target);
                int result;
                while ((result = fin.read()) != -1)
                {
                    fout.write(result);
                }
            } catch (FileNotFoundException e)
            {
                e.printStackTrace();
            } catch (IOException e)
            {
                e.printStackTrace();
            } finally
            {
                close(fin);
                close(fout);
            }
        }
    };

    //使用缓冲区的流的拷贝
    private static FileCopyRunner bufferStreamCopy = new FileCopyRunner()
    {
        @Override
        public void copyFile(File source, File target)
        {
            InputStream fin = null;
            OutputStream fout = null;
            try
            {
                fin = new FileInputStream(source);
                fout = new FileOutputStream(target);
                //创建缓冲区
                byte[] buffer = new byte[1024];
                int result;
                while ((result = fin.read(buffer)) != -1)
                {
                    //result这里表示从中读出来的具体字节数
                    //虽然缓冲区中能缓存1024，但是我们读取的时候不一定就有这么多字节
                    //所以我们使用result做下面的参数
                    fout.write(buffer, 0, result);
                }
            } catch (FileNotFoundException e)
            {
                e.printStackTrace();
            } catch (IOException e)
            {
                e.printStackTrace();
            } finally
            {
                close(fin);
                close(fout);
            }
        }
    };

    //使用带有缓冲区的channel复制 nio
    private static FileCopyRunner nioBufferCopy = new FileCopyRunner()
    {
        @Override
        public void copyFile(File source, File target)
        {
            FileChannel fin = null;
            FileChannel fout = null;

            try
            {
                fin = new FileInputStream(source).getChannel();
                fout = new FileOutputStream(target).getChannel();
                ByteBuffer byteBuffer = ByteBuffer.allocate(1024);

                while (fin.read(byteBuffer) != -1)
                {
                    byteBuffer.flip();//转变为读模式
                    while (byteBuffer.hasRemaining())
                    {
                        fout.write(byteBuffer);
                    }
                    byteBuffer.clear();//转变为写模式
                }
            } catch (FileNotFoundException e)
            {
                e.printStackTrace();
            } catch (IOException e)
            {
                e.printStackTrace();
            } finally
            {
                close(fin);
                close(fout);
            }
        }
    };

    //使用没有缓冲区的channel复制文件
    private static FileCopyRunner nioTransferCopy = ((source, target) ->
    {
        FileChannel fin = null;
        FileChannel fout = null;

        try
        {
            fin = new FileInputStream(source).getChannel();
            fout = new FileOutputStream(target).getChannel();

            long transferred = 0L;
            long size = fin.size();
            while (transferred != size)
            {
                //如果拷贝的大小没有达到源文件的大小就要一直拷贝
                transferred += fin.transferTo(0, size, fout);
            }
        } catch (FileNotFoundException e)
        {
            e.printStackTrace();
        } catch (IOException e)
        {
            e.printStackTrace();
        } finally
        {
            close(fin);
            close(fout);
        }
    });

    public static void main(String[] args)
    {
        File source = new File("J:\\StudySpace\\Java秒杀系统方案优化-高性能高并发实战\\project.zip");
        File target = new File("J:\\StudySpace\\Java秒杀系统方案优化-高性能高并发实战\\p1.zip");
        File target2 = new File("J:\\StudySpace\\Java秒杀系统方案优化-高性能高并发实战\\p2.zip");
        File target3 = new File("J:\\StudySpace\\Java秒杀系统方案优化-高性能高并发实战\\p3.zip");
        File target4 = new File("J:\\StudySpace\\Java秒杀系统方案优化-高性能高并发实战\\p4.zip");

        new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                noBufferStreamCopy.copyFile(source, target);
            }
        }).start();
        new Thread(() -> bufferStreamCopy.copyFile(source, target2)).start();
        new Thread(() -> nioBufferCopy.copyFile(source, target3)).start();
        new Thread(() -> nioTransferCopy.copyFile(source, target4)).start();
    }
}

