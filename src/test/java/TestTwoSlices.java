/*
 * Copyright (c) 2023 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

import com.sshtools.twoslices.Toast;
import com.sshtools.twoslices.ToastType;
import com.sshtools.twoslices.ToasterSettings;


/**
 * TestTwoSlices. ???
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2023-05-14 nsano initial version <br>
 */
public class TestTwoSlices {

    /**
     * @param args
     */
    public static void main(String[] args) {
        System.setProperty("twoslices.preferred", "com.sshtools.twoslices.impl.OsXToaster");
        Toast.toast(ToastType.INFO, "src/main/resources/duke.png", "Boo!", "See my image?");
    }
}
