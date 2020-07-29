package com.ms.silverking.cloud.dht.net.test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

import com.ms.silverking.cloud.dht.net.MessageGroup;
import com.ms.silverking.cloud.dht.net.MessageGroupConnection;
import com.ms.silverking.cloud.dht.net.MessageGroupConnectionCreator;
import com.ms.silverking.cloud.dht.net.MessageGroupReceiver;
import com.ms.silverking.log.Log;
import com.ms.silverking.net.async.PersistentAsyncServer;
import com.ms.silverking.numeric.NumConversion;
import com.ms.silverking.thread.lwt.LWTPoolProvider;
import com.ms.silverking.time.SimpleStopwatch;
import com.ms.silverking.time.Stopwatch;
import com.ms.silverking.thread.ThreadUtil;

public class MessageGroupTest implements MessageGroupReceiver {
  private final PersistentAsyncServer<MessageGroupConnection> paServer;
  private final InetSocketAddress serverAddr;
  private final Mode mode;
  private Semaphore semaphore;
  private AtomicInteger messagesSent;
  private AtomicInteger messagesReceived;
  private int clientPort;

  private enum Test {MessageGroupConnection}

  ;

  private enum Mode {client, server}

  ;

  private static final int testClientPort = 7629;
  private static final int testServerPort = 7627;
  private static final int numSelectorControllers = 8;
  private static final String selectorControllerClass = "PingPong";
  private static final double displayIntervalSeconds = 10.0;
  private static final double extraSeconds = 60.0;

  public MessageGroupTest(Mode mode, String serverHost) throws IOException {
    messagesSent = new AtomicInteger();
    messagesReceived = new AtomicInteger();
    this.mode = mode;
    switch (mode) {
    case client:
      serverAddr = new InetSocketAddress(serverHost, testServerPort);
      paServer = new PersistentAsyncServer<>(testClientPort, new MessageGroupConnectionCreator(this),
          numSelectorControllers, selectorControllerClass);
      clientPort = paServer.getPort();
      semaphore = new Semaphore(0);
      break;
    case server:
      serverAddr = null;
      paServer = new PersistentAsyncServer<>(testServerPort, new MessageGroupConnectionCreator(this),
          numSelectorControllers, selectorControllerClass);
      break;
    default:
      throw new RuntimeException("panic");
    }
    paServer.enable();
  }

  public void runTest(Test test, double durationSeconds) throws IOException {
    switch (test) {
    case MessageGroupConnection:
      runBufferedDataConnectionTest(durationSeconds);
      break;
    default:
      throw new RuntimeException("panic");
    }
  }

  private void runBufferedDataConnectionTest(double durationSeconds) throws IOException {
    switch (mode) {
    case client:
      runQueueingConnectionTestClient(durationSeconds);
      break;
    case server:
      runQueueingConnectionTestServer(durationSeconds);
      break;
    default:
      throw new RuntimeException("panic");
    }
  }

  private void runQueueingConnectionTestServer(double durationSeconds) {
    ThreadUtil.sleepSeconds(durationSeconds + extraSeconds);
  }

  private void runQueueingConnectionTestClient(double durationSeconds) throws IOException {
    MessageGroup msg;
    Stopwatch sw;
    Stopwatch displaySW;

    //msg = createMessage(NumConversion.intToBytes(index));
    sw = new SimpleStopwatch();
    displaySW = new SimpleStopwatch();
    do {
      msg = createMessage(NumConversion.intToBytes(clientPort));
      //System.out.println("Sending");
      messagesSent.incrementAndGet();
      paServer.send(serverAddr, msg, false, Long.MAX_VALUE);
      try {
        semaphore.acquire();
      } catch (InterruptedException ie) {
      }
      //System.out.println("past semaphore");
      //rewindBuffers(msg);
      if (displaySW.getSplitSeconds() > displayIntervalSeconds) {
        displayStats(sw.getSplitSeconds());
        displaySW.reset();
      }
    } while (sw.getSplitSeconds() < durationSeconds);
    sw.stop();
    displayStats(sw.getElapsedSeconds());
  }

  private void displayStats(double time) {
    System.out.printf("messagesReceived: %d\tduration %f\n\n", messagesReceived.get(), time);
    System.out.printf("messagesSent: %d\tduration %f\n", messagesReceived.get(), time);
    System.out.printf("messageRate (msgs/s): %f\n", (double) messagesReceived.get() / time);
    System.out.printf("messageLatency (ms):  %f\n", (double) time / (double) messagesReceived.get());
  }

  private MessageGroup createMessage(byte[] payload) {
    ByteBuffer[] buffers;

    buffers = new ByteBuffer[1];
        /*
        buffers[0] = ByteBuffer.wrap(payload);
        */
    /**/
    buffers[0] = wrapInDirect(payload);
    /**/
    //return new MessageGroup(MessageType.PUT, null, 0, buffers, null, Integer.MAX_VALUE, true);
    return null;
  }

  private ByteBuffer wrapInDirect(byte[] a) {
    ByteBuffer buf;

    buf = ByteBuffer.allocateDirect(a.length);
    buf.put(a);
    buf.rewind();
    return buf;
  }

  @Override
  public void receive(MessageGroup message, MessageGroupConnection connection) {
    Log.fine("received: ", message);
    switch (mode) {
    case client:
      semaphore.release();
      messagesReceived.incrementAndGet();
      break;
    case server:
      try {
        InetSocketAddress other;
        MessageGroup msg;

        other = new InetSocketAddress(connection.getRemoteSocketAddress().getHostName(), testClientPort);
        msg = createMessage(NumConversion.intToBytes(testClientPort));
        paServer.send(other, msg, false, Long.MAX_VALUE);
        //connection.sendAsynchronous(bufferedData, Long.MAX_VALUE);
      } catch (IOException ioe) {
        ioe.printStackTrace();
      }
      break;
    default:
      throw new RuntimeException("panic");
    }
  }

  /**
   * @param args
   */
  public static void main(String[] args) {
    try {
      if (args.length != 3 && args.length != 4) {
        System.out.println("server <test> <durationSeconds>");
        System.out.println("or");
        System.out.println("client <test> <durationSeconds> <serverHost>");
      } else {
        Mode mode;
        Test test;
        MessageGroupTest pingPongTest;
        double durationSeconds;

        LWTPoolProvider.createDefaultWorkPools();
        mode = Mode.valueOf(args[0]);
        test = Test.valueOf(args[1]);
        durationSeconds = Double.parseDouble(args[2]);
        //Log.setLevelAll();
        switch (mode) {
        case server:
          pingPongTest = new MessageGroupTest(mode, null);
          pingPongTest.runTest(test, durationSeconds);
          break;
        case client:
          String serverHost;

          serverHost = args[3];
          pingPongTest = new MessageGroupTest(mode, serverHost);
          pingPongTest.runTest(test, durationSeconds);
          break;
        default:
          throw new RuntimeException("panic");
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
