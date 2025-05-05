package server.commands;

import common.network.requests.Request;
import common.network.responses.Response;

public interface Executable {
  /**
   * Выполнить что-либо.
   * @param request запрос с данными для выполнения команды
   * @return результат выполнения
   */
  Response apply(Request request);
}
