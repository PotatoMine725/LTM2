package shared;

public final class Protocol {
    public static final String TEXT = "TEXT";
    public static final String IMAGE = "IMAGE";
    public static final String DISCONNECT = "DISCONNECT";
    public static final String HISTORY = "HISTORY";
    public static final String HISTORY_END = "HISTORY_END";
    public static final int DEFAULT_PORT = 8080;
    public static final int MAX_IMAGE_SIZE = 10 * 1024 * 1024;
    public static final String IMAGE_SAVE_DIR = "received_images";

    private Protocol() {
    }

    public static boolean isCommand(String value) {
        return TEXT.equals(value) || IMAGE.equals(value) || DISCONNECT.equals(value);
    }
}
