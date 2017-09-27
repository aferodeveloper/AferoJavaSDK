/*
 * Copyright (c) 2014-2017 Afero, Inc. All rights reserved.
 */

package io.afero.sdk.conclave;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.security.cert.Certificate;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.zip.DeflaterOutputStream;

import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import io.afero.sdk.client.afero.models.ConclaveAccessDetails;
import io.afero.sdk.log.AfLog;
import io.afero.sdk.utils.JSONUtils;
import rx.Observable;
import rx.schedulers.Schedulers;
import rx.subjects.PublishSubject;

public class ConclaveClient {
    public static String CLIENT_VERSION = "1.3.0";
    public static String DEFAULT_HOST = "conclave-stream.afero.io";
    public static int DEFAULT_PORT = 443;

    public static final int ERROR_CODE_INVALID_TOKEN = 906;

    private static final int RETRY_MAX = 20;
    private static final int HEARBEAT_TIMEOUT_DEFAULT = 270;
    private static final int HEARTBEAT_TIMEOUT_EXTRA = 15;

    public enum Status {
        CONNECTING,
        CONNECTED,
        DISCONNECTING,
        DISCONNECTED
    }

    private String mHost = DEFAULT_HOST;
    private int mPort = DEFAULT_PORT;
    private boolean mUseCompression = true;
    private boolean mUseSSL = true;
    private int mRetryDelay;

    private String mServerVersion;
    private int mSessionId;

    private Socket mSocket;
    private PrintWriter mWriter;
    private BufferedReader mReader;
    private ReaderThread mReaderThread;
    private PublishSubject<JsonNode> mMessageSubject = PublishSubject.create();
    private PublishSubject<Status> mStatusSubject = PublishSubject.create();
    private final Object mConnectLock = new Object();

    public synchronized Observable<ConclaveClient> connect(ConclaveAccessDetails cad) {
        mRetryDelay = 0;

        if (cad.conclaveHosts != null) {
            for (ConclaveAccessDetails.ConclaveHost ch : cad.conclaveHosts) {
                if ("socket".equals(ch.type)) {
                    mHost = ch.host;
                    mPort = ch.port;
                    break;
                }
            }
        }

        if (mReaderThread != null) {
            close();
        }

        return Observable.fromCallable(new Callable<ConclaveClient>() {
            @Override
            public ConclaveClient call() throws Exception {

                synchronized (mConnectLock) {
                    if (!isConnected()) {
                        openSocket();
                        readloop();
                    }
                }

                return ConclaveClient.this;
            }
        }).subscribeOn(Schedulers.io());
    }

    public synchronized void write(Object message) {
        try {
            String s = JSONUtils.writeValueAsString(message);
            AfLog.i("ConclaveClient.write: " + s);
            mWriter.println(s);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }

    public void login(String accountId, String userId, String token, String type, boolean trace) {
        write(new ConclaveMessage.Login(accountId, userId, token, type, CLIENT_VERSION, trace));
    }

    public void say(String event, Object data) {
        write(new ConclaveMessage.Say(event, data));
    }

    public Observable<ConclaveMessage.Say> sayAsync(String event, Object data) {
        ConclaveMessage.Say say = new ConclaveMessage.Say(event, data);
        return Observable.fromCallable(new SayCallable(say))
            .subscribeOn(Schedulers.io());
    }

    public void whisper(int sessionId, String event, Object data) {
        write(new ConclaveMessage.Whisper(sessionId, event, data));
    }

    public void bye() {
        write(new ConclaveMessage.Bye());
    }

    public synchronized void close() {
        try {
            if (mReaderThread != null) {
                mReaderThread.stopRunning();
                mReaderThread = null;
            }

            if (mSocket != null) {
                mStatusSubject.onNext(ConclaveClient.Status.DISCONNECTED);
                Observable.fromCallable(new CloseSocketCallable(mSocket))
                    .subscribeOn(Schedulers.io());
                mSocket = null;
            }
        } catch (Exception e) {
            // ignore
        }
    }

    private void openSocket() throws IOException {
        InetAddress address = InetAddress.getByName(mHost);

        mStatusSubject.onNext(Status.CONNECTING);

        if (mUseSSL) {
            AfLog.i("ConclaveClient: Starting SSL connection to " + mHost + ":" + mPort);
            mSocket = SSLSocketFactory.getDefault().createSocket(address, mPort);
            ((SSLSocket)mSocket).setEnabledProtocols(new String[] {"TLSv1.1", "TLSv1.2"});
        } else {
            AfLog.i("ConclaveClient: Starting non-SSL connection to " + mHost + ":" + mPort);
            mSocket = new Socket(address, mPort);
        }

        OutputStream os = mSocket.getOutputStream();
        InputStream is = mSocket.getInputStream();

        if (mUseCompression) {
            is = new StreamingInflaterInputStream(is);
            os = new DeflaterOutputStream(os, true);
        }

        mReader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
        mWriter = new PrintWriter(new BufferedWriter(new OutputStreamWriter(os)), true);

        setHeartbeatTimeout(HEARBEAT_TIMEOUT_DEFAULT);
    }

    private void closeSocket() {

        mStatusSubject.onNext(Status.DISCONNECTING);

        if (mSocket != null) {
            try {
                mSocket.close();
            } catch (IOException e) {
                // I'll alert the president.
            }

            mSocket = null;
        }

        mStatusSubject.onNext(Status.DISCONNECTED);
    }

    public boolean isConnected() {
        return mSocket != null && mSocket.isConnected();
    }

    public Observable<Status> statusObservable() {
        return mStatusSubject;
    }

    public Observable<JsonNode> messageObservable() {
        return mMessageSubject;
    }

    private class ReaderThread extends Thread {

        public boolean mIsRunning = true;

        public ReaderThread() {
            super("ConclaveClient");
        }

        public void stopRunning() {
            mIsRunning = false;
            interrupt();
        }

        public void run() {

            AfLog.i("ConclaveClient: reader thread started");

            while (mIsRunning && !Thread.interrupted()) {
                try {
                    if (mSocket == null || !mSocket.isConnected()) {
                        AfLog.i("ConclaveClient: reconnecting in " + mRetryDelay + "s");

                        mStatusSubject.onNext(Status.DISCONNECTED);

                        Thread.sleep(mRetryDelay * 1000);
                        if (!mIsRunning) {
                            break;
                        }

                        openSocket();

                        AfLog.i("ConclaveClient: reconnected!");
                    }

                    if (!mIsRunning) {
                        break;
                    }

                    String line = mReader.readLine();
                    if (line != null) {
                        if (line.length() > 0) {
                            AfLog.i("ConclaveClient: readLine=" + line);

                            JsonNode node = JSONUtils.getObjectMapper().readTree(line);
                            readEvent(node);
                        } else {
                            AfLog.i("ConclaveClient: readLine=<empty> (heartbeat)");
                            mWriter.println();
                        }
                    } else {
                        throw new IOException("readLine returned null");
                    }
                } catch (Exception e) {
                    AfLog.i("ConclaveClient: Reader died");
                    e.printStackTrace();

                    if (mIsRunning) {
                        mRetryDelay = Math.max(1, Math.min(mRetryDelay * 2, RETRY_MAX));
                        closeSocket();
                    }
                }
            }

            AfLog.i("ConclaveClient: reader thread exiting");
        }
    }

    private void readloop() {
        mReaderThread = new ReaderThread();
        mReaderThread.start();
    }

    private void setHeartbeatTimeout(int timeoutInSeconds) {
        AfLog.i("ConclaveClient: setHeartbeatTimeout to " + timeoutInSeconds);

        if (timeoutInSeconds > 0) {
            timeoutInSeconds += HEARTBEAT_TIMEOUT_EXTRA;
        }

        try {
            mSocket.setSoTimeout(timeoutInSeconds * 1000);
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

    private void readEvent(JsonNode node) {
        mMessageSubject.onNext(node);

        try {
            ObjectMapper mapper = JSONUtils.getObjectMapper();
            Map.Entry<String,JsonNode> entry = node.fields().next();
            String key = entry.getKey().toLowerCase(Locale.ROOT);

            if (key.equals("hello")) {
                ConclaveMessage.HelloFields hello = mapper.treeToValue(node.get("hello"), ConclaveMessage.HelloFields.class);
                mServerVersion = hello.version;
                setHeartbeatTimeout(hello.heartbeat);
            } else if (key.equals("welcome")) {
                ConclaveMessage.WelcomeFields welcome = mapper.treeToValue(node.get("welcome"), ConclaveMessage.WelcomeFields.class);
                mSessionId = welcome.sessionId;
                mRetryDelay = 0;
                mStatusSubject.onNext(Status.CONNECTED);
//            } else if (key.equals("join")) {
//                ConclaveMessage.JoinFields join = mapper.treeToValue(node.get("join"), ConclaveMessage.JoinFields.class);
//                handler.onJoin(join);
//            } else if (key.equals("leave")) {
//                ConclaveMessage.LeaveFields leave = mapper.treeToValue(node.get("leave"), ConclaveMessage.LeaveFields.class);
//                handler.onLeave(leave);
//            } else if (key.equals("public")) {
//                ConclaveMessage.MessageFields message = mapper.treeToValue(node.get("public"), ConclaveMessage.MessageFields.class);
//                handler.onPublic(message);
//            } else if (key.equals("private")) {
//                ConclaveMessage.MessageFields message = mapper.treeToValue(node.get("private"), ConclaveMessage.MessageFields.class);
//                handler.onPrivate(message);
            }

        } catch (Exception e) {
            AfLog.i("ConclaveClient.readEvent: failed to unpack message");
            AfLog.e(e);
        }
    }

    private static void printServerCertificate(SSLSocket socket) {
        try {
            Certificate[] serverCerts =
                    socket.getSession().getPeerCertificates();
            for (int i = 0; i < serverCerts.length; i++) {
                Certificate myCert = serverCerts[i];
                AfLog.i("====Certificate:" + (i+1) + "====");
                AfLog.i("-Public Key-\n" + myCert.getPublicKey());
                AfLog.i("-Certificate Type-\n " + myCert.getType());

                System.out.println();
            }
        } catch (SSLPeerUnverifiedException e) {
            AfLog.i("Could not verify peer");
            e.printStackTrace();
//            System.exit(-1);
        }
    }

    private static void printSocketInfo(SSLSocket s) {
        AfLog.i("Socket class: "+s.getClass());
        AfLog.i("   Remote address = "
                +s.getInetAddress().toString());
        AfLog.i("   Remote port = "+s.getPort());
        AfLog.i("   Local socket address = "
                +s.getLocalSocketAddress().toString());
        AfLog.i("   Local address = "
                +s.getLocalAddress().toString());
        AfLog.i("   Local port = "+s.getLocalPort());
        AfLog.i("   Need client authentication = "
                +s.getNeedClientAuth());
        SSLSession ss = s.getSession();
        AfLog.i("   Cipher suite = "+ss.getCipherSuite());
        AfLog.i("   Protocol = "+ss.getProtocol());
    }

    private class SayCallable implements Callable<ConclaveMessage.Say> {

        private final ConclaveMessage.Say mSay;

        public SayCallable(ConclaveMessage.Say say) {
            mSay = say;
        }

        @Override
        public ConclaveMessage.Say call() throws Exception {
            write(mSay);
            return mSay;
        }
    }

    private static class CloseSocketCallable implements Callable<Socket> {

        private final Socket mSocket;

        public CloseSocketCallable(Socket socket) {
            mSocket = socket;
        }

        @Override
        public Socket call() throws Exception {
            AfLog.d("ConclaveClient: closing socket " + mSocket.toString());
            mSocket.close();
            return mSocket;
        }
    }
}
