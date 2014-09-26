package com.chitacan.bridge;

import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Manager;
import com.github.nkzawa.socketio.client.Socket;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.channels.UnresolvedAddressException;
import java.util.Iterator;
import java.util.concurrent.ArrayBlockingQueue;

/**
 * Created by chitacan on 2014. 9. 26..
 */
public class Bridge {

    private ServerBridge mServer = null;
    private DaemonBridge mDaemon = null;

    public void create(Bundle bundle) {
        String adbdPort = bundle.getString("adbport");
        String host = bundle.getString("host");
        String port = bundle.getString("port");

        if (mServer == null) {
            mServer = new ServerBridge();
        }

        if (mDaemon == null || !mDaemon.isAlive()) {
            mDaemon = new DaemonBridge();
            mDaemon.setServer(mServer);
            mDaemon.start();
        }

        mServer.setDaemon(mDaemon);
        mServer.connect(Util.createUrl(host, Integer.parseInt(port), "bridge/daemon"));
    }

    public void remove() {
        if (mDaemon != null)
            mDaemon.interrupt();

        mDaemon = null;
        mServer.disconnect();
    }

    public String getSocketId() {
        if (mServer.isConnected()) {
            Manager io = mServer.getSocket().io();

            // retrieve socket id via reflection.
            try {
                Field fEngine = Manager.class.getDeclaredField("engine");
                fEngine.setAccessible(true);
                Object socket = fEngine.get(io);

                Field fId = com.github.nkzawa.engineio.client.Socket.class.getDeclaredField("id");
                fId.setAccessible(true);

                return (String) fId.get(socket);
            } catch (NoSuchFieldException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        throw new RuntimeException("Server not Connected.");
    }

    private class ServerBridge {

        private Socket mSocket = null;
        private boolean isConnected = false;
        private DaemonBridge mDaemon = null;
        private Handler mStatusHandler = null;
        private IO.Options mOpt = new IO.Options();
        private String mUrl = null;

        public ServerBridge() {
            mOpt.forceNew             = true;
            mOpt.reconnectionDelay    = 10 * 1000;
            mOpt.reconnectionDelayMax = 20 * 1000;
        }

        private void createSocket(String url) {
            try {
                mSocket = IO.socket(url, mOpt);
                subscribe();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private void subscribe() {
            mSocket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {

                @Override
                public void call(Object... args) {
                    isConnected = true;
                    mSocket.emit("bd-host", hostInfo());
                    setStatus("server connected");
                }

            }).on("bs-data", new Emitter.Listener() {

                @Override
                public void call(Object... args) {
                    mDaemon.queue(args[0]);
                }

            }).on(Socket.EVENT_DISCONNECT, new Emitter.Listener() {

                @Override
                public void call(Object... args) {
                    isConnected = false;
                    String reason = (String) args[0];
                    setStatus("server disconnected - " + reason);
                }

            }).on(Socket.EVENT_CONNECT_ERROR, new Emitter.Listener() {

                @Override
                public void call(Object... args) {
                    Exception e = (Exception) args[0];
                    e.printStackTrace();
                    setStatus("server connect error - " + e.getMessage());
                }

            }).on(Socket.EVENT_CONNECT_TIMEOUT, new Emitter.Listener() {

                @Override
                public void call(Object... args) {
                    setStatus("server timeout");
                }
            }).on(Socket.EVENT_RECONNECTING, new Emitter.Listener() {

                @Override
                public void call(Object... args) {
                    setStatus("server reconnecting...");
                }
            });
        }

        private JSONObject hostInfo() {
            try {
                JSONObject obj = new JSONObject();
                obj.put("brand"        , Build.BRAND);
                obj.put("manufacturer" , Build.MANUFACTURER);
                obj.put("model"        , Build.MODEL);
                obj.put("type"         , Build.TYPE);
                obj.put("version"      , Build.VERSION.RELEASE);
                obj.put("sdk_version"  , Build.VERSION.SDK_INT);
                return obj;
            } catch (JSONException e) {
                e.printStackTrace();
                return null;
            }
        }

        public void emit(String event, final Object... args) {
            mSocket.emit(event, args);
        }

        public Socket connect(String url) {
            if (mSocket != null) {
                disconnect();
            }
            mUrl = url;
            createSocket(url);
            return mSocket.connect();
        }

        public void disconnect() {
            mSocket.close();
            mSocket.disconnect();
        }

        public void setDaemon(DaemonBridge bridge) {
            mDaemon = bridge;
        }

        public boolean isConnected() {
            return isConnected;
        }

        public Socket getSocket() {
            return mSocket;
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
    }

    private class DaemonBridge extends Thread {
        private Selector mSelector = null;
        private ServerBridge mServer = null;
        private ArrayBlockingQueue mQueue = new ArrayBlockingQueue(10);

        private SocketChannel channel = null;

        // setup buffers.
        private ByteBuffer mLBuffer = ByteBuffer.allocateDirect(6 * 1024);
        private ByteBuffer mRBuffer = ByteBuffer.allocateDirect(6 * 1024);

        private InetSocketAddress mLAddr = null;
        private Handler mStatusHandler = null;

        DaemonBridge() {
            this.setName("Daemon");
        }

        @Override
        public void run() {
            super.run();
            String result = null;
            try {
                init();
                connect();
                loop();
            } catch (ConnectException e) {
                // adbd connect exception
                result = "adbd is not available";
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (CancelledKeyException e) {
                e.printStackTrace();
            } catch (UnresolvedAddressException e) {
                // no connectiom. (airplane mode)
                e.printStackTrace();
            } catch (JSONException e) {
                result = "JSON parse exception";
                e.printStackTrace();
            } finally {
                close(result);
            }
        }

        private void init() throws IOException {
            mLAddr = new InetSocketAddress("127.0.0.1", 6666);
            mSelector = Selector.open();
            channel = SocketChannel.open();
            channel.configureBlocking(false);
            channel.register(mSelector, SelectionKey.OP_CONNECT);
        }

        private void connect() throws IOException {
            channel.connect(mLAddr);
        }

        private void loop() throws IOException, JSONException {
            setStatus("daemon loop");
            while(true) {
                if (this.isInterrupted()) break;

                int ready = mSelector.select();

                if (mQueue.peek() != null) {
                    JSONObject obj = (JSONObject) mQueue.poll();
                    byte[] data = (byte[]) obj.get("binary");
                    mRBuffer.put(data);
                    mRBuffer.flip();
                    int write = channel.write(mRBuffer);
                    setStatus("write : " + write);
                    mRBuffer.clear();
                }

                if (ready == 0) continue;

                Iterator<SelectionKey> i = mSelector.selectedKeys().iterator();

                while(i.hasNext()) {
                    SelectionKey key = i.next();
                    SocketChannel sc = (SocketChannel) key.channel();
                    if (key.isConnectable()) {
                        if (sc.isConnectionPending()) {
                            sc.finishConnect();
                            setStatus("daemon connected");
                            key.interestOps(SelectionKey.OP_READ);
                        }
                    } else if (key.isReadable()) {
                        int read = sc.read(mLBuffer);
                        setStatus("read : " + read);
                        if (read == -1)
                            return;
                        else {
                            mLBuffer.flip();
                            byte[] b = new byte[mLBuffer.remaining()];
                            mLBuffer.get(b);
                            mLBuffer.clear();
                            JSONObject req = new JSONObject();
                            req.put("hello", "req");
                            req.put("binary", b);
                            mServer.emit("bd-data", req);
                        }
                    }
                    i.remove();
                }
            }
        }

        private void close(String msg) {
            if (msg == null)
                msg = "daemon close";
            try {
                if (channel != null)
                    channel.close();

                mQueue.clear();

                if (mServer.isConnected())
                    mServer.emit("bd-collapse");
            } catch(IOException e) {
                e.printStackTrace();
            }
            setStatus(msg);
        }

        public boolean queue(Object obj) {
            boolean result = mQueue.offer(obj);
            mSelector.wakeup();
            return result;
        }

        public void setServer(ServerBridge server) {
            mServer = server;
        }

        @Override
        public void interrupt() {
            mSelector.wakeup();
            super.interrupt();
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
    }
}