package com.chitacan.bridge;

import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.WindowManager;

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

    private static final String NAME = "Bridge";
    public static final int MSG_DAEMON_ADBD_ERR   = -1;
    public static final int MSG_DAEMON_SERVER_ERR = -2;
    public static final int MSG_DAEMON_INIT       = 0;
    public static final int MSG_DAEMON_CONNECTED  = 1;
    public static final int MSG_DAEMON_DISCONNECT = 2;

    public static final int MSG_SERVER_ERR        = -1;
    public static final int MSG_SERVER_TIMEOUT    = -2;
    public static final int MSG_SERVER_INIT       = 0;
    public static final int MSG_SERVER_CONNECTED  = 1;
    public static final int MSG_SERVER_DISCONNECT = 2;
    public static final int MSG_SERVER_RECONNECT  = 3;
    public static final int MSG_SERVER_COLLAPSE   = 4;
    public static final int MSG_SERVER_BRIDGED    = 5;

    private Context mContext;
    private ServerBridge mServer = null;
    private DaemonBridge mDaemon = null;
    private Bundle mBridgeInfo;
    private BridgeListener mBridgeListener;

    private static final int STATUS_NONE    = 0;
    private static final int STATUS_CREATED = 1;
    private static final int STATUS_REMOVED = 2;
    private int mStatus = STATUS_NONE;

    private static final int MSG_CREATE = 0;
    private static final int MSG_UPDATE = 1;
    private static final int MSG_REMOVE = 2;
    private static final int MSG_ERROR  = 3;

    private final Handler mMainHandler = new Handler(Looper.getMainLooper()) {

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_CREATE:
                    if (mDaemon == null) return;

                    if (mServer.isConnected() && mDaemon.isConnected()) {
                        mBridgeListener.onBridgeCreated();
                        mStatus = STATUS_CREATED;
                        if (BuildConfig.DEBUG)
                            Log.i(Util.createTag(NAME), "STATUS_CREATED");
                    }

                    break;

                case MSG_UPDATE:
                    mBridgeListener.onStatusUpdate(getStatus());
                    break;

                case MSG_REMOVE:
                    if (!mServer.isConnected() && mDaemon == null) {
                        mBridgeListener.onBridgeRemoved();
                        mStatus = STATUS_REMOVED;
                        if (BuildConfig.DEBUG)
                            Log.i(Util.createTag(NAME), "STATUS_REMOVED");
                        mMainHandler.removeMessages(MSG_REMOVE);
                    }
                    break;

                case MSG_ERROR:
                    mBridgeListener.onBridgeError(getStatus());
                    break;
            }
        }
    };

    public Bridge(Context context) {
        this(context, null);
    }

    public Bridge(Context context, BridgeListener listener) {
        mContext = context;
        mBridgeListener = listener;
    }

    private void update() {
        if (mBridgeListener == null)
            return;

        mMainHandler.sendEmptyMessage(MSG_UPDATE);
    }

    private void error() {
        if (mBridgeListener == null)
            return;

        mMainHandler.sendEmptyMessage(MSG_ERROR);
    }

    public void create(Bundle bundle) {
        mBridgeInfo = bundle;

        if (mServer == null) {
            mServer = new ServerBridge();
        }

        if (mDaemon == null || !mDaemon.isAlive()) {
            mDaemon = new DaemonBridge(bundle.getInt("adbport"));
            mDaemon.setServer(mServer);
            mDaemon.start();
        }

        mServer.setDaemon(mDaemon);
        mServer.connect(bundle);
    }

    public void remove() {
        if (mDaemon != null)
            mDaemon.interrupt();

        mDaemon = null;
        mServer.disconnect();
    }

    public Bundle getStatus() {
        Bundle bundle = new Bundle();

        bundle.putInt("bridge_status", mStatus);

        if (mBridgeInfo != null)
            bundle.putAll(mBridgeInfo);

        if (mServer != null)
            bundle.putAll(mServer.getStatus());

        if (mDaemon != null)
            bundle.putAll(mDaemon.getStatus());

        return bundle;
    }

    private class ServerBridge {

        private static final String NAME = "server";
        private Socket mSocket = null;
        private boolean isConnected = false;
        private DaemonBridge mDaemon = null;
        private IO.Options mOpt = new IO.Options();
        private String mUrl = null;
        private String mClientId = null;
        private int mStatus = MSG_SERVER_INIT;

        public ServerBridge() {
            mOpt.forceNew             = true;
            mOpt.reconnectionAttempts = 3;
            mOpt.reconnectionDelay    = 10 * 1000;
            mOpt.reconnectionDelayMax = 20 * 1000;
        }

        private void createSocket() {
            try {
                mSocket = IO.socket(mUrl, mOpt);
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
                    setStatus(MSG_SERVER_CONNECTED);
                    mMainHandler.sendEmptyMessage(MSG_CREATE);
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
                    setStatus(MSG_SERVER_DISCONNECT);
                }

            }).on(Socket.EVENT_CONNECT_ERROR, new Emitter.Listener() {

                @Override
                public void call(Object... args) {
                    Exception e = (Exception) args[0];
                    e.printStackTrace();
                    setStatus(MSG_SERVER_ERR);
                }

            }).on(Socket.EVENT_CONNECT_TIMEOUT, new Emitter.Listener() {

                @Override
                public void call(Object... args) {
                    setStatus(MSG_SERVER_TIMEOUT);
                }
            }).on(Socket.EVENT_RECONNECTING, new Emitter.Listener() {

                @Override
                public void call(Object... args) {
                    setStatus(MSG_SERVER_RECONNECT);
                }
            }).on("bs-bridged", new Emitter.Listener() {

                @Override
                public void call(Object... args) {
                    try {
                        JSONObject obj = (JSONObject) args[0];
                        mClientId = obj.getString("client");
                        setStatus(MSG_SERVER_BRIDGED);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }).on("bs-collapse", new Emitter.Listener() {

                @Override
                public void call(Object... args) {
                    setStatus(MSG_SERVER_COLLAPSE);
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
                obj.put("displayInfo"  , getDisplayInfo());
                obj.put("clientId"     , mClientId);
                return obj;
            } catch (JSONException e) {
                e.printStackTrace();
                return null;
            }
        }

        private JSONObject getDisplayInfo() throws JSONException {
            DisplayMetrics metrics = new DisplayMetrics();
            WindowManager wm = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
            wm.getDefaultDisplay().getRealMetrics(metrics);

            JSONObject obj = new JSONObject();
            obj.put("density"      , metrics.density);
            obj.put("densityDpi"   , metrics.densityDpi);
            obj.put("height"       , metrics.heightPixels);
            obj.put("width"        , metrics.widthPixels);
            obj.put("scaledDensity", metrics.scaledDensity);
            obj.put("xdpi"         , metrics.xdpi);
            obj.put("ydpi"         , metrics.ydpi);
            return obj;
        }

        public void emit(String event, final Object... args) {
            mSocket.emit(event, args);
        }

        public Socket connect(Bundle bundle) {
            String host = bundle.getString("host");
            int    port = bundle.getInt("port");

            mUrl      = Util.createUrl(host, port, "bridge/daemon");
            mClientId = bundle.getString("clientId");

            if (mSocket != null) {
                disconnect();
            }
            createSocket();
            return mSocket.connect();
        }

        public void disconnect() {
            if (isConnected) {
                disconnectSocket();
            } else {
                // If we try to disconnect socket while connecting, It fails with silence.
                // Therefore we add another connect callback to disconnect that socket.
                mSocket.off(Socket.EVENT_CONNECT);
                mSocket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {

                    @Override
                    public void call(Object... args) {
                        disconnectSocket();
                    }

                });
            }
        }

        private void disconnectSocket() {
            mSocket.disconnect();
            mSocket.off();
            isConnected = false;
            mMainHandler.sendEmptyMessage(MSG_REMOVE);
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

        private void setStatus(int status) {
            mStatus = status;
            if (BuildConfig.DEBUG)
                Log.i(Util.createTag(NAME), getMessageString(status));

            if (status >= 0)
                update();
            else
                error();
        }

        private String getMessageString(int msgId) {
            switch (msgId) {
                case MSG_SERVER_ERR:
                    return "MSG_SERVER_ERR";
                case MSG_SERVER_TIMEOUT:
                    return "MSG_SERVER_TIMEOUT";
                case MSG_SERVER_INIT:
                    return "MSG_SERVER_INIT";
                case MSG_SERVER_CONNECTED:
                    return "MSG_SERVER_CONNECTED";
                case MSG_SERVER_DISCONNECT:
                    return "MSG_SERVER_DISCONNECT";
                case MSG_SERVER_RECONNECT:
                    return "MSG_SERVER_RECONNECT";
                case MSG_SERVER_COLLAPSE:
                    return "MSG_SERVER_COLLAPSE";
                case MSG_SERVER_BRIDGED:
                    return "MSG_SERVER_BRIDGED";
                default:
                    return "NOT_DEFINED";
            }
        }

        public String getSocketId() {
            if (isConnected()) {
                Manager io = mSocket.io();

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

        public Bundle getStatus() {
            Bundle bundle = new Bundle();

            bundle.putBoolean("server_connected", isConnected());
            bundle.putString("server_endpoint", mUrl);
            bundle.putInt("server_status", mStatus);
            bundle.putString("clientId", mClientId);
            return bundle;
        }
    }

    private class DaemonBridge extends Thread {

        private static final String NAME = "daemon";
        private Selector mSelector = null;
        private ServerBridge mServer = null;
        private ArrayBlockingQueue mQueue = new ArrayBlockingQueue(10);

        private SocketChannel channel = null;

        // setup buffers.
        private ByteBuffer mLBuffer = ByteBuffer.allocateDirect(6 * 1024);
        private ByteBuffer mRBuffer = ByteBuffer.allocateDirect(6 * 1024);

        private InetSocketAddress mLAddr = null;

        private int mAdbPort = 0;
        private int mStatus = MSG_DAEMON_INIT;

        private long mReceive  = 0;
        private long mTransmit = 0;

        DaemonBridge(int adbPort) {
            mAdbPort = adbPort;
            this.setName("Daemon");
        }

        @Override
        public void run() {
            super.run();
            int result = MSG_DAEMON_INIT;
            try {
                init();
                connect();
                loop();
            } catch (ConnectException e) {
                result = MSG_DAEMON_ADBD_ERR;
                e.printStackTrace();
            } catch (IOException e) {
                result = MSG_DAEMON_SERVER_ERR;
                e.printStackTrace();
            } catch (CancelledKeyException e) {
                result = MSG_DAEMON_SERVER_ERR;
                e.printStackTrace();
            } catch (UnresolvedAddressException e) {
                // no connection. (airplane mode)
                result = MSG_DAEMON_SERVER_ERR;
                e.printStackTrace();
            } catch (JSONException e) {
                result = MSG_DAEMON_SERVER_ERR;
                e.printStackTrace();
            } finally {
                close(result);
            }
        }

        private void init() throws IOException {
            mReceive  = 0;
            mTransmit = 0;
            mLAddr = new InetSocketAddress("127.0.0.1", mAdbPort);
            mSelector = Selector.open();
            channel = SocketChannel.open();
            channel.configureBlocking(false);
            channel.register(mSelector, SelectionKey.OP_CONNECT);
        }

        private void connect() throws IOException {
            channel.connect(mLAddr);
        }

        private void loop() throws IOException, JSONException {
            while(true) {
                if (this.isInterrupted()) break;

                int ready = mSelector.select();

                if (mQueue.peek() != null) {
                    JSONObject obj = (JSONObject) mQueue.poll();
                    byte[] data = (byte[]) obj.get("binary");
                    mRBuffer.put(data);
                    mRBuffer.flip();
                    mReceive += channel.write(mRBuffer);
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
                            setStatus(MSG_DAEMON_CONNECTED);
                            mMainHandler.sendEmptyMessage(MSG_CREATE);
                            key.interestOps(SelectionKey.OP_READ);
                        }
                    } else if (key.isReadable()) {
                        int read = sc.read(mLBuffer);
                        if (read == -1)
                            return;
                        else {
                            mTransmit += read;
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

        private void close(int msg) {
            try {
                if (channel != null)
                    channel.close();

                mQueue.clear();

                if (mServer.isConnected())
                    mServer.emit("bd-collapse");
            } catch(IOException e) {
                e.printStackTrace();
            }

            if (msg == MSG_DAEMON_INIT)
                msg = MSG_DAEMON_DISCONNECT;

            setStatus(msg);
            mMainHandler.sendEmptyMessage(MSG_REMOVE);
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

        private void setStatus(int status) {
            mStatus = status;
            if (BuildConfig.DEBUG)
                Log.i(Util.createTag(NAME), getMessageString(status));

            if (status >= 0)
                update();
            else
                error();
        }

        public String getMessageString(int msgId) {
            switch (msgId) {
                case MSG_DAEMON_ADBD_ERR:
                    return "MSG_DAEMON_ADBD_ERR";
                case MSG_DAEMON_SERVER_ERR:
                    return "MSG_DAEMON_SERVER_ERR";
                case MSG_DAEMON_INIT:
                    return "MSG_DAEMON_INIT";
                case MSG_DAEMON_CONNECTED:
                    return "MSG_DAEMON_CONNECTED";
                case MSG_DAEMON_DISCONNECT:
                    return "MSG_DAEMON_DISCONNECT";
                default:
                    return "NOT_DEFINED";
            }
        }

        public boolean isConnected() {
            if (channel == null)
                return false;
            return channel.isConnected();
        }

        public Bundle getStatus() {
            Bundle bundle = new Bundle();

            bundle.putBoolean("daemon_connected", isConnected());
            bundle.putInt("daemon_status", mStatus);
            return bundle;
        }
    }

    interface BridgeListener {
        public void onStatusUpdate(Bundle bundle);
        public void onBridgeCreated();
        public void onBridgeRemoved();
        public void onBridgeError(Bundle bundle);
    }
}
