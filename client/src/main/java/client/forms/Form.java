package client.forms;

import common.exceptions.IncorrectInputInScriptException;
import common.exceptions.InvalidFormException;

public abstract class Form<T> {
  public abstract T build() throws IncorrectInputInScriptException, InvalidFormException;
}
