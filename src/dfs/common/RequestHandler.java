package dfs.common;

import java.io.IOException;

// the server passes each command it receives to this handler, which decides what to do with it
public interface RequestHandler {
    void handle(String commandLine, Connection connection) throws IOException;
}