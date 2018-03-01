/*
 * Debug.java
 *
 * Created on 2007. február 13., 13:13
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package amdocs.ra.share;

/**
 * This class can be used to turn on-off the debug logging of the resource adapter.
 * This utility class has a member variable that controls the logging of the
 * Amdocs resource adapter (turns on/off). When logging is off no debug info is
 * written into the log -only error messages.
 *
 * @author attila.rezner
 */
public class Debug {

    /** This variable sets the logging on/off Currently: {@value}. */
    public static final boolean LOG_INFO = true;

    /** Creates a new instance of Debug */
    public Debug() {
    }

}
