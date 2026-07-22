package dslab.util;

import dslab.util.generator.SecureIntGenerator;
import dslab.util.generator.SecureStringGenerator;

public interface Global {
    SecureStringGenerator SECURE_STRING_GENERATOR = new SecureStringGenerator();
    SecureIntGenerator SECURE_INT_GENERATOR = new SecureIntGenerator();
}
