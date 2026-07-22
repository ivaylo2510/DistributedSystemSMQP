package dslab.util;

import java.security.SecureRandom;

public class RandomStringGenerator {

    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    public static String getSecureString() {
        return getSecureString(9);
    }

    public static String getSecureString(int length) {
        StringBuilder result = new StringBuilder(length);

        for (int i = 0; i < length; i++) {
            int index = RANDOM.nextInt(CHARACTERS.length());
            result.append(CHARACTERS.charAt(index));
        }

        return result.toString();
    }

    public static String[] getSecureStrings(int i) {
        String[] result = new String[i];

        for (int j = 0; j < i; j++) {
            result[j] = getSecureString();
        }

        return result;
    }
}
