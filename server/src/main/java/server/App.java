package server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import server.handlers.CommandHandler;
import server.managers.DumpManager;
import server.managers.CommandManager;
import server.network.UDPDatagramServer;
import server.repositories.ProductRepository;
import server.commands.*;
import common.utility.Commands;

import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.file.Paths;

public class App {
  public static final int PORT = 23586;
  public static final String DATA_FILE = "products.json"; // Имя файла с коллекцией

  public static Logger logger = LogManager.getLogger("ServerLogger");

  public static void main(String[] args) {
    // Получаем путь к файлу относительно расположения проекта
    String filePath = Paths.get("").toAbsolutePath().resolve(DATA_FILE).toString();
    logger.info("Используется файл данных: " + filePath);

    var dumpManager = new DumpManager(filePath);
    var repository = new ProductRepository(dumpManager);

    if(!repository.validateAll()) {
      logger.fatal("Невалидные продукты в загруженном файле!");
      System.exit(2);
    }

    Runtime.getRuntime().addShutdownHook(new Thread(repository::save));

    var commandManager = new CommandManager() {{
      register(Commands.HELP, new Help(this));
      register(Commands.INFO, new Info(repository));
      register(Commands.SHOW, new Show(repository));
      register(Commands.ADD, new Add(repository));
      register(Commands.UPDATE, new Update(repository));
      register(Commands.REMOVE_BY_ID, new RemoveById(repository));
      register(Commands.CLEAR, new Clear(repository));
      register(Commands.HEAD, new Head(repository));
      register(Commands.ADD_IF_MAX, new AddIfMax(repository));
      register(Commands.ADD_IF_MIN, new AddIfMin(repository));
      register(Commands.SUM_OF_PRICE, new SumOfPrice(repository));
      register(Commands.FILTER_BY_PRICE, new FilterByPrice(repository));
      register(Commands.FILTER_CONTAINS_PART_NUMBER, new FilterContainsPartNumber(repository));
    }};

    try {
      // Явно указываем 127.0.0.1 для IPv4
      var server = new UDPDatagramServer(InetAddress.getByName("127.0.0.1"), PORT, new CommandHandler(commandManager));
      logger.info("Сервер запущен на 127.0.0.1:" + PORT); // Логируем адрес

      server.setAfterHook(repository::save);
      server.run();
    } catch (SocketException e) {
      logger.fatal("Ошибка сокета. Возможно, порт " + PORT + " уже занят.", e);
    } catch (UnknownHostException e) {
      logger.fatal("Неизвестный хост", e);
    }
  }
}
