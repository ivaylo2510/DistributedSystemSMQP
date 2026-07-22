package dslab;

import dslab.client.Client;
import dslab.client.IClient;

import java.io.InputStream;
import java.io.OutputStream;

public class ComponentFactory {

    public static IClient createClient(String componentId, InputStream in, OutputStream out) {
        return new Client(componentId, in, out);
    }

}
