package client;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import client.cli.Runner;
import client.network.UDPClient;
import client.utility.console.StandardConsole;

import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.time.Duration;
import java.time.Instant;

public class App {
  private static final int PORT = 23586;
  private static final int CONNECTION_TIMEOUT_MS = 5000; // 5 секунд на попытку подключения
  private static final int RECONNECT_INTERVAL_MS = 1000; // 1 секунда между попытками
  private static final int CONNECTION_LOST_TIMEOUT_MS = 30000; // 30 секунд до сообщения о потере
  public static final Logger logger = LogManager.getLogger("ClientLogger");

  public static void main(String[] args) {
    var console = new StandardConsole();
    Instant lastSuccessTime = Instant.now();
    boolean connectionLostMessageShown = false;

    while (true) {
      try {
        var client = new UDPClient(InetAddress.getByName("127.0.0.1"), PORT, CONNECTION_TIMEOUT_MS);
        var cli = new Runner(client, console);

        if (client.testConnection()) {
          lastSuccessTime = Instant.now();
          connectionLostMessageShown = false;
          console.println("Подключение к серверу успешно установлено!");
          cli.interactiveMode();
        } else if (!connectionLostMessageShown &&
          Duration.between(lastSuccessTime, Instant.now()).toMillis() > CONNECTION_LOST_TIMEOUT_MS) {
          console.println("Соединение с сервером потеряно!");
          connectionLostMessageShown = true;
        }

      } catch (SocketTimeoutException e) {
        if (!connectionLostMessageShown &&
          Duration.between(lastSuccessTime, Instant.now()).toMillis() > CONNECTION_LOST_TIMEOUT_MS) {
          console.println("Соединение с сервером потеряно!");
          connectionLostMessageShown = true;
        }
        logger.warn("Таймаут подключения к серверу", e);
      } catch (IOException e) {
        if (!connectionLostMessageShown &&
          Duration.between(lastSuccessTime, Instant.now()).toMillis() > CONNECTION_LOST_TIMEOUT_MS) {
          console.println("Соединение с сервером потеряно!");
          connectionLostMessageShown = true;
        }
        logger.error("Ошибка подключения", e);
      }

      try {
        Thread.sleep(RECONNECT_INTERVAL_MS);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        logger.error("Поток переподключения прерван", e);
        break;
      }
    }
  }
}
