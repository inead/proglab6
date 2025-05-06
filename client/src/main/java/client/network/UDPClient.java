package client.network;

import client.App;
import common.network.requests.Request;
import common.network.responses.Response;
import com.google.common.primitives.Bytes;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.logging.log4j.Logger;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

public class UDPClient {
  private final int PACKET_SIZE = 1024;
  private final int DATA_SIZE = PACKET_SIZE - 1;
  private final int DEFAULT_TIMEOUT_MS = 5000;

  private final DatagramChannel client;
  private final InetSocketAddress addr;
  private final Logger logger = App.logger;
  private int timeoutMs;

  public UDPClient(InetAddress address, int port) throws IOException {
    this(address, port, 5000);
  }

  public UDPClient(InetAddress address, int port, int timeoutMs) throws IOException {
    this.addr = new InetSocketAddress(address, port);
    this.client = DatagramChannel.open().bind(null).connect(addr);
    this.client.configureBlocking(false);
    this.timeoutMs = timeoutMs;
    logger.info("DatagramChannel подключен к " + addr + " с таймаутом " + timeoutMs + " мс");
  }

  public boolean testConnection() {
    try {
      byte[] testPacket = "PING".getBytes();
      sendData(testPacket);

      long startTime = System.currentTimeMillis();
      while (System.currentTimeMillis() - startTime < timeoutMs) {
        byte[] response = tryReceive();
        if (response != null && new String(response).equals("PONG")) {
          return true;
        }
        TimeUnit.MILLISECONDS.sleep(100);
      }
      return false;
    } catch (IOException | InterruptedException e) {
      logger.error("Ошибка проверки соединения", e);
      return false;
    }
  }

  private byte[] tryReceive() throws IOException {
    ByteBuffer buffer = ByteBuffer.allocate(PACKET_SIZE);
    SocketAddress address = client.receive(buffer);
    return address != null ? buffer.array() : null;
  }

  public Response sendAndReceiveCommand(Request request) throws IOException {
    var data = SerializationUtils.serialize(request);
    var responseBytes = sendAndReceiveData(data);

    Response response = SerializationUtils.deserialize(responseBytes);
    logger.info("Получен ответ от сервера: " + response);
    return response;
  }

  private void sendData(byte[] data) throws IOException {
    byte[][] chunks = splitIntoChunks(data);
    logger.info("Отправляется " + chunks.length + " чанков...");

    for (int i = 0; i < chunks.length; i++) {
      byte lastByte = (byte) (i == chunks.length - 1 ? 1 : 0);
      byte[] chunk = Bytes.concat(chunks[i], new byte[]{lastByte});
      client.send(ByteBuffer.wrap(chunk), addr);
      logger.info("Чанк " + (i + 1) + "/" + chunks.length + " отправлен");
    }
    logger.info("Отправка данных завершена");
  }

  private byte[][] splitIntoChunks(byte[] data) {
    int chunkCount = (int) Math.ceil(data.length / (double) DATA_SIZE);
    byte[][] chunks = new byte[chunkCount][];

    for (int i = 0; i < chunkCount; i++) {
      int start = i * DATA_SIZE;
      int end = Math.min(start + DATA_SIZE, data.length);
      chunks[i] = Arrays.copyOfRange(data, start, end);
    }
    return chunks;
  }

  private byte[] receiveData() throws IOException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    boolean receivedLast = false;
    long startTime = System.currentTimeMillis();

    while (!receivedLast && (System.currentTimeMillis() - startTime < timeoutMs)) {
      ByteBuffer buffer = ByteBuffer.allocate(PACKET_SIZE);
      SocketAddress address = client.receive(buffer);

      if (address != null) {
        byte[] packet = buffer.array();
        boolean isLast = packet[packet.length - 1] == 1;
        baos.write(packet, 0, packet.length - 1);
        receivedLast = isLast;
      }
    }

    if (!receivedLast) {
      throw new SocketTimeoutException("Таймаут приема данных");
    }
    return baos.toByteArray();
  }

  private byte[] sendAndReceiveData(byte[] data) throws IOException {
    sendData(data);
    return receiveData();
  }

  public void setTimeout(int timeoutMs) {
    this.timeoutMs = timeoutMs;
  }

  public void close() {
    try {
      if (client != null && client.isOpen()) {
        client.close();
        logger.info("Соединение закрыто");
      }
    } catch (IOException e) {
      logger.error("Ошибка при закрытии соединения", e);
    }
  }
}
