package com.chitacan.bridge;

import android.os.Handler;
import android.os.Message;
import android.util.Log;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.channels.UnresolvedAddressException;
import java.util.Iterator;

/**
 * Created by chitacan on 2014. 8. 20..
 */
public class Extender extends Thread {
    private static final int SOCKET_LOCAL  = 0;
    private static final int SOCKET_REMOTE = 1;

    private Selector mSelector = null;

    private SocketChannel mLChannel = null;
    private SocketChannel mRChannel = null;

    // setup buffers.
    private ByteBuffer mLBuffer = ByteBuffer.allocateDirect(6 * 1024);
    private ByteBuffer mRBuffer = ByteBuffer.allocateDirect(6 * 1024);

    private InetSocketAddress mLAddr = null;
    private InetSocketAddress mRAddr = null;

    private String mLocalAddr = null;
    private String mRemoteAddr = null;

    private int mLocalPort = 0;
    private int mRemotePort = 0;

    private Handler mStatusHandler = null;

    Extender(String localAddr, int localPort, String remoteAddr, int remotePort) {
        super("Extender Thread");
        mLocalAddr = localAddr;
        mRemoteAddr = remoteAddr;

        mLocalPort = localPort;
        mRemotePort = remotePort;

        setPriority(Thread.MIN_PRIORITY);
    }

    public void setHandler(Handler handler) {
        mStatusHandler = handler;
    }

    private void setStatus(String status) {
        if (mStatusHandler == null) return;

        Message msg = mStatusHandler.obtainMessage();
        msg.what = 0;
        msg.obj = status;
        mStatusHandler.sendMessage(msg);
    }

    @Override
    public void run() {
        super.run();
        try {
            init();
            connect();
            loop();
        } catch (IOException e) {
            e.printStackTrace();
            setStatus(e.getMessage());
        } catch (CancelledKeyException e) {
            e.printStackTrace();
            setStatus(e.getMessage());
        } catch (UnresolvedAddressException e) {
            // no connectiom. (airplane mode)
            e.printStackTrace();
            setStatus(e.getMessage());
        } finally {
            close();
        }
    }

    private void init() throws IOException {
        int key = SelectionKey.OP_CONNECT;

        mLAddr = new InetSocketAddress(mLocalAddr, mLocalPort);
        mRAddr = new InetSocketAddress(mRemoteAddr, mRemotePort);
        mSelector = Selector.open();
        mLChannel = SocketChannel.open();
        mRChannel = SocketChannel.open();
        mLChannel.configureBlocking(false);
        mRChannel.configureBlocking(false);
        mLChannel.register(mSelector, key, SOCKET_LOCAL);
        mRChannel.register(mSelector, key, SOCKET_REMOTE);

        setStatus("init");
    }

    private void connect() throws IOException {
        mLChannel.connect(mLAddr);
        mRChannel.connect(mRAddr);

        setStatus("connect");
    }

    private void loop() throws IOException {
        Log.d("chitacan", "let's dive into loop");
        setStatus("loop");
        while (true) {
            if (this.isInterrupted()) break;

            int readyChannels = mSelector.select();

            if (readyChannels == 0) continue;

            Iterator<SelectionKey> i = mSelector.selectedKeys().iterator();

            while (i.hasNext()) {
                SelectionKey key = i.next();
                SocketChannel sc = (SocketChannel) key.channel();
                int type = (Integer) key.attachment();
                if (key.isConnectable()) {
                    sc.finishConnect();
                    key.interestOps(SelectionKey.OP_READ);
                } else if (key.isReadable()) {
                    handleRead(type);
                    for (SelectionKey k : mSelector.keys()) {
                        if (!k.equals(key))
                            k.interestOps(SelectionKey.OP_READ | SelectionKey.OP_WRITE);
                    }
                } else if (key.isWritable()) {
                    handleWrite(type);
                    key.interestOps(SelectionKey.OP_READ);
                }

                i.remove();
            }
        }
    }

    private void handleRead(int type) throws IOException {
        int read = 0;
        switch (type) {
            case SOCKET_LOCAL:
                read = mLChannel.read(mLBuffer);
                Log.i("chitacan", "local read : " + read);

                setStatus("local read : " + read);
                break;
            case SOCKET_REMOTE:
                read = mRChannel.read(mRBuffer);
                Log.i("chitacan", "remote read : " + read);

                setStatus("remote read : " + read);
                break;
        }

        if (read == -1)
            close();
    }

    private void handleWrite(Integer type) throws IOException {
        int write = 0;
        switch (type) {
            case SOCKET_LOCAL:
                if (mRBuffer.position() != 0) {
                    mRBuffer.flip();
                    while (mRBuffer.hasRemaining()) {
                        write = mLChannel.write(mRBuffer);
                        Log.i("chitacan", "local write: " + write);
                    }
                    mRBuffer.clear();
                    setStatus("local write : " + write);
                }
                break;
            case SOCKET_REMOTE:
                if (mLBuffer.position() != 0) {
                    mLBuffer.flip();
                    while (mLBuffer.hasRemaining()) {
                        write = mRChannel.write(mLBuffer);
                        Log.i("chitacan", "remote write: " + write);
                    }
                    mLBuffer.clear();
                    setStatus("remote write : " + write);
                }
                break;
        }
    }

    @Override
    public void interrupt() {
        super.interrupt();
        setStatus("interrupted");
    }

    private void close() {
        try {
            if (mRChannel != null)
                mRChannel.close();
            if (mLChannel != null)
                mLChannel.close();
            Log.d("chitacan", "close");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
