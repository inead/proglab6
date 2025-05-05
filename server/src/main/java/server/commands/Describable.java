package server.commands;

public interface Describable {
  /**
   * Получить имя.
   * @return имя
   */
  String getName();

  /**
   * Получить описание.
   * @return описание
   */
  String getDescription();
}
