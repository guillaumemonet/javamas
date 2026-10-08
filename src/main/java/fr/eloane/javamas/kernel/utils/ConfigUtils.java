/*
 * The MIT License
 *
 * Copyright 2018 Guillaume Monet.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package fr.eloane.javamas.kernel.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 *
 * @author guillaume
 * @version 2.0.0
 */
public final class ConfigUtils {

    private static final System.Logger LOGGER = System.getLogger(ConfigUtils.class.getName());

    /**
     * The loaded configuration, null until {@link #loadConfig()} is called
     */
    public static Properties prop = null;

    /**
     * Load the config.properties resource
     *
     * @return if the configuration was loaded
     */
    public static boolean loadConfig() {
        prop = new Properties();
        try (InputStream in = ConfigUtils.class.getResourceAsStream("config.properties")) {
            if (in == null) {
                return false;
            }
            prop.load(in);
            return true;
        } catch (IOException e) {
            LOGGER.log(System.Logger.Level.WARNING, "Can't load config.properties", e);
            return false;
        }
    }

    /**
     *
     * @return the loaded configuration
     */
    public static Properties getProperties() {
        return prop;
    }
}
