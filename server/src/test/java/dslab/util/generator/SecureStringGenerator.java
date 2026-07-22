package dslab.util.generator;

import java.security.SecureRandom;

public class SecureStringGenerator {

    private final SecureRandom random = new SecureRandom();

    public String getSecureString() {
        return getSecureString(9);
    }

    public String getSecureString(int length) {
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

        StringBuilder result = new StringBuilder(length);

        for (int i = 0; i < length; i++) {
            int index = random.nextInt(characters.length());
            result.append(characters.charAt(index));
        }

        return result.toString();
    }
}
